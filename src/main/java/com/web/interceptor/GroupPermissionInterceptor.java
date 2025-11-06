package com.web.interceptor;

import com.web.annotation.RequireGroupPermission;
import com.web.exception.WeebException;
import com.web.service.GroupPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 群组权限拦截器
 * 拦截带有@RequireGroupPermission注解的方法，检查用户权限
 */
@Slf4j
@Component
public class GroupPermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireGroupPermission annotation = handlerMethod.getMethodAnnotation(RequireGroupPermission.class);
        
        if (annotation == null) {
            return true;
        }

        // 从请求中获取userId和groupId
        Long userId = (Long) request.getAttribute("userId");
        String groupIdStr = request.getParameter("groupId");
        
        if (userId == null || groupIdStr == null) {
            throw new WeebException("缺少必要参数");
        }

        Long groupId = Long.valueOf(groupIdStr);
        int requiredPermission = annotation.permission();

        // 检查权限
        if (!groupPermissionService.hasPermission(userId, groupId, requiredPermission)) {
            log.warn("⚠️ 权限不足: userId={}, groupId={}, requiredPermission={}", userId, groupId, requiredPermission);
            throw new WeebException(annotation.message());
        }

        return true;
    }
}
