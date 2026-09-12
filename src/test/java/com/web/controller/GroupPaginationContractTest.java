package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.dto.GroupDto;
import com.web.exception.GlobalExceptionHandler;
import com.web.mapper.GroupMapper;
import com.web.model.Group;
import com.web.service.*;
import com.web.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GroupPaginationContractTest {
    private GroupMapper mapper;
    private SearchService search;
    private MockMvc mvc;

    @BeforeEach
    void realControllersAndServiceWithCapturedSqlBoundary() {
        mapper = mock(GroupMapper.class);
        GroupServiceImpl service = new GroupServiceImpl();
        ReflectionTestUtils.setField(service, "groupMapper", mapper);
        GroupController groups = new GroupController();
        ReflectionTestUtils.setField(groups, "groupService", service);
        SearchController searches = new SearchController();
        ReflectionTestUtils.setField(searches, "groupService", service);
        search = mock(SearchService.class);
        ReflectionTestUtils.setField(searches, "searchService", search);
        ReflectionTestUtils.setField(searches, "articleService", mock(ArticleService.class));
        ReflectionTestUtils.setField(searches, "messageSearchService", mock(MessageSearchService.class));
        mvc = standaloneSetup(groups, searches).setCustomArgumentResolvers(new UserInfoArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void legacyNoParameterRoutesKeepArrayAndExplicitParametersUseServerPage() throws Exception {
        GroupDto row = new GroupDto(); row.setId(33L);
        when(mapper.selectUserGroupsWithDetails(7L)).thenReturn(List.of(row));
        when(mapper.selectUserCreatedGroupsWithDetails(7L)).thenReturn(List.of(row));
        when(mapper.countUserGroups(7L, true)).thenReturn(31L);
        when(mapper.selectUserGroupsPage(7L, 10L, 10, true)).thenReturn(List.of(row));
        when(mapper.countUserCreatedGroups(7L)).thenReturn(21L);
        when(mapper.selectUserCreatedGroupsPage(7L, 10L, 10)).thenReturn(List.of(row));
        for (String route : List.of("my-groups", "my-created")) {
            mvc.perform(get("/api/groups/" + route).requestAttr("userinfo", Map.of("userId", 7L)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(33));
            var request = get("/api/groups/" + route).param("page", "1").param("size", "10")
                    .param("userId", "999").requestAttr("userinfo", Map.of("userId", 7L));
            if (route.equals("my-groups")) request.param("excludeOwned", "true");
            mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].id").value(33))
                    .andExpect(jsonPath("$.data.page").value(1)).andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.total").value(route.equals("my-groups") ? 31 : 21));
        }
        verify(mapper).countUserGroups(7L, true);
        verify(mapper).selectUserGroupsPage(7L, 10L, 10, true);
    }

    @Test
    void invalidOrIncompletePaginationGets400BeforeDatabaseQueries() throws Exception {
        for (Map<String, String> values : List.of(Map.of("page", "-1", "size", "10"),
                Map.of("page", "0", "size", "101"), Map.of("page", "1"), Map.of("size", "10"),
                Map.of("excludeOwned", "true"))) {
            var request = get("/api/groups/my-groups").requestAttr("userinfo", Map.of("userId", 7L));
            values.forEach(request::param);
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/groups/my-created").param("page", "0").param("size", "10"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void groupSearchPagesCarryAuthoritativeRoleWithoutLoadingFullJoinedGroups() throws Exception {
        Group group = new Group(); group.setId(44L); group.setGroupName("team"); group.setOwnerId(9L);
        GroupDto role = new GroupDto(); role.setId(44L); role.setCurrentUserRole("ADMIN");
        when(mapper.selectCurrentUserRoles(7L, List.of(44L))).thenReturn(List.of(role));
        when(search.searchGroupsWithFilters(eq("team"), anyInt(), anyInt(), isNull(), isNull(), eq("relevance")))
                .thenReturn(Map.of("list", List.of(group), "total", 1L));
        when(search.searchGroups("team", 0, 5)).thenReturn(Map.of("list", List.of(group), "total", 1L));
        when(mapper.searchGroups("team", 0, 10)).thenReturn(List.of(group));
        for (String route : List.of("/api/search/group?keyword=team", "/api/search?type=group&q=team",
                "/api/search/all?q=team", "/api/groups/search?q=team")) {
            var response = mvc.perform(get(route).param("userId", "999").requestAttr("userinfo", Map.of("userId", 7L)))
                    .andExpect(status().isOk());
            String jsonPath = route.startsWith("/api/groups/") ? "$.data[0].currentUserRole"
                    : route.contains("/all") ? "$.data.groups.list[0].currentUserRole" : "$.data.list[0].currentUserRole";
            response.andExpect(jsonPath(jsonPath).value("ADMIN"));
        }
        verify(mapper, times(4)).selectCurrentUserRoles(7L, List.of(44L));
        verify(mapper, never()).selectUserGroupsWithDetails(anyLong());
    }
}
