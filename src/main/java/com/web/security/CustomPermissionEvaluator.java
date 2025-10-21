package com.web.security;

import com.web.model.Permission;
import com.web.model.User;
import com.web.service.PermissionService;
import com.web.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 自定义权限评估器
 * 用于Spring Security的@PreAuthorize和@PostAuthorize注解中的权限检查
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

    private final UserService userService;
    private final PermissionService permissionService;

    public CustomPermissionEvaluator(UserService userService, PermissionService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param authentication 当前认证用户
     * @param targetId 目标对象ID
     * @param permission 权限名称或权限表达式
     * @return 是否有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetId, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);

            if (user == null) {
                logger.warn("User not found: {}", username);
                return false;
            }

            String permissionStr = permission.toString();

            // 解析权限表达式，格式如：ARTICLE_DELETE_OWN 或 ARTICLE_DELETE_ANY
            if (permissionStr.contains("_")) {
                String[] parts = permissionStr.split("_");
                if (parts.length >= 3) {
                    String resource = parts[0];        // ARTICLE
                    String action = parts[1];          // DELETE
                    String condition = parts[2];       // OWN 或 ANY

                    return checkPermission(user, resource, action, condition, targetId);
                }
            }

            // 简单权限检查
            return hasSimplePermission(user, permissionStr);

        } catch (Exception e) {
            logger.error("Error checking permission", e);
            return false;
        }
    }

    /**
     * 检查用户是否有指定类型的权限
     *
     * @param authentication 当前认证用户
     * @param targetType 目标对象类型
     * @param permission 权限名称
     * @return 是否有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetType, Object permission) {
        // 这种方法签名用于类型级别的权限检查，我们可以委托给上面的方法
        return hasPermission(authentication, targetType, permission);
    }

    /**
     * 检查用户的简单权限
     */
    private boolean hasSimplePermission(User user, String permissionName) {
        List<Permission> permissions = userService.getUserPermissions(user.getId());
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName) && p.getStatus() == 1);
    }

    /**
     * 检查用户的复杂权限（包含条件的权限）
     */
    private boolean checkPermission(User user, String resource, String action, String condition, Object targetId) {
        List<Permission> permissions = userService.getUserPermissions(user.getId());

        // 检查是否有ANY级别的权限（对资源有完全操作权限）
        boolean hasAnyPermission = permissions.stream()
                .anyMatch(p -> p.getResource().equals(resource)
                        && p.getAction().equals(action)
                        && "ANY".equals(p.getCondition())
                        && p.getStatus() == 1);

        if (hasAnyPermission) {
            return true;
        }

        // 检查OWN级别的权限（只能操作自己的资源）
        boolean hasOwnPermission = permissions.stream()
                .anyMatch(p -> p.getResource().equals(resource)
                        && p.getAction().equals(action)
                        && "OWN".equals(p.getCondition())
                        && p.getStatus() == 1);

        if (hasOwnPermission) {
            return checkOwnership(user, resource, targetId);
        }

        return false;
    }

    /**
     * 检查用户是否拥有指定资源的所有权
     */
    private boolean checkOwnership(User user, String resource, Object targetId) {
        if (targetId == null) {
            return false;
        }

        try {
            Long resourceId = Long.valueOf(targetId.toString());

            switch (resource.toLowerCase()) {
                case "article":
                    return userService.isArticleOwner(user.getId(), resourceId);
                case "user":
                    return user.getId().equals(resourceId);
                case "message":
                    return userService.isMessageOwner(user.getId(), resourceId);
                case "group":
                    return userService.isGroupOwner(user.getId(), resourceId);
                default:
                    logger.warn("Unknown resource type for ownership check: {}", resource);
                    return false;
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid target ID format: {}", targetId);
            return false;
        } catch (Exception e) {
            logger.error("Error checking ownership for resource: {}, targetId: {}", resource, targetId, e);
            return false;
        }
    }
}