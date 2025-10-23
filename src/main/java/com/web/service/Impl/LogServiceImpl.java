package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.SystemLogMapper;
import com.web.model.SystemLog;
import com.web.service.LogService;
import com.web.util.ValidationUtils;
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

    @Override
    public Map<String, Object> getSystemLogsWithFilters(int page, int pageSize,
                                                       Long operatorId, String action,
                                                       String ipAddress, String startDate,
                                                       String endDate, String keyword) {
        if (page <= 0) {
            throw new WeebException("页码必须为正数");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new WeebException("页面大小必须在1-100之间");
        }

        try {
            // 验证日期格式
            if (startDate != null && !startDate.trim().isEmpty() && !ValidationUtils.isValidDate(startDate)) {
                throw new WeebException("开始日期格式不正确，应为 yyyy-MM-dd 格式");
            }
            if (endDate != null && !endDate.trim().isEmpty() && !ValidationUtils.isValidDate(endDate)) {
                throw new WeebException("结束日期格式不正确，应为 yyyy-MM-dd 格式");
            }

            // 验证IP地址格式
            if (ipAddress != null && !ipAddress.trim().isEmpty() && !ValidationUtils.isValidIpAddress(ipAddress)) {
                throw new WeebException("IP地址格式不正确");
            }

            // 处理关键词搜索
            String safeKeyword = null;
            if (keyword != null && !keyword.trim().isEmpty()) {
                safeKeyword = ValidationUtils.sanitizeSearchKeyword(keyword.trim());
                if (safeKeyword.length() > 50) {
                    throw new WeebException("搜索关键词长度不能超过50个字符");
                }
            }

            int offset = (page - 1) * pageSize;
            List<SystemLog> logs = systemLogMapper.findLogsWithFilters(offset, pageSize,
                    operatorId, action, ipAddress, startDate, endDate, safeKeyword);
            long total = systemLogMapper.countLogsWithFilters(operatorId, action, ipAddress, startDate, endDate, safeKeyword);

            Map<String, Object> result = new HashMap<>();
            result.put("list", logs);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            throw new WeebException("获取系统日志失败: " + e.getMessage());
        }
    }
}
