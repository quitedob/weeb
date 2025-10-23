package com.web.service;

import com.web.model.SystemLog;
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
}
