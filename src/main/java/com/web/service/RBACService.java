package com.web.service;

import java.util.Set;

/**
 * RBAC权限检查服务接口
 * 提供基于角色的访问控制权限检查
 */
public interface RBACService {

    /**
     * 检查用户是否有指定权限
     * @param userId 用户ID
     * @param permission 权限名称
     * @return 是否有权限
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * 检查用户是否有任一指定权限
     * @param userId 用户ID
     * @param permissions 权限名称数组
     * @return 是否有任一权限
     */
    boolean hasAnyPermission(Long userId, String... permissions);

    /**
     * 检查用户是否有所有指定权限
     * @param userId 用户ID
     * @param permissions 权限名称数组
     * @return 是否有所有权限
     */
    boolean hasAllPermissions(Long userId, String... permissions);

    /**
     * 获取用户的所有权限集合
     * @param userId 用户ID
     * @return 权限名称集合
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * 检查用户是否为管理员
     * @param userId 用户ID
     * @return 是否为管理员
     */
    boolean isAdmin(Long userId);

    /**
     * 检查用户是否为超级管理员
     * @param userId 用户ID
     * @return 是否为超级管理员
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 检查用户是否有权限执行指定操作
     * @param userId 用户ID
     * @param resource 资源名称（如：user, article）
     * @param action 操作名称（如：create, read, update, delete）
     * @return 是否有权限
     */
    boolean hasPermissionForResource(Long userId, String resource, String action);

    /**
     * 检查用户是否有权限执行指定操作（带条件判断）
     * @param userId 用户ID
     * @param resource 资源名称
     * @param action 操作名称
     * @param condition 权限条件（如：own, any）
     * @param resourceOwnerId 资源所有者ID（用于own条件判断）
     * @return 是否有权限
     */
    boolean hasPermissionForResource(Long userId, String resource, String action, String condition, Long resourceOwnerId);

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否分配成功
     */
    boolean assignRoleToUser(Long userId, Long roleId);

    /**
     * 从用户移除角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否移除成功
     */
    boolean removeRoleFromUser(Long userId, Long roleId);

    /**
     * 清除用户权限缓存
     * @param userId 用户ID
     */
    void clearUserPermissionCache(Long userId);

    /**
     * 刷新所有用户权限缓存
     */
    void refreshAllPermissionCache();

    /**
     * 检查用户是否有权限访问指定资源
     * @param userId 用户ID
     * @param resourceType 资源类型（如：article, user）
     * @param resourceId 资源ID
     * @param action 操作类型（如：read, update, delete）
     * @return 是否有权限
     */
    boolean checkResourceAccess(Long userId, String resourceType, Long resourceId, String action);

    /**
     * 获取用户的角色集合
     * @param userId 用户ID
     * @return 角色名称集合
     */
    Set<String> getUserRoles(Long userId);

    /**
     * 检查角色是否有指定权限
     * @param roleName 角色名称
     * @param permission 权限名称
     * @return 是否有权限
     */
    boolean roleHasPermission(String roleName, String permission);

    /**
     * 验证用户是否有权限执行操作，如果没有则抛出异常
     * @param userId 用户ID
     * @param permission 权限名称
     * @throws SecurityException 如果没有权限
     */
    void requirePermission(Long userId, String permission) throws SecurityException;

    /**
     * 验证用户是否有权限执行操作（带条件判断）
     * @param userId 用户ID
     * @param resource 资源名称
     * @param action 操作名称
     * @param condition 权限条件
     * @param resourceOwnerId 资源所有者ID
     * @throws SecurityException 如果没有权限
     */
    void requirePermissionForResource(Long userId, String resource, String action, String condition, Long resourceOwnerId) throws SecurityException;
}
