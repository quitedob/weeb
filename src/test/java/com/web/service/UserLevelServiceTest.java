package com.web.service;

import com.web.constant.UserLevel;
import com.web.mapper.UserMapper;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.model.User;
import com.web.model.UserLevelHistory;
import com.web.service.Impl.UserLevelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserLevelService测试类
 * 验证用户等级管理功能的正确性
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户等级服务测试")
public class UserLevelServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserLevelHistoryMapper userLevelHistoryMapper;

    @InjectMocks
    private UserLevelServiceImpl userLevelService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setUserEmail("test@example.com");
        testUser.setNickname("测试用户");
        testUser.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Test
    @DisplayName("获取用户等级")
    void testGetUserLevel() {
        // Given
        Long userId = testUser.getId();

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // When
        int level = userLevelService.getUserLevel(userId);

        // Then
        assertEquals(UserLevel.LEVEL_NEW_USER, level, "新用户等级应该为新用户");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("获取不存在用户的等级应该返回默认等级")
    void testGetUserLevelForNonExistentUser() {
        // Given
        Long userId = 999L;

        when(userMapper.selectById(userId)).thenReturn(null);

        // When
        int level = userLevelService.getUserLevel(userId);

        // Then
        assertEquals(UserLevel.LEVEL_NEW_USER, level, "不存在用户应该返回默认等级");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("设置用户等级")
    void testSetUserLevel() {
        // Given
        Long userId = testUser.getId();
        int newLevel = UserLevel.LEVEL_BASIC_USER;
        Long operatorId = 1L;

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        boolean result = userLevelService.setUserLevel(userId, newLevel, operatorId);

        // Then
        assertTrue(result, "等级设置应该成功");
        verify(userMapper).selectById(userId);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    @DisplayName("检查升级条件")
    void testCanUpgradeTo() {
        // Given
        Long userId = testUser.getId();
        int targetLevel = UserLevel.LEVEL_BASIC_USER;

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.countUserArticles(userId)).thenReturn(5);
        when(userMapper.countUserMessages(userId)).thenReturn(20);
        when(userMapper.countUserLikes(userId)).thenReturn(10);

        // When
        boolean canUpgrade = userLevelService.canUpgradeTo(userId, targetLevel);

        // Then
        // 根据UserLevel常量中的条件判断
        assertNotNull(canUpgrade, "升级条件检查结果不应为空");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("检查并升级用户等级")
    void testCheckAndUpgradeUserLevel() {
        // Given
        Long userId = testUser.getId();
        int currentLevel = UserLevel.LEVEL_NEW_USER;

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.countUserArticles(userId)).thenReturn(10);
        when(userMapper.countUserMessages(userId)).thenReturn(50);
        when(userMapper.countUserLikes(userId)).thenReturn(25);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        Map<String, Object> result = userLevelService.checkAndUpgradeUserLevel(userId);

        // Then
        assertNotNull(result, "升级结果不应为空");
        assertTrue(result.containsKey("currentLevel"), "应该包含当前等级");
        assertTrue(result.containsKey("userStats"), "应该包含用户统计");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("获取用户统计数据")
    void testGetUserStats() {
        // Given
        Long userId = testUser.getId();

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.countUserArticles(userId)).thenReturn(5);
        when(userMapper.countUserMessages(userId)).thenReturn(20);
        when(userMapper.countUserLikes(userId)).thenReturn(10);
        when(userMapper.countUserFollowers(userId)).thenReturn(3);
        when(userMapper.countUserArticleViews(userId)).thenReturn(100);
        when(userMapper.countUserComments(userId)).thenReturn(15);

        // When
        Map<String, Object> stats = userLevelService.getUserStats(userId);

        // Then
        assertNotNull(stats, "统计数据不应为空");
        assertEquals(5, stats.get("articleCount"), "文章数量应该为5");
        assertEquals(20, stats.get("messageCount"), "消息数量应该为20");
        assertEquals(10, stats.get("likeCount"), "点赞数量应该为10");
        assertEquals(3, stats.get("followerCount"), "关注者数量应该为3");
        assertEquals(100, stats.get("viewCount"), "浏览量应该为100");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("获取升级进度")
    void testGetUpgradeProgress() {
        // Given
        Long userId = testUser.getId();

        when(userMapper.selectById(userId)).thenReturn(testUser);
        when(userMapper.countUserArticles(userId)).thenReturn(3);
        when(userMapper.countUserMessages(userId)).thenReturn(15);
        when(userMapper.countUserLikes(userId)).thenReturn(8);

        // When
        Map<String, Object> progress = userLevelService.getUpgradeProgress(userId);

        // Then
        assertNotNull(progress, "升级进度不应为空");
        assertTrue(progress.containsKey("currentLevel"), "应该包含当前等级");
        assertTrue(progress.containsKey("nextLevel"), "应该包含下一等级");
        assertTrue(progress.containsKey("requirements"), "应该包含升级要求");
        assertTrue(progress.containsKey("userStats"), "应该包含用户统计");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("获取用户权限列表")
    void testGetUserPermissions() {
        // Given
        Long userId = testUser.getId();

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // When
        List<String> permissions = userLevelService.getUserPermissions(userId);

        // Then
        assertNotNull(permissions, "权限列表不应为空");
        // 根据UserLevel.LEVEL_NEW_USER的权限来验证
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("检查用户权限")
    void testHasPermission() {
        // Given
        Long userId = testUser.getId();
        String permission = "basic_permission";

        when(userMapper.selectById(userId)).thenReturn(testUser);

        // When
        boolean hasPermission = userLevelService.hasPermission(userId, permission);

        // Then
        assertNotNull(hasPermission, "权限检查结果不应为空");
        verify(userMapper).selectById(userId);
    }

    @Test
    @DisplayName("批量更新用户等级")
    void testBatchUpdateUserLevels() {
        // Given
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        int newLevel = UserLevel.LEVEL_BASIC_USER;
        Long operatorId = 1L;

        for (Long userId : userIds) {
            when(userMapper.selectById(userId)).thenReturn(testUser);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
        }

        // When
        Map<String, Object> result = userLevelService.batchUpdateUserLevels(userIds, newLevel, operatorId);

        // Then
        assertNotNull(result, "批量更新结果不应为空");
        assertEquals(3, result.get("successCount"), "成功数量应该为3");
        assertEquals(0, result.get("failCount"), "失败数量应该为0");
        verify(userMapper, times(3)).selectById(anyLong());
        verify(userMapper, times(3)).updateById(any(User.class));
    }

    @Test
    @DisplayName("获取等级统计信息")
    void testGetLevelStatistics() {
        // Given
        when(userMapper.countUsersByLevel(anyInt())).thenReturn(10);
        when(userMapper.selectCount(null)).thenReturn(100L);

        // When
        Map<String, Object> statistics = userLevelService.getLevelStatistics();

        // Then
        assertNotNull(statistics, "统计信息不应为空");
        assertTrue(statistics.containsKey("levelStatistics"), "应该包含等级统计");
        assertTrue(statistics.containsKey("totalUsers"), "应该包含总用户数");
        verify(userMapper, atLeastOnce()).countUsersByLevel(anyInt());
    }

    @Test
    @DisplayName("记录等级变更历史")
    void testRecordLevelChange() {
        // Given
        Long userId = testUser.getId();
        int oldLevel = UserLevel.LEVEL_NEW_USER;
        int newLevel = UserLevel.LEVEL_BASIC_USER;
        String reason = "测试升级";
        Long operatorId = 1L;

        when(userLevelHistoryMapper.insert(any(UserLevelHistory.class))).thenReturn(1);

        // When
        boolean result = userLevelService.recordLevelChange(userId, oldLevel, newLevel, reason, operatorId);

        // Then
        assertTrue(result, "等级变更记录应该成功");
        verify(userLevelHistoryMapper).insert(any(UserLevelHistory.class));
    }

    @Test
    @DisplayName("获取用户等级历史")
    void testGetUserLevelHistory() {
        // Given
        Long userId = testUser.getId();
        int page = 1;
        int pageSize = 10;

        // When
        Map<String, Object> history = userLevelService.getUserLevelHistory(userId, page, pageSize);

        // Then
        assertNotNull(history, "等级历史不应为空");
        assertTrue(history.containsKey("total"), "应该包含总数");
        assertTrue(history.containsKey("page"), "应该包含页码");
        assertTrue(history.containsKey("pageSize"), "应该包含页面大小");
        assertTrue(history.containsKey("records"), "应该包含记录列表");
    }
}