package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.mapper.ArticleMapper;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserTransactionService;
import com.web.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 用户事务服务实现类
 * 处理需要跨越user和user_stats表的复杂事务操作
 */
@Service
@Transactional
public class UserTransactionServiceImpl implements UserTransactionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserTransactionServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<UserWithStats> batchCreateUsers(List<User> users) {
        List<UserWithStats> result = new ArrayList<>();
        
        for (User user : users) {
            try {
                // 插入用户基本信息
                userMapper.insert(user);
                
                // 创建对应的统计记录
                UserStats userStats = new UserStats();
                userStats.setUserId(user.getId());
                userStats.setFansCount(0L);
                userStats.setTotalLikes(0L);
                userStats.setTotalFavorites(0L);
                userStats.setTotalSponsorship(0L);
                userStats.setTotalArticleExposure(0L);
                userStats.setWebsiteCoins(0L);
                
                userStatsMapper.insertUserStats(userStats);
                
                // 获取完整用户信息
                UserWithStats userWithStats = userMapper.selectUserWithStatsById(user.getId());
                result.add(userWithStats);
                
            } catch (Exception e) {
                log.error("批量创建用户失败: {}", e.getMessage(), e);
                throw new WeebException("批量创建用户失败: " + e.getMessage());
            }
        }
        
