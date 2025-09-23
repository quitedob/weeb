package com.web.service.Impl;

import com.web.mapper.UserStatsMapper;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户统计数据服务实现类
 */
@Service
public class UserStatsServiceImpl implements UserStatsService {

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Override
    @Cacheable(value = "userStats", key = "#userId", unless = "#result == null")
    public UserStats getStatsByUserId(Long userId) {
        UserStats stats = userStatsMapper.selectByUserId(userId);
        if (stats == null) {
            // 如果统计数据不存在，创建默认记录
            stats = createStatsForUser(userId);
        }
        return stats;
    }

    @Override
    @Transactional
    public UserStats createStatsForUser(Long userId) {
        // 检查是否已存在
        if (userStatsMapper.countByUserId(userId) > 0) {
            return userStatsMapper.selectByUserId(userId);
        }

        UserStats userStats = new UserStats(userId);
        int result = userStatsMapper.insertUserStats(userStats);
        if (result > 0) {
            return userStats;
        }
        throw new RuntimeException("创建用户统计数据失败");
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userStats.userId")
    @Transactional
    public boolean updateStats(UserStats userStats) {
        int result = userStatsMapper.updateUserStats(userStats);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean incrementFansCount(Long userId) {
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.incrementFansCount(userId);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean decrementFansCount(Long userId) {
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.decrementFansCount(userId);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean incrementTotalLikes(Long userId, Long count) {
        if (count == null || count <= 0) {
            count = 1L;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.incrementTotalLikes(userId, count);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean incrementTotalFavorites(Long userId, Long count) {
        if (count == null || count <= 0) {
            count = 1L;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.incrementTotalFavorites(userId, count);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean incrementArticleExposure(Long userId, Long count) {
        if (count == null || count <= 0) {
            count = 1L;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.incrementArticleExposure(userId, count);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean addWebsiteCoins(Long userId, Long coins) {
        if (coins == null || coins <= 0) {
            return false;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.addWebsiteCoins(userId, coins);
        return result > 0;
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean deductWebsiteCoins(Long userId, Long coins) {
        if (coins == null || coins <= 0) {
            return false;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.deductWebsiteCoins(userId, coins);
        return result > 0; // 如果余额不足，影响行数为0，返回false
    }

    @Override
    @CacheEvict(value = "userStats", key = "#userId")
    @Transactional
    public boolean addSponsorship(Long userId, Long amount) {
        if (amount == null || amount <= 0) {
            return false;
        }
        // 确保统计记录存在
        ensureStatsExist(userId);
        int result = userStatsMapper.addSponsorship(userId, amount);
        return result > 0;
    }

    @Override
    @Cacheable(value = "userWithStats", key = "#userId", unless = "#result == null")
    public UserWithStats getUserWithStats(Long userId) {
        UserWithStats userWithStats = userStatsMapper.selectUserWithStats(userId);
        if (userWithStats != null && userWithStats.getUser() != null) {
            // 确保有统计数据
            if (userWithStats.getUserStats() == null) {
                UserStats stats = createStatsForUser(userId);
                userWithStats.setUserStats(stats);
            }
            // 安全起见，不返回密码
            userWithStats.getUser().setPassword(null);
        }
        return userWithStats;
    }

    @Override
    public Map<Long, UserStats> getStatsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        List<UserStats> statsList = userStatsMapper.selectByUserIds(userIds);
        Map<Long, UserStats> statsMap = new HashMap<>();
        
        for (UserStats stats : statsList) {
            statsMap.put(stats.getUserId(), stats);
        }

        // 为没有统计数据的用户创建默认记录
        for (Long userId : userIds) {
            if (!statsMap.containsKey(userId)) {
                UserStats defaultStats = createStatsForUser(userId);
                statsMap.put(userId, defaultStats);
            }
        }

        return statsMap;
    }

    @Override
    @CacheEvict(value = {"userStats", "userWithStats"}, key = "#userId")
    @Transactional
    public boolean deleteStatsByUserId(Long userId) {
        int result = userStatsMapper.deleteByUserId(userId);
        return result > 0;
    }

    /**
     * 确保用户统计数据存在
     * @param userId 用户ID
     */
    private void ensureStatsExist(Long userId) {
        if (userStatsMapper.countByUserId(userId) == 0) {
            createStatsForUser(userId);
        }
    }
}