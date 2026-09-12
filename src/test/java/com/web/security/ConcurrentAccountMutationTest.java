package com.web.security;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.PasswordResetService;
import com.web.service.UserCreationService;
import com.web.service.impl.AuthServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConcurrentAccountMutationTest {
    @Test
    @SuppressWarnings("unchecked")
    void loginCompletingAfterBanCannotRestoreItsStaleEnabledStatus() throws Exception {
        var auth = mock(AuthMapper.class);
        var users = mock(UserMapper.class);
        var encoder = mock(PasswordEncoder.class);
        var jwt = mock(JwtUtil.class);
        var redis = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        var service = new AuthServiceImpl(auth, users, mock(UserCreationService.class), encoder, redis,
                jwt, mock(PasswordResetService.class));
        User snapshot = new User();
        snapshot.setId(1L);
        snapshot.setUsername("account");
        snapshot.setPassword("old-hash");
        snapshot.setStatus(1);
        AtomicInteger persistedStatus = new AtomicInteger(1);
        CountDownLatch credentialsRead = new CountDownLatch(1);
        CountDownLatch banCommitted = new CountDownLatch(1);
        when(auth.findByUsername("account")).thenReturn(snapshot);
        when(encoder.matches("password", "old-hash")).thenAnswer(call -> {
            credentialsRead.countDown();
            if (!banCommitted.await(5, TimeUnit.SECONDS)) throw new AssertionError("Ban did not complete");
            return true;
        });
        when(auth.updateUser(any())).thenAnswer(call -> {
            persistedStatus.set(((User) call.getArgument(0)).getStatus());
            return 1;
        });
        when(users.updateStatus(1L, 0)).thenAnswer(call -> { persistedStatus.set(0); return 1; });
        when(jwt.generateToken(1L, "account", "old-hash")).thenReturn("fixture-token");
        var executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> login = executor.submit(() -> service.login("account", "password"));
            assertTrue(credentialsRead.await(5, TimeUnit.SECONDS));
            users.updateStatus(1L, 0);
            banCommitted.countDown();
            assertEquals("fixture-token", login.get(5, TimeUnit.SECONDS));
            assertEquals(0, persistedStatus.get());
            verify(auth).updateLoginTime(eq(1L), any());
            verify(auth, never()).updateUser(any());
        } finally {
            banCommitted.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleVerifiedPasswordCannotOverwriteACompletedReset() {
        var users = mock(UserMapper.class);
        var jwt = mock(JwtUtil.class);
        var service = new AuthServiceImpl(mock(AuthMapper.class), users, mock(UserCreationService.class),
                mock(PasswordEncoder.class), mock(RedisTemplate.class), jwt, mock(PasswordResetService.class));
        AtomicReference<String> storedHash = new AtomicReference<>("reset-hash");
        when(users.compareAndSetPassword(eq(1L), anyString(), anyString())).thenAnswer(call ->
                storedHash.compareAndSet(call.getArgument(2), call.getArgument(1)) ? 1 : 0);
        assertFalse(service.changePassword(1L, "attacker-hash", "old-verified-hash"));
        assertEquals("reset-hash", storedHash.get());
        verifyNoInteractions(jwt);
        assertTrue(service.changePassword(1L, "next-hash", "reset-hash"));
        assertEquals("next-hash", storedHash.get());
        verify(jwt).blacklistAllUserTokens(1L);
        verify(users, never()).updatePassword(anyLong(), anyString());
    }
}
