package com.web.util;

import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户统计数据工具类
 * 提供便利方法来处理用户和统计数据的操作
 */
@Component
public class UserStatsUtil {

    @Autowired
    private UserStatsService userStatsService;

    /**
     * 为用户列表批量获取统计数据
     * @param users 用户列表
     * @return 用户完整信息列表
     */
    public List<UserWithStats> enrichUsersWithStats(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, UserStats> statsMap = userStatsService.getStatsByUserIds(userIds);

        return users.stream()
                .map(user -> {
                    UserWithStats userWithStats = new UserWithStats();
                    userWithStats.setUser(user);
                    userWithStats.setUserStats(statsMap.get(user.getId()));
                    return userWithStats;
                })
                .collect(Collectors.toList());
    }

    /**
     * 为单个用户获取完整信息
     * @param user 用户对象
     * @return 用户完整信息
     */
    public UserWithStats enrichUserWithStats(User user) {
        if (user == null) {
            return null;
        }

        UserStats stats = userStatsService.getStatsByUserId(user.getId());
        UserWithStats userWithStats = new UserWithStats();
        userWithStats.setUser(user);
        userWithStats.setUserStats(stats);
        return userWithStats;
    }

    /**
     * 批量创建用户统计数据（用于数据迁移）
     * @param userIds 用户ID列表
     * @return 成功创建的数量
     */
    public int batchCreateStats(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                userStatsService.createStatsForUser(userId);
                successCount++;
            } catch (Exception e) {
                // 记录错误但继续处理其他用户
                System.err.println("Failed to create stats for user " + userId + ": " + e.getMessage());
            }
        }
        return successCount;
    }

    /**
     * 检查用户是否有统计数据
     * @param userId 用户ID
     * @return 是否存在统计数据
     */
    public boolean hasStats(Long userId) {
        try {
            UserStats stats = userStatsService.getStatsByUserId(userId);
            return stats != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安全地获取用户粉丝数
     * @param userId 用户ID
     * @return 粉丝数，如果获取失败返回0
     */
    public Long getFansCountSafely(Long userId) {
        try {
            UserStats stats = userStatsService.getStatsByUserId(userId);
            return stats != null ? stats.getFansCount() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 安全地获取用户总点赞数
     * @param userId 用户ID
     * @return 总点赞数，如果获取失败返回0
     */
    public Long getTotalLikesSafely(Long userId) {
        try {
            UserStats stats = userStatsService.getStatsByUserId(userId);
            return stats != null ? stats.getTotalLikes() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 安全地获取用户网站金币
     * @param userId 用户ID
     * @return 网站金币数，如果获取失败返回0
     */
    public Long getWebsiteCoinsSafely(Long userId) {
        try {
            UserStats stats = userStatsService.getStatsByUserId(userId);
            return stats != null ? stats.getWebsiteCoins() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 批量更新用户统计数据（用于数据修复）
     * @param statsUpdates 统计数据更新映射表
     * @return 成功更新的数量
     */
    public int batchUpdateStats(Map<Long, UserStats> statsUpdates) {
        if (statsUpdates == null || statsUpdates.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Map.Entry<Long, UserStats> entry : statsUpdates.entrySet()) {
            try {
                boolean success = userStatsService.updateStats(entry.getValue());
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("Failed to update stats for user " + entry.getKey() + ": " + e.getMessage());
            }
        }
        return successCount;
    }
}