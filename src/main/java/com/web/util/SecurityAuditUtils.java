package com.web.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全审计日志工具类
 * 记录安全相关的事件和操作
 */
@Component
@Slf4j
public class SecurityAuditUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, Integer> FAILED_LOGIN_ATTEMPTS = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> LOCKED_ACCOUNTS = new ConcurrentHashMap<>();

    /**
     * 记录登录成功事件
     */
    public static void logLoginSuccess(String username, String ip, String userAgent) {
        String message = String.format("登录成功 - 用户: %s, IP: %s, UserAgent: %s", username, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
        
        // 清除失败登录尝试计数
        FAILED_LOGIN_ATTEMPTS.remove(username);
    }

    /**
     * 记录登录失败事件
     */
    public static void logLoginFailure(String username, String ip, String userAgent, String reason) {
        String message = String.format("登录失败 - 用户: %s, IP: %s, UserAgent: %s, 原因: %s", username, ip, userAgent, reason);
        log.warn("SECURITY_AUDIT: {}", message);
        
        // 增加失败登录尝试计数
        int attempts = FAILED_LOGIN_ATTEMPTS.getOrDefault(username, 0) + 1;
        FAILED_LOGIN_ATTEMPTS.put(username, attempts);
        
        // 检查是否需要锁定账号
        if (attempts >= com.web.Config.SecurityConfig.LoginSecurity.MAX_LOGIN_ATTEMPTS) {
            lockAccount(username, ip);
        }
    }

    /**
     * 记录账号锁定事件
     */
    private static void lockAccount(String username, String ip) {
        LocalDateTime lockTime = LocalDateTime.now();
        LOCKED_ACCOUNTS.put(username, lockTime);
        
        String message = String.format("账号已锁定 - 用户: %s, IP: %s, 锁定时间: %s, 解锁时间: %s", 
                username, ip, lockTime.format(DATE_FORMATTER), 
                lockTime.plusMinutes(com.web.Config.SecurityConfig.LoginSecurity.LOCK_TIME_MINUTES).format(DATE_FORMATTER));
        log.error("SECURITY_AUDIT: {}", message);
    }

    /**
     * 检查账号是否被锁定
     */
    public static boolean isAccountLocked(String username) {
        LocalDateTime lockTime = LOCKED_ACCOUNTS.get(username);
        if (lockTime == null) {
            return false;
        }
        
        LocalDateTime unlockTime = lockTime.plusMinutes(com.web.Config.SecurityConfig.LoginSecurity.LOCK_TIME_MINUTES);
        if (LocalDateTime.now().isAfter(unlockTime)) {
            LOCKED_ACCOUNTS.remove(username);
            FAILED_LOGIN_ATTEMPTS.remove(username);
            log.info("SECURITY_AUDIT: 账号已自动解锁 - 用户: {}", username);
            return false;
        }
        
        return true;
    }

    /**
     * 记录注册事件
     */
    public static void logRegistration(String username, String email, String ip, String userAgent) {
        String message = String.format("用户注册 - 用户: %s, 邮箱: %s, IP: %s, UserAgent: %s", username, email, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录密码重置事件
     */
    public static void logPasswordReset(String username, String ip, String userAgent) {
        String message = String.format("密码重置 - 用户: %s, IP: %s, UserAgent: %s", username, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录权限检查失败事件
     */
    public static void logPermissionDenied(String username, String resource, String action, String ip) {
        String message = String.format("权限拒绝 - 用户: %s, 资源: %s, 操作: %s, IP: %s", username, resource, action, ip);
        log.warn("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录访问拒绝事件
     */
    public static void logAccessDenied(String username, String method, String requestUri, String ip, String reason) {
        String message = String.format("访问被拒绝 - 用户: %s, 方法: %s, URI: %s, IP: %s, 原因: %s", username, method, requestUri, ip, reason);
        log.warn("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录SQL注入尝试事件
     */
    public static void logSqlInjectionAttempt(String input, String paramName, String ip, String userAgent) {
        String message = String.format("SQL注入尝试 - 参数: %s, 输入: %s, IP: %s, UserAgent: %s", paramName, input, ip, userAgent);
        log.error("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录XSS攻击尝试事件
     */
    public static void logXssAttempt(String input, String paramName, String ip, String userAgent) {
        String message = String.format("XSS攻击尝试 - 参数: %s, 输入: %s, IP: %s, UserAgent: %s", paramName, input, ip, userAgent);
        log.error("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录敏感内容检测事件
     */
    public static void logSensitiveContentDetected(String content, String type, String ip, String userAgent) {
        String message = String.format("敏感内容检测 - 类型: %s, 内容: %s, IP: %s, UserAgent: %s", type, content, ip, userAgent);
        log.warn("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录文件上传事件
     */
    public static void logFileUpload(String filename, long fileSize, String contentType, String ip, String userAgent) {
        String message = String.format("文件上传 - 文件名: %s, 大小: %d bytes, 类型: %s, IP: %s, UserAgent: %s", 
                filename, fileSize, contentType, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录异常API调用事件
     */
    public static void logAbnormalApiCall(String endpoint, String method, Map<String, String> params, String ip, String userAgent) {
        String message = String.format("异常API调用 - 端点: %s, 方法: %s, 参数: %s, IP: %s, UserAgent: %s", 
                endpoint, method, params, ip, userAgent);
        log.warn("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录数据访问事件
     */
    public static void logDataAccess(String table, String operation, String condition, String ip, String userAgent) {
        String message = String.format("数据访问 - 表: %s, 操作: %s, 条件: %s, IP: %s, UserAgent: %s", 
                table, operation, condition, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录会话管理事件
     */
    public static void logSessionManagement(String username, String action, String ip, String userAgent) {
        String message = String.format("会话管理 - 用户: %s, 操作: %s, IP: %s, UserAgent: %s", username, action, ip, userAgent);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 获取当前请求信息
     */
    public static Map<String, String> getCurrentRequestInfo() {
        Map<String, String> info = new HashMap<>();

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (request != null) {
                info.put("ip", getClientIpAddress(request));
                info.put("userAgent", request.getHeader("User-Agent"));
                info.put("referer", request.getHeader("Referer"));
                info.put("requestUri", request.getRequestURI());
                info.put("method", request.getMethod());
                info.put("protocol", request.getProtocol());
                info.put("remoteAddr", request.getRemoteAddr());
                info.put("remoteHost", request.getRemoteHost());
                info.put("serverName", request.getServerName());
                info.put("serverPort", String.valueOf(request.getServerPort()));
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败", e);
        }

        return info;
    }

    /**
     * 获取客户端真实IP地址
     */
    private static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 记录安全配置变更事件
     */
    public static void logSecurityConfigChange(String configType, String oldValue, String newValue, String username) {
        String message = String.format("安全配置变更 - 类型: %s, 旧值: %s, 新值: %s, 操作者: %s", 
                configType, oldValue, newValue, username);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 记录系统安全事件
     */
    public static void logSystemSecurityEvent(String eventType, String description, String severity) {
        String message = String.format("系统安全事件 - 类型: %s, 描述: %s, 严重程度: %s", eventType, description, severity);
        log.info("SECURITY_AUDIT: {}", message);
    }

    /**
     * 获取失败登录尝试次数
     */
    public static int getFailedLoginAttempts(String username) {
        return FAILED_LOGIN_ATTEMPTS.getOrDefault(username, 0);
    }

    /**
     * 清除失败登录尝试计数
     */
    public static void clearFailedLoginAttempts(String username) {
        FAILED_LOGIN_ATTEMPTS.remove(username);
    }

    /**
     * 获取账号锁定信息
     */
    public static LocalDateTime getAccountLockTime(String username) {
        return LOCKED_ACCOUNTS.get(username);
    }
}
