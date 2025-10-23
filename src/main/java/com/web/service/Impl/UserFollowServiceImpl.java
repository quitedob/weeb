package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.UserFollowMapper;
import com.web.mapper.UserMapper;
import com.web.model.UserFollow;
import com.web.service.NotificationService;
import com.web.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户关注服务实现类
 */
@Service
@Transactional
public class UserFollowServiceImpl implements UserFollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new WeebException("不能关注自己");
        }
        
        // 检查被关注用户是否存在
        if (userMapper.selectById(followeeId) == null) {
            throw new WeebException("用户不存在");
        }
        
        // 检查是否已关注
        if (userFollowMapper.findFollowRelation(followerId, followeeId) != null) {
            throw new WeebException("已经关注了该用户");
        }
        
        // 创建关注关系
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(followerId);
        userFollow.setFolloweeId(followeeId);
        userFollow.setCreatedAt(new Date());
        
        userFollowMapper.insert(userFollow);
        
        // 创建关注通知
        try {
            notificationService.createNewFollowerNotification(followerId, followeeId);
        } catch (Exception e) {
            // 通知创建失败不影响关注操作
            System.err.println("创建关注通知失败: " + e.getMessage());
        }
    }

    @Override
    public void unfollowUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new WeebException("不能取消关注自己");
        }
        
        int deleted = userFollowMapper.deleteFollowRelation(followerId, followeeId);
        if (deleted == 0) {
            throw new WeebException("未关注该用户");
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return false;
        }
        return userFollowMapper.findFollowRelation(followerId, followeeId) != null;
    }

    @Override
    public Map<String, Object> getFollowingList(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        
        List<Map<String, Object>> followingList = userFollowMapper.getFollowingList(userId, offset, size);
        int total = userFollowMapper.getFollowingCount(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("following", followingList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }

    @Override
    public Map<String, Object> getFollowersList(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        
        List<Map<String, Object>> followersList = userFollowMapper.getFollowersList(userId, offset, size);
        int total = userFollowMapper.getFollowersCount(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("followers", followersList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }

    @Override
    public Map<String, Object> getFollowStats(Long userId) {
        int followingCount = userFollowMapper.getFollowingCount(userId);
        int followersCount = userFollowMapper.getFollowersCount(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("followingCount", followingCount);
        stats.put("followersCount", followersCount);
        
        return stats;
    }
}