package com.web.service.impl;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.service.AuthService;
import com.web.constant.UserOnlineStatus;
import com.web.util.JwtUtil;
import com.web.util.SecurityAuditUtils;
import com.web.util.ValidationUtils;
import com.web.Config.SecurityConfig;
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
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(AuthMapper authMapper, UserMapper userMapper, PasswordEncoder passwordEncoder, RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil) {
        this.authMapper = authMapper;
        this.userMapper = userMapper;
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
    public void updateAuth(User user) {
        authMapper.updateAuth(user);
    }

    @Override
    public Long findUserIdByUsername(String username) {
        return authMapper.findUserIdByUsername(username);
    }

    @Override
    public void register(User user) {
        // 参数验证
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        if (!ValidationUtils.validateUsername(user.getUsername())) {
            throw new RuntimeException("用户名不符合要求：" + SecurityConfig.UsernamePolicy.REQUIREMENT);
        }
        if (!ValidationUtils.validatePassword(user.getPassword())) {
            throw new RuntimeException("密码不符合要求：" + SecurityConfig.PasswordPolicy.REQUIREMENT);
        }
        if (!ValidationUtils.validateEmail(user.getUserEmail())) {
            throw new RuntimeException("邮箱格式不正确：" + SecurityConfig.EmailPolicy.REQUIREMENT);
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (!ValidationUtils.validatePhone(user.getPhoneNumber())) {
                throw new RuntimeException("手机号格式不正确：" + SecurityConfig.PhonePolicy.REQUIREMENT);
            }
        }
        
        // 检查用户名是否已存在
        User existingUser = authMapper.findByUsername(user.getUsername().trim());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        Long existingEmailCount = authMapper.countByEmail(user.getUserEmail().trim());
        if (existingEmailCount != null && existingEmailCount > 0) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 记录注册事件
        Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
        SecurityAuditUtils.logRegistration(user.getUsername(), user.getUserEmail(), 
                requestInfo.get("ip"), requestInfo.get("userAgent"));
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        // 设置注册时间
        user.setRegistrationDate(new Date());
        // 设置默认在线状态
        user.setOnlineStatus(UserOnlineStatus.OFFLINE.getCode());
        // 设置默认用户状态为启用
        user.setStatus(1);
        
        authMapper.insertUser(user);
    }

    @Override
    public String login(String username, String password) {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure("", requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "用户名为空");
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "密码为空");
            throw new RuntimeException("密码不能为空");
        }
        
        // 检查账号是否被锁定
        if (SecurityAuditUtils.isAccountLocked(username)) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "账号已被锁定");
            throw new RuntimeException("账号已被锁定，请稍后再试");
        }
        
        // 查找用户
        User user = authMapper.findByUsername(username.trim());
        if (user == null) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "用户不存在");
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "用户账号已被禁用");
            throw new RuntimeException("用户账号已被禁用");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password.trim(), user.getPassword())) {
            Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
            SecurityAuditUtils.logLoginFailure(username, requestInfo.get("ip"), 
                    requestInfo.get("userAgent"), "密码错误");
            throw new RuntimeException("密码错误");
        }

        // 记录登录成功事件
        Map<String, String> requestInfo = SecurityAuditUtils.getCurrentRequestInfo();
        SecurityAuditUtils.logLoginSuccess(username, requestInfo.get("ip"), 
                requestInfo.get("userAgent"));

        // 生成真正的JWT令牌
        String token = jwtUtil.generateToken(user.getId());

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
    public void logout(String token) {
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
    public User findDateByUsername(String username) {
        return authMapper.findDateByUsername(username);
    }

    @Override
    public void online(Long userId) {
        // 更新数据库中的在线状态
        userMapper.updateOnlineStatus(userId, UserOnlineStatus.ONLINE.getCode());
        // 将用户ID添加到Redis在线用户集合
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
    }

    @Override
    public void offline(Long userId) {
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
    public User getUserByIdForTalk(Long userID) {
        return authMapper.getUserByIdForTalk(userID);
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
        // TODO: 实现获取用户统计信息的逻辑
        // 这里需要根据实际需求实现，可能需要关联查询user_stats表
        return null;
    }
}