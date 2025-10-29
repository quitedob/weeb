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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，支持 /topic 和 /queue 前缀
        // 配置心跳：服务端每25秒发送心跳，期望客户端每25秒发送心跳
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000});

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
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("收到WebSocket CONNECT请求");
                    
                    // 从连接头中获取Authorization token
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.info("Authorization头: {}", authHeader != null ? "存在" : "不存在");
                    
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        log.info("提取到token，长度: {}", token.length());
                        
                        try {
                            // 验证JWT token
                            if (jwtUtil.validateToken(token)) {
                                Long userId = jwtUtil.getUserIdFromToken(token);
                                String userIdStr = String.valueOf(userId);
                                
                                // 创建认证对象
                                Principal principal = () -> userIdStr;
                                accessor.setUser(principal);
                                
                                log.info("✅ WebSocket连接认证成功: userId={}", userId);
                            } else {
                                log.warn("❌ WebSocket连接认证失败: token无效");
                            }
                        } catch (Exception e) {
                            log.error("❌ WebSocket认证异常", e);
                        }
                    } else {
                        log.warn("❌ WebSocket连接缺少Authorization头或格式错误");
                    }
                }
                
                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 配置客户端出站通道
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveSeconds(60);
    }
}