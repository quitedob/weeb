package com.web.mapper;

import com.web.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AuthMapper 接口，负责操作数据库中与用户（auth 表）相关的 CRUD 以及其他查询
 */
@Mapper
public interface AuthMapper {

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户信息
     * @param email 邮箱
     * @return 用户对象
     */
    User findByEmail(@Param("email") String email);

    /**
     * 统计邮箱数量
     * @param userEmail 邮箱
     * @return 邮箱数量
     */
    Long countByEmail(@Param("userEmail") String userEmail);

    /**
     * 插入新用户
     * @param user 用户对象
     */
    void insertUser(User user);

    /**
     * 根据用户名只查询用户ID
     * @param username 用户名
     * @return 用户ID
     */
    Long findUserIdByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户信息
     * @param userID 用户ID
     * @return 用户对象
     */
    User findByUserID(@Param("userID") Long userID);

    /**
     * 根据用户名查询用户信息，包括注册日期
     * @param username 用户名
     * @return 用户对象，包含注册日期
     */
    User findDateByUsername(@Param("username") String username);

    /**
     * 获取所有用户列表，按类型降序排序（如果有 type 字段）
     * @return 用户列表
     */
    List<User> listUser();

    /**
     * 获取所有用户映射，键为用户ID
     * @return 用户Map
     */
    Map<Long, User> listMapUser();

    /**
     * 根据用户ID获取用户信息，用于聊天时展示
     * @param userID 用户ID
     * @return 用户对象
     */
    User getUserByIdForTalk(@Param("userID") Long userID);

    /**
     * 更新用户信息，只更新允许更新的字段
     * @param user 用户对象（包含需要更新的字段）
     * @return 受影响行数
     */
    int updateUser(User user);

    // 根据用户ID获取用户基本信息
    User selectAuthById(@Param("id") Long id);

    // 更新用户的基本信息（username, userEmail, phoneNumber, sex）
    int updateAuth(User user);

    /**
     * 根据用户ID删除用户
     * @param userId 用户ID
     * @return 受影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);
}
