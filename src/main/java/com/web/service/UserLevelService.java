package com.web.service;

import com.web.model.User;
import java.util.List;
import java.util.Map;

/**
 * 用户等级服务接口
 * 处理用户等级管理和自动升级逻辑
 */
public interface UserLevelService {

    /**
     * 获取用户当前等级
     * @param userId 用户ID
     * @return 用户等级
     */
    int getUserLevel(Long userId);

    /**
     * 设置用户等级
     * @param userId 用户ID
     * @param level 新等级
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean setUserLevel(Long userId, int level, Long operatorId);

    /**
     * 检查用户是否可以升级到指定等级
     * @param userId 用户ID
     * @param targetLevel 目标等级
     * @return 是否可以升级
     */
    boolean canUpgradeTo(Long userId, int targetLevel);

    /**
     * 自动检查并升级用户等级
     * @param userId 用户ID
     * @return 升级结果，包含新等级信息
     */
    Map<String, Object> checkAndUpgradeUserLevel(Long userId);

    /**
     * 获取用户统计数据用于等级评估
     * @param userId 用户ID
     * @return 用户统计数据
     */
    Map<String, Object> getUserStats(Long userId);

    /**
     * 获取用户等级升级进度
     * @param userId 用户ID
     * @return 升级进度信息
     */
    Map<String, Object> getUpgradeProgress(Long userId);

    /**
     * 获取用户的所有权限
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 检查用户是否有特定权限
     * @param userId 用户ID
     * @param permission 权限名称
     * @return 是否有权限
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * 批量更新用户等级（管理员功能）
     * @param userIds 用户ID列表
     * @param level 目标等级
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    Map<String, Object> batchUpdateUserLevels(List<Long> userIds, int level, Long operatorId);

    /**
     * 获取等级分布统计
     * @return 等级统计信息
     */
    Map<String, Object> getLevelStatistics();

    /**
     * 获取用户等级历史记录
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 等级变更记录
     */
    Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize);

    /**
     * 记录等级变更历史
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @param reason 变更原因
     * @param operatorId 操作者ID
     * @return 记录结果
     */
    boolean recordLevelChange(Long userId, int oldLevel, int newLevel, String reason, Long operatorId);
}