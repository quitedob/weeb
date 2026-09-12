package com.web.integration;

import com.web.exception.WeebException;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.PasswordResetDeliveryService;
import com.web.service.impl.PasswordResetServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Redis contracts; only random fixture keys are created and removed. Never flush the database. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_REDIS_PORT", matches = "16379")
@Timeout(20)
class RedisTokenIntegrationTest {
    private static final String TEST_SECRET = "local-integration-jwt-signing-key-for-audit-tests-only";
    private final Set<String> ownedKeys = new HashSet<>();
    private final AtomicReference<String> passwordHash = new AtomicReference<>();
    private final AtomicReference<String> deliveredToken = new AtomicReference<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private AuthMapper auth;
    private UserMapper users;
    private JwtUtil jwt;
    private PasswordResetServiceImpl reset;
    private Long userId;
    private String username;
    private String email;

    @BeforeEach
    void connectToExplicitDisposableRedis() throws Exception {
        int port = Integer.parseInt(System.getenv("WEEB_TEST_REDIS_PORT"));
        assertEquals(16379, port, "Only the isolated audit Redis port is allowed");
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        try (var connection = connectionFactory.getConnection()) {
            assertEquals("PONG", connection.ping());
        }

        userId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 9_000_000_000_000L);
        username = "redis_audit_" + userId;
        email = username + "@example.invalid";
        assertFalse(Boolean.TRUE.equals(redis.hasKey("auth:session-generation:" + userId)));
        assertFalse(Boolean.TRUE.equals(redis.hasKey("auth:password-reset-attempts:" + digest(email))));
        ownedKeys.add("auth:session-generation:" + userId);
        ownedKeys.add("auth:password-reset-attempts:" + digest(email));
        passwordHash.set(encoder.encode("OldPassword123!"));

        auth = mock(AuthMapper.class);
        users = mock(UserMapper.class);
        when(auth.findByUserID(userId)).thenAnswer(call -> userSnapshot());
        when(auth.findByUsername(username)).thenAnswer(call -> userSnapshot());
        when(auth.findByEmail(email)).thenAnswer(call -> userSnapshot());
        when(users.compareAndSetPassword(eq(userId), anyString(), anyString())).thenAnswer(call ->
                passwordHash.compareAndSet(call.getArgument(2), call.getArgument(1)) ? 1 : 0);
        jwt = createJwt(30_000L);

        PasswordResetDeliveryService delivery = mock(PasswordResetDeliveryService.class);
        when(delivery.isAvailable()).thenReturn(true);
        doAnswer(call -> {
            String token = call.getArgument(1);
            deliveredToken.set(token);
            ownedKeys.add(resetKey(token));
            return null;
        }).when(delivery).send(eq(email), anyString());
        reset = new PasswordResetServiceImpl(auth, users, jwt, encoder, redis, delivery);
    }

    @AfterEach
    void removeOnlyKnownFixtureKeys() {
        try {
            if (redis != null && !ownedKeys.isEmpty()) redis.delete(ownedKeys);
        } finally {
            if (connectionFactory != null) connectionFactory.destroy();
        }
    }

    @Test
    void logoutDenyListHasTokenTtlAndSurvivesAnotherJwtServiceInstance() {
        String first = jwt.generateToken(userId, username);
        String second = jwt.generateToken(userId, username);
        assertNotEquals(first, second);
        String revokedKey = "auth:revoked:" + jwt.parseToken(first).getId();
        ownedKeys.add(revokedKey);
        jwt.blacklistToken(first);

        Long ttl = redis.getExpire(revokedKey, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 30_000, "The deny-list entry must expire with its token");
        assertFalse(jwt.validateToken(first));
        JwtUtil anotherInstance = createJwt(30_000L);
        assertFalse(anotherInstance.validateToken(first));
        assertTrue(anotherInstance.validateToken(second));

        jwt.blacklistAllUserTokens(userId);
        assertFalse(anotherInstance.validateToken(second));
        assertTrue(anotherInstance.validateToken(anotherInstance.generateToken(userId, username)));
    }

    @Test
    void denyListExpiresNaturallyAndExpiredTokenRemainsInvalid() throws Exception {
        JwtUtil shortLived = createJwt(3_000L);
        String token = shortLived.generateToken(userId, username);
        String key = "auth:revoked:" + shortLived.parseToken(token).getId();
        ownedKeys.add(key);
        shortLived.blacklistToken(token);
        assertTrue(Boolean.TRUE.equals(redis.hasKey(key)));
        await(() -> !Boolean.TRUE.equals(redis.hasKey(key)), Duration.ofSeconds(6));
        assertTrue(shortLived.isTokenExpired(token));
        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void resetNonceHasFifteenMinuteTtlAndConcurrentGetDeleteAllowsOnePasswordWrite() throws Exception {
        String accessToken = jwt.generateToken(userId, username);
        var response = reset.sendPasswordResetLink(email);
        String token = deliveredToken.get();
        assertNotNull(token);
        assertFalse(response.toString().contains(token));
        assertFalse(jwt.validateToken(token));
        String key = resetKey(token);
        Long ttl = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > Duration.ofMinutes(14).toMillis() && ttl <= Duration.ofMinutes(15).toMillis());
        assertEquals(true, reset.validateResetToken(token).get("valid"));

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                assertTrue(start.await(3, TimeUnit.SECONDS));
                try {
                    return Boolean.TRUE.equals(reset.resetPassword(token, "NewPassword123!", "NewPassword123!").get("success"));
                } catch (WeebException alreadyConsumed) {
                    return false;
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS),
                    "Exactly one concurrent request may consume the reset nonce");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        }

        verify(users, times(1)).compareAndSetPassword(eq(userId), anyString(), anyString());
        assertTrue(encoder.matches("NewPassword123!", passwordHash.get()));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(key)));
        assertEquals(false, reset.validateResetToken(token).get("valid"));
        assertFalse(jwt.validateToken(accessToken));
        assertTrue(jwt.validateToken(jwt.generateToken(userId, username)));
    }

    @Test
    void expiredResetNonceCannotBeConsumedOrWriteTheDatabase() throws Exception {
        reset.sendPasswordResetLink(email);
        String token = deliveredToken.get();
        String key = resetKey(token);
        // Accelerate only this fixture's nonce expiration while exercising actual Redis expiry and GETDEL.
        assertTrue(Boolean.TRUE.equals(redis.expire(key, Duration.ofMillis(100))));
        await(() -> !Boolean.TRUE.equals(redis.hasKey(key)), Duration.ofSeconds(3));
        assertEquals(false, reset.validateResetToken(token).get("valid"));
        assertThrows(WeebException.class, () -> reset.resetPassword(token, "NewPassword123!", "NewPassword123!"));
        verifyNoInteractions(users);
    }

    private JwtUtil createJwt(long expiration) {
        JwtUtil instance = new JwtUtil(redis, auth);
        ReflectionTestUtils.setField(instance, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(instance, "expiration", expiration);
        instance.init();
        return instance;
    }

    private User userSnapshot() {
        User user = new User();
        user.setId(userId); user.setUsername(username); user.setUserEmail(email);
        user.setStatus(1); user.setType("USER"); user.setPassword(passwordHash.get());
        return user;
    }

    private static String resetKey(String token) throws Exception { return "auth:password-reset:" + digest(token); }

    private static String digest(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertTrue(condition.getAsBoolean(), "Redis did not expire the fixture within the deadline");
    }
}
