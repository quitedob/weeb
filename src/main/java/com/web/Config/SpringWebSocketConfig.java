package com.web.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Spring WebSocket 配置类
 * 用于替代自定义 Netty WebSocket 实现
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class SpringWebSocketConfig implements WebSocketMessageBrokerConfigurer {

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