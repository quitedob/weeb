package com.web.security;

import com.web.controller.ArticleCenterController;
import com.web.controller.ContentReportController;
import com.web.controller.UserController;
import com.web.service.ArticleService;
import com.web.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminEndpointAuthorizationTest {
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void normalUserIsDeniedEveryModerationAndUserAdminEndpoint() throws Exception {
        login("ROLE_USER");
        assertDenied(new UserController(), "banUser", "unbanUser", "resetUserPassword");
        assertDenied(new ArticleCenterController(mock(ArticleService.class)), "deleteArticleByAdmin", "getPendingArticles", "approveArticle", "rejectArticle", "getModerationStatistics");
        assertDenied(new ContentReportController(), "getPendingReports", "processReport", "batchProcessReports", "getContentReportDetails", "getReportStatistics", "getTopReportedContent", "getReviewerStats", "markReportAsUrgent", "getReportDetails");
        assertDenied(new com.web.controller.MigrationController(), "validatePreMigration", "validatePostMigration", "createMissingUserStats", "getMigrationStatus");
        assertDenied(new com.web.controller.DiagnosticController(), "clearUserStatsCache");
        assertDenied(new com.web.controller.UserLevelIntegrationController(), "handleLevelChange", "batchHandleLevelChanges");
    }

    @Test void administratorCanPerformGuardedUserOperation() {
        UserService users = mock(UserService.class);
        when(users.banUser(2L)).thenReturn(true);
        UserController target = new UserController();
        ReflectionTestUtils.setField(target, "userService", users);
        login("ROLE_ADMIN");
        UserController controller = (UserController) proxy(target);
        assertEquals(200, controller.banUser(2L).getStatusCode().value());
        verify(users).banUser(2L);
    }

    private static void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static Object proxy(Object target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
        return factory.getProxy();
    }

    private static void assertDenied(Object target, String... names) throws Exception {
        Object controller = proxy(target);
        for (String name : names) {
            var method = java.util.Arrays.stream(target.getClass().getMethods()).filter(item -> item.getName().equals(name)).findFirst().orElseThrow();
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                if (method.getParameterTypes()[i] == int.class) args[i] = 1;
            }
            InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> method.invoke(controller, args), name);
            assertInstanceOf(AccessDeniedException.class, exception.getCause(), name);
        }
    }
}
