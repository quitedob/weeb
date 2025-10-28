package com.web.service;

/**
 * 用户类型安全服务接口
 * 基于简化的用户类型系统（ADMIN/USER/BOT）进行权限检查
 * 替代复杂的RBAC权限系统
 */
public interface UserTypeSecurityService {

    /**
     * 检查用户是否为管理员
     * @param username 用户名
     * @return 是否为管理员
     */
    boolean isAdmin(String username);

    /**
     * 检查用户是否为机器人
     * @param username 用户名
     * @return 是否为机器人
     */
    boolean isBot(String username);

    /**
     * 检查用户是否为普通用户
     * @param username 用户名
     * @return 是否为普通用户
     */
    boolean isRegularUser(String username);

    /**
     * 获取用户类型
     * @param username 用户名
     * @return 用户类型（ADMIN/USER/BOT）
     */
    String getUserType(String username);

    /**
     * 检查用户是否可以访问管理员功能
     * @param username 用户名
     * @return 是否可以访问
     */
    boolean canAccessAdminFeatures(String username);

    /**
     * 检查用户是否可以执行写操作
     * @param username 用户名
     * @return 是否可以执行写操作
     */
    boolean canWrite(String username);

    /**
     * 检查用户是否可以执行读操作
     * @param username 用户名
     * @return 是否可以执行读操作
     */
    boolean canRead(String username);
}