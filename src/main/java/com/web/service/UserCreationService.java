package com.web.service;

import com.web.model.Role;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.util.WeebException;

/**
 * 用户创建服务
 * 负责处理用户创建的完整业务流程，确保数据一致性和事务安全
 */
public interface UserCreationService {

    /**
     * 创建新用户及其相关数据
     * 在单个事务中完成用户创建、角色分配、统计数据初始化等操作
     *
     * @param user 用户基本信息
     * @return 创建成功的用户信息
     * @throws WeebException 创建失败时抛出异常
     */
    User createUserWithDependencies(User user);

    /**
     * 为用户分配默认角色
     * 确保角色分配的事务安全性
     *
     * @param userId 用户ID
     * @throws WeebException 角色分配失败时抛出异常
     */
    void assignDefaultRole(Long userId);

    /**
     * 初始化用户统计数据
     * 为新用户创建对应的统计数据记录
     *
     * @param userId 用户ID
     * @throws WeebException 初始化失败时抛出异常
     */
    void initializeUserStats(Long userId);

    /**
     * 验证用户创建的前置条件
     * 检查用户名、邮箱等是否已存在
     *
     * @param user 待创建的用户信息
     * @throws WeebException 验证失败时抛出异常
     */
    void validateUserCreationPrerequisites(User user);
}