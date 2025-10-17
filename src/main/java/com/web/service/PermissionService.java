package com.web.service;

import com.web.model.Permission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限服务接口
 * 提供权限管理的业务逻辑
 */
public interface PermissionService {

    /**
     * 创建权限
     * @param permission 权限对象
     * @return 创建的权限
     */
    Permission createPermission(Permission permission);

    /**
     * 更新权限
     * @param permission 权限对象
     * @return 更新后的权限
     */
    Permission updatePermission(Permission permission);

    /**
     * 删除权限
     * @param permissionId 权限ID
     * @return 是否删除成功
     */
    boolean deletePermission(Long permissionId);

    /**
     * 根据ID获取权限
     * @param permissionId 权限ID
     * @return 权限对象
     */
    Permission getPermissionById(Long permissionId);

    /**
     * 根据权限名称获取权限
     * @param name 权限名称
     * @return 权限对象
     */
    Permission getPermissionByName(String name);

    /**
     * 获取所有权限
     * @return 权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 分页获取权限列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 权限列表和总数
     */
    Map<String, Object> getPermissionsWithPaging(int page, int pageSize, String keyword);

    /**
     * 根据权限名称检查权限是否存在
     * @param name 权限名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据资源和操作获取权限
     * @param resource 资源名称
     * @param action 操作名称
     * @return 权限对象
     */
    Permission getPermissionByResourceAndAction(String resource, String action);

    /**
     * 获取权限组列表
     * @return 权限组名称列表
     */
    Set<String> getPermissionGroups();

    /**
     * 根据权限组获取权限列表
     * @param group 权限组名称
     * @return 权限列表
     */
    List<Permission> getPermissionsByGroup(String group);

    /**
     * 批量创建权限（用于初始化系统权限）
     * @param permissions 权限列表
     * @return 创建成功的权限数量
     */
    int batchCreatePermissions(List<Permission> permissions);

    /**
     * 初始化系统权限（创建所有预定义的权限）
     * @return 初始化结果
     */
    Map<String, Object> initializeSystemPermissions();
}
