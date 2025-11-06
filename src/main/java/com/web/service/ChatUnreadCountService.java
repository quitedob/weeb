package com.web.service;

import com.web.model.ChatUnreadCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 聊天未读计数服务
 * 优化未读消息计数的性能和准确性
 */
@Slf4j
@Service
public class ChatUnreadCountService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_COUNT_PREFIX = "chat:unread:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    /**
     * 获取用户在某个聊天的未读数
     * @param userId 用户ID
     * @param chatId 聊天ID
     * @return 未读数
     */
    public int getUnreadCount(Long userId, Long chatId) {
        try {
            // 1. 尝试从Redis缓存获取
            String cacheKey = UNREAD_COUNT_PREFIX + userId + ":" + chatId;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Integer.parseInt(cached.toString());
            }

            // 2. 从数据库获取
            String sql = "SELECT unread_count FROM chat_unread_count WHERE user_id = ? AND chat_id = ?";
            List<Integer> results = jdbcTemplate.queryForList(sql, Integer.class, userId, chatId);
            
            int unreadCount = results.isEmpty() ? 0 : results.get(0);

            // 3. 缓存到Redis
            redisTemplate.opsForValue().set(cacheKey, unreadCount, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            return unreadCount;

        } catch (Exception e) {
            log.error("获取未读计数失败: userId={}, chatId={}", userId, chatId, e);
            return 0;
        }
    }

    /**
     * 增加未读计数
     * @param userId 用户ID
     * @param chatId 聊天ID
     * @param increment 增加数量（默认1）
     */
    @Transactional
    public void incrementUnreadCount(Long userId, Long chatId, int increment) {
        try {
            String sql = "INSERT INTO chat_unread_count (user_id, chat_id, unread_count, updated_at) " +
                        "VALUES (?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "unread_count = unread_count + ?, " +
                        "updated_at = NOW()";
            
            jdbcTemplate.update(sql, userId, chatId, increment, increment);

            // 清除Redis缓存
            String cacheKey = UNREAD_COUNT_PREFIX + userId + ":" + chatId;
            redisTemplate.delete(cacheKey);

            log.debug("增加未读计数: userId={}, chatId={}, increment={}", userId, chatId, increment);

        } catch (Exception e) {
            log.error("增加未读计数失败: userId={}, chatId={}", userId, chatId, e);
        }
    }

    /**
     * 批量标记已读（重置未读计数为0）
     * @param userId 用户ID
     * @param chatId 聊天ID
     * @param lastReadMessageId 最后已读消息ID
     */
    @Transactional
    public void markAsRead(Long userId, Long chatId, Long lastReadMessageId) {
        try {
            String sql = "INSERT INTO chat_unread_count (user_id, chat_id, unread_count, last_read_message_id, updated_at) " +
                        "VALUES (?, ?, 0, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "unread_count = 0, " +
                        "last_read_message_id = ?, " +
                        "updated_at = NOW()";
            
            jdbcTemplate.update(sql, userId, chatId, lastReadMessageId, lastReadMessageId);

            // 清除Redis缓存
            String cacheKey = UNREAD_COUNT_PREFIX + userId + ":" + chatId;
            redisTemplate.delete(cacheKey);

            log.info("标记已读: userId={}, chatId={}, lastReadMessageId={}", userId, chatId, lastReadMessageId);

        } catch (Exception e) {
            log.error("标记已读失败: userId={}, chatId={}", userId, chatId, e);
        }
    }

    /**
     * 获取用户所有聊天的未读总数
     * @param userId 用户ID
     * @return 未读总数
     */
    public int getTotalUnreadCount(Long userId) {
        try {
            // 尝试从Redis缓存获取
            String cacheKey = UNREAD_COUNT_PREFIX + userId + ":total";
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Integer.parseInt(cached.toString());
            }

            // 从数据库获取
            String sql = "SELECT COALESCE(SUM(unread_count), 0) FROM chat_unread_count WHERE user_id = ?";
            Integer total = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            
            int totalUnread = total != null ? total : 0;

            // 缓存到Redis（较短的过期时间）
            redisTemplate.opsForValue().set(cacheKey, totalUnread, 5, TimeUnit.MINUTES);

            return totalUnread;

        } catch (Exception e) {
            log.error("获取总未读计数失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 获取用户所有聊天的未读计数列表
     * @param userId 用户ID
     * @return 未读计数列表
     */
    public List<Map<String, Object>> getUnreadCountList(Long userId) {
        try {
            String sql = "SELECT chat_id, unread_count, last_read_message_id, updated_at " +
                        "FROM chat_unread_count " +
                        "WHERE user_id = ? AND unread_count > 0 " +
                        "ORDER BY updated_at DESC";
            
            return jdbcTemplate.queryForList(sql, userId);

        } catch (Exception e) {
            log.error("获取未读计数列表失败: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 清除用户的所有未读计数缓存
     * @param userId 用户ID
     */
    public void clearUserCache(Long userId) {
        try {
            String pattern = UNREAD_COUNT_PREFIX + userId + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("清除用户未读计数缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 群组消息未读计数优化
     * 仅追踪最后已读消息ID，计算未读数
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 未读数
     */
    public int getGroupUnreadCount(Long userId, Long groupId) {
        try {
            // 获取用户在该群组的最后已读消息ID
            String sql1 = "SELECT last_read_message_id FROM chat_unread_count " +
                         "WHERE user_id = ? AND chat_id = ?";
            
            List<Long> results = jdbcTemplate.queryForList(sql1, Long.class, userId, groupId);
            Long lastReadMessageId = results.isEmpty() ? 0L : results.get(0);

            // 计算未读数：群组中ID大于lastReadMessageId的消息数
            String sql2 = "SELECT COUNT(*) FROM message " +
                         "WHERE group_id = ? AND id > ? AND sender_id != ?";
            
            Integer count = jdbcTemplate.queryForObject(sql2, Integer.class, groupId, lastReadMessageId, userId);
            
            return count != null ? count : 0;

        } catch (Exception e) {
            log.error("获取群组未读计数失败: userId={}, groupId={}", userId, groupId, e);
            return 0;
        }
    }

    /**
     * 批量更新多个聊天的未读计数
     * @param userId 用户ID
     * @param chatIds 聊天ID列表
     */
    @Transactional
    public void batchMarkAsRead(Long userId, List<Long> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return;
        }

        try {
            String sql = "UPDATE chat_unread_count SET unread_count = 0, updated_at = NOW() " +
                        "WHERE user_id = ? AND chat_id IN (" +
                        String.join(",", chatIds.stream().map(String::valueOf).toArray(String[]::new)) + ")";
            
            int updated = jdbcTemplate.update(sql, userId);
            log.info("批量标记已读: userId={}, count={}", userId, updated);

            // 清除缓存
            clearUserCache(userId);

        } catch (Exception e) {
            log.error("批量标记已读失败: userId={}", userId, e);
        }
    }
}
