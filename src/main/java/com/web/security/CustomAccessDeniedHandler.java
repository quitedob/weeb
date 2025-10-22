package com.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.util.SecurityAuditUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义访问拒绝处理器
 * 处理权限不足的情况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String username = SecurityUtils.getCurrentUsername();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIpAddress(request);

        // 记录访问拒绝事件
        log.warn("Access denied for user: {} when accessing {} {}", username, method, requestUri);

        // 记录安全审计
        SecurityAuditUtils.logAccessDenied(
            username != null ? username : "anonymous",
            method,
            requestUri,
            ipAddress,
            accessDeniedException.getMessage()
        );

        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 构建错误响应
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorResponse.put("message", "访问被拒绝：您没有权限执行此操作");
        errorResponse.put("error", "ACCESS_DENIED");
        errorResponse.put("path", requestUri);
        errorResponse.put("method", method);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        // 根据请求类型返回不同的响应
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            // 如果是浏览器请求，返回HTML页面
            response.sendRedirect("/error/403");
        } else {
            // 如果是API请求，返回JSON
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.getWriter().flush();
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // 如果是通过代理访问，取第一个IP地址
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    /**
     * 判断是否为AJAX请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith) ||
               (request.getContentType() != null && request.getContentType().contains("application/json"));
    }

    /**
     * 检查是否为API请求
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.startsWith("/api/") || requestUri.startsWith("/v1/") || requestUri.startsWith("/v2/");
    }

    /**
     * 获取请求的详细信息
     */
    private Map<String, Object> getRequestDetails(HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("uri", request.getRequestURI());
        details.put("method", request.getMethod());
        details.put("queryString", request.getQueryString());
        details.put("userAgent", request.getHeader("User-Agent"));
        details.put("referer", request.getHeader("Referer"));
        details.put("timestamp", System.currentTimeMillis());
        return details;
    }

    /**
     * 检查是否为敏感资源访问
     */
    private boolean isSensitiveResource(String requestUri) {
        return requestUri.contains("/admin/") ||
               requestUri.contains("/delete") ||
               requestUri.contains("/update") ||
               requestUri.contains("/create");
    }

    /**
     * 生成访问拒绝的详细日志信息
     */
    private void logAccessDeniedDetails(HttpServletRequest request, AccessDeniedException exception) {
        String username = SecurityUtils.getCurrentUsername();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = getClientIpAddress(request);

        Map<String, Object> details = getRequestDetails(request);

        log.warn("Access Denied Details - User: {}, IP: {}, Method: {}, URI: {}, Exception: {}, Details: {}",
                username, ipAddress, method, requestUri, exception.getMessage(), details);

        // 如果是敏感资源，记录更详细的审计信息
        if (isSensitiveResource(requestUri)) {
            log.error("Sensitive Resource Access Attempt - User: {}, IP: {}, Method: {}, URI: {}, User-Agent: {}",
                    username, ipAddress, method, requestUri, request.getHeader("User-Agent"));
        }
    }
}