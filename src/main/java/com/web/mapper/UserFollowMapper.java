package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户关注关系Mapper接口
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
    
    /**
     * 检查是否已关注
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 关注记录
     */
    UserFollow findFollowRelation(@Param("followerId") Long followerId, 
                                 @Param("followeeId") Long followeeId);
    
    /**
     * 获取关注列表（我关注的人）
     * @param followerId 关注者ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 关注列表
     */
    List<Map<String, Object>> getFollowingList(@Param("followerId") Long followerId, 
                                              @Param("offset") int offset, 
                                              @Param("limit") int limit);
    
    /**
     * 获取粉丝列表（关注我的人）
     * @param followeeId 被关注者ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 粉丝列表
     */
    List<Map<String, Object>> getFollowersList(@Param("followeeId") Long followeeId, 
                                              @Param("offset") int offset, 
                                              @Param("limit") int limit);
    
    /**
     * 获取关注数量
     * @param followerId 关注者ID
     * @return 关注数量
     */
    int getFollowingCount(@Param("followerId") Long followerId);
    
    /**
     * 获取粉丝数量
     * @param followeeId 被关注者ID
     * @return 粉丝数量
     */
    int getFollowersCount(@Param("followeeId") Long followeeId);
    
    /**
     * 删除关注关系
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 删除数量
     */
    int deleteFollowRelation(@Param("followerId") Long followerId, 
                           @Param("followeeId") Long followeeId);

    /**
     * 检查是否正在关注
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 是否关注
     */
    boolean isFollowing(@Param("followerId") Long followerId, 
                       @Param("followeeId") Long followeeId);

    /**
     * 获取关注的用户ID列表
     * @param followerId 关注者ID
     * @return 用户ID列表
     */
    List<Long> getFollowingIds(@Param("followerId") Long followerId);

    /**
     * 获取粉丝的用户ID列表
     * @param followeeId 被关注者ID
     * @return 用户ID列表
     */
    List<Long> getFollowerIds(@Param("followeeId") Long followeeId);

    /**
     * 取消关注
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 影响行数
     */
    int unfollowUser(@Param("followerId") Long followerId, 
                    @Param("followeeId") Long followeeId);
}