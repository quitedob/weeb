package com.web.controller;

import com.web.mapper.ChatListMapper;
import com.web.mapper.MessageMapper;
import com.web.model.ChatList;
import com.web.model.Group;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebSocketMessageRegressionTest {
    private WebSocketMessageController controller;
    private ChatListMapper chatMapper;
    private ChatService chatService;
    private MessageMapper messageMapper;
    private MessageBroadcastService broadcast;
    private UserService users;
    private SimpMessageSendingOperations template;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        controller = new WebSocketMessageController();
        chatMapper = mock(ChatListMapper.class);
        chatService = mock(ChatService.class);
        messageMapper = mock(MessageMapper.class);
        broadcast = mock(MessageBroadcastService.class);
        users = mock(UserService.class);
        template = mock(SimpMessageSendingOperations.class);
        ReflectionTestUtils.setField(controller, "chatService", chatService);
        ReflectionTestUtils.setField(controller, "chatAccessService", new ChatAccessService(chatMapper));
        ReflectionTestUtils.setField(controller, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(controller, "messageBroadcastService", broadcast);
        ReflectionTestUtils.setField(controller, "userService", users);
        ReflectionTestUtils.setField(controller, "messagingTemplate", template);
        ReflectionTestUtils.setField(controller, "deduplicationService", mock(MessageDeduplicationService.class));
        when(users.findByUsername("alice")).thenReturn(user(1L, "alice"));
    }

    @Test
    void privateSendUsesStompPrincipalWithoutThreadLocalSecurityContext() {
        ChatList chat = chat();
        when(chatMapper.selectChatListByIdString("5_1")).thenReturn(chat);
        when(chatMapper.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        when(chatService.sendMessage(eq(1L), eq("5_1"), any())).thenAnswer(call -> call.getArgument(2));
        controller.sendPrivateMessage(Map.of("chatId", "5_1", "content", "hello", "senderId", 999L), () -> "alice");
        verify(chatService).sendMessage(eq(1L), eq("5_1"), argThat(m -> m.getSenderId().equals(1L)));
        verify(broadcast).confirmMessageToSender(any(), eq(1L), isNull());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void groupPayloadRoomResolvesGroupIdToSharedChatId() {
        Group group = new Group();
        group.setId(90L);
        group.setSharedChatId(5L);
        when(chatMapper.selectGroupById(90L)).thenReturn(group);
        when(chatMapper.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        when(chatService.sendMessageBySharedChatId(eq(1L), eq(5L), any())).thenAnswer(call -> call.getArgument(2));
        controller.sendMessage(Map.of("roomId", "group_90", "content", "hello"), () -> "alice");
        verify(chatService).sendMessageBySharedChatId(eq(1L), eq(5L), any());
        verify(broadcast).confirmMessageToSender(any(), eq(1L), isNull());
    }

    @Test
    void forgedPrivateOrGroupPayloadIsDeniedBeforeSaveAndConfirmation() {
        ChatList other = chat();
        other.setUserId(2L);
        when(chatMapper.selectChatListByIdString("5_2")).thenReturn(other);
        assertThrows(AccessDeniedException.class,
                () -> controller.sendPrivateMessage(Map.of("chatId", "5_2", "content", "hello"), () -> "alice"));
        assertThrows(AccessDeniedException.class,
                () -> controller.sendMessage(Map.of("roomId", "group_90", "content", "hello"), () -> "alice"));
        verifyNoInteractions(chatService, broadcast);
    }

    @Test
    void readReceiptPassesObservedBoundaryToTransactionalServiceAndDoesNotPushBeforeCommit() {
        when(chatMapper.selectChatListByIdString("5_1")).thenReturn(chat());
        when(chatMapper.canUserAccessSharedChat(1L, 5L)).thenReturn(true);
        Message message = new Message();
        message.setId(8L);
        message.setSenderId(2L);
        message.setChatId(6L);
        when(chatService.markAsReadBySharedChatId(1L, 5L, 8L)).thenThrow(new AccessDeniedException("foreign boundary"));
        assertThrows(AccessDeniedException.class,
                () -> controller.handleReadReceipt(Map.of("chatId", "5_1", "messageId", 8L), () -> "alice"));
        verifyNoInteractions(template);
        doReturn(Map.of("lastReadMessageId", 8L)).when(chatService).markAsReadBySharedChatId(1L, 5L, 8L);
        when(users.getUserBasicInfo(2L)).thenReturn(user(2L, "bob"));
        controller.handleReadReceipt(Map.of("chatId", "5_1", "messageId", 8L), () -> "alice");
        verify(chatService, times(2)).markAsReadBySharedChatId(1L, 5L, 8L);
        verifyNoInteractions(template);
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }

    private static ChatList chat() {
        ChatList chat = new ChatList();
        chat.setId("5_1");
        chat.setUserId(1L);
        chat.setSharedChatId(5L);
        chat.setTargetId(2L);
        chat.setType("PRIVATE");
        return chat;
    }
}
