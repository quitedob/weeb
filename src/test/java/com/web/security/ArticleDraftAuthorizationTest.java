package com.web.security;

import com.web.controller.ArticleVersionController;
import com.web.exception.GlobalExceptionHandler;
import com.web.mapper.ArticleMapper;
import com.web.mapper.ArticleVersionMapper;
import com.web.mapper.AuthMapper;
import com.web.model.Article;
import com.web.model.ArticleVersion;
import com.web.model.User;
import com.web.service.impl.ArticleServiceImpl;
import com.web.service.impl.ArticleVersionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ArticleDraftAuthorizationTest {
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final ArticleVersionMapper versions = mock(ArticleVersionMapper.class);
    private final AuthMapper auth = mock(AuthMapper.class);
    private final ArticleVersionServiceImpl versionService = new ArticleVersionServiceImpl();
    private final User actor = new User();
    private final Article article = new Article();

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(versionService, "articleMapper", articles);
        ReflectionTestUtils.setField(versionService, "articleVersionMapper", versions);
        ReflectionTestUtils.setField(versionService, "authMapper", auth);
        actor.setId(7L);
        actor.setUsername("evil_admin");
        actor.setStatus(1);
        actor.setType("USER");
        article.setArticleId(100L);
        article.setUserId(1L);
        article.setStatus(0);
        when(articles.selectArticleById(100L)).thenReturn(article);
        when(auth.findByUsername("evil_admin")).thenReturn(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("evil_admin", null, List.of()));
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void unrelatedUserCannotReadOrWriteVersionsEvenForPublishedRoot() {
        for (int status : List.of(0, 1, 2, 3)) {
            article.setStatus(status);
            assertThrows(AccessDeniedException.class, () -> versionService.getArticleVersions(100L));
            assertThrows(AccessDeniedException.class, () -> versionService.getLatestVersion(100L));
            assertThrows(AccessDeniedException.class, () -> versionService.getVersionStatistics(100L));
            assertThrows(AccessDeniedException.class, () -> versionService.autoSaveVersion(100L, "title", "draft", 7L));
            assertThrows(AccessDeniedException.class, () -> versionService.createVersion(100L, "title", "draft", 7L));
            assertThrows(AccessDeniedException.class, () -> versionService.cleanupOldVersions(100L));
            assertThrows(AccessDeniedException.class, () -> versionService.deleteAllVersions(100L));
        }
        verifyNoInteractions(versions);
    }

    @Test void authorAndPersistedAdministratorCanUseVersionsButCannotForgeActor() {
        when(versions.insert(any(ArticleVersion.class))).thenReturn(1);
        article.setUserId(7L);
        assertTrue(versionService.autoSaveVersion(100L, "title", "draft", 7L));
        verify(versions).insert(argThat((ArticleVersion version) -> version.getCreatedBy().equals(7L)));
        assertThrows(AccessDeniedException.class, () -> versionService.createVersion(100L, "title", "draft", 1L));
        article.setUserId(1L);
        actor.setType("ADMIN");
        assertTrue(versionService.createVersion(100L, "admin title", "draft", 7L));
        actor.setStatus(0);
        assertThrows(AccessDeniedException.class, () -> versionService.getLatestVersion(100L));
    }

    @Test void hiddenArticleCannotBeFavoritedToExposeItsContent() {
        var service = new ArticleServiceImpl();
        ReflectionTestUtils.setField(service, "articleMapper", articles);
        try (var identity = mockStatic(SecurityUtils.class)) {
            identity.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            identity.when(SecurityUtils::isAdmin).thenReturn(false);
            for (int status : List.of(0, 1, 3)) {
                article.setStatus(status);
                assertThrows(AccessDeniedException.class, () -> service.favoriteArticle(100L, 7L));
            }
            verify(articles, never()).insertFavorite(anyLong(), anyLong());
            when(articles.getUserFavoriteArticles(7L, 10, 10, false)).thenReturn(List.of());
            when(articles.countUserFavoriteArticles(7L, false)).thenReturn(0);
            assertEquals(0, service.getUserFavoriteArticles(7L, 2, 10).get("total"));
            verify(articles).getUserFavoriteArticles(7L, 10, 10, false);
            verify(articles).countUserFavoriteArticles(7L, false);
        }
    }

    @Test void versionHttpBoundaryPreservesForbiddenStatus() throws Exception {
        var controller = new ArticleVersionController();
        ReflectionTestUtils.setField(controller, "articleVersionService", versionService);
        var mvc = standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/articles/100/versions/latest")).andExpect(status().isForbidden());
        mvc.perform(get("/api/articles/100/versions/statistics")).andExpect(status().isForbidden());
    }
}
