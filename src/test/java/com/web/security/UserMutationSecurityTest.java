package com.web.security;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.User;
import com.web.service.PasswordResetService;
import com.web.service.RedisCacheService;
import com.web.service.UserCreationService;
import com.web.service.UserFollowService;
import com.web.service.impl.AuthServiceImpl;
import com.web.service.impl.UserServiceImpl;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserMutationSecurityTest {
    @org.junit.jupiter.api.AfterEach void clearRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test void publicRegistrationForcesUserEvenForExplicitAdminType() {
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
        User user = new User(); user.setUsername("evil_admin"); user.setType("ADMIN");
        user.setUserEmail("test@example.com"); user.setPassword("StrongPassword123!");
        UserCreationService creation = mock(UserCreationService.class);
        AuthServiceImpl auth = new AuthServiceImpl(mock(AuthMapper.class), mock(UserMapper.class), creation,
                new BCryptPasswordEncoder(4), mock(RedisTemplate.class), mock(JwtUtil.class), mock(PasswordResetService.class));
        auth.register(user);
        verify(creation).createUserWithDependencies(user);
        assertEquals("USER", user.getType());
        assertEquals(1, user.getStatus());
        assertEquals(0, user.getSex());
    }

    @Test void banAndPasswordChangeUseDedicatedSqlAndRevokeSessions() {
        User user = new User(); user.setId(2L); user.setUsername("target");
        UserMapper users = mock(UserMapper.class); JwtUtil jwt = mock(JwtUtil.class);
        when(users.selectById(2L)).thenReturn(user);
        when(users.updateStatus(2L, 0)).thenReturn(1);
        when(users.updatePassword(eq(2L), anyString())).thenReturn(1);
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "jwtUtil", jwt);
        ReflectionTestUtils.setField(service, "passwordEncoder", new BCryptPasswordEncoder(4));
        assertTrue(service.banUser(2L));
        assertTrue(service.resetUserPassword(2L, "NewPassword123!"));
        verify(jwt, times(2)).blacklistAllUserTokens(2L);
        verify(users, never()).updateUser(any());
        var hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(users).updatePassword(eq(2L), hash.capture());
        assertTrue(new BCryptPasswordEncoder().matches("NewPassword123!", hash.getValue()));
    }

    @Test void followAliasesUseRelationServiceInsteadOfInventingCounts() {
        UserFollowService follows = mock(UserFollowService.class);
        UserStatsMapper stats = mock(UserStatsMapper.class);
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userFollowService", follows);
        ReflectionTestUtils.setField(service, "userStatsMapper", stats);
        assertTrue(service.followUser(1L, 2L));
        assertTrue(service.unfollowUser(1L, 2L));
        verify(follows).followUser(1L, 2L);
        verify(follows).unfollowUser(1L, 2L);
        verifyNoInteractions(stats);
    }
}
