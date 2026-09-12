package com.web.security;

import com.web.mapper.AuthMapper;
import com.web.model.User;
import com.web.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtRevocationTest {
    private static final String SECRET = "test-only-signing-key-for-jwt-regressions-2026";
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private StringRedisTemplate redis;
    private ValueOperations<String, String> operations;
    private JwtUtil jwt;
    private User user;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        operations = mock(ValueOperations.class);
        AuthMapper users = mock(AuthMapper.class);
        user = new User();
        user.setId(1L); user.setUsername("evil_admin"); user.setStatus(1); user.setPassword("stored-password-hash");
        when(users.findByUserID(1L)).thenReturn(user);
        when(users.findByUsername(user.getUsername())).thenReturn(user);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        when(operations.setIfAbsent(anyString(), anyString())).thenAnswer(call -> values.putIfAbsent(call.getArgument(0), call.getArgument(1)) == null);
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(operations).set(anyString(), anyString());
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(operations).set(anyString(), anyString(), any(Duration.class));
        when(redis.hasKey(anyString())).thenAnswer(call -> values.containsKey(call.getArgument(0)));
        jwt = new JwtUtil(redis, users);
        ReflectionTestUtils.setField(jwt, "secret", SECRET);
        ReflectionTestUtils.setField(jwt, "expiration", 3600000L);
        jwt.init();
    }

    @Test void logoutRevokesOnlyThatDistinctTokenUntilItsExpiry() {
        String first = jwt.generateToken(1L, user.getUsername());
        String second = jwt.generateToken(1L, user.getUsername());
        assertNotEquals(first, second);
        assertTrue(jwt.validateToken(first));
        jwt.blacklistToken(first);
        assertFalse(jwt.validateToken(first));
        assertTrue(jwt.validateToken(second));
        verify(operations).set(startsWith("auth:revoked:"), eq("1"), argThat((Duration ttl) -> ttl.toMillis() > 0 && ttl.toMillis() <= 3600000));
    }

    @Test void revokeAllRejectsOldTokensButAllowsNewLogin() {
        String first = jwt.generateToken(1L, user.getUsername());
        String second = jwt.generateToken(1L, user.getUsername());
        jwt.blacklistAllUserTokens(user.getUsername());
        assertFalse(jwt.validateToken(first));
        assertFalse(jwt.validateToken(second));
        assertTrue(jwt.validateToken(jwt.generateToken(1L, user.getUsername())));
    }

    @Test void credentialChangeInvalidatesTokensEvenWhenLoginRacesRevocation() {
        jwt.blacklistAllUserTokens(1L);
        String racingLogin = jwt.generateToken(1L, user.getUsername());
        user.setPassword("changed-password-hash");
        assertFalse(jwt.validateToken(racingLogin));
        assertTrue(jwt.validateToken(jwt.generateToken(1L, user.getUsername())));
    }

    @Test void missingRedisStateAndRedisOutageFailClosed() {
        String token = jwt.generateToken(1L, user.getUsername());
        values.clear();
        assertFalse(jwt.validateToken(token));
        when(operations.get(anyString())).thenThrow(new IllegalStateException("offline"));
        assertFalse(jwt.validateToken(token));
    }

    @Test void signedResetAndLegacyTokensCannotAuthenticate() {
        String access = jwt.generateToken(1L, user.getUsername());
        var claims = jwt.parseToken(access);
        claims.put("purpose", "password_reset");
        String reset = Jwts.builder().setClaims(claims).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertFalse(jwt.validateToken(reset));
        String legacy = Jwts.builder().setSubject("1").claim("username", user.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + 60000)).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertFalse(jwt.validateToken(legacy));
        assertFalse(jwt.validateToken("opaque-reset-nonce"));
    }

    @Test void disabledUserCannotUsePreviouslyIssuedToken() {
        String token = jwt.generateToken(1L, user.getUsername());
        user.setStatus(0);
        assertFalse(jwt.validateToken(token));
    }
}
