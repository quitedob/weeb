package com.web.security;

import com.web.model.User;
import com.web.service.UserService;
import com.web.service.UserTypeSecurityService;
import com.web.util.SecurityAuditUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全工具类
 * 提供安全相关的实用方法
 */
@Slf4j
@Component
public class SecurityUtils {

    private static UserService userService;
    private static UserTypeSecurityService userTypeSecurityService;

    public SecurityUtils(UserService userService, UserTypeSecurityService userTypeSecurityService) {
        SecurityUtils.userService = userService;
        SecurityUtils.userTypeSecurityService = userTypeSecurityService;
    }

    /**
     * 获取当前认证的用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 获取当前认证的用户ID
     */
    public static Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (username != null) {
                User user = userService.findByUsername(username);
                return user != null ? user.getId() : null;
            }
        } catch (Exception e) {
            log.error("Error getting current user ID", e);
        }
        return null;
    }

    /**
     * 获取当前认证的用户
     */
    public static User getCurrentUser() {
        try {
            String username = getCurrentUsername();
            if (username != null) {
                return userService.findByUsername(username);
            }
        } catch (Exception e) {
            log.error("Error getting current user", e);
        }
        return null;
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
               && !"anonymousUser".equals(authentication.getName());
    }

    /**
     * 检查当前用户是否具有指定权限
     * @deprecated 使用基于用户类型的权限检查替代
     */
    @Deprecated
    public static boolean hasPermission(String permission) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return false;
            }

            // 基于用户类型进行简单权限检查
            String userType = userTypeSecurityService.getUserType(username);

            // 管理员拥有所有权限
            if ("ADMIN".equals(userType)) {
                return true;
            }

            // 机器人和普通用户只有基本权限
            if ("BOT".equals(userType) || "USER".equals(userType)) {
                return "READ".equals(permission) || "READ_SELF".equals(permission);
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking permission: {}", permission, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否具有指定角色
     * @deprecated 使用UserTypeSecurityService的方法替代
     */
    @Deprecated
    public static boolean hasRole(String role) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return false;
            }

            return switch (role.toUpperCase()) {
                case "ADMIN" -> userTypeSecurityService.isAdmin(username);
                case "BOT" -> userTypeSecurityService.isBot(username);
                case "USER" -> userTypeSecurityService.isRegularUser(username);
                default -> false;
            };

        } catch (Exception e) {
            log.error("Error checking role: {}", role, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        try {
            String username = getCurrentUsername();
            return username != null && userTypeSecurityService.isAdmin(username);
        } catch (Exception e) {
            log.error("Error checking admin status", e);
            return false;
        }
    }

    /**
     * 获取当前用户的所有权限
     */
    public static List<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是资源的所有者
     */
    public static boolean isOwner(String resource, Long resourceId) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return false;
            }

            return switch (resource.toLowerCase()) {
                case "article" -> userService.isArticleOwner(currentUserId, resourceId);
                case "message" -> userService.isMessageOwner(currentUserId, resourceId);
                case "group" -> userService.isGroupOwner(currentUserId, resourceId);
                case "user" -> currentUserId.equals(resourceId);
                default -> false;
            };
        } catch (Exception e) {
            log.error("Error checking ownership for resource: {}, resourceId: {}", resource, resourceId, e);
            return false;
        }
    }

    /**
     * 记录安全操作审计
     */
    public static void auditSecurityOperation(String operation, String resource, Long resourceId, boolean success) {
        try {
            String username = getCurrentUsername();
            String ipAddress = getClientIpAddress();

            SecurityAuditUtils.logSecurityOperation(
                username != null ? username : "anonymous",
                operation,
                resource,
                resourceId != null ? resourceId.toString() : null,
                ipAddress,
                success
            );
        } catch (Exception e) {
            log.error("Error logging security audit", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private static String getClientIpAddress() {
        // 这里可以从当前请求中获取IP地址
        // 由于SecurityUtils不在Web层，这里返回一个默认值
        return "unknown";
    }

    /**
     * 检查当前用户是否有权限执行操作
     */
    public static boolean canPerformOperation(String permission, String resource, Long resourceId) {
        try {
            String username = getCurrentUsername();
            if (username == null) {
                return false;
            }

            // 管理员拥有所有权限
            if (userTypeSecurityService.isAdmin(username)) {
                return true;
            }

            // 检查读权限
            if (permission.startsWith("READ")) {
                return userTypeSecurityService.canRead(username);
            }

            // 检查写权限
            if (permission.startsWith("WRITE") || permission.startsWith("CREATE") || permission.startsWith("UPDATE") || permission.startsWith("DELETE")) {
                if (!userTypeSecurityService.canWrite(username)) {
                    return false;
                }
            }

            // 检查资源所有权
            if (permission.endsWith("_OWN")) {
                return isOwner(resource, resourceId);
            }

            // 默认情况
            return false;

        } catch (Exception e) {
            log.error("Error checking operation permission: {}, resource: {}", permission, resource, e);
            return false;
        }
    }

    /**
     * 获取用户会话信息
     */
    public static Object getSessionAttribute(String attributeName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof org.springframework.security.web.authentication.WebAuthenticationDetails) {
            // 这里可以从会话中获取属性
            // 具体实现取决于会话管理方式
            return null;
        }
        return null;
    }

    /**
     * 清除当前用户的安全上下文
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared for user: {}", getCurrentUsername());
    }
}