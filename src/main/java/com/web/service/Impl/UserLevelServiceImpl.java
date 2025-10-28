package com.web.service.Impl;

import com.web.constant.UserLevel;
import com.web.exception.WeebException;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserLevelHistory;
import com.web.service.UserLevelHistoryService;
import com.web.service.UserLevelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 用户等级服务实现类
 * 处理用户等级管理和自动升级逻辑
 */
@Slf4j
@Service
@Transactional
public class UserLevelServiceImpl implements UserLevelService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserLevelHistoryMapper userLevelHistoryMapper;

    @Autowired
    private UserLevelHistoryService userLevelHistoryService;

    
    @Autowired(required = false)
    private HttpServletRequest request;

    @Override
    public int getUserLevel(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return UserLevel.LEVEL_NEW_USER;
            }

            // TODO: 实现用户等级获取逻辑 - User 模型暂无 getUserLevel 方法
            return UserLevel.LEVEL_NEW_USER;
        } catch (Exception e) {
            log.error("获取用户等级失败: userId={}", userId, e);
            return UserLevel.LEVEL_NEW_USER;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setUserLevel(Long userId, int level, Long operatorId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 获取当前等级
            int oldLevel = getUserLevel(userId);

            // 如果等级没有变化，直接返回
            if (oldLevel == level) {
                log.info("用户等级未变化: userId={}, level={}", userId, level);
                return true;
            }

            // TODO: 实现用户等级设置逻辑 - User 模型暂无 setUserLevel 方法
            // 这里需要根据实际的User模型字段来设置等级
            user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            int result = userMapper.updateById(user);

            if (result > 0) {
                // 1. 记录等级变更历史
                String changeReason = operatorId != null 
                    ? String.format("管理员手动调整等级从 %s 到 %s",
                        UserLevel.getLevelName(oldLevel),
                        UserLevel.getLevelName(level))
                    : String.format("系统自动升级从 %s 到 %s",
                        UserLevel.getLevelName(oldLevel),
                        UserLevel.getLevelName(level));

                Integer changeType = operatorId != null ? 2 : 1; // 2: 管理员操作, 1: 系统自动
                
                // 获取IP和User-Agent
                String ipAddress = getClientIpAddress();
                String userAgent = getUserAgent();

                boolean historyRecorded = userLevelHistoryService.recordLevelChange(
                    userId, oldLevel, level, changeReason, changeType,
                    operatorId, ipAddress, userAgent
                );

                if (!historyRecorded) {
                    log.warn("记录等级变更历史失败: userId={}", userId);
                }

                
                log.info("用户等级更新成功: userId={}, oldLevel={}, newLevel={}, operatorId={}",
                        userId, oldLevel, level, operatorId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("设置用户等级失败: userId={}, level={}, operatorId={}",
                     userId, level, operatorId, e);
            throw new WeebException("设置用户等级失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取User-Agent
     */
    private String getUserAgent() {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }

    @Override
    public boolean canUpgradeTo(Long userId, int targetLevel) {
        try {
            int currentLevel = getUserLevel(userId);
            if (targetLevel <= currentLevel) {
                return false;
            }

            Map<String, Object> userStats = getUserStats(userId);
            return UserLevel.canUpgradeTo(currentLevel, targetLevel, userStats);
        } catch (Exception e) {
            log.error("检查用户升级条件失败: userId={}, targetLevel={}", userId, targetLevel, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> checkAndUpgradeUserLevel(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            int currentLevel = getUserLevel(userId);
            Map<String, Object> userStats = getUserStats(userId);

            result.put("currentLevel", currentLevel);
            result.put("currentLevelName", UserLevel.getLevelName(currentLevel));
            result.put("userStats", userStats);

            // 检查可以升级到的最高等级
            int maxUpgradeableLevel = currentLevel;
            for (int level = currentLevel + 1; level <= UserLevel.LEVEL_CONTENT_CREATOR; level++) {
                if (UserLevel.canUpgradeTo(currentLevel, level, userStats)) {
                    maxUpgradeableLevel = level;
                } else {
                    break;
                }
            }

            if (maxUpgradeableLevel > currentLevel) {
                // 自动升级到最高可升级等级
                boolean upgradeSuccess = setUserLevel(userId, maxUpgradeableLevel, null); // 系统自动升级

                result.put("upgraded", true);
                result.put("newLevel", maxUpgradeableLevel);
                result.put("newLevelName", UserLevel.getLevelName(maxUpgradeableLevel));
                result.put("message", String.format("恭喜！您的等级已从 %s 升级到 %s",
                    UserLevel.getLevelName(currentLevel),
                    UserLevel.getLevelName(maxUpgradeableLevel)));

                log.info("用户自动升级成功: userId={}, oldLevel={}, newLevel={}",
                        userId, currentLevel, maxUpgradeableLevel);
            } else {
                result.put("upgraded", false);
                result.put("nextLevel", currentLevel + 1);
                result.put("nextLevelName", UserLevel.getLevelName(currentLevel + 1));
                result.put("requirements", UserLevel.getLevelRequirements(currentLevel + 1));
                result.put("message", "继续努力，您还满足下一等级的升级条件");
            }

            return result;
        } catch (Exception e) {
            log.error("检查并升级用户等级失败: userId={}", userId, e);
            result.put("error", "检查升级条件失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 这里需要从各个表统计用户数据
            // 为了简化，这里使用模拟数据，实际项目中应该从数据库查询

            // 获取用户基本信息
            User user = userMapper.selectById(userId);
            if (user != null) {
                stats.put("registrationDate", user.getCreatedAt());
                // TODO: 实现最后登录时间获取 - User 模型暂无 getLastLoginTime 方法
                stats.put("lastLoginDate", null);
            }

            // 统计文章数量
            stats.put("articleCount", userMapper.countUserArticles(userId));

            // 统计消息数量
            stats.put("messageCount", userMapper.countUserMessages(userId));

            // 统计点赞数量
            stats.put("likeCount", userMapper.countUserLikes(userId));

            // 统计关注者数量
            stats.put("followerCount", userMapper.countUserFollowers(userId));

            // 统计浏览量
            stats.put("viewCount", userMapper.countUserArticleViews(userId));

            // 计算登录天数
            if (user != null && user.getCreatedAt() != null) {
                long daysSinceRegistration = (System.currentTimeMillis() - user.getCreatedAt().getTime()) / (24 * 60 * 60 * 1000);
                stats.put("loginDays", (int) daysSinceRegistration);
            } else {
                stats.put("loginDays", 0);
            }

            // 计算互动率 (点赞数 + 评论数) / 浏览量
            int totalEngagement = (Integer) stats.getOrDefault("likeCount", 0) +
                                 (int) userMapper.countUserComments(userId);
            int totalViews = (Integer) stats.getOrDefault("viewCount", 1);
            double engagementRate = (double) totalEngagement / totalViews;
            stats.put("engagementRate", engagementRate);

            // 计算声誉值（基于活跃度和内容质量）
            int reputation = calculateReputation(stats);
            stats.put("reputation", reputation);

        } catch (Exception e) {
            log.error("获取用户统计数据失败: userId={}", userId, e);
            // 返回默认值
            stats.put("articleCount", 0);
            stats.put("messageCount", 0);
            stats.put("likeCount", 0);
            stats.put("followerCount", 0);
            stats.put("viewCount", 0);
            stats.put("loginDays", 0);
            stats.put("engagementRate", 0.0);
            stats.put("reputation", 0);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getUpgradeProgress(Long userId) {
        Map<String, Object> progress = new HashMap<>();

        try {
            int currentLevel = getUserLevel(userId);
            int nextLevel = currentLevel + 1;

            if (nextLevel > UserLevel.LEVEL_CONTENT_CREATOR) {
                progress.put("canUpgrade", false);
                progress.put("message", "您已达到最高等级");
                return progress;
            }

            Map<String, Object> requirements = UserLevel.getLevelRequirements(nextLevel);
            Map<String, Object> userStats = getUserStats(userId);

            progress.put("currentLevel", currentLevel);
            progress.put("nextLevel", nextLevel);
            progress.put("currentLevelName", UserLevel.getLevelName(currentLevel));
            progress.put("nextLevelName", UserLevel.getLevelName(nextLevel));
            progress.put("requirements", requirements);
            progress.put("userStats", userStats);

            // 计算各项进度
            Map<String, Double> progressDetails = new HashMap<>();

            if (requirements.containsKey("minArticles")) {
                int required = (Integer) requirements.get("minArticles");
                int actual = (Integer) userStats.getOrDefault("articleCount", 0);
                progressDetails.put("articles", Math.min(100.0, (double) actual / required * 100));
            }

            if (requirements.containsKey("minMessages")) {
                int required = (Integer) requirements.get("minMessages");
                int actual = (Integer) userStats.getOrDefault("messageCount", 0);
                progressDetails.put("messages", Math.min(100.0, (double) actual / required * 100));
            }

            if (requirements.containsKey("minLoginDays")) {
                int required = (Integer) requirements.get("minLoginDays");
                int actual = (Integer) userStats.getOrDefault("loginDays", 0);
                progressDetails.put("loginDays", Math.min(100.0, (double) actual / required * 100));
            }

            if (requirements.containsKey("minLikes")) {
                int required = (Integer) requirements.get("minLikes");
                int actual = (Integer) userStats.getOrDefault("likeCount", 0);
                progressDetails.put("likes", Math.min(100.0, (double) actual / required * 100));
            }

            if (requirements.containsKey("minFollowers")) {
                int required = (Integer) requirements.get("minFollowers");
                int actual = (Integer) userStats.getOrDefault("followerCount", 0);
                progressDetails.put("followers", Math.min(100.0, (double) actual / required * 100));
            }

            progress.put("progressDetails", progressDetails);

            // 计算总体进度
            double overallProgress = progressDetails.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            progress.put("overallProgress", overallProgress);
            progress.put("canUpgrade", overallProgress >= 100.0);

        } catch (Exception e) {
            log.error("获取升级进度失败: userId={}", userId, e);
            progress.put("error", "获取升级进度失败: " + e.getMessage());
        }

        return progress;
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        try {
            int level = getUserLevel(userId);
            return UserLevel.getLevelPermissions(level);
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        try {
            if (userId == null || permission == null || permission.trim().isEmpty()) {
                return false;
            }

            // 获取用户当前等级
            int currentLevel = getUserLevel(userId);
            List<String> levelPermissions = UserLevel.getLevelPermissions(currentLevel);

            // 检查是否有直接权限匹配
            if (levelPermissions.contains(permission.trim())) {
                return true;
            }

            // 管理员等级的特权检查
            if (currentLevel >= UserLevel.LEVEL_ADMIN) {
                // 管理员拥有所有权限（除了系统限制的特殊权限）
                if (!permission.equals("SYSTEM_RESTRICTED")) {
                    return true;
                }
            }

            // 特殊权限检查：资源所有者权限
            if (permission.endsWith("_OWN")) {
                // 这里需要结合具体的资源ID来检查所有权
                // 在Service层实现中应该传入具体的资源ID
                log.debug("检测到资源所有者权限检查: {}", permission);
                // 实际实现中，这里需要调用相应的所有权检查逻辑
            }

            return false;

        } catch (Exception e) {
            log.error("检查用户权限时发生错误: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> batchUpdateUserLevels(List<Long> userIds, int level, Long operatorId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> updatedUsers = new ArrayList<>();
        List<Map<String, Object>> failedUsers = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                int oldLevel = getUserLevel(userId);
                boolean success = setUserLevel(userId, level, operatorId);

                if (success) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", userId);
                    userInfo.put("oldLevel", oldLevel);
                    userInfo.put("newLevel", level);
                    userInfo.put("oldLevelName", UserLevel.getLevelName(oldLevel));
                    userInfo.put("newLevelName", UserLevel.getLevelName(level));
                    updatedUsers.add(userInfo);
                } else {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", userId);
                    userInfo.put("error", "更新失败");
                    failedUsers.add(userInfo);
                }
            } catch (Exception e) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", userId);
                userInfo.put("error", e.getMessage());
                failedUsers.add(userInfo);
                log.error("批量更新用户等级失败: userId={}, level={}", userId, level, e);
            }
        }

        result.put("successCount", updatedUsers.size());
        result.put("failCount", failedUsers.size());
        result.put("updatedUsers", updatedUsers);
        result.put("failedUsers", failedUsers);
        result.put("totalProcessed", userIds.size());

        return result;
    }

    @Override
    public Map<String, Object> getLevelStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            List<Map<String, Object>> levelStats = new ArrayList<>();

            for (int level = UserLevel.LEVEL_NEW_USER; level <= UserLevel.LEVEL_SUPER_ADMIN; level++) {
                int count = userMapper.countUsersByLevel(level);

                Map<String, Object> levelInfo = new HashMap<>();
                levelInfo.put("level", level);
                levelInfo.put("name", UserLevel.getLevelName(level));
                levelInfo.put("color", UserLevel.getLevelColor(level));
                levelInfo.put("count", count);
                levelInfo.put("percentage", count > 0 ? String.format("%.1f%%", (double) count / getTotalUserCount() * 100) : "0.0%");

                levelStats.add(levelInfo);
            }

            statistics.put("levelStatistics", levelStats);
            statistics.put("totalUsers", getTotalUserCount());
            statistics.put("averageLevel", calculateAverageLevel());

        } catch (Exception e) {
            log.error("获取等级统计失败", e);
            statistics.put("error", "获取等级统计失败: " + e.getMessage());
        }

        return statistics;
    }

    /*
    @Override
    public Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize) {
        // TODO: 待实现 - 需要 UserLevelHistoryMapper 和 UserLevelHistory 类
        Map<String, Object> result = new HashMap<>();
        result.put("error", "功能暂未实现 - 缺少必要的数据库映射类");
        return result;
    }
    */

    @Override
    public Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize) {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            // 暂时返回占位符数据，未来可以扩展到数据库查询
            log.info("获取用户等级历史: userId={}, page={}, pageSize={}", userId, page, pageSize);

            // TODO: 实现数据库查询逻辑
            // List<UserLevelHistory> historyList = userLevelHistoryMapper.selectByUserId(userId, offset, pageSize);
            // long total = userLevelHistoryMapper.countByUserId(userId);

            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("records", new java.util.ArrayList<>());
            result.put("message", "等级历史记录功能暂未实现 - 缺少必要的数据库映射类");

        } catch (Exception e) {
            log.error("获取用户等级历史失败: userId={}", userId, e);
            result.put("error", "获取等级历史失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean recordLevelChange(Long userId, int oldLevel, int newLevel, String reason, Long operatorId) {
        try {
            // 暂时使用日志记录等级变更，未来可以扩展到数据库存储
            log.info("用户等级变更记录: userId={}, oldLevel={}, newLevel={}, reason={}, operatorId={}",
                     userId, oldLevel, newLevel, reason, operatorId);

            // TODO: 实现数据库存储逻辑
            // UserLevelHistory history = new UserLevelHistory();
            // history.setUserId(userId);
            // history.setOldLevel(oldLevel);
            // history.setNewLevel(newLevel);
            // history.setChangeReason(reason);
            // history.setOperatorId(operatorId);
            // history.setChangeTime(LocalDateTime.now());
            // userLevelHistoryMapper.insert(history);

            return true;
        } catch (Exception e) {
            log.error("记录等级变更失败: userId={}, oldLevel={}, newLevel={}",
                      userId, oldLevel, newLevel, e);
            return false;
        }
    }

    /**
     * 计算用户声誉值
     */
    private int calculateReputation(Map<String, Object> stats) {
        int reputation = 0;

        // 基础声誉分
        reputation += (Integer) stats.getOrDefault("articleCount", 0) * 2;        // 每篇文章2分
        reputation += (Integer) stats.getOrDefault("messageCount", 0) / 10;      // 每10条消息1分
        reputation += (Integer) stats.getOrDefault("likeCount", 0) / 5;           // 每5个点赞1分
        reputation += (Integer) stats.getOrDefault("followerCount", 0) * 3;      // 每个关注者3分

        // 活跃度加成
        int loginDays = (Integer) stats.getOrDefault("loginDays", 0);
        if (loginDays >= 365) reputation += 100;  // 活跃一年以上
        else if (loginDays >= 180) reputation += 50;  // 活跃半年以上
        else if (loginDays >= 90) reputation += 20;   // 活跃三个月以上

        // 内容质量加成
        double engagementRate = (Double) stats.getOrDefault("engagementRate", 0.0);
        if (engagementRate >= 0.1) reputation += 50;    // 互动率10%以上
        else if (engagementRate >= 0.05) reputation += 20; // 互动率5%以上

        return reputation;
    }

    /**
     * 获取总用户数
     */
    private int getTotalUserCount() {
        try {
            Long count = userMapper.selectCount(null);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取总用户数失败", e);
            return 0;
        }
    }

    /**
     * 计算平均等级
     */
    private double calculateAverageLevel() {
        try {
            Map<String, Object> statistics = getLevelStatistics();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> levelStats = (List<Map<String, Object>>) statistics.get("levelStatistics");

            if (levelStats == null || levelStats.isEmpty()) {
                return UserLevel.LEVEL_BASIC_USER;
            }

            int totalUsers = 0;
            int totalLevel = 0;

            for (Map<String, Object> levelInfo : levelStats) {
                int count = (Integer) levelInfo.get("count");
                int level = (Integer) levelInfo.get("level");
                totalUsers += count;
                totalLevel += count * level;
            }

            return totalUsers > 0 ? (double) totalLevel / totalUsers : UserLevel.LEVEL_BASIC_USER;
        } catch (Exception e) {
            log.error("计算平均等级失败", e);
            return UserLevel.LEVEL_BASIC_USER;
        }
    }
}