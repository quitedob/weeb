package com.web.service.Impl;

import com.web.mapper.RoleMapper;
import com.web.mapper.UserMapper;
import com.web.mapper.UserRoleMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.Role;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.service.UserCreationService;
import com.web.util.WeebException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户创建服务实现
 * 提供用户创建的完整业务流程，确保数据一致性和事务安全
 */
@Service
@Slf4j
public class UserCreationServiceImpl implements UserCreationService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserStatsMapper userStatsMapper;
    private final RoleMapper roleMapper;

    @Autowired
    public UserCreationServiceImpl(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            UserStatsMapper userStatsMapper,
            RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userStatsMapper = userStatsMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUserWithDependencies(User user) {
        log.info("开始创建用户及其依赖数据: username={}", user.getUsername());

        try {
            // Step 1: 验证创建前置条件
            validateUserCreationPrerequisites(user);

            // Step 2: 创建用户基本信息
            log.debug("创建用户基本信息");
            userMapper.insertUser(user);

            if (user.getId() == null) {
                throw new WeebException("用户创建失败：未获取到用户ID");
            }

            log.info("用户基本信息创建成功: userId={}, username={}", user.getId(), user.getUsername());

            // Step 3: 分配默认角色
            log.debug("为用户分配默认角色");
            assignDefaultRole(user.getId());

            // Step 4: 初始化用户统计数据
            log.debug("初始化用户统计数据");
            initializeUserStats(user.getId());

            log.info("用户及其依赖数据创建完成: userId={}, username={}", user.getId(), user.getUsername());

            return user;

        } catch (Exception e) {
            log.error("创建用户失败: username={}, error={}", user.getUsername(), e.getMessage(), e);

            // 事务会自动回滚，这里只需要重新抛出异常
            if (e instanceof WeebException) {
                throw e;
            } else {
                throw new WeebException("用户创建失败：" + e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDefaultRole(Long userId) {
        log.debug("开始为用户分配默认角色: userId={}", userId);

        try {
            // 查找默认角色（is_default = true）
            Role defaultRole = roleMapper.selectDefaultRole();

            if (defaultRole != null) {
                // 分配默认角色
                int rowsAffected = userRoleMapper.assignRoleToUser(userId, defaultRole.getId());

                if (rowsAffected <= 0) {
                    throw new WeebException("默认角色分配失败：未更新任何记录");
                }

                log.info("默认角色分配成功: userId={}, roleId={}, roleName={}",
                        userId, defaultRole.getId(), defaultRole.getName());
                return;
            }

            // 如果没有默认角色，尝试查找"USER"角色
            log.warn("未找到默认角色，尝试查找'USER'角色");
            Role userRole = roleMapper.selectByName("USER");

            if (userRole != null) {
                int rowsAffected = userRoleMapper.assignRoleToUser(userId, userRole.getId());

                if (rowsAffected <= 0) {
                    throw new WeebException("USER角色分配失败：未更新任何记录");
                }

                log.info("USER角色分配成功: userId={}, roleId={}", userId, userRole.getId());
                return;
            }

            // 尝试查找"用户"角色（中文角色名）
            log.warn("未找到'USER'角色，尝试查找'用户'角色");
            Role chineseUserRole = roleMapper.selectByName("用户");

            if (chineseUserRole != null) {
                int rowsAffected = userRoleMapper.assignRoleToUser(userId, chineseUserRole.getId());

                if (rowsAffected <= 0) {
                    throw new WeebException("用户角色分配失败：未更新任何记录");
                }

                log.info("用户角色分配成功: userId={}, roleId={}", userId, chineseUserRole.getId());
                return;
            }

            // 所有角色查找都失败
            throw new WeebException("系统配置错误：未找到任何可用的用户角色，请联系管理员");

        } catch (WeebException e) {
            log.error("角色分配失败: userId={}, error={}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("角色分配时发生未知错误: userId={}, error={}", userId, e.getMessage(), e);
            throw new WeebException("角色分配失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeUserStats(Long userId) {
        log.debug("开始初始化用户统计数据: userId={}", userId);

        try {
            // 检查是否已存在统计数据
            UserStats existingStats = userStatsMapper.selectByUserId(userId);

            if (existingStats != null) {
                log.debug("用户统计数据已存在，跳过初始化: userId={}", userId);
                return;
            }

            // 创建新的用户统计数据
            UserStats userStats = new UserStats(userId);
            userStats.setUserLevel(1);        // 初始等级为1（基础用户）
            userStats.setLoginCount(0L);       // 初始登录次数为0
            userStats.setArticleCount(0L);      // 初始文章数为0
            userStats.setFollowerCount(0L);     // 初始粉丝数为0
            userStats.setFollowingCount(0L);     // 初始关注数为0

            int rowsAffected = userStatsMapper.insertUserStats(userStats);

            if (rowsAffected <= 0) {
                throw new WeebException("用户统计数据初始化失败：未插入任何记录");
            }

            log.info("用户统计数据初始化成功: userId={}, statsId={}", userId, userStats.getId());

        } catch (WeebException e) {
            log.error("用户统计数据初始化失败: userId={}, error={}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户统计数据初始化时发生未知错误: userId={}, error={}", userId, e.getMessage(), e);
            throw new WeebException("用户统计数据初始化失败：" + e.getMessage());
        }
    }

    @Override
    public void validateUserCreationPrerequisites(User user) {
        log.debug("验证用户创建前置条件: username={}", user.getUsername());

        // 验证用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            throw new WeebException("用户名已存在：" + user.getUsername());
        }

        // 验证邮箱是否已存在
        if (user.getUserEmail() != null && !user.getUserEmail().trim().isEmpty()) {
            User existingEmailUser = userMapper.selectByEmail(user.getUserEmail());
            if (existingEmailUser != null) {
                throw new WeebException("邮箱已被使用：" + user.getUserEmail());
            }
        }

        log.debug("用户创建前置条件验证通过: username={}", user.getUsername());
    }
}