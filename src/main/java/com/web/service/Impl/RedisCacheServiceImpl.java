package com.web.service.Impl;

import com.web.model.User;
import com.web.model.Article;
import com.web.model.Group;
import com.web.model.UserWithStats;
import com.web.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务实现类
 * 统一管理应用中的缓存操作
 */
@Slf4j
@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 用户缓存 ====================

    @Override
    public void cacheUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String key = USER_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
        log.debug("缓存用户信息: userId={}", user.getId());
    }

    @Override
    public User getCachedUser(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof User) {
                log.debug("命中用户缓存: userId={}", userId);
                return (User) cached;
            }
        } catch (Exception e) {
            log.error("获取用户缓存失败: userId={}", userId, e);
        }
        return null;
    }

    @Override
    public void cacheUserWithStats(UserWithStats userWithStats) {
        if (userWithStats == null || userWithStats.getId() == null) {
            return;
        }
        String key = USER_WITH_STATS_PREFIX + userWithStats.getId();
        redisTemplate.opsForValue().set(key, userWithStats, 30, TimeUnit.MINUTES);
        log.debug("缓存用户完整信息: userId={}", userWithStats.getId());
    }

    @Override
    public UserWithStats getCachedUserWithStats(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_WITH_STATS_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof UserWithStats) {
                log.debug("命中用户完整信息缓存: userId={}", userId);
                return (UserWithStats) cached;
            }
        } catch (Exception e) {
            log.error("获取用户完整信息缓存失败: userId={}", userId, e);
        }
        return null;
    }

    @Override
    public void evictUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String userKey = USER_PREFIX + userId;
        String userStatsKey = USER_WITH_STATS_PREFIX + userId;
        redisTemplate.delete(userKey);
        redisTemplate.delete(userStatsKey);
        log.debug("清除用户缓存: userId={}", userId);
    }

    @Override
    public void evictAllUserStatsCache() {
        try {
            Set<String> keys = redisTemplate.keys(USER_WITH_STATS_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除所有用户统计信息缓存: count={}", keys.size());
            }
        } catch (Exception e) {
            log.error("清除所有用户统计信息缓存失败", e);
        }
    }

    @Override
    public List<User> getCachedUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        try {
            List<String> keys = userIds.stream()
                    .map(id -> USER_PREFIX + id)
                    .toList();
            List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(keys);
            return cachedUsers.stream()
                    .filter(user -> user instanceof User)
                    .map(user -> (User) user)
                    .toList();
        } catch (Exception e) {
            log.error("批量获取用户缓存失败", e);
            return List.of();
        }
    }

    // ==================== 文章缓存 ====================

    @Override
    public void cacheArticle(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        String key = ARTICLE_PREFIX + article.getId();
        redisTemplate.opsForValue().set(key, article, 60, TimeUnit.MINUTES);
        log.debug("缓存文章信息: articleId={}", article.getId());
    }

    @Override
    public Article getCachedArticle(Long articleId) {
        if (articleId == null) {
            return null;
        }
        String key = ARTICLE_PREFIX + articleId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Article) {
                log.debug("命中文章缓存: articleId={}", articleId);
                return (Article) cached;
            }
        } catch (Exception e) {
            log.error("获取文章缓存失败: articleId={}", articleId, e);
        }
        return null;
    }

    @Override
    public void evictArticleCache(Long articleId) {
        if (articleId == null) {
            return;
        }
        String key = ARTICLE_PREFIX + articleId;
        redisTemplate.delete(key);
        log.debug("清除文章缓存: articleId={}", articleId);
    }

    @Override
    public void cacheArticleList(String cacheKey, List<Article> articles, long ttl) {
        if (cacheKey == null || articles == null) {
            return;
        }
        String key = ARTICLE_LIST_PREFIX + cacheKey;
        redisTemplate.opsForValue().set(key, articles, ttl, TimeUnit.SECONDS);
        log.debug("缓存文章列表: key={}, size={}", key, articles.size());
    }

    @Override
    public List<Article> getCachedArticleList(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        String key = ARTICLE_LIST_PREFIX + cacheKey;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("命中文章列表缓存: key={}", key);
                return (List<Article>) cached;
            }
        } catch (Exception e) {
            log.error("获取文章列表缓存失败: key={}", cacheKey, e);
        }
        return null;
    }

    // ==================== 群组缓存 ====================

    @Override
    public void cacheGroup(Group group) {
        if (group == null || group.getId() == null) {
            return;
        }
        String key = GROUP_PREFIX + group.getId();
        redisTemplate.opsForValue().set(key, group, 30, TimeUnit.MINUTES);
        log.debug("缓存群组信息: groupId={}", group.getId());
    }

    @Override
    public Group getCachedGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        String key = GROUP_PREFIX + groupId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Group) {
                log.debug("命中群组缓存: groupId={}", groupId);
                return (Group) cached;
            }
        } catch (Exception e) {
            log.error("获取群组缓存失败: groupId={}", groupId, e);
        }
        return null;
    }

    @Override
    public void evictGroupCache(Long groupId) {
        if (groupId == null) {
            return;
        }
        String key = GROUP_PREFIX + groupId;
        redisTemplate.delete(key);
        log.debug("清除群组缓存: groupId={}", groupId);
    }

    @Override
    public void cacheUserGroups(Long userId, List<Group> groups) {
        if (userId == null || groups == null) {
            return;
        }
        String key = USER_GROUPS_PREFIX + userId;
        redisTemplate.opsForValue().set(key, groups, 20, TimeUnit.MINUTES);
        log.debug("缓存用户群组列表: userId={}, size={}", userId, groups.size());
    }

    @Override
    public List<Group> getCachedUserGroups(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_GROUPS_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                log.debug("命中用户群组列表缓存: userId={}", userId);
                return (List<Group>) cached;
            }
        } catch (Exception e) {
            log.error("获取用户群组列表缓存失败: userId={}", userId, e);
        }
        return null;
    }

    // ==================== 在线用户管理 ====================

    @Override
    public void addOnlineUser(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
        // 设置在线状态的过期时间
        String onlineKey = ONLINE_USERS_KEY + ":" + userId;
        redisTemplate.opsForValue().set(onlineKey, "1", 5, TimeUnit.MINUTES);
        log.debug("添加在线用户: userId={}", userId);
    }

    @Override
    public void removeOnlineUser(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
        String onlineKey = ONLINE_USERS_KEY + ":" + userId;
        redisTemplate.delete(onlineKey);
        log.debug("移除在线用户: userId={}", userId);
    }

    @Override
    public Set<Long> getOnlineUsers() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            return members.stream()
                    .map(member -> Long.valueOf(member.toString()))
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return Set.of();
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        String onlineKey = ONLINE_USERS_KEY + ":" + userId;
        return redisTemplate.hasKey(onlineKey);
    }

    // ==================== 通用缓存操作 ====================

    @Override
    public void set(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            log.debug("设置缓存: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
        }
    }

    @Override
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("命中缓存: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败: key={}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("删除缓存: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public void expire(String key, long ttl) {
        try {
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            log.debug("设置缓存过期时间: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}", key, e);
        }
    }

    @Override
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取缓存过期时间失败: key={}", key, e);
            return -1;
        }
    }
}