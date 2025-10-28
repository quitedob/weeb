package com.web.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.Message;
import com.web.service.MessageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 消息缓存服务实现
 * 使用Redis缓存消息，提高查询性能
 */
@Slf4j
@Service
public class MessageCacheServiceImpl implements MessageCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis键前缀
    private static final String MESSAGE_KEY_PREFIX = "message:";
    private static final String MESSAGE_LIST_KEY_PREFIX = "message:list:";
    private static final String CACHE_STATS_KEY = "message:cache:stats";

    // 缓存过期时间
    private static final long MESSAGE_CACHE_TTL = 3600; // 1小时
    private static final long MESSAGE_LIST_CACHE_TTL = 1800; // 30分钟

    // 缓存的最大消息数
    private static final int MAX_CACHED_MESSAGES = 100;

    @Override
    public void cacheMessage(Message message) {
        if (message == null || message.getId() == null) {
            return;
        }

        try {
            String key = MESSAGE_KEY_PREFIX + message.getId();
            redisTemplate.opsForValue().set(key, message, MESSAGE_CACHE_TTL, TimeUnit.SECONDS);

            // 更新统计
            redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "cachedMessages", 1);

            log.debug("消息已缓存: messageId={}", message.getId());

        } catch (Exception e) {
            log.error("缓存消息失败: messageId={}", message.getId(), e);
        }
    }

    @Override
    public Message getCachedMessage(Long messageId) {
        if (messageId == null) {
            return null;
        }

        try {
            String key = MESSAGE_KEY_PREFIX + messageId;
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                // 更新统计
                redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "cacheHits", 1);

                if (cached instanceof Message) {
                    return (Message) cached;
                }
            }

            // 更新统计
            redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "cacheMisses", 1);

            return null;

        } catch (Exception e) {
            log.error("获取缓存消息失败: messageId={}", messageId, e);
            return null;
        }
    }

    @Override
    public void cacheMessageList(Object chatId, List<Message> messages) {
        if (chatId == null || messages == null || messages.isEmpty()) {
            return;
        }

        try {
            // 只缓存最近的消息
            List<Message> messagesToCache = messages.size() > MAX_CACHED_MESSAGES ?
                    messages.subList(0, MAX_CACHED_MESSAGES) : messages;

            String key = MESSAGE_LIST_KEY_PREFIX + chatId.toString();
            redisTemplate.opsForValue().set(key, messagesToCache, MESSAGE_LIST_CACHE_TTL, TimeUnit.SECONDS);

            // 同时缓存单个消息
            for (Message message : messagesToCache) {
                cacheMessage(message);
            }

            log.debug("消息列表已缓存: chatId={}, count={}", chatId, messagesToCache.size());

        } catch (Exception e) {
            log.error("缓存消息列表失败: chatId={}", chatId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Message> getCachedMessageList(Object chatId, int page, int size) {
        if (chatId == null) {
            return null;
        }

        try {
            String key = MESSAGE_LIST_KEY_PREFIX + chatId.toString();
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                List<Message> messages;
                
                if (cached instanceof List) {
                    messages = (List<Message>) cached;
                } else {
                    // 尝试反序列化
                    messages = objectMapper.convertValue(cached, new TypeReference<List<Message>>() {});
                }

                // 分页处理
                int startIndex = (page - 1) * size;
                int endIndex = Math.min(startIndex + size, messages.size());

                if (startIndex < messages.size()) {
                    // 更新统计
                    redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "listCacheHits", 1);
                    return new ArrayList<>(messages.subList(startIndex, endIndex));
                }
            }

            // 更新统计
            redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "listCacheMisses", 1);

            return null;

        } catch (Exception e) {
            log.error("获取缓存消息列表失败: chatId={}", chatId, e);
            return null;
        }
    }

    @Override
    public void evictMessage(Long messageId) {
        if (messageId == null) {
            return;
        }

        try {
            String key = MESSAGE_KEY_PREFIX + messageId;
            redisTemplate.delete(key);

            log.debug("消息缓存已清除: messageId={}", messageId);

        } catch (Exception e) {
            log.error("清除消息缓存失败: messageId={}", messageId, e);
        }
    }

    @Override
    public void evictMessageList(Object chatId) {
        if (chatId == null) {
            return;
        }

        try {
            String key = MESSAGE_LIST_KEY_PREFIX + chatId.toString();
            redisTemplate.delete(key);

            log.debug("消息列表缓存已清除: chatId={}", chatId);

        } catch (Exception e) {
            log.error("清除消息列表缓存失败: chatId={}", chatId, e);
        }
    }

    @Override
    public void preloadMessages(Object chatId, int page, int size) {
        // 预加载逻辑由调用方实现
        // 这里只是标记预加载请求
        try {
            String preloadKey = "message:preload:" + chatId.toString() + ":" + page + ":" + size;
            redisTemplate.opsForValue().set(preloadKey, System.currentTimeMillis(), 60, TimeUnit.SECONDS);

            log.debug("消息预加载请求已记录: chatId={}, page={}, size={}", chatId, page, size);

        } catch (Exception e) {
            log.error("记录消息预加载请求失败: chatId={}", chatId, e);
        }
    }

    @Override
    public Map<String, Object> getCacheStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(CACHE_STATS_KEY);
            Map<String, Object> result = new HashMap<>();

            stats.forEach((key, value) -> result.put(key.toString(), value));

            // 计算缓存命中率
            long hits = getLongValue(result.get("cacheHits"));
            long misses = getLongValue(result.get("cacheMisses"));
            long total = hits + misses;

            if (total > 0) {
                double hitRate = (double) hits / total * 100;
                result.put("hitRate", String.format("%.2f%%", hitRate));
            } else {
                result.put("hitRate", "0.00%");
            }

            // 列表缓存命中率
            long listHits = getLongValue(result.get("listCacheHits"));
            long listMisses = getLongValue(result.get("listCacheMisses"));
            long listTotal = listHits + listMisses;

            if (listTotal > 0) {
                double listHitRate = (double) listHits / listTotal * 100;
                result.put("listHitRate", String.format("%.2f%%", listHitRate));
            } else {
                result.put("listHitRate", "0.00%");
            }

            return result;

        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取Long值
     */
    private long getLongValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