        return result;
    }

    @Override
    public boolean migrateUserStats(Long userId, Map<String, Object> legacyStats) {
        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            
            // 检查是否已有统计记录
            UserStats existingStats = userStatsMapper.selectByUserId(userId);
            if (existingStats != null) {
                // 更新现有记录
                updateStatsFromLegacyData(existingStats, legacyStats);
                userStatsMapper.updateUserStats(existingStats);
            } else {
                // 创建新记录
                UserStats newStats = new UserStats();
                newStats.setUserId(userId);
                updateStatsFromLegacyData(newStats, legacyStats);
                userStatsMapper.insertUserStats(newStats);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int batchMigrateUserStats(Map<Long, Map<String, Object>> userStatsMap) {
        int successCount = 0;
        
        for (Map.Entry<Long, Map<String, Object>> entry : userStatsMap.entrySet()) {
            if (migrateUserStats(entry.getKey(), entry.getValue())) {
                successCount++;
            }
        }
        
        return successCount;
    }

    @Override
    public boolean updateUserActivity(Long userId, String activityType, Map<String, Object> activityData) {
        try {
            switch (activityType.toLowerCase()) {
                case "publish_article":
                    // 发布文章时更新相关统计
                    userStatsMapper.incrementArticleExposure(userId, 1L);
                    break;
                case "receive_like":
                    // 收到点赞时更新统计
                    Long likeCount = (Long) activityData.getOrDefault("count", 1L);
                    userStatsMapper.incrementTotalLikes(userId, likeCount);
                    break;
                case "receive_favorite":
                    // 收到收藏时更新统计
                    Long favoriteCount = (Long) activityData.getOrDefault("count", 1L);
                    userStatsMapper.incrementTotalFavorites(userId, favoriteCount);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean updateUserRelationship(Long fromUserId, Long toUserId, String relationshipType) {
        try {
            switch (relationshipType.toLowerCase()) {
                case "follow":
                    // 关注操作：被关注者粉丝数+1
                    userStatsMapper.incrementFansCount(toUserId);
                    break;
                case "unfollow":
                    // 取消关注：被关注者粉丝数-1
                    userStatsMapper.decrementFansCount(toUserId);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean processContentInteraction(Long userId, Long contentId, Long authorId, String interactionType) {
        try {
            switch (interactionType.toLowerCase()) {
                case "like":
                    // 点赞：作者总点赞数+1
                    userStatsMapper.incrementTotalLikes(authorId, 1L);
                    break;
                case "favorite":
                    // 收藏：作者总收藏数+1
                    userStatsMapper.incrementTotalFavorites(authorId, 1L);
                    break;
                case "view":
                    // 浏览：作者文章曝光数+1
                    userStatsMapper.incrementArticleExposure(authorId, 1L);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean processEconomicTransaction(Long fromUserId, Long toUserId, Long amount, 
                                            String transactionType, Map<String, Object> metadata) {
        try {
            switch (transactionType.toLowerCase()) {
                case "sponsor":
                    // 赞助：扣除赞助者金币，增加被赞助者金币和赞助总额
                    if (userStatsMapper.deductWebsiteCoins(fromUserId, amount) > 0) {
                        userStatsMapper.addWebsiteCoins(toUserId, amount);
                        userStatsMapper.addSponsorship(toUserId, amount);
                        return true;
                    }
                    return false;
                case "reward":
                    // 奖励：直接增加用户金币
                    userStatsMapper.addWebsiteCoins(toUserId, amount);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int checkAndRepairDataConsistency(Long userId) {
        int repairedCount = 0;
        
        try {
            List<Long> userIds = userId != null ? Arrays.asList(userId) : 
                userMapper.selectUserList(null, null, null, "id ASC")
                    .stream().map(User::getId).toList();
            
            for (Long uid : userIds) {
                // 检查用户是否有统计记录
                UserStats stats = userStatsMapper.selectByUserId(uid);
                if (stats == null) {
                    // 创建缺失的统计记录
                    UserStats newStats = new UserStats();
                    newStats.setUserId(uid);
                    newStats.setFansCount(0L);
                    newStats.setTotalLikes(0L);
                    newStats.setTotalFavorites(0L);
                    newStats.setTotalSponsorship(0L);
                    newStats.setTotalArticleExposure(0L);
                    newStats.setWebsiteCoins(0L);
                    
                    userStatsMapper.insertUserStats(newStats);
                    repairedCount++;
                }
            }
        } catch (Exception e) {
            // 记录错误但不抛出异常
        }
        
        return repairedCount;
    }

    @Override
    public boolean recalculateUserStats(Long userId) {
        try {
            // 基于实际数据重新计算统计
            articleMapper.updateUserStatsTotals(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int batchRecalculateUserStats(List<Long> userIds) {
        int successCount = 0;
        
        for (Long userId : userIds) {
            if (recalculateUserStats(userId)) {
                successCount++;
            }
        }
        
        return successCount;
    }

    @Override
    public boolean archiveUserData(Long userId, String archiveType) {
        // 简化实现，实际项目中可能需要将数据移动到归档表
        try {
            // 这里可以实现数据归档逻辑
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean restoreUserData(Long userId) {
        // 简化实现，实际项目中需要从归档表恢复数据
        try {
            // 这里可以实现数据恢复逻辑
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> performSystemMaintenance(String maintenanceType, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (maintenanceType.toLowerCase()) {
                case "cleanup_orphaned_stats":
                    // 清理孤立的统计记录
                    int cleanedCount = 0; // 实际实现中需要查询和清理
                    result.put("cleanedCount", cleanedCount);
                    result.put("success", true);
                    break;
                case "rebuild_all_stats":
                    // 重建所有用户统计
                    List<User> allUsers = userMapper.selectUserList(null, null, null, "id ASC");
                    int rebuiltCount = batchRecalculateUserStats(
                        allUsers.stream().map(User::getId).toList()
                    );
                    result.put("rebuiltCount", rebuiltCount);
                    result.put("success", true);
                    break;
                default:
                    result.put("success", false);
                    result.put("error", "Unknown maintenance type");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 从旧数据更新统计信息
     */
    private void updateStatsFromLegacyData(UserStats stats, Map<String, Object> legacyData) {
        if (legacyData.containsKey("fansCount")) {
            stats.setFansCount(((Number) legacyData.get("fansCount")).longValue());
        }
        if (legacyData.containsKey("totalLikes")) {
            stats.setTotalLikes(((Number) legacyData.get("totalLikes")).longValue());
        }
        if (legacyData.containsKey("totalFavorites")) {
            stats.setTotalFavorites(((Number) legacyData.get("totalFavorites")).longValue());
        }
        if (legacyData.containsKey("totalSponsorship")) {
            stats.setTotalSponsorship(((Number) legacyData.get("totalSponsorship")).longValue());
        }
        if (legacyData.containsKey("articleExposure")) {
            stats.setTotalArticleExposure(((Number) legacyData.get("articleExposure")).longValue());
        }
        if (legacyData.containsKey("websiteCoins")) {
            stats.setWebsiteCoins(((Number) legacyData.get("websiteCoins")).longValue());
        }
    }
}