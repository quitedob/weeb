package com.web.campus;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CampusAccessTest {
    @Test
    void schoolLockRejectsCallsWithoutTransactionBeforeAnyDatabaseWork() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        assertThrows(IllegalStateException.class, () -> new CampusAccessService(jdbc).lockSchool(1, 2));
        verifyNoInteractions(jdbc);
    }

    @Test
    void notificationReadPredicateDoesNotConvertDatastoreOutageIntoPermissionSuccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doThrow(new DataAccessResourceFailureException("simulated datastore outage"))
                .when(jdbc).query(anyString(), any(RowMapper.class), eq(5L));
        assertThrows(DataAccessResourceFailureException.class, () -> new CampusAccessService(jdbc).canReadPost(1, 5));
    }
}
