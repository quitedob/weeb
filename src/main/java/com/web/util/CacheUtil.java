package com.web.util;

// 引入必要的类和注解
import com.github.benmanes.caffeine.cache.Cache; // Caffeine 缓存接口，用于定义缓存操作
import com.github.benmanes.caffeine.cache.Caffeine; // 用于构建缓存实例的工具类
import org.springframework.stereotype.Component; // Spring 注解，标记该类为 Spring 容器的组件

import java.util.concurrent.TimeUnit; // 用于指定时间单位

/**
 * 缓存工具类，使用 Caffeine 实现缓存操作
 */
@Component
public class CacheUtil {

    // 定义一个缓存，用于记录用户最后一次查询记录的目标用户 ID
    private final Cache<Long, Long> userReadMsgCache;
    // 定义一个缓存，用于存储用户的登录信息（键是 userId，值是 token）
    private final Cache<Long, String> userSessionCache;

    /**
     * 构造方法，用于初始化两个缓存
     */
    public CacheUtil() {
        // 初始化 userReadMsgCache 缓存，设置缓存的过期时间为 12 小时
        this.userReadMsgCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS) // 数据在写入后 12 小时过期
                .build(); // 构建缓存实例

        // 初始化 userSessionCache 缓存，设置缓存的过期时间为 12 小时
        this.userSessionCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS) // 数据在写入后 12 小时过期
                .build(); // 构建缓存实例
    }

    /**
     * 方法：将用户的登录信息（userId 和 token）存入 userSessionCache 缓存
     *
     * @param userId 用户的唯一标识 ID
     * @param token  用户的身份令牌
     */
    public void putUserSessionCache(Long userId, String token) {
        userSessionCache.put(userId, token); // 将键值对存入缓存
    }

    /**
     * 方法：从 userSessionCache 缓存中获取指定 userId 的 token
     *
     * @param userId 用户的唯一标识 ID
     * @return 对应的 token，如果缓存中不存在则返回 null
     */
    public String getUserSessionCache(Long userId) {
        // 使用 getIfPresent 方法获取缓存值，返回 null 表示缓存中没有该值
        return userSessionCache.getIfPresent(userId);
    }

    /**
     * 方法：将用户的查询记录（userId 和 targetId）存入 userReadMsgCache 缓存
     *
     * @param userId   用户的唯一标识 ID
     * @param targetId 目标用户的唯一标识 ID
     */
    public void putUserReadCache(Long userId, Long targetId) {
        userReadMsgCache.put(userId, targetId); // 将键值对存入缓存
    }

    /**
     * 方法：从 userReadMsgCache 缓存中获取指定 userId 对应的 targetId
     *
     * @param userId 用户的唯一标识 ID
     * @return 对应的 targetId，如果缓存中不存在则返回 null
     */
    public Long getUserReadCache(Long userId) {
        // 使用 getIfPresent 方法获取缓存值，返回 null 表示缓存中没有该值
        return userReadMsgCache.getIfPresent(userId);
    }
}
