package com.web.service.Impl;

import cn.hutool.json.JSONUtil;
import com.web.constant.NotifyType;
import com.web.dto.NotifyDto;
import com.web.mapper.AuthMapper;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.ChatListService;
import com.web.service.WebSocketService;
import com.web.util.CacheUtil;
import com.web.util.JwtUtil;
import com.web.mapper.UserMapper; // Import UserMapper
import com.web.exception.WeebException;
import com.web.vo.user.UpdateUserVo;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AuthService 的实现类，包含用户注册、登录、登出、获取用户信息、上线下线通知等功能
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private CacheUtil cacheUtil; // 自定义缓存工具类

    @Autowired
    @Lazy
    private WebSocketService webSocketService; // WebSocket 服务（懒加载，避免循环依赖）

    // 使用构造器注入，并添加@Lazy注解，避免循环依赖
    private final ChatListService chatListService;

    @Autowired
    public AuthServiceImpl(@Lazy ChatListService chatListService) {
        this.chatListService = chatListService;
    }

    @Autowired
    private AuthMapper authMapper;   // 改名后的 Mapper 注入

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil; // JWT 工具类，用于生成和解析 Token

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Redis 客户端

    /**
     * 根据用户名查询用户ID
     */
    @Override
    public Long findUserIdByUsername(String username) {
        User user = authMapper.findByUsername(username);
        return user == null ? null : user.getId();
    }

    /**
     * 用户注册
     */
    @Override
    public void register(User user) {
        // 检查用户名是否已存在
        User existingUser = authMapper.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 对用户密码进行加密后再插入
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        // 设置注册时间为主机北京时间（Asia/Shanghai）
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        Calendar calendar = Calendar.getInstance(tz);
        user.setRegistrationDate(calendar.getTime());

        // 设置type默认值为"user"
        user.setType("user");

        authMapper.insertUser(user);
    }
    /**
     * 用户登录，返回JWT令牌
     */
    @Override
    public String login(String username, String password) {
        System.out.println("尝试登录的用户名：" + username);
        System.out.println("尝试登录的密码：" + password);

        User user = authMapper.findByUsername(username);
        if (user == null) {
            System.out.println("未找到该用户");
            throw new RuntimeException("用户名或密码错误");
        }

        boolean passwordMatches = BCrypt.checkpw(password, user.getPassword());
        System.out.println("密码匹配结果: " + passwordMatches);

        if (passwordMatches) {
            // 生成JWT令牌，使用userId作为主体
            String token = jwtUtil.generateToken(user.getId());
            System.out.println("生成的JWT令牌: " + token);

            // 将令牌和userId存入Redis，并设置过期时间
            redisTemplate.opsForValue().set(token, String.valueOf(user.getId()), jwtUtil.getExpiration(), TimeUnit.MILLISECONDS);
            return token;
        }
        throw new RuntimeException("用户名或密码错误");
    }

    /**
     * 用户登出
     */
    @Override
    public void logout(String token) {
        // 从Redis中删除token
        Boolean hasKey = redisTemplate.hasKey(token);
        if (hasKey != null && hasKey) {
            redisTemplate.delete(token);
            System.out.println("成功从Redis中删除令牌: " + token);
        } else {
            System.out.println("令牌不存在或已过期: " + token);
        }
    }

    /**
     * 根据用户ID查询用户信息
     */
    @Override
    @Cacheable(value = "user", key = "#userID", unless = "#result == null")
    public User findByUserID(Long userID) {
        return authMapper.findByUserID(userID);
    }

    /**
     * 从Redis中获取token对应的userId，并查询用户信息
     */
    @Override
    public User getUserInfo(String token) {
        // 从Redis中获取 token 对应的 userId
        String userIdStr = redisTemplate.opsForValue().get(token);
        if (userIdStr == null) {
            throw new RuntimeException("无效的令牌或用户未登录");
        }
        Long userId = Long.valueOf(userIdStr);
        // 查询用户信息
        User user = authMapper.findByUserID(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 不返回密码等敏感信息
        user.setPassword(null);
        return user;
    }

    /**
     * 根据用户名查询用户信息
     */
    @Override
    public User findByUsername(String username) {
        return authMapper.findByUsername(username);
    }

    /**
     * 根据用户名查询用户信息，包括注册日期
     */
    @Override
    public User findDateByUsername(String username) {
        return authMapper.findDateByUsername(username);
    }

    /**
     * 用户上线通知
     */
    @Override
    public void online(Long userId) {
        // 用户上线通知
        NotifyDto<String> notifyDto = new NotifyDto<>(); // 指定泛型为字符串
        notifyDto.setTime(new Date()); // 设置当前时间
        notifyDto.setType(NotifyType.Web_Online); // 设置通知类型
        notifyDto.setContent(JSONUtil.toJsonStr(getUserByIdForTalk(userId))); // 设置通知内容
        // webSocketService.sendNotifyToGroup(notifyDto); // 发送通知 - 暂时注释掉
    }

    /**
     * 根据用户ID获取用户信息（简化版，用于聊天展示）
     */
    @Override
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    public User getUserByIdForTalk(Long userId) { // 修改参数类型为 Long
        return authMapper.getUserByIdForTalk(userId);
    }

    /**
     * 用户下线通知
     */
    @Override
    public void offline(Long userId) {
        // 用户下线通知
        NotifyDto<String> notifyDto = new NotifyDto<>(); // 指定泛型为字符串
        notifyDto.setTime(new Date());
        notifyDto.setType(NotifyType.Web_Offline);
        notifyDto.setContent(JSONUtil.toJsonStr(getUserByIdForTalk(userId)));

        // 更新未读消息为已读
        Long targetId = cacheUtil.getUserReadCache(userId);
        if (targetId != null) {
            chatListService.resetUnreadCount(userId, targetId);
        }

        // 发送用户下线通知
        // webSocketService.sendNotifyToGroup(notifyDto); // 暂时注释掉
    }

    /**
     * 获取所有用户列表
     */
    @Override
    public List<User> listUser() {
        return authMapper.listUser();
    }

    /**
     * 获取在线用户列表（此处转换为 List<String> 返回）
     */
    @Override
    public List<String> onlineWeb() {
        // 从 WebSocketService 中获取在线用户的 ID (Long)
        List<Long> onlineUserIds = webSocketService.getOnlineUser();
        List<String> result = new ArrayList<>();
        for (Long id : onlineUserIds) {
            result.add(String.valueOf(id)); // 转成字符串
        }
        return result;
    }

    /**
     * 获取所有用户，并按用户ID作为键存储在 Map 中
     * 解决方案：调用返回 List 的方法，然后在 Service 层转换为 Map
     * @return 返回 Map<Long, User>，key 为用户ID
     */
    @Override
    public Map<Long, User> listMapUser() {
        // 调用 mapper 方法，获取所有用户列表
        List<User> userList = authMapper.listUser(); // 获取所有用户，按 type 降序排序

        // 创建一个新的 HashMap 用于存放转换后的用户映射
        Map<Long, User> userMap = new HashMap<>(); // key 为用户ID，value 为对应的 User 对象

        // 遍历用户列表，将每个用户放入 Map 中
        for (User user : userList) {
            userMap.put(user.getId(), user); // 将用户的 id 作为 key 存入 Map 中
        }

        // 返回转换后的 Map
        return userMap;
    }
    /**
     * 更新用户信息（只更新允许更新的字段）
     */
    @Override
    public boolean updateUser(User user) {
        // 只更新允许的字段，比如 username, avatar 等
        int rows = authMapper.updateUser(user);
        return rows > 0; // 返回是否更新成功
    }
    /**
     * 根据用户ID获取用户的基本信息
     */
    @Override
    public User getAuthById(Long id) {
        return authMapper.selectAuthById(id);
    }

    /**
     * 更新用户的基本信息（仅更新 username, userEmail, phoneNumber, sex）
     */
    @Override
    @CacheEvict(value = "user", key = "#user.id")
    public void updateAuth(User user) {
        // 可添加用户是否存在的判断逻辑
        int rows = authMapper.updateAuth(user);
        if (rows == 0) {
            throw new RuntimeException("更新失败，用户不存在或数据未修改");
        }
    }

    @Override
    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User updateUser(Integer userId, UpdateUserVo updateUserVo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new WeebException("用户不存在");
        }

        // 仅当传入的字段不为空时才更新
        if (StringUtils.hasText(updateUserVo.getUsername())) {
            user.setUsername(updateUserVo.getUsername());
        }
        if (StringUtils.hasText(updateUserVo.getAvatar())) {
            user.setAvatar(updateUserVo.getAvatar());
        }

        userMapper.updateById(user);
        return user;
    }
}
