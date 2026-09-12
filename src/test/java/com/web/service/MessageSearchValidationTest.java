package com.web.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageSearchValidationTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final MessageSearchService service = new MessageSearchService(jdbc);

    @Test
    void unauthenticatedAndInvalidRequestsNeverReachDatabase() {
        assertThrows(AccessDeniedException.class, () -> search(null, "secret", 0, 10, null, null, null, null, null, "relevance"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "", 0, 10, null, null, null, null, null, "relevance"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", -1, 10, null, null, null, null, null, "relevance"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 101, null, null, null, null, null, "relevance"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 10, null, null, null, null, null, "id; DROP TABLE message"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 10, "2026-02-30", null, null, null, null, "relevance"));
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 10, "2026-02-02", "2026-02-01", null, null, null, "relevance"));
        for (String malformed : List.of("1 OR 1=1", "0", "1,", "-2", "9223372036854775808")) {
            assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 10, null, null, null, malformed, null, "relevance"));
        }
        assertThrows(IllegalArgumentException.class, () -> search(1L, "secret", 0, 10, null, null, "4", null, null, "relevance"));
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void countAndPageUseSameBoundFiltersAndLiteralKeyword() {
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(7L);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        String keyword = "O'Reilly 10%_!";
        Map<String, Object> result = search(7L, keyword, 2, 3, "2026-01-01", "2026-01-31", "1,3", "5,6", "50", "username_asc");
        assertEquals(7L, result.get("total"));
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> countParams = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(countSql.capture(), countParams.capture(), eq(Long.class));
        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> pageParams = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).query(pageSql.capture(), pageParams.capture(), any(RowMapper.class));
        assertSame(countParams.getValue(), pageParams.getValue());
        assertFalse(countSql.getValue().contains(keyword));
        assertFalse(pageSql.getValue().contains(keyword));
        var params = countParams.getValue();
        assertEquals(7L, params.getValue("actor"));
        assertEquals("%O'Reilly 10!%!_!!%", params.getValue("keyword"));
        assertEquals(List.of(5L, 6L), params.getValue("senders"));
        assertEquals(List.of(50L), params.getValue("groups"));
        assertEquals(List.of(1L, 3L), params.getValue("types"));
        assertEquals(6L, params.getValue("offset"));
        assertEquals(Timestamp.valueOf("2026-02-01 00:00:00"), params.getValue("end"));
    }

    private Map<String, Object> search(Long actor, String keyword, int page, int size, String start, String end,
                                       String types, String users, String groups, String sort) {
        return service.search(actor, keyword, page, size, start, end, types, users, groups, sort);
    }
}
