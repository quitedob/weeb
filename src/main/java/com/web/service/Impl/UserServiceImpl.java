package com.web.service.impl;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserService;
import com.web.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 用户服务实现类，处理用户基本信息和统计数据的双表操作
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private UserStatsService userStatsService;

    @Override
    @Cacheable(value = "userProfile", key = "#userId", unless = "#result == null")
    public UserWithStats getUserProfile(Long userId) {
        // 使用JOIN查询获取用户完整信息
        UserWithStats userWithStats = userStatsMapper.selectUserWithStats(userId);
        
        if (userWithStats != null && userWithStats.getUser() != null) {
            // 确保有统计数据
            if (userWithStats.getUserStats() == null) {
                UserStats stats = userStatsService.createStatsForUser(userId);
                userWithStats.setUserStats(stats);
            }
            // 安全起见，不返回密码
            userWithStats.getUser().setPassword(null);
        }
        
        return userWithStats;
    }

    @Override
    public List<UserWithStats> getUserProfiles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<UserWithStats> profiles = new ArrayList<>();
        for (Long userId : userIds) {
            UserWithStats profile = getUserProfile(userId);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        
        return profiles;
    }

    @Override
    @CacheEvict(value = {"userProfile", "user"}, key = "#user.id")
    @Transactional
    public boolean updateUserBasicInfo(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        
        // 只更新user表中的基本信息字段
        int result = authMapper.updateAuth(user);
        return result > 0;
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#userStats.userId")
    @Transactional
    public boolean updateUserStats(UserStats userStats) {
        if (userStats == null || userStats.getUserId() == null) {
            return false;
        }
        
        // 确保统计记录存在
        if (userStatsMapper.countByUserId(userStats.getUserId()) == 0) {
            userStatsService.createStatsForUser(userStats.getUserId());
        }
        
        // 更新统计数据
        int result = userStatsMapper.updateUserStats(userStats);
        return result > 0;
    }

    @Override
    @CacheEvict(value = {"userProfile", "user", "userStats"}, key = "#userWithStats.user.id")
    @Transactional
    public boolean updateUserProfile(UserWithStats userWithStats) {
        if (userWithStats == null || userWithStats.getUser() == null) {
            return false;
        }

        Long userId = userWithStats.getUser().getId();
        if (userId == null) {
            return false;
        }

        try {
            // 更新用户基本信息
            if (userWithStats.getUser() != null) {
                int userResult = authMapper.updateAuth(userWithStats.getUser());
                if (userResult <= 0) {
                    throw new RuntimeException("更新用户基本信息失败");
                }
            }

            // 更新用户统计数据
            if (userWithStats.getUserStats() != null) {
                // 确保统计记录存在
                if (userStatsMapper.countByUserId(userId) == 0) {
                    userStatsService.createStatsForUser(userId);
                }
                
                userWithStats.getUserStats().setUserId(userId);
                int statsResult = userStatsMapper.updateUserStats(userWithStats.getUserStats());
                if (statsResult <= 0) {
                    throw new RuntimeException("更新用户统计数据失败");
                }
            }

            return true;
        } catch (Exception e) {
            // 事务会自动回滚
            throw new RuntimeException("更新用户完整信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public UserWithStats createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        try {
            // 创建用户基本信息
            authMapper.insertUser(user);
            
            if (user.getId() == null) {
                throw new RuntimeException("创建用户失败，未获取到用户ID");
            }

            // 创建用户统计数据记录
            UserStats userStats = userStatsService.createStatsForUser(user.getId());

            // 返回完整的用户信息
            UserWithStats userWithStats = new UserWithStats(user, userStats);
            // 不返回密码
            userWithStats.getUser().setPassword(null);
            
            return userWithStats;
        } catch (Exception e) {
            throw new RuntimeException("创建用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "user", "userStats"}, key = "#userId")
    @Transactional
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            // 删除用户统计数据
            userStatsMapper.deleteByUserId(userId);
            
            // 删除用户基本信息
            int result = authMapper.deleteByUserId(userId);
            
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#followedId")
    @Transactional
    public boolean followUser(Long followerId, Long followedId) {
        if (followerId == null || followedId == null || followerId.equals(followedId)) {
            return false;
        }

        // 检查用户是否存在
        if (!userExists(followerId) || !userExists(followedId)) {
            return false;
        }

        try {
            // 这里应该有关注关系表的操作，暂时省略
            // followMapper.insertFollow(followerId, followedId);
            
            // 增加被关注用户的粉丝数
            return userStatsService.incrementFansCount(followedId);
        } catch (Exception e) {
            throw new RuntimeException("关注用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#followedId")
    @Transactional
    public boolean unfollowUser(Long followerId, Long followedId) {
        if (followerId == null || followedId == null || followerId.equals(followedId)) {
            return false;
        }

        try {
            // 这里应该有关注关系表的操作，暂时省略
            // followMapper.deleteFollow(followerId, followedId);
            
            // 减少被关注用户的粉丝数
            return userStatsService.decrementFansCount(followedId);
        } catch (Exception e) {
            throw new RuntimeException("取消关注用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#authorId")
    @Transactional
    public boolean likeArticle(Long userId, Long articleId, Long authorId) {
        if (userId == null || articleId == null || authorId == null) {
            return false;
        }

        try {
            // 这里应该有点赞记录表的操作，暂时省略
            // likeMapper.insertLike(userId, articleId);
            
            // 增加文章作者的总点赞数
            return userStatsService.incrementTotalLikes(authorId, 1L);
        } catch (Exception e) {
            throw new RuntimeException("点赞文章失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#authorId")
    @Transactional
    public boolean favoriteArticle(Long userId, Long articleId, Long authorId) {
        if (userId == null || articleId == null || authorId == null) {
            return false;
        }

        try {
            // 这里应该有收藏记录表的操作，暂时省略
            // favoriteMapper.insertFavorite(userId, articleId);
            
            // 增加文章作者的总收藏数
            return userStatsService.incrementTotalFavorites(authorId, 1L);
        } catch (Exception e) {
            throw new RuntimeException("收藏文章失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#authorId")
    @Transactional
    public boolean recordArticleExposure(Long authorId, Long exposureCount) {
        if (authorId == null) {
            return false;
        }

        if (exposureCount == null || exposureCount <= 0) {
            exposureCount = 1L;
        }

        try {
            // 增加文章作者的总曝光数
            return userStatsService.incrementArticleExposure(authorId, exposureCount);
        } catch (Exception e) {
            throw new RuntimeException("记录文章曝光失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, allEntries = true)
    @Transactional
    public boolean sponsorUser(Long sponsorId, Long recipientId, Long amount) {
        if (sponsorId == null || recipientId == null || amount == null || amount <= 0) {
            return false;
        }

        if (sponsorId.equals(recipientId)) {
            return false; // 不能赞助自己
        }

        try {
            // 扣除赞助者的金币
            boolean deductResult = userStatsService.deductWebsiteCoins(sponsorId, amount);
            if (!deductResult) {
                return false; // 余额不足
            }

            // 增加被赞助者的赞助金额和金币
            boolean addSponsorshipResult = userStatsService.addSponsorship(recipientId, amount);
            boolean addCoinsResult = userStatsService.addWebsiteCoins(recipientId, amount);

            if (!addSponsorshipResult || !addCoinsResult) {
                throw new RuntimeException("赞助操作失败");
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("赞助用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#userId")
    @Transactional
    public boolean consumeCoins(Long userId, Long coins) {
        if (userId == null || coins == null || coins <= 0) {
            return false;
        }

        try {
            return userStatsService.deductWebsiteCoins(userId, coins);
        } catch (Exception e) {
            throw new RuntimeException("消费金币失败: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#userId")
    @Transactional
    public boolean rewardCoins(Long userId, Long coins) {
        if (userId == null || coins == null || coins <= 0) {
            return false;
        }

        try {
            return userStatsService.addWebsiteCoins(userId, coins);
        } catch (Exception e) {
            throw new RuntimeException("奖励金币失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Cacheable(value = "userRanking", key = "#statType + '_' + #limit")
    public List<UserWithStats> getUserRanking(String statType, int limit) {
        if (statType == null || limit <= 0) {
            return new ArrayList<>();
        }

        // 这里需要在UserStatsMapper中添加排行榜查询方法
        // 暂时返回空列表，实际实现需要根据statType查询排序
        return new ArrayList<>();
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, allEntries = true)
    @Transactional
    public int batchUpdateUserStats(Map<Long, UserStats> updates) {
        if (updates == null || updates.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Map.Entry<Long, UserStats> entry : updates.entrySet()) {
            Long userId = entry.getKey();
            UserStats stats = entry.getValue();
            stats.setUserId(userId);

            try {
                if (updateUserStats(stats)) {
                    successCount++;
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他记录
                System.err.println("批量更新用户统计数据失败，用户ID: " + userId + ", 错误: " + e.getMessage());
            }
        }

        return successCount;
    }

    @Override
    @CacheEvict(value = {"userProfile", "userStats"}, key = "#userId")
    @Transactional
    public boolean resetUserStats(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            // 删除现有统计数据
            userStatsMapper.deleteByUserId(userId);
            
            // 创建新的默认统计数据
            UserStats newStats = userStatsService.createStatsForUser(userId);
            
            return newStats != null;
        } catch (Exception e) {
            throw new RuntimeException("重置用户统计数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userExists(Long userId) {
        if (userId == null) {
            return false;
        }
        
        User user = authMapper.findByUserID(userId);
        return user != null;
    }

    @Override
    @Cacheable(value = "user", key = "#userId", unless = "#result == null")
    public User getUserBasicInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        
        User user = authMapper.findByUserID(userId);
        if (user != null) {
            // 不返回密码
            user.setPassword(null);
        }
        
        return user;
    }

    @Override
    @Cacheable(value = "userStats", key = "#userId", unless = "#result == null")
    public UserStats getUserStatsOnly(Long userId) {
        if (userId == null) {
            return null;
        }
        
        return userStatsService.getStatsByUserId(userId);
    }
}