package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
// import com.web.model.Permission; // 权限系统已禁用
import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
// import com.web.service.PermissionService; // 权限系统已禁用
import com.web.service.UserService;
import com.web.service.RedisCacheService;
import com.web.util.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.IntStream;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 处理用户基本信息和统计数据的双表操作
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // @Autowired
    // private PermissionService permissionService; // 权限系统已禁用

    @Autowired
    private RedisCacheService redisCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserWithStats getUserProfile(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }

        // 先尝试从缓存获取
        UserWithStats cached = redisCacheService.getCachedUserWithStats(userId);
        if (cached != null) {
            log.debug("命中用户完整信息缓存: userId={}", userId);
            return cached;
        }

        // 缓存未命中，从数据库查询
        UserWithStats userWithStats = userMapper.selectUserWithStatsById(userId);
        if (userWithStats != null) {
            // 缓存结果
            redisCacheService.cacheUserWithStats(userWithStats);
            log.debug("缓存用户完整信息: userId={}", userId);
        }
        return userWithStats;
    }

    @Override
    public List<UserWithStats> getUserProfiles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new WeebException("用户ID列表不能为空");
        }
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new WeebException("用户ID必须为正数");
            }
        }
        return userMapper.selectUsersWithStatsByIds(userIds);
    }

    @Override
    public boolean updateUserBasicInfo(User user) {
        if (user == null) {
            throw new WeebException("用户信息不能为空");
        }
        if (user.getId() == null || user.getId() <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        int result = userMapper.updateUser(user);
        return result > 0;
    }

    @Override
    public boolean updateUserStats(UserStats userStats) {
        if (userStats == null) {
            throw new WeebException("用户统计信息不能为空");
        }
        if (userStats.getUserId() == null || userStats.getUserId() <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        int result = userStatsMapper.updateUserStats(userStats);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean updateUserProfile(UserWithStats userWithStats) {
        if (userWithStats == null) {
            throw new WeebException("用户信息不能为空");
        }
        if (userWithStats.getId() == null || userWithStats.getId() <= 0) {
            throw new WeebException("用户ID必须为正数");
        }

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
            log.error("更新用户信息失败: {}", e.getMessage(), e);
            throw new WeebException("更新用户信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserWithStats createUser(User user) {
        if (user == null) {
            throw new WeebException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new WeebException("用户名不能为空");
        }
        if (!ValidationUtils.isValidUsername(user.getUsername())) {
            throw new WeebException("用户名格式不正确");
        }

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
            log.error("创建用户失败: {}", e.getMessage(), e);
            throw new WeebException("创建用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }

        try {
            // 删除统计数据
            userStatsMapper.deleteByUserId(userId);
            // 删除用户基本信息
            int result = userMapper.deleteById(userId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            throw new WeebException("删除用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean followUser(Long followerId, Long followedId) {
        if (followerId == null || followerId <= 0) {
            throw new WeebException("关注者ID必须为正数");
        }
        if (followedId == null || followedId <= 0) {
            throw new WeebException("被关注者ID必须为正数");
        }
        if (followerId.equals(followedId)) {
            throw new WeebException("不能关注自己");
        }

        try {
            // 增加被关注用户的粉丝数
            int result = userStatsMapper.incrementFansCount(followedId);
            return result > 0;
        } catch (Exception e) {
            log.error("关注用户失败: {}", e.getMessage(), e);
            throw new WeebException("关注用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean unfollowUser(Long followerId, Long followedId) {
        if (followerId == null || followerId <= 0) {
            throw new WeebException("关注者ID必须为正数");
        }
        if (followedId == null || followedId <= 0) {
            throw new WeebException("被关注者ID必须为正数");
        }
        if (followerId.equals(followedId)) {
            throw new WeebException("不能取消关注自己");
        }

        try {
            // 减少被关注用户的粉丝数
            int result = userStatsMapper.decrementFansCount(followedId);
            return result > 0;
        } catch (Exception e) {
            log.error("取消关注用户失败: {}", e.getMessage(), e);
            throw new WeebException("取消关注用户失败: " + e.getMessage());
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

    @Override
    public Map<String, Object> getUsersWithPaging(int page, int pageSize, String keyword, String status) {
        if (page <= 0) {
            throw new WeebException("页码必须为正数");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new WeebException("页面大小必须在1-100之间");
        }

        try {
            Map<String, Object> result = new HashMap<>();

            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 处理状态过滤
            Integer statusFilter = null;
            if (status != null && !status.trim().isEmpty()) {
                switch (status.toLowerCase()) {
                    case "active":
                        statusFilter = 1; // 1表示启用状态
                        break;
                    case "banned":
                        statusFilter = 0; // 0表示禁用状态
                        break;
                    case "all":
                        // 不设置状态过滤，查询所有用户
                        break;
                    default:
                        throw new WeebException("无效的状态参数: " + status + "，支持的值: active, banned, all");
                }
            }

            // 查询用户列表 - 使用安全的搜索参数
            List<UserWithStats> users;
            String safeKeyword = null;
            if (keyword != null && !keyword.trim().isEmpty()) {
                safeKeyword = ValidationUtils.sanitizeSearchKeyword(keyword.trim());
                if (safeKeyword.length() > 50) {
                    throw new WeebException("搜索关键词长度不能超过50个字符");
                }
                users = userMapper.selectUsersWithStatsByKeywordAndStatus(safeKeyword, statusFilter, offset, pageSize);
            } else {
                users = userMapper.selectUsersWithStatsWithPagingAndStatus(statusFilter, offset, pageSize);
            }

            // 查询总数量
            int total;
            if (safeKeyword != null) {
                total = userMapper.countUsersByKeywordAndStatus(safeKeyword, statusFilter);
            } else {
                total = userMapper.countUsersByStatus(statusFilter);
            }

            result.put("list", users);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            log.error("分页查询用户失败: page={}, pageSize={}, keyword={}, status={}", page, pageSize, keyword, status, e);
            throw new WeebException("分页查询用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean banUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }

        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 设置用户状态为禁用
            user.setStatus(0); // 0表示禁用
            user.setUpdatedAt(new Date());
            int result = userMapper.updateUser(user);

            if (result > 0) {
                log.info("封禁用户成功: userId={}", userId);
                return true;
            } else {
                throw new WeebException("封禁用户失败");
            }
        } catch (Exception e) {
            log.error("封禁用户失败: userId={}", userId, e);
            throw new WeebException("封禁用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean unbanUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }

        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 设置用户状态为启用
            user.setStatus(1); // 1表示启用
            user.setUpdatedAt(new Date());
            int result = userMapper.updateUser(user);

            if (result > 0) {
                log.info("解封用户成功: userId={}", userId);
                return true;
            } else {
                throw new WeebException("解封用户失败");
            }
        } catch (Exception e) {
            log.error("解封用户失败: userId={}", userId, e);
            throw new WeebException("解封用户失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean resetUserPassword(Long userId, String newPassword) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new WeebException("新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            throw new WeebException("密码长度必须在6-50个字符之间");
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new WeebException("密码格式不正确");
        }

        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 加密新密码
            String encodedPassword = passwordEncoder.encode(newPassword.trim());

            // 更新密码
            user.setPassword(encodedPassword);
            user.setUpdatedAt(new Date());
            int result = userMapper.updateUser(user);

            if (result > 0) {
                log.info("重置用户密码成功: userId={}", userId);
                return true;
            } else {
                throw new WeebException("重置用户密码失败");
            }
        } catch (Exception e) {
            log.error("重置用户密码失败: userId={}", userId, e);
            throw new WeebException("重置用户密码失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getSystemStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 获取用户总数
            int totalUsers = userMapper.countUsers();
            stats.put("totalUsers", totalUsers);

            // 获取活跃用户数（最近30天有活动的用户）
            Date thirtyDaysAgo = Date.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
            int activeUsers = userMapper.countActiveUsers(thirtyDaysAgo);
            stats.put("activeUsers", activeUsers);

            // 获取新注册用户数（最近7天）
            Date sevenDaysAgo = Date.from(LocalDateTime.now().minusDays(7).atZone(ZoneId.systemDefault()).toInstant());
            int newUsers = userMapper.countNewUsers(sevenDaysAgo);
            stats.put("newUsers", newUsers);

            // 获取被封禁用户数
            int bannedUsers = userMapper.countBannedUsers();
            stats.put("bannedUsers", bannedUsers);

            // 获取用户统计数据汇总
            Map<String, Object> userStatsSummary = userStatsMapper.selectUserStatsSummary();
            stats.put("userStats", userStatsSummary);

            // 计算用户活跃度
            double activityRate = totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
            stats.put("activityRate", Math.round(activityRate * 100.0) / 100.0);

            log.debug("获取系统统计信息成功");
            return stats;
        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            throw new WeebException("获取系统统计信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        if (userId == null || userId <= 0) {
            throw new WeebException("用户ID必须为正数");
        }
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new WeebException("当前密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new WeebException("新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            throw new WeebException("密码长度必须在6-50个字符之间");
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new WeebException("密码格式不正确");
        }

        try {
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            // 验证当前密码
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new WeebException("当前密码不正确");
            }

            // 检查新密码是否与旧密码相同
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new WeebException("新密码不能与旧密码相同");
            }

            // 加密新密码
            String encodedPassword = passwordEncoder.encode(newPassword.trim());

            // 更新密码
            user.setPassword(encodedPassword);
            user.setUpdatedAt(new Date());
            int result = userMapper.updateUser(user);

            if (result > 0) {
                log.info("修改密码成功: userId={}", userId);
                return true;
            } else {
                throw new WeebException("修改密码失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败: userId={}", userId, e);
            throw new WeebException("修改密码失败: " + e.getMessage());
        }
    }

    @Override
    public User findByUsername(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return null;
            }
            return userMapper.selectByUsername(username.trim());
        } catch (Exception e) {
            log.error("根据用户名查找用户失败: username={}", username, e);
            return null;
        }
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        // 权限系统已禁用，返回空列表
        log.debug("权限系统已禁用，getUserPermissions 返回空列表: userId={}", userId);
        return List.of();
    }

    @Override
    public boolean isArticleOwner(Long userId, Long articleId) {
        try {
            if (userId == null || articleId == null) {
                return false;
            }
            Long articleOwnerId = userMapper.selectArticleOwnerId(articleId);
            return userId.equals(articleOwnerId);
        } catch (Exception e) {
            log.error("检查文章所有权失败: userId={}, articleId={}", userId, articleId, e);
            return false;
        }
    }

    @Override
    public boolean isMessageOwner(Long userId, Long messageId) {
        try {
            if (userId == null || messageId == null) {
                return false;
            }
            Long messageOwnerId = userMapper.selectMessageOwnerId(messageId);
            return userId.equals(messageOwnerId);
        } catch (Exception e) {
            log.error("检查消息所有权失败: userId={}, messageId={}", userId, messageId, e);
            return false;
        }
    }

    @Override
    public boolean isGroupOwner(Long userId, Long groupId) {
        try {
            if (userId == null || groupId == null) {
                return false;
            }
            Long groupOwnerId = userMapper.selectGroupOwnerId(groupId);
            return userId.equals(groupOwnerId);
        } catch (Exception e) {
            log.error("检查群组所有权失败: userId={}, groupId={}", userId, groupId, e);
            return false;
        }
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        // 权限系统已禁用，返回空列表
        log.debug("权限系统已禁用，getUserRoles 返回空列表: userId={}", userId);
        return List.of();
    }

    // ==================== 缓存相关辅助方法 ====================

    /**
     * 清除用户相关缓存
     * @param userId 用户ID
     */
    private void evictUserCache(Long userId) {
        try {
            redisCacheService.evictUserCache(userId);
            log.debug("清除用户缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 清除用户统计缓存
     * @param userId 用户ID
     */
    private void evictUserStatsCache(Long userId) {
        try {
            // 用户完整信息缓存也包含了统计数据，所以直接清除用户缓存即可
            evictUserCache(userId);
            log.debug("清除用户统计缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户统计缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 更新用户信息时清除缓存
     * @param user 用户对象
     */
    private void handleUserUpdate(User user) {
        if (user != null && user.getId() != null) {
            // 清除缓存，下次访问时会重新缓存
            evictUserCache(user.getId());
        }
    }

    /**
     * 批量获取用户时使用缓存优化
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    private List<User> getUsersWithCache(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        try {
            // 先尝试从缓存批量获取
            List<User> cachedUsers = redisCacheService.getCachedUsers(userIds);
            if (cachedUsers.size() == userIds.size()) {
                // 全部命中缓存
                log.debug("批量命中用户缓存: count={}", cachedUsers.size());
                return cachedUsers;
            }

            // 部分命中或全部未命中，从数据库查询缺失的用户
            Set<Long> cachedUserIds = cachedUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = userIds.stream()
                    .filter(id -> !cachedUserIds.contains(id))
                    .collect(Collectors.toList());

            if (!missingIds.isEmpty()) {
                // 查询缺失的用户
                List<User> dbUsers = userMapper.selectByIds(missingIds);
                // 缓存新查询的用户
                dbUsers.forEach(redisCacheService::cacheUser);
                // 合并结果
                Map<Long, User> userMap = cachedUsers.stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
                dbUsers.forEach(user -> userMap.put(user.getId(), user));
                return new ArrayList<>(userMap.values());
            }

            return cachedUsers;
        } catch (Exception e) {
            log.error("批量获取用户缓存失败", e);
            // 降级到数据库查询
            return userMapper.selectByIds(userIds);
        }
    }

    @Override
    public Map<String, Object> getUsersWithPaging(Integer page, Integer pageSize, String keyword, String status) {
        // 调用int版本的方法
        return getUsersWithPaging(page.intValue(), pageSize.intValue(), keyword, status);
    }

    @Override
    public Map<String, Object> getUsersWithPaging(int page, int pageSize, String keyword) {
        // 调用4参数版本的方法，默认状态为null
        return getUsersWithPaging(page, pageSize, keyword, null);
    }

    // ==================== 用户行为监控相关方法实现 ====================

    @Override
    public Map<String, Object> getUserBehaviorAnalysis(int days) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            // 计算时间范围
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 获取基础统计数据
            analysis.put("activeUsers", getActiveUsersCount(startTime, endTime));
            analysis.put("pageViews", getTotalPageViews(startTime, endTime));
            analysis.put("avgSessionDuration", getAverageSessionDuration(startTime, endTime));
            analysis.put("bounceRate", getBounceRate(startTime, endTime));

            // 计算趋势数据（与上一个周期对比）
            LocalDateTime prevStartTime = startTime.minusDays(days);
            LocalDateTime prevEndTime = startTime;

            analysis.put("activeUsersTrend", calculateTrend(
                getActiveUsersCount(prevStartTime, prevEndTime),
                getActiveUsersCount(startTime, endTime)
            ));

            analysis.put("pageViewsTrend", calculateTrend(
                getTotalPageViews(prevStartTime, prevEndTime),
                getTotalPageViews(startTime, endTime)
            ));

            analysis.put("avgSessionTrend", calculateTrend(
                getAverageSessionDurationMinutes(prevStartTime, prevEndTime),
                getAverageSessionDurationMinutes(startTime, endTime)
            ));

            analysis.put("bounceRateTrend", calculateTrend(
                getBounceRateValue(prevStartTime, prevEndTime),
                getBounceRateValue(startTime, endTime)
            ));

            // 获取热门页面数据
            analysis.put("topPages", getTopPagesData(startTime, endTime, 10));

            // 获取事件统计
            analysis.put("totalEvents", getTotalEvents(startTime, endTime));
            analysis.put("uniqueUsers", getUniqueEventUsers(startTime, endTime));
            analysis.put("conversionRate", getConversionRate(startTime, endTime));

            // 获取最近事件
            analysis.put("recentEvents", getRecentEvents(startTime, endTime, 20));

            log.info("用户行为分析完成，时间范围：{} 天", days);

        } catch (Exception e) {
            log.error("获取用户行为分析失败", e);
            analysis.put("error", "获取分析数据失败: " + e.getMessage());
        }

        return analysis;
    }

    @Override
    public Map<String, Object> getUserBehaviorEvents(int days, String eventType, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 计算分页偏移量
            int offset = (page - 1) * pageSize;

            // 获取事件列表（这里需要实现实际的事件查询逻辑）
            List<Map<String, Object>> events = getUserEvents(startTime, endTime, eventType, offset, pageSize);

            // 获取总数
            int totalCount = getUserEventsCount(startTime, endTime, eventType);

            // 计算分页信息
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            result.put("events", events);
            result.put("pagination", Map.of(
                "page", page,
                "pageSize", pageSize,
                "total", totalCount,
                "totalPages", totalPages,
                "hasNext", page < totalPages,
                "hasPrev", page > 1
            ));

            result.put("statistics", Map.of(
                "totalEvents", totalCount,
                "filteredType", eventType,
                "timeRange", days + " days"
            ));

        } catch (Exception e) {
            log.error("获取用户行为事件失败", e);
            result.put("error", "获取事件数据失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getUserSegmentAnalysis(int days) {
        Map<String, Object> segments = new HashMap<>();

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 计算各用户分群数据
            Map<String, Object> newUsers = calculateNewUsersSegment(startTime, endTime);
            Map<String, Object> activeUsers = calculateActiveUsersSegment(startTime, endTime);
            Map<String, Object> returningUsers = calculateReturningUsersSegment(startTime, endTime);
            Map<String, Object> atRiskUsers = calculateAtRiskUsersSegment(startTime, endTime);

            // 计算各分群的百分比
            int totalUsers = getTotalUsersCount(startTime, endTime);

            newUsers.put("percentage", calculatePercentage(
                (Integer) newUsers.get("count"), totalUsers
            ));

            activeUsers.put("percentage", calculatePercentage(
                (Integer) activeUsers.get("count"), totalUsers
            ));

            returningUsers.put("percentage", calculatePercentage(
                (Integer) returningUsers.get("count"), totalUsers
            ));

            atRiskUsers.put("percentage", calculatePercentage(
                (Integer) atRiskUsers.get("count"), totalUsers
            ));

            segments.put("newUsers", newUsers);
            segments.put("activeUsers", activeUsers);
            segments.put("returningUsers", returningUsers);
            segments.put("atRiskUsers", atRiskUsers);

            segments.put("totalUsers", totalUsers);
            segments.put("analysisPeriod", days + " days");

            log.info("用户分群分析完成，总用户数：{}", totalUsers);

        } catch (Exception e) {
            log.error("获取用户分群分析失败", e);
            segments.put("error", "获取分群数据失败: " + e.getMessage());
        }

        return segments;
    }

    @Override
    public Map<String, Object> getPopularPages(int days) {
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 获取热门页面数据
            List<Map<String, Object>> pages = getTopPagesData(startTime, endTime, 20);

            result.put("pages", pages);
            result.put("totalPages", pages.size());
            result.put("timeRange", days + " days");
            result.put("analysisDate", endTime.toString());

            // 计算页面访问统计
            int totalViews = pages.stream()
                    .mapToInt(page -> (Integer) page.get("views"))
                    .sum();

            result.put("totalViews", totalViews);
            result.put("avgViewsPerPage", pages.isEmpty() ? 0 : totalViews / pages.size());

        } catch (Exception e) {
            log.error("获取热门页面统计失败", e);
            result.put("error", "获取页面数据失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getBehaviorAnomalies(int hours) {
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);

            // 检测异常行为
            List<Map<String, Object>> anomalies = detectBehaviorAnomalies(startTime, endTime);

            result.put("anomalies", anomalies);
            result.put("totalAnomalies", anomalies.size());
            result.put("timeRange", hours + " hours");
            result.put("lastChecked", endTime.toString());

            // 按严重程度分组
            Map<String, Long> severityCount = anomalies.stream()
                    .collect(Collectors.groupingBy(
                            anomaly -> (String) anomaly.get("severity"),
                            Collectors.counting()
                    ));

            result.put("severityBreakdown", severityCount);

        } catch (Exception e) {
            log.error("获取异常行为检测失败", e);
            result.put("error", "检测异常行为失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> runAnomalyDetection() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始运行异常行为检测...");

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24); // 检测最近24小时

            // 运行各种异常检测算法
            List<Map<String, Object>> detectedAnomalies = new ArrayList<>();

            // 检测异常登录行为
            detectedAnomalies.addAll(detectAnomalousLogins(startTime, endTime));

            // 检测异常访问模式
            detectedAnomalies.addAll(detectAnomalousAccessPatterns(startTime, endTime));

            // 检测异常操作频率
            detectedAnomalies.addAll(detectAnomalousOperationFrequency(startTime, endTime));

            // 检测异常地理位置访问
            detectedAnomalies.addAll(detectAnomalousGeoAccess(startTime, endTime));

            result.put("detectedAnomalies", detectedAnomalies);
            result.put("totalDetected", detectedAnomalies.size());
            result.put("detectionTime", endTime.toString());
            result.put("timeRange", "24 hours");

            // 按类型分组统计
            Map<String, Long> typeCount = detectedAnomalies.stream()
                    .collect(Collectors.groupingBy(
                            anomaly -> (String) anomaly.get("type"),
                            Collectors.counting()
                    ));

            result.put("typeBreakdown", typeCount);

            log.info("异常行为检测完成，检测到 {} 个异常", detectedAnomalies.size());

        } catch (Exception e) {
            log.error("运行异常行为检测失败", e);
            result.put("error", "异常检测失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public byte[] exportBehaviorData(String format, int days) {
        try {
            log.info("开始导出用户行为数据，格式：{}，时间范围：{} 天", format, days);

            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            // 获取导出数据
            Map<String, Object> behaviorData = getUserBehaviorAnalysis(days);

            switch (format.toLowerCase()) {
                case "csv":
                    return exportToCsv(behaviorData, startTime, endTime);
                case "xlsx":
                    return exportToExcel(behaviorData, startTime, endTime);
                case "json":
                    return exportToJson(behaviorData, startTime, endTime);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }

        } catch (Exception e) {
            log.error("导出用户行为数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getActivityHeatmap(String type, int days) {
        Map<String, Object> heatmap = new HashMap<>();

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);

            switch (type.toLowerCase()) {
                case "hourly":
                    heatmap = generateHourlyHeatmap(startTime, endTime);
                    break;
                case "weekly":
                    heatmap = generateWeeklyHeatmap(startTime, endTime);
                    break;
                case "monthly":
                    heatmap = generateMonthlyHeatmap(startTime, endTime);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的热力图类型: " + type);
            }

            heatmap.put("type", type);
            heatmap.put("timeRange", days + " days");
            heatmap.put("generatedAt", endTime.toString());

        } catch (Exception e) {
            log.error("获取活动热力图失败", e);
            heatmap.put("error", "生成热力图失败: " + e.getMessage());
        }

        return heatmap;
    }

    @Override
    public Map<String, Object> getUserRetention(String cohortType, int periods) {
        Map<String, Object> retention = new HashMap<>();

        try {
            // 根据队列类型生成留存分析
            switch (cohortType.toLowerCase()) {
                case "daily":
                    retention = calculateDailyRetention(periods);
                    break;
                case "weekly":
                    retention = calculateWeeklyRetention(periods);
                    break;
                case "monthly":
                    retention = calculateMonthlyRetention(periods);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的队列类型: " + cohortType);
            }

            retention.put("cohortType", cohortType);
            retention.put("periods", periods);
            retention.put("generatedAt", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("获取用户留存分析失败", e);
            retention.put("error", "留存分析失败: " + e.getMessage());
        }

        return retention;
    }

    @Override
    public int getCurrentOnlineUserCount() {
        try {
            // 从Redis或会话存储中获取当前在线用户数
            // 这里使用模拟数据，实际项目中应该从实际的会话存储中获取
            return (int) (Math.random() * 500) + 100;
        } catch (Exception e) {
            log.error("获取当前在线用户数失败", e);
            return 0;
        }
    }

    @Override
    public int getPeakOnlineUsersToday() {
        try {
            // 获取今日在线用户峰值
            // 这里使用模拟数据，实际项目中应该从统计数据中获取
            return (int) (Math.random() * 800) + 200;
        } catch (Exception e) {
            log.error("获取今日在线用户峰值失败", e);
            return 0;
        }
    }

    // ==================== 私有辅助方法 ====================

    private int getActiveUsersCount(LocalDateTime start, LocalDateTime end) {
        // 模拟实现，实际应该从用户行为日志表查询
        return (int) (Math.random() * 1000) + 500;
    }

    private int getTotalPageViews(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        return (int) (Math.random() * 10000) + 5000;
    }

    private String getAverageSessionDuration(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        int minutes = (int) (Math.random() * 30) + 5;
        return minutes + "m";
    }

    private double getAverageSessionDurationMinutes(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        return (Math.random() * 30) + 5;
    }

    private String getBounceRate(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        int rate = (int) (Math.random() * 40) + 20;
        return rate + "%";
    }

    private double getBounceRateValue(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        return (Math.random() * 40) + 20;
    }

    private int calculateTrend(int oldValue, int newValue) {
        if (oldValue == 0) return newValue > 0 ? 100 : 0;
        return (int) Math.round(((double) (newValue - oldValue) / oldValue) * 100);
    }

    private int calculateTrend(double oldValue, double newValue) {
        if (oldValue == 0) return newValue > 0 ? 100 : 0;
        return (int) Math.round(((newValue - oldValue) / oldValue) * 100);
    }

    private List<Map<String, Object>> getTopPagesData(LocalDateTime start, LocalDateTime end, int limit) {
        // 模拟热门页面数据
        List<String> pages = Arrays.asList("/dashboard", "/profile", "/messages", "/articles", "/settings");
        return pages.stream().map(path -> {
            Map<String, Object> page = new HashMap<>();
            page.put("path", path);
            page.put("views", (int) (Math.random() * 1000) + 100);
            page.put("avgDuration", (int) (Math.random() * 10) + 1 + "m");
            page.put("trend", (int) (Math.random() * 40) - 20);
            return page;
        }).sorted((a, b) -> Integer.compare((Integer) b.get("views"), (Integer) a.get("views")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private int getTotalEvents(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        return (int) (Math.random() * 5000) + 2000;
    }

    private int getUniqueEventUsers(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        return (int) (Math.random() * 500) + 200;
    }

    private String getConversionRate(LocalDateTime start, LocalDateTime end) {
        // 模拟实现
        int rate = (int) (Math.random() * 20) + 5;
        return rate + "%";
    }

    private List<Map<String, Object>> getRecentEvents(LocalDateTime start, LocalDateTime end, int limit) {
        // 模拟最近事件数据
        List<String> types = Arrays.asList("PAGE_VIEW", "BUTTON_CLICK", "FORM_SUBMIT", "FILE_DOWNLOAD");
        List<String> descriptions = Arrays.asList(
                "访问了首页", "点击了登录按钮", "提交了注册表单", "下载了用户手册",
                "查看了个人资料", "发送了消息", "创建了文章", "更新了设置"
        );

        return IntStream.range(0, limit).mapToObj(i -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", i + 1);
            event.put("type", types.get((int) (Math.random() * types.size())));
            event.put("description", descriptions.get((int) (Math.random() * descriptions.size())));
            event.put("username", "user" + (int) (Math.random() * 100));
            event.put("timestamp", start.plusMinutes((long) (Math.random() * (end.toLocalTime().toSecondOfDay() / 60))).toString());
            return event;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUserEvents(LocalDateTime start, LocalDateTime end, String eventType, int offset, int limit) {
        // 模拟获取用户事件列表
        return getRecentEvents(start, end, limit).stream()
                .filter(event -> eventType == null || eventType.equals(event.get("type")))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private int getUserEventsCount(LocalDateTime start, LocalDateTime end, String eventType) {
        // 模拟获取事件总数
        List<Map<String, Object>> allEvents = getRecentEvents(start, end, 1000);
        if (eventType == null) {
            return allEvents.size();
        }
        return (int) allEvents.stream()
                .filter(event -> eventType.equals(event.get("type")))
                .count();
    }

    private Map<String, Object> calculateNewUsersSegment(LocalDateTime start, LocalDateTime end) {
        // 模拟新用户分群数据
        return Map.of(
                "count", (int) (Math.random() * 100) + 50,
                "avgSession", (int) (Math.random() * 10) + 2 + "m",
                "bounceRate", (int) (Math.random() * 60) + 30 + "%"
        );
    }

    private Map<String, Object> calculateActiveUsersSegment(LocalDateTime start, LocalDateTime end) {
        // 模拟活跃用户分群数据
        return Map.of(
                "count", (int) (Math.random() * 300) + 200,
                "avgSession", (int) (Math.random() * 45) + 15 + "m",
                "frequency", (int) (Math.random() * 5) + 1 + "次/天"
        );
    }

    private Map<String, Object> calculateReturningUsersSegment(LocalDateTime start, LocalDateTime end) {
        // 模拟回访用户分群数据
        return Map.of(
                "count", (int) (Math.random() * 200) + 100,
                "avgSession", (int) (Math.random() * 30) + 10 + "m",
                "interval", (int) (Math.random() * 7) + 1 + "天"
        );
    }

    private Map<String, Object> calculateAtRiskUsersSegment(LocalDateTime start, LocalDateTime end) {
        // 模拟流失风险用户分群数据
        return Map.of(
                "count", (int) (Math.random() * 50) + 10,
                "lastActive", (int) (Math.random() * 14) + 7 + "天前",
                "churnRisk", (int) (Math.random() * 40) + 30 + "%"
        );
    }

    private int getTotalUsersCount(LocalDateTime start, LocalDateTime end) {
        // 模拟总用户数
        return (int) (Math.random() * 500) + 300;
    }

    private String calculatePercentage(int value, int total) {
        if (total == 0) return "0%";
        return String.format("%.1f%%", (double) value / total * 100);
    }

    private List<Map<String, Object>> detectBehaviorAnomalies(LocalDateTime start, LocalDateTime end) {
        // 模拟异常行为检测
        List<Map<String, Object>> anomalies = new ArrayList<>();

        // 随机生成一些异常
        if (Math.random() > 0.7) {
            Map<String, Object> anomaly = new HashMap<>();
            anomaly.put("id", 1);
            anomaly.put("type", "suspicious_activity");
            anomaly.put("title", "异常登录行为");
            anomaly.put("description", "检测到来自多个地理位置的频繁登录尝试");
            anomaly.put("severity", "high");
            anomaly.put("affectedUsers", 3);
            anomaly.put("detectedAt", LocalDateTime.now().toString());
            anomalies.add(anomaly);
        }

        return anomalies;
    }

    private List<Map<String, Object>> detectAnomalousLogins(LocalDateTime start, LocalDateTime end) {
        // 检测异常登录行为
        return new ArrayList<>();
    }

    private List<Map<String, Object>> detectAnomalousAccessPatterns(LocalDateTime start, LocalDateTime end) {
        // 检测异常访问模式
        return new ArrayList<>();
    }

    private List<Map<String, Object>> detectAnomalousOperationFrequency(LocalDateTime start, LocalDateTime end) {
        // 检测异常操作频率
        return new ArrayList<>();
    }

    private List<Map<String, Object>> detectAnomalousGeoAccess(LocalDateTime start, LocalDateTime end) {
        // 检测异常地理位置访问
        return new ArrayList<>();
    }

    private byte[] exportToCsv(Map<String, Object> data, LocalDateTime start, LocalDateTime end) {
        // 模拟CSV导出
        String csv = "用户行为数据导出\n" +
                     "导出时间," + LocalDateTime.now() + "\n" +
                     "时间范围," + start + " 至 " + end + "\n\n";
        return csv.getBytes();
    }

    private byte[] exportToExcel(Map<String, Object> data, LocalDateTime start, LocalDateTime end) {
        // 模拟Excel导出
        String excel = "Excel格式数据 (模拟实现)\n" +
                      "导出时间: " + LocalDateTime.now() + "\n" +
                      "时间范围: " + start + " 至 " + end + "\n";
        return excel.getBytes();
    }

    private byte[] exportToJson(Map<String, Object> data, LocalDateTime start, LocalDateTime end) {
        // 模拟JSON导出
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportTime", LocalDateTime.now());
        exportData.put("timeRange", Map.of("start", start, "end", end));
        exportData.put("behaviorData", data);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(exportData);
        } catch (Exception e) {
            log.error("JSON导出失败", e);
            return "{}".getBytes();
        }
    }

    private Map<String, Object> generateHourlyHeatmap(LocalDateTime start, LocalDateTime end) {
        // 生成小时级热力图数据
        Map<String, Object> heatmap = new HashMap<>();
        int[][] data = new int[24][7]; // 24小时 x 7天

        // 填充模拟数据
        for (int hour = 0; hour < 24; hour++) {
            for (int day = 0; day < 7; day++) {
                data[hour][day] = (int) (Math.random() * 100);
            }
        }

        heatmap.put("data", data);
        heatmap.put("labels", Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        heatmap.put("hours", IntStream.range(0, 24).mapToObj(i -> i + ":00").collect(Collectors.toList()));

        return heatmap;
    }

    private Map<String, Object> generateWeeklyHeatmap(LocalDateTime start, LocalDateTime end) {
        // 生成周级热力图数据
        return new HashMap<>();
    }

    private Map<String, Object> generateMonthlyHeatmap(LocalDateTime start, LocalDateTime end) {
        // 生成月级热力图数据
        return new HashMap<>();
    }

    private Map<String, Object> calculateDailyRetention(int periods) {
        // 计算日留存数据
        return new HashMap<>();
    }

    private Map<String, Object> calculateWeeklyRetention(int periods) {
        // 计算周留存数据
        return new HashMap<>();
    }

    private Map<String, Object> calculateMonthlyRetention(int periods) {
        // 计算月留存数据
        return new HashMap<>();
    }

    // ==================== 新增的用户统计和资料管理方法 ====================

    @Override
    @Transactional
    public Map<String, Object> getUserStatistics(Long userId) {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 获取用户基本信息
            User user = getUserBasicInfo(userId);
            if (user == null) {
                return statistics;
            }

            // 获取用户统计数据
            UserStats userStats = getUserStatsOnly(userId);
            if (userStats == null) {
                // 创建默认统计数据
                userStats = new UserStats();
                userStats.setUserId(userId);
                userStats.setArticleCount(0L);
                userStats.setFollowerCount(0L);
                userStats.setFollowingCount(0L);
                userStats.setTotalLikes(0L);
                userStats.setTotalFavorites(0L);
                userStats.setTotalViews(0L);
                userStats.setLoginCount(0L);
                userStats.setLastLoginTime(LocalDateTime.now());
            }

            // 构建统计信息
            statistics.put("userId", userId);
            statistics.put("username", user.getUsername());
            statistics.put("nickname", user.getNickname());
            statistics.put("articleCount", userStats.getArticleCount());
            statistics.put("followerCount", userStats.getFollowerCount());
            statistics.put("followingCount", userStats.getFollowingCount());
            statistics.put("totalLikes", userStats.getTotalLikes());
            statistics.put("totalFavorites", userStats.getTotalFavorites());
            statistics.put("totalViews", userStats.getTotalViews());
            statistics.put("loginCount", userStats.getLoginCount());
            statistics.put("lastLoginTime", userStats.getLastLoginTime());
            statistics.put("joinTime", user.getCreatedAt());
            statistics.put("userLevel", user.getUserLevel());

            // 计算一些衍生统计
            statistics.put("engagementScore", calculateEngagementScore(userStats));
            statistics.put("activityLevel", calculateActivityLevel(userStats));

            return statistics;
        } catch (Exception e) {
            log.error("获取用户统计信息失败，用户ID: {}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> getUserRecentActivities(Long userId, int limit) {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();

            // 模拟最近活动数据（实际应该从数据库获取）
            activities.addAll(getArticleActivities(userId, limit / 3));
            activities.addAll(getSocialActivities(userId, limit / 3));
            activities.addAll(getSystemActivities(userId, limit / 3));

            // 按时间排序并限制数量
            activities.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("createdAt");
                LocalDateTime timeB = (LocalDateTime) b.get("createdAt");
                return timeB.compareTo(timeA);
            });

            return activities.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户最近活动失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public boolean updateUserProfile(User user) {
        try {
            if (user == null || user.getId() == null) {
                return false;
            }

            // 只允许更新特定字段
            User existingUser = getUserBasicInfo(user.getId());
            if (existingUser == null) {
                return false;
            }

            // 更新允许修改的字段
            if (user.getNickname() != null) {
                existingUser.setNickname(user.getNickname());
            }
            if (user.getBio() != null) {
                existingUser.setBio(user.getBio());
            }
            if (user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }
            if (user.getPhone() != null) {
                existingUser.setPhone(user.getPhone());
            }
            if (user.getAvatar() != null) {
                existingUser.setAvatar(user.getAvatar());
            }

            existingUser.setUpdatedAt(new Date());

            int result = userMapper.updateById(existingUser);
            return result > 0;
        } catch (Exception e) {
            log.error("更新用户资料失败，用户ID: {}", user != null ? user.getId() : null, e);
            return false;
        }
    }

    @Override
    @Transactional
    public String uploadUserAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
        try {
            if (userId == null || file == null || file.isEmpty()) {
                return null;
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("只能上传图片文件");
            }

            // 验证文件大小
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB
                throw new IllegalArgumentException("图片大小不能超过5MB");
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;

            // 这里应该实现实际的文件存储逻辑
            // 目前返回模拟的URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新用户头像
            User user = new User();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            updateUserProfile(user);

            log.info("用户头像上传成功，用户ID: {}, 头像URL: {}", userId, avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("上传用户头像失败，用户ID: {}", userId, e);
            return null;
        }
    }

    // ==================== 私有辅助方法 ====================

    private int calculateEngagementScore(UserStats userStats) {
        if (userStats == null) return 0;

        // 简单的参与度评分算法
        int score = 0;
        score += userStats.getArticleCount().intValue() * 10; // 文章数
        score += userStats.getTotalLikes().intValue() * 2;    // 获赞数
        score += userStats.getTotalFavorites().intValue() * 3; // 收藏数
        score += userStats.getFollowerCount().intValue() * 5; // 粉丝数
        score += userStats.getLoginCount().intValue() * 1;    // 登录次数

        return score;
    }

    private String calculateActivityLevel(UserStats userStats) {
        if (userStats == null) return "低";

        int engagementScore = calculateEngagementScore(userStats);
        if (engagementScore >= 1000) return "高";
        if (engagementScore >= 100) return "中";
        return "低";
    }

    private List<Map<String, Object>> getArticleActivities(Long userId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // 模拟文章相关活动
        for (int i = 0; i < limit; i++) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "article_" + (i + 1));
            activity.put("type", "article");
            activity.put("title", "发布了文章《示例文章" + (i + 1) + "》");
            activity.put("createdAt", LocalDateTime.now().minusDays(i));
            activities.add(activity);
        }

        return activities;
    }

    private List<Map<String, Object>> getSocialActivities(Long userId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // 模拟社交活动
        String[] socialActions = {"关注了用户", "被用户关注", "点赞了文章", "收藏了文章"};
        for (int i = 0; i < limit; i++) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "social_" + (i + 1));
            activity.put("type", "social");
            activity.put("title", socialActions[i % socialActions.length]);
            activity.put("createdAt", LocalDateTime.now().minusDays(i).minusHours(i % 3));
            activities.add(activity);
        }

        return activities;
    }

    private List<Map<String, Object>> getSystemActivities(Long userId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // 模拟系统活动
        activities.add(Map.of(
            "id", "system_login",
            "type", "login",
            "title", "登录系统",
            "createdAt", LocalDateTime.now().minusHours(1)
        ));

        return activities;
    }
}