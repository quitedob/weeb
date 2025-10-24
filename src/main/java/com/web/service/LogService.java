package com.web.service;

import com.web.model.SystemLog;
import java.util.List;
import java.util.Map;

public interface LogService {

    /**
     * 记录一条系统日志
     * @param log 日志实体
     */
    void recordLog(SystemLog log);

    /**
     * 分页获取系统日志
     * @param page 页码
     * @param pageSize 每页大小
     * @return 包含日志列表和总数的Map
     */
    Map<String, Object> getSystemLogs(int page, int pageSize);

    /**
     * 分页获取系统日志（带综合过滤条件）
     * @param page 页码
     * @param pageSize 每页大小
     * @param operatorId 操作者ID（可选）
     * @param action 操作类型（可选）
     * @param ipAddress IP地址（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @param keyword 关键词搜索（可选，搜索操作详情）
     * @return 包含日志列表和总数的Map
     */
    Map<String, Object> getSystemLogsWithFilters(int page, int pageSize,
                                                Long operatorId, String action,
                                                String ipAddress, String startDate,
                                                String endDate, String keyword);

    /**
     * 获取系统日志统计信息
     * @return 统计信息
     */
    Map<String, Object> getLogStatistics();

    /**
     * 获取错误日志统计
     * @param hours 统计小时数
     * @return 错误日志统计
     */
    Map<String, Object> getErrorStatistics(int hours);

    /**
     * 获取最近活动统计
     * @param minutes 统计分钟数
     * @return 活动统计
     */
    Map<String, Object> getRecentActivityStatistics(int minutes);

    /**
     * 获取操作类型列表
     * @return 操作类型列表
     */
    List<String> getAvailableActions();

    /**
     * 获取操作员列表
     * @param days 最近天数
     * @return 操作员列表
     */
    List<Map<String, Object>> getAvailableOperators(int days);

    /**
     * 导出系统日志
     * @param format 导出格式 (csv, xlsx, json)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param filters 过滤条件
     * @return 导出数据
     */
    byte[] exportLogs(String format, String startDate, String endDate, Map<String, Object> filters);

    /**
     * 清理过期日志
     * @param days 保留天数
     * @return 清理结果
     */
    Map<String, Object> cleanupOldLogs(int days);

    /**
     * 批量删除日志
     * @param logIds 日志ID列表
     * @param operatorId 操作者ID
     * @return 删除结果
     */
    Map<String, Object> batchDeleteLogs(List<Long> logIds, Long operatorId);

    /**
     * 获取日志详情
     * @param logId 日志ID
     * @return 日志详情
     */
    SystemLog getLogDetails(Long logId);

    /**
     * 获取日志级别分布
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日志级别分布
     */
    Map<String, Object> getLogLevelDistribution(String startDate, String endDate);

    /**
     * 获取每小时日志统计
     * @param date 日期 (yyyy-MM-dd)
     * @return 每小时日志统计
     */
    Map<String, Object> getHourlyLogStatistics(String date);

    /**
     * 搜索日志内容
     * @param keyword 搜索关键词
     * @param limit 返回数量限制
     * @return 搜索结果
     */
    List<SystemLog> searchLogs(String keyword, int limit);
}
