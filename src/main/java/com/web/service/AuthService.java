package com.web.service;

import com.web.model.User;

import java.util.List;
import java.util.Map;

/**
 * 认证服务接口，定义用户注册、登录、登出、获取用户信息、更新用户等方法
 */
public interface AuthService {
    // 根据用户ID获取用户基本信息
    User getAuthById(Long id);

    // 更新用户的基本信息
    boolean updateAuth(User user);
    /**
     * 根据用户名查询用户ID
     * @param username 用户名
     * @return 用户ID
     */
    Long findUserIdByUsername(String username);

    /**
     * 用户注册
     * @param user 用户信息
     */
    void register(User user);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功后生成的JWT令牌
     */
    String login(String username, String password);

    /**
     * 用户登出
     * @param token 要登出的JWT令牌
     */
    void logout(String token);

    /**
     * 根据用户ID查询用户信息
     * @param userID 用户ID
     * @return 用户信息
     */
    User findByUserID(Long userID);

    /**
     * 通过 Redis 中的 Token 获取当前用户信息
     * @param token JWT令牌
     * @return 用户信息（不包含密码）
     */
    User getUserInfo(String token);

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户信息
     * @param email 用户邮箱
     * @return 用户信息
     */
    User findByEmail(String email);

    /**
     * 根据用户名查询用户信息，并获取注册日期
     * @param username 用户名
     * @return 用户信息（带注册日期）
     */
    User findDateByUsername(String username);

    /**
     * 用户上线动作（WebSocket 使用）
     * @param userId 用户ID
     */
    void online(Long userId);

    /**
     * 用户下线动作（WebSocket 使用）
     * @param userId 用户ID
     */
    void offline(Long userId);

    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    List<User> listUser();

    /**
     * 获取在线用户列表
     * @return 在线用户的ID列表（本示例中用 String 表示，也可直接用 Long）
     */
    List<String> onlineWeb();

    /**
     * 获取所有用户映射，键为用户ID
     * @return 用户Map，键为用户ID
     */
    Map<Long, User> listMapUser();

    /**
     * 更新用户信息（只更新允许更新的字段）
     * @param user 要更新的用户对象
     * @return 是否更新成功
     */
    boolean updateUser(User user);

    User getUserByIdForTalk(Long userId);

    User getUserById(Integer userId);
    User updateUser(Integer userId, com.web.vo.user.UpdateUserVo updateUserVo);

    /**
     * 获取用户完整信息（包含统计数据）
     * @param userId 用户ID
     * @return 用户完整信息
     */
    com.web.model.UserWithStats getUserWithStats(Long userId);
}
