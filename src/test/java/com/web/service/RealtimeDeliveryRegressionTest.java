package com.web.service;

import com.web.mapper.NotificationMapper;
import com.web.mapper.UserMapper;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealtimeDeliveryRegressionTest {
    private SimpMessagingTemplate template;
    private UserService users;
    private UserOnlineStatusService online;
    private ChatUnreadCountService unread;
    private JdbcTemplate jdbc;
    private MessageBroadcastService broadcast;

    @BeforeEach
    void setUp() {
        template = mock(SimpMessagingTemplate.class);
        users = mock(UserService.class);
        online = mock(UserOnlineStatusService.class);
        unread = mock(ChatUnreadCountService.class);
        jdbc = mock(JdbcTemplate.class);
        broadcast = new MessageBroadcastService();
        ReflectionTestUtils.setField(broadcast, "messagingTemplate", template);
        ReflectionTestUtils.setField(broadcast, "userService", users);
        ReflectionTestUtils.setField(broadcast, "onlineStatusService", online);
        ReflectionTestUtils.setField(broadcast, "chatUnreadCountService", unread);
        ReflectionTestUtils.setField(broadcast, "jdbcTemplate", jdbc);
        when(users.getUserBasicInfo(1L)).thenReturn(user(1L, "alice"));
        when(users.getUserBasicInfo(2L)).thenReturn(user(2L, "bob"));
        when(users.getUserBasicInfo(3L)).thenReturn(user(3L, "charlie"));
    }

    @Test
    void privateBroadcastUsesUsernameAndDoesNotDoubleCountOfflineMessages() {
        broadcast.broadcastMessageToReceiver(message(), 2L);
        verifyNoInteractions(unread);
        when(online.isUserOnline(2L)).thenReturn(true);
        broadcast.broadcastMessageToReceiver(message(), 2L);
        verify(template).convertAndSendToUser(eq("bob"), eq("/queue/private"), any(Object.class));
        verifyNoInteractions(unread);
    }

    @Test
    void groupDeliveryCountsEachRecipientOnceWithSharedIdRegardlessOfPresence() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(90L))).thenReturn(List.of(1L, 2L, 3L));
        when(online.isUserOnline(2L)).thenReturn(true);
        broadcast.broadcastMessageToGroup(message(), 90L);
        verify(unread).incrementUnreadCount(2L, 5L, 1);
        verify(unread).incrementUnreadCount(3L, 5L, 1);
        verifyNoMoreInteractions(unread);
        verify(template).convertAndSendToUser(eq("bob"), eq("/queue/private"), any(Object.class));
        verify(template, never()).convertAndSendToUser(eq("alice"), anyString(), any(Object.class));
        verify(jdbc).queryForList(contains("kicked_at IS NULL"), eq(Long.class), eq(90L));
    }

    @Test
    void notificationAndContactQueuesUseSameUsernameAsSocketPrincipal() {
        UserMapper mapper = mock(UserMapper.class);
        NotificationMapper notifications = mock(NotificationMapper.class);
        when(mapper.selectById(1L)).thenReturn(user(1L, "alice"));
        when(mapper.selectById(2L)).thenReturn(user(2L, "bob"));
        NotificationServiceImpl service = new NotificationServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "baseMapper", notifications);
        ReflectionTestUtils.setField(service, "messagingTemplate", template);
        UserPreferencesService preferences = mock(UserPreferencesService.class);
        when(preferences.isNotificationEnabled(anyLong(), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(service, "preferencesService", preferences);
        service.createAndPublishNotification(2L, 1L, "request", "CONTACT", 8L);
        verify(template).convertAndSendToUser(eq("bob"), eq("/queue/notifications"), any(Object.class));
        verify(template).convertAndSendToUser(eq("bob"), eq("/queue/contacts"), any(Object.class));
    }

    @Test
    void redisFanoutResolvesRecipientUsername() {
        RedisSubscriber subscriber = new RedisSubscriber();
        ReflectionTestUtils.setField(subscriber, "messagingTemplate", template);
        ReflectionTestUtils.setField(subscriber, "userService", users);
        subscriber.handleMessage("{\"targetUserId\":2,\"messageBody\":\"hello\"}");
        verify(template).convertAndSendToUser("bob", "/queue/redis", "hello");
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }

    private static Message message() {
        Message message = new Message();
        message.setId(8L);
        message.setSenderId(1L);
        message.setChatId(5L);
        message.setMessageType(1);
        return message;
    }
}
