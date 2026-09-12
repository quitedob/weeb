package com.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.MessageMapper;
import com.web.mapper.UserMapper;
import com.web.model.Message;
import com.web.model.User;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Durable retry of broker dispatch, not proof that a browser received an event. */
@Slf4j
@Service
public class MessageOutboxService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final MessageMapper messages;
    private final UserMapper users;
    private final ChatAccessService access;
    private final MessageBroadcastService broadcast;
    private final TransactionTemplate transaction;
    private final AtomicBoolean dispatching = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "message-outbox");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${weeb.message-outbox.auto-dispatch:true}")
    private boolean autoDispatch = true;

    public MessageOutboxService(JdbcTemplate jdbc, ObjectMapper json, MessageMapper messages,
                                UserMapper users, ChatAccessService access, MessageBroadcastService broadcast,
                                PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.json = json;
        this.messages = messages;
        this.users = users;
        this.access = access;
        this.broadcast = broadcast;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueMessage(Message message, List<Long> recipients) {
        Map<String, Object> payload = json.convertValue(message, new TypeReference<>() {});
        User sender = users.selectById(message.getSenderId());
        payload.put("messageId", message.getId());
        payload.put("sharedChatId", message.getChatId());
        payload.put("fromId", message.getSenderId());
        payload.put("fromName", sender == null ? "" : sender.getUsername());
        payload.put("msgContent", message.getContent() == null ? "" : message.getContent().getContent());
        payload.put("timestamp", message.getCreatedAt());
        enqueue("MESSAGE:" + message.getId(), "MESSAGE", message.getChatId(), message.getId(), recipients, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String eventKey, String eventType, Long chatId, Long messageId,
                        List<Long> recipients, Map<String, Object> payload) {
        if (!Set.of("MESSAGE", "READ", "REACTION").contains(eventType)) {
            throw new IllegalArgumentException("Unsupported message event");
        }
        final String body;
        try { body = json.writeValueAsString(payload); }
        catch (Exception e) { throw new IllegalArgumentException("Cannot serialize message event", e); }
        for (Long recipient : new LinkedHashSet<>(recipients)) {
            if (recipient == null) continue;
            jdbc.update("INSERT INTO message_outbox(event_key,recipient_id,chat_id,message_id,event_type,payload) "
                            + "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE event_key=VALUES(event_key)",
                    eventKey, recipient, chatId, messageId, eventType, body);
        }
        if (autoDispatch && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { executor.execute(MessageOutboxService.this::dispatchDue); }
            });
        }
    }

    public void dispatchScheduled() {
        if (autoDispatch) dispatchDue();
    }

    /** Atomic SQL leases also recover a worker that died after claiming an event. */
    public int dispatchDue() {
        if (!dispatching.compareAndSet(false, true)) return 0;
        try {
            List<Map<String, Object>> claimed = transaction.execute(status -> {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM message_outbox "
                        + "WHERE delivered_at IS NULL AND available_at<=CURRENT_TIMESTAMP(3) "
                        + "AND (lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP(3)) "
                        + "ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED");
                for (Map<String, Object> row : rows) {
                    String token = UUID.randomUUID().toString();
                    jdbc.update("UPDATE message_outbox SET lease_token=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 30 SECOND),"
                            + "attempts=attempts+1 WHERE id=?", token, row.get("id"));
                    row.put("lease_token", token);
                }
                return rows;
            });
            if (claimed == null) return 0;
            int completed = 0;
            for (Map<String, Object> row : claimed) {
                try {
                    transaction.executeWithoutResult(status -> dispatch(row));
                    completed++;
                } catch (Exception failure) {
                    // Keep only the exception type; messages can contain private payloads or database details.
                    transaction.executeWithoutResult(status -> jdbc.update("UPDATE message_outbox SET lease_until=NULL,lease_token=NULL,"
                                    + "available_at=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 5 SECOND),last_error=? "
                                    + "WHERE id=? AND lease_token=? AND delivered_at IS NULL",
                            failure.getClass().getSimpleName(), row.get("id"), row.get("lease_token")));
                }
            }
            return completed;
        } catch (Exception failure) {
            log.warn("Message outbox remains pending: {}", failure.getClass().getSimpleName());
            return 0;
        } finally {
            dispatching.set(false);
        }
    }

    private void dispatch(Map<String, Object> row) {
        // Lock and recheck the lease so an expired/reclaimed worker cannot mark another worker's job complete.
        List<String> leases = jdbc.queryForList("SELECT lease_token FROM message_outbox WHERE id=? "
                + "AND delivered_at IS NULL FOR UPDATE", String.class, row.get("id"));
        if (leases.isEmpty() || !Objects.equals(leases.get(0), row.get("lease_token"))) return;
        Long recipientId = ((Number) row.get("recipient_id")).longValue();
        Long chatId = ((Number) row.get("chat_id")).longValue();
        Long messageId = ((Number) row.get("message_id")).longValue();
        User recipient = users.selectById(recipientId);
        Message message = messages.selectMessageById(messageId);
        String skipped = null;
        if (recipient == null || !Integer.valueOf(1).equals(recipient.getStatus()) || message == null
                || !chatId.equals(message.getChatId())) skipped = "recipient or message unavailable";
        if (skipped == null) {
            try { access.requireAccess(recipientId, chatId); }
            catch (AccessDeniedException denied) { skipped = "access revoked"; }
        }
        String type = row.get("event_type").toString();
        if ("MESSAGE".equals(type) && Integer.valueOf(1).equals(message == null ? null : message.getIsRecalled())) {
            skipped = "message recalled";
        }
        if (skipped == null) {
            try {
                Map<String, Object> payload = json.readValue(row.get("payload").toString(), new TypeReference<>() {});
                payload.put("eventId", row.get("id"));
                if ("MESSAGE".equals(type)) {
                    payload.put("isFromMe", recipientId.equals(message.getSenderId()));
                    payload.put("status", message.getStatus());
                }
                broadcast.dispatchOutboxEvent(recipient.getUsername(), type, payload);
            } catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new IllegalStateException("Cannot read persisted message event", e); }
        }
        jdbc.update("UPDATE message_outbox SET delivered_at=CURRENT_TIMESTAMP(3),lease_until=NULL,lease_token=NULL,last_error=? "
                        + "WHERE id=? AND lease_token=?", skipped, row.get("id"), row.get("lease_token"));
    }

    @PreDestroy public void close() { executor.shutdownNow(); }
}
