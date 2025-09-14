package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.dto.RedisBroadcastMsg;
import org.slf4j.Logger; // Added for logging
import org.slf4j.LoggerFactory; // Added for logging
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Redis消息订阅者服务
 * 简化注释：Redis订阅者
 */
@Service
public class RedisSubscriber {

    private static final Logger log = LoggerFactory.getLogger(RedisSubscriber.class); // Added logger

    @Autowired
    private WebSocketService webSocketService; // Assuming WebSocketService is injectable

    // Using a static ObjectMapper is fine for this, or inject one if configured as a bean elsewhere
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Redis消息的统一处理器. This method name "handleMessage" is configured in RedisConfig's MessageListenerAdapter.
     * @param message 消息内容（JSON字符串 of RedisBroadcastMsg）
     * 简化注释：处理消息
     */
    public void handleMessage(String message) {
        log.debug("Received message from Redis topic: {}", message);
        try {
            RedisBroadcastMsg broadcastMsg = objectMapper.readValue(message, RedisBroadcastMsg.class);
            if (broadcastMsg != null && broadcastMsg.getTargetUserId() != null && broadcastMsg.getMessageBody() != null) {
                // Call WebSocketService to send the message to a locally connected user
                webSocketService.sendLocalMessage(broadcastMsg.getTargetUserId(), broadcastMsg.getMessageBody());
            } else {
                log.warn("Received malformed RedisBroadcastMsg: {}", message);
            }
        } catch (IOException e) {
            log.error("Failed to parse Redis broadcast message: " + message, e);
        }
    }
}
