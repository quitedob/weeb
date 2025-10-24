package com.web.service;

import com.web.model.Role;
import com.web.model.Permission;
import com.web.model.User;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色权限服务接口
 * 处理角色权限自动分配和管理逻辑
 */
public interface RolePermissionService {

    /**
     * 为用户自动分配角色基于其等级
     * @param userId 用户ID
     * @return 分配结果
     */
    Map<String, Object> autoAssignRoles(Long userId);

    /**
     * 根据用户等级获取应该拥有的角色
     * @param userLevel 用户等级
     * @return 角色列表
     */
    List<Role> getRolesForUserLevel(int userLevel);

    /**
     * 检查用户是否拥有指定角色
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 是否拥有角色
     */
    boolean hasRole(Long userId, String roleName);

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param operatorId 操作者ID
     * @return 分配结果
     */
    boolean assignRole(Long userId, Long roleId, Long operatorId);

    /**
     * 移除用户角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param operatorId 操作者ID
     * @return 移除结果
     */
    boolean removeRole(Long userId, Long roleId, Long operatorId);

    /**
     * 获取用户的所有角色
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getUserRoles(Long userId);

    /**
     * 获取角色的所有权限
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getRolePermissions(Long roleId);

    /**
     * 获取用户的所有权限（包括角色权限和个人权限）
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> getAllUserPermissions(Long userId);

    /**
     * 检查用户是否有特定权限
     * @param userId 用户ID
     * @param permission 权限名称
     * @return 是否有权限
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * 批量自动分配角色给多个用户
     * @param userIds 用户ID列表
     * @return 批量分配结果
     */
    Map<String, Object> batchAutoAssignRoles(List<Long> userIds);

    /**
     * 创建或更新角色权限映射
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    boolean updateRolePermissions(Long roleId, List<Long> permissionIds, Long operatorId);

    /**
     * 获取角色分配统计
     * @return 统计信息
     */
    Map<String, Object> getRoleAssignmentStatistics();

    /**
     * 检查角色权限映射是否正确
     * @param roleId 角色ID
     * @return 验证结果
     */
    Map<String, Object> validateRolePermissions(Long roleId);

    /**
     * 同步用户角色权限（当用户等级变更时）
     * @param userId 用户ID
     * @param oldLevel 原等级
     * @param newLevel 新等级
     * @return 同步结果
     */
    Map<String, Object> syncUserRolesOnLevelChange(Long userId, int oldLevel, int newLevel);

    /**
     * 获取角色权限变更建议
     * @param userLevel 用户等级
     * @return 建议的角色权限配置
     */
    Map<String, Object> getRolePermissionRecommendations(int userLevel);

    /**
     * 应用角色权限模板
     * @param templateName 模板名称
     * @param targetRoleId 目标角色ID
     * @param operatorId 操作者ID
     * @return 应用结果
     */
    boolean applyRolePermissionTemplate(String templateName, Long targetRoleId, Long operatorId);
}