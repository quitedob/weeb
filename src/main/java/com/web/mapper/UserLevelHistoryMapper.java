package com.web.mapper;

import com.web.model.UserLevelHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户等级变更历史数据访问层接口
 */
@Mapper
public interface UserLevelHistoryMapper {

    /**
     * 插入用户等级变更记录
     * @param userLevelHistory 用户等级变更历史对象
     * @return 影响的行数
     */
    int insert(UserLevelHistory userLevelHistory);

    /**
     * 批量插入用户等级变更记录
     * @param userLevelHistories 用户等级变更历史列表
     * @return 影响的行数
     */
    int batchInsert(@Param("userLevelHistories") List<UserLevelHistory> userLevelHistories);

    /**
     * 更新用户等级变更记录
     * @param userLevelHistory 用户等级变更历史对象
     * @return 影响的行数
     */
    int update(UserLevelHistory userLevelHistory);

    /**
     * 删除用户等级变更记录
     * @param id 主键ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID删除等级变更记录
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 批量删除用户等级变更记录
     * @param ids ID列表
     * @return 影响的行数
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询用户等级变更记录
     * @param id 主键ID
     * @return 用户等级变更记录对象
     */
    UserLevelHistory findById(@Param("id") Long id);

    /**
     * 根据用户ID查询等级变更记录列表
     * @param userId 用户ID
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询等级变更记录列表（分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findByUserIdWithPaging(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 查询用户等级变更记录列表（分页）
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @param userId 用户ID（可选）
     * @param changeType 变更类型（可选）
     * @param operatorId 操作者ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findWithPaging(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("userId") Long userId,
            @Param("changeType") Integer changeType,
            @Param("operatorId") Long operatorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计用户等级变更记录数量
     * @param userId 用户ID（可选）
     * @param changeType 变更类型（可选）
     * @param operatorId 操作者ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 总数
     */
    long count(
            @Param("userId") Long userId,
            @Param("changeType") Integer changeType,
            @Param("operatorId") Long operatorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询用户最近的等级变更记录
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    /**
     * 查询指定时间范围内的等级变更记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 返回数量限制
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );

    /**
     * 查询等级提升记录
     * @param userId 用户ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param limit 返回数量限制
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findLevelUpRecords(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );

    /**
     * 查询等级降低记录
     * @param userId 用户ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param limit 返回数量限制
     * @return 用户等级变更记录列表
     */
    List<UserLevelHistory> findLevelDownRecords(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );

    /**
     * 统计用户等级变更次数
     * @param userId 用户ID
     * @return 变更次数
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 获取用户当前等级
     * @param userId 用户ID
     * @return 当前等级，如果没有记录则返回null
     */
    Integer getCurrentLevelByUserId(@Param("userId") Long userId);

    /**
     * 获取用户等级变更统计信息
     * @param userId 用户ID
     * @param days 统计天数
     * @return 统计信息Map
     */
    List<UserLevelHistory> getUserLevelStats(
            @Param("userId") Long userId,
            @Param("days") int days
    );

    /**
     * 清理过期的等级变更记录
     * @param beforeTime 清理时间点
     * @return 清理的记录数
     */
    int cleanupExpiredRecords(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 更新记录状态
     * @param id 主键ID
     * @param status 状态
     * @return 影响的行数
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );

    /**
     * 批量更新记录状态
     * @param ids ID列表
     * @param status 状态
     * @return 影响的行数
     */
    int batchUpdateStatus(
            @Param("ids") List<Long> ids,
            @Param("status") Integer status
    );
}