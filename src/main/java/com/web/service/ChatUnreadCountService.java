package com.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** SQL is authoritative. Reads do not cache a value from an uncommitted message transaction. */
@Service
public class ChatUnreadCountService {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    public int getUnreadCount(Long userId, Long chatId) {
        List<Integer> values = jdbcTemplate.queryForList(
                "SELECT unread_count FROM chat_unread_count WHERE user_id=? AND chat_id=?", Integer.class, userId, chatId);
        return values.isEmpty() ? 0 : values.get(0);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void incrementUnreadCount(Long userId, Long chatId, int increment) {
        jdbcTemplate.update("INSERT INTO chat_unread_count(user_id,chat_id,unread_count,updated_at) VALUES (?,?,?,NOW()) "
                + "ON DUPLICATE KEY UPDATE unread_count=unread_count+?,updated_at=NOW()", userId, chatId, increment, increment);
        jdbcTemplate.update("UPDATE chat_list SET unread_count=unread_count+?,update_time=CURRENT_TIMESTAMP(3) "
                + "WHERE user_id=? AND shared_chat_id=?", increment, userId, chatId);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void markAsRead(Long userId, Long chatId, Long lastReadMessageId) {
        markThrough(userId, chatId, lastReadMessageId == null ? 0L : lastReadMessageId);
    }

    /** Caller validates the observed message and locks the shared conversation before advancing this cursor. */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long markThrough(Long userId, Long chatId, long observedId) {
        jdbcTemplate.queryForObject("SELECT id FROM shared_chat WHERE id=? FOR UPDATE", Long.class, chatId);
        jdbcTemplate.update("INSERT INTO chat_unread_count(user_id,chat_id,unread_count,last_read_message_id,updated_at) "
                + "VALUES (?,?,0,0,NOW()) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)", userId, chatId);
        Long current = jdbcTemplate.queryForObject("SELECT last_read_message_id FROM chat_unread_count "
                + "WHERE user_id=? AND chat_id=? FOR UPDATE", Long.class, userId, chatId);
        long boundary = Math.max(current == null ? 0 : current, observedId);
        // The shared-chat lock serializes sends and reads. A delayed acknowledgement cannot clear newer messages.
        Integer unread = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message WHERE chat_id=? AND sender_id<>? "
                + "AND id>? AND COALESCE(is_recalled,0)=0", Integer.class, chatId, userId, boundary);
        jdbcTemplate.update("UPDATE chat_unread_count SET last_read_message_id=?,unread_count=?,updated_at=NOW() "
                + "WHERE user_id=? AND chat_id=?", boundary, unread, userId, chatId);
        jdbcTemplate.update("UPDATE chat_list SET unread_count=? WHERE user_id=? AND shared_chat_id=?", unread, userId, chatId);
        jdbcTemplate.update("UPDATE message SET status=3 WHERE chat_id=? AND receiver_id=? AND id<=? AND status<3",
                chatId, userId, boundary);
        return boundary;
    }

    public int getTotalUnreadCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(unread_count),0) FROM chat_unread_count WHERE user_id=?",
                Integer.class, userId);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> getUnreadCountList(Long userId) {
        return jdbcTemplate.queryForList("SELECT chat_id,unread_count,last_read_message_id,updated_at FROM chat_unread_count "
                + "WHERE user_id=? AND unread_count>0 ORDER BY updated_at DESC", userId);
    }

    /** Compatibility hook for callers clearing pre-upgrade cache keys; correctness no longer depends on Redis. */
    public void clearUserCache(Long userId) {
        if (redisTemplate != null) {
            try {
                var keys = redisTemplate.keys("chat:unread:" + userId + ":*");
                if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
            } catch (RuntimeException ignored) { /* Legacy cache cleanup cannot affect SQL reads. */ }
        }
    }

    public int getGroupUnreadCount(Long userId, Long groupId) {
        List<Long> chats = jdbcTemplate.queryForList("SELECT shared_chat_id FROM `group` WHERE id=?", Long.class, groupId);
        return chats.isEmpty() || chats.get(0) == null ? 0 : getUnreadCount(userId, chats.get(0));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void batchMarkAsRead(Long userId, List<Long> chatIds) {
        if (chatIds == null) return;
        for (Long chatId : chatIds.stream().distinct().sorted().toList()) {
            jdbcTemplate.queryForObject("SELECT id FROM shared_chat WHERE id=? FOR UPDATE", Long.class, chatId);
            Long last = jdbcTemplate.queryForObject("SELECT MAX(id) FROM message WHERE chat_id=?", Long.class, chatId);
            markThrough(userId, chatId, last == null ? 0 : last);
        }
    }
}
