package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
// RBAC相关mapper已删除
// import com.web.mapper.UserRoleMapper;
// import com.web.mapper.RoleMapper;
import com.web.model.User;
import com.web.model.UserWithStats;
// RBAC相关model已删除
// import com.web.model.Role;
import com.web.service.AuthService;
import com.web.service.UserCreationService;
import com.web.constant.UserOnlineStatus;
import com.web.util.JwtUtil;
import com.web.util.SecurityAuditUtils;
import com.web.util.ValidationUtils;
import com.web.Config.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

/**
 * 认证服务实现类
 * 处理用户注册、登录、登出等认证相关业务
 */
@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    // RBAC相关mapper已删除
    // private final UserRoleMapper userRoleMapper;
    // private final RoleMapper roleMapper;
    private final UserCreationService userCreationService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AuthMapper authMapper, UserMapper userMapper, UserCreationService userCreationService, PasswordEncoder passwordEncoder, RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil) {
        this.authMapper = authMapper;
        this.userMapper = userMapper;
        // RBAC相关mapper已删除
        // this.userRoleMapper = userRoleMapper;
        // this.roleMapper = roleMapper;
        this.userCreationService = userCreationService;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String USER_TOKEN_PREFIX = "user_token:";
    private static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60; // 24小时

    @Override
    public User getAuthById(Long id) {
        return authMapper.selectAuthById(id);
    }

    @Override
    public boolean updateUser(User user) {
        int result = authMapper.updateUser(user);
        return result > 0;
    }


    /**
     * 更新用户认证信息（内部方法）
     * @param user 用户对象
     */
    public boolean updateAuth(User user) {
        return authMapper.updateAuth(user) > 0;
    }

    @Override
    public Long findUserIdByUsername(String username) {
        return authMapper.findUserIdByUsername(username);
    }

    @Override
    public User authenticate(String username, String password) {
        try {
            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return null;
            }
            if (password == null || password.trim().isEmpty()) {
                return null;
            }

            // 验证并清理输入
            String safeUsername = ValidationUtils.sanitizeUsername(username.trim());

            // 查找用户
            User user = authMapper.findByUsername(safeUsername);
            if (user == null) {
                return null;
            }

            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                return null;
            }

            // 验证密码
            if (!passwordEncoder.matches(password.trim(), user.getPassword())) {
                return null;
            }

            // 不返回密码信息
            user.setPassword(null);
            return user;

        } catch (Exception e) {
            log.error("用户认证失败: username={}", username, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        // 参数验证
        if (user == null) {
            throw new WeebException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new WeebException("用户名不能为空");
        }
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            throw new WeebException("用户名长度必须在3-20个字符之间");
        }
        if (!ValidationUtils.validateUsername(user.getUsername())) {
            throw new WeebException("用户名不符合要求：" + SecurityConstants.UsernamePolicy.REQUIREMENT);
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new WeebException("密码不能为空");
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 50) {
            throw new WeebException("密码长度必须在6-50个字符之间");
        }
        if (!ValidationUtils.validatePassword(user.getPassword())) {
            throw new WeebException("密码不符合要求：" + SecurityConstants.PasswordPolicy.REQUIREMENT);
        }
        if (user.getUserEmail() == null || user.getUserEmail().trim().isEmpty()) {
            throw new WeebException("邮箱不能为空");
        }
        if (user.getUserEmail().length() > 100) {
            throw new WeebException("邮箱长度不能超过100个字符");
        }
        if (!ValidationUtils.validateEmail(user.getUserEmail())) {
            throw new WeebException("邮箱格式不正确：" + SecurityConstants.EmailPolicy.REQUIREMENT);
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (user.getPhoneNumber().length() > 20) {
                throw new WeebException("手机号长度不能超过20个字符");
            }
            if (!ValidationUtils.validatePhone(user.getPhoneNumber())) {
                throw new WeebException("手机号格式不正确：" + SecurityConstants.PhonePolicy.REQUIREMENT);
            }
        }
        if (user.getNickname() != null && user.getNickname().length() > 50) {
            throw new WeebException("昵称长度不能超过50个字符");
        }
        if (user.getBio() != null && user.getBio().length() > 200) {
            throw new WeebException("个人简介长度不能超过200个字符");
        }

        // 验证并清理输入
        String safeUsername = ValidationUtils.sanitizeUsername(user.getUsername().trim());
        String safeEmail = ValidationUtils.sanitizeEmail(user.getUserEmail().trim());
        String safePhone = user.getPhoneNumber() != null ?
                ValidationUtils.sanitizePhone(user.getPhoneNumber().trim()) : null;

        // 检查用户名是否已存在
        User existingUser = authMapper.findByUsername(safeUsername);
        if (existingUser != null) {
            throw new WeebException("用户名已存在");
        }

        // 检查邮箱是否已存在
        Long existingEmailCount = authMapper.countByEmail(safeEmail);
        if (existingEmailCount != null && existingEmailCount > 0) {
            throw new WeebException("邮箱已被注册");
        }

        // 记录注册事件
        Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
        SecurityAuditUtils.logRegistration(safeUsername, safeEmail,
                requestInfo.get("ip"), requestInfo.get("userAgent"));

        // 设置清理后的数据
        user.setUsername(safeUsername);
        user.setUserEmail(safeEmail);
        user.setPhoneNumber(safePhone);

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        // 设置注册时间
        user.setRegistrationDate(new Date());
        // 设置默认在线状态
        user.setOnlineStatus(UserOnlineStatus.OFFLINE.getCode());
        // 设置默认用户状态为启用
        user.setStatus(1);

        // 使用UserCreationService创建用户及其相关数据，确保事务安全
        userCreationService.createUserWithDependencies(user);
    }

    @Override
    @Transactional
    public String login(String username, String password) {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure("", requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "用户名为空");
            throw new WeebException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "密码为空");
            throw new WeebException("密码不能为空");
        }
        if (username.length() > 50 || password.length() > 100) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "用户名或密码过长");
            throw new WeebException("用户名或密码长度超出限制");
        }

        // 验证并清理输入
        String safeUsername = ValidationUtils.sanitizeUsername(username.trim());

        // 检查账号是否被锁定
        if (SecurityAuditUtils.isAccountLocked(safeUsername)) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(safeUsername, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "账号已被锁定");
            throw new WeebException("账号已被锁定，请稍后再试");
        }

        // 查找用户
        User user = authMapper.findByUsername(safeUsername);
        if (user == null) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(safeUsername, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "用户不存在");
            throw new WeebException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(safeUsername, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "用户账号已被禁用");
            throw new WeebException("用户账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(password.trim(), user.getPassword())) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(safeUsername, requestInfo.get("ip"),
                    requestInfo.get("userAgent"), "密码错误");
            throw new WeebException("密码错误");
        }

        // 记录登录成功事件
        Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
        SecurityAuditUtils.logLoginSuccess(safeUsername, requestInfo.get("ip"),
                requestInfo.get("userAgent"));

        // 生成真正的JWT令牌（携带用户名声明，便于过滤器按用户名校验）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 将用户信息存储到Redis（用于快速查询）
        redisTemplate.opsForValue().set(USER_TOKEN_PREFIX + token, user, TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);

        // 更新登录时间
        user.setLoginTime(new Date());
        authMapper.updateUser(user);

        // 设置用户在线状态
        online(user.getId());

        return token;
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new WeebException("token不能为空");
        }
        if (token.length() > 500) {
            throw new WeebException("token长度超出限制");
        }

        // 从Redis获取用户信息
        User user = (User) redisTemplate.opsForValue().get(USER_TOKEN_PREFIX + token);
        if (user != null) {
            // 记录登出事件
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logSessionManagement(user.getUsername(), "logout",
                    requestInfo.get("ip"), requestInfo.get("userAgent"));

            // 设置用户离线状态
            offline(user.getId());
            // 删除Redis中的token
            redisTemplate.delete(USER_TOKEN_PREFIX + token);
        }
    }

    @Override
    public User findByUserID(Long userID) {
        return authMapper.findByUserID(userID);
    }

    @Override
    public User getUserInfo(String token) {
        User user = (User) redisTemplate.opsForValue().get(USER_TOKEN_PREFIX + token);
        if (user != null) {
            // 不返回密码信息
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        return authMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return authMapper.findByEmail(email);
    }

    @Override
    public User findDateByUsername(String username) {
        return authMapper.findDateByUsername(username);
    }

    @Override
    public void online(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        // 更新数据库中的在线状态
        userMapper.updateOnlineStatus(userId, UserOnlineStatus.ONLINE.getCode());
        // 将用户ID添加到Redis在线用户集合
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
    }

    @Override
    public void offline(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        // 更新数据库中的在线状态
        userMapper.updateOnlineStatus(userId, UserOnlineStatus.OFFLINE.getCode());
        // 从Redis在线用户集合中移除用户ID
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
    }

    @Override
    public List<User> listUser() {
        return authMapper.listUser();
    }

    @Override
    public Map<Long, User> listMapUser() {
        return authMapper.listMapUser();
    }

    @Override
    public List<String> onlineWeb() {
        Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        return members.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList());
    }

    
    @Override
    public User getUserById(Integer userId) {
        return authMapper.selectAuthById(userId.longValue());
    }

    @Override
    public User updateUser(Integer userId, com.web.vo.user.UpdateUserVo updateUserVo) {
        User user = new User();
        user.setId(userId.longValue());
        user.setUsername(updateUserVo.getUsername());
        user.setAvatar(updateUserVo.getAvatar());
        user.setNickname(updateUserVo.getNickname());
        user.setBio(updateUserVo.getBio());
        boolean result = updateUser(user);
        if (result) {
            return getUserById(userId);
        }
        return null;
    }

    @Override
    public com.web.model.UserWithStats getUserWithStats(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }

            // 获取用户基本信息和统计信息
            com.web.model.UserWithStats userWithStats = authMapper.selectUserWithStatsById(userId);

            if (userWithStats == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 不返回密码信息
            userWithStats.getUser().setPassword(null);

            return userWithStats;
        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户统计信息失败: userId={}", userId, e);
            throw new WeebException("获取用户统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public boolean sendPasswordResetEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new WeebException("邮箱地址不能为空");
            }

            // 验证邮箱格式
            ValidationUtils.validateEmail(email);

            // 检查用户是否存在
            User user = findByEmail(email);
            if (user == null) {
                // 为安全起见，不暴露用户是否存在的信息
                log.info("尝试重置不存在用户的密码: {}", email);
                return true; // 仍然返回true以避免泄露用户信息
            }

            // 生成重置令牌（暂时使用普通JWT令牌，实际应该使用专门的重置令牌）
            String resetToken = jwtUtil.generateToken(user.getId(), user.getUsername());

            // TODO: 发送邮件逻辑
            // 这里应该集成邮件服务发送重置邮件
            log.info("发送密码重置邮件到: {}，令牌: {}", email, resetToken);

            // 暂时记录到日志，实际应该发送邮件
            log.warn("密码重置功能需要实现邮件发送服务。令牌: {}", resetToken);

            return true;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送密码重置邮件失败: email={}", email, e);
            throw new WeebException("发送密码重置邮件失败: " + e.getMessage());
        }
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new WeebException("重置令牌不能为空");
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new WeebException("新密码不能为空");
            }

            // 验证密码强度
            ValidationUtils.validatePassword(newPassword);

            // 验证令牌
            if (!jwtUtil.validateToken(token)) {
                throw new WeebException("重置令牌无效或已过期");
            }

            // 从令牌中提取用户ID
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在");
            }

            // 更新密码
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);

            // 保存到数据库
            int updateResult = userMapper.updateUser(user);
            if (updateResult <= 0) {
                throw new WeebException("密码更新失败");
            }

            // 使所有旧的JWT令牌失效（可选）
            // redisTemplate.delete("jwt:" + userId);

            log.info("用户密码重置成功: userId={}", userId);
            return true;

        } catch (WeebException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置密码失败: token={}", token, e);
            throw new WeebException("重置密码失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyResetToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            return jwtUtil.validateToken(token);

        } catch (Exception e) {
            log.error("验证重置令牌失败: token={}", token, e);
            return false;
        }
    }

    }