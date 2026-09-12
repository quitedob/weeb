package com.web.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.web.service.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存服务实现
 * 实现L1（Caffeine本地缓存）+ L2（Redis分布式缓存）策略
 */
@Slf4j
@Service
public class MultiLevelCacheServiceImpl implements MultiLevelCacheService {

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    @Qualifier("redisCacheManager")
    private CacheManager redisCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 从多级缓存中获取数据
     * L1 → L2 → Loader
     */
    @Override
    public <T> T get(String cacheName, String key, Supplier<T> loader) {
        return get(cacheName, key, null, loader);
    }

    /**
     * 从多级缓存中获取数据（指定类型）
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Class<T> type, Supplier<T> loader) {
        // 1. 尝试从L1缓存获取
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            org.springframework.cache.Cache.ValueWrapper l1Value = l1Cache.get(key);
            if (l1Value != null && l1Value.get() != null) {
                log.debug("✅ L1缓存命中: cacheName={}, key={}", cacheName, key);
                return (T) l1Value.get();
            }
        }

        // 2. L1未命中，尝试从L2缓存获取
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            org.springframework.cache.Cache.ValueWrapper l2Value = l2Cache.get(key);
            if (l2Value != null && l2Value.get() != null) {
                log.debug("✅ L2缓存命中: cacheName={}, key={}", cacheName, key);
                T value = (T) l2Value.get();
                
                // 回写到L1缓存
                if (l1Cache != null) {
                    l1Cache.put(key, value);
                    log.debug("⬆️ 数据回写到L1缓存: cacheName={}, key={}", cacheName, key);
                }
                
                return value;
            }
        }

        // 3. L1和L2都未命中，执行loader加载数据
        log.debug("❌ 缓存未命中，执行loader: cacheName={}, key={}", cacheName, key);
        T value = loader.get();
        
        if (value != null) {
            // 写入L1和L2缓存
            put(cacheName, key, value);
        }
        
        return value;
    }

    /**
     * 设置缓存数据到多级缓存
     */
    @Override
    public void put(String cacheName, String key, Object value) {
        if (value == null) {
            log.warn("⚠️ 尝试缓存null值，已忽略: cacheName={}, key={}", cacheName, key);
            return;
        }

        // 写入L1缓存
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            l1Cache.put(key, value);
            log.debug("✅ 数据写入L1缓存: cacheName={}, key={}", cacheName, key);
        }

        // 写入L2缓存
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.put(key, value);
            log.debug("✅ 数据写入L2缓存: cacheName={}, key={}", cacheName, key);
        }
    }

    /**
     * 从多级缓存中删除数据
     */
    @Override
    public void evict(String cacheName, String key) {
        // 从L1缓存删除
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            l1Cache.evict(key);
            log.debug("🗑️ 从L1缓存删除: cacheName={}, key={}", cacheName, key);
        }

        // 从L2缓存删除
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.evict(key);
            log.debug("🗑️ 从L2缓存删除: cacheName={}, key={}", cacheName, key);
        }
    }

    /**
     * 清空指定缓存的所有数据
     */
    @Override
    public void clear(String cacheName) {
        // 清空L1缓存
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null) {
            l1Cache.clear();
            log.info("🗑️ L1缓存已清空: cacheName={}", cacheName);
        }

        // 清空L2缓存
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(cacheName);
        if (l2Cache != null) {
            l2Cache.clear();
            log.info("🗑️ L2缓存已清空: cacheName={}", cacheName);
        }
    }

    /**
     * 检查缓存是否存在
     */
    @Override
    public boolean exists(String cacheName, String key) {
        // 先检查L1缓存
        org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
        if (l1Cache != null && l1Cache.get(key) != null) {
            return true;
        }

        // 再检查L2缓存
        org.springframework.cache.Cache l2Cache = redisCacheManager.getCache(cacheName);
        return l2Cache != null && l2Cache.get(key) != null;
    }

    /**
     * 获取缓存统计信息
     */
    @Override
    public Map<String, Object> getStatistics(String cacheName) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // L1缓存统计（Caffeine）
            org.springframework.cache.Cache l1Cache = caffeineCacheManager.getCache(cacheName);
            if (l1Cache != null) {
                Object nativeCache = l1Cache.getNativeCache();
                if (nativeCache instanceof Cache) {
                    @SuppressWarnings("unchecked")
                    Cache<Object, Object> caffeineCache = (Cache<Object, Object>) nativeCache;
                    CacheStats cacheStats = caffeineCache.stats();
                    
                    Map<String, Object> l1Stats = new HashMap<>();
                    l1Stats.put("hitCount", cacheStats.hitCount());
                    l1Stats.put("missCount", cacheStats.missCount());
                    l1Stats.put("hitRate", cacheStats.hitRate());
                    l1Stats.put("evictionCount", cacheStats.evictionCount());
                    l1Stats.put("size", caffeineCache.estimatedSize());
                    
                    stats.put("l1Cache", l1Stats);
                }
            }

            // L2缓存统计（Redis）
            // Redis缓存统计需要通过RedisTemplate获取
            Map<String, Object> l2Stats = new HashMap<>();
            l2Stats.put("type", "Redis");
            l2Stats.put("connected", redisTemplate.getConnectionFactory() != null);
            stats.put("l2Cache", l2Stats);

        } catch (Exception e) {
            log.error("获取缓存统计信息失败: cacheName={}", cacheName, e);
        }

        return stats;
    }
}
