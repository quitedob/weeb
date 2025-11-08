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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.security.Principal;
import java.util.List;

/**
 * Spring WebSocket 配置类
 * 用于替代自定义 Netty WebSocket 实现
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class SpringWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.web.service.UserOnlineStatusService onlineStatusService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，支持 /topic 和 /queue 前缀
        // 简化配置，不设置心跳以避免TaskScheduler问题
        config.enableSimpleBroker("/topic", "/queue");

        // 设置应用目标前缀
        config.setApplicationDestinationPrefixes("/app");

        // 设置用户目标前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 在生产环境中应该设置具体的允许来源
                .withSockJS(); // 启用 SockJS 支持，提供降级方案
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 配置WebSocket传输参数
        registration
                .setMessageSizeLimit(128 * 1024) // 消息大小限制：128KB
                .setSendBufferSizeLimit(512 * 1024) // 发送缓冲区大小：512KB
                .setSendTimeLimit(20 * 1000); // 发送超时时间：20秒
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 配置客户端入站通道
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);

        // 添加认证拦截器
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 配置客户端出站通道
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);
    }

    /**
     * WebSocket 认证拦截器
     */
    private class WebSocketAuthInterceptor implements ChannelInterceptor {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

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
         * ✅ 修复2：处理连接认证（增强版）
         */
        private void handleConnectAuthentication(StompHeaderAccessor accessor) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            String sessionId = accessor.getSessionId();

            log.info("🔐 WebSocket连接认证开始: sessionId={}, token={}", sessionId, authToken != null ? "存在" : "不存在");

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String token = authToken.substring(7);
                
                log.debug("🔑 Token内容: {}", token.substring(0, Math.min(20, token.length())) + "...");

                try {
                    // ✅ 修复2：详细的token验证日志
                    boolean isValid = jwtUtil.validateToken(token);
                    log.info("🔍 Token验证结果: {}", isValid ? "有效" : "无效");
                    
                    if (isValid) {
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        String username = jwtUtil.extractUsername(token);
                        String userIdStr = String.valueOf(userId);

                        log.info("👤 从Token提取用户信息: userId={}, username={}", userId, username);

                        // ✅ 修复2：设置用户认证信息（使用username而不是userId）
                        Principal principal = () -> username != null ? username : userIdStr;
                        accessor.setUser(principal);

                        // ✅ 修复2：在session属性中保存用户信息
                        accessor.getSessionAttributes().put("username", username != null ? username : userIdStr);
                        accessor.getSessionAttributes().put("userId", userId);

                        // 更新在线状态
                        try {
                            onlineStatusService.userOnline(userId, sessionId);
                            log.info("📡 用户在线状态已更新: userId={}", userId);
                        } catch (Exception e) {
                            log.warn("⚠️ 更新在线状态失败: {}", e.getMessage());
                            // 不中断连接，只记录警告
                        }

                        log.info("✅ WebSocket用户认证成功: userId={}, username={}, sessionId={}", userId, username, sessionId);
                    } else {
                        log.warn("❌ WebSocket连接认证失败: Token验证失败, sessionId={}", sessionId);
                        throw new RuntimeException("Token验证失败");
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    log.error("❌ WebSocket连接认证失败: Token已过期, sessionId={}", sessionId);
                    throw new RuntimeException("Token已过期");
                } catch (io.jsonwebtoken.MalformedJwtException e) {
                    log.error("❌ WebSocket连接认证失败: Token格式错误, sessionId={}", sessionId);
                    throw new RuntimeException("Token格式错误");
                } catch (Exception e) {
                    log.error("❌ WebSocket连接认证异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
                    throw new RuntimeException("认证异常: " + e.getMessage());
                }
            } else {
                log.warn("❌ WebSocket连接缺少认证token: sessionId={}, authToken={}", sessionId, authToken);
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
}