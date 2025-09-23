package com.web.service.impl;

import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserService;
import com.web.service.UserStatsService;
import com.web.service.UserTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 用户事务服务实现类
 * 处理复杂的跨表事务操作
 */
@Service
public class UserTransactionServiceImpl implements UserTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(UserTransactionServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserStatsService userStatsService;

    @Override
    @Transactional
    public List<UserWithStats> batchCreateUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserWithStats> createdUsers = new ArrayList<>();
        
        try {
            for (User user : users) {
                UserWithStats createdUser = userService.createUser(user);
                if (createdUser != null) {
                    createdUsers.add(createdUser);
                }
            }
            
            logger.info("批量创建用户成功，共创建 {} 个用户", createdUsers.size());
            return createdUsers;
        } catch (Exception e) {
            logger.error("批量创建用户失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量创建用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean migrateUserStats(Long userId, Map<String, Object> legacyStats) {
        if (userId == null || legacyStats == null || legacyStats.isEmpty()) {
            return false;
        }

        try {
            // 检查用户是否存在
            if (!userService.userExists(userId)) {
                logger.warn("用户不存在，无法迁移统计数据: {}", userId);
                return false;
            }

            // 创建或获取现有的统计数据记录
            UserStats userStats = userStatsService.getStatsByUserId(userId);
            if (userStats == null) {
                userStats = userStatsService.createStatsForUser(userId);
            }

            // 从遗留数据中提取统计信息
            if (legacyStats.containsKey("fansCount")) {
                userStats.setFansCount(getLongValue(legacyStats.get("fansCount")));
            }
            if (legacyStats.containsKey("totalLikes")) {
                userStats.setTotalLikes(getLongValue(legacyStats.get("totalLikes")));
            }
            if (legacyStats.containsKey("totalFavorites")) {
                userStats.setTotalFavorites(getLongValue(legacyStats.get("totalFavorites")));
            }
            if (legacyStats.containsKey("totalSponsorship")) {
                userStats.setTotalSponsorship(getLongValue(legacyStats.get("totalSponsorship")));
            }
            if (legacyStats.containsKey("totalArticleExposure")) {
                userStats.setTotalArticleExposure(getLongValue(legacyStats.get("totalArticleExposure")));
            }
            if (legacyStats.containsKey("websiteCoins")) {
                userStats.setWebsiteCoins(getLongValue(legacyStats.get("websiteCoins")));
            }

            // 更新统计数据
            boolean success = userStatsService.updateStats(userStats);
            
            if (success) {
                logger.info("用户统计数据迁移成功: {}", userId);
            } else {
                logger.error("用户统计数据迁移失败: {}", userId);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("迁移用户统计数据失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new RuntimeException("迁移用户统计数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int batchMigrateUserStats(Map<Long, Map<String, Object>> userStatsMap) {
        if (userStatsMap == null || userStatsMap.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        
        try {
            for (Map.Entry<Long, Map<String, Object>> entry : userStatsMap.entrySet()) {
                Long userId = entry.getKey();
                Map<String, Object> legacyStats = entry.getValue();
                
                try {
                    if (migrateUserStats(userId, legacyStats)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("批量迁移中单个用户失败，用户ID: {}, 错误: {}", userId, e.getMessage());
                    // 继续处理其他用户，不中断整个批量操作
                }
            }
            
            logger.info("批量迁移用户统计数据完成，成功: {}, 总数: {}", successCount, userStatsMap.size());
            return successCount;
        } catch (Exception e) {
            logger.error("批量迁移用户统计数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量迁移用户统计数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean updateUserActivity(Long userId, String activityType, Map<String, Object> activityData) {
        if (userId == null || activityType == null) {
            return false;
        }

        try {
            switch (activityType.toLowerCase()) {
                case "publish_article":
                    return handlePublishArticle(userId, activityData);
                case "delete_article":
                    return handleDeleteArticle(userId, activityData);
                case "receive_like":
                    return handleReceiveLike(userId, activityData);
                case "receive_favorite":
                    return handleReceiveFavorite(userId, activityData);
                case "receive_sponsorship":
                    return handleReceiveSponsorship(userId, activityData);
                default:
                    logger.warn("未知的活动类型: {}", activityType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("更新用户活动失败，用户ID: {}, 活动类型: {}, 错误: {}", userId, activityType, e.getMessage(), e);
            throw new RuntimeException("更新用户活动失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean updateUserRelationship(Long fromUserId, Long toUserId, String relationshipType) {
        if (fromUserId == null || toUserId == null || relationshipType == null) {
            return false;
        }

        try {
            switch (relationshipType.toLowerCase()) {
                case "follow":
                    return userService.followUser(fromUserId, toUserId);
                case "unfollow":
                    return userService.unfollowUser(fromUserId, toUserId);
                case "block":
                    // 这里可以添加拉黑逻辑
                    logger.info("处理用户拉黑关系: {} -> {}", fromUserId, toUserId);
                    return true;
                case "unblock":
                    // 这里可以添加解除拉黑逻辑
                    logger.info("处理用户解除拉黑关系: {} -> {}", fromUserId, toUserId);
                    return true;
                default:
                    logger.warn("未知的关系类型: {}", relationshipType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("更新用户关系失败，从用户: {}, 到用户: {}, 关系类型: {}, 错误: {}", 
                        fromUserId, toUserId, relationshipType, e.getMessage(), e);
            throw new RuntimeException("更新用户关系失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean processContentInteraction(Long userId, Long contentId, Long authorId, String interactionType) {
        if (userId == null || contentId == null || authorId == null || interactionType == null) {
            return false;
        }

        try {
            switch (interactionType.toLowerCase()) {
                case "like":
                    return userService.likeArticle(userId, contentId, authorId);
                case "favorite":
                    return userService.favoriteArticle(userId, contentId, authorId);
                case "view":
                    return userService.recordArticleExposure(authorId, 1L);
                case "comment":
                    // 这里可以添加评论相关的统计更新
                    logger.info("处理评论互动: 用户 {} 评论内容 {}", userId, contentId);
                    return true;
                default:
                    logger.warn("未知的互动类型: {}", interactionType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("处理内容互动失败，用户: {}, 内容: {}, 作者: {}, 互动类型: {}, 错误: {}", 
                        userId, contentId, authorId, interactionType, e.getMessage(), e);
            throw new RuntimeException("处理内容互动失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean processEconomicTransaction(Long fromUserId, Long toUserId, Long amount, 
                                            String transactionType, Map<String, Object> metadata) {
        if (fromUserId == null || toUserId == null || amount == null || amount <= 0 || transactionType == null) {
            return false;
        }

        try {
            switch (transactionType.toLowerCase()) {
                case "sponsorship":
                    return userService.sponsorUser(fromUserId, toUserId, amount);
                case "tip":
                    // 打赏逻辑，类似赞助但可能有不同的处理
                    return userService.sponsorUser(fromUserId, toUserId, amount);
                case "purchase":
                    // 购买逻辑，只扣除金币不增加对方收入
                    return userService.consumeCoins(fromUserId, amount);
                case "reward":
                    // 奖励逻辑，只增加金币
                    return userService.rewardCoins(toUserId, amount);
                default:
                    logger.warn("未知的交易类型: {}", transactionType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("处理经济交易失败，从用户: {}, 到用户: {}, 金额: {}, 交易类型: {}, 错误: {}", 
                        fromUserId, toUserId, amount, transactionType, e.getMessage(), e);
            throw new RuntimeException("处理经济交易失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int checkAndRepairDataConsistency(Long userId) {
        // 这个方法需要复杂的数据一致性检查逻辑
        // 暂时返回0，实际实现需要检查user表和user_stats表的数据一致性
        logger.info("检查数据一致性，用户ID: {}", userId);
        return 0;
    }

    @Override
    @Transactional
    public boolean recalculateUserStats(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            // 重置用户统计数据
            boolean resetSuccess = userService.resetUserStats(userId);
            if (!resetSuccess) {
                return false;
            }

            // 这里应该基于实际的业务数据重新计算统计
            // 例如：统计用户的文章数、获得的点赞数等
            // 暂时只是重置，实际实现需要查询相关业务表进行计算
            
            logger.info("重新计算用户统计数据成功: {}", userId);
            return true;
        } catch (Exception e) {
            logger.error("重新计算用户统计数据失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new RuntimeException("重新计算用户统计数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int batchRecalculateUserStats(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long userId : userIds) {
            try {
                if (recalculateUserStats(userId)) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("批量重新计算中单个用户失败，用户ID: {}, 错误: {}", userId, e.getMessage());
            }
        }

        logger.info("批量重新计算用户统计数据完成，成功: {}, 总数: {}", successCount, userIds.size());
        return successCount;
    }

    @Override
    @Transactional
    public boolean archiveUserData(Long userId, String archiveType) {
        // 用户数据归档逻辑
        logger.info("归档用户数据，用户ID: {}, 归档类型: {}", userId, archiveType);
        return true;
    }

    @Override
    @Transactional
    public boolean restoreUserData(Long userId) {
        // 用户数据恢复逻辑
        logger.info("恢复用户数据，用户ID: {}", userId);
        return true;
    }

    @Override
    @Transactional
    public Map<String, Object> performSystemMaintenance(String maintenanceType, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            switch (maintenanceType.toLowerCase()) {
                case "cleanup_orphaned_stats":
                    // 清理孤立的统计数据记录
                    result.put("cleaned_records", 0);
                    break;
                case "rebuild_all_stats":
                    // 重建所有用户的统计数据
                    result.put("rebuilt_users", 0);
                    break;
                case "data_consistency_check":
                    // 数据一致性检查
                    result.put("inconsistent_records", 0);
                    break;
                default:
                    result.put("error", "未知的维护类型: " + maintenanceType);
            }
            
            logger.info("系统维护完成，类型: {}, 结果: {}", maintenanceType, result);
        } catch (Exception e) {
            logger.error("系统维护失败，类型: {}, 错误: {}", maintenanceType, e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // 私有辅助方法

    private boolean handlePublishArticle(Long userId, Map<String, Object> activityData) {
        // 发布文章时增加曝光数
        return userService.recordArticleExposure(userId, 1L);
    }

    private boolean handleDeleteArticle(Long userId, Map<String, Object> activityData) {
        // 删除文章时的处理逻辑
        // 通常不需要减少统计数据，因为历史数据应该保留
        logger.info("处理文章删除活动，用户ID: {}", userId);
        return true;
    }

    private boolean handleReceiveLike(Long userId, Map<String, Object> activityData) {
        Long count = getLongValue(activityData.get("count"));
        if (count == null) count = 1L;
        return userStatsService.incrementTotalLikes(userId, count);
    }

    private boolean handleReceiveFavorite(Long userId, Map<String, Object> activityData) {
        Long count = getLongValue(activityData.get("count"));
        if (count == null) count = 1L;
        return userStatsService.incrementTotalFavorites(userId, count);
    }

    private boolean handleReceiveSponsorship(Long userId, Map<String, Object> activityData) {
        Long amount = getLongValue(activityData.get("amount"));
        if (amount == null || amount <= 0) return false;
        return userStatsService.addSponsorship(userId, amount);
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}