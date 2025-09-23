package com.web.service;

import com.web.model.UserStats;
import com.web.model.UserWithStats;

/**
 * 用户统计数据服务接口
 * 处理用户统计数据的增删改查操作
 */
public interface UserStatsService {

    /**
     * 根据用户ID获取统计数据
     * @param userId 用户ID
     * @return 用户统计数据
     */
    UserStats getStatsByUserId(Long userId);

    /**
     * 创建用户统计数据记录
     * @param userId 用户ID
     * @return 创建的统计数据记录
     */
    UserStats createStatsForUser(Long userId);

    /**
     * 更新用户统计数据
     * @param userStats 要更新的统计数据
     * @return 是否更新成功
     */
    boolean updateStats(UserStats userStats);

    /**
     * 增加粉丝数
     * @param userId 用户ID
     * @return 是否操作成功
     */
    boolean incrementFansCount(Long userId);

    /**
     * 减少粉丝数
     * @param userId 用户ID
     * @return 是否操作成功
     */
    boolean decrementFansCount(Long userId);

    /**
     * 增加点赞数
     * @param userId 用户ID
     * @param count 增加的数量（默认为1）
     * @return 是否操作成功
     */
    boolean incrementTotalLikes(Long userId, Long count);

    /**
     * 增加收藏数
     * @param userId 用户ID
     * @param count 增加的数量（默认为1）
     * @return 是否操作成功
     */
    boolean incrementTotalFavorites(Long userId, Long count);

    /**
     * 增加文章曝光数
     * @param userId 用户ID
     * @param count 增加的数量（默认为1）
     * @return 是否操作成功
     */
    boolean incrementArticleExposure(Long userId, Long count);

    /**
     * 增加网站金币
     * @param userId 用户ID
     * @param coins 增加的金币数量
     * @return 是否操作成功
     */
    boolean addWebsiteCoins(Long userId, Long coins);

    /**
     * 扣除网站金币
     * @param userId 用户ID
     * @param coins 扣除的金币数量
     * @return 是否操作成功（余额不足时返回false）
     */
    boolean deductWebsiteCoins(Long userId, Long coins);

    /**
     * 增加赞助金额
     * @param userId 用户ID
     * @param amount 赞助金额
     * @return 是否操作成功
     */
    boolean addSponsorship(Long userId, Long amount);

    /**
     * 获取用户完整信息（包含基本信息和统计数据）
     * @param userId 用户ID
     * @return 用户完整信息
     */
    UserWithStats getUserWithStats(Long userId);

    /**
     * 批量获取用户统计数据
     * @param userIds 用户ID列表
     * @return 用户统计数据映射表
     */
    java.util.Map<Long, UserStats> getStatsByUserIds(java.util.List<Long> userIds);

    /**
     * 删除用户统计数据
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteStatsByUserId(Long userId);
}