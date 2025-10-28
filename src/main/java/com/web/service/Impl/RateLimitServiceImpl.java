package com.web.service.Impl;

import com.web.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流服务实现
 */
@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_CONFIG_KEY_PREFIX = "rate:limit:config:";
    private static final String RATE_LIMIT_STATS_KEY = "rate:limit:stats";
    private static final String RATE_LIMIT_EVENT_KEY_PREFIX = "rate:limit:event:";
    private static final String RATE_LIMIT_ALERT_KEY_PREFIX = "rate:limit:alert:";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";

    @Override
    public boolean setDynamicRateLimit(String path, int maxRequests) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        if (maxRequests <= 0) {
            return false;
        }

        try {
            String key = RATE_LIMIT_CONFIG_KEY_PREFIX + path;
            redisTemplate.opsForValue().set(key, maxRequests);

            log.info("动态限流配置已设置: path={}, maxRequests={}", path, maxRequests);
            return true;

        } catch (Exception e) {
            log.error("设置动态限流配置失败: path={}", path, e);
            return false;
        }
    }

    @Override
    public Integer getDynamicRateLimit(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        try {
            String key = RATE_LIMIT_CONFIG_KEY_PREFIX + path;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                return Integer.parseInt(value.toString());
            }

            return null;

        } catch (Exception e) {
            log.error("获取动态限流配置失败: path={}", path, e);
            return null;
        }
    }

    @Override
    public boolean removeDynamicRateLimit(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        try {
            String key = RATE_LIMIT_CONFIG_KEY_PREFIX + path;
            Boolean result = redisTemplate.delete(key);

            log.info("动态限流配置已删除: path={}", path);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.error("删除动态限流配置失败: path={}", path, e);
            return false;
        }
    }

    @Override
    public Map<String, Integer> getAllDynamicRateLimits() {
        Map<String, Integer> configs = new HashMap<>();

        try {
            Set<String> keys = redisTemplate.keys(RATE_LIMIT_CONFIG_KEY_PREFIX + "*");

            if (keys != null) {
                for (String key : keys) {
                    String path = key.substring(RATE_LIMIT_CONFIG_KEY_PREFIX.length());
                    Object value = redisTemplate.opsForValue().get(key);

                    if (value != null) {
                        configs.put(path, Integer.parseInt(value.toString()));
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取所有动态限流配置失败", e);
        }

        return configs;
    }

    @Override
    public Map<String, Object> getRateLimitStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(RATE_LIMIT_STATS_KEY);
            Map<String, Object> result = new HashMap<>();

            stats.forEach((key, value) -> result.put(key.toString(), value));

            return result;

        } catch (Exception e) {
            log.error("获取限流统计信息失败", e);
            return new HashMap<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRateLimitEvents(int limit) {
        List<Map<String, Object>> events = new ArrayList<>();

        try {
            Set<String> keys = redisTemplate.keys(RATE_LIMIT_EVENT_KEY_PREFIX + "*");

            if (keys != null) {
                // 转换为列表并排序
                List<String> keyList = new ArrayList<>(keys);
                keyList.sort(Collections.reverseOrder()); // 按时间戳倒序

                // 限制数量
                int count = Math.min(limit, keyList.size());

                for (int i = 0; i < count; i++) {
                    Object event = redisTemplate.opsForValue().get(keyList.get(i));
                    if (event instanceof Map) {
                        events.add((Map<String, Object>) event);
                    }
                }
            }

        } catch (Exception e) {
            log.error("获取限流事件列表失败", e);
        }

        return events;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRateLimitAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        try {
            Set<String> keys = redisTemplate.keys(RATE_LIMIT_ALERT_KEY_PREFIX + "*");

            if (keys != null) {
                for (String key : keys) {
                    Object alert = redisTemplate.opsForValue().get(key);
                    if (alert instanceof Map) {
                        alerts.add((Map<String, Object>) alert);
                    }
                }
            }

            // 按时间戳排序
            alerts.sort((a1, a2) -> {
                String t1 = (String) a1.get("timestamp");
                String t2 = (String) a2.get("timestamp");
                return t2.compareTo(t1);
            });

        } catch (Exception e) {
            log.error("获取限流告警列表失败", e);
        }

        return alerts;
    }

    @Override
    public boolean clearRateLimitStatistics() {
        try {
            Boolean result = redisTemplate.delete(RATE_LIMIT_STATS_KEY);

            log.info("限流统计已清除");
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.error("清除限流统计失败", e);
            return false;
        }
    }

    @Override
    public boolean unlockRateLimit(String identifier, String path, String type) {
        if (identifier == null || path == null || type == null) {
            return false;
        }

        try {
            // 删除所有相关的限流键
            String pattern = RATE_LIMIT_KEY_PREFIX + type + ":" + identifier + ":" + path + ":*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("限流已解除: type={}, identifier={}, path={}, deleted={}",
                        type, identifier, path, deleted);
                return deleted != null && deleted > 0;
            }

            return false;

        } catch (Exception e) {
            log.error("解除限流失败: type={}, identifier={}, path={}", type, identifier, path, e);
            return false;
        }
    }
}
