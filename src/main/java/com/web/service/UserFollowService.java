package com.web.service;

import java.util.Map;

/**
 * 用户关注服务接口
 */
public interface UserFollowService {
    
    /**
     * 关注用户
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     */
    void followUser(Long followerId, Long followeeId);
    
    /**
     * 取消关注
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     */
    void unfollowUser(Long followerId, Long followeeId);
    
    /**
     * 检查是否已关注
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);
    
    /**
     * 获取关注列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 关注列表
     */
    Map<String, Object> getFollowingList(Long userId, int page, int size);
    
    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 粉丝列表
     */
    Map<String, Object> getFollowersList(Long userId, int page, int size);
    
    /**
     * 获取关注统计
     * @param userId 用户ID
     * @return 关注统计信息
     */
    Map<String, Object> getFollowStats(Long userId);
}