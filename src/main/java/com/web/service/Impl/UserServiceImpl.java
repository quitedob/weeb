package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.Permission;
import com.web.model.User;
import com.web.model.UserStats;
import com.web.model.UserWithStats;
import com.web.service.PermissionService;
import com.web.service.UserService;
import com.web.service.RedisCacheService;
import com.web.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RedisCacheService redisCacheService;

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
    public List<Permission> getUserPermissions(Long userId) {
        try {
            if (userId == null) {
                return List.of();
            }
            return userMapper.selectUserPermissions(userId);
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return List.of();
        }
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
        try {
            if (userId == null) {
                return List.of();
            }

            // 从权限中提取角色信息
            List<Permission> permissions = getUserPermissions(userId);

            // 获取以ROLE_开头的权限作为角色
            return permissions.stream()
                    .map(Permission::getName)
                    .filter(permission -> permission.startsWith("ROLE_"))
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return List.of();
        }
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
}