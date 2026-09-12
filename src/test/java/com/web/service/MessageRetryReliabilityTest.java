package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.ChatList;
import com.web.model.Message;
import com.web.model.User;
import com.web.service.impl.MessageRetryServiceImpl;
import com.web.service.impl.UnifiedMessageServiceImpl;
import com.web.vo.message.SendMessageVo;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageRetryReliabilityTest {
    @Test
    @SuppressWarnings("unchecked")
    void legacyRetryUsesDeterministicClientKeyAndMarksInternalAttempt() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        UnifiedMessageService unified = mock(UnifiedMessageService.class);
        MessageRetryServiceImpl retry = new MessageRetryServiceImpl();
        ReflectionTestUtils.setField(retry, "redisTemplate", redis);
        ReflectionTestUtils.setField(retry, "unifiedMessageService", unified);
        ReflectionTestUtils.setField(retry, "objectMapper", new ObjectMapper());
        String id = UUID.randomUUID().toString();
        when(redis.opsForValue().setIfAbsent(eq("message:failed:" + id + ":lease"), anyString(), eq(90L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(redis.opsForHash().entries("message:failed:" + id)).thenReturn(Map.of("status", "PENDING", "retryCount", 0,
                "userId", 7L, "sendMessageVo", Map.of("targetType", "PRIVATE", "targetId", 8L, "content", "hello", "messageType", 1)));
        when(unified.sendMessage(any(), eq(7L))).thenThrow(new IllegalStateException("temporary transport failure"));
        assertFalse(retry.retryFailedMessage(id));
        assertFalse(retry.retryFailedMessage(id));
        verify(unified, times(2)).sendMessage(argThat(request -> request.isRetryAttempt()
                && ("retry:" + id).equals(request.getClientMessageId())), eq(7L));
        verify(redis.opsForHash(), never()).putAll(anyString(), anyMap());
    }

    @Test
    void failedRetryDoesNotSpawnAnotherJobAndConflictingKeyIsNeverQueued() {
        UnifiedMessageServiceImpl unified = new UnifiedMessageServiceImpl();
        UserService users = mock(UserService.class);
        User user = new User(); user.setId(7L); user.setStatus(1);
        when(users.getUserBasicInfo(7L)).thenReturn(user);
        ChatService chat = mock(ChatService.class);
        ChatList conversation = new ChatList(); conversation.setId("55_7"); conversation.setSharedChatId(55L);
        when(chat.createChat(7L, 8L)).thenReturn(conversation);
        UserPreferencesService preferences = mock(UserPreferencesService.class);
        when(preferences.canReceiveMessages(8L)).thenReturn(true);
        MessageRetryService retry = mock(MessageRetryService.class);
        ReflectionTestUtils.setField(unified, "userService", users);
        ReflectionTestUtils.setField(unified, "chatService", chat);
        ReflectionTestUtils.setField(unified, "chatAccessService", mock(ChatAccessService.class));
        ReflectionTestUtils.setField(unified, "userPreferencesService", preferences);
        ReflectionTestUtils.setField(unified, "messageRetryService", retry);
        SendMessageVo request = new SendMessageVo(); request.setTargetType("PRIVATE"); request.setTargetId(8L);
        request.setContent("hello"); request.setMessageType(1); request.setClientMessageId("original-key"); request.setRetryAttempt(true);
        when(chat.sendMessage(eq(7L), eq("55_7"), any())).thenThrow(new IllegalStateException("injected persistence failure"));
        assertThrows(IllegalStateException.class, () -> unified.sendMessage(request, 7L));
        verifyNoInteractions(retry);
        request.setRetryAttempt(false);
        when(chat.sendMessage(eq(7L), eq("55_7"), any())).thenThrow(new IllegalArgumentException("conflicting key"));
        assertThrows(IllegalArgumentException.class, () -> unified.sendMessage(request, 7L));
        verifyNoInteractions(retry);
        verify(chat, times(2)).sendMessage(eq(7L), eq("55_7"), argThat(message -> "original-key".equals(message.getClientMessageId())));
    }
}
