package com.web.mapper;

import com.web.model.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 权限数据访问接口
 * 提供权限相关的数据库操作
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限名称查询权限
     * @param name 权限名称
     * @return 权限对象
     */
    Permission selectByName(@Param("name") String name);

    /**
     * 根据资源和操作查询权限
     * @param resource 资源名称
     * @param action 操作名称
     * @return 权限对象
     */
    Permission selectByResourceAndAction(@Param("resource") String resource, @Param("action") String action);

    /**
     * 分页查询权限列表
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 权限列表
     */
    List<Permission> selectPermissionsWithPaging(@Param("keyword") String keyword,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);

    /**
     * 统计权限数量
     * @param keyword 搜索关键词
     * @return 权限总数
     */
    int countPermissions(@Param("keyword") String keyword);

    /**
     * 检查权限名称是否存在
     * @param name 权限名称
     * @return 是否存在
     */
    boolean existsByName(@Param("name") String name);

    /**
     * 获取所有权限组
     * @return 权限组名称集合
     */
    Set<String> selectPermissionGroups();

    /**
     * 根据权限组查询权限
     * @param group 权限组名称
     * @return 权限列表
     */
    List<Permission> selectByGroup(@Param("group") String group);

    /**
     * 批量插入权限
     * @param permissions 权限列表
     * @return 插入成功的数量
     */
    int batchInsert(@Param("permissions") List<Permission> permissions);
}