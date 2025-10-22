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
}
