package com.web.service;

import com.web.model.User;
import com.web.model.Article;
import com.web.model.Group;
import com.web.model.UserWithStats;

import java.util.List;
import java.util.Set;

/**
 * Redis缓存服务接口
 * 统一管理应用中的缓存操作
 */
public interface RedisCacheService {

    // ==================== 用户缓存 ====================

    /**
     * 缓存用户信息
     * @param user 用户对象
     */
    void cacheUser(User user);

    /**
     * 获取缓存的用户信息
     * @param userId 用户ID
     * @return 用户对象，如果不存在返回null
     */
    User getCachedUser(Long userId);

    /**
     * 缓存用户完整信息（包含统计数据）
     * @param userWithStats 用户完整信息
     */
    void cacheUserWithStats(UserWithStats userWithStats);

    /**
     * 获取缓存的用户完整信息
     * @param userId 用户ID
     * @return 用户完整信息，如果不存在返回null
     */
    UserWithStats getCachedUserWithStats(Long userId);

    /**
     * 删除用户缓存
     * @param userId 用户ID
     */
    void evictUserCache(Long userId);

    /**
     * 批量获取缓存的用户信息
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<User> getCachedUsers(List<Long> userIds);

    // ==================== 文章缓存 ====================

    /**
     * 缓存文章信息
     * @param article 文章对象
     */
    void cacheArticle(Article article);

    /**
     * 获取缓存的文章信息
     * @param articleId 文章ID
     * @return 文章对象，如果不存在返回null
     */
    Article getCachedArticle(Long articleId);

    /**
     * 删除文章缓存
     * @param articleId 文章ID
     */
    void evictArticleCache(Long articleId);

    /**
     * 缓存文章列表
     * @param cacheKey 缓存键
     * @param articles 文章列表
     * @param ttl 过期时间（秒）
     */
    void cacheArticleList(String cacheKey, List<Article> articles, long ttl);

    /**
     * 获取缓存的文章列表
     * @param cacheKey 缓存键
     * @return 文章列表，如果不存在返回null
     */
    List<Article> getCachedArticleList(String cacheKey);

    // ==================== 群组缓存 ====================

    /**
     * 缓存群组信息
     * @param group 群组对象
     */
    void cacheGroup(Group group);

    /**
     * 获取缓存的群组信息
     * @param groupId 群组ID
     * @return 群组对象，如果不存在返回null
     */
    Group getCachedGroup(Long groupId);

    /**
     * 删除群组缓存
     * @param groupId 群组ID
     */
    void evictGroupCache(Long groupId);

    /**
     * 缓存用户群组列表
     * @param userId 用户ID
     * @param groups 群组列表
     */
    void cacheUserGroups(Long userId, List<Group> groups);

    /**
     * 获取缓存的用户群组列表
     * @param userId 用户ID
     * @return 群组列表，如果不存在返回null
     */
    List<Group> getCachedUserGroups(Long userId);

    // ==================== 在线用户管理 ====================

    /**
     * 添加用户到在线集合
     * @param userId 用户ID
     */
    void addOnlineUser(Long userId);

    /**
     * 从在线集合中移除用户
     * @param userId 用户ID
     */
    void removeOnlineUser(Long userId);

    /**
     * 获取所有在线用户ID
     * @return 在线用户ID集合
     */
    Set<Long> getOnlineUsers();

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(Long userId);

    // ==================== 通用缓存操作 ====================

    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间（秒）
     */
    void set(String key, Object value, long ttl);

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值
     */
    Object get(String key);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间
     * @param key 缓存键
     * @param ttl 过期时间（秒）
     */
    void expire(String key, long ttl);

    /**
     * 获取剩余过期时间
     * @param key 缓存键
     * @return 剩余时间（秒），如果不存在返回-1
     */
    long getExpire(String key);

    // ==================== 缓存键常量 ====================

    // 用户缓存相关
    String USER_PREFIX = "user:";
    String USER_WITH_STATS_PREFIX = "user:stats:";
    String USER_LIST_PREFIX = "user:list:";

    // 文章缓存相关
    String ARTICLE_PREFIX = "article:";
    String ARTICLE_LIST_PREFIX = "article:list:";

    // 群组缓存相关
    String GROUP_PREFIX = "group:";
    String USER_GROUPS_PREFIX = "user:groups:";

    // 在线用户相关
    String ONLINE_USERS_KEY = "online:users";
}