package com.web.interceptor;

import com.web.annotation.RequiresPermission;
import com.web.common.ApiResponse;
import com.web.service.RolePermissionService;
import com.web.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 权限验证拦截器
 * 基于角色权限的API访问控制
 * 
 * 使用方式：在Controller方法上添加 @RequiresPermission 注解
 * 例如：@RequiresPermission("ARTICLE_DELETE_ANY")
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法是否有 @RequiresPermission 注解
        RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (requiresPermission == null) {
            // 没有权限要求，直接放行
            return true;
        }

        // 获取需要的权限
        String[] requiredPermissions = requiresPermission.value();
        boolean requireAll = requiresPermission.requireAll();

        // 从请求头获取JWT Token
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("未提供认证Token: uri={}", request.getRequestURI());
            sendUnauthorizedResponse(response, "未提供认证信息");
            return false;
        }

        // 验证Token并获取用户ID
        Long userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("无效的Token: uri={}", request.getRequestURI());
                sendUnauthorizedResponse(response, "认证信息无效");
                return false;
            }
        } catch (Exception e) {
            log.error("Token验证失败: uri={}", request.getRequestURI(), e);
            sendUnauthorizedResponse(response, "认证信息验证失败");
            return false;
        }

        // 验证权限
        try {
            boolean hasPermission = checkPermissions(userId, requiredPermissions, requireAll);
            
            if (!hasPermission) {
                log.warn("权限不足: userId={}, uri={}, requiredPermissions={}",
                        userId, request.getRequestURI(), String.join(",", requiredPermissions));
                sendForbiddenResponse(response, "权限不足，无法访问该资源");
                return false;
            }

            log.debug("权限验证通过: userId={}, uri={}, permissions={}",
                    userId, request.getRequestURI(), String.join(",", requiredPermissions));
            return true;

        } catch (Exception e) {
            log.error("权限验证异常: userId={}, uri={}", userId, request.getRequestURI(), e);
            sendErrorResponse(response, "权限验证失败");
            return false;
        }
    }

    /**
     * 检查用户权限
     */
    private boolean checkPermissions(Long userId, String[] requiredPermissions, boolean requireAll) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }

        if (requireAll) {
            // 需要所有权限
            for (String permission : requiredPermissions) {
                if (!rolePermissionService.hasPermission(userId, permission)) {
                    return false;
                }
            }
            return true;
        } else {
            // 只需要任意一个权限
            for (String permission : requiredPermissions) {
                if (rolePermissionService.hasPermission(userId, permission)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 从请求头提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 发送未认证响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                ApiResponse.error(401, message));
    }

    /**
     * 发送权限不足响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                ApiResponse.error(403, message));
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                ApiResponse.error(500, message));
    }

    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpServletResponse response, int status, ApiResponse<?> apiResponse) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(apiResponse));
            writer.flush();
        }
    }
}
