package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Permission;
import com.web.model.User;
import com.web.model.UserWithStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 高级搜索用户（支持多维度过滤）
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userTypes 用户类型列表
     * @param onlineStatus 在线状态
     * @param minFansCount 最小粉丝数
     * @param maxFansCount 最大粉丝数
     * @param minTotalLikes 最小总点赞数
     * @param maxTotalLikes 最大总点赞数
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 用户列表
     */
    List<User> searchUsersAdvanced(@Param("keyword") String keyword,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("startDate") String startDate,
                                  @Param("endDate") String endDate,
                                  @Param("userTypes") List<Integer> userTypes,
                                  @Param("onlineStatus") Integer onlineStatus,
                                  @Param("minFansCount") Long minFansCount,
                                  @Param("maxFansCount") Long maxFansCount,
                                  @Param("minTotalLikes") Long minTotalLikes,
                                  @Param("maxTotalLikes") Long maxTotalLikes,
                                  @Param("sortBy") String sortBy,
                                  @Param("sortOrder") String sortOrder);

    /**
     * 统计高级搜索用户数量
     * @param keyword 搜索关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userTypes 用户类型列表
     * @param onlineStatus 在线状态
     * @param minFansCount 最小粉丝数
     * @param maxFansCount 最大粉丝数
     * @param minTotalLikes 最小总点赞数
     * @param maxTotalLikes 最大总点赞数
     * @return 总数
     */
    long countSearchUsersAdvanced(@Param("keyword") String keyword,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate,
                                 @Param("userTypes") List<Integer> userTypes,
                                 @Param("onlineStatus") Integer onlineStatus,
                                 @Param("minFansCount") Long minFansCount,
                                 @Param("maxFansCount") Long maxFansCount,
                                 @Param("minTotalLikes") Long minTotalLikes,
                                 @Param("maxTotalLikes") Long maxTotalLikes);

    /**
     * 根据关键词搜索用户（包含统计数据，分页支持）
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUsersWithStatsByKeyword(@Param("keyword") String keyword,
                                                     @Param("offset") int offset,
                                                     @Param("pageSize") int pageSize);

    /**
     * 根据关键词和状态搜索用户（包含统计数据，分页支持）
     * @param keyword 搜索关键词
     * @param status 用户状态
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUsersWithStatsByKeywordAndStatus(@Param("keyword") String keyword,
                                                              @Param("status") Integer status,
                                                              @Param("offset") int offset,
                                                              @Param("pageSize") int pageSize);

    /**
     * 获取用户列表（包含统计数据，分页支持）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUsersWithStatsWithPaging(@Param("offset") int offset,
                                                      @Param("pageSize") int pageSize);

    /**
     * 获取用户列表（包含统计数据，分页支持，带状态过滤）
     * @param status 用户状态
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 用户完整信息列表（包含统计数据）
     */
    List<UserWithStats> selectUsersWithStatsWithPagingAndStatus(@Param("status") Integer status,
                                                               @Param("offset") int offset,
                                                               @Param("pageSize") int pageSize);

    /**
     * 根据关键词统计用户数量
     * @param keyword 搜索关键词
     * @return 用户数量
     */
    int countUsersByKeyword(@Param("keyword") String keyword);

    /**
     * 根据关键词和状态统计用户数量
     * @param keyword 搜索关键词
     * @param status 用户状态
     * @return 用户数量
     */
    int countUsersByKeywordAndStatus(@Param("keyword") String keyword,
                                    @Param("status") Integer status);

    /**
     * 统计用户总数
     * @return 用户总数
     */
    int countUsers();

    /**
     * 根据状态统计用户数量
     * @param status 用户状态
     * @return 用户数量
     */
    int countUsersByStatus(@Param("status") Integer status);

    /**
     * 统计活跃用户数量
     * @param sinceTime 活跃时间阈值
     * @return 活跃用户数量
     */
    int countActiveUsers(@Param("sinceTime") java.util.Date sinceTime);

    /**
     * 统计新用户数量
     * @param sinceTime 注册时间阈值
     * @return 新用户数量
     */
    int countNewUsers(@Param("sinceTime") java.util.Date sinceTime);

    /**
     * 统计被封禁用户数量
     * @return 被封禁用户数量
     */
    int countBannedUsers();

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

    /**
     * 获取用户的所有权限
     * @param userId 用户ID
     * @return 用户权限列表
     */
    @Select("SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1")
    List<Permission> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 获取文章的所有者ID
     * @param articleId 文章ID
     * @return 所有者用户ID
     */
    @Select("SELECT user_id FROM articles WHERE id = #{articleId}")
    Long selectArticleOwnerId(@Param("articleId") Long articleId);

    /**
     * 获取消息的所有者ID
     * @param messageId 消息ID
     * @return 所有者用户ID
     */
    @Select("SELECT from_id FROM message WHERE id = #{messageId}")
    Long selectMessageOwnerId(@Param("messageId") Long messageId);

    /**
     * 获取群组的所有者ID
     * @param groupId 群组ID
     * @return 所有者用户ID
     */
    @Select("SELECT owner_id FROM `group` WHERE id = #{groupId}")
    Long selectGroupOwnerId(@Param("groupId") Long groupId);
}
