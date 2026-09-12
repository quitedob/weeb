package com.web.campus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.web.service.UserPreferencesService;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/** Durable inbox rows are part of the business transaction; socket delivery is best effort after commit. */
@Service
public class CampusNotificationService {
    private static final Logger log = LoggerFactory.getLogger(CampusNotificationService.class);
    private static final Set<String> POST_TYPES = Set.of("CAMPUS_REVIEW", "CAMPUS_LIKE", "CAMPUS_COMMENT", "CAMPUS_REPLY");
    private static final Set<String> SCHOOL_TYPES = Set.of("CAMPUS_VERIFICATION", "CAMPUS_MEMBERSHIP");
    private final JdbcTemplate jdbc;
    private final CampusAccessService access;
    private final UserPreferencesService preferences;
    private final ObjectProvider<SimpMessagingTemplate> messaging;
    private final TransactionTemplate committedRead;
    private final ThreadPoolExecutor deliveries = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128), task -> {
                Thread thread = new Thread(task, "campus-notification-delivery");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public CampusNotificationService(JdbcTemplate jdbc, CampusAccessService access, UserPreferencesService preferences,
                                     ObjectProvider<SimpMessagingTemplate> messaging, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.access = access;
        this.preferences = preferences;
        this.messaging = messaging;
        this.committedRead = new TransactionTemplate(manager);
        this.committedRead.setReadOnly(true);
        this.committedRead.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.committedRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public void send(long actor, long recipient, String type, String entityType, long entityId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Campus notifications require a transaction");
        if (!(POST_TYPES.contains(type) && "campus_post".equals(entityType))
                && !(SCHOOL_TYPES.contains(type) && "campus_school".equals(entityType))) {
            throw new IllegalArgumentException("Invalid campus notification contract");
        }
        if (actor == recipient || !preferences.isNotificationEnabled(recipient, type)) return;
        List<String> recipients = jdbc.query("SELECT username FROM `user` WHERE id=? AND status=1", (rs, n) -> rs.getString(1), recipient);
        List<String> actors = jdbc.query("SELECT username FROM `user` WHERE id=? AND status=1", (rs, n) -> rs.getString(1), actor);
        if (recipients.isEmpty() || actors.isEmpty()) return;
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        LocalDateTime createdAt = LocalDateTime.now();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO notifications(recipient_id,actor_id,type,entity_type,entity_id,is_read,created_at) VALUES (?,?,?,?,?,FALSE,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, recipient); statement.setLong(2, actor); statement.setString(3, type);
            statement.setString(4, entityType); statement.setLong(5, entityId); statement.setObject(6, createdAt);
            return statement;
        }, key);
        long id = java.util.Objects.requireNonNull(key.getKey()).longValue();
        Map<String, Object> payload = Map.of("id", id, "type", type, "entityType", entityType, "entityId", entityId,
                "actorId", actor, "actorName", actors.get(0), "createdAt", createdAt);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    // Return immediately so the outer transaction releases its pooled connection.
                    // AbortPolicy keeps saturated delivery queues off the request thread.
                    deliveries.execute(() -> {
                        try {
                            Boolean deliver = committedRead.execute(status -> {
                                access.requireActiveUser(recipient);
                                return !"campus_post".equals(entityType) || access.canReadPost(recipient, entityId);
                            });
                            if (Boolean.TRUE.equals(deliver)) messaging.getObject().convertAndSendToUser(recipients.get(0), "/queue/notifications", payload);
                        } catch (Exception failure) {
                            log.debug("Campus notification {} delivery deferred: {}", id, failure.getClass().getSimpleName());
                        }
                    });
                } catch (RejectedExecutionException failure) {
                    // No credentials, application details or content in logs. REST inbox is the recovery path.
                    log.debug("Campus notification {} delivery deferred: {}", id, failure.getClass().getSimpleName());
                }
            }
        });
    }

    @jakarta.annotation.PreDestroy
    public void close() {
        deliveries.shutdown();
        try { if (!deliveries.awaitTermination(5, TimeUnit.SECONDS)) deliveries.shutdownNow(); }
        catch (InterruptedException interrupted) { deliveries.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
