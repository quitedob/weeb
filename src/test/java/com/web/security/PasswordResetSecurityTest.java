package com.web.security;

import com.web.exception.WeebException;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.PasswordResetDeliveryService;
import com.web.service.impl.PasswordResetServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetSecurityTest {
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final AtomicReference<String> deliveredToken = new AtomicReference<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PasswordResetServiceImpl service;
    private PasswordResetDeliveryService delivery;
    private AuthMapper auth;
    private UserMapper users;
    private JwtUtil jwt;
    private User user;
    private ValueOperations<String, String> operations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        operations = mock(ValueOperations.class);
        auth = mock(AuthMapper.class); users = mock(UserMapper.class); jwt = mock(JwtUtil.class);
        delivery = mock(PasswordResetDeliveryService.class);
        user = new User(); user.setId(1L); user.setStatus(1); user.setPassword(encoder.encode("OldPassword123!"));
        when(auth.findByEmail("user@example.com")).thenReturn(user);
        when(auth.findByUserID(1L)).thenReturn(user);
        when(delivery.isAvailable()).thenReturn(true);
        doAnswer(call -> { deliveredToken.set(call.getArgument(1)); return null; }).when(delivery).send(anyString(), anyString());
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        when(operations.getAndDelete(anyString())).thenAnswer(call -> values.remove(call.getArgument(0)));
        when(operations.increment(anyString())).thenReturn(1L);
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(operations).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(call -> values.remove(call.getArgument(0)) != null);
        when(users.compareAndSetPassword(eq(1L), anyString(), anyString())).thenAnswer(call -> {
            if (!user.getPassword().equals(call.getArgument(2))) return 0;
            user.setPassword(call.getArgument(1));
            return 1;
        });
        service = new PasswordResetServiceImpl(auth, users, jwt, encoder, redis, delivery);
    }

    @Test void deliveredNonceIsShortLivedSingleUseAndNeverReturned() {
        Map<String, Object> response = service.sendPasswordResetLink("user@example.com");
        String token = deliveredToken.get();
        assertNotNull(token);
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertFalse(response.toString().contains(token));
        assertTrue(values.keySet().stream().noneMatch(key -> key.contains(token)));
        verify(operations).set(startsWith("auth:password-reset:"), anyString(), eq(Duration.ofMinutes(15)));
        assertEquals(true, service.validateResetToken(token).get("valid"));
        assertEquals(true, service.resetPassword(token, "NewPassword123!", "NewPassword123!").get("success"));
        assertTrue(encoder.matches("NewPassword123!", user.getPassword()));
        verify(jwt).blacklistAllUserTokens(1L);
        assertFalse((boolean) service.validateResetToken(token).get("valid"));
        assertThrows(WeebException.class, () -> service.resetPassword(token, "AnotherPassword123!", "AnotherPassword123!"));
        verify(users, times(1)).compareAndSetPassword(eq(1L), anyString(), anyString());
    }

    @Test void ordinaryAccessTokenAndExpiredResetCannotChangePassword() {
        assertFalse((boolean) service.validateResetToken("eyJhbGciOiJIUzI1NiJ9.payload.signature").get("valid"));
        service.sendPasswordResetLink("user@example.com");
        String token = deliveredToken.get();
        values.clear();
        assertThrows(WeebException.class, () -> service.resetPassword(token, "NewPassword123!", "NewPassword123!"));
        verifyNoInteractions(users, jwt);
    }

    @Test void otherIssuedResetLinksBecomeInvalidWhenPasswordChanges() {
        service.sendPasswordResetLink("user@example.com");
        String old = deliveredToken.get();
        service.sendPasswordResetLink("user@example.com");
        String latest = deliveredToken.get();
        assertNotEquals(old, latest);
        service.resetPassword(latest, "NewPassword123!", "NewPassword123!");
        assertFalse((boolean) service.validateResetToken(old).get("valid"));
    }

    @Test void missingDeliveryDoesNotPretendSuccessOrDiscloseAccountExistence() {
        when(delivery.isAvailable()).thenReturn(false);
        assertThrows(WeebException.class, () -> service.sendPasswordResetLink("user@example.com"));
        assertThrows(WeebException.class, () -> service.sendPasswordResetLink("missing@example.com"));
        verifyNoInteractions(auth);
    }

    @Test void failedDeliveryDeletesCredentialWithoutExposingTransportException() {
        doThrow(new IllegalStateException("sensitive-message-body")).when(delivery).send(anyString(), anyString());
        WeebException error = assertThrows(WeebException.class, () -> service.sendPasswordResetLink("user@example.com"));
        assertFalse(error.getMessage().contains("sensitive-message-body"));
        assertNull(error.getCause());
        assertTrue(values.isEmpty());
    }
}
