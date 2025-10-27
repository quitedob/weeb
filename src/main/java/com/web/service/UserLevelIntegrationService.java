package com.web.service;

import java.util.List;
import java.util.Map;

/**
 * 用户等级集成服务接口
 * 整合等级变更、历史记录和角色同步的完整流程
 */
public interface UserLevelIntegrationService {

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
    Map<String, Object> handleLevelChange(Long userId, Integer oldLevel, Integer newLevel,
                                         String changeReason, Integer changeType,
                                         Long operatorId, String ipAddress, String userAgent);

    /**
     * 批量处理用户等级变更
     *
     * @param levelChanges 等级变更列表
     * @return 批量处理结果
     */
    Map<String, Object> batchHandleLevelChanges(List<Map<String, Object>> levelChanges);

    /**
     * 获取用户等级完整信息
     *
     * @param userId 用户ID
     * @return 用户等级和角色信息
     */
    Map<String, Object> getUserLevelCompleteInfo(Long userId);

    /**
     * 验证等级变更的合法性
     *
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @return 验证结果
     */
    Map<String, Object> validateLevelChange(Long userId, Integer oldLevel, Integer newLevel);
}
