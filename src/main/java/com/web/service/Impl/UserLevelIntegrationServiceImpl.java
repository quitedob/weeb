package com.web.service.impl;

import com.web.service.RolePermissionService;
import com.web.service.UserLevelHistoryService;
import com.web.service.UserLevelIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户等级集成服务实现
 * 整合等级变更、历史记录和角色同步的完整流程
 */
@Slf4j
@Service
public class UserLevelIntegrationServiceImpl implements UserLevelIntegrationService {

    @Autowired
    private UserLevelHistoryService userLevelHistoryService;

    @Autowired
    private RolePermissionService rolePermissionService;

    /**
     * 处理用户等级变更的完整流程
     * 包括：记录历史、同步角色、返回结果
     * 
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @param changeReason 变更原因
     * @param changeType 变更类型 (1: 系统自动, 2: 管理员操作, 3: 用户行为触发)
     * @param operatorId 操作者ID（可选）
     * @param ipAddress IP地址（可选）
     * @param userAgent 用户代理（可选）
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleLevelChange(Long userId, Integer oldLevel, Integer newLevel,
                                                 String changeReason, Integer changeType,
                                                 Long operatorId, String ipAddress, String userAgent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始处理用户等级变更: userId={}, oldLevel={}, newLevel={}, changeType={}",
                    userId, oldLevel, newLevel, changeType);

            // 1. 记录等级变更历史
            boolean historyRecorded = userLevelHistoryService.recordLevelChange(
                    userId, oldLevel, newLevel, changeReason, changeType,
                    operatorId, ipAddress, userAgent);

            if (!historyRecorded) {
                log.error("记录等级变更历史失败: userId={}", userId);
                result.put("success", false);
                result.put("error", "记录历史失败");
                return result;
            }

            // 2. 同步用户角色
            Map<String, Object> roleSyncResult = rolePermissionService.syncUserRolesOnLevelChange(
                    userId, oldLevel, newLevel);

            // 3. 构建返回结果
            result.put("success", true);
            result.put("userId", userId);
            result.put("oldLevel", oldLevel);
            result.put("newLevel", newLevel);
            result.put("changeReason", changeReason);
            result.put("changeType", changeType);
            result.put("historyRecorded", true);
            result.put("roleSyncResult", roleSyncResult);
            result.put("message", "等级变更处理完成");

            log.info("用户等级变更处理完成: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel);

            return result;

        } catch (Exception e) {
            log.error("处理用户等级变更失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            result.put("success", false);
            result.put("error", "处理失败: " + e.getMessage());
            throw e; // 抛出异常以触发事务回滚
        }
    }

    /**
     * 批量处理用户等级变更
     * 
     * @param levelChanges 等级变更列表
     * @return 批量处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchHandleLevelChanges(java.util.List<Map<String, Object>> levelChanges) {
        Map<String, Object> batchResult = new HashMap<>();
        java.util.List<Map<String, Object>> successResults = new java.util.ArrayList<>();
        java.util.List<Map<String, Object>> failedResults = new java.util.ArrayList<>();

        for (Map<String, Object> change : levelChanges) {
            try {
                Long userId = ((Number) change.get("userId")).longValue();
                Integer oldLevel = (Integer) change.get("oldLevel");
                Integer newLevel = (Integer) change.get("newLevel");
                String changeReason = (String) change.get("changeReason");
                Integer changeType = (Integer) change.get("changeType");
                Long operatorId = change.containsKey("operatorId") ? 
                        ((Number) change.get("operatorId")).longValue() : null;

                Map<String, Object> result = handleLevelChange(
                        userId, oldLevel, newLevel, changeReason, changeType,
                        operatorId, null, null);

                if ((Boolean) result.get("success")) {
                    successResults.add(result);
                } else {
                    failedResults.add(result);
                }

            } catch (Exception e) {
                Map<String, Object> failedResult = new HashMap<>();
                failedResult.put("change", change);
                failedResult.put("error", e.getMessage());
                failedResults.add(failedResult);
                log.error("批量处理等级变更失败: change={}", change, e);
            }
        }

        batchResult.put("totalCount", levelChanges.size());
        batchResult.put("successCount", successResults.size());
        batchResult.put("failedCount", failedResults.size());
        batchResult.put("successResults", successResults);
        batchResult.put("failedResults", failedResults);

        log.info("批量处理等级变更完成: total={}, success={}, failed={}",
                levelChanges.size(), successResults.size(), failedResults.size());

        return batchResult;
    }

    /**
     * 获取用户等级变更的完整信息
     * 包括历史记录、当前角色、权限等
     * 
     * @param userId 用户ID
     * @return 完整信息
     */
    public Map<String, Object> getUserLevelCompleteInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();

        try {
            // 获取当前等级
            Integer currentLevel = userLevelHistoryService.getCurrentLevel(userId);

            // 获取最近的等级变更记录
            java.util.List<com.web.model.UserLevelHistory> recentHistory = 
                    userLevelHistoryService.getRecentHistory(userId, 10);

            // 获取用户角色
            java.util.List<com.web.model.Role> userRoles = 
                    rolePermissionService.getUserRoles(userId);

            // 获取用户所有权限
            java.util.Set<String> allPermissions = 
                    rolePermissionService.getAllUserPermissions(userId);

            // 获取等级统计
            Map<String, Object> stats = userLevelHistoryService.getUserLevelStats(userId, 30);

            info.put("userId", userId);
            info.put("currentLevel", currentLevel);
            info.put("recentHistory", recentHistory);
            info.put("userRoles", userRoles);
            info.put("allPermissions", allPermissions);
            info.put("stats", stats);

            return info;

        } catch (Exception e) {
            log.error("获取用户等级完整信息失败: userId={}", userId, e);
            info.put("error", "获取信息失败: " + e.getMessage());
            return info;
        }
    }

    /**
     * 验证用户等级变更的合法性
     * 
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @return 验证结果
     */
    public Map<String, Object> validateLevelChange(Long userId, Integer oldLevel, Integer newLevel) {
        Map<String, Object> validation = new HashMap<>();

        try {
            // 获取用户当前等级
            Integer currentLevel = userLevelHistoryService.getCurrentLevel(userId);

            // 验证原等级是否匹配
            if (currentLevel != null && !currentLevel.equals(oldLevel)) {
                validation.put("valid", false);
                validation.put("error", "原等级不匹配，当前等级为: " + currentLevel);
                return validation;
            }

            // 验证等级范围
            if (newLevel < 1 || newLevel > 10) {
                validation.put("valid", false);
                validation.put("error", "新等级超出有效范围 (1-10)");
                return validation;
            }

            // 验证等级变化是否合理（可选）
            if (oldLevel != null && Math.abs(newLevel - oldLevel) > 3) {
                validation.put("valid", false);
                validation.put("warning", "等级变化幅度过大，请确认");
            }

            validation.put("valid", true);
            validation.put("message", "等级变更验证通过");
            return validation;

        } catch (Exception e) {
            log.error("验证等级变更失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            validation.put("valid", false);
            validation.put("error", "验证失败: " + e.getMessage());
            return validation;
        }
    }
}
