package com.web.service;

import com.web.model.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色服务接口
 * 提供角色管理的业务逻辑
 */
public interface RoleService {

    /**
     * 创建角色
     * @param role 角色对象
     * @return 创建的角色
     */
    Role createRole(Role role);

    /**
     * 更新角色
     * @param role 角色对象
     * @return 更新后的角色
     */
    Role updateRole(Role role);

    /**
     * 删除角色
     * @param roleId 角色ID
     * @return 是否删除成功
     */
    boolean deleteRole(Long roleId);

    /**
     * 根据ID获取角色
     * @param roleId 角色ID
     * @return 角色对象（包含权限列表）
     */
    Role getRoleById(Long roleId);

    /**
     * 根据角色名称获取角色
     * @param name 角色名称
     * @return 角色对象（包含权限列表）
     */
    Role getRoleByName(String name);

    /**
     * 获取所有角色
     * @return 角色列表（包含权限列表）
     */
    List<Role> getAllRoles();

    /**
     * 分页获取角色列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @param status 状态筛选（可选）
     * @return 角色列表和总数
     */
    Map<String, Object> getRolesWithPaging(int page, int pageSize, String keyword, String status);

    /**
     * 根据角色名称检查角色是否存在
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 为角色分配权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否分配成功
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 从角色移除权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否移除成功
     */
    boolean removePermissionsFromRole(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<String> getRolePermissions(Long roleId);

    /**
     * 获取角色的权限名称集合
     * @param roleId 角色ID
     * @return 权限名称集合
     */
    Set<String> getRolePermissionNames(Long roleId);

    /**
     * 复制角色权限
     * @param sourceRoleId 源角色ID
     * @param targetRoleId 目标角色ID
     * @return 是否复制成功
     */
    boolean copyRolePermissions(Long sourceRoleId, Long targetRoleId);

    /**
     * 初始化系统角色（创建默认的管理员、用户等角色）
     * @return 初始化结果
     */
    Map<String, Object> initializeSystemRoles();

    /**
     * 创建默认角色集合
     * @return 默认角色列表
     */
    List<Role> createDefaultRoles();

    /**
     * 获取用户角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getUserRoles(Long userId);

    /**
     * 检查角色是否为系统角色（不可删除）
     * @param roleId 角色ID
     * @return 是否为系统角色
     */
    boolean isSystemRole(Long roleId);
}
