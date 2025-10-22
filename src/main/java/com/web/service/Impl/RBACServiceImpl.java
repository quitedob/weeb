package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.UserRoleMapper;
import com.web.model.Role;
import com.web.service.RBACService;
import com.web.service.RoleService;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RBAC权限检查服务实现类
 * 提供基于角色的访问控制权限检查实现
 */
@Slf4j
@Service
@Transactional
public class RBACServiceImpl implements RBACService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis缓存配置
    private static final String USER_PERMISSIONS_CACHE_PREFIX = "user_permissions:";
    private static final String USER_ROLES_CACHE_PREFIX = "user_roles:";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    @Override
    public boolean hasPermission(Long userId, String permission) {
        try {
            if (userId == null || userId <= 0) {
                return false;
            }
            if (permission == null || permission.trim().isEmpty()) {
                return false;
            }

            // 验证权限格式
            String safePermission = ValidationUtils.sanitizePermission(permission.trim());
            if (safePermission.length() > 100) {
                log.warn("权限名称过长: {}", permission);
                return false;
            }

            // 首先检查缓存
            String cacheKey = USER_PERMISSIONS_CACHE_PREFIX + userId;
            Set<String> userPermissions = (Set<String>) redisTemplate.opsForValue().get(cacheKey);

            if (userPermissions == null) {
                // 缓存未命中，从数据库查询
                userPermissions = getUserPermissionsFromDB(userId);
                // 存入缓存
                redisTemplate.opsForValue().set(cacheKey, userPermissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }

            return userPermissions.contains(safePermission);
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissions) {
        try {
            if (userId == null || permissions == null || permissions.length == 0) {
                return false;
            }

            Set<String> userPermissions = getUserPermissions(userId);
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("检查用户任一权限失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean hasAllPermissions(Long userId, String... permissions) {
        try {
            if (userId == null || permissions == null || permissions.length == 0) {
                return false;
            }

            Set<String> userPermissions = getUserPermissions(userId);
            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查用户所有权限失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        try {
            if (userId == null) {
                return Collections.emptySet();
            }

            // 首先检查缓存
            String cacheKey = USER_PERMISSIONS_CACHE_PREFIX + userId;
            Set<String> userPermissions = (Set<String>) redisTemplate.opsForValue().get(cacheKey);

            if (userPermissions == null) {
                // 缓存未命中，从数据库查询
                userPermissions = getUserPermissionsFromDB(userId);
                // 存入缓存
                redisTemplate.opsForValue().set(cacheKey, userPermissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }

            return userPermissions;
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public boolean isAdmin(Long userId) {
        try {
            if (userId == null) {
                return false;
            }

            // 管理员角色包括：超级管理员、管理员
            return hasRoleByName(userId, "超级管理员") || hasRoleByName(userId, "管理员");
        } catch (Exception e) {
            log.error("检查用户是否为管理员失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean isSuperAdmin(Long userId) {
        try {
            if (userId == null) {
                return false;
            }

            return hasRoleByName(userId, "超级管理员");
        } catch (Exception e) {
            log.error("检查用户是否为超级管理员失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean hasPermissionForResource(Long userId, String resource, String action) {
        try {
            if (userId == null || userId <= 0) {
                return false;
            }
            if (resource == null || resource.trim().isEmpty()) {
                return false;
            }
            if (action == null || action.trim().isEmpty()) {
                return false;
            }

            // 验证并清理资源名称和操作
            String safeResource = ValidationUtils.sanitizeResourceName(resource.trim());
            String safeAction = ValidationUtils.sanitizeActionName(action.trim());

            if (safeResource.length() > 50 || safeAction.length() > 50) {
                log.warn("资源名称或操作名称过长: resource={}, action={}", resource, action);
                return false;
            }

            // 构建权限名称：RESOURCE_ACTION
            String permission = safeResource.toUpperCase() + "_" + safeAction.toUpperCase();
            return hasPermission(userId, permission);
        } catch (Exception e) {
            log.error("检查用户资源权限失败: userId={}, resource={}, action={}", userId, resource, action, e);
            return false;
        }
    }

    @Override
    public boolean hasPermissionForResource(Long userId, String resource, String action, String condition, Long resourceOwnerId) {
        try {
            if (userId == null || resource == null || action == null) {
                return false;
            }

            // 首先检查通用权限
            if (hasPermissionForResource(userId, resource, action)) {
                return true;
            }

            // 检查条件权限
            if ("own".equalsIgnoreCase(condition) && resourceOwnerId != null) {
                // 如果是own条件，检查用户是否是资源所有者
                if (userId.equals(resourceOwnerId)) {
                    // 构建own权限名称：RESOURCE_ACTION_OWN
                    String ownPermission = resource.toUpperCase() + "_" + action.toUpperCase() + "_OWN";
                    return hasPermission(userId, ownPermission);
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查用户条件资源权限失败: userId={}, resource={}, action={}, condition={}",
                    userId, resource, action, condition, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean assignRoleToUser(Long userId, Long roleId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (roleId == null || roleId <= 0) {
            throw new WeebException("角色ID必须为正数");
        }

        try {
            // 检查用户是否已经拥有该角色
            if (userRoleMapper.hasRole(userId, roleId)) {
                log.warn("用户已拥有该角色: userId={}, roleId={}", userId, roleId);
                return true;
            }

            // 分配角色
            int result = userRoleMapper.assignRoleToUser(userId, roleId);
            if (result > 0) {
                // 清除用户权限缓存
                clearUserPermissionCache(userId);
                log.info("为用户分配角色成功: userId={}, roleId={}", userId, roleId);
                return true;
            } else {
                throw new WeebException("分配角色失败");
            }
        } catch (Exception e) {
            log.error("为用户分配角色失败: userId={}, roleId={}", userId, roleId, e);
            throw new WeebException("分配角色失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean removeRoleFromUser(Long userId, Long roleId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (roleId == null || roleId <= 0) {
            throw new WeebException("角色ID必须为正数");
        }

        try {
            // 检查用户是否拥有该角色
            if (!userRoleMapper.hasRole(userId, roleId)) {
                log.warn("用户不拥有该角色: userId={}, roleId={}", userId, roleId);
                return true;
            }

            // 移除角色
            int result = userRoleMapper.removeRoleFromUser(userId, roleId);
            if (result > 0) {
                // 清除用户权限缓存
                clearUserPermissionCache(userId);
                log.info("从用户移除角色成功: userId={}, roleId={}", userId, roleId);
                return true;
            } else {
                throw new WeebException("移除角色失败");
            }
        } catch (Exception e) {
            log.error("从用户移除角色失败: userId={}, roleId={}", userId, roleId, e);
            throw new WeebException("移除角色失败: " + e.getMessage());
        }
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        try {
            if (userId == null) {
                return;
            }

            // 清除权限缓存
            String permissionsCacheKey = USER_PERMISSIONS_CACHE_PREFIX + userId;
            redisTemplate.delete(permissionsCacheKey);

            // 清除角色缓存
            String rolesCacheKey = USER_ROLES_CACHE_PREFIX + userId;
            redisTemplate.delete(rolesCacheKey);

            log.debug("清除用户权限缓存成功: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户权限缓存失败: userId={}", userId, e);
        }
    }

    @Override
    public void refreshAllPermissionCache() {
        try {
            // 清除所有用户权限缓存
            Set<String> keys = redisTemplate.keys(USER_PERMISSIONS_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            // 清除所有用户角色缓存
            keys = redisTemplate.keys(USER_ROLES_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            log.info("刷新所有用户权限缓存成功");
        } catch (Exception e) {
            log.error("刷新所有用户权限缓存失败", e);
            throw new WeebException("刷新权限缓存失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkResourceAccess(Long userId, String resourceType, Long resourceId, String action) {
        try {
            if (userId == null || resourceType == null || action == null) {
                return false;
            }

            // 首先检查通用权限
            if (hasPermissionForResource(userId, resourceType, action)) {
                return true;
            }

            // 实现细粒度的权限检查逻辑
            // 检查用户是否是资源的所有者
            if (isResourceOwner(userId, resourceType, resourceId)) {
                // 资源所有者拥有所有权限
                log.debug("用户 {} 是资源 {} {} 的所有者，允许访问", userId, resourceType, resourceId);
                return true;
            }

            // 检查资源的共享设置
            if (isResourceShared(userId, resourceType, resourceId)) {
                log.debug("用户 {} 可以访问共享资源 {} {}", userId, resourceType, resourceId);
                return true;
            }

            // 检查特定资源的访问权限（如文章、群组等）
            if (checkSpecificResourceAccess(userId, resourceType, resourceId, action)) {
                return true;
            }

            log.debug("用户 {} 无权访问资源 {} {}", userId, resourceType, resourceId);
            return false;
        } catch (Exception e) {
            log.error("检查用户资源访问权限失败: userId={}, resourceType={}, resourceId={}, action={}",
                    userId, resourceType, resourceId, action, e);
            return false;
        }
    }

    @Override
    public Set<String> getUserRoles(Long userId) {
        try {
            if (userId == null) {
                return Collections.emptySet();
            }

            // 首先检查缓存
            String cacheKey = USER_ROLES_CACHE_PREFIX + userId;
            Set<String> userRoles = (Set<String>) redisTemplate.opsForValue().get(cacheKey);

            if (userRoles == null) {
                // 缓存未命中，从数据库查询
                List<String> roleNames = userRoleMapper.selectRoleNamesByUserId(userId);
                userRoles = new HashSet<>(roleNames);
                // 存入缓存
                redisTemplate.opsForValue().set(cacheKey, userRoles, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }

            return userRoles;
        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public boolean roleHasPermission(String roleName, String permission) {
        try {
            if (roleName == null || permission == null) {
                return false;
            }

            // 获取角色
            Role role = roleService.getRoleByName(roleName);
            if (role == null || role.getStatus() == 0) {
                return false;
            }

            // 检查角色是否拥有该权限
            return roleService.getRolePermissionNames(role.getId()).contains(permission);
        } catch (Exception e) {
            log.error("检查角色权限失败: roleName={}, permission={}", roleName, permission, e);
            return false;
        }
    }

    @Override
    public void requirePermission(Long userId, String permission) throws SecurityException {
        if (!hasPermission(userId, permission)) {
            throw new SecurityException("用户没有权限: " + permission);
        }
    }

    @Override
    public void requirePermissionForResource(Long userId, String resource, String action, String condition, Long resourceOwnerId) throws SecurityException {
        if (!hasPermissionForResource(userId, resource, action, condition, resourceOwnerId)) {
            throw new SecurityException("用户没有权限执行操作: " + resource + "." + action);
        }
    }

    /**
     * 从数据库获取用户权限集合
     */
    private Set<String> getUserPermissionsFromDB(Long userId) {
        try {
            Set<String> permissions = userRoleMapper.selectPermissionNamesByUserId(userId);
            return permissions != null ? permissions : Collections.emptySet();
        } catch (Exception e) {
            log.error("从数据库获取用户权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 检查用户是否拥有指定角色名称
     */
    private boolean hasRoleByName(Long userId, String roleName) {
        try {
            return userRoleMapper.hasRoleByName(userId, roleName);
        } catch (Exception e) {
            log.error("检查用户角色失败: userId={}, roleName={}", userId, roleName, e);
            return false;
        }
    }

    /**
     * 检查用户是否是资源的所有者
     */
    private boolean isResourceOwner(Long userId, String resourceType, Long resourceId) {
        try {
            switch (resourceType.toLowerCase()) {
                case "article":
                    return isArticleOwner(userId, resourceId);
                case "group":
                    return isGroupOwner(userId, resourceId);
                case "comment":
                    return isCommentOwner(userId, resourceId);
                case "file":
                    return isFileOwner(userId, resourceId);
                default:
                    log.warn("未知的资源类型: {}", resourceType);
                    return false;
            }
        } catch (Exception e) {
            log.error("检查资源所有者失败: userId={}, resourceType={}, resourceId={}",
                    userId, resourceType, resourceId, e);
            return false;
        }
    }

    /**
     * 检查资源是否对用户共享
     */
    private boolean isResourceShared(Long userId, String resourceType, Long resourceId) {
        try {
            switch (resourceType.toLowerCase()) {
                case "article":
                    return isArticleShared(userId, resourceId);
                case "group":
                    return isGroupMember(userId, resourceId);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("检查资源共享状态失败: userId={}, resourceType={}, resourceId={}",
                    userId, resourceType, resourceId, e);
            return false;
        }
    }

    /**
     * 检查特定资源的访问权限
     */
    private boolean checkSpecificResourceAccess(Long userId, String resourceType, Long resourceId, String action) {
        try {
            // 检查群组成员权限
            if ("group".equals(resourceType.toLowerCase())) {
                return checkGroupMemberAccess(userId, resourceId, action);
            }

            // 检查文章访问权限
            if ("article".equals(resourceType.toLowerCase())) {
                return checkArticleAccess(userId, resourceId, action);
            }

            return false;
        } catch (Exception e) {
            log.error("检查特定资源访问权限失败: userId={}, resourceType={}, resourceId={}, action={}",
                    userId, resourceType, resourceId, action, e);
            return false;
        }
    }

    /**
     * 检查文章所有者
     */
    private boolean isArticleOwner(Long userId, Long articleId) {
        // 这里应该查询数据库验证文章所有者
        // 临时实现：假设用户ID 1 是管理员，拥有所有权限
        if (userId.equals(1L)) {
            return true;
        }
        // 实际实现应该查询 article 表的 author_id 字段
        return false;
    }

    /**
     * 检查群组所有者
     */
    private boolean isGroupOwner(Long userId, Long groupId) {
        // 这里应该查询数据库验证群组所有者
        // 临时实现：假设用户ID 1 是管理员，拥有所有权限
        if (userId.equals(1L)) {
            return true;
        }
        // 实际实现应该查询 group 表的 owner_id 字段
        return false;
    }

    /**
     * 检查评论所有者
     */
    private boolean isCommentOwner(Long userId, Long commentId) {
        // 这里应该查询数据库验证评论所有者
        if (userId.equals(1L)) {
            return true;
        }
        // 实际实现应该查询 comment 表的 user_id 字段
        return false;
    }

    /**
     * 检查文件所有者
     */
    private boolean isFileOwner(Long userId, Long fileId) {
        // 这里应该查询数据库验证文件所有者
        if (userId.equals(1L)) {
            return true;
        }
        // 实际实现应该查询 file_record 表的 user_id 字段
        return false;
    }

    /**
     * 检查文章是否共享给用户
     */
    private boolean isArticleShared(Long userId, Long articleId) {
        // 检查文章是否公开或者用户有访问权限
        // 实际实现应该检查文章的可见性设置
        return false;
    }

    /**
     * 检查用户是否是群组成员
     */
    private boolean isGroupMember(Long userId, Long groupId) {
        // 这里应该查询 group_member 表
        // 临时实现：管理员用户ID 1 自动是所有群组成员
        if (userId.equals(1L)) {
            return true;
        }
        return false;
    }

    /**
     * 检查群组成员访问权限
     */
    private boolean checkGroupMemberAccess(Long userId, Long groupId, String action) {
        // 检查用户是否是群组成员以及相应的操作权限
        if (isGroupMember(userId, groupId)) {
            // 群组成员可以查看群组信息，发送消息等
            return "read".equalsIgnoreCase(action) ||
                   "message".equalsIgnoreCase(action) ||
                   "view".equalsIgnoreCase(action);
        }
        return false;
    }

    /**
     * 检查文章访问权限
     */
    private boolean checkArticleAccess(Long userId, Long articleId, String action) {
        // 检查文章的公开性和用户权限
        if ("read".equalsIgnoreCase(action)) {
            // 读取权限：文章公开或者用户有特殊权限
            return isArticleShared(userId, articleId);
        }
        return false;
    }
}