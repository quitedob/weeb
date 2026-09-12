package com.web.service;

import com.web.mapper.ContactMapper;
import com.web.task.ContactRequestCleanupTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactExpiryScheduleRegressionTest {
    @Test
    void schedulerRetriesOnlyDeadlocksAndStopsAfterThreeAttempts() {
        ContactMapper mapper = mock(ContactMapper.class);
        ContactRequestCleanupTask task = new ContactRequestCleanupTask();
        ReflectionTestUtils.setField(task, "contactMapper", mapper);
        RuntimeException deadlock = new RuntimeException(new java.sql.SQLException("deadlock", "40001", 1213));
        when(mapper.expirePendingRequests(any(), any())).thenThrow(deadlock).thenReturn(2);
        assertDoesNotThrow(task::cleanExpiredContactRequests);
        verify(mapper, times(2)).expirePendingRequests(any(), any());

        reset(mapper);
        when(mapper.expirePendingRequests(any(), any())).thenThrow(deadlock);
        assertSame(deadlock, assertThrows(RuntimeException.class, task::cleanExpiredContactRequests));
        verify(mapper, times(3)).expirePendingRequests(any(), any());
    }

    @Test
    void otherDatabaseFailuresRemainVisibleWithoutRetry() {
        ContactMapper mapper = mock(ContactMapper.class);
        ContactRequestCleanupTask task = new ContactRequestCleanupTask();
        ReflectionTestUtils.setField(task, "contactMapper", mapper);
        for (int code : new int[] {1205, 1064}) {
            reset(mapper);
            RuntimeException failure = new RuntimeException(new java.sql.SQLException("fixture failure", "HY000", code));
            when(mapper.expirePendingRequests(any(), any())).thenThrow(failure);
            assertSame(failure, assertThrows(RuntimeException.class, task::cleanExpiredContactRequests));
            verify(mapper, times(1)).expirePendingRequests(any(), any());
        }
    }

    @Test
    void schedulerUsesOneConditionalUpdateWithCurrentAndLegacyCutoffs() {
        ContactMapper mapper = mock(ContactMapper.class);
        ContactRequestCleanupTask task = new ContactRequestCleanupTask();
        ReflectionTestUtils.setField(task, "contactMapper", mapper);
        LocalDateTime before = LocalDateTime.now();
        task.cleanExpiredContactRequests();
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> legacyCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).expirePendingRequests(now.capture(), legacyCutoff.capture());
        verifyNoMoreInteractions(mapper);
        assertFalse(now.getValue().isBefore(before));
        assertFalse(now.getValue().isAfter(after));
        assertEquals(now.getValue().minusDays(7), legacyCutoff.getValue());
    }
}
