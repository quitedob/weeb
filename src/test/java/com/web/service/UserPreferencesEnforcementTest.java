package com.web.service;

import com.web.controller.UserController;
import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.mapper.NotificationMapper;
import com.web.mapper.UserMapper;
import com.web.mapper.UserPreferencesMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.model.Notification;
import com.web.model.User;
import com.web.model.UserPreferences;
import com.web.security.SecurityUtils;
import com.web.service.impl.ChatServiceImpl;
import com.web.service.impl.NotificationServiceImpl;
import com.web.vo.message.TextMessageContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserPreferencesEnforcementTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothPrivateSendPathsRejectThePersistedRecipientBeforeWriting(boolean useSharedId) {
        ChatFixture fixture = new ChatFixture();
        fixture.recipientPreferences.setAllowMessages(false);
        Message submitted = message();
        submitted.setReceiverId(999L);

        assertThrows(AccessDeniedException.class, () -> fixture.send(useSharedId, submitted));

        verify(fixture.preferenceMapper).findByUserId(2L);
        verify(fixture.preferenceMapper, never()).findByUserId(999L);
        verify(fixture.messages, never()).insertMessage(any());
        verifyNoInteractions(fixture.outbox, fixture.broadcast, fixture.unread);
        verify(fixture.chats, never()).insertChatList(any());
        verify(fixture.chats, never()).updateLastMessage(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothSendPathsStillDeliverWhenTheRecipientAllowsMessages(boolean useSharedId) {
        ChatFixture fixture = new ChatFixture();
        Message submitted = message();
        submitted.setSenderId(999L);
        submitted.setReceiverId(888L);

        Message saved = fixture.send(useSharedId, submitted);

        assertEquals(1L, saved.getSenderId());
        assertEquals(2L, saved.getReceiverId());
        verify(fixture.messages).insertMessage(saved);
        verify(fixture.outbox).enqueueMessage(saved, List.of(1L, 2L));
        verifyNoInteractions(fixture.broadcast);
        verify(fixture.unread).incrementUnreadCount(2L, 42L, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "follow", "comment", "GROUP_APPLICATION", "NEW_MESSAGE"})
    void disabledNotificationCategoriesDoNotPersistOrPublish(String type) {
        NotificationFixture fixture = new NotificationFixture();
        fixture.service.createAndPublishNotification(2L, 1L, type, "article", 100L);
        verifyNoInteractions(fixture.notifications, fixture.messaging, fixture.users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FRIEND_REQUEST", "TEST_NOTIFICATION"})
    void contactAndSystemNotificationsStillPersistAndReachTheUsernameQueue(String type) {
        NotificationFixture fixture = new NotificationFixture();
        boolean contact = type.equals("FRIEND_REQUEST");
        fixture.service.createAndPublishNotification(2L, 1L, type, contact ? "CONTACT" : "SYSTEM", 100L);

        verify(fixture.notifications).insert(argThat((Notification item) -> item.getRecipientId().equals(2L)
                && item.getActorId().equals(1L) && item.getType().equals(type)));
        verify(fixture.messaging).convertAndSendToUser(eq("recipient"), eq("/queue/notifications"), any(Map.class));
        if (contact) {
            verify(fixture.messaging).convertAndSendToUser(eq("recipient"), eq("/queue/contacts"), any(Map.class));
        } else {
            verify(fixture.messaging, never()).convertAndSendToUser(anyString(), eq("/queue/contacts"), any(Map.class));
        }
    }

    @Test
    void hiddenFollowingListRejectsAnotherViewerButRemainsAvailableToItsOwner() {
        Object previousUserService = ReflectionTestUtils.getField(SecurityUtils.class, "userService");
        Object previousTypeService = ReflectionTestUtils.getField(SecurityUtils.class, "userTypeSecurityService");
        var previousContext = SecurityContextHolder.getContext();
        try {
            UserService users = mock(UserService.class);
            when(users.findByUsername("viewer")).thenReturn(user(1L, "viewer"));
            when(users.findByUsername("owner")).thenReturn(user(2L, "owner"));
            new SecurityUtils(users, mock(UserTypeSecurityService.class));

            UserPreferencesMapper preferences = mock(UserPreferencesMapper.class);
            UserPreferences stored = new UserPreferences();
            stored.setShowFollows(false);
            when(preferences.findByUserId(2L)).thenReturn(stored);
            UserFollowService follows = mock(UserFollowService.class);
            Map<String, Object> list = Map.of("list", List.of(user(3L, "followed")), "total", 1);
            when(follows.getFollowingList(2L, 1, 20)).thenReturn(list);
            UserController controller = new UserController();
            ReflectionTestUtils.setField(controller, "preferencesService", new UserPreferencesService(preferences));
            ReflectionTestUtils.setField(controller, "userFollowService", follows);

            authenticate("viewer");
            assertEquals(403, controller.getUserFollowing(2L, 1, 20).getStatusCode().value());
            verifyNoInteractions(follows);

            authenticate("owner");
            var response = controller.getUserFollowing(2L, 1, 20);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().getCode());
            assertEquals(list, response.getBody().getData());
        } finally {
            SecurityContextHolder.setContext(previousContext);
            ReflectionTestUtils.setField(SecurityUtils.class, "userService", previousUserService);
            ReflectionTestUtils.setField(SecurityUtils.class, "userTypeSecurityService", previousTypeService);
        }
    }

    private static void authenticate(String username) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(username, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Message message() {
        Message message = new Message();
        message.setMessageType(1);
        TextMessageContent content = new TextMessageContent();
        content.setContent("hello");
        message.setContent(content);
        return message;
    }

    private static ChatList chat(String id, Long owner, Long recipient) {
        ChatList chat = new ChatList();
        chat.setId(id);
        chat.setUserId(owner);
        chat.setTargetId(recipient);
        chat.setSharedChatId(42L);
        chat.setType("PRIVATE");
        return chat;
    }

    private static class ChatFixture {
        final ChatListMapper chats = mock(ChatListMapper.class);
        final MessageMapper messages = mock(MessageMapper.class);
        final MessageBroadcastService broadcast = mock(MessageBroadcastService.class);
        final MessageOutboxService outbox = mock(MessageOutboxService.class);
        final ChatUnreadCountService unread = mock(ChatUnreadCountService.class);
        final UserPreferencesMapper preferenceMapper = mock(UserPreferencesMapper.class);
        final UserPreferences recipientPreferences = new UserPreferences();
        final ChatServiceImpl service = new ChatServiceImpl();

        ChatFixture() {
            when(messages.selectCurrentRecipients(42L)).thenReturn(List.of(1L, 2L));
            when(preferenceMapper.findByUserId(2L)).thenReturn(recipientPreferences);
            ChatList sender = chat("42_1", 1L, 2L);
            when(chats.selectChatListByIdString("42_1")).thenReturn(sender);
            when(chats.selectChatListByUserIdAndSharedChatId(1L, 42L)).thenReturn(sender);
            when(chats.selectChatListByUserAndTarget(2L, 1L)).thenReturn(chat("42_2", 2L, 1L));
            when(chats.canUserAccessSharedChat(1L, 42L)).thenReturn(true);
            ReflectionTestUtils.setField(service, "chatListMapper", chats);
            ReflectionTestUtils.setField(service, "messageMapper", messages);
            ReflectionTestUtils.setField(service, "messageBroadcastService", broadcast);
            ReflectionTestUtils.setField(service, "messageOutboxService", outbox);
            ReflectionTestUtils.setField(service, "chatUnreadCountService", unread);
            ReflectionTestUtils.setField(service, "chatAccessService", new ChatAccessService(chats));
            ReflectionTestUtils.setField(service, "preferencesService", new UserPreferencesService(preferenceMapper));
        }

        Message send(boolean useSharedId, Message message) {
            return useSharedId ? service.sendMessageBySharedChatId(1L, 42L, message)
                    : service.sendMessage(1L, "42_1", message);
        }
    }

    private static class NotificationFixture {
        final NotificationMapper notifications = mock(NotificationMapper.class);
        final UserMapper users = mock(UserMapper.class);
        final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        final NotificationServiceImpl service = new NotificationServiceImpl();

        NotificationFixture() {
            UserPreferencesMapper preferences = mock(UserPreferencesMapper.class);
            UserPreferences stored = new UserPreferences();
            stored.setNewMessages(false);
            stored.setFollows(false);
            stored.setLikes(false);
            stored.setComments(false);
            stored.setGroupInvites(false);
            when(preferences.findByUserId(2L)).thenReturn(stored);
            when(users.selectById(2L)).thenReturn(user(2L, "recipient"));
            when(users.selectById(1L)).thenReturn(user(1L, "actor"));
            when(notifications.insert(any(Notification.class))).thenReturn(1);
            ReflectionTestUtils.setField(service, "baseMapper", notifications);
            ReflectionTestUtils.setField(service, "notificationMapper", notifications);
            ReflectionTestUtils.setField(service, "userMapper", users);
            ReflectionTestUtils.setField(service, "messagingTemplate", messaging);
            ReflectionTestUtils.setField(service, "preferencesService", new UserPreferencesService(preferences));
        }
    }
}
