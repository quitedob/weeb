package com.web.service;

/**
 * 用户统计数据同步服务
 * 负责User和UserStats表之间的数据同步，确保一致性
 */
public interface UserStatsSyncService {

    /**
     * 同步用户等级信息从User表到UserStats表
     * 确保UserStats表中的userLevel字段与User表保持一致
     *
     * @param userId 用户ID
     */
    void syncUserLevelToStats(Long userId);

    /**
     * 批量同步所有用户的等级信息
     * 用于系统启动时的数据一致性检查和修复
     */
    void syncAllUserLevels();

    /**
     * 更新用户等级并同步到两个表
     * 在一个事务中同时更新User.userLevel和UserStats.userLevel
     *
     * @param userId 用户ID
     * @param newLevel 新的用户等级
     */
    void updateUserLevel(Long userId, Integer newLevel);

    /**
     * 检查用户等级数据一致性
     * 比较User表和UserStats表中的userLevel字段
     *
     * @param userId 用户ID
     * @return 是否一致
     */
    boolean isUserLevelConsistent(Long userId);
}