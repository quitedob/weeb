package com.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.ChatListMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.ChatAccessService;
import com.web.service.UserOnlineStatusService;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketOutboundAuthorizationTest {
    private final SpringWebSocketConfig config = new SpringWebSocketConfig();
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final UserMapper users = mock(UserMapper.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final Map<String, Object> attributes = new HashMap<>();
    private final User user = new User();
    private SpringWebSocketConfig.WebSocketAuthInterceptor inbound;
    private SpringWebSocketConfig.WebSocketDeliveryInterceptor outbound;

    @BeforeEach
    void connect() {
        ReflectionTestUtils.setField(config, "jwtUtil", jwt);
        ReflectionTestUtils.setField(config, "userMapper", users);
        ReflectionTestUtils.setField(config, "onlineStatusService", mock(UserOnlineStatusService.class));
        ReflectionTestUtils.setField(config, "chatAccessService", new ChatAccessService(chats));
        ReflectionTestUtils.setField(config, "objectMapper", new ObjectMapper());
        user.setId(1L);
        user.setUsername("alice");
        user.setStatus(1);
        when(jwt.validateToken("session-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("session-token")).thenReturn(1L);
        when(jwt.extractUsername("session-token")).thenReturn("alice");
        when(users.selectById(1L)).thenReturn(user);
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        inbound = config.new WebSocketAuthInterceptor();
        outbound = config.new WebSocketDeliveryInterceptor();
        var connect = frame(StompCommand.CONNECT, null, null);
        connect.setNativeHeader("Authorization", "Bearer session-token");
        send(connect);
    }

    @Test
    void rewrittenUserQueueDeliversWhileValidAndStopsAfterPassiveRevocationOrExpiry() {
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/private", "private-sub"));
        Message<byte[]> push = delivery("session-1", "private-sub", "/queue/private-usersession-1", "{\"chatId\":\"5\",\"content\":\"secret\"}");
        assertSame(push, outbound.preSend(push, null));
        // Logout, account-wide revocation and token expiry all make validateToken false.
        when(jwt.validateToken("session-token")).thenReturn(false);
        assertNull(outbound.preSend(push, null), "No subsequent inbound frame is required to stop delivery");
        when(jwt.validateToken("session-token")).thenReturn(true);
        user.setStatus(0);
        assertNull(outbound.preSend(push, null));
    }

    @Test
    void kickedMemberStopsReceivingExistingRoomSubscriptionsAndQueuedGroupPayloads() {
        send(frame(StompCommand.SUBSCRIBE, "/topic/chat/5", "room-sub"));
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/private", "private-sub"));
        var event = delivery("session-1", "room-sub", "/topic/chat/5", "{\"type\":\"typing\"}");
        var queuedGroupMessage = delivery("session-1", "private-sub", "/queue/private-usersession-1", "{\"chatId\":5}");
        assertSame(event, outbound.preSend(event, null));
        assertSame(queuedGroupMessage, outbound.preSend(queuedGroupMessage, null));
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(false);
        assertNull(outbound.preSend(event, null));
        assertNull(outbound.preSend(queuedGroupMessage, null));
    }

    @Test
    void deliveryRequiresTheRegisteredSessionSubscriptionAndPayloadConversation() {
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/private", "private-sub"));
        assertNull(outbound.preSend(delivery("other-session", "private-sub", "/queue/private-userother", "{\"chatId\":5}"), null));
        assertNull(outbound.preSend(delivery("session-1", "unknown", "/queue/private-usersession-1", "{\"chatId\":5}"), null));
        assertNull(outbound.preSend(delivery("session-1", "private-sub", "/queue/private-usersession-1", "{\"chatId\":999}"), null));
        assertNull(outbound.preSend(delivery("session-1", "private-sub", "/queue/private-usersession-1", "{}"), null));
    }

    @Test
    void ordinaryGroupUpdatesRecheckMembershipWhileTerminalNoticesRemainDeliverable() {
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/group-info-change", "info"));
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/group-member-change", "members"));
        var group = new com.web.model.Group(); group.setId(9L); group.setSharedChatId(5L);
        when(chats.selectGroupById(9L)).thenReturn(group);
        var info = delivery("session-1", "info", "/queue/group-info-change-usersession-1",
                "{\"type\":\"GROUP_INFO_CHANGE\",\"groupId\":9,\"changeType\":\"INFO_UPDATED\",\"newGroupDescription\":\"private notes\"}");
        var member = delivery("session-1", "members", "/queue/group-member-change-usersession-1",
                "{\"type\":\"GROUP_MEMBER_CHANGE\",\"groupId\":9,\"changeType\":\"ROLE_CHANGED\",\"affectedUserId\":2}");
        assertSame(info, outbound.preSend(info, null));
        assertSame(member, outbound.preSend(member, null));
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(false);
        assertNull(outbound.preSend(info, null));
        assertNull(outbound.preSend(member, null));
        for (String change : new String[] {"MEMBER_REMOVED", "MEMBER_LEFT"}) {
            var ownRemoval = delivery("session-1", "members", "/queue/group-member-change-usersession-1",
                    "{\"type\":\"GROUP_MEMBER_CHANGE\",\"groupId\":9,\"changeType\":\"" + change + "\",\"affectedUserId\":1}");
            assertSame(ownRemoval, outbound.preSend(ownRemoval, null));
        }
        var otherRemoval = delivery("session-1", "members", "/queue/group-member-change-usersession-1",
                "{\"type\":\"GROUP_MEMBER_CHANGE\",\"groupId\":9,\"changeType\":\"MEMBER_REMOVED\",\"affectedUserId\":2}");
        assertNull(outbound.preSend(otherRemoval, null));
        when(chats.selectGroupById(9L)).thenReturn(null);
        var dissolved = delivery("session-1", "info", "/queue/group-info-change-usersession-1",
                "{\"type\":\"GROUP_INFO_CHANGE\",\"groupId\":9,\"changeType\":\"GROUP_DISSOLVED\"}");
        assertSame(dissolved, outbound.preSend(dissolved, null));
        user.setStatus(0);
        assertNull(outbound.preSend(dissolved, null), "Terminal events still require an enabled authenticated account");
    }

    @Test
    void unsubscribeAndDisconnectRemoveDeliveryRightsButProtocolFramesRemainUsable() {
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", "events"));
        var push = delivery("session-1", "events", "/queue/notifications-usersession-1", "{}");
        assertSame(push, outbound.preSend(push, null));
        send(frame(StompCommand.UNSUBSCRIBE, null, "events"));
        assertNull(outbound.preSend(push, null));
        send(frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", "events"));
        config.forgetDisconnectedSession(new SessionDisconnectEvent(this, push, "session-1", CloseStatus.NORMAL));
        assertNull(outbound.preSend(push, null));
        for (SimpMessageType type : new SimpMessageType[] {SimpMessageType.CONNECT_ACK, SimpMessageType.DISCONNECT_ACK, SimpMessageType.HEARTBEAT}) {
            var headers = SimpMessageHeaderAccessor.create(type);
            var protocol = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            assertSame(protocol, outbound.preSend(protocol, null));
        }
    }

    private StompHeaderAccessor frame(StompCommand command, String destination, String subscription) {
        var headers = StompHeaderAccessor.create(command);
        headers.setSessionId("session-1");
        headers.setSessionAttributes(attributes);
        headers.setUser(() -> "alice");
        headers.setDestination(destination);
        headers.setSubscriptionId(subscription);
        headers.setLeaveMutable(true);
        return headers;
    }

    private void send(StompHeaderAccessor headers) {
        inbound.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }

    private Message<byte[]> delivery(String session, String subscription, String destination, String json) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(session);
        headers.setSubscriptionId(subscription);
        headers.setDestination(destination);
        return MessageBuilder.createMessage(json.getBytes(StandardCharsets.UTF_8), headers.getMessageHeaders());
    }
}
