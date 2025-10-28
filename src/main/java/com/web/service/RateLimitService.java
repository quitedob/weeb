package com.web.service;

import java.util.Map;

/**
 * 限流服务接口
 */
public interface RateLimitService {

    /**
     * 设置动态限流配置
     * @param path API路径
     * @param maxRequests 最大请求数
     * @return 是否成功
     */
    boolean setDynamicRateLimit(String path, int maxRequests);

    /**
     * 获取动态限流配置
     * @param path API路径
     * @return 最大请求数
     */
    Integer getDynamicRateLimit(String path);

    /**
     * 删除动态限流配置
     * @param path API路径
     * @return 是否成功
     */
    boolean removeDynamicRateLimit(String path);

    /**
     * 获取所有动态限流配置
     * @return 配置列表
     */
    Map<String, Integer> getAllDynamicRateLimits();

    /**
     * 获取限流统计信息
     * @return 统计信息
     */
    Map<String, Object> getRateLimitStatistics();

    /**
     * 获取限流事件列表
     * @param limit 数量限制
     * @return 事件列表
     */
    java.util.List<Map<String, Object>> getRateLimitEvents(int limit);

    /**
     * 获取限流告警列表
     * @return 告警列表
     */
    java.util.List<Map<String, Object>> getRateLimitAlerts();

    /**
     * 清除限流统计
     * @return 是否成功
     */
    boolean clearRateLimitStatistics();

    /**
     * 手动解除限流
     * @param identifier 标识符（用户ID或IP）
     * @param path API路径
     * @param type 类型（USER或IP）
     * @return 是否成功
     */
    boolean unlockRateLimit(String identifier, String path, String type);
}
