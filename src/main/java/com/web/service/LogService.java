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
}
