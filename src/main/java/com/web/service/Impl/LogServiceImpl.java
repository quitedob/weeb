package com.web.service.Impl;

import com.web.mapper.SystemLogMapper;
import com.web.model.SystemLog;
import com.web.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LogServiceImpl implements LogService {

    @Autowired
    private SystemLogMapper systemLogMapper;

    @Override
    public void recordLog(SystemLog log) {
        systemLogMapper.insertLog(log);
    }

    @Override
    public Map<String, Object> getSystemLogs(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<SystemLog> logs = systemLogMapper.findLogsWithPaging(offset, pageSize);
        long total = systemLogMapper.countLogs();

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", total);
        return result;
    }
}
