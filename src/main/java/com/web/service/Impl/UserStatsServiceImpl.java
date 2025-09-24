package com.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.mapper.UserStatsMapper;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户统计服务实现类
 * 专门处理用户统计数据的业务逻辑
 */
@Service
@Transactional
public class UserStatsServiceImpl extends ServiceImpl<UserStatsMapper, UserStats> implements UserStatsService {

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Override
    public UserStats getStatsByUserId(Long userId) {
        return userStatsMapper.selectByUserId(userId);
    }

    @Override
    public UserStats createStatsForUser(Long userId) {
        UserStats userStats = new UserStats();
        userStats.setUserId(userId);
        userStats.setFansCount(0L);
        userStats.setTotalLikes(0L);
        userStats.setTotalFavorites(0L);
        userStats.setTotalSponsorship(0L);
        userStats.setTotalArticleExposure(0L);
        userStats.setWebsiteCoins(0L);
        
        int result = userStatsMapper.insertUserStats(userStats);
        if (result > 0) {
            return userStats;
        }
        throw new RuntimeException("创建用户统计数据失败");
    }

    @Override
    public boolean updateStats(UserStats userStats) {
        try {
            int result = userStatsMapper.updateUserStats(userStats);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UserWithStats getUserWithStats(Long userId) {
        return userStatsMapper.selectUserWithStats(userId);
    }

    @Override
    public boolean incrementFansCount(Long userId) {
        try {
            int result = userStatsMapper.incrementFansCount(userId);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean decrementFansCount(Long userId) {
        try {
            int result = userStatsMapper.decrementFansCount(userId);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean incrementTotalLikes(Long userId, Long count) {
        try {
            int result = userStatsMapper.incrementTotalLikes(userId, count);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean incrementTotalFavorites(Long userId, Long count) {
        try {
            int result = userStatsMapper.incrementTotalFavorites(userId, count);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean incrementArticleExposure(Long userId, Long count) {
        try {
            int result = userStatsMapper.incrementArticleExposure(userId, count);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean addWebsiteCoins(Long userId, Long coins) {
        try {
            int result = userStatsMapper.addWebsiteCoins(userId, coins);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deductWebsiteCoins(Long userId, Long coins) {
        try {
            int result = userStatsMapper.deductWebsiteCoins(userId, coins);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean addSponsorship(Long userId, Long amount) {
        try {
            int result = userStatsMapper.addSponsorship(userId, amount);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.Map<Long, UserStats> getStatsByUserIds(java.util.List<Long> userIds) {
        List<UserStats> statsList = userStatsMapper.selectByUserIds(userIds);
        java.util.Map<Long, UserStats> statsMap = new java.util.HashMap<>();
        for (UserStats stats : statsList) {
            statsMap.put(stats.getUserId(), stats);
        }
        return statsMap;
    }

    @Override
    public boolean deleteStatsByUserId(Long userId) {
        try {
            int result = userStatsMapper.deleteByUserId(userId);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }
}