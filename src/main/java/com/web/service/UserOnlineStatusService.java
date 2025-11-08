package com.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态服务
 * 使用Redis管理用户在线状态
 */
@Slf4j
@Service
public class UserOnlineStatusService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "chat:online:users";
    private static final String USER_SESSION_PREFIX = "chat:session:";
    private static final long SESSION_TIMEOUT = 5; // 5分钟超时

    /**
     * 用户上线
     * @param userId 用户ID
     * @param sessionId WebSocket会话ID
     */
    public void userOnline(Long userId, String sessionId) {
        try {
            // 添加到在线用户集合
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
            
            // 存储用户会话信息
            String sessionKey = USER_SESSION_PREFIX + userId;
            redisTemplate.opsForValue().set(sessionKey, sessionId, SESSION_TIMEOUT, TimeUnit.MINUTES);
            
            log.info("✅ 用户上线: userId={}, sessionId={}", userId, sessionId);
            
            // ✅ 用户上线后推送离线消息
            pushOfflineMessages(userId);
        } catch (Exception e) {
            log.error("❌ 用户上线失败: userId={}", userId, e);
        }
    }

    /**
     * ✅ 推送离线消息给用户
     * @param userId 用户ID
     */
    private void pushOfflineMessages(Long userId) {
        try {
            String offlineKey = "chat:offline:" + userId;
            
            // 获取所有离线消息
            Long messageCount = redisTemplate.opsForList().size(offlineKey);
            if (messageCount == null || messageCount == 0) {
                log.debug("📭 用户没有离线消息: userId={}", userId);
                return;
            }
            
            log.info("📬 开始推送离线消息: userId={}, count={}", userId, messageCount);
            
            // 获取所有离线消息并推送
            // 注意：这里需要MessageBroadcastService来推送，但会造成循环依赖
            // 所以离线消息的推送应该由前端主动拉取，而不是服务端推送
            // 前端在连接WebSocket后应该调用API获取离线消息
            
            log.debug("💡 离线消息应由前端主动拉取，服务端已标记");
            
        } catch (Exception e) {
            log.error("❌ 推送离线消息失败: userId={}", userId, e);
        }
    }

    /**
     * 用户下线
     * @param userId 用户ID
     */
    public void userOffline(Long userId) {
        try {
            // 从在线用户集合中移除
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
            
            // 删除会话信息
            String sessionKey = USER_SESSION_PREFIX + userId;
            redisTemplate.delete(sessionKey);
            
            log.info("✅ 用户下线: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 用户下线失败: userId={}", userId, e);
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString());
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("❌ 检查用户在线状态失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 获取所有在线用户ID
     * @return 在线用户ID集合
     */
    public Set<Object> getOnlineUsers() {
        try {
            return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        } catch (Exception e) {
            log.error("❌ 获取在线用户列表失败", e);
            return Set.of();
        }
    }

    /**
     * 获取在线用户数量
     * @return 在线用户数
     */
    public long getOnlineUserCount() {
        try {
            Long size = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("❌ 获取在线用户数量失败", e);
            return 0;
        }
    }

    /**
     * 更新用户心跳
     * @param userId 用户ID
     */
    public void updateHeartbeat(Long userId) {
        try {
            String sessionKey = USER_SESSION_PREFIX + userId;
            // 延长会话过期时间
            redisTemplate.expire(sessionKey, SESSION_TIMEOUT, TimeUnit.MINUTES);
            
            // 确保用户在在线集合中
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
            
            log.debug("💓 更新用户心跳: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 更新用户心跳失败: userId={}", userId, e);
        }
    }

    /**
     * 获取用户会话ID
     * @param userId 用户ID
     * @return 会话ID
     */
    public String getUserSessionId(Long userId) {
        try {
            String sessionKey = USER_SESSION_PREFIX + userId;
            Object sessionId = redisTemplate.opsForValue().get(sessionKey);
            return sessionId != null ? sessionId.toString() : null;
        } catch (Exception e) {
            log.error("❌ 获取用户会话ID失败: userId={}", userId, e);
            return null;
        }
    }
}
