package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.SystemLogMapper;
import com.web.model.SystemLog;
import com.web.service.LogService;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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

    // ==================== 扩展的日志服务方法 ====================

    @Override
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 获取总日志数
            long totalLogs = systemLogMapper.countLogs();
            statistics.put("totalLogs", totalLogs);

            // 获取今日日志数
            long todayLogs = systemLogMapper.countLogsByDate(java.time.LocalDate.now().toString());
            statistics.put("todayLogs", todayLogs);

            // 获取本周日志数
            long weekLogs = systemLogMapper.countLogsByWeek(
                java.time.LocalDate.now().minusDays(7).toString()
            );
            statistics.put("weekLogs", weekLogs);

            // 获取错误日志数
            long errorLogs = systemLogMapper.countLogsByAction("ERROR");
            statistics.put("errorLogs", errorLogs);

            // 获取警告日志数
            long warningLogs = systemLogMapper.countLogsByAction("WARNING");
            statistics.put("warningLogs", warningLogs);

            // 获取最活跃的操作员
            List<Map<String, Object>> topOperators = systemLogMapper.getTopOperators(10);
            statistics.put("topOperators", topOperators);

            // 获取最常见的操作
            List<Map<String, Object>> topActions = systemLogMapper.getTopActions(10);
            statistics.put("topActions", topActions);

            statistics.put("generatedAt", java.time.LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            statistics.put("error", "获取统计失败: " + e.getMessage());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getErrorStatistics(int hours) {
        Map<String, Object> errorStats = new HashMap<>();

        try {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTime = endTime.minusHours(hours);

            // 获取错误日志列表
            List<SystemLog> errorLogs = systemLogMapper.findErrorLogs(startTime, endTime);

            // 统计错误类型分布
            Map<String, Long> errorTypes = errorLogs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            log -> log.getAction(),
                            java.util.stream.Collectors.counting()
                    ));

            // 统计错误发生的时间分布
            Map<Integer, Long> hourlyErrors = errorLogs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            log -> log.getCreatedAt().getHour(),
                            java.util.stream.Collectors.counting()
                    ));

            errorStats.put("totalErrors", errorLogs.size());
            errorStats.put("errorTypes", errorTypes);
            errorStats.put("hourlyDistribution", hourlyErrors);
            errorStats.put("timeRange", hours + " hours");
            errorStats.put("generatedAt", endTime.toString());

        } catch (Exception e) {
            log.error("获取错误日志统计失败", e);
            errorStats.put("error", "获取错误统计失败: " + e.getMessage());
        }

        return errorStats;
    }

    @Override
    public Map<String, Object> getRecentActivityStatistics(int minutes) {
        Map<String, Object> activityStats = new HashMap<>();

        try {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTime = endTime.minusMinutes(minutes);

            // 获取最近的活动日志
            List<SystemLog> recentLogs = systemLogMapper.findRecentLogs(startTime, endTime, 100);

            // 统计活动类型分布
            Map<String, Long> actionTypes = recentLogs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            log -> log.getAction(),
                            java.util.stream.Collectors.counting()
                    ));

            // 统计活动时间分布
            Map<Integer, Long> minuteActivity = recentLogs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            log -> log.getCreatedAt().getMinute(),
                            java.util.stream.Collectors.counting()
                    ));

            activityStats.put("totalActivities", recentLogs.size());
            activityStats.put("activityTypes", actionTypes);
            activityStats.put("minuteDistribution", minuteActivity);
            activityStats.put("timeRange", minutes + " minutes");
            activityStats.put("generatedAt", endTime.toString());

        } catch (Exception e) {
            log.error("获取最近活动统计失败", e);
            activityStats.put("error", "获取活动统计失败: " + e.getMessage());
        }

        return activityStats;
    }

    @Override
    public List<String> getAvailableActions() {
        try {
            return systemLogMapper.getDistinctActions();
        } catch (Exception e) {
            log.error("获取操作类型列表失败", e);
            return java.util.List.of("LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "ERROR", "WARNING");
        }
    }

    @Override
    public List<Map<String, Object>> getAvailableOperators(int days) {
        try {
            List<String> operators = systemLogMapper.getDistinctOperators(days);
            return operators.stream()
                    .map(operator -> {
                        Map<String, Object> operatorMap = new java.util.HashMap<>();
                        operatorMap.put("operatorId", operator);
                        operatorMap.put("operatorName", operator);
                        return operatorMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取操作员列表失败", e);
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public byte[] exportLogs(String format, String startDate, String endDate, Map<String, Object> filters) {
        try {
            log.info("开始导出系统日志，格式：{}，时间范围：{} 至 {}", format, startDate, endDate);

            // 获取日志数据
            List<SystemLog> logs = systemLogMapper.findLogsWithFilters(
                    0, 10000, // 导出最多10000条
                    (Long) filters.get("operatorId"),
                    (String) filters.get("action"),
                    (String) filters.get("ipAddress"),
                    startDate,
                    endDate,
                    (String) filters.get("keyword")
            );

            switch (format.toLowerCase()) {
                case "csv":
                    return exportToCsv(logs, startDate, endDate);
                case "xlsx":
                    return exportToExcel(logs, startDate, endDate);
                case "json":
                    return exportToJson(logs, startDate, endDate);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }

        } catch (Exception e) {
            log.error("导出系统日志失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> cleanupOldLogs(int days) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (days < 7) {
                throw new WeebException("保留天数不能少于7天");
            }

            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(days);

            int deletedCount = systemLogMapper.deleteLogsBefore(cutoffDate);

            result.put("deletedCount", deletedCount);
            result.put("cutoffDate", cutoffDate.toString());
            result.put("message", "已清理 " + deletedCount + " 条超过 " + days + " 天的日志");

            log.info("日志清理完成，删除 {} 条 {} 天前的日志", deletedCount, days);

        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            result.put("error", "清理失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> batchDeleteLogs(List<Long> logIds, Long operatorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (logIds == null || logIds.isEmpty()) {
                throw new WeebException("日志ID列表不能为空");
            }

            if (logIds.size() > 1000) {
                throw new WeebException("批量删除数量不能超过1000条");
            }

            int deletedCount = systemLogMapper.batchDeleteLogs(logIds);

            result.put("deletedCount", deletedCount);
            result.put("requestedCount", logIds.size());
            result.put("operatorId", operatorId);
            result.put("message", "已删除 " + deletedCount + " 条日志");

            log.info("批量删除日志完成，操作者：{}，删除数量：{}", operatorId, deletedCount);

        } catch (Exception e) {
            log.error("批量删除日志失败", e);
            result.put("error", "删除失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public SystemLog getLogDetails(Long logId) {
        try {
            SystemLog log = systemLogMapper.findById(logId);
            if (log == null) {
                throw new WeebException("日志不存在: " + logId);
            }
            return log;
        } catch (Exception e) {
            log.error("获取日志详情失败", e);
            throw new WeebException("获取日志详情失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getLogLevelDistribution(String startDate, String endDate) {
        Map<String, Object> distribution = new HashMap<>();

        try {
            List<SystemLog> logs = systemLogMapper.findLogsWithFilters(
                    0, 10000, null, null, null, startDate, endDate, null
            );

            // 统计不同操作类型的分布
            Map<String, Long> actionDistribution = logs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            SystemLog::getAction,
                            java.util.stream.Collectors.counting()
                    ));

            // 统计每天的分布
            Map<String, Long> dailyDistribution = logs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            log -> log.getCreatedAt().toLocalDate().toString(),
                            java.util.stream.Collectors.counting()
                    ));

            distribution.put("actionDistribution", actionDistribution);
            distribution.put("dailyDistribution", dailyDistribution);
            distribution.put("totalLogs", logs.size());
            distribution.put("timeRange", startDate + " 至 " + endDate);

        } catch (Exception e) {
            log.error("获取日志级别分布失败", e);
            distribution.put("error", "获取分布失败: " + e.getMessage());
        }

        return distribution;
    }

    @Override
    public Map<String, Object> getHourlyLogStatistics(String date) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            java.time.LocalDateTime startOfDay = java.time.LocalDateTime.parse(date + "T00:00:00");
            java.time.LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            List<SystemLog> logs = systemLogMapper.findLogsByDate(date);

            // 统计每小时的日志数量
            int[] hourlyCounts = new int[24];
            Map<String, Long> hourlyActivity = new java.util.HashMap<>();

            for (SystemLog log : logs) {
                int hour = log.getCreatedAt().getHour();
                hourlyCounts[hour]++;
            }

            // 构建每小时统计
            for (int hour = 0; hour < 24; hour++) {
                hourlyActivity.put(String.format("%02d:00", hour), (long) hourlyCounts[hour]);
            }

            statistics.put("hourlyActivity", hourlyActivity);
            statistics.put("totalLogs", logs.size());
            statistics.put("date", date);
            statistics.put("generatedAt", java.time.LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("获取每小时日志统计失败", e);
            statistics.put("error", "获取统计失败: " + e.getMessage());
        }

        return statistics;
    }

    @Override
    public List<SystemLog> searchLogs(String keyword, int limit) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return new java.util.ArrayList<>();
            }

            String safeKeyword = com.web.util.ValidationUtils.sanitizeSearchKeyword(keyword.trim());
            if (safeKeyword.length() > 100) {
                throw new WeebException("搜索关键词长度不能超过100个字符");
            }

            return systemLogMapper.searchLogs(safeKeyword, Math.min(limit, 1000));

        } catch (Exception e) {
            log.error("搜索日志失败", e);
            return new java.util.ArrayList<>();
        }
    }

    // ==================== 私有辅助方法 ====================

    private byte[] exportToCsv(List<SystemLog> logs, String startDate, String endDate) {
        StringBuilder csv = new StringBuilder();

        // CSV 头部
        csv.append("ID,操作者ID,操作类型,详情,IP地址,创建时间\n");
        csv.append("导出时间:").append(java.time.LocalDateTime.now()).append("\n");
        csv.append("时间范围:").append(startDate).append(" 至 ").append(endDate).append("\n\n");

        // CSV 数据行
        for (SystemLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getOperatorId()).append(",");
            csv.append(escapeCsv(log.getAction())).append(",");
            csv.append(escapeCsv(log.getDetails())).append(",");
            csv.append(escapeCsv(log.getIpAddress())).append(",");
            csv.append(log.getCreatedAt()).append("\n");
        }

        return csv.toString().getBytes();
    }

    private byte[] exportToExcel(List<SystemLog> logs, String startDate, String endDate) {
        // 简化的Excel导出实现
        StringBuilder excel = new StringBuilder();

        excel.append("系统日志导出\n");
        excel.append("导出时间:").append(java.time.LocalDateTime.now()).append("\n");
        excel.append("时间范围:").append(startDate).append(" 至 ").append(endDate).append("\n");
        excel.append("导出数量:").append(logs.size()).append(" 条\n\n");

        excel.append("ID\t操作者ID\t操作类型\t详情\tIP地址\t创建时间\n");
        for (SystemLog log : logs) {
            excel.append(log.getId()).append("\t")
                .append(log.getOperatorId()).append("\t")
                .append(log.getAction()).append("\t")
                .append(log.getDetails()).append("\t")
                .append(log.getIpAddress()).append("\t")
                .append(log.getCreatedAt()).append("\n");
        }

        return excel.toString().getBytes();
    }

    private byte[] exportToJson(List<SystemLog> logs, String startDate, String endDate) {
        try {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("exportTime", java.time.LocalDateTime.now());
            exportData.put("timeRange", Map.of("start", startDate, "end", endDate));
            exportData.put("totalLogs", logs.size());
            exportData.put("logs", logs);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsBytes(exportData);
        } catch (Exception e) {
            log.error("JSON导出失败", e);
            return "{}".getBytes();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== 缺失方法的实现 ====================

    /**
     * 根据日期统计日志数量
     */
    private long countLogsByDate(String date) {
        try {
            return systemLogMapper.countLogsByDate(date);
        } catch (Exception e) {
            log.warn("统计日期日志失败: {}", date, e);
            return 0;
        }
    }

    /**
     * 根据日期范围统计日志数量
     */
    private long countLogsByWeek(String startDate) {
        try {
            return systemLogMapper.countLogsByWeek(startDate);
        } catch (Exception e) {
            log.warn("统计周日志失败: {}", startDate, e);
            return 0;
        }
    }

    /**
     * 根据操作类型统计日志数量
     */
    private long countLogsByAction(String action) {
        try {
            return systemLogMapper.countLogsByAction(action);
        } catch (Exception e) {
            log.warn("统计操作日志失败: {}", action, e);
            return 0;
        }
    }

    /**
     * 获取最活跃的操作员
     */
    private List<Map<String, Object>> getTopOperators(int limit) {
        try {
            return systemLogMapper.getTopOperators(limit);
        } catch (Exception e) {
            log.warn("获取最活跃操作员失败", e);
            return List.of();
        }
    }

    /**
     * 获取最常见的操作
     */
    private List<Map<String, Object>> getTopActions(int limit) {
        try {
            return systemLogMapper.getTopActions(limit);
        } catch (Exception e) {
            log.warn("获取最常见操作失败", e);
            return List.of();
        }
    }

    /**
     * 查找错误日志
     */
    private List<SystemLog> findErrorLogs(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        try {
            return systemLogMapper.findErrorLogs(startTime, endTime);
        } catch (Exception e) {
            log.warn("查找错误日志失败", e);
            return List.of();
        }
    }

    /**
     * 查找最近日志
     */
    private List<SystemLog> findRecentLogs(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, int limit) {
        try {
            return systemLogMapper.findRecentLogs(startTime, endTime, limit);
        } catch (Exception e) {
            log.warn("查找最近日志失败", e);
            return List.of();
        }
    }

    /**
     * 获取所有不同的操作类型
     */
    private List<String> getDistinctActions() {
        try {
            return systemLogMapper.getDistinctActions();
        } catch (Exception e) {
            log.warn("获取操作类型失败", e);
            return List.of();
        }
    }

    /**
     * 获取最活跃的操作员
     */
    private List<Map<String, Object>> getDistinctOperators(int limit) {
        try {
            List<String> operators = systemLogMapper.getDistinctOperators(limit);
            return operators.stream()
                    .map(operator -> {
                        Map<String, Object> operatorMap = new java.util.HashMap<>();
                        operatorMap.put("operatorId", operator);
                        operatorMap.put("operatorName", operator);
                        return operatorMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("获取操作员失败", e);
            return List.of();
        }
    }

    /**
     * 删除指定日期之前的日志
     */
    private int deleteLogsBefore(java.time.LocalDateTime cutoffTime) {
        try {
            return systemLogMapper.deleteLogsBefore(cutoffTime);
        } catch (Exception e) {
            log.warn("删除过期日志失败", e);
            return 0;
        }
    }

    /**
     * 批量删除日志
     */
    private int batchDeleteLogs(List<Long> logIds) {
        try {
            return systemLogMapper.batchDeleteLogs(logIds);
        } catch (Exception e) {
            log.warn("批量删除日志失败", e);
            return 0;
        }
    }

    /**
     * 根据ID查找日志
     */
    private SystemLog findById(Long id) {
        try {
            return systemLogMapper.findById(id);
        } catch (Exception e) {
            log.warn("查找日志失败: {}", id, e);
            return null;
        }
    }

    /**
     * 根据日期查找日志
     */
    private List<SystemLog> findLogsByDate(String date) {
        try {
            return systemLogMapper.findLogsByDate(date);
        } catch (Exception e) {
            log.warn("查找日期日志失败: {}", date, e);
            return List.of();
        }
    }
}
