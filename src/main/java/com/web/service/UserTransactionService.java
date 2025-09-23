package com.web.service;

import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;

import java.util.List;
import java.util.Map;

/**
 * 用户事务服务接口
 * 处理需要跨越user和user_stats表的复杂事务操作
 * 确保数据一致性和事务完整性
 */
public interface UserTransactionService {

    /**
     * 批量用户注册事务
     * 同时创建多个用户的基本信息和统计数据记录
     * @param users 用户列表
     * @return 创建成功的用户完整信息列表
     */
    List<UserWithStats> batchCreateUsers(List<User> users);

    /**
     * 用户数据迁移事务
     * 将用户统计数据从user表迁移到user_stats表
     * @param userId 用户ID
     * @param legacyStats 从旧表结构中提取的统计数据
     * @return 是否迁移成功
     */
    boolean migrateUserStats(Long userId, Map<String, Object> legacyStats);

    /**
     * 批量用户数据迁移事务
     * 批量处理用户统计数据迁移
     * @param userStatsMap 用户ID到统计数据的映射
     * @return 成功迁移的用户数量
     */
    int batchMigrateUserStats(Map<Long, Map<String, Object>> userStatsMap);

    /**
     * 用户活动综合更新事务
     * 处理用户的一次活动可能影响多个统计指标的情况
     * 例如：发布文章可能同时影响文章数、曝光数等
     * @param userId 用户ID
     * @param activityType 活动类型
     * @param activityData 活动相关数据
     * @return 是否更新成功
     */
    boolean updateUserActivity(Long userId, String activityType, Map<String, Object> activityData);

    /**
     * 用户关系变更事务
     * 处理用户关系变更时的双向统计更新
     * 例如：A关注B时，A的关注数+1，B的粉丝数+1
     * @param fromUserId 发起用户ID
     * @param toUserId 目标用户ID
     * @param relationshipType 关系类型（follow, unfollow, block等）
     * @return 是否更新成功
     */
    boolean updateUserRelationship(Long fromUserId, Long toUserId, String relationshipType);

    /**
     * 内容互动事务
     * 处理用户对内容的互动（点赞、收藏、评论等）
     * 同时更新内容统计和用户统计
     * @param userId 用户ID
     * @param contentId 内容ID
     * @param authorId 内容作者ID
     * @param interactionType 互动类型
     * @return 是否更新成功
     */
    boolean processContentInteraction(Long userId, Long contentId, Long authorId, String interactionType);

    /**
     * 经济活动事务
     * 处理涉及金币、赞助等经济活动的复杂事务
     * @param fromUserId 支付用户ID
     * @param toUserId 接收用户ID
     * @param amount 金额
     * @param transactionType 交易类型
     * @param metadata 交易元数据
     * @return 是否交易成功
     */
    boolean processEconomicTransaction(Long fromUserId, Long toUserId, Long amount, 
                                     String transactionType, Map<String, Object> metadata);

    /**
     * 用户数据一致性检查和修复
     * 检查user表和user_stats表数据的一致性，并修复不一致的数据
     * @param userId 用户ID，null表示检查所有用户
     * @return 修复的记录数
     */
    int checkAndRepairDataConsistency(Long userId);

    /**
     * 用户统计数据重新计算事务
     * 基于实际的业务数据重新计算用户的统计数据
     * @param userId 用户ID
     * @return 是否重新计算成功
     */
    boolean recalculateUserStats(Long userId);

    /**
     * 批量用户统计数据重新计算
     * @param userIds 用户ID列表
     * @return 成功重新计算的用户数量
     */
    int batchRecalculateUserStats(List<Long> userIds);

    /**
     * 用户数据归档事务
     * 将不活跃用户的数据进行归档处理
     * @param userId 用户ID
     * @param archiveType 归档类型
     * @return 是否归档成功
     */
    boolean archiveUserData(Long userId, String archiveType);

    /**
     * 用户数据恢复事务
     * 从归档中恢复用户数据
     * @param userId 用户ID
     * @return 是否恢复成功
     */
    boolean restoreUserData(Long userId);

    /**
     * 系统维护事务
     * 执行系统级别的用户数据维护操作
     * @param maintenanceType 维护类型
     * @param parameters 维护参数
     * @return 维护结果
     */
    Map<String, Object> performSystemMaintenance(String maintenanceType, Map<String, Object> parameters);
}