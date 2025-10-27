package com.web.service.Impl;

import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.service.UserStatsSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户统计数据同步服务实现
 * 负责User和UserStats表之间的数据同步，确保一致性
 */
@Service
@Slf4j
public class UserStatsSyncServiceImpl implements UserStatsSyncService {

    private final UserMapper userMapper;
    private final UserStatsMapper userStatsMapper;

    @Autowired
    public UserStatsSyncServiceImpl(UserMapper userMapper, UserStatsMapper userStatsMapper) {
        this.userMapper = userMapper;
        this.userStatsMapper = userStatsMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUserLevelToStats(Long userId) {
        log.debug("开始同步用户等级: userId={}", userId);

        try {
            // 获取User表中的用户等级
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，无法同步等级: userId={}", userId);
                return;
            }

            // 获取UserStats表中的记录
            UserStats userStats = userStatsMapper.selectByUserId(userId);
            if (userStats == null) {
                log.warn("用户统计数据不存在，创建新记录: userId={}", userId);

                // 创建新的UserStats记录
                userStats = new UserStats(userId);
                userStats.setUserLevel(user.getUserLevel());

                int rowsAffected = userStatsMapper.insertUserStats(userStats);
                if (rowsAffected > 0) {
                    log.info("用户统计数据创建并同步等级成功: userId={}, level={}", userId, user.getUserLevel());
                } else {
                    log.error("用户统计数据创建失败: userId={}", userId);
                }
            } else {
                // 更新现有UserStats记录的userLevel
                Integer oldLevel = userStats.getUserLevel();
                userStats.setUserLevel(user.getUserLevel());

                int rowsAffected = userStatsMapper.updateUserStats(userStats);
                if (rowsAffected > 0) {
                    log.info("用户等级同步成功: userId={}, oldLevel={}, newLevel={}",
                             userId, oldLevel, user.getUserLevel());
                } else {
                    log.error("用户等级同步失败: userId={}", userId);
                }
            }

        } catch (Exception e) {
            log.error("同步用户等级失败: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAllUserLevels() {
        log.info("开始批量同步所有用户等级");

        try {
            // 获取所有用户的基本信息（只需ID和userLevel）
            List<User> users = userMapper.selectAllUserLevels();

            if (users == null || users.isEmpty()) {
                log.info("没有用户数据需要同步");
                return;
            }

            int syncedCount = 0;
            int errorCount = 0;

            for (User user : users) {
                try {
                    syncUserLevelToStats(user.getId());
                    syncedCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("同步用户等级失败: userId={}, error={}", user.getId(), e.getMessage());
                }
            }

            log.info("批量用户等级同步完成: 总数={}, 成功={}, 失败={}",
                     users.size(), syncedCount, errorCount);

        } catch (Exception e) {
            log.error("批量同步用户等级失败: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserLevel(Long userId, Integer newLevel) {
        log.info("开始更新用户等级: userId={}, newLevel={}", userId, newLevel);

        try {
            // 参数验证
            if (userId == null || newLevel == null || newLevel < 1) {
                throw new IllegalArgumentException("用户ID或等级参数无效");
            }

            // 更新User表中的userLevel
            User user = new User();
            user.setId(userId);
            user.setUserLevel(newLevel);

            int userUpdateResult = userMapper.updateUserLevel(user);
            if (userUpdateResult <= 0) {
                throw new RuntimeException("更新用户User表等级失败");
            }

            // 更新UserStats表中的userLevel
            UserStats userStats = new UserStats();
            userStats.setUserId(userId);
            userStats.setUserLevel(newLevel);

            int statsUpdateResult = userStatsMapper.updateUserLevel(userStats);
            if (statsUpdateResult <= 0) {
                throw new RuntimeException("更新用户UserStats表等级失败");
            }

            log.info("用户等级更新成功: userId={}, newLevel={}", userId, newLevel);

        } catch (Exception e) {
            log.error("更新用户等级失败: userId={}, newLevel={}, error={}", userId, newLevel, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isUserLevelConsistent(Long userId) {
        try {
            // 获取User表中的等级
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return false;
            }

            // 获取UserStats表中的等级
            UserStats userStats = userStatsMapper.selectByUserId(userId);
            if (userStats == null) {
                log.warn("用户统计数据不存在: userId={}", userId);
                return false;
            }

            // 比较两个等级是否一致
            Integer userLevel = user.getUserLevel();
            Integer statsLevel = userStats.getUserLevel();

            boolean isConsistent = userLevel.equals(statsLevel);

            if (!isConsistent) {
                log.warn("用户等级数据不一致: userId={}, userLevel={}, statsLevel={}",
                         userId, userLevel, statsLevel);
            }

            return isConsistent;

        } catch (Exception e) {
            log.error("检查用户等级一致性失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
}