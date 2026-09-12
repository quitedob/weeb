package com.web.service;

import com.web.constant.UserLevel;
import com.web.controller.UserLevelIntegrationController;
import com.web.exception.WeebException;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserLevelHistory;
import com.web.service.impl.UserLevelHistoryServiceImpl;
import com.web.service.impl.UserLevelIntegrationServiceImpl;
import com.web.service.impl.UserLevelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserLevelBehaviorTest {
    private final UserMapper users = mock(UserMapper.class);
    private final UserLevelHistoryService histories = mock(UserLevelHistoryService.class);
    private final UserLevelServiceImpl levels = new UserLevelServiceImpl();
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(levels, "userMapper", users);
        ReflectionTestUtils.setField(levels, "userLevelHistoryService", histories);
        user = user(1L, "USER");
        when(users.selectById(1L)).thenReturn(user);
        when(histories.getCurrentLevel(1L)).thenReturn(1);
    }

    @Test
    void statsKeepLongCountsAndUseRealLoginDaysWithFiniteZeroViewEngagement() {
        user.setRegistrationDate(new Date(0));
        when(users.countUserArticles(1L)).thenReturn(3_000_000_000L);
        when(users.countUserLikes(1L)).thenReturn(15L);
        when(users.countUserComments(1L)).thenReturn(5L);
        when(users.countUserLoginDays(1L)).thenReturn(2L);
        var stats = levels.getUserStats(1L);
        assertEquals(3_000_000_000L, stats.get("articleCount"));
        assertEquals(2L, stats.get("loginDays"));
        assertEquals(0.0, stats.get("engagementRate"));
        assertTrue(((Number) stats.get("reputation")).longValue() > Integer.MAX_VALUE);
        when(users.countUserArticleViews(1L)).thenReturn(100L);
        assertEquals(0.2, levels.getUserStats(1L).get("engagementRate"));
    }

    @Test
    void everyBooleanRequirementNeedsExplicitEvidenceAndNumericCountsAcceptLongs() {
        Map<String, Object> stats = richStats();
        assertTrue(UserLevel.canUpgradeTo(1, 2, stats));
        for (int target = 4; target <= 8; target++) {
            assertFalse(UserLevel.canUpgradeTo(target - 1, target, stats), "Missing gate for " + target);
            Map<String, Object> approved = new HashMap<>(stats);
            UserLevel.getLevelRequirements(target).forEach((key, value) -> {
                if (Boolean.TRUE.equals(value)) approved.put(key, true);
            });
            assertTrue(UserLevel.canUpgradeTo(target - 1, target, approved));
            approved.put("paymentRequired", "true");
            if (target == 4) assertFalse(UserLevel.canUpgradeTo(3, target, approved));
        }
        assertFalse(UserLevel.canUpgradeTo(-1, 1, stats));
        assertFalse(UserLevel.canUpgradeTo(1, 9, stats));
        stats.put("articleCount", Double.NaN);
        assertFalse(UserLevel.canUpgradeTo(1, 2, stats));
    }

    @Test
    void progressIncludesUnmetPaymentAndDoesNotClaimUpgradeOnFailedHistoryWrite() {
        when(histories.getCurrentLevel(1L)).thenReturn(3);
        stubRichCounts();
        var progress = levels.getUpgradeProgress(1L);
        assertEquals(false, progress.get("canUpgrade"));
        assertEquals(0.0, ((Map<?, ?>) progress.get("progressDetails")).get("paymentRequired"));
        assertTrue(((Number) progress.get("overallProgress")).doubleValue() < 100);
        when(histories.getCurrentLevel(1L)).thenReturn(1);
        when(histories.recordLevelChange(anyLong(), anyInt(), anyInt(), anyString(), anyInt(), isNull(), isNull(), isNull())).thenReturn(false);
        var result = levels.checkAndUpgradeUserLevel(1L);
        assertEquals(false, result.get("upgraded"));
        assertEquals(1, result.get("newLevel"));
        verify(histories).recordLevelChange(eq(1L), eq(1), eq(3), anyString(), eq(1), isNull(), isNull(), isNull());
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void averageUsesOneDistributionPassAndHistoryReturnsStoredRows() {
        when(users.countUsersByLevel(1)).thenReturn(2);
        when(users.countUsersByLevel(4)).thenReturn(1);
        var stats = levels.getLevelStatistics();
        assertEquals(3L, stats.get("totalUsers"));
        assertEquals(2.0, stats.get("averageLevel"));
        verify(users, times(9)).countUsersByLevel(anyInt());
        var rows = List.of(UserLevelHistory.builder().userId(1L).newLevel(2).build());
        when(histories.getUserLevelHistory(1L, 2, 10)).thenReturn(Map.of("list", rows, "total", 11L));
        assertSame(rows, levels.getUserLevelHistory(1L, 2, 10).get("records"));
    }

    @Test
    void achievementLevelsNeverGrantAdministrativeAuthority() {
        when(histories.getCurrentLevel(1L)).thenReturn(8);
        assertFalse(levels.hasPermission(1L, "ROLE_ADMIN"));
        assertFalse(levels.hasPermission(1L, "BAN_USERS"));
        assertFalse(UserLevel.getLevelPermissions(6).contains("MODERATE_CONTENT"));
        user.setType("ADMIN");
        when(histories.getCurrentLevel(1L)).thenReturn(1);
        assertTrue(levels.hasPermission(1L, "ROLE_ADMIN"));
        user.setStatus(0);
        assertFalse(levels.hasPermission(1L, "ROLE_ADMIN"));
    }

    @Test
    void historyDefaultsToBasicAndSerializesChangesWithoutUpdatingMissingUserColumns() {
        UserLevelHistoryMapper mapper = mock(UserLevelHistoryMapper.class);
        UserLevelHistoryServiceImpl service = historyService(mapper);
        // MyBatis returns null for no row; Mockito's default boxed Integer is zero.
        when(mapper.getCurrentLevelByUserId(1L)).thenReturn(null);
        assertEquals(1, service.getCurrentLevel(1L));
        when(mapper.getCurrentLevelByUserId(1L)).thenReturn(0);
        assertEquals(0, service.getCurrentLevel(1L), "An explicitly stored level zero is not a missing record");
        when(users.lockUserForLevelChange(1L)).thenReturn(1L);
        when(users.selectCurrentLevelForUpdate(1L)).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);
        assertTrue(service.recordLevelChange(1L, 1, 2, "activity", 1, null, null, null));
        ArgumentCaptor<UserLevelHistory> row = ArgumentCaptor.forClass(UserLevelHistory.class);
        verify(mapper).insert(row.capture());
        assertEquals(1, row.getValue().getOldLevel());
        assertEquals(2, row.getValue().getNewLevel());
        assertEquals(1, row.getValue().getStatus());
        when(users.selectCurrentLevelForUpdate(1L)).thenReturn(3);
        assertThrows(WeebException.class, () -> service.recordLevelChange(1L, 1, 2, "stale", 1, null, null, null));
        verify(mapper, times(1)).insert(any());
        assertThrows(WeebException.class, () -> service.recordLevelChange(1L, 3, 8, "role", 1, null, null, null));
        assertThrows(WeebException.class, () -> service.getUserLevelHistory(1L, 0, 10));
        assertThrows(WeebException.class, () -> service.getUserLevelHistory(1L, 1, 0));
    }

    @Test
    void integrationRejectsInvalidOrStaleChangesWithoutOverwritingValidationFailure() {
        UserLevelIntegrationServiceImpl service = new UserLevelIntegrationServiceImpl();
        ReflectionTestUtils.setField(service, "userLevelHistoryService", histories);
        assertEquals(false, service.validateLevelChange(1L, 1, 9).get("valid"));
        assertEquals(false, service.validateLevelChange(1L, 1, 5).get("valid"));
        assertEquals(false, service.validateLevelChange(1L, 2, 3).get("valid"));
        assertEquals(true, service.validateLevelChange(1L, 1, 0).get("valid"));
        assertThrows(WeebException.class, () -> service.handleLevelChange(1L, 1, 5, "jump", 2, 2L, null, null));
        verify(histories, never()).recordLevelChange(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void adminControllerUsesAuthenticatedOperatorAndSocketAddressForSingleAndBatch() {
        UserLevelIntegrationService integration = mock(UserLevelIntegrationService.class);
        var controller = new UserLevelIntegrationController();
        ReflectionTestUtils.setField(controller, "userLevelIntegrationService", integration);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "forged");
        request.addHeader("User-Agent", "test-agent");
        when(integration.handleLevelChange(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(Map.of("success", true));
        controller.handleLevelChange(1L, 1, 2, "review", 1, 17L, request);
        verify(integration).handleLevelChange(1L, 1, 2, "review", 2, 17L, "127.0.0.1", "test-agent");
        when(integration.batchHandleLevelChanges(any())).thenReturn(Map.of("successCount", 1));
        controller.batchHandleLevelChanges(List.of(Map.of("userId", 1, "operatorId", 99, "changeType", 1)), 17L, request);
        ArgumentCaptor<List> capture = ArgumentCaptor.forClass(List.class);
        verify(integration).batchHandleLevelChanges(capture.capture());
        Map<String, Object> trusted = (Map<String, Object>) capture.getValue().get(0);
        assertEquals(17L, trusted.get("operatorId"));
        assertEquals(2, trusted.get("changeType"));
        assertEquals("127.0.0.1", trusted.get("ipAddress"));
    }

    private UserLevelHistoryServiceImpl historyService(UserLevelHistoryMapper mapper) {
        var service = new UserLevelHistoryServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "userLevelHistoryMapper", mapper);
        return service;
    }

    private void stubRichCounts() {
        when(users.countUserArticles(1L)).thenReturn(500L);
        when(users.countUserMessages(1L)).thenReturn(5000L);
        when(users.countUserLikes(1L)).thenReturn(1000L);
        when(users.countUserFollowers(1L)).thenReturn(100L);
        when(users.countUserLoginDays(1L)).thenReturn(365L);
    }

    private Map<String, Object> richStats() {
        return new HashMap<>(Map.of("articleCount", 500L, "messageCount", 5000L, "likeCount", 1000L,
                "followerCount", 100L, "loginDays", 365L, "viewCount", 20000L,
                "engagementRate", 0.1, "reputation", 2000L));
    }

    private User user(Long id, String type) {
        User value = new User();
        value.setId(id);
        value.setType(type);
        value.setStatus(1);
        return value;
    }
}
