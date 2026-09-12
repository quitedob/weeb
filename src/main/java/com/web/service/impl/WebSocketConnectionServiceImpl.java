package com.web.service.impl;

import com.web.service.WebSocketConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
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
    private static final String SESSION_OWNER_PREFIX = "ws:session:owner:";
    private static final String CHAT_ONLINE_USERS_KEY = "chat:online:users";
    private static final String CHAT_SESSION_PREFIX = "chat:session:";

    // 连接超时时间（秒）
    private static final long CONNECTION_TIMEOUT = 90; // 90秒无心跳则认为连接断开
    private static final long SESSION_TTL = 120; // 会话在Redis中的TTL

    // All values passed to Redis retain the application's existing JSON serializers.
    private static final String TOUCH_SESSION = """
            if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[1] then return 0 end
            redis.call('HSET', KEYS[1], 'lastHeartbeat', ARGV[2], 'status', ARGV[3])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('SADD', KEYS[2], ARGV[5])
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            redis.call('SADD', KEYS[3], ARGV[6])
            redis.call('SADD', KEYS[4], ARGV[7])
            redis.call('SET', KEYS[5], ARGV[5], 'EX', 300)
            redis.call('SET', KEYS[6], ARGV[6], 'EX', 360)
            return 1
            """;

    // Recheck each timestamp inside Redis: a heartbeat that wins the race prevents stale cleanup.
    // An explicit disconnect removes only that session; other live tabs keep both presence indexes.
    private static final String RECONCILE_USER = """
            local function decoded(raw)
                if not raw then return nil end
                local ok, value = pcall(cjson.decode, raw)
                if not ok then return nil end
                if type(value) == 'table' then value = value[2] end
                return value
            end
            local removed = 0
            local function reap(sessionId, member, force)
                local hash = ARGV[4] .. sessionId
                local owner = redis.call('HGET', hash, 'userId')
                local heartbeat = tonumber(decoded(redis.call('HGET', hash, 'lastHeartbeat')))
                if owner == ARGV[1] and not force and heartbeat and heartbeat > tonumber(ARGV[6]) then
                    return false
                end
                local indexed = redis.call('SREM', KEYS[1], member)
                if owner == ARGV[1] then
                    redis.call('DEL', hash, ARGV[5] .. sessionId)
                    removed = removed + 1
                else
                    if redis.call('GET', ARGV[5] .. sessionId) == ARGV[2] then
                        redis.call('DEL', ARGV[5] .. sessionId)
                    end
                    removed = removed + indexed
                end
                return true
            end
            if ARGV[7] ~= '' then reap(ARGV[7], ARGV[8], true) end
            local liveMember = nil
            for _, member in ipairs(redis.call('SMEMBERS', KEYS[1])) do
                local sessionId = decoded(member)
                if type(sessionId) ~= 'string' or sessionId == '' then
                    redis.call('SREM', KEYS[1], member)
                elseif not reap(sessionId, member, false) then
                    liveMember = member
                end
            end
            if liveMember then
                redis.call('SADD', KEYS[2], ARGV[2])
                redis.call('SADD', KEYS[3], ARGV[3])
                redis.call('SET', KEYS[4], liveMember, 'EX', 300)
            else
                redis.call('DEL', KEYS[1], KEYS[4])
                redis.call('SREM', KEYS[2], ARGV[2])
                redis.call('SREM', KEYS[3], ARGV[3])
            end
            if removed > 0 then redis.call('HINCRBY', KEYS[5], 'totalDisconnections', removed) end
            return removed
            """;

    @SuppressWarnings("unchecked")
    private byte[] value(Object value) {
        return ((RedisSerializer<Object>) redisTemplate.getValueSerializer()).serialize(value);
    }

    @SuppressWarnings("unchecked")
    private byte[] hashValue(Object value) {
        return ((RedisSerializer<Object>) redisTemplate.getHashValueSerializer()).serialize(value);
    }

    private byte[] text(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private long execute(String script, List<String> keys, byte[]... arguments) {
        byte[][] raw = new byte[keys.size() + arguments.length][];
        RedisSerializer<String> serializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
        for (int index = 0; index < keys.size(); index++) raw[index] = serializer.serialize(keys.get(index));
        System.arraycopy(arguments, 0, raw, keys.size(), arguments.length);
        Long result = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.scriptingCommands().eval(text(script), ReturnType.INTEGER, keys.size(), raw));
        return result == null ? 0 : result;
    }

    private Long validUserId(Object value) {
        if (value == null) return null;
        try {
            long userId = Long.parseLong(value.toString());
            return userId > 0 ? userId : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private long touchSession(String sessionId, Long userId, String status) {
        return execute(TOUCH_SESSION, List.of(SESSION_KEY_PREFIX + sessionId,
                        USER_SESSIONS_KEY_PREFIX + userId, ONLINE_USERS_KEY, CHAT_ONLINE_USERS_KEY,
                        CHAT_SESSION_PREFIX + userId, SESSION_OWNER_PREFIX + sessionId),
                hashValue(userId), hashValue(System.currentTimeMillis()), hashValue(status),
                text(Long.toString(SESSION_TTL)), value(sessionId), value(userId), value(userId.toString()));
    }

    private long reconcileUser(Long userId, String disconnectedSession) {
        return execute(RECONCILE_USER, List.of(USER_SESSIONS_KEY_PREFIX + userId,
                        ONLINE_USERS_KEY, CHAT_ONLINE_USERS_KEY, CHAT_SESSION_PREFIX + userId, CONNECTION_STATS_KEY),
                hashValue(userId), value(userId), value(userId.toString()), text(SESSION_KEY_PREFIX),
                text(SESSION_OWNER_PREFIX), text(Long.toString(System.currentTimeMillis() - CONNECTION_TIMEOUT * 1000)),
                text(disconnectedSession == null ? "" : disconnectedSession),
                value(disconnectedSession == null ? "" : disconnectedSession));
    }

    @Override
    public void registerConnection(String sessionId, Long userId, String username) {
        if (sessionId == null || sessionId.isBlank() || userId == null || userId <= 0
                || username == null || username.isBlank()) {
            throw new IllegalArgumentException("A session and valid user identity are required");
        }
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
            touchSession(sessionId, userId, "CONNECTED");

            log.info("WebSocket连接已注册: sessionId={}, userId={}, username={}", sessionId, userId, username);

        } catch (Exception e) {
            log.error("注册WebSocket连接失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }

    @Override
    public void unregisterConnection(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            Long userId = validUserId(redisTemplate.opsForHash().get(SESSION_KEY_PREFIX + sessionId, "userId"));
            if (userId == null) userId = validUserId(redisTemplate.opsForValue().get(SESSION_OWNER_PREFIX + sessionId));
            if (userId != null) reconcileUser(userId, sessionId);
            // Unknown duplicate/legacy disconnects are reconciled by the scheduled sweep.
        } catch (Exception e) {
            log.error("注销WebSocket连接失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public void updateHeartbeat(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            Long userId = validUserId(redisTemplate.opsForHash().get(SESSION_KEY_PREFIX + sessionId, "userId"));
            if (userId != null && touchSession(sessionId, userId, "ACTIVE") == 1) {
                redisTemplate.opsForHash().increment(CONNECTION_STATS_KEY, "totalHeartbeats", 1);
            }
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

            Set<Long> validUserIds = new HashSet<>();
            for (Object entry : userIds) {
                Long userId = null;
                if (entry != null) {
                    try {
                        userId = Long.valueOf(entry.toString());
                    } catch (NumberFormatException ignored) {
                        // Legacy registrations may have stored a blank or nonnumeric identity.
                    }
                }
                if (userId != null && userId > 0) {
                    validUserIds.add(userId);
                } else {
                    try {
                        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, entry);
                    } catch (Exception cleanupFailure) {
                        log.warn("Unable to remove an invalid WebSocket online user entry");
                    }
                }
            }
            return validUserIds;

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
            // Include the existing chat presence index so legacy chat-only ghosts are reconciled too.
            Set<Object> chatOnlineIds = redisTemplate.opsForSet().members(CHAT_ONLINE_USERS_KEY);
            if (chatOnlineIds != null) for (Object entry : chatOnlineIds) {
                Long userId = validUserId(entry);
                if (userId != null) onlineUserIds.add(userId);
            }
            for (Long userId : onlineUserIds) cleanedCount += (int) reconcileUser(userId, null);

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
