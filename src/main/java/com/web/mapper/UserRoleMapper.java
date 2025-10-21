package com.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 用户角色关联数据访问接口
 * 提供用户角色关联的数据库操作
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 插入成功的数量
     */
    int assignRoleToUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 从用户移除角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 删除成功的数量
     */
    int removeRoleFromUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 获取用户的角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的角色名称列表
     * @param userId 用户ID
     * @return 角色名称列表
     */
    List<String> selectRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的权限名称集合
     * @param userId 用户ID
     * @return 权限名称集合
     */
    Set<String> selectPermissionNamesByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否拥有指定角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否拥有
     */
    boolean hasRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 检查用户是否拥有指定角色名称
     * @param userId 用户ID
     * @param roleName 角色名称
     * @return 是否拥有
     */
    boolean hasRoleByName(@Param("userId") Long userId, @Param("roleName") String roleName);

    /**
     * 检查用户是否拥有指定权限
     * @param userId 用户ID
     * @param permissionName 权限名称
     * @return 是否拥有
     */
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionName") String permissionName);

    /**
     * 清空用户的所有角色
     * @param userId 用户ID
     * @return 删除成功的数量
     */
    int clearUserRoles(@Param("userId") Long userId);

    /**
     * 为用户分配多个角色
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 插入成功的数量
     */
    int assignRolesToUser(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}