package com.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.ChatAccessService;
import com.web.service.UserOnlineStatusService;
import com.web.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class SpringWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Set<String> USER_QUEUES = Set.of(
            "private", "chat-list-update", "message-status", "read-receipt", "group-member-change",
            "group-info-change", "reaction-change", "errors", "contacts", "notifications", "redis");
    private static final Set<String> SEND_DESTINATIONS = Set.of(
            "/app/chat/private", "/app/chat.sendMessage", "/app/chat/heartbeat", "/app/chat/read-receipt");

    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserMapper userMapper;
    @Autowired private UserOnlineStatusService onlineStatusService;
    @Autowired private ChatAccessService chatAccessService;
    @Autowired private ObjectMapper objectMapper;

    private final Map<String, AuthenticatedSession> sessions = new ConcurrentHashMap<>();
    private record AuthenticatedSession(Long userId, String username, String token,
                                        Map<String, String> subscriptions) {}

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024).setSendBufferSizeLimit(512 * 1024).setSendTimeLimit(20000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(4).maxPoolSize(8).keepAliveSeconds(60);
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(4).maxPoolSize(8).keepAliveSeconds(60);
        registration.interceptors(new WebSocketDeliveryInterceptor());
    }

    @EventListener
    public void forgetDisconnectedSession(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId());
    }

    /** Broker deliveries are checked even when the subscriber sends no further frames. */
    class WebSocketDeliveryInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
            String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
            String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(message.getHeaders());
            AuthenticatedSession session = sessionId == null ? null : sessions.get(sessionId);
            if (session == null || subscriptionId == null) return null;
            String destination = session.subscriptions().get(subscriptionId);
            if (destination == null) return null;
            try {
                User user = validateUser(session.token());
                if (!session.userId().equals(user.getId()) || !session.username().equals(user.getUsername())) return null;
                // Use the captured subscription, since /user queues are rewritten to broker session destinations.
                authorizeSubscription(user.getId(), destination);
                if (Set.of("/user/queue/group-info-change", "/user/queue/group-member-change").contains(destination)) {
                    JsonNode body = deliveryBody(message.getPayload());
                    JsonNode group = body == null ? null : body.get("groupId");
                    if (group == null || !group.asText().matches("[1-9][0-9]*")) return null;
                    boolean dissolved = "GROUP_INFO_CHANGE".equals(body.path("type").asText())
                            && "GROUP_DISSOLVED".equals(body.path("changeType").asText());
                    boolean ownRemoval = "GROUP_MEMBER_CHANGE".equals(body.path("type").asText())
                            && Set.of("MEMBER_REMOVED", "MEMBER_LEFT").contains(body.path("changeType").asText())
                            && user.getId().equals(body.path("affectedUserId").asLong(-1));
                    if (!dissolved && !ownRemoval) chatAccessService.resolveRoom(user.getId(), "group_" + group.asText());
                }
                if (Set.of("/user/queue/private", "/user/queue/chat-list-update", "/user/queue/read-receipt",
                        "/user/queue/message-status", "/user/queue/reaction-change").contains(destination)) {
                    JsonNode body = deliveryBody(message.getPayload());
                    JsonNode chat = body == null ? null : body.get("sharedChatId");
                    if (chat == null || chat.isNull()) chat = body == null ? null : body.get("chatId");
                    if (chat != null && !chat.isNull()) chatAccessService.resolveRoom(user.getId(), chat.asText());
                    else if ("/user/queue/private".equals(destination)) return null;
                }
                return message;
            } catch (Exception rejected) {
                // Do not log token-bearing headers or payloads on a denied delivery.
                return null;
            }
        }

        private JsonNode deliveryBody(Object payload) throws java.io.IOException {
            return payload instanceof byte[] bytes ? objectMapper.readTree(bytes)
                    : payload instanceof String text ? objectMapper.readTree(text) : objectMapper.valueToTree(payload);
        }
    }

    class WebSocketAuthInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) {
                throw new AccessDeniedException("Missing message headers");
            }
            StompCommand command = accessor.getCommand();
            if (command == StompCommand.CONNECT) {
                connect(accessor);
            } else if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
                User user = requireAuthenticatedUser(accessor);
                if (command == StompCommand.SUBSCRIBE) {
                    authorizeSubscription(user.getId(), accessor.getDestination());
                    AuthenticatedSession session = sessions.get(accessor.getSessionId());
                    if (session != null && accessor.getSubscriptionId() != null) {
                        session.subscriptions().put(accessor.getSubscriptionId(), accessor.getDestination());
                    }
                } else {
                    authorizeSend(user.getId(), accessor.getDestination());
                }
            } else if (command == StompCommand.UNSUBSCRIBE) {
                AuthenticatedSession session = sessions.get(accessor.getSessionId());
                if (session != null && accessor.getSubscriptionId() != null) session.subscriptions().remove(accessor.getSubscriptionId());
            } else if (command == StompCommand.DISCONNECT && accessor.getSessionId() != null) {
                sessions.remove(accessor.getSessionId());
            }
            return message;
        }

        private void connect(StompHeaderAccessor accessor) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("Authentication required");
            }
            String token = authorization.substring(7);
            User user = validateUser(token);
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes == null || accessor.getSessionId() == null) {
                throw new AccessDeniedException("Missing connection session");
            }
            attributes.put("accessToken", token);
            attributes.put("username", user.getUsername());
            attributes.put("userId", user.getId());
            accessor.setUser(new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
            sessions.put(accessor.getSessionId(), new AuthenticatedSession(user.getId(), user.getUsername(), token,
                    new ConcurrentHashMap<>()));
            try {
                onlineStatusService.userOnline(user.getId(), accessor.getSessionId());
            } catch (Exception e) {
                log.warn("Unable to update WebSocket online status for user {}", user.getId());
            }
        }

        private User requireAuthenticatedUser(StompHeaderAccessor accessor) {
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (accessor.getUser() == null || attributes == null
                    || !(attributes.get("accessToken") instanceof String token)) {
                throw new AccessDeniedException("Authentication required");
            }
            User user = validateUser(token);
            if (!user.getUsername().equals(accessor.getUser().getName())) {
                throw new AccessDeniedException("Connection identity mismatch");
            }
            return user;
        }

    }

    private User validateUser(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new AccessDeniedException("Invalid access token");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !user.getUsername().equals(jwtUtil.extractUsername(token))) {
            throw new AccessDeniedException("Account unavailable");
        }
        return user;
    }

    void authorizeSubscription(Long userId, String destination) {
        if (destination != null && destination.startsWith("/user/queue/")
                && USER_QUEUES.contains(destination.substring("/user/queue/".length()))) {
            return;
        }
        if ("/app/chat/connect".equals(destination)) {
            return;
        }
        if (destination != null && destination.startsWith("/topic/chat/")) {
            chatAccessService.resolveRoom(userId, destination.substring("/topic/chat/".length()));
            return;
        }
        throw new AccessDeniedException("Subscription denied");
    }

    void authorizeSend(Long userId, String destination) {
        if (SEND_DESTINATIONS.contains(destination == null ? "" : destination)) {
            return; // Payload resource authorization runs in ChatService before writes.
        }
        if (destination != null) {
            for (String action : List.of("join", "leave", "typing")) {
                String prefix = "/app/chat/" + action + "/";
                if (destination.startsWith(prefix)) {
                    chatAccessService.resolveRoom(userId, destination.substring(prefix.length()));
                    return;
                }
            }
            if (destination.matches("/app/chat/recall/[1-9][0-9]*")) {
                return; // Sender ownership is checked by the recall handler.
            }
        }
        // Clients cannot publish directly to broker destinations.
        throw new AccessDeniedException("Destination denied");
    }
}
