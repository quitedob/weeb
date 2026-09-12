package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.exception.AuthStateUnavailableException;
import com.web.config.SpringWebSocketConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.AuthMapper;
import com.web.mapper.AuthTokenStateMapper;
import com.web.mapper.UserMapper;
import com.web.service.UserService;
import com.web.service.ChatAccessService;
import com.web.service.UserOnlineStatusService;
import com.web.service.impl.UserServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Durable authority and actual Redis faults, using only generated test users/keys in the audit services. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_REDIS_PORT", matches = "16379")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthRevocationMySqlRedisIntegrationTest {
    private static final String SECRET = "auth-ledger-test-only-signing-key-at-least-32-bytes";
    private final Set<String> ownedKeys = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean redisUnavailable = new AtomicBoolean();
    private final AtomicBoolean redisWriteUnavailable = new AtomicBoolean();
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private SqlSessionTemplate session;
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis, faultRedis;
    private JwtUtil jwt;
    private UserService users;
    private long userId;
    private String username, generationKey;

    @BeforeAll
    void productionMappersAndAuditConnections() throws Exception {
        var source = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        transactions = new DataSourceTransactionManager(source);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(AuthTokenStateMapper.class);
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setConfiguration(config);
        bean.setDataSource(source);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        session = new SqlSessionTemplate(java.util.Objects.requireNonNull(bean.getObject()));
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", 16379);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        faultRedis = spy(redis);
        ValueOperations<String, String> values = spy(redis.opsForValue());
        doAnswer(call -> {
            if (redisWriteUnavailable.get()) throw new RedisConnectionFailureException("Injected audit Redis write failure");
            return call.callRealMethod();
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        doAnswer(call -> {
            if (redisUnavailable.get()) throw new RedisConnectionFailureException("Injected audit Redis outage");
            return values;
        }).when(faultRedis).opsForValue();
    }

    @BeforeEach
    void committedOwnedUser() {
        redisUnavailable.set(false);
        redisWriteUnavailable.set(false);
        userId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        username = "auth_state_" + userId;
        generationKey = "auth:session-generation:" + userId;
        ownedKeys.add(generationKey);
        jdbc.update("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                userId, username, "verified-original-hash", username + "@example.invalid", "USER");
        JwtUtil target = new JwtUtil(faultRedis, session.getMapper(AuthMapper.class), session.getMapper(AuthTokenStateMapper.class));
        ReflectionTestUtils.setField(target, "secret", SECRET);
        ReflectionTestUtils.setField(target, "expiration", 60_000L);
        target.init();
        jwt = transactional(target, JwtUtil.class);
        UserServiceImpl userTarget = new UserServiceImpl();
        ReflectionTestUtils.setField(userTarget, "userMapper", session.getMapper(UserMapper.class));
        ReflectionTestUtils.setField(userTarget, "jwtUtil", jwt);
        users = transactional(userTarget, UserService.class);
    }

    @AfterEach
    void removeOnlyOwnedState() {
        redisUnavailable.set(false);
        redisWriteUnavailable.set(false);
        if (userId != 0) jdbc.update("DELETE FROM `user` WHERE id=?", userId);
        if (!ownedKeys.isEmpty()) redis.delete(ownedKeys);
        ownedKeys.clear();
    }

    @AfterAll
    void closeOwnedConnection() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void exactLogoutSurvivesPartialCacheLossAndFreshInstancesWithoutRevokingOtherSessions() {
        String old = login();
        String other = login();
        jwt.blacklistToken(old);
        String jti = jwt.parseToken(old).getId();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM auth_token_revocation WHERE jti=?", Integer.class, jti));
        redis.delete("auth:revoked:" + jti); // Only this test token's cache entry is lost.
        assertFalse(jwt.validateToken(old));
        assertTrue(jwt.validateToken(other));
        JwtUtil another = new JwtUtil(redis, session.getMapper(AuthMapper.class), session.getMapper(AuthTokenStateMapper.class));
        ReflectionTestUtils.setField(another, "secret", SECRET);
        ReflectionTestUtils.setField(another, "expiration", 60_000L);
        another.init();
        assertFalse(another.validateToken(old));
        assertTrue(another.validateToken(other));
    }

    @Test
    void banThenUnbanCannotReviveOldTokenEvenWhenRedisRestoresOldGeneration() {
        String old = login();
        String snapshot = redis.opsForValue().get(generationKey);
        assertTrue(users.banUser(userId));
        assertFalse(jwt.validateToken(old));
        assertTrue(users.unbanUser(userId));
        redis.opsForValue().set(generationKey, snapshot); // Simulate restoring only our old cached session state.
        assertFalse(jwt.validateToken(old));
        assertThrows(AccessDeniedException.class, () -> jwt.renewToken(old));
        assertTrue(jwt.validateToken(login()));
        assertFalse(jwt.validateToken(old));
    }

    @Test
    void emptyCacheFailsClosedAndCredentialLoginRepairsItWithoutRevivingLoggedOutToken() {
        String old = login();
        String active = login();
        jwt.blacklistToken(old);
        redis.delete(Set.of(generationKey, "auth:revoked:" + jwt.parseToken(old).getId()));
        assertFalse(jwt.validateToken(active));
        assertFalse(jwt.validateToken(old));
        assertTrue(jwt.validateToken(login()));
        assertFalse(jwt.validateToken(old));
    }

    @Test
    void concurrentBearerRenewalHasExactlyOneDurableWinner() throws Exception {
        String old = login();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<String> renew = () -> { start.await(); return track(jwt.renewToken(old)); };
            var first = executor.submit(renew);
            var second = executor.submit(renew);
            start.countDown();
            int successes = 0, denials = 0;
            for (Future<String> result : java.util.List.of(first, second)) {
                try {
                    String token = result.get(10, TimeUnit.SECONDS);
                    assertTrue(jwt.validateToken(token));
                    successes++;
                } catch (ExecutionException failure) {
                    assertInstanceOf(AccessDeniedException.class, failure.getCause());
                    denials++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, denials);
            assertFalse(jwt.validateToken(old));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM auth_token_revocation WHERE user_id=?", Integer.class, userId));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void tokenExpiringWhileRenewalWaitsForUserLockCannotBeReissued() throws Exception {
        JwtUtil original = AopTestUtils.getUltimateTargetObject(jwt);
        ReflectionTestUtils.setField(original, "expiration", 3_000L);
        String old = login();
        long expiresAt = jwt.parseToken(old).getExpiration().getTime();
        var parsed = new CountDownLatch(1);
        JwtUtil observed = spy(original);
        doAnswer(call -> {
            var claims = call.callRealMethod();
            parsed.countDown();
            return claims;
        }).when(observed).parseToken(old);
        JwtUtil renewing = transactional(observed, JwtUtil.class);
        var executor = Executors.newSingleThreadExecutor();
        try (var blocker = java.util.Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            blocker.setAutoCommit(false);
            try (var lock = blocker.prepareStatement("SELECT id FROM `user` WHERE id=? FOR UPDATE")) {
                lock.setLong(1, userId);
                try (var row = lock.executeQuery()) { assertTrue(row.next()); }
            }
            Future<String> result = executor.submit(() -> renewing.renewToken(old));
            assertTrue(parsed.await(5, TimeUnit.SECONDS), "Token must be parsed before expiry and lock acquisition");
            assertFalse(result.isDone(), "Renewal must be waiting behind the actual user row lock");
            while (System.currentTimeMillis() <= expiresAt) Thread.sleep(25);
            blocker.commit();
            ExecutionException denied = assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
            assertInstanceOf(AccessDeniedException.class, denied.getCause());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_token_revocation WHERE user_id=?", Integer.class, userId));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void renewalCannotCrossCompletedAccountWideRevocationOrPasswordChange() {
        String old = login();
        assertTrue(jwt.validateToken(old));
        jwt.blacklistAllUserTokens(userId);
        assertThrows(AccessDeniedException.class, () -> jwt.renewToken(old));
        String beforeReset = login();
        jdbc.update("UPDATE `user` SET password=? WHERE id=?", "reset-completed-hash", userId);
        assertThrows(AccessDeniedException.class, () -> jwt.renewToken(beforeReset));
        assertThrows(IllegalArgumentException.class,
                () -> jwt.generateToken(userId, username, "verified-original-hash"));
        assertFalse(jwt.validateToken(beforeReset));
        assertTrue(jwt.validateToken(track(jwt.generateToken(userId, username, "reset-completed-hash"))));
    }

    @Test
    void failedRedisWriteRollsBackRenewalConsumptionAndRecoveryAllowsRetry() {
        String old = login();
        redisUnavailable.set(true);
        assertThrows(AuthStateUnavailableException.class, () -> jwt.validateToken(old));
        redisUnavailable.set(false);
        redisWriteUnavailable.set(true);
        assertThrows(AuthStateUnavailableException.class, () -> jwt.renewToken(old));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_token_revocation WHERE user_id=?", Integer.class, userId));
        redisWriteUnavailable.set(false);
        assertTrue(jwt.validateToken(old));
        assertTrue(jwt.validateToken(track(jwt.renewToken(old))));
        assertFalse(jwt.validateToken(old));
    }

    @Test
    void failedLogoutCannotReportSuccessOrLeaveAnUncommittedDurableRevocation() {
        String old = login();
        redisWriteUnavailable.set(true);
        assertThrows(AuthStateUnavailableException.class, () -> jwt.blacklistToken(old));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_token_revocation WHERE user_id=?", Integer.class, userId));
        redisWriteUnavailable.set(false);
        assertTrue(jwt.validateToken(old));
        jwt.blacklistToken(old);
        assertFalse(jwt.validateToken(old));
    }

    @ParameterizedTest
    @ValueSource(strings = {"logout", "ban", "expiry", "outage"})
    void existingStompSubscriberUsesActualTokenAndDurableStateAtEveryDelivery(String change) throws Exception {
        if ("expiry".equals(change)) ReflectionTestUtils.setField((Object) AopTestUtils.getUltimateTargetObject(jwt), "expiration", 3_000L);
        String token = login();
        SocketFixture socket = socket(token);
        assertSame(socket.delivery(), socket.outbound().preSend(socket.delivery(), null));
        if ("outage".equals(change)) {
            redisUnavailable.set(true);
            assertNull(socket.outbound().preSend(socket.delivery(), null));
            assertThrows(AuthStateUnavailableException.class, () -> socket.inbound().preSend(socket.subscribe(), null));
            redisUnavailable.set(false);
            assertSame(socket.delivery(), socket.outbound().preSend(socket.delivery(), null));
            return;
        }
        if ("logout".equals(change)) {
            jwt.blacklistToken(token);
            redis.delete("auth:revoked:" + jwt.parseToken(token).getId());
        } else if ("ban".equals(change)) {
            String oldGeneration = redis.opsForValue().get(generationKey);
            users.banUser(userId);
            users.unbanUser(userId);
            redis.opsForValue().set(generationKey, oldGeneration);
        } else {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7);
            while (!jwt.isTokenExpired(token) && System.nanoTime() < deadline) Thread.sleep(25);
            assertTrue(jwt.isTokenExpired(token));
        }
        assertNull(socket.outbound().preSend(socket.delivery(), null), "A passive subscriber must receive no protected payload");
        assertThrows(AccessDeniedException.class, () -> socket.inbound().preSend(socket.subscribe(), null));
        var send = StompHeaderAccessor.create(StompCommand.SEND);
        send.setSessionId("auth-integration-" + userId);
        send.setSessionAttributes(socket.attributes());
        send.setUser(() -> username);
        send.setDestination("/app/chat/heartbeat");
        send.setLeaveMutable(true);
        assertThrows(AccessDeniedException.class, () -> socket.inbound().preSend(MessageBuilder.createMessage(new byte[0], send.getMessageHeaders()), null));
        assertThrows(AccessDeniedException.class, () -> socket(token));
    }

    private SocketFixture socket(String token) throws Exception {
        SpringWebSocketConfig config = new SpringWebSocketConfig();
        ReflectionTestUtils.setField(config, "jwtUtil", jwt);
        ReflectionTestUtils.setField(config, "userMapper", session.getMapper(UserMapper.class));
        ReflectionTestUtils.setField(config, "onlineStatusService", mock(UserOnlineStatusService.class));
        ReflectionTestUtils.setField(config, "chatAccessService", mock(ChatAccessService.class));
        ReflectionTestUtils.setField(config, "objectMapper", new ObjectMapper());
        ChannelInterceptor inbound = interceptor(config, "WebSocketAuthInterceptor");
        ChannelInterceptor outbound = interceptor(config, "WebSocketDeliveryInterceptor");
        String sessionId = "auth-integration-" + userId;
        Map<String, Object> attributes = new HashMap<>();
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setSessionId(sessionId);
        connect.setSessionAttributes(attributes);
        connect.setNativeHeader("Authorization", "Bearer " + token);
        connect.setLeaveMutable(true);
        inbound.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), null);
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setSessionId(sessionId);
        subscribe.setSessionAttributes(attributes);
        subscribe.setUser(connect.getUser());
        subscribe.setDestination("/user/queue/notifications");
        subscribe.setSubscriptionId("notifications");
        subscribe.setLeaveMutable(true);
        Message<byte[]> subscription = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());
        inbound.preSend(subscription, null);
        var delivery = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        delivery.setSessionId(sessionId);
        delivery.setSubscriptionId("notifications");
        delivery.setDestination("/queue/notifications-user" + sessionId);
        return new SocketFixture(inbound, outbound, attributes, subscription,
                MessageBuilder.createMessage(new byte[] {'{', '}'}, delivery.getMessageHeaders()));
    }

    private ChannelInterceptor interceptor(SpringWebSocketConfig config, String name) throws Exception {
        var constructor = Class.forName(SpringWebSocketConfig.class.getName() + "$" + name)
                .getDeclaredConstructor(SpringWebSocketConfig.class);
        constructor.setAccessible(true);
        return (ChannelInterceptor) constructor.newInstance(config);
    }

    private record SocketFixture(ChannelInterceptor inbound, ChannelInterceptor outbound, Map<String, Object> attributes,
                                 Message<byte[]> subscribe, Message<byte[]> delivery) {}

    private String login() { return track(jwt.generateToken(userId, username, "verified-original-hash")); }

    private String track(String token) {
        ownedKeys.add("auth:revoked:" + jwt.parseToken(token).getId());
        return token;
    }

    private <T> T transactional(Object target, Class<T> type) {
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return type.cast(proxy.getProxy());
    }
}
