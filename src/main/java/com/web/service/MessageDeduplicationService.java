package com.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息去重服务
 * 使用Redis缓存最近消息ID，防止重复消息
 */
@Slf4j
@Service
public class MessageDeduplicationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String MESSAGE_ID_PREFIX = "msg:dedup:";
    private static final long CACHE_EXPIRE_MINUTES = 30; // 30分钟过期

    /**
     * 检查消息是否已存在（去重）
     * @param clientMessageId 客户端消息ID
     * @return true=已存在（重复），false=不存在（新消息）
     */
    public boolean isDuplicate(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isEmpty()) {
            return false;
        }

        try {
            String key = MESSAGE_ID_PREFIX + clientMessageId;
            Boolean exists = redisTemplate.hasKey(key);
            
            if (exists != null && exists) {
                log.warn("⚠️ 检测到重复消息: clientMessageId={}", clientMessageId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("❌ 检查消息重复失败: clientMessageId={}", clientMessageId, e);
            // 出错时返回false，允许消息通过（避免误拦截）
            return false;
        }
    }

    /**
     * 标记消息已处理
     * @param clientMessageId 客户端消息ID
     * @param messageId 数据库消息ID
     */
    public void markAsProcessed(String clientMessageId, Long messageId) {
        if (clientMessageId == null || clientMessageId.isEmpty()) {
            return;
        }

        try {
            String key = MESSAGE_ID_PREFIX + clientMessageId;
            // 存储消息ID，30分钟后自动过期
            redisTemplate.opsForValue().set(key, messageId, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.debug("✅ 消息已标记为已处理: clientMessageId={}, messageId={}", clientMessageId, messageId);
        } catch (Exception e) {
            log.error("❌ 标记消息失败: clientMessageId={}", clientMessageId, e);
        }
    }

    /**
     * 获取消息ID（如果已存在）
     * @param clientMessageId 客户端消息ID
     * @return 消息ID，如果不存在返回null
     */
    public Long getMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isEmpty()) {
            return null;
        }

        try {
            String key = MESSAGE_ID_PREFIX + clientMessageId;
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                return Long.valueOf(value.toString());
            }
            
            return null;
        } catch (Exception e) {
            log.error("❌ 获取消息ID失败: clientMessageId={}", clientMessageId, e);
            return null;
        }
    }

    /**
     * 清除消息缓存
     * @param clientMessageId 客户端消息ID
     */
    public void clear(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isEmpty()) {
            return;
        }

        try {
            String key = MESSAGE_ID_PREFIX + clientMessageId;
            redisTemplate.delete(key);
            
            log.debug("✅ 消息缓存已清除: clientMessageId={}", clientMessageId);
        } catch (Exception e) {
            log.error("❌ 清除消息缓存失败: clientMessageId={}", clientMessageId, e);
        }
    }

    /**
     * 批量检查消息是否重复
     * @param clientMessageIds 客户端消息ID列表
     * @return 重复的消息ID列表
     */
    public java.util.List<String> findDuplicates(java.util.List<String> clientMessageIds) {
        java.util.List<String> duplicates = new java.util.ArrayList<>();
        
        if (clientMessageIds == null || clientMessageIds.isEmpty()) {
            return duplicates;
        }

        for (String clientMessageId : clientMessageIds) {
            if (isDuplicate(clientMessageId)) {
                duplicates.add(clientMessageId);
            }
        }

        return duplicates;
    }
}
