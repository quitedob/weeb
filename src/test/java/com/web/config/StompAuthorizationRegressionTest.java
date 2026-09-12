package com.web.config;

import com.web.mapper.ChatListMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.ChatAccessService;
import com.web.service.UserOnlineStatusService;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompAuthorizationRegressionTest {
    private SpringWebSocketConfig config;
    private SpringWebSocketConfig.WebSocketAuthInterceptor interceptor;
    private JwtUtil jwt;
    private ChatListMapper chats;
    private User user;
    private Map<String, Object> session;

    @BeforeEach
    void setUp() {
        config = new SpringWebSocketConfig();
        jwt = mock(JwtUtil.class);
        UserMapper users = mock(UserMapper.class);
        chats = mock(ChatListMapper.class);
        ReflectionTestUtils.setField(config, "jwtUtil", jwt);
        ReflectionTestUtils.setField(config, "userMapper", users);
        ReflectionTestUtils.setField(config, "onlineStatusService", mock(UserOnlineStatusService.class));
        ReflectionTestUtils.setField(config, "chatAccessService", new ChatAccessService(chats));
        interceptor = config.new WebSocketAuthInterceptor();
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setStatus(1);
        when(jwt.validateToken("valid-access")).thenReturn(true);
        when(jwt.getUserIdFromToken("valid-access")).thenReturn(1L);
        when(jwt.extractUsername("valid-access")).thenReturn("alice");
        when(users.selectById(1L)).thenReturn(user);
        session = new HashMap<>();
    }

    @Test
    void connectUsesVerifiedIdentityAndRejectsMissingInvalidOrBannedAuthentication() {
        StompHeaderAccessor headers = frame(StompCommand.CONNECT, null, null);
        assertThrows(AccessDeniedException.class, () -> send(headers));
        headers.setNativeHeader("Authorization", "Bearer reset-token");
        assertThrows(AccessDeniedException.class, () -> send(headers));
        headers.setNativeHeader("Authorization", "Bearer valid-access");
        send(headers);
        assertEquals("alice", headers.getUser().getName());
        assertEquals(1L, session.get("userId"));
        user.setStatus(0);
        assertThrows(AccessDeniedException.class, () -> send(headers));
    }

    @Test
    void authenticatedUsersCanSubscribeToTheirStandardPrivateQueues() {
        connect();
        for (String queue : new String[] {"private", "chat-list-update", "message-status", "read-receipt",
                "group-member-change", "group-info-change", "reaction-change", "errors", "contacts", "notifications"}) {
            assertDoesNotThrow(() -> send(frame(StompCommand.SUBSCRIBE, "/user/queue/" + queue, () -> "alice")));
        }
    }

    @Test
    void arbitraryBrokerQueuesOtherUsersAndWildcardSubscriptionsAreDenied() {
        connect();
        for (String target : new String[] {"/user/bob/queue/private", "/user/alice/queue/private",
                "/queue/private-user-other-session", "/topic/group.9", "/topic/chat/*", "/topic/chat/**",
                "/topic/chat/5/extra", "/user/queue/*", "/app/unknown"}) {
            assertThrows(AccessDeniedException.class,
                    () -> send(frame(StompCommand.SUBSCRIBE, target, () -> "alice")), target);
        }
    }

    @Test
    void exactChatRoomsRequireMembershipAndRevocationIsRechecked() {
        connect();
        StompHeaderAccessor subscription = frame(StompCommand.SUBSCRIBE, "/topic/chat/5", () -> "alice");
        assertThrows(AccessDeniedException.class, () -> send(subscription));
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        assertDoesNotThrow(() -> send(subscription));
        when(jwt.validateToken("valid-access")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> send(subscription));
    }

    @Test
    void clientCannotPublishDirectlyToBrokerOrSendBeforeConnect() {
        assertThrows(AccessDeniedException.class,
                () -> send(frame(StompCommand.SEND, "/app/chat/private", () -> "alice")));
        connect();
        for (String target : new String[] {"/topic/chat/5", "/queue/private", "/user/bob/queue/private", "/app/unknown"}) {
            assertThrows(AccessDeniedException.class, () -> send(frame(StompCommand.SEND, target, () -> "alice")));
        }
        assertDoesNotThrow(() -> send(frame(StompCommand.SEND, "/app/chat/private", () -> "alice")));
        assertDoesNotThrow(() -> send(frame(StompCommand.SEND, "/app/chat.sendMessage", () -> "alice")));
        assertThrows(AccessDeniedException.class,
                () -> send(frame(StompCommand.SEND, "/app/chat/typing/5", () -> "alice")));
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        assertDoesNotThrow(() -> send(frame(StompCommand.SEND, "/app/chat/typing/5", () -> "alice")));
        assertThrows(AccessDeniedException.class,
                () -> send(frame(StompCommand.SEND, "/app/chat/private", () -> "bob")));
    }

    private void connect() {
        StompHeaderAccessor headers = frame(StompCommand.CONNECT, null, null);
        headers.setNativeHeader("Authorization", "Bearer valid-access");
        send(headers);
    }

    private StompHeaderAccessor frame(StompCommand command, String destination, Principal principal) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setSessionId("test-session");
        headers.setSessionAttributes(session);
        headers.setDestination(destination);
        headers.setUser(principal);
        headers.setLeaveMutable(true);
        return headers;
    }

    private Message<?> send(StompHeaderAccessor headers) {
        return interceptor.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }
}
