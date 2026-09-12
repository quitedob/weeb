package com.web.campus;

import com.web.annotation.Userid;
import com.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
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

class CampusPostControllerTest {
    private CampusPostService posts;
    private MockMvc mvc;
    private Long actor;

    @BeforeEach
    void actualControllerAndSharedExceptionMapping() {
        actor = 71L;
        posts = mock(CampusPostService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CampusPostController(posts))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) { return parameter.hasParameterAnnotation(Userid.class); }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory factory) { return actor; }
                }).build();
    }

    @Test
    void postCreationUsesAuthenticatedActorAndPathSchoolDespiteForgedBodyIdentity() throws Exception {
        when(posts.create(eq(71L), eq(5L), any())).thenReturn(Map.of("id", 18L, "status", "PENDING"));
        mvc.perform(post("/api/campus/schools/5/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"title\",\"content\":\"body\",\"category\":\"GENERAL\",\"mediaIds\":[],\"submit\":true,\"authorId\":999,\"schoolId\":999,\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.id").value(18));
        var input = ArgumentCaptor.forClass(CampusPostDtos.PostWrite.class);
        verify(posts).create(eq(71L), eq(5L), input.capture());
        assertEquals("body", input.getValue().content());
        assertTrue(input.getValue().submit());
        assertNull(input.getValue().version());
    }

    @Test
    void pagingScopeAndTargetStateDeletionAreForwardedWithoutLegacyOneBasedConversion() throws Exception {
        when(posts.list(71L, 5L, "100%_", "STUDY", "popular", "bookmarks", null, 2, 10))
                .thenReturn(new CampusPage<>(List.of(), 42L, 2, 10));
        mvc.perform(get("/api/campus/schools/5/posts").param("q", "100%_").param("category", "STUDY")
                        .param("sort", "popular").param("scope", "bookmarks").param("page", "2").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(2)).andExpect(jsonPath("$.data.total").value(42));
        when(posts.like(71L, 18L, false)).thenReturn(Map.of("likedByMe", false, "likeCount", 0));
        mvc.perform(delete("/api/campus/posts/18/like")).andExpect(status().isOk()).andExpect(jsonPath("$.data.likedByMe").value(false));
        verify(posts).like(71L, 18L, false);
        mvc.perform(delete("/api/campus/posts/18").param("version", "3")).andExpect(status().isOk()).andExpect(jsonPath("$.data.success").value(true));
        verify(posts).delete(71L, 18L, 3L);
    }

    @Test
    void authorizationConflictAndMissingVersionRemainDistinctHttpFailures() throws Exception {
        when(posts.get(71L, 18L)).thenThrow(new CampusException(403, "forbidden"));
        mvc.perform(get("/api/campus/posts/18")).andExpect(status().isForbidden());
        when(posts.review(eq(71L), eq(18L), any())).thenThrow(new CampusException(409, "version conflict"));
        mvc.perform(put("/api/campus/posts/18/review").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\",\"reason\":\"\",\"version\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/campus/posts/18")).andExpect(status().isBadRequest());
        actor = null;
        mvc.perform(put("/api/campus/posts/18/bookmark")).andExpect(status().isUnauthorized());
        verify(posts, never()).bookmark(anyLong(), anyLong(), anyBoolean());
    }
}
