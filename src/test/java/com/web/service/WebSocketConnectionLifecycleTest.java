package com.web.service;

import com.web.config.RedisConfig;
import com.web.service.impl.WebSocketConnectionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real Redis scripts and production JSON serializers; cleanup touches only random fixture identities. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_REDIS_PORT", matches = "16379")
@Timeout(20)
class WebSocketConnectionLifecycleTest {
    private final WebSocketConnectionServiceImpl service = new WebSocketConnectionServiceImpl();
    private final Set<String> ownedKeys = new HashSet<>();
    private LettuceConnectionFactory factory;
    private RedisTemplate<String, Object> redis;
    private Long userId;

    @BeforeEach
    void connectToDisposableRedis() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("WEEB_TEST_REDIS_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new RedisConfig().redisTemplate(factory);
        userId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_000_000_000_000L);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(indexKey())));
        assertFalse(Boolean.TRUE.equals(redis.opsForSet().isMember("ws:online:users", userId)));
        assertFalse(Boolean.TRUE.equals(redis.opsForSet().isMember("chat:online:users", userId.toString())));
        ownedKeys.add(indexKey());
        ownedKeys.add("chat:session:" + userId);
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
    }

    @AfterEach
    void cleanOnlyOwnedKeysAndSetMembers() {
        try {
            if (redis != null && userId != null) {
                redis.delete(ownedKeys);
                redis.opsForSet().remove("ws:online:users", userId);
                redis.opsForSet().remove("chat:online:users", userId.toString());
            }
        } finally {
            if (factory != null) factory.destroy();
        }
    }

    @Test
    void heartbeatRenewsBothLeasesAndExistingChatPresence() {
        String session = register();
        redis.expire(hashKey(session), 2, TimeUnit.SECONDS);
        redis.expire(indexKey(), 2, TimeUnit.SECONDS);
        redis.opsForHash().put(hashKey(session), "lastHeartbeat", System.currentTimeMillis() - 95_000);

        service.updateHeartbeat(session);

        assertTrue(service.isConnectionAlive(session));
        assertTrue(redis.getExpire(hashKey(session)) > 100);
        assertTrue(redis.getExpire(indexKey()) > 100);
        assertTrue(redis.getExpire(ownerKey(session)) > 300);
        assertEquals(Set.of(session), service.getUserActiveSessions(userId));
        assertBothPresence(true);
    }

    @Test
    void firstTabDisconnectKeepsOtherTabAndLastDisconnectClearsBothPresenceIndexes() {
        String first = register();
        String second = register();

        service.unregisterConnection(first);

        assertFalse(Boolean.TRUE.equals(redis.hasKey(hashKey(first))));
        assertEquals(Set.of(second), service.getUserActiveSessions(userId));
        assertEquals(second, redis.opsForValue().get("chat:session:" + userId));
        assertBothPresence(true);

        service.unregisterConnection(second);
        service.unregisterConnection(second); // Duplicate disconnect is harmless.

        assertBothPresence(false);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(indexKey())));
        assertFalse(Boolean.TRUE.equals(redis.hasKey("chat:session:" + userId)));
    }

    @Test
    void expiredHashDisconnectUsesSavedOwnerAndPreservesAnotherLiveTab() {
        String expired = register();
        String live = register();
        redis.delete(hashKey(expired));

        service.unregisterConnection(expired);

        assertEquals(Set.of(live), service.getUserActiveSessions(userId));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(ownerKey(expired))));
        assertBothPresence(true);
    }

    @Test
    void cleanupRemovesOnlineGhostWhenHashAndUserIndexAlreadyExpired() {
        String session = register();
        redis.delete(Set.of(hashKey(session), indexKey()));
        assertBothPresence(true);

        sweepOwnUser();

        assertBothPresence(false);
        assertFalse(Boolean.TRUE.equals(redis.hasKey("chat:session:" + userId)));
    }

    @Test
    void cleanupAlsoRemovesLegacyChatOnlyPresenceWithoutSession() {
        redis.opsForSet().add("chat:online:users", userId.toString());
        sweepOwnUser();
        assertBothPresence(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cleanupRechecksTimestampAfterConcurrentHeartbeatWins() {
        String session = register();
        redis.opsForHash().put(hashKey(session), "lastHeartbeat", System.currentTimeMillis() - 95_000);
        assertFalse(service.isConnectionAlive(session));
        RedisTemplate<String, Object> racingRedis = spy(redis);
        AtomicBoolean interleave = new AtomicBoolean(true);
        doAnswer(call -> {
            if (interleave.compareAndSet(true, false)) service.updateHeartbeat(session);
            return call.callRealMethod();
        }).when(racingRedis).execute(any(RedisCallback.class));
        ReflectionTestUtils.setField(service, "redisTemplate", racingRedis);

        sweepOwnUser();

        assertTrue(service.isConnectionAlive(session));
        assertEquals(Set.of(session), service.getUserActiveSessions(userId));
        assertBothPresence(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void heartbeatCannotRecreateSessionAfterConcurrentDisconnectWins() {
        String session = register();
        RedisTemplate<String, Object> racingRedis = spy(redis);
        AtomicBoolean interleave = new AtomicBoolean(true);
        doAnswer(call -> {
            if (interleave.compareAndSet(true, false)) service.unregisterConnection(session);
            return call.callRealMethod();
        }).when(racingRedis).execute(any(RedisCallback.class));
        ReflectionTestUtils.setField(service, "redisTemplate", racingRedis);

        service.updateHeartbeat(session);

        assertFalse(Boolean.TRUE.equals(redis.hasKey(hashKey(session))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(ownerKey(session))));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(indexKey())));
        assertBothPresence(false);
    }

    private String register() {
        String session = "presence-fixture-" + UUID.randomUUID();
        ownedKeys.add(hashKey(session));
        ownedKeys.add(ownerKey(session));
        service.registerConnection(session, userId, "presence_audit_" + userId);
        assertTrue(Boolean.TRUE.equals(redis.hasKey(hashKey(session))));
        return session;
    }

    private String hashKey(String session) { return "ws:session:" + session; }
    private String ownerKey(String session) { return "ws:session:owner:" + session; }
    private String indexKey() { return "ws:user:sessions:" + userId; }

    private void sweepOwnUser() {
        // Exercise the same atomic operation as the scheduled sweep without reconciling other test users.
        ReflectionTestUtils.invokeMethod(service, "reconcileUser", userId, (String) null);
    }

    private void assertBothPresence(boolean online) {
        assertEquals(online, service.isUserOnline(userId));
        assertEquals(online, Boolean.TRUE.equals(redis.opsForSet().isMember("chat:online:users", userId.toString())));
    }
}
