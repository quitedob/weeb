package com.web.mapper;

import com.web.model.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联数据访问层接口
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 插入角色权限关联
     * @param rolePermission 角色权限关联对象
     * @return 影响的行数
     */
    int insert(RolePermission rolePermission);

    /**
     * 批量插入角色权限关联
     * @param rolePermissions 角色权限关联列表
     * @return 影响的行数
     */
    int batchInsert(@Param("rolePermissions") List<RolePermission> rolePermissions);

    /**
     * 更新角色权限关联
     * @param rolePermission 角色权限关联对象
     * @return 影响的行数
     */
    int update(RolePermission rolePermission);

    /**
     * 删除角色权限关联
     * @param id 主键ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据角色ID删除角色权限关联
     * @param roleId 角色ID
     * @return 影响的行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID删除角色权限关联
     * @param permissionId 权限ID
     * @return 影响的行数
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 批量删除角色权限关联
     * @param ids ID列表
     * @return 影响的行数
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID查询角色权限关联
     * @param id 主键ID
     * @return 角色权限关联对象
     */
    RolePermission findById(@Param("id") Long id);

    /**
     * 根据角色ID查询权限关联列表
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    List<RolePermission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID查询角色关联列表
     * @param permissionId 权限ID
     * @return 角色权限关联列表
     */
    List<RolePermission> findByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 查询角色权限关联列表（带分页）
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @param roleId 角色ID（可选）
     * @param permissionId 权限ID（可选）
     * @param status 状态（可选）
     * @return 角色权限关联列表
     */
    List<RolePermission> findWithPaging(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId,
            @Param("status") Integer status
    );

    /**
     * 统计角色权限关联数量
     * @param roleId 角色ID（可选）
     * @param permissionId 权限ID（可选）
     * @param status 状态（可选）
     * @return 总数
     */
    long count(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId,
            @Param("status") Integer status
    );

    /**
     * 统计角色的权限数量
     * @param roleId 角色ID
     * @return 权限数量
     */
    long countByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查角色是否拥有指定权限
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 是否拥有权限
     */
    boolean existsByRoleIdAndPermissionId(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId
    );

    /**
     * 获取角色的所有权限ID列表
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 获取权限的所有角色ID列表
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    List<Long> findRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 根据角色ID和权限状态查询关联
     * @param roleId 角色ID
     * @param status 状态
     * @return 关联列表
     */
    List<RolePermission> findByRoleIdAndStatus(
            @Param("roleId") Long roleId,
            @Param("status") Integer status
    );

    /**
     * 更新角色权限关联状态
     * @param id 主键ID
     * @param status 状态
     * @return 影响的行数
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );

    /**
     * 批量更新角色权限关联状态
     * @param ids ID列表
     * @param status 状态
     * @return 影响的行数
     */
    int batchUpdateStatus(
            @Param("ids") List<Long> ids,
            @Param("status") Integer status
    );
}