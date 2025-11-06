package com.web.Config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 缓存配置类
 * 实现多级缓存策略：L1（Caffeine本地缓存）+ L2（Redis分布式缓存）
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private static final Random RANDOM = new Random();

    /**
     * L1缓存：本地缓存管理器（Caffeine）
     * 用于高频访问的热点数据
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置不同类型数据的缓存策略
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入后5分钟过期（添加随机时间避免缓存雪崩）
                .expireAfterWrite(Duration.ofMinutes(5).plusSeconds(RANDOM.nextInt(60)))
                // 最大缓存条目数
                .maximumSize(1000)
                // 启用统计
                .recordStats());

        log.info("✅ Caffeine本地缓存管理器初始化完成");
        return cacheManager;
    }

    /**
     * L2缓存：Redis缓存管理器
     * 用于分布式环境下的数据共享
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager() {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 设置默认过期时间：10分钟 + 随机时间（避免缓存雪崩）
                .entryTtl(Duration.ofMinutes(10).plusSeconds(RANDOM.nextInt(120)))
                // 禁用缓存null值
                .disableCachingNullValues()
                // 设置key序列化器
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 设置value序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 针对不同缓存名称设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户缓存：30分钟
        cacheConfigurations.put("userCache", defaultConfig
                .entryTtl(Duration.ofMinutes(30).plusSeconds(RANDOM.nextInt(180))));
        
        // 群组缓存：15分钟
        cacheConfigurations.put("groupCache", defaultConfig
                .entryTtl(Duration.ofMinutes(15).plusSeconds(RANDOM.nextInt(120))));
        
        // 文章缓存：10分钟
        cacheConfigurations.put("articleCache", defaultConfig
                .entryTtl(Duration.ofMinutes(10).plusSeconds(RANDOM.nextInt(60))));
        
        // 搜索结果缓存：3分钟
        cacheConfigurations.put("searchCache", defaultConfig
                .entryTtl(Duration.ofMinutes(3).plusSeconds(RANDOM.nextInt(30))));
        
        // 消息缓存：5分钟
        cacheConfigurations.put("messageCache", defaultConfig
                .entryTtl(Duration.ofMinutes(5).plusSeconds(RANDOM.nextInt(60))));
        
        // 联系人缓存：20分钟
        cacheConfigurations.put("contactCache", defaultConfig
                .entryTtl(Duration.ofMinutes(20).plusSeconds(RANDOM.nextInt(120))));

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("✅ Redis分布式缓存管理器初始化完成");
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

