package com.web.service;

import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口，处理用户基本信息和统计数据的双表操作
 * 提供事务管理确保用户表和用户统计表的数据一致性
 */
public interface UserService {

    /**
     * 获取用户完整信息（包含统计数据）
     * 使用JOIN查询获取用户基本信息和统计数据
     * @param userId 用户ID
     * @return 用户完整信息
     */
    UserWithStats getUserProfile(Long userId);

    /**
     * 批量获取用户完整信息
     * @param userIds 用户ID列表
     * @return 用户完整信息列表
     */
    List<UserWithStats> getUserProfiles(List<Long> userIds);

    /**
     * 更新用户基本信息
     * 只更新user表中的基本信息字段
     * @param user 要更新的用户信息
     * @return 是否更新成功
     */
    boolean updateUserBasicInfo(User user);

    /**
     * 更新用户统计数据
     * 只更新user_stats表中的统计字段
     * @param userStats 要更新的统计数据
     * @return 是否更新成功
     */
    boolean updateUserStats(UserStats userStats);

    /**
     * 事务性更新用户完整信息
     * 同时更新user表和user_stats表，确保数据一致性
     * @param userWithStats 包含用户基本信息和统计数据的完整对象
     * @return 是否更新成功
     */
    boolean updateUserProfile(UserWithStats userWithStats);

    /**
     * 创建新用户（包含统计数据记录）
     * 在user表中创建用户记录，同时在user_stats表中创建对应的统计记录
     * @param user 用户基本信息
     * @return 创建的用户完整信息
     */
    UserWithStats createUser(User user);

    /**
     * 删除用户（级联删除统计数据）
     * 删除user表记录，同时删除user_stats表中的对应记录
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);

    /**
     * 用户关注操作
     * 更新被关注用户的粉丝数统计
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 是否操作成功
     */
    boolean followUser(Long followerId, Long followedId);

    /**
     * 用户取消关注操作
     * 更新被关注用户的粉丝数统计
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 是否操作成功
     */
    boolean unfollowUser(Long followerId, Long followedId);

    /**
     * 文章点赞操作
     * 更新文章作者的总点赞数统计
     * @param userId 点赞用户ID
     * @param articleId 文章ID
     * @param authorId 文章作者ID
     * @return 是否操作成功
     */
    boolean likeArticle(Long userId, Long articleId, Long authorId);

    /**
     * 文章收藏操作
     * 更新文章作者的总收藏数统计
     * @param userId 收藏用户ID
     * @param articleId 文章ID
     * @param authorId 文章作者ID
     * @return 是否操作成功
     */
    boolean favoriteArticle(Long userId, Long articleId, Long authorId);

    /**
     * 文章曝光统计
     * 更新文章作者的总曝光数统计
     * @param authorId 文章作者ID
     * @param exposureCount 曝光次数（默认为1）
     * @return 是否操作成功
     */
    boolean recordArticleExposure(Long authorId, Long exposureCount);

    /**
     * 赞助操作
     * 更新被赞助用户的总赞助金额和网站金币
     * @param sponsorId 赞助者ID
     * @param recipientId 被赞助者ID
     * @param amount 赞助金额
     * @return 是否操作成功
     */
    boolean sponsorUser(Long sponsorId, Long recipientId, Long amount);

    /**
     * 金币消费操作
     * 扣除用户的网站金币
     * @param userId 用户ID
     * @param coins 消费的金币数量
     * @return 是否操作成功（余额不足时返回false）
     */
    boolean consumeCoins(Long userId, Long coins);

    /**
     * 金币奖励操作
     * 增加用户的网站金币
     * @param userId 用户ID
     * @param coins 奖励的金币数量
     * @return 是否操作成功
     */
    boolean rewardCoins(Long userId, Long coins);

    /**
     * 获取用户统计排行榜
     * @param statType 统计类型（fans_count, total_likes, total_favorites等）
     * @param limit 返回数量限制
     * @return 排行榜列表
     */
    List<UserWithStats> getUserRanking(String statType, int limit);

    /**
     * 批量更新用户统计数据
     * 用于批量处理统计数据更新，提高性能
     * @param updates 统计数据更新映射表（用户ID -> 统计数据）
     * @return 成功更新的记录数
     */
    int batchUpdateUserStats(Map<Long, UserStats> updates);

    /**
     * 重置用户统计数据
     * 将指定用户的统计数据重置为默认值
     * @param userId 用户ID
     * @return 是否重置成功
     */
    boolean resetUserStats(Long userId);

    /**
     * 检查用户是否存在
     * @param userId 用户ID
     * @return 用户是否存在
     */
    boolean userExists(Long userId);

    /**
     * 获取用户基本信息（不包含统计数据）
     * @param userId 用户ID
     * @return 用户基本信息
     */
    User getUserBasicInfo(Long userId);

    /**
     * 获取用户统计数据（不包含基本信息）
     * @param userId 用户ID
     * @return 用户统计数据
     */
    UserStats getUserStatsOnly(Long userId);
}