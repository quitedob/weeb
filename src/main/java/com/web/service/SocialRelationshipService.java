package com.web.service;

import java.util.List;
import java.util.Map;

/**
 * 社交关系服务接口
 * 统一管理联系人（好友）和关注系统
 * 实现关系状态机和自动关系建立
 */
public interface SocialRelationshipService {

    /**
     * 社交关系状态枚举
     */
    enum RelationshipStatus {
        STRANGER,           // 陌生人
        FOLLOWING,          // 我关注了对方
        FOLLOWER,           // 对方关注了我
        MUTUAL_FOLLOW,      // 互相关注
        FRIEND,             // 好友
        FRIEND_REQUEST_SENT,    // 已发送好友请求
        FRIEND_REQUEST_RECEIVED // 收到好友请求
    }

    /**
     * 获取两个用户之间的关系状态
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 关系状态
     */
    RelationshipStatus getRelationshipStatus(Long userId, Long targetUserId);

    /**
     * 关注用户
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 是否成功
     */
    boolean followUser(Long userId, Long targetUserId);

    /**
     * 取消关注用户
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 是否成功
     */
    boolean unfollowUser(Long userId, Long targetUserId);

    /**
     * 发送好友请求
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID
     * @param message 申请消息
     * @return 是否成功
     */
    boolean sendFriendRequest(Long userId, Long targetUserId, String message);

    /**
     * 接受好友请求
     * @param userId 当前用户ID
     * @param requestId 请求ID
     * @return 是否成功
     */
    boolean acceptFriendRequest(Long userId, Long requestId);

    /**
     * 拒绝好友请求
     * @param userId 当前用户ID
     * @param requestId 请求ID
     * @return 是否成功
     */
    boolean rejectFriendRequest(Long userId, Long requestId);

    /**
     * 删除好友
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @param keepFollow 是否保留关注关系
     * @return 是否成功
     */
    boolean removeFriend(Long userId, Long friendId, boolean keepFollow);

    /**
     * 检查是否互相关注
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否互相关注
     */
    boolean isMutualFollow(Long userId, Long targetUserId);

    /**
     * 检查是否为好友
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @return 是否为好友
     */
    boolean isFriend(Long userId, Long targetUserId);

    /**
     * 获取好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    List<Map<String, Object>> getFriendList(Long userId);

    /**
     * 获取关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    List<Map<String, Object>> getFollowingList(Long userId);

    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @return 粉丝列表
     */
    List<Map<String, Object>> getFollowerList(Long userId);

    /**
     * 获取互相关注列表
     * @param userId 用户ID
     * @return 互相关注列表
     */
    List<Map<String, Object>> getMutualFollowList(Long userId);

    /**
     * 获取好友推荐列表
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<Map<String, Object>> getFriendRecommendations(Long userId, int limit);

    /**
     * 获取关注推荐列表
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐列表
     */
    List<Map<String, Object>> getFollowRecommendations(Long userId, int limit);

    /**
     * 获取社交统计信息
     * @param userId 用户ID
     * @return 统计信息（好友数、关注数、粉丝数等）
     */
    Map<String, Object> getSocialStatistics(Long userId);

    /**
     * 处理互相关注自动建立好友关系
     * 当两个用户互相关注时，自动建立好友关系
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     */
    void handleMutualFollowAutoFriend(Long userId, Long targetUserId);
}
