package com.web.controller;

import com.web.annotation.Userid;
import com.web.mapper.ChatListMapper;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageReliabilityControllerTest {
    private ChatService service;
    private MockMvc mvc;
    private WebSocketMessageController socket;
    private SimpMessageSendingOperations transport;

    @BeforeEach
    void actualAdaptersWithAuthenticatedActor() {
        service = mock(ChatService.class);
        ChatController controller = new ChatController();
        ReflectionTestUtils.setField(controller, "chatService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter parameter) { return parameter.hasParameterAnnotation(Userid.class); }
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                          NativeWebRequest request, WebDataBinderFactory binder) { return 7L; }
        }).build();
        socket = new WebSocketMessageController();
        UserService users = mock(UserService.class);
        User actor = new User(); actor.setId(7L); actor.setUsername("actor"); actor.setStatus(1);
        when(users.findByUsername("actor")).thenReturn(actor);
        ChatListMapper chats = mock(ChatListMapper.class);
        when(chats.canUserAccessSharedChat(7L, 55L)).thenReturn(true);
        transport = mock(SimpMessageSendingOperations.class);
        ReflectionTestUtils.setField(socket, "chatService", service);
        ReflectionTestUtils.setField(socket, "userService", users);
        ReflectionTestUtils.setField(socket, "chatAccessService", new ChatAccessService(chats));
        ReflectionTestUtils.setField(socket, "messageBroadcastService", mock(MessageBroadcastService.class));
        ReflectionTestUtils.setField(socket, "messagingTemplate", transport);
        when(service.sendMessageBySharedChatId(eq(7L), eq(55L), any())).thenAnswer(call -> {
            Message message = call.getArgument(2); message.setId(101L); return message;
        });
    }

    @Test
    void httpAndBothSocketRoutesPreserveSameCanonicalPayloadAndClientKey() throws Exception {
        mvc.perform(post("/api/chats/55/messages").contentType(MediaType.APPLICATION_JSON).content("""
                {"senderId":999,"clientMessageId":"retry-key","messageType":1,"replyToMessageId":90,
                 "content":{"content":"hello","contentType":1,"url":"/owned.png","atUidList":[8]}}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.clientMessageId").value("retry-key"));
        Map<String, Object> payload = Map.of("sharedChatId", "55", "chatId", "55", "roomId", "group_999",
                "senderId", 999, "clientMessageId", "retry-key", "replyToMessageId", 90,
                "content", Map.of("content", "hello", "contentType", 1, "url", "/owned.png", "atUidList", List.of(8)));
        socket.sendPrivateMessage(payload, () -> "actor");
        socket.sendMessage(payload, () -> "actor");
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(service, times(3)).sendMessageBySharedChatId(eq(7L), eq(55L), messages.capture());
        Message http = messages.getAllValues().get(0);
        for (Message message : messages.getAllValues()) {
            assertEquals(7L, message.getSenderId());
            assertEquals("retry-key", message.getClientMessageId());
            assertEquals(90L, message.getReplyToMessageId());
            assertEquals(http.getContent(), message.getContent());
        }
        verify(service, never()).sendMessage(anyLong(), anyString(), any());
    }

    @Test
    void httpReadPassesObservedBoundaryAndPreservesLegacyEmptyBody() throws Exception {
        when(service.markAsReadBySharedChatId(7L, 55L, 101L)).thenReturn(Map.of("lastReadMessageId", 101L, "unreadCount", 2));
        when(service.markAsReadBySharedChatId(7L, 55L)).thenReturn(true);
        mvc.perform(post("/api/chats/55/read").contentType(MediaType.APPLICATION_JSON).content("{\"lastReadMessageId\":101}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.lastReadMessageId").value(101))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
        mvc.perform(post("/api/chats/55/read")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(service).markAsReadBySharedChatId(7L, 55L, 101L);
        verify(service).markAsReadBySharedChatId(7L, 55L);
    }

    @Test
    void targetStateReactionRoutesDoNotInvokeLegacyToggle() throws Exception {
        when(service.setReaction(7L, 101L, "thumb", true)).thenReturn(Map.of("reactionVersion", 2L, "reactions", List.of()));
        when(service.setReaction(7L, 101L, "thumb", false)).thenReturn(Map.of("reactionVersion", 3L, "reactions", List.of()));
        mvc.perform(put("/api/chats/messages/101/react").param("reactionType", "thumb"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reactionVersion").value(2));
        mvc.perform(delete("/api/chats/messages/101/react").param("reactionType", "thumb"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reactionVersion").value(3));
        verify(service).setReaction(7L, 101L, "thumb", true);
        verify(service).setReaction(7L, 101L, "thumb", false);
        verify(service, never()).addReaction(anyLong(), anyLong(), anyString());
    }

    @Test
    void cursorSyncUsesAuthenticatedActorAndExplicitAscendingBoundary() throws Exception {
        when(service.syncMessages(7L, 55L, 101L, 12)).thenReturn(Map.of("list", List.of(), "nextAfterMessageId", 101L, "hasMore", false));
        mvc.perform(get("/api/chats/55/sync").param("afterMessageId", "101").param("size", "12").param("userId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nextAfterMessageId").value(101));
        verify(service).syncMessages(7L, 55L, 101L, 12);
    }

    @Test
    void existingMessageRefreshPassesOnlyRequestedIdsAndCurrentActor() throws Exception {
        when(service.getMessageStates(7L, 55L, List.of(101L, 102L))).thenReturn(List.of());
        mvc.perform(get("/api/chats/55/messages/state").param("ids", "101,102").param("userId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(service).getMessageStates(7L, 55L, List.of(101L, 102L));
    }

    @Test
    void rejectedSocketSendOnlyEchoesSafeCorrelationKey() {
        socket.handleException(new IllegalArgumentException("private error detail"), () -> "actor",
                new GenericMessage<>(Map.of("clientMessageId", "retry-key", "content", "private text", "senderId", 999)));
        verify(transport).convertAndSendToUser(eq("actor"), eq("/queue/errors"), argThat(value ->
                value instanceof Map<?, ?> payload && "retry-key".equals(payload.get("clientMessageId"))
                        && !payload.containsKey("content") && !payload.containsKey("senderId")
                        && !payload.toString().contains("private error detail")));
    }
}
