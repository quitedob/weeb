package com.web.controller;

import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.*;
import com.web.vo.message.TextMessageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Authenticated STOMP handlers; all resource checks precede persistence or fanout. */
@Slf4j
@Controller
public class WebSocketMessageController {
    @Autowired private ChatService chatService;
    @Autowired private ChatAccessService chatAccessService;
    @Autowired private UserService userService;
    @Autowired private MessageMapper messageMapper;
    @Autowired private SimpMessageSendingOperations messagingTemplate;
    @Autowired private MessageBroadcastService messageBroadcastService;
    @Autowired private MessageDeduplicationService deduplicationService;
    @Autowired private WebSocketConnectionService connectionService;

    private User requireUser(Principal principal) {
        User user = principal == null ? null : userService.findByUsername(principal.getName());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new AccessDeniedException("Authentication required");
        }
        return user;
    }

    @SubscribeMapping("/chat/connect")
    public String handleConnect(Principal principal, SimpMessageHeaderAccessor headers) {
        User user = requireUser(principal);
        if (headers.getSessionAttributes() != null) {
            headers.getSessionAttributes().put("username", user.getUsername());
            headers.getSessionAttributes().put("connectTime", LocalDateTime.now());
        }
        return "Connected";
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        User user = requireUser(principal);
        Long sharedChatId = chatAccessService.resolveRoom(user.getId(), stringValue(payload.get("roomId")));
        saveAndConfirm(user, sharedChatId, null, payload);
    }

    @MessageMapping("/chat/private")
    public void sendPrivateMessage(@Payload Map<String, Object> payload, Principal principal) {
        User user = requireUser(principal);
        ChatList chat;
        if (payload.get("chatId") != null) {
            chat = chatAccessService.requireOwnedChat(user.getId(), payload.get("chatId").toString());
        } else {
            Long targetId = longValue(payload.get("targetId"));
            if (targetId == null && payload.get("targetUser") != null) {
                User target = userService.findByUsername(payload.get("targetUser").toString());
                targetId = target == null ? null : target.getId();
            }
            if (targetId == null) {
                throw new IllegalArgumentException("Recipient required");
            }
            chat = chatService.createChat(user.getId(), targetId);
        }
        if (!"PRIVATE".equals(chat.getType())) {
            throw new AccessDeniedException("Not a private conversation");
        }
        chatAccessService.requireAccess(user.getId(), chat.getSharedChatId());
        saveAndConfirm(user, chat.getSharedChatId(), chat.getId(), payload);
    }

    private void saveAndConfirm(User user, Long sharedChatId, String chatId, Map<String, Object> payload) {
        String clientId = stringValue(payload.get("clientMessageId"));
        // A client-generated identifier is scoped to its authenticated sender.
        String deduplicationKey = clientId == null ? null : user.getId() + ":" + clientId;
        if (deduplicationKey != null && deduplicationService.isDuplicate(deduplicationKey)) {
            Long id = deduplicationService.getMessageId(deduplicationKey);
            Message existing = id == null ? null : messageMapper.selectMessageById(id);
            if (existing != null && user.getId().equals(existing.getSenderId())
                    && sharedChatId.equals(existing.getChatId())) {
                messageBroadcastService.confirmMessageToSender(existing, user.getId(), clientId);
                return;
            }
            throw new IllegalArgumentException("Client message ID already used");
        }
        Message message = toMessage(payload, user.getId());
        Message saved = chatId == null
                ? chatService.sendMessageBySharedChatId(user.getId(), sharedChatId, message)
                : chatService.sendMessage(user.getId(), chatId, message);
        if (deduplicationKey != null) {
            deduplicationService.markAsProcessed(deduplicationKey, saved.getId());
        }
        messageBroadcastService.confirmMessageToSender(saved, user.getId(), clientId);
    }

    private Message toMessage(Map<String, Object> payload, Long userId) {
        TextMessageContent content = new TextMessageContent();
        Object rawContent = payload.get("content");
        if (rawContent instanceof Map<?, ?> values) {
            content.setContent(stringValue(values.get("content")));
            content.setUrl(stringValue(values.get("url")));
            content.setContentType(values.get("contentType") == null ? 1
                    : Integer.valueOf(values.get("contentType").toString()));
        } else {
            content.setContent(stringValue(rawContent));
            content.setUrl(stringValue(payload.get("url")));
            String type = stringValue(payload.get("type"));
            content.setContentType("image".equals(type) ? 2 : "file".equals(type) ? 3 : 1);
        }
        Message message = new Message();
        message.setSenderId(userId);
        message.setContent(content);
        message.setMessageType(payload.get("messageType") == null ? 1
                : Integer.valueOf(payload.get("messageType").toString()));
        message.setStatus(Message.STATUS_SENT);
        message.setIsRecalled(0);
        message.setSource("WebSocket");
        message.setReplyToMessageId(longValue(payload.get("replyToMessageId")));
        message.setThreadId(longValue(payload.get("threadId")));
        return message;
    }

    @MessageMapping("/chat/join/{roomId}")
    public void joinRoom(@DestinationVariable String roomId, Principal principal) {
        publishRoomEvent("join", roomId, principal, Map.of());
    }

    @MessageMapping("/chat/leave/{roomId}")
    public void leaveRoom(@DestinationVariable String roomId, Principal principal) {
        publishRoomEvent("leave", roomId, principal, Map.of());
    }

    @MessageMapping("/chat/typing/{roomId}")
    public void handleTyping(@DestinationVariable String roomId, @Payload Map<String, Object> payload,
                             Principal principal) {
        publishRoomEvent("typing", roomId, principal,
                Map.of("isTyping", Boolean.TRUE.equals(payload.get("isTyping"))));
    }

    private void publishRoomEvent(String type, String roomId, Principal principal, Map<String, Object> data) {
        User user = requireUser(principal);
        Long sharedChatId = chatAccessService.resolveRoom(user.getId(), roomId);
        Map<String, Object> event = new HashMap<>(data);
        event.put("type", type);
        event.put("username", user.getUsername());
        event.put("userId", user.getId());
        event.put("chatId", sharedChatId);
        event.put("roomId", roomId);
        event.put("timestamp", LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, event);
    }

    @MessageMapping("/chat/recall/{messageId}")
    public void recallMessage(@DestinationVariable Long messageId, Principal principal) {
        User user = requireUser(principal);
        chatService.recallMessage(user.getId(), messageId);
    }

    @MessageMapping("/chat/heartbeat")
    public void handleHeartbeat(Principal principal, SimpMessageHeaderAccessor headers) {
        requireUser(principal);
        if (headers.getSessionId() != null) {
            connectionService.updateHeartbeat(headers.getSessionId());
        }
    }

    @MessageMapping("/chat/read-receipt")
    public void handleReadReceipt(@Payload Map<String, Object> receipt, Principal principal) {
        User user = requireUser(principal);
        ChatList chat = chatAccessService.requireOwnedChat(user.getId(), stringValue(receipt.get("chatId")));
        Long messageId = longValue(receipt.get("messageId"));
        Message message = messageId == null ? null : messageMapper.selectMessageById(messageId);
        if (message == null || !chat.getSharedChatId().equals(message.getChatId())) {
            throw new AccessDeniedException("Message is not in this conversation");
        }
        chatService.markAsReadBySharedChatId(user.getId(), chat.getSharedChatId());
        User sender = userService.getUserBasicInfo(message.getSenderId());
        if (sender != null && !user.getId().equals(sender.getId())) {
            Map<String, Object> event = new HashMap<>();
            event.put("chatId", chat.getSharedChatId());
            event.put("messageId", messageId);
            event.put("status", Message.STATUS_READ);
            event.put("timestamp", LocalDateTime.now());
            messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/read-receipt", event);
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception exception, Principal principal) {
        log.warn("WebSocket request rejected: {}", exception.getClass().getSimpleName());
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                    Map.of("type", "error", "message", "Message request rejected"));
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longValue(Object value) {
        return value == null ? null : Long.valueOf(value.toString());
    }
}
