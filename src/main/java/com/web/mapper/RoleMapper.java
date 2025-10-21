package com.web.mapper;

import com.web.model.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色数据访问接口
 * 提供角色相关的数据库操作
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色名称查询角色
     * @param name 角色名称
     * @return 角色对象
     */
    Role selectByName(@Param("name") String name);

    /**
     * 分页查询角色列表
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 角色列表
     */
    List<Role> selectRolesWithPaging(@Param("keyword") String keyword,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    /**
     * 统计角色数量
     * @param keyword 搜索关键词
     * @return 角色总数
     */
    int countRoles(@Param("keyword") String keyword);

    /**
     * 检查角色名称是否存在
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(@Param("name") String name);

    /**
     * 获取用户角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 获取默认角色
     * @return 默认角色
     */
    Role selectDefaultRole();

    /**
     * 根据角色ID获取包含权限的角色
     * @param roleId 角色ID
     * @return 包含权限的角色对象
     */
    Role selectRoleWithPermissions(@Param("roleId") Long roleId);

    /**
     * 为角色分配权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 插入成功的数量
     */
    int assignPermissionsToRole(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 从角色移除权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 删除成功的数量
     */
    int removePermissionsFromRole(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 获取角色的权限ID列表
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 获取角色的权限名称列表
     * @param roleId 角色ID
     * @return 权限名称列表
     */
    List<String> selectPermissionNamesByRoleId(@Param("roleId") Long roleId);

    /**
     * 清空角色的所有权限
     * @param roleId 角色ID
     * @return 删除成功的数量
     */
    int clearRolePermissions(@Param("roleId") Long roleId);

    /**
     * 复制角色权限
     * @param sourceRoleId 源角色ID
     * @param targetRoleId 目标角色ID
     * @return 复制成功的数量
     */
    int copyRolePermissions(@Param("sourceRoleId") Long sourceRoleId, @Param("targetRoleId") Long targetRoleId);

    /**
     * 检查角色是否为系统角色
     * @param roleId 角色ID
     * @return 是否为系统角色
     */
    boolean isSystemRole(@Param("roleId") Long roleId);
}