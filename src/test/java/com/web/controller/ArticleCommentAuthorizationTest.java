package com.web.controller;

import com.web.config.UserInfoArgumentResolver;
import com.web.exception.GlobalExceptionHandler;
import com.web.exception.WeebException;
import com.web.mapper.ArticleCommentMapper;
import com.web.mapper.ArticleMapper;
import com.web.model.Article;
import com.web.model.ArticleComment;
import com.web.security.SecurityUtils;
import com.web.service.impl.ArticleCommentServiceImpl;
import com.web.service.impl.ArticleServiceImpl;
import com.web.vo.article.ArticleCommentVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ArticleCommentAuthorizationTest {
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final ArticleCommentMapper comments = mock(ArticleCommentMapper.class);
    private final ArticleCommentServiceImpl service = new ArticleCommentServiceImpl();
    private final Article article = new Article();
    private MockedStatic<SecurityUtils> identity;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var articleService = new ArticleServiceImpl();
        ReflectionTestUtils.setField(articleService, "articleMapper", articles);
        ReflectionTestUtils.setField(service, "articleService", articleService);
        ReflectionTestUtils.setField(service, "articleCommentMapper", comments);
        article.setArticleId(100L);
        article.setUserId(1L);
        article.setStatus(2);
        when(articles.selectArticleById(100L)).thenReturn(article);
        identity = mockStatic(SecurityUtils.class);
        identity.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
        identity.when(SecurityUtils::isAdmin).thenReturn(false);
        var controller = new ArticleCommentController();
        ReflectionTestUtils.setField(controller, "articleCommentService", service);
        mvc = standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new UserInfoArgumentResolver()).build();
    }

    @AfterEach
    void clearIdentity() {
        identity.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void hiddenArticleCommentsCannotBeReadCountedOrCreatedByOutsider(int status) throws Exception {
        article.setStatus(status);
        mvc.perform(get("/api/articles/100/comments")).andExpect(status().isForbidden());
        mvc.perform(get("/api/articles/100/comments/count")).andExpect(status().isForbidden());
        mvc.perform(post("/api/articles/100/comments").requestAttr("userinfo", actor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"A valid comment\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(comments);
    }

    @Test
    void publishedCommentsAndOwnDraftCommentsReuseArticleVisibilityPolicy() {
        when(comments.getCommentsByArticleId(100L)).thenReturn(List.of());
        when(comments.countCommentsByArticleId(100L)).thenReturn(0);
        when(comments.insertComment(any())).thenAnswer(call -> {
            ArticleComment comment = call.getArgument(0);
            comment.setId(73L);
            return 1;
        });
        assertTrue(service.getCommentsByArticleId(100L).isEmpty());
        assertEquals(0, service.getCommentCount(100L));
        assertEquals(73L, service.addComment(100L, request(null), 7L));
        article.setStatus(0);
        identity.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
        assertTrue(service.getCommentsByArticleId(100L).isEmpty());
        assertEquals(73L, service.addComment(100L, request(null), 1L));
    }

    @Test
    void missingOrForeignParentIsRejectedBeforeInsertButSameArticleReplyWorks() {
        ArticleComment foreign = new ArticleComment();
        foreign.setArticleId(101L);
        when(comments.selectById(72L)).thenReturn(foreign);
        for (Long parent : List.of(-1L, 71L, 72L)) {
            assertThrows(WeebException.class, () -> service.addComment(100L, request(parent), 7L));
        }
        verify(comments, never()).insertComment(any());
        foreign.setArticleId(100L);
        when(comments.insertComment(any())).thenAnswer(call -> {
            ArticleComment comment = call.getArgument(0);
            comment.setId(73L);
            return 1;
        });
        assertEquals(73L, service.addComment(100L, request(72L), 7L));
        verify(comments).insertComment(argThat(comment -> comment.getParentId().equals(72L)
                && comment.getArticleId().equals(100L) && comment.getUserId().equals(7L)));
    }

    @Test
    void deletionCarriesPathArticleAndAuthenticatedAuthorToTheDatabase() throws Exception {
        when(comments.deleteComment(100L, 73L, 7L)).thenReturn(1);
        mvc.perform(delete("/api/articles/101/comments/73").requestAttr("userinfo", actor()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/articles/100/comments/73").requestAttr("userinfo", actor()))
                .andExpect(status().isOk());
        verify(comments).deleteComment(101L, 73L, 7L);
        verify(comments).deleteComment(100L, 73L, 7L);
    }

    @Test
    void absentArticleReturnsClientErrorBeforeReadingOrCreatingComments() throws Exception {
        mvc.perform(get("/api/articles/101/comments")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/articles/101/comments").requestAttr("userinfo", actor())
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"A valid comment\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(comments);
    }

    private ArticleCommentVo request(Long parentId) {
        var request = new ArticleCommentVo();
        request.setContent("A valid comment");
        request.setParentId(parentId);
        return request;
    }

    private Map<String, Object> actor() {
        return Map.of("userId", 7L);
    }
}
