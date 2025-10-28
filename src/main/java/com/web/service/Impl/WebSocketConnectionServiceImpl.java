package com.web.service.Impl;

import com.web.service.WebSocketConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WebSocket连接管理服务实现
 */
@Slf4j
@Service
public class WebSocketConnectionServiceImpl implements WebSocketConnectionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis键前缀
    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "ws:user:sessions:";
    private static final String ONLINE_USERS_KEY = "ws:online:users";
    private static final String CONNECTION_STATS_KEY = "ws:stats";

    // 连接超时时间（秒）
    private static final long CONNECTION_TIMEOUT = 90; // 90秒无心跳则认为连接断开
    private static final long SESSION_TTL = 120; // 会话在Redis中的TTL

    @Override
    public void registerConnection(String sessionId, Long userId, String username) {
        try {
            // 保存会话信息
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("userId", userId);
            sessionInfo.put("username", username);
            sessionInfo.put("connectTime", LocalDateTime.now().toString());
            sessionInfo.put("lastHeartbeat", System.currentTimeMillis());
            sessionInfo.put("status", "CONNECTED");

            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.opsForHash().putAll(sessionKey, sessionInfo);
            redisTemplate.expire(sessionKey, SESSION_TTL, TimeUnit.SECONDS);

            // 添加到用户会话集合
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, SESSION_TTL, TimeUnit.SECONDS);

            // 添加到在线用户集合
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);

            // 更新统计信息
            redisTemplate.opsForHash().increment(CONNECTION_STATS_KEY, "totalConnections", 1);
            redisTemplate.opsForHash().put(CONNECTION_STATS_KEY, "lastUpdateTime", LocalDateTime.now().toString());

            log.info("WebSocket连接已注册: sessionId={}, userId={}, username={}", sessionId, userId, username);

        } catch (Exception e) {
            log.error("注册WebSocket连接失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }

    @Override
    public void unregisterConnection(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;

            // 获取会话信息
            Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionKey);
            if (sessionInfo.isEmpty()) {
                return;
            }

            Long userId = Long.valueOf(sessionInfo.get("userId").toString());

            // 从用户会话集合中移除
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().remove(userSessionsKey, sessionId);

            // 检查用户是否还有其他活跃会话
            Long remainingSessions = redisTemplate.opsForSet().size(userSessionsKey);
            if (remainingSessions == null || remainingSessions == 0) {
                // 从在线用户集合中移除
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
            }

            // 删除会话信息
            redisTemplate.delete(sessionKey);

            // 更新统计信息
            redisTemplate.opsForHash().increment(CONNECTION_STATS_KEY, "totalDisconnections", 1);

            log.info("WebSocket连接已注销: sessionId={}, userId={}", sessionId, userId);

        } catch (Exception e) {
            log.error("注销WebSocket连接失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public void updateHeartbeat(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;

            // 检查会话是否存在
            if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
                log.warn("会话不存在，无法更新心跳: sessionId={}", sessionId);
                return;
            }

            // 更新心跳时间
            redisTemplate.opsForHash().put(sessionKey, "lastHeartbeat", System.currentTimeMillis());
            redisTemplate.opsForHash().put(sessionKey, "status", "ACTIVE");

            // 延长会话TTL
            redisTemplate.expire(sessionKey, SESSION_TTL, TimeUnit.SECONDS);

            // 更新统计信息
            redisTemplate.opsForHash().increment(CONNECTION_STATS_KEY, "totalHeartbeats", 1);

            log.debug("心跳已更新: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("更新心跳失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public boolean isConnectionAlive(String sessionId) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;

            // 检查会话是否存在
            if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey))) {
                return false;
            }

            // 获取最后心跳时间
            Object lastHeartbeatObj = redisTemplate.opsForHash().get(sessionKey, "lastHeartbeat");
            if (lastHeartbeatObj == null) {
                return false;
            }

            long lastHeartbeat = Long.parseLong(lastHeartbeatObj.toString());
            long currentTime = System.currentTimeMillis();

            // 检查是否超时
            return (currentTime - lastHeartbeat) < (CONNECTION_TIMEOUT * 1000);

        } catch (Exception e) {
            log.error("检查连接状态失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    @Override
    public Set<String> getUserActiveSessions(Long userId) {
        try {
            String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
            Set<Object> sessions = redisTemplate.opsForSet().members(userSessionsKey);

            if (sessions == null || sessions.isEmpty()) {
                return new HashSet<>();
            }

            // 过滤出存活的会话
            return sessions.stream()
                    .map(Object::toString)
                    .filter(this::isConnectionAlive)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("获取用户活跃会话失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    @Override
    public long getOnlineUserCount() {
        try {
            Long count = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
            return 0;
        }
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        try {
            Set<Object> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            if (userIds == null || userIds.isEmpty()) {
                return new HashSet<>();
            }

            return userIds.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("获取在线用户ID列表失败", e);
            return new HashSet<>();
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId));
        } catch (Exception e) {
            log.error("检查用户在线状态失败: userId={}", userId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getConnectionStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(CONNECTION_STATS_KEY);
            Map<String, Object> result = new HashMap<>();

            stats.forEach((key, value) -> result.put(key.toString(), value));

            // 添加当前在线用户数
            result.put("currentOnlineUsers", getOnlineUserCount());
            result.put("timestamp", LocalDateTime.now().toString());

            return result;

        } catch (Exception e) {
            log.error("获取连接统计信息失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public int cleanExpiredConnections() {
        int cleanedCount = 0;

        try {
            // 获取所有在线用户
            Set<Long> onlineUserIds = getOnlineUserIds();

            for (Long userId : onlineUserIds) {
                Set<String> sessions = getUserActiveSessions(userId);

                // 检查每个会话是否过期
                String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
                Set<Object> allSessions = redisTemplate.opsForSet().members(userSessionsKey);

                if (allSessions != null) {
                    for (Object sessionObj : allSessions) {
                        String sessionId = sessionObj.toString();

                        if (!isConnectionAlive(sessionId)) {
                            // 清理过期连接
                            unregisterConnection(sessionId);
                            cleanedCount++;
                        }
                    }
                }
            }

            log.info("清理过期连接完成: 清理数量={}", cleanedCount);

        } catch (Exception e) {
            log.error("清理过期连接失败", e);
        }

        return cleanedCount;
    }

    @Override
    public Map<String, Object> getUserConnectionInfo(Long userId) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("userId", userId);
            info.put("isOnline", isUserOnline(userId));

            Set<String> activeSessions = getUserActiveSessions(userId);
            info.put("activeSessionCount", activeSessions.size());

            List<Map<String, Object>> sessionDetails = new ArrayList<>();
            for (String sessionId : activeSessions) {
                String sessionKey = SESSION_KEY_PREFIX + sessionId;
                Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries(sessionKey);

                if (!sessionInfo.isEmpty()) {
                    Map<String, Object> detail = new HashMap<>();
                    sessionInfo.forEach((key, value) -> detail.put(key.toString(), value));
                    sessionDetails.add(detail);
                }
            }

            info.put("sessions", sessionDetails);
            info.put("timestamp", LocalDateTime.now().toString());

            return info;

        } catch (Exception e) {
            log.error("获取用户连接信息失败: userId={}", userId, e);
            return new HashMap<>();
        }
    }
}
