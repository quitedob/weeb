package com.web.service.impl;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.service.AuthService;
import com.web.constant.UserOnlineStatus;
import com.web.util.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (user.getUserEmail() == null || user.getUserEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
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
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        // 设置注册时间
        user.setRegistrationDate(new Date());
        // 设置默认在线状态
        user.setOnlineStatus(UserOnlineStatus.OFFLINE.getCode());
        
        authMapper.insertUser(user);
    }

    @Override
    public String login(String username, String password) {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        
        // 查找用户
        User user = authMapper.findByUsername(username.trim());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("用户账号已被禁用");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password.trim(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

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
    public List<String> onlineWeb() {
        Set<Object> onlineUserIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        List<String> result = new ArrayList<>();
        if (onlineUserIds != null) {
            for (Object userId : onlineUserIds) {
                result.add(userId.toString());
            }
        }
        return result;
    }

    @Override
    public Map<Long, User> listMapUser() {
        return authMapper.listMapUser();
    }

    @Override
    public boolean updateUser(User user) {
        int result = authMapper.updateUser(user);
        return result > 0;
    }

    @Override
    public User getUserByIdForTalk(Long userId) {
        return authMapper.getUserByIdForTalk(userId);
    }

    @Override
    public User getUserById(Integer userId) {
        return authMapper.findByUserID(userId.longValue());
    }

    @Override
    public User updateUser(Integer userId, com.web.vo.user.UpdateUserVo updateUserVo) {
        User user = authMapper.findByUserID(userId.longValue());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息 - 根据UpdateUserVo实际可用字段
        if (updateUserVo.getUsername() != null) {
            user.setUsername(updateUserVo.getUsername());
        }
        if (updateUserVo.getAvatar() != null) {
            user.setAvatar(updateUserVo.getAvatar());
        }
        // 注意：根据错误信息，UpdateUserVo可能只有username和avatar字段
        // 如果需要更多字段，需要在UpdateUserVo中添加
        
        authMapper.updateUser(user);
        return user;
    }

    @Override
    public UserWithStats getUserWithStats(Long userId) {
        return userMapper.selectUserWithStatsById(userId);
    }
}