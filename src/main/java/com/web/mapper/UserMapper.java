package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.User;
import com.web.model.UserWithStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户Mapper接口
 * 提供用户基本信息的数据库访问方法，统计数据相关操作请使用UserStatsMapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 更新用户的在线状态
     * @param userId 用户ID
     * @param onlineStatus 在线状态码
     */
    @Update("UPDATE `user` SET online_status = #{onlineStatus} WHERE id = #{userId}")
    void updateOnlineStatus(@Param("userId") Long userId, @Param("onlineStatus") Integer onlineStatus);

    /**
     * 根据用户ID获取用户基本信息
     * @param userId 用户ID
     * @return 用户基本信息
     */
    User selectById(@Param("userId") Long userId);

    /**
     * 根据用户ID获取用户完整信息（包含统计数据）
     * @param userId 用户ID
     * @return 用户完整信息（包含统计数据）
     */
    UserWithStats selectUserWithStatsById(@Param("userId") Long userId);

    /**
     * 批量获取用户基本信息
     * @param userIds 用户ID列表
     * @return 用户基本信息列表
     */
    List<User> selectByIds(@Param("userIds") List<Long> userIds);

    /**
     * 批量获取用户完整信息（包含统计数据）
     * @param userIds 用户ID列表
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUsersWithStatsByIds(@Param("userIds") List<Long> userIds);

    /**
     * 根据用户名获取用户完整信息（包含统计数据）
     * @param username 用户名
     * @return 用户完整信息（包含统计数据）
     */
    UserWithStats selectUserWithStatsByUsername(@Param("username") String username);

    /**
     * 获取用户列表（分页支持）
     * @param type 用户类型（可选）
     * @param onlineStatus 在线状态（可选）
     * @param username 用户名模糊查询（可选）
     * @param orderBy 排序字段（可选）
     * @return 用户基本信息列表
     */
    List<User> selectUserList(@Param("type") Integer type, 
                             @Param("onlineStatus") Integer onlineStatus,
                             @Param("username") String username,
                             @Param("orderBy") String orderBy);

    /**
     * 获取用户列表（包含统计数据，分页支持）
     * @param type 用户类型（可选）
     * @param onlineStatus 在线状态（可选）
     * @param username 用户名模糊查询（可选）
     * @param minFansCount 最小粉丝数（可选）
     * @param minTotalLikes 最小点赞数（可选）
     * @param orderBy 排序字段（可选）
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUserListWithStats(@Param("type") Integer type,
                                               @Param("onlineStatus") Integer onlineStatus,
                                               @Param("username") String username,
                                               @Param("minFansCount") Long minFansCount,
                                               @Param("minTotalLikes") Long minTotalLikes,
                                               @Param("orderBy") String orderBy);

    /**
     * 更新用户基本信息（不包含统计数据）
     * 注意：统计数据更新请使用UserStatsMapper
     * @param user 用户对象（包含需要更新的字段）
     * @return 受影响行数
     */
    int updateUser(User user);

    /**
     * 删除用户（注意：统计数据会通过外键级联删除）
     * @param userId 用户ID
     * @return 受影响行数
     */
    int deleteById(@Param("userId") Long userId);

    /**
     * 统计用户总数
     * @param type 用户类型（可选）
     * @param onlineStatus 在线状态（可选）
     * @return 用户总数
     */
    long countUsers(@Param("type") Integer type, @Param("onlineStatus") Integer onlineStatus);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 搜索用户（分页）
     * @param keyword 搜索关键词（用户名或昵称）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> searchUsers(@Param("keyword") String keyword, 
                          @Param("offset") int offset, 
                          @Param("limit") int limit);

    /**
     * 统计搜索用户的总数
     * @param keyword 搜索关键词
     * @return 总数
     */
    long countSearchUsers(@Param("keyword") String keyword);

    /**
     * 根据用户ID查找用户（兼容测试用）
     * @param userId 用户ID
     * @return 用户信息
     */
    User selectUserById(@Param("userId") Long userId);

    /**
     * 根据用户名查找用户（兼容测试用）
     * @param username 用户名
     * @return 用户信息
     */
    User selectUserByUsername(@Param("username") String username);

    /**
     * 插入用户（兼容测试用）
     * @param user 用户对象
     * @return 影响行数
     */
    int insertUser(User user);

    /**
     * 带过滤条件的搜索用户
     * @param keyword 关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param sortClause 排序子句
     * @return 用户列表
     */
    List<User> searchUsersWithFilters(@Param("keyword") String keyword,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate,
                                     @Param("sortClause") String sortClause);

    /**
     * 统计带过滤条件的搜索用户数量
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总数
     */
    long countSearchUsersWithFilters(@Param("keyword") String keyword,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate);
}
