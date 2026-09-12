package com.web.service;

import com.web.exception.WeebException;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.impl.AuthServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuccessfulLoginDayTest {
    @Test
    @SuppressWarnings("unchecked")
    void onlyAnEnabledUserWithVerifiedCredentialsRecordsLoginActivity() {
        AuthMapper auth = mock(AuthMapper.class);
        UserMapper users = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        JwtUtil jwt = mock(JwtUtil.class);
        var service = new AuthServiceImpl(auth, users, mock(UserCreationService.class), encoder, redis,
                jwt, mock(PasswordResetService.class));
        User user = new User();
        user.setId(123L);
        user.setUsername("leveluser");
        user.setPassword("fixture-hash");
        user.setStatus(1);
        when(auth.findByUsername("leveluser")).thenReturn(user);
        when(encoder.matches("correct-password", "fixture-hash")).thenReturn(true);
        when(jwt.generateToken(123L, "leveluser")).thenReturn("local-fixture-token");

        assertThrows(WeebException.class, () -> service.login("leveluser", "wrong-password"));
        verify(users, never()).recordSuccessfulLoginDay(anyLong());
        user.setStatus(0);
        assertThrows(WeebException.class, () -> service.login("leveluser", "correct-password"));
        verify(users, never()).recordSuccessfulLoginDay(anyLong());
        user.setStatus(1);
        assertEquals("local-fixture-token", service.login("leveluser", "correct-password"));
        verify(users, times(1)).recordSuccessfulLoginDay(123L);
    }
}
