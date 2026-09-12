package com.web.service;

import com.web.mapper.*;
import com.web.model.*;
import com.web.service.impl.MessageThreadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Security contracts only: the opt-in thread prototype has no production schema. */
class MessageThreadAuthorizationRegressionTest {
    private final MessageThreadMapper threads = mock(MessageThreadMapper.class);
    private final MessageMapper messages = mock(MessageMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final ChatListMapper chats = mock(ChatListMapper.class);
    private final MessageThreadServiceImpl service = new MessageThreadServiceImpl(
            threads, messages, users, new ChatAccessService(chats));
    private MessageThread thread;
    private Message root;

    @BeforeEach
    void setUp() {
        thread = new MessageThread(100L, "Discussion", 1L);
        thread.setId(10L);
        root = new Message();
        root.setId(100L);
        root.setChatId(500L);
        root.setGroupId(50L);
        when(threads.findById(10L)).thenReturn(thread);
        when(messages.selectById(100L)).thenReturn(root);
    }

    @Test
    void creatorAndParticipantCannotReadOrMutateAfterLosingRootChatMembership() {
        assertThrows(AccessDeniedException.class, () -> service.getThreadById(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.getThreadMessages(10L, 1L, 1, 20));
        assertThrows(AccessDeniedException.class, () -> service.getThreadStatistics(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.replyToThread(10L, "reply", 1L));
        assertThrows(AccessDeniedException.class, () -> service.joinThread(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.leaveThread(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.archiveThread(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.closeThread(10L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.pinThread(10L, 1L, true));
        assertThrows(AccessDeniedException.class, () -> service.lockThread(10L, 1L, true));
        verify(threads, never()).getThreadMessages(anyLong(), anyInt(), anyInt());
        verify(threads, never()).getThreadMessageCount(anyLong());
        verify(threads, never()).update(any());
        verify(messages, never()).insert(any(Message.class));
    }

    @Test
    void creationAndUnthreadedContextCannotExposeAnUnrelatedRootMessage() {
        assertThrows(AccessDeniedException.class, () -> service.createThread(100L, "title", 1L));
        assertThrows(AccessDeniedException.class, () -> service.getThreadContext(100L, 1L));
        verify(threads, never()).insert(any());
        verifyNoInteractions(users);
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(true);
        Map<String, Object> context = service.getThreadContext(100L, 1L);
        assertEquals(true, context.get("canCreateThread"));
        assertNull(context.get("thread"));
        assertSame(root, context.get("message"));
    }

    @Test
    void legitimateReplyKeepsRootConversationAndAuthenticatedSender() {
        when(chats.canUserAccessSharedChat(1L, 500L)).thenReturn(true);
        User user = new User();
        user.setUsername("member");
        when(users.selectById(1L)).thenReturn(user);
        Message reply = (Message) service.replyToThread(10L, "reply", 1L).get("message");
        assertEquals(500L, reply.getChatId());
        assertEquals(1L, reply.getSenderId());
        assertEquals(100L, reply.getReplyToMessageId());
        assertEquals(10L, reply.getThreadId());
        verify(messages).insert(reply);
    }

    @Test
    void corruptThreadReferenceCannotJoinMessagesFromDifferentChats() {
        Message reply = new Message();
        reply.setId(200L);
        reply.setChatId(600L);
        reply.setThreadId(10L);
        when(messages.selectById(200L)).thenReturn(reply);
        when(chats.canUserAccessSharedChat(eq(1L), anyLong())).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> service.getThreadContext(200L, 1L));
    }

    @Test
    void listAndCountQueriesBothReceiveAuthenticatedActorBeforePagination() {
        when(threads.getActiveThreads(1L, 20, 20)).thenReturn(List.of(thread));
        when(threads.getActiveThreadCount(1L)).thenReturn(1);
        when(threads.searchThreads(1L, "title", 0, 20)).thenReturn(List.of(thread));
        when(threads.searchThreadCount(1L, "title")).thenReturn(1);
        assertEquals(List.of(thread), service.getActiveThreads(1L, 2, 20).get("threads"));
        assertEquals(List.of(thread), service.searchThreads(1L, "title", 1, 20).get("threads"));
        verify(threads).getActiveThreadCount(1L);
        verify(threads).searchThreadCount(1L, "title");
    }
}
