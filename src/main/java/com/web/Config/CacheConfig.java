package com.web.Config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 缓存配置类
 * 配置多级缓存策略：本地缓存 + Redis缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 本地缓存管理器（Caffeine）
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置不同类型数据的缓存策略
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入后5分钟过期
                .expireAfterWrite(Duration.ofMinutes(5))
                // 最大缓存条目数
                .maximumSize(1000)
                // 启用统计
                .recordStats());

        return cacheManager;
    }

    /**
     * 用户数据缓存配置
     */
    @Bean
    public Caffeine<Object, Object> userCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(500)
                .recordStats();
    }

    /**
     * 群组数据缓存配置
     */
    @Bean
    public Caffeine<Object, Object> groupCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(200)
                .recordStats();
    }

    /**
     * 文章数据缓存配置
     */
    @Bean
    public Caffeine<Object, Object> articleCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(300)
                .recordStats();
    }

    /**
     * 搜索结果缓存配置
     */
    @Bean
    public Caffeine<Object, Object> searchCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(3))
                .maximumSize(100)
                .recordStats();
    }
}

