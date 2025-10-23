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
}
