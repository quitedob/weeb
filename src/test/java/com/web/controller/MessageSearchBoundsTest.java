package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import com.web.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MessageSearchBoundsTest {
    private NamedParameterJdbcTemplate jdbc;
    private SearchService otherSearch;
    private ArticleService articles;
    private MockMvc mvc;

    @BeforeEach
    void actualMessageServiceAndHttpExceptionMapping() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(0L);
        SearchController controller = new SearchController();
        ReflectionTestUtils.setField(controller, "messageSearchService", new MessageSearchService(jdbc));
        otherSearch = mock(SearchService.class); articles = mock(ArticleService.class);
        ReflectionTestUtils.setField(controller, "searchService", otherSearch);
        ReflectionTestUtils.setField(controller, "articleService", articles);
        ReflectionTestUtils.setField(controller, "groupService", mock(GroupService.class));
        mvc = standaloneSetup(controller).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void rawKeywordAndDistinctTermBoundsRejectBeforeAnySearchWorkAcrossMessageRoutes() throws Exception {
        for (String route : List.of("/api/search/messages", "/api/search?type=message", "/api/search/all", "/api/search?type=all")) {
            for (String query : List.of(" ".repeat(101) + "alpha", "a b c d e f g h i j k")) {
                mvc.perform(get(route).param("q", query).requestAttr("userinfo", Map.of("userId", 7L)))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(jdbc, otherSearch, articles);
    }

    @Test
    void oversizedAndDeepPagesReturn400WithoutOverflowOrDatabaseAccess() throws Exception {
        for (Map<String, String> parameters : List.of(Map.of("page", "0", "size", "101"),
                Map.of("page", "100", "size", "100"), Map.of("page", String.valueOf(Integer.MAX_VALUE), "size", "100"),
                Map.of("page", "-1", "size", "10"))) {
            var request = get("/api/search/messages").param("q", "alpha").requestAttr("userinfo", Map.of("userId", 7L));
            parameters.forEach(request::param);
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(jdbc);
    }

    @Test
    void existingKeywordAndLastResultPageBoundariesRemainAccepted() throws Exception {
        mvc.perform(get("/api/search/messages").param("q", "x".repeat(100)).param("page", "99").param("size", "100")
                        .requestAttr("userinfo", Map.of("userId", 7L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(99))
                .andExpect(jsonPath("$.data.size").value(100));
        verify(jdbc).queryForObject(anyString(), argThat((SqlParameterSource parameters) ->
                parameters.getValue("offset").equals(9900L) && parameters.getValue("size").equals(100)), eq(Long.class));
        assertDoesNotThrow(() -> MessageSearchService.validateMessageRequest(7L,
                "Alpha ALPHA beta gamma delta epsilon zeta eta theta iota kappa", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> MessageSearchService.validateMessageRequest(7L,
                "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda", 0, 10));
    }
}
