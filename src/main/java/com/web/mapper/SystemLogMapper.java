package com.web.mapper;

import com.web.model.SystemLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemLogMapper {

    /**
     * 插入一条新的系统日志
     * @param log 日志实体
     * @return 影响的行数
     */
    int insertLog(SystemLog log);

    /**
     * 分页查询系统日志
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 日志列表
     */
    List<SystemLog> findLogsWithPaging(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 统计系统日志总数
     * @return 日志总数
     */
    long countLogs();

    /**
     * 分页查询系统日志（带过滤条件）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @param operatorId 操作者ID（可选）
     * @param action 操作类型（可选）
     * @param ipAddress IP地址（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param keyword 关键词（可选）
     * @return 日志列表
     */
    List<SystemLog> findLogsWithFilters(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                       @Param("operatorId") Long operatorId, @Param("action") String action,
                                       @Param("ipAddress") String ipAddress, @Param("startDate") String startDate,
                                       @Param("endDate") String endDate, @Param("keyword") String keyword);

    /**
     * 统计带过滤条件的系统日志总数
     * @param operatorId 操作者ID（可选）
     * @param action 操作类型（可选）
     * @param ipAddress IP地址（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param keyword 关键词（可选）
     * @return 日志总数
     */
    long countLogsWithFilters(@Param("operatorId") Long operatorId, @Param("action") String action,
                             @Param("ipAddress") String ipAddress, @Param("startDate") String startDate,
                             @Param("endDate") String endDate, @Param("keyword") String keyword);

    /**
     * 按日期统计日志数量
     * @param date 日期
     * @return 日志数量
     */
    long countLogsByDate(@Param("date") String date);

    /**
     * 按周统计日志数量
     * @param startDate 周开始日期
     * @return 日志数量
     */
    long countLogsByWeek(@Param("startDate") String startDate);

    /**
     * 按操作类型统计日志数量
     * @param action 操作类型
     * @return 日志数量
     */
    long countLogsByAction(@Param("action") String action);

    /**
     * 获取最活跃的操作员
     * @param limit 限制数量
     * @return 操作员统计列表
     */
    List<java.util.Map<String, Object>> getTopOperators(@Param("limit") int limit);

    /**
     * 获取最常见的操作
     * @param limit 限制数量
     * @return 操作统计列表
     */
    List<java.util.Map<String, Object>> getTopActions(@Param("limit") int limit);

    /**
     * 查找错误日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误日志列表
     */
    List<SystemLog> findErrorLogs(@Param("startTime") java.time.LocalDateTime startTime,
                                  @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 查找最近日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 日志列表
     */
    List<SystemLog> findRecentLogs(@Param("startTime") java.time.LocalDateTime startTime,
                                   @Param("endTime") java.time.LocalDateTime endTime,
                                   @Param("limit") int limit);

    /**
     * 获取所有操作类型
     * @return 操作类型列表
     */
    List<String> getDistinctActions();

    /**
     * 获取活跃操作员列表
     * @param limit 限制数量
     * @return 操作员列表
     */
    List<String> getDistinctOperators(@Param("limit") int limit);

    /**
     * 删除指定时间之前的日志
     * @param beforeTime 时间点
     * @return 删除的行数
     */
    int deleteLogsBefore(@Param("beforeTime") java.time.LocalDateTime beforeTime);

    /**
     * 批量删除日志
     * @param ids 日志ID列表
     * @return 删除的行数
     */
    int batchDeleteLogs(@Param("ids") List<Long> ids);

    /**
     * 根据ID查找日志
     * @param id 日志ID
     * @return 日志对象
     */
    SystemLog findById(@Param("id") Long id);

    /**
     * 根据日期查找日志
     * @param date 日期
     * @return 日志列表
     */
    List<SystemLog> findLogsByDate(@Param("date") String date);

    /**
     * 搜索日志
     * @param keyword 关键词
     * @param limit 限制数量
     * @return 日志列表
     */
    List<SystemLog> searchLogs(@Param("keyword") String keyword, @Param("limit") int limit);
}
