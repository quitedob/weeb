package com.web.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * SQL注入防护工具类
 * 提供SQL注入检测和防护方法
 */
@Component
@Slf4j
public class SqlInjectionUtils {

    // SQL注入检测正则表达式
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        // SQL关键字检测
        Pattern.compile("(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|FROM|WHERE|AND|OR|NOT|IN|EXISTS|LIKE|BETWEEN|IS|NULL)\\b)"),
        // 注释检测
        Pattern.compile("(?i)(--|#|/\\*|\\*/|;)"),
        // 分隔符检测
        Pattern.compile("(?i)(\\b(OR|AND)\\b\\s*\\d+\\s*=\\s*\\d+|\\b(OR|AND)\\b\\s*'[^']*'\\s*=\\s*'[^']*')"),
        // 函数调用检测
        Pattern.compile("(?i)(\\b(CAST|CONVERT|SUBSTRING|CHAR|ASCII|ORD|HEX|LENGTH|REPLACE|TRIM|UPPER|LOWER|CONCAT)\\b\\s*\\()"),
        // 条件语句检测
        Pattern.compile("(?i)(\\b(IF|CASE|WHEN|THEN|ELSE|END)\\b)"),
        // 存储过程检测
        Pattern.compile("(?i)(\\b(CALL|EXEC|EXECUTE|SP_|XP_)\\b)"),
        // 系统表检测
        Pattern.compile("(?i)(\\b(INFORMATION_SCHEMA|SYS|MASTER|MSDB|MYSQL|PG_|DB_)\\b)"),
        // 脚本标签检测
        Pattern.compile("(?i)(<script|</script>|javascript:|vbscript:|onload=|onerror=)"),
        // 特殊字符检测
        Pattern.compile("[;\"'`\\\\]")
    };

    /**
     * 检测输入是否包含SQL注入风险
     * @param input 输入字符串
     * @param paramName 参数名称（用于日志）
     * @return true表示存在SQL注入风险，false表示安全
     */
    public static boolean detectSqlInjection(String input, String paramName) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase().trim();
        
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(lowerInput).find()) {
                log.warn("检测到潜在的SQL注入风险 - 参数: {}, 输入: {}, 匹配模式: {}", 
                    paramName, input, pattern.pattern());
                return true;
            }
        }
        
        return false;
    }

    /**
     * 清理输入，移除潜在的SQL注入字符
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // 移除危险字符
        String sanitized = input.trim()
                .replaceAll(";", "")
                .replaceAll("\"", "")
                .replaceAll("'", "")
                .replaceAll("`", "")
                .replaceAll("\\\\", "");
        
        // 移除SQL关键字
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", 
            "EXEC", "UNION", "FROM", "WHERE", "AND", "OR", "NOT", "IN", 
            "EXISTS", "LIKE", "BETWEEN", "IS", "NULL", "CAST", "CONVERT",
            "SUBSTRING", "CHAR", "ASCII", "ORD", "HEX", "LENGTH", "REPLACE",
            "TRIM", "UPPER", "LOWER", "CONCAT", "IF", "CASE", "WHEN",
            "THEN", "ELSE", "END", "CALL", "EXECUTE", "SP_", "XP_",
            "INFORMATION_SCHEMA", "SYS", "MASTER", "MSDB", "MYSQL",
            "PG_", "DB_"
        };
        
        String lowerSanitized = sanitized.toLowerCase();
        for (String keyword : sqlKeywords) {
            lowerSanitized = lowerSanitized.replaceAll("\\b" + keyword.toLowerCase() + "\\b", "");
        }
        
        return sanitized;
    }

    /**
     * 验证SQL查询参数
     * @param paramValue 参数值
     * @param paramName 参数名称
     * @return true表示安全，false表示存在风险
     */
    public static boolean validateSqlParam(String paramValue, String paramName) {
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return false;
        }
        
        // 检测SQL注入
        if (detectSqlInjection(paramValue, paramName)) {
            return false;
        }
        
        // 检查长度限制
        if (paramValue.length() > 1000) {
            log.warn("SQL参数过长 - 参数: {}, 长度: {}", paramName, paramValue.length());
            return false;
        }
        
        return true;
    }

    /**
     * 验证表名和列名
     * @param name 表名或列名
     * @param type 类型（"table"或"column"）
     * @return true表示安全，false表示存在风险
     */
    public static boolean validateTableOrColumnName(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String lowerName = name.toLowerCase().trim();
        
        // 只允许字母、数字和下划线
        if (!lowerName.matches("^[a-zA-Z0-9_]+$")) {
            log.warn("{}名称包含非法字符 - {}: {}", type, name, lowerName);
            return false;
        }
        
        // 检查长度限制
        if (lowerName.length() > 64) {
            log.warn("{}名称过长 - {}: {}", type, name, lowerName.length());
            return false;
        }
        
        // 检查是否以数字开头
        if (Character.isDigit(lowerName.charAt(0))) {
            log.warn("{}名称不能以数字开头 - {}: {}", type, name, lowerName);
            return false;
        }
        
        // 检查是否为SQL关键字
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "FROM", "WHERE", "AND", "OR", "NOT", "IN", "EXISTS", "LIKE",
            "ORDER", "GROUP", "BY", "HAVING", "LIMIT", "OFFSET"
        };
        
        for (String keyword : sqlKeywords) {
            if (lowerName.equalsIgnoreCase(keyword)) {
                log.warn("{}名称不能为SQL关键字 - {}: {}", type, name, keyword);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 验证排序参数
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return true表示安全，false表示存在风险
     */
    public static boolean validateSortParams(String sortBy, String sortOrder) {
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if (!validateTableOrColumnName(sortBy, "排序字段")) {
                return false;
            }
        }
        
        if (sortOrder != null && !sortOrder.trim().isEmpty()) {
            String lowerSortOrder = sortOrder.toLowerCase().trim();
            if (!"asc".equals(lowerSortOrder) && !"desc".equals(lowerSortOrder)) {
                log.warn("排序方向无效 - {}", sortOrder);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 验证分页参数
     * @param page 页码
     * @param pageSize 页大小
     * @param paramName 参数名称前缀
     * @return true表示安全，false表示存在风险
     */
    public static boolean validatePaginationParams(Integer page, Integer pageSize, String paramName) {
        if (page != null && (page < 1 || page > 10000)) {
            log.warn("页码参数无效 - {}: {}", paramName + ".page", page);
            return false;
        }
        
        if (pageSize != null && (pageSize < 1 || pageSize > 1000)) {
            log.warn("页大小参数无效 - {}: {}", paramName + ".pageSize", pageSize);
            return false;
        }
        
        return true;
    }

    /**
     * 转义SQL字符串中的特殊字符
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    public static String escapeSqlString(String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("'", "''")
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 检查动态SQL是否安全
     * @param sql SQL语句
     * @param params 参数数组
     * @return true表示安全，false表示存在风险
     */
    public static boolean validateDynamicSql(String sql, Object[] params) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        // 检查SQL语句中是否包含字符串拼接
        if (sql.contains("+") && sql.contains("\"")) {
            log.warn("动态SQL包含字符串拼接，存在SQL注入风险 - {}", sql);
            return false;
        }
        
        // 检查参数数量是否匹配占位符数量
        int placeholderCount = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                placeholderCount++;
            }
        }
        
        if (params != null && params.length != placeholderCount) {
            log.warn("动态SQL参数数量与占位符数量不匹配 - SQL: {}, 参数数量: {}, 占位符数量: {}", 
                    sql, params.length, placeholderCount);
            return false;
        }
        
        return true;
    }
}
