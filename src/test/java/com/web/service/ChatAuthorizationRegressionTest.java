package com.web.service;

import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.mapper.MessageReactionMapper;
import com.web.model.ChatList;
import com.web.model.Group;
import com.web.model.Message;
import com.web.service.impl.ChatServiceImpl;
import com.web.vo.message.TextMessageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatAuthorizationRegressionTest {
    private ChatListMapper chats;
    private MessageMapper messages;
    private MessageReactionMapper reactions;
    private MessageBroadcastService broadcast;
    private ChatUnreadCountService unread;
    private ChatAccessService access;
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        chats = mock(ChatListMapper.class);
        messages = mock(MessageMapper.class);
        reactions = mock(MessageReactionMapper.class);
        broadcast = mock(MessageBroadcastService.class);
        unread = mock(ChatUnreadCountService.class);
        access = new ChatAccessService(chats);
        service = new ChatServiceImpl();
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        ReflectionTestUtils.setField(service, "messageMapper", messages);
        ReflectionTestUtils.setField(service, "messageReactionMapper", reactions);
        ReflectionTestUtils.setField(service, "messageBroadcastService", broadcast);
        ReflectionTestUtils.setField(service, "chatUnreadCountService", unread);
        ReflectionTestUtils.setField(service, "chatAccessService", access);
        UserPreferencesService preferences = mock(UserPreferencesService.class);
        when(preferences.canReceiveMessages(anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(service, "preferencesService", preferences);
    }

    @Test
    void outsiderCannotReadOrWriteSharedConversation() {
        assertThrows(AccessDeniedException.class, () -> service.getChatMessagesBySharedChatId(3L, 5L, 1, 20));
        assertThrows(AccessDeniedException.class, () -> service.sendMessageBySharedChatId(3L, 5L, message()));
        verifyNoInteractions(messages, broadcast, unread);
    }

    @Test
    void staleChatListDoesNotLeakLatestMessagesToFormerGroupMembers() {
        when(chats.selectChatListByUserId(1L)).thenReturn(List.of(chat("5_1", 1L, null)));
        assertTrue(service.getChatList(1L).isEmpty());
        verifyNoInteractions(messages);
    }

    @Test
    void participantsCanReadPaginatedHistory() {
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        when(messages.selectMessagesBySharedChatId(5L, 20, 20)).thenReturn(List.of(message()));
        assertEquals(1, service.getChatMessagesBySharedChatId(1L, 5L, 2, 20).size());
        verify(messages).selectMessagesBySharedChatId(5L, 20, 20);
    }

    @Test
    void guessedChatListIdCannotBeUsedByAnotherParticipant() {
        when(chats.selectChatListByIdString("5_2")).thenReturn(chat("5_2", 2L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.sendMessage(1L, "5_2", message()));
        assertThrows(AccessDeniedException.class, () -> service.getChatMessages(1L, "5_2", 1, 20));
        assertThrows(AccessDeniedException.class, () -> service.markAsRead(1L, "5_2"));
        verifyNoInteractions(messages, broadcast, unread);
    }

    @Test
    void bothSendPathsReplaceForgedSenderAndRecipientAndIncrementRecipientOnce() {
        ChatList senderChat = chat("5_1", 1L, 2L);
        when(chats.selectChatListByIdString("5_1")).thenReturn(senderChat);
        when(chats.selectChatListByUserIdAndSharedChatId(1L, 5L)).thenReturn(senderChat);
        when(chats.selectChatListByUserAndTarget(2L, 1L)).thenReturn(chat("5_2", 2L, 1L));
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        for (boolean useSharedId : List.of(false, true)) {
            clearInvocations(messages, broadcast, unread, chats);
            Message forged = message();
            forged.setSenderId(999L);
            forged.setReceiverId(888L);
            forged.setGroupId(777L);
            Message saved = useSharedId ? service.sendMessageBySharedChatId(1L, 5L, forged)
                    : service.sendMessage(1L, "5_1", forged);
            assertEquals(1L, saved.getSenderId());
            assertEquals(2L, saved.getReceiverId());
            assertNull(saved.getGroupId());
            assertEquals(5L, saved.getChatId());
            verify(messages).insertMessage(saved);
            verify(chats).updateLastMessage("5_1", "hello");
            verify(chats, never()).updateLastMessageAndUnreadCount(eq("5_1"), anyString());
            verify(unread).incrementUnreadCount(2L, 5L, 1);
            verify(broadcast).broadcastMessageToReceiver(saved, 2L);
        }
    }

    @Test
    void formerGroupMemberWithStaleChatListCannotSendOrMarkRead() {
        ChatList stale = chat("group-row", 1L, null);
        stale.setType("GROUP");
        stale.setGroupId(90L);
        when(chats.selectChatListByIdString("group-row")).thenReturn(stale);
        when(chats.selectChatListByUserIdAndSharedChatId(1L, 5L)).thenReturn(stale);
        assertThrows(AccessDeniedException.class, () -> service.sendMessage(1L, "group-row", message()));
        assertThrows(AccessDeniedException.class, () -> service.markAsReadBySharedChatId(1L, 5L));
        verifyNoInteractions(messages, broadcast, unread);
    }

    @Test
    void acceptedGroupMemberCanRestoreMissingChatListAndSend() {
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        Group group = new Group();
        group.setId(90L);
        group.setSharedChatId(5L);
        group.setGroupName("Team");
        when(chats.selectGroupBySharedChatId(5L)).thenReturn(group);
        Message saved = service.sendMessageBySharedChatId(1L, 5L, message());
        assertEquals(90L, saved.getGroupId());
        verify(chats).insertChatList(argThat(c -> c.getGroupId().equals(90L) && c.getSharedChatId().equals(5L)));
        verify(broadcast).broadcastMessageToGroup(saved, 90L);
    }

    @Test
    void referencesAndReactionsCannotCrossConversationBoundary() {
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        when(chats.selectChatListByIdString("5_1")).thenReturn(chat("5_1", 1L, 2L));
        Message foreign = message();
        foreign.setChatId(6L);
        when(messages.selectMessageById(99L)).thenReturn(foreign);
        Message reply = message();
        reply.setReplyToMessageId(99L);
        assertThrows(AccessDeniedException.class, () -> service.sendMessage(1L, "5_1", reply));
        assertThrows(AccessDeniedException.class, () -> service.addReaction(1L, 99L, "like"));
        verify(messages, never()).insertMessage(any());
        verifyNoInteractions(reactions, broadcast);
    }

    @Test
    void roomAliasesResolveToAuthoritativeSharedIdAndRejectWildcards() {
        Group group = new Group();
        group.setId(90L);
        group.setSharedChatId(5L);
        when(chats.selectGroupById(90L)).thenReturn(group);
        when(chats.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        when(chats.selectChatListByIdString("5_1")).thenReturn(chat("5_1", 1L, 2L));
        assertEquals(5L, access.resolveRoom(1L, "group_90"));
        assertEquals(5L, access.resolveRoom(1L, "5_1"));
        assertEquals(5L, access.resolveRoom(1L, "5"));
        assertEquals(5L, access.resolveRoom(1L, "private_5"));
        for (String room : List.of("*", "**", "group_90/extra", "private_invalid", "group_91")) {
            assertThrows(AccessDeniedException.class, () -> access.resolveRoom(1L, room));
        }
        when(chats.selectGroupBySharedChatId(5L)).thenReturn(group);
        assertThrows(AccessDeniedException.class, () -> access.resolveRoom(1L, "private_5"));
    }

    private static ChatList chat(String id, Long owner, Long recipient) {
        ChatList chat = new ChatList();
        chat.setId(id);
        chat.setUserId(owner);
        chat.setTargetId(recipient);
        chat.setSharedChatId(5L);
        chat.setType("PRIVATE");
        return chat;
    }

    private static Message message() {
        Message message = new Message();
        message.setMessageType(1);
        TextMessageContent content = new TextMessageContent();
        content.setContent("hello");
        content.setContentType(1);
        message.setContent(content);
        return message;
    }
}
