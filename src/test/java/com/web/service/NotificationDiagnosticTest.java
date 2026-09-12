package com.web.service;

import com.web.mapper.NotificationMapper;
import com.web.mapper.UserMapper;
import com.web.model.Notification;
import com.web.model.User;
import com.web.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationDiagnosticTest {
    private final NotificationMapper notifications = mock(NotificationMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final UserPreferencesService preferences = mock(UserPreferencesService.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "baseMapper", notifications);
        ReflectionTestUtils.setField(service, "notificationMapper", notifications);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "preferencesService", preferences);
        ReflectionTestUtils.setField(service, "messagingTemplate", messaging);
        User user = new User();
        user.setId(91L);
        user.setUsername("notification-diagnostic");
        when(users.selectById(91L)).thenReturn(user);
        when(preferences.isNotificationEnabled(eq(91L), anyString())).thenReturn(true);
        when(notifications.insert(any(Notification.class))).thenAnswer(call -> {
            ((Notification) call.getArgument(0)).setId(19L);
            return 1;
        });
    }

    @Test
    void intentionalDiagnosticIsStoredAndSentToAuthenticatedUsername() {
        service.createAndPublishNotification(91L, 91L, "TEST_NOTIFICATION", "test", 1L);
        verify(notifications).insert(argThat((Notification n) -> n.getRecipientId().equals(91L)
                && n.getActorId().equals(91L) && "TEST_NOTIFICATION".equals(n.getType())));
        verify(messaging).convertAndSendToUser(eq("notification-diagnostic"), eq("/queue/notifications"), any(Object.class));
    }

    @Test
    void ordinarySelfActivityStillDoesNotCreateOrDeliverNotifications() {
        service.createAndPublishNotification(91L, 91L, "follow", "user", 91L);
        service.createAndPublishNotification(91L, 91L, "like", "article", 5L);
        verifyNoInteractions(notifications, messaging);
    }

    @Test
    void diagnosticDoesNotBypassNotificationPreference() {
        when(preferences.isNotificationEnabled(91L, "TEST_NOTIFICATION")).thenReturn(false);
        service.createAndPublishNotification(91L, 91L, "TEST_NOTIFICATION", "test", 1L);
        verifyNoInteractions(notifications, messaging);
    }
}
