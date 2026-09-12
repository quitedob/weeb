package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import com.web.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MessageReadControllerIdentityTest {
    @Test
    void everyMessageSearchRouteUsesAuthenticatedActorDespiteForgedQueryUser() throws Exception {
        var messages = mock(MessageSearchService.class);
        when(messages.search(eq(7L), eq("secret"), anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("relevance")))
                .thenReturn(Map.of("list", List.of(), "total", 0L));
        var controller = new SearchController();
        ReflectionTestUtils.setField(controller, "messageSearchService", messages);
        ReflectionTestUtils.setField(controller, "searchService", mock(SearchService.class));
        ReflectionTestUtils.setField(controller, "articleService", mock(ArticleService.class));
        ReflectionTestUtils.setField(controller, "groupService", mock(GroupService.class));
        var mvc = standaloneSetup(controller).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String route : List.of("/api/search/messages", "/api/search/all", "/api/search?type=message", "/api/search?type=all")) {
            mvc.perform(get(route).param("q", "secret").param("userId", "99").param("page", "0").param("size", "10")
                            .requestAttr("userinfo", Map.of("userId", 7L)))
                    .andExpect(status().isOk());
        }
        verify(messages, times(4)).search(7L, "secret", 0, 10, null, null, null, null, null, "relevance");

        when(messages.search(eq(7L), anyString(), anyInt(), anyInt(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new AccessDeniedException("denied"));
        mvc.perform(get("/api/search/messages").param("q", "secret").requestAttr("userinfo", Map.of("userId", 7L)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/search/all").param("q", "secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void threadReadRoutesPassActorForDetailMessagesStatisticsAndGlobalLists() throws Exception {
        var threads = mock(MessageThreadService.class);
        var mvc = standaloneSetup(new MessageThreadController(threads))
                .setCustomArgumentResolvers(new UserInfoArgumentResolver()).build();
        for (String route : List.of("/api/threads/10", "/api/threads/10/messages", "/api/threads/10/statistics",
                "/api/threads/active", "/api/threads/search?keyword=secret")) {
            mvc.perform(get(route).param("userId", "99").requestAttr("userinfo", Map.of("userId", 7L)))
                    .andExpect(status().isOk());
        }
        verify(threads).getThreadById(10L, 7L);
        verify(threads).getThreadMessages(10L, 7L, 1, 20);
        verify(threads).getThreadStatistics(10L, 7L);
        verify(threads).getActiveThreads(7L, 1, 20);
        verify(threads).searchThreads(7L, "secret", 1, 20);
    }
}
