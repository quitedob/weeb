package com.web.service;

import com.web.model.UserLevelHistory;
import com.web.vo.userlevel.UserLevelHistoryQueryVo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户等级变更历史服务接口
 * 负责记录和查询用户等级变化历史
 */
public interface UserLevelHistoryService {

    /**
     * 记录用户等级变更
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @param changeReason 变更原因
     * @param changeType 变更类型 (1: 系统自动, 2: 管理员操作, 3: 用户行为触发)
     * @param operatorId 操作者ID（可选）
     * @param ipAddress IP地址（可选）
     * @param userAgent 用户代理（可选）
     * @return 是否记录成功
     */
    boolean recordLevelChange(Long userId, Integer oldLevel, Integer newLevel, 
                            String changeReason, Integer changeType, 
                            Long operatorId, String ipAddress, String userAgent);

    /**
     * 根据ID查询等级变更记录
     * @param id 记录ID
     * @return 等级变更记录
     */
    UserLevelHistory getById(Long id);

    /**
     * 根据用户ID查询等级变更历史
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 等级变更历史列表
     */
    Map<String, Object> getUserLevelHistory(Long userId, int page, int pageSize);

    /**
     * 查询等级变更历史（支持多条件查询）
     * @param queryVo 查询条件
     * @return 查询结果
     */
    Map<String, Object> queryLevelHistory(UserLevelHistoryQueryVo queryVo);

    /**
     * 获取用户最近的等级变更记录
     * @param userId 用户ID
     * @param limit 返回数量
     * @return 等级变更记录列表
     */
    List<UserLevelHistory> getRecentHistory(Long userId, int limit);

    /**
     * 获取用户当前等级
     * @param userId 用户ID
     * @return 当前等级
     */
    Integer getCurrentLevel(Long userId);

    /**
     * 获取用户等级变更统计
     * @param userId 用户ID
     * @param days 统计天数
     * @return 统计信息
     */
    Map<String, Object> getUserLevelStats(Long userId, int days);

    /**
     * 获取等级提升记录
     * @param userId 用户ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param limit 返回数量
     * @return 等级提升记录列表
     */
    List<UserLevelHistory> getLevelUpRecords(Long userId, LocalDateTime startTime, 
                                            LocalDateTime endTime, int limit);

    /**
     * 获取等级降低记录
     * @param userId 用户ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param limit 返回数量
     * @return 等级降低记录列表
     */
    List<UserLevelHistory> getLevelDownRecords(Long userId, LocalDateTime startTime, 
                                              LocalDateTime endTime, int limit);

    /**
     * 统计用户等级变更次数
     * @param userId 用户ID
     * @return 变更次数
     */
    long countUserLevelChanges(Long userId);

    /**
     * 清理过期的等级变更记录
     * @param beforeTime 清理时间点
     * @return 清理的记录数
     */
    int cleanupExpiredRecords(LocalDateTime beforeTime);

    /**
     * 批量记录等级变更
     * @param histories 等级变更记录列表
     * @return 是否记录成功
     */
    boolean batchRecordLevelChanges(List<UserLevelHistory> histories);

    /**
     * 删除用户的等级变更记录
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUserHistory(Long userId);

    /**
     * 更新记录状态
     * @param id 记录ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);
}
