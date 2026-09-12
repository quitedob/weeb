package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.Message;
import com.web.service.impl.MessageRetryServiceImpl;
import com.web.vo.message.SendMessageVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageRetryKeyIsolationTest {
    private static final String ID = "12345678-1234-1234-1234-123456789abc";
    private static final String RECORD = "message:failed:" + ID;
    private static final String INDEX = "message:failed:user:77";
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
    private final UnifiedMessageService messages = mock(UnifiedMessageService.class);
    private final MessageRetryServiceImpl service = new MessageRetryServiceImpl();
    private final Map<Object, Object> record = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
        ReflectionTestUtils.setField(service, "unifiedMessageService", messages);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(mock(SetOperations.class));
        Cursor<String> cursor = mock(Cursor.class);
        Iterator<String> iterator = List.of(INDEX, RECORD, "message:failed:metadata", RECORD).iterator();
        when(cursor.hasNext()).thenAnswer(call -> iterator.hasNext());
        when(cursor.next()).thenAnswer(call -> iterator.next());
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        SendMessageVo request = new SendMessageVo();
        request.setTargetId(88L);
        request.setTargetType("PRIVATE");
        request.setContent("retry me");
        record.put("retryId", ID);
        record.put("userId", 77L);
        record.put("retryCount", 0);
        record.put("status", "PENDING");
        record.put("sendMessageVo", request);
        when(hashes.entries(RECORD)).thenReturn(record);
    }

    @Test
    void retrySkipsSetIndexesAndProcessesEachRecordOnceEvenWithScanDuplicates() {
        Message saved = new Message();
        saved.setId(100L);
        when(messages.sendMessage(any(), eq(77L))).thenReturn(saved);
        assertEquals(1, service.autoRetryFailedMessages());
        verify(messages, times(1)).sendMessage(any(), eq(77L));
        verify(hashes, never()).entries(INDEX);
        verify(hashes, never()).entries("message:failed:metadata");
        verify(redis, never()).keys(anyString());
    }

    @Test
    void cleanupDeletesOnlyTerminalRecordHashesWithoutReadingIndexesAsHashes() {
        record.put("status", "SUCCESS");
        assertEquals(1, service.cleanExpiredFailedRecords());
        verify(redis, times(1)).delete(RECORD);
        verify(redis, never()).delete(INDEX);
        verify(hashes, never()).entries(INDEX);
    }

    @Test
    void terminalRecordsAndIndexLikeIdentifiersCannotBeReplayed() {
        record.put("status", "SUCCESS");
        assertFalse(service.retryFailedMessage(ID));
        assertFalse(service.retryFailedMessage("user:77"));
        verifyNoInteractions(messages);
        verify(hashes, never()).entries(INDEX);
    }
}
