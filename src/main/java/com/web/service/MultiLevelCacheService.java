package com.web.service;

import java.util.function.Supplier;

/**
 * 多级缓存服务接口
 * 实现L1（Caffeine）+ L2（Redis）多级缓存策略
 */
public interface MultiLevelCacheService {

    /**
     * 从多级缓存中获取数据
     * 优先从L1缓存获取，L1未命中则从L2获取，L2未命中则执行loader并缓存结果
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 数据加载器（缓存未命中时执行）
     * @param <T> 数据类型
     * @return 缓存数据
     */
    <T> T get(String cacheName, String key, Supplier<T> loader);

    /**
     * 从多级缓存中获取数据（指定类型）
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param type 数据类型
     * @param loader 数据加载器
     * @param <T> 数据类型
     * @return 缓存数据
     */
    <T> T get(String cacheName, String key, Class<T> type, Supplier<T> loader);

    /**
     * 设置缓存数据到多级缓存
     * 同时写入L1和L2缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(String cacheName, String key, Object value);

    /**
     * 从多级缓存中删除数据
     * 同时从L1和L2缓存中删除
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    void evict(String cacheName, String key);

    /**
     * 清空指定缓存的所有数据
     * 同时清空L1和L2缓存
     * 
     * @param cacheName 缓存名称
     */
    void clear(String cacheName);

    /**
     * 检查缓存是否存在
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String cacheName, String key);

    /**
     * 获取缓存统计信息
     * 
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    java.util.Map<String, Object> getStatistics(String cacheName);
}
