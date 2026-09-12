package com.web.service;

import com.web.mapper.*;
import com.web.model.*;
import com.web.service.impl.UnifiedMessageServiceImpl;
import com.web.vo.message.SendMessageVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlternativeMessageAuthorizationRegressionTest {
    private final MessageMapper messages = mock(MessageMapper.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final GroupMapper groups = mock(GroupMapper.class);
    private final GroupMemberMapper members = mock(GroupMemberMapper.class);
    private final ChatService chatService = mock(ChatService.class);
    private final UserService users = mock(UserService.class);
    private final UserPreferencesService preferences = mock(UserPreferencesService.class);
    private final MessageCacheService cache = mock(MessageCacheService.class);
    private final MessageRetryService retries = mock(MessageRetryService.class);
    private final UnifiedMessageServiceImpl service = new UnifiedMessageServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "messageMapper", messages);
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        ReflectionTestUtils.setField(service, "groupMapper", groups);
        ReflectionTestUtils.setField(service, "groupMemberMapper", members);
        ReflectionTestUtils.setField(service, "chatService", chatService);
        ReflectionTestUtils.setField(service, "chatAccessService", new ChatAccessService(chats));
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "userPreferencesService", preferences);
        ReflectionTestUtils.setField(service, "messageCacheService", cache);
        ReflectionTestUtils.setField(service, "messageRetryService", retries);
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        when(users.getUserBasicInfo(1L)).thenReturn(user);
    }

    @Test
    void senderAndGroupRoleCannotBypassRevokedMembershipForReadOrMutation() {
        Message message = message(10L, 500L, 1L);
        message.setGroupId(50L);
        when(messages.selectById(10L)).thenReturn(message);
        assertFalse(service.hasMessagePermission(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.getMessageById(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.deleteMessage(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.recallMessage(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.markMessageAsRead(10L, 1L));
        verify(messages, never()).updateById(any(Message.class));
        verifyNoInteractions(chatService, cache);
    }

    @Test
    void acceptedGroupHistoryAndSendUseSharedChatIdentityAndRecheckBeforeCache() {
        Group group = new Group();
        group.setId(50L);
        group.setSharedChatId(500L);
        when(groups.selectById(50L)).thenReturn(group);
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(true);
        Message saved = message(10L, 500L, 1L);
        when(chatService.sendMessageBySharedChatId(eq(1L), eq(500L), any())).thenReturn(saved);
        when(cache.getCachedMessageList(500L, 1, 20)).thenReturn(List.of(saved));
        assertSame(saved, service.sendGroupMessage(50L, "hello", 1L));
        assertEquals(List.of(saved), service.getGroupMessageHistory(50L, 1L, 1, 20));
        verify(chatService).sendMessageBySharedChatId(eq(1L), eq(500L), argThat(m -> m.getSenderId().equals(1L)));
        clearInvocations(cache, chatService);
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.getGroupMessageHistory(50L, 1L, 1, 20));
        assertThrows(AccessDeniedException.class, () -> service.sendGroupMessage(50L, "hello", 1L));
        verifyNoInteractions(cache, chatService);
    }

    @Test
    void privateChatUsesOpaqueOwnedChatIdAndRecipientPrivacyBlocksBeforeWriteAndRetry() {
        SendMessageVo request = new SendMessageVo();
        request.setTargetType("PRIVATE");
        request.setTargetId(2L);
        request.setContent("hello");
        assertThrows(AccessDeniedException.class, () -> service.sendMessage(request, 1L));
        verifyNoInteractions(chatService, messages, retries, cache);

        when(preferences.canReceiveMessages(2L)).thenReturn(true);
        ChatList chat = new ChatList();
        chat.setId("500_1");
        chat.setSharedChatId(500L);
        when(chatService.createChat(1L, 2L)).thenReturn(chat);
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(true);
        Message sent = message(10L, 500L, 1L);
        when(chatService.sendMessage(eq(1L), eq("500_1"), any())).thenReturn(sent);
        assertSame(sent, service.sendPrivateMessage(2L, "hello", 1L));
        verify(chatService).sendMessage(eq(1L), eq("500_1"), any());
    }

    @Test
    void memberMayReadButOnlyOriginalSenderMayEditMessage() {
        when(messages.selectById(10L)).thenReturn(message(10L, 500L, 2L));
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(true);
        assertTrue(service.hasMessagePermission(10L, 1L));
        assertFalse(service.deleteMessage(10L, 1L));
        assertFalse(service.recallMessage(10L, 1L));
        verify(messages, never()).updateById(any(Message.class));
        verifyNoInteractions(chatService);
    }

    @Test
    void staleConversationListDoesNotCauseGlobalMessageQuery() {
        ChatList chat = new ChatList();
        chat.setSharedChatId(500L);
        when(chatService.getChatList(1L)).thenReturn(List.of(chat));
        when(members.selectList(any())).thenReturn(List.of());
        assertEquals(List.of(), service.getUnifiedMessageList(1L, 1, 20).get("messages"));
        assertEquals(0L, service.searchMessages(1L, "secret", 1, 20).get("total"));
        verifyNoInteractions(messages);
    }

    private Message message(Long id, Long sharedId, Long sender) {
        Message message = new Message();
        message.setId(id);
        message.setChatId(sharedId);
        message.setSenderId(sender);
        return message;
    }
}
