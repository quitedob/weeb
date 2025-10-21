package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 用户统计数据Mapper接口
 */
@Mapper
public interface UserStatsMapper extends BaseMapper<UserStats> {

    /**
     * 根据用户ID获取统计数据
     * @param userId 用户ID
     * @return 用户统计数据
     */
    UserStats selectByUserId(@Param("userId") Long userId);

    /**
     * 插入用户统计数据
     * @param userStats 用户统计数据
     * @return 影响行数
     */
    int insertUserStats(UserStats userStats);

    /**
     * 更新用户统计数据
     * @param userStats 用户统计数据
     * @return 影响行数
     */
    int updateUserStats(UserStats userStats);

    /**
     * 增加粉丝数
     * @param userId 用户ID
     * @return 影响行数
     */
    int incrementFansCount(@Param("userId") Long userId);

    /**
     * 减少粉丝数
     * @param userId 用户ID
     * @return 影响行数
     */
    int decrementFansCount(@Param("userId") Long userId);

    /**
     * 增加点赞数
     * @param userId 用户ID
     * @param count 增加的数量
     * @return 影响行数
     */
    int incrementTotalLikes(@Param("userId") Long userId, @Param("count") Long count);

    /**
     * 增加点赞数（兼容测试用，单个用户ID重载）
     * @param userId 用户ID
     * @return 影响行数
     */
    int incrementTotalLikes(@Param("userId") Long userId);

    /**
     * 增加收藏数
     * @param userId 用户ID
     * @param count 增加的数量
     * @return 影响行数
     */
    int incrementTotalFavorites(@Param("userId") Long userId, @Param("count") Long count);

    /**
     * 增加收藏数（兼容测试用，单个用户ID重载）
     * @param userId 用户ID
     * @return 影响行数
     */
    int incrementTotalFavorites(@Param("userId") Long userId);

    /**
     * 增加文章曝光数
     * @param userId 用户ID
     * @param count 增加的数量
     * @return 影响行数
     */
    int incrementArticleExposure(@Param("userId") Long userId, @Param("count") Long count);

    /**
     * 增加网站金币
     * @param userId 用户ID
     * @param coins 增加的金币数量
     * @return 影响行数
     */
    int addWebsiteCoins(@Param("userId") Long userId, @Param("coins") Long coins);

    /**
     * 扣除网站金币（仅在余额足够时）
     * @param userId 用户ID
     * @param coins 扣除的金币数量
     * @return 影响行数
     */
    int deductWebsiteCoins(@Param("userId") Long userId, @Param("coins") Long coins);

    /**
     * 增加赞助金额
     * @param userId 用户ID
     * @param amount 赞助金额
     * @return 影响行数
     */
    int addSponsorship(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 获取用户完整信息（JOIN查询）
     * @param userId 用户ID
     * @return 用户完整信息
     */
    UserWithStats selectUserWithStats(@Param("userId") Long userId);

    /**
     * 批量获取用户统计数据
     * @param userIds 用户ID列表
     * @return 用户统计数据列表
     */
    List<UserStats> selectByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 删除用户统计数据
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 检查用户统计数据是否存在
     * @param userId 用户ID
     * @return 记录数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找用户统计数据（兼容测试用）
     * @param userId 用户ID
     * @return 用户统计数据
     */
    UserStats selectUserStatsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户统计摘要信息
     * @return 用户统计摘要
     */
    Map<String, Object> selectUserStatsSummary();
}