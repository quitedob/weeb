package com.web.listener;

import com.web.service.WebSocketConnectionService;
import com.web.service.AuthService;
import com.web.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

/**
 * WebSocket事件监听器
 * 监听连接、断开、订阅等事件
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private WebSocketConnectionService connectionService;

    @Autowired
    private AuthService authService;

    /**
     * 处理WebSocket连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal principal = event.getUser() != null ? event.getUser() : headerAccessor.getUser();

            if (principal != null && sessionId != null && !sessionId.isBlank()) {
                String username = principal.getName();
                if (username == null || username.isBlank()) return;
                User user = authService.findByUsername(username);
                if (user == null || user.getId() == null || user.getId() <= 0
                        || !Integer.valueOf(1).equals(user.getStatus()) || !username.equals(user.getUsername())) {
                    log.warn("Ignoring WebSocket connection with an unavailable account: sessionId={}", sessionId);
                    return;
                }
                Long userId = user.getId();

                // 注册连接
                connectionService.registerConnection(sessionId, userId, username);

                log.info("WebSocket连接建立: sessionId={}, userId={}, username={}", 
                        sessionId, userId, username);
            }

        } catch (Exception e) {
            log.error("处理WebSocket连接事件失败", e);
        }
    }

    /**
     * 处理WebSocket断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal principal = headerAccessor.getUser();

            if (sessionId != null) {
                // 注销连接
                connectionService.unregisterConnection(sessionId);

                String username = principal != null ? principal.getName() : "unknown";
                log.info("WebSocket连接断开: sessionId={}, username={}", sessionId, username);
            }

        } catch (Exception e) {
            log.error("处理WebSocket断开事件失败", e);
        }
    }

    /**
     * 处理订阅事件
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String destination = headerAccessor.getDestination();
            Principal principal = headerAccessor.getUser();

            String username = principal != null ? principal.getName() : "unknown";
            log.debug("WebSocket订阅: sessionId={}, username={}, destination={}", 
                    sessionId, username, destination);

        } catch (Exception e) {
            log.error("处理WebSocket订阅事件失败", e);
        }
    }

    /**
     * 处理取消订阅事件
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal principal = headerAccessor.getUser();

            String username = principal != null ? principal.getName() : "unknown";
            log.debug("WebSocket取消订阅: sessionId={}, username={}", sessionId, username);

        } catch (Exception e) {
            log.error("处理WebSocket取消订阅事件失败", e);
        }
    }

}
