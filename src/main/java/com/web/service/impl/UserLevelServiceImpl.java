package com.web.service.impl;

import com.web.constant.UserLevel;
import com.web.constant.UserType;
import com.web.exception.WeebException;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.UserLevelHistoryService;
import com.web.service.UserLevelService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserLevelServiceImpl implements UserLevelService {
    @Autowired private UserMapper userMapper;
    @Autowired private UserLevelHistoryService userLevelHistoryService;
    @Autowired(required = false) private HttpServletRequest request;

    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "ROLE_ADMIN", "READ_ANY", "UPDATE_ANY", "DELETE_ANY", "MANAGE_USERS", "SYSTEM_CONFIG",
            "VIEW_LOGS", "MODERATE_CONTENT", "DELETE_ANY_ARTICLE", "BAN_USERS", "VIEW_REPORTS");

    @Override
    public int getUserLevel(Long userId) {
        return userLevelHistoryService.getCurrentLevel(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setUserLevel(Long userId, int level, Long operatorId) {
        requireActiveUser(userId);
        int oldLevel = getUserLevel(userId);
        authorizeChange(userId, oldLevel, level, operatorId);
        if (oldLevel == level) return true;
        return recordHistory(userId, oldLevel, level,
                operatorId == null ? "系统自动升级" : "管理员调整等级", operatorId);
    }

    private void authorizeChange(Long userId, int currentLevel, int targetLevel, Long operatorId) {
        if (!UserLevel.isValidLevel(targetLevel)) throw new WeebException("等级必须在0到8之间");
        if (operatorId != null) {
            if (!UserType.ADMIN.equals(UserType.normalize(requireActiveUser(operatorId).getType()))) {
                throw new WeebException("需要管理员权限");
            }
        } else if (currentLevel != targetLevel) {
            if (!meetsAutomaticRequirements(currentLevel, targetLevel, getUserStats(userId))) {
                throw new WeebException("尚未满足升级条件");
            }
        }
    }

    private boolean meetsAutomaticRequirements(int currentLevel, int targetLevel, Map<String, Object> stats) {
        if (targetLevel <= currentLevel || targetLevel > UserLevel.LEVEL_CONTENT_CREATOR) return false;
        for (int level = currentLevel + 1; level <= targetLevel; level++) {
            if (!UserLevel.canUpgradeTo(level - 1, level, stats)) return false;
        }
        return true;
    }

    @Override
    public boolean canUpgradeTo(Long userId, int targetLevel) {
        requireActiveUser(userId);
        return meetsAutomaticRequirements(getUserLevel(userId), targetLevel, getUserStats(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkAndUpgradeUserLevel(Long userId) {
        requireActiveUser(userId);
        int currentLevel = getUserLevel(userId);
        Map<String, Object> stats = getUserStats(userId);
        int candidate = currentLevel;
        for (int level = currentLevel + 1; level <= UserLevel.LEVEL_CONTENT_CREATOR; level++) {
            if (!UserLevel.canUpgradeTo(level - 1, level, stats)) break;
            candidate = level;
        }
        boolean upgraded = candidate > currentLevel && setUserLevel(userId, candidate, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentLevel", currentLevel);
        result.put("currentLevelName", UserLevel.getLevelName(currentLevel));
        result.put("userStats", stats);
        result.put("upgraded", upgraded);
        result.put("newLevel", upgraded ? candidate : currentLevel);
        result.put("newLevelName", UserLevel.getLevelName(upgraded ? candidate : currentLevel));
        result.put("message", upgraded ? "等级升级成功" : "等级未变更");
        if (currentLevel < UserLevel.LEVEL_SUPER_ADMIN) {
            result.put("nextLevel", currentLevel + 1);
            result.put("nextLevelName", UserLevel.getLevelName(currentLevel + 1));
            result.put("requirements", UserLevel.getLevelRequirements(currentLevel + 1));
        }
        return result;
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        User user = requireUser(userId);
        long articles = userMapper.countUserArticles(userId);
        long messages = userMapper.countUserMessages(userId);
        long likes = userMapper.countUserLikes(userId);
        long followers = userMapper.countUserFollowers(userId);
        long views = userMapper.countUserArticleViews(userId);
        long comments = userMapper.countUserComments(userId);
        long loginDays = userMapper.countUserLoginDays(userId);
        double engagement = views > 0 ? ((double) likes + comments) / views : 0.0;
        double reputation = articles * 2.0 + messages / 10 + likes / 5 + followers * 3.0;
        reputation += loginDays >= 365 ? 100 : loginDays >= 180 ? 50 : loginDays >= 90 ? 20 : 0;
        reputation += engagement >= 0.1 ? 50 : engagement >= 0.05 ? 20 : 0;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("registrationDate", user.getRegistrationDate());
        stats.put("lastLoginDate", user.getLoginTime());
        stats.put("articleCount", articles);
        stats.put("messageCount", messages);
        stats.put("likeCount", likes);
        stats.put("followerCount", followers);
        stats.put("viewCount", views);
        stats.put("commentCount", comments);
        stats.put("loginDays", loginDays);
        stats.put("engagementRate", engagement);
        stats.put("reputation", (long) reputation);
        // Payment and review approval are never inferred from activity or a client profile.
        return stats;
    }

    @Override
    public Map<String, Object> getUpgradeProgress(Long userId) {
        User user = requireUser(userId);
        int current = getUserLevel(userId);
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("currentLevel", current);
        progress.put("currentLevelName", UserLevel.getLevelName(current));
        if (current == UserLevel.LEVEL_SUPER_ADMIN) {
            progress.put("canUpgrade", false);
            progress.put("overallProgress", 100.0);
            progress.put("progressDetails", Map.of());
            progress.put("message", "已达到最高等级");
            return progress;
        }
        int next = current + 1;
        Map<String, Object> requirements = UserLevel.getLevelRequirements(next);
        Map<String, Object> stats = getUserStats(userId);
        Map<String, Double> details = new LinkedHashMap<>();
        for (var requirement : requirements.entrySet()) {
            String key = requirement.getKey();
            if (requirement.getValue() instanceof Number required) {
                Object value = "minLevel".equals(key) ? current : stats.get(UserLevel.statKeyForRequirement(key));
                double actual = value instanceof Number number ? number.doubleValue() : 0;
                double percent = Math.max(0, Math.min(100, actual / required.doubleValue() * 100));
                String displayKey = switch (key) {
                    case "minArticles" -> "articles";
                    case "minMessages" -> "messages";
                    case "minLikes" -> "likes";
                    case "minFollowers" -> "followers";
                    case "minViews" -> "views";
                    default -> UserLevel.statKeyForRequirement(key);
                };
                details.put(displayKey, percent);
            } else if (Boolean.TRUE.equals(requirement.getValue())) {
                details.put(key, Boolean.TRUE.equals(stats.get(key)) ? 100.0 : 0.0);
            }
        }
        progress.put("nextLevel", next);
        progress.put("nextLevelName", UserLevel.getLevelName(next));
        progress.put("requirements", requirements);
        progress.put("userStats", stats);
        progress.put("progressDetails", details);
        progress.put("overallProgress", details.values().stream().mapToDouble(Double::doubleValue).average().orElse(100));
        progress.put("canUpgrade", Integer.valueOf(1).equals(user.getStatus())
                && meetsAutomaticRequirements(current, next, stats));
        return progress;
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        User user = requireUser(userId);
        if (!Integer.valueOf(1).equals(user.getStatus())) return List.of();
        List<String> permissions = new ArrayList<>(UserLevel.getLevelPermissions(getUserLevel(userId)));
        if (UserType.ADMIN.equals(UserType.normalize(user.getType()))) permissions.addAll(ADMIN_PERMISSIONS);
        return permissions;
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        return userId != null && permission != null && getUserPermissions(userId).contains(permission.trim());
    }

    @Override
    public Map<String, Object> batchUpdateUserLevels(List<Long> userIds, int level, Long operatorId) {
        if (userIds == null || userIds.size() > 100 || operatorId == null) throw new WeebException("无效的批量更新请求");
        if (!UserType.ADMIN.equals(UserType.normalize(requireActiveUser(operatorId).getType()))) throw new WeebException("需要管理员权限");
        List<Map<String, Object>> updated = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                int old = getUserLevel(userId);
                if (setUserLevel(userId, level, operatorId)) {
                    updated.add(Map.of("userId", userId, "oldLevel", old, "newLevel", level,
                            "oldLevelName", UserLevel.getLevelName(old), "newLevelName", UserLevel.getLevelName(level)));
                } else failed.add(Map.of("userId", userId, "error", "更新失败"));
            } catch (RuntimeException error) {
                Map<String, Object> failure = new HashMap<>();
                failure.put("userId", userId);
                failure.put("error", "等级更新失败");
                failed.add(failure);
            }
        }
        return Map.of("successCount", updated.size(), "failCount", failed.size(), "updatedUsers", updated,
                "failedUsers", failed, "totalProcessed", userIds.size());
    }

    @Override
    public Map<String, Object> getLevelStatistics() {
        List<Map<String, Object>> levels = new ArrayList<>();
        long total = 0;
        double weightedTotal = 0;
        for (int level = UserLevel.LEVEL_NEW_USER; level <= UserLevel.LEVEL_SUPER_ADMIN; level++) {
            int count = userMapper.countUsersByLevel(level);
            total += count;
            weightedTotal += (double) count * level;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("level", level);
            row.put("name", UserLevel.getLevelName(level));
            row.put("color", UserLevel.getLevelColor(level));
            row.put("count", count);
            levels.add(row);
        }
        for (Map<String, Object> row : levels) {
            double percent = total == 0 ? 0 : ((Number) row.get("count")).doubleValue() * 100 / total;
            row.put("percentage", String.format(Locale.ROOT, "%.1f%%", percent));
        }
        return Map.of("levelStatistics", levels, "totalUsers", total,
                "averageLevel", total == 0 ? (double) UserLevel.LEVEL_BASIC_USER : weightedTotal / total);
    }

    @Override
    public Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>(userLevelHistoryService.getUserLevelHistory(userId, page, pageSize));
        result.put("records", result.get("list"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordLevelChange(Long userId, int oldLevel, int newLevel, String reason, Long operatorId) {
        requireActiveUser(userId);
        int current = getUserLevel(userId);
        if (oldLevel != current) throw new WeebException("等级已变更，请刷新后重试");
        authorizeChange(userId, current, newLevel, operatorId);
        return recordHistory(userId, oldLevel, newLevel, reason, operatorId);
    }

    private boolean recordHistory(Long userId, int oldLevel, int newLevel, String reason, Long operatorId) {
        String ip = null;
        String agent = null;
        if (request != null) {
            try {
                ip = request.getRemoteAddr();
                agent = request.getHeader("User-Agent");
            } catch (IllegalStateException ignored) {
                // Scheduled upgrades have no HTTP request.
            }
        }
        return userLevelHistoryService.recordLevelChange(userId, oldLevel, newLevel, reason,
                operatorId == null ? 1 : 2, operatorId, ip, agent);
    }

    private User requireUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) throw new WeebException("用户不存在");
        return user;
    }

    private User requireActiveUser(Long userId) {
        User user = requireUser(userId);
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new WeebException("用户账号已被禁用");
        return user;
    }
}
