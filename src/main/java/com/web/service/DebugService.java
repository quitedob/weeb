package com.web.service;

import com.web.model.Permission;
import com.web.model.Role;

import java.util.List;
import java.util.Map;

/**
 * 调试服务接口
 * 提供系统调试和信息查询功能
 */
public interface DebugService {

    /**
     * 获取用户权限信息
     * @param userId 用户ID
     * @return 包含用户角色和权限的映射
     */
    Map<String, Object> getUserPermissions(Long userId);

    /**
     * 获取所有权限列表
     * @return 包含权限列表和总数的映射
     */
    Map<String, Object> getAllPermissions();

    /**
     * 获取所有角色列表
     * @return 包含角色列表和总数的映射
     */
    Map<String, Object> getAllRoles();

    /**
     * 检查系统健康状态
     * @return 系统健康状态信息
     */
    Map<String, Object> getSystemHealth();

    /**
     * 获取数据库连接状态
     * @return 数据库连接信息
     */
    Map<String, Object> getDatabaseStatus();

    /**
     * 获取系统配置信息
     * @return 系统配置
     */
    Map<String, Object> getSystemConfiguration();
}