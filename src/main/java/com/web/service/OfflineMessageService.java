package com.web.service;

import com.web.vo.message.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 离线消息服务
 * 管理用户离线期间的消息存储和推送
 */
@Slf4j
@Service
public class OfflineMessageService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String OFFLINE_MESSAGE_PREFIX = "chat:offline:";
    private static final long MESSAGE_EXPIRE_DAYS = 30; // 30天过期

    /**
     * 存储离线消息
     */
    public void storeOfflineMessage(Long userId, MessageResponse message) {
        try {
            String key = OFFLINE_MESSAGE_PREFIX + userId;
            
            // 使用List存储离线消息
            redisTemplate.opsForList().rightPush(key, message);
            
            // 设置过期时间
            redisTemplate.expire(key, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.debug("📦 离线消息已存储: userId={}, messageId={}", userId, message.getId());
        } catch (Exception e) {
            log.error("❌ 存储离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 获取用户的离线消息
     */
    public List<MessageResponse> getOfflineMessages(Long userId) {
        try {
            String key = OFFLINE_MESSAGE_PREFIX + userId;
            
            // 获取所有离线消息
            List<Object> messages = redisTemplate.opsForList().range(key, 0, -1);
            
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<MessageResponse> result = new ArrayList<>();
            for (Object obj : messages) {
                if (obj instanceof MessageResponse) {
                    result.add((MessageResponse) obj);
                }
            }
            
            log.info("📬 获取离线消息: userId={}, count={}", userId, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 获取离线消息失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 清除用户的离线消息
     */
    public void clearOfflineMessages(Long userId) {
        try {
            String key = OFFLINE_MESSAGE_PREFIX + userId;
            redisTemplate.delete(key);
            
            log.info("✅ 离线消息已清除: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 清除离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 获取离线消息数量
     */
    public long getOfflineMessageCount(Long userId) {
        try {
            String key = OFFLINE_MESSAGE_PREFIX + userId;
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("❌ 获取离线消息数量失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 批量存储离线消息
     */
    public void batchStoreOfflineMessages(Long userId, List<MessageResponse> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            String key = OFFLINE_MESSAGE_PREFIX + userId;
            
            // 批量添加
            redisTemplate.opsForList().rightPushAll(key, messages.toArray());
            
            // 设置过期时间
            redisTemplate.expire(key, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.info("📦 批量存储离线消息: userId={}, count={}", userId, messages.size());
        } catch (Exception e) {
            log.error("❌ 批量存储离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 定时清理过期的离线消息（每天凌晨3点执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMessages() {
        try {
            log.info("🧹 开始清理过期离线消息");
            
            // 获取所有离线消息key
            Set<String> keys = redisTemplate.keys(OFFLINE_MESSAGE_PREFIX + "*");
            
            if (keys == null || keys.isEmpty()) {
                log.info("✅ 无需清理");
                return;
            }
            
            int cleanedCount = 0;
            for (String key : keys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.DAYS);
                
                // 如果TTL小于1天，删除
                if (ttl != null && ttl < 1) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }
            
            log.info("✅ 清理完成: 清理了{}个过期消息队列", cleanedCount);
        } catch (Exception e) {
            log.error("❌ 清理过期消息失败", e);
        }
    }

    /**
     * 获取离线消息统计
     */
    public java.util.Map<String, Object> getOfflineMessageStats(Long userId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            long count = getOfflineMessageCount(userId);
            stats.put("count", count);
            stats.put("hasMessages", count > 0);
            
            if (count > 0) {
                String key = OFFLINE_MESSAGE_PREFIX + userId;
                Long ttl = redisTemplate.getExpire(key, TimeUnit.DAYS);
                stats.put("expireInDays", ttl);
            }
            
        } catch (Exception e) {
            log.error("❌ 获取离线消息统计失败: userId={}", userId, e);
        }
        
        return stats;
    }
}
