package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.dto.RedisBroadcastMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Redis message subscriber service
 * Redis subscriber
 */
@Service
public class RedisSubscriber {

    private static final Logger log = LoggerFactory.getLogger(RedisSubscriber.class);

    @Autowired
    private WebSocketService webSocketService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Unified handler for Redis messages. This method name "handleMessage" is configured in RedisConfig's MessageListenerAdapter.
     * @param message Message content (JSON string of RedisBroadcastMsg)
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