package com.web.service.impl;

import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 * 处理用户基本信息和统计数据的双表操作
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Override
    public UserWithStats getUserProfile(Long userId) {
        return userMapper.selectUserWithStatsById(userId);
    }

    @Override
    public List<UserWithStats> getUserProfiles(List<Long> userIds) {
        return userMapper.selectUsersWithStatsByIds(userIds);
    }

    @Override
    public boolean updateUserBasicInfo(User user) {
        int result = userMapper.updateUser(user);
        return result > 0;
    }

    @Override
    public boolean updateUserStats(UserStats userStats) {
        int result = userStatsMapper.updateUserStats(userStats);
        return result > 0;
    }

    @Override
    public boolean updateUserProfile(UserWithStats userWithStats) {
        try {
            // 更新用户基本信息
            User user = new User();
            user.setId(userWithStats.getId());
            user.setUsername(userWithStats.getUsername());
            user.setNickname(userWithStats.getNickname());
            user.setAvatar(userWithStats.getAvatar());
            // 注意：根据UserWithStats的实际字段进行调整
            // 如果UserWithStats没有这些字段，需要从关联的User对象获取
            if (userWithStats.getUser() != null) {
                User sourceUser = userWithStats.getUser();
                user.setSex(sourceUser.getSex());
                user.setPhoneNumber(sourceUser.getPhoneNumber());
                user.setUserEmail(sourceUser.getUserEmail());
                user.setBio(sourceUser.getBio());
            }
            
            userMapper.updateUser(user);
            
            // 更新统计数据
            if (userWithStats.getUserStats() != null) {
                userStatsMapper.updateUserStats(userWithStats.getUserStats());
            }
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("更新用户信息失败", e);
        }
    }

    @Override
    public UserWithStats createUser(User user) {
        try {
            // 插入用户基本信息
            userMapper.insert(user);
            
            // 创建对应的统计记录
            UserStats userStats = new UserStats();
            userStats.setUserId(user.getId());
            userStats.setFansCount(0L);
            userStats.setTotalLikes(0L);
            userStats.setTotalFavorites(0L);
            userStats.setTotalSponsorship(0L);
            userStats.setTotalArticleExposure(0L);
            userStats.setWebsiteCoins(0L);
            
            userStatsMapper.insertUserStats(userStats);
            
            // 返回完整的用户信息
            return getUserProfile(user.getId());
        } catch (Exception e) {
            throw new RuntimeException("创建用户失败", e);
        }
    }

    @Override
    public boolean deleteUser(Long userId) {
        try {
            // 删除统计数据
            userStatsMapper.deleteByUserId(userId);
            // 删除用户基本信息
            int result = userMapper.deleteById(userId);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除用户失败", e);
        }
    }

    @Override
    public boolean followUser(Long followerId, Long followedId) {
        try {
            // 增加被关注用户的粉丝数
            int result = userStatsMapper.incrementFansCount(followedId);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean unfollowUser(Long followerId, Long followedId) {
        try {
            // 减少被关注用户的粉丝数
            int result = userStatsMapper.decrementFansCount(followedId);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean likeArticle(Long userId, Long articleId, Long authorId) {
        try {
            // 增加文章作者的总点赞数
            int result = userStatsMapper.incrementTotalLikes(authorId, 1L);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean favoriteArticle(Long userId, Long articleId, Long authorId) {
        try {
            // 增加文章作者的总收藏数
            int result = userStatsMapper.incrementTotalFavorites(authorId, 1L);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean recordArticleExposure(Long authorId, Long exposureCount) {
        try {
            // 增加文章作者的总曝光数
            int result = userStatsMapper.incrementArticleExposure(authorId, exposureCount);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sponsorUser(Long sponsorId, Long recipientId, Long amount) {
        try {
            // 增加被赞助用户的总赞助金额和网站金币
            userStatsMapper.addSponsorship(recipientId, amount);
            userStatsMapper.addWebsiteCoins(recipientId, amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean consumeCoins(Long userId, Long coins) {
        try {
            // 扣除用户的网站金币（仅在余额足够时）
            int result = userStatsMapper.deductWebsiteCoins(userId, coins);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean rewardCoins(Long userId, Long coins) {
        try {
            // 增加用户的网站金币
            int result = userStatsMapper.addWebsiteCoins(userId, coins);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<UserWithStats> getUserRanking(String statType, int limit) {
        // 根据统计类型构建排序字段
        String orderBy = switch (statType.toLowerCase()) {
            case "fans" -> "fans_count DESC";
            case "likes" -> "total_likes DESC";
            case "favorites" -> "total_favorites DESC";
            case "sponsorship" -> "total_sponsorship DESC";
            case "exposure" -> "article_exposure DESC";
            case "coins" -> "website_coins DESC";
            default -> "fans_count DESC";
        };
        
        return userMapper.selectUserListWithStats(null, null, null, null, null, orderBy);
    }

    @Override
    public int batchUpdateUserStats(Map<Long, UserStats> updates) {
        int successCount = 0;
        for (Map.Entry<Long, UserStats> entry : updates.entrySet()) {
            try {
                UserStats stats = entry.getValue();
                stats.setUserId(entry.getKey());
                if (userStatsMapper.updateUserStats(stats) > 0) {
                    successCount++;
                }
            } catch (Exception e) {
                // 记录错误但继续处理其他更新
                continue;
            }
        }
        return successCount;
    }

    @Override
    public boolean resetUserStats(Long userId) {
        try {
            UserStats userStats = new UserStats();
            userStats.setUserId(userId);
            userStats.setFansCount(0L);
            userStats.setTotalLikes(0L);
            userStats.setTotalFavorites(0L);
            userStats.setTotalSponsorship(0L);
            userStats.setTotalArticleExposure(0L);
            userStats.setWebsiteCoins(0L);
            
            int result = userStatsMapper.updateUserStats(userStats);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean userExists(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null;
    }

    @Override
    public User getUserBasicInfo(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public UserStats getUserStatsOnly(Long userId) {
        return userStatsMapper.selectByUserId(userId);
    }
}