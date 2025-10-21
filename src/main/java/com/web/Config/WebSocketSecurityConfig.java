package com.web.Config;

import com.web.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 安全配置
 * 处理 WebSocket 连接的认证和授权
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    /**
     * WebSocket 认证拦截器
     */
    private class WebSocketAuthInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // 处理连接认证
                handleConnectAuthentication(accessor);
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                // 处理订阅授权
                handleSubscribeAuthorization(accessor);
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                // 处理发送消息授权
                handleSendAuthorization(accessor);
            }

            return message;
        }

        /**
         * 处理连接认证
         */
        private void handleConnectAuthentication(StompHeaderAccessor accessor) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            String sessionId = accessor.getSessionId();

            log.debug("WebSocket连接认证: sessionId={}, token={}", sessionId, authToken);

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String token = authToken.substring(7);

                try {
                    if (jwtUtil.validateToken(token)) {
                        String username = jwtUtil.extractUsername(token);

                        // 设置用户认证信息
                        Authentication auth = new WebSocketAuthentication(username);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        accessor.setUser(auth);

                        accessor.setHeader("username", username);
                        log.info("WebSocket用户认证成功: username={}, sessionId={}", username, sessionId);
                    } else {
                        log.warn("WebSocket连接认证失败: 无效的token, sessionId={}", sessionId);
                        throw new RuntimeException("认证失败");
                    }
                } catch (Exception e) {
                    log.error("WebSocket连接认证异常: sessionId={}", sessionId, e);
                    throw new RuntimeException("认证异常: " + e.getMessage());
                }
            } else {
                log.warn("WebSocket连接缺少认证token: sessionId={}", sessionId);
                throw new RuntimeException("缺少认证token");
            }
        }

        /**
         * 处理订阅授权
         */
        private void handleSubscribeAuthorization(StompHeaderAccessor accessor) {
            String destination = accessor.getDestination();
            String username = accessor.getUser().getName();

            log.debug("WebSocket订阅检查: username={}, destination={}", username, destination);

            if (destination == null) {
                return;
            }

            // 检查订阅权限
            if (destination.startsWith("/topic/chat/")) {
                // 检查用户是否有权限加入聊天室
                String roomId = extractRoomId(destination);
                if (!hasChatRoomAccess(username, roomId)) {
                    log.warn("用户 {} 无权限访问聊天室 {}", username, roomId);
                    throw new RuntimeException("无权限访问聊天室");
                }
            } else if (destination.startsWith("/user/")) {
                // 检查用户订阅的是否为自己的队列
                String targetUser = extractTargetUser(destination);
                if (!username.equals(targetUser)) {
                    log.warn("用户 {} 尝试订阅其他用户的队列: {}", username, targetUser);
                    throw new RuntimeException("无权限订阅其他用户的队列");
                }
            }
        }

        /**
         * 处理发送消息授权
         */
        private void handleSendAuthorization(StompHeaderAccessor accessor) {
            String destination = accessor.getDestination();
            String username = accessor.getUser().getName();

            log.debug("WebSocket发送消息检查: username={}, destination={}", username, destination);

            if (destination == null) {
                return;
            }

            // 检查发送权限
            if (destination.startsWith("/app/chat/")) {
                // 检查用户是否有权限发送消息到聊天室
                String roomId = extractRoomId(destination);
                if (!hasChatRoomAccess(username, roomId)) {
                    log.warn("用户 {} 无权限向聊天室 {} 发送消息", username, roomId);
                    throw new RuntimeException("无权限发送消息");
                }
            }
        }

        private String extractRoomId(String destination) {
            // 从 /topic/chat/{roomId} 中提取 roomId
            String[] parts = destination.split("/");
            return parts.length > 3 ? parts[3] : null;
        }

        private String extractTargetUser(String destination) {
            // 从 /user/{username}/... 中提取用户名
            String[] parts = destination.split("/");
            return parts.length > 1 ? parts[1] : null;
        }

        private boolean hasChatRoomAccess(String username, String roomId) {
            // 这里应该实现实际的聊天室权限检查逻辑
            // 例如检查用户是否是聊天室成员
            return true; // 简化实现，实际需要查询数据库
        }
    }

    /**
     * WebSocket 认证对象
     */
    private static class WebSocketAuthentication implements Authentication {

        private final String username;
        private final boolean authenticated;

        public WebSocketAuthentication(String username) {
            this.username = username;
            this.authenticated = true;
        }

        @Override
        public String getName() {
            return username;
        }

        @Override
        public List getAuthorities() {
            return List.of(); // 可以根据需要返回权限
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            // 只读实现
        }

        @Override
        public String toString() {
            return "WebSocketAuthentication{" +
                    "username='" + username + '\'' +
                    ", authenticated=" + authenticated +
                    '}';
        }
    }
}