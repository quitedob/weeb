package com.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.util.JwtUtil;
import com.web.exception.AuthStateUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAccountStatusTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unavailableTokenStorageBlocksControllersWith503AndTheSameTokenWorksAfterRecovery() throws Exception {
        var users = mock(CustomUserDetailsService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.validateToken("access-token"))
                .thenThrow(new AuthStateUnavailableException(new org.springframework.data.redis.RedisConnectionFailureException("outage")))
                .thenReturn(true);
        when(jwt.getUserIdFromToken("access-token")).thenReturn(7L);
        when(jwt.extractUsername("access-token")).thenReturn("member");
        when(users.loadUserById(7L)).thenReturn(User.withUsername("member").password("hash").roles("USER").build());
        var filter = new JwtAuthenticationFilter(users, jwt, new ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        var blockedChain = new MockFilterChain();
        filter.doFilter(request, response, blockedChain);
        assertEquals(503, response.getStatus());
        assertNull(blockedChain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        var recoveredChain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), recoveredChain);
        assertSame(request, recoveredChain.getRequest());
        assertEquals("member", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void failedUserDetailsLookupIsAnOutageRatherThanAMissingAccount() throws Exception {
        var users = mock(CustomUserDetailsService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.validateToken("access-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("access-token")).thenReturn(7L);
        when(users.loadUserById(7L)).thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "lookup failed", new org.springframework.dao.DataAccessResourceFailureException("database unavailable")));
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new JwtAuthenticationFilter(users, jwt, new ObjectMapper()).doFilter(request, response, chain);
        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void disabledAccountCannotUseAnOtherwiseValidAccessToken() throws Exception {
        var users = mock(CustomUserDetailsService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.validateToken("access-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("access-token")).thenReturn(7L);
        when(users.loadUserById(7L)).thenReturn(User.withUsername("member")
                .password("hash").roles("USER").disabled(true).build());
        var filter = new JwtAuthenticationFilter(users, jwt, new ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest(), "Disabled account must never reach the controller");
        assertEquals(401, new ObjectMapper().readTree(response.getContentAsString()).get("code").asInt());
    }

    @Test
    void enabledAccountRetainsAuthenticatedIdentityAndUserId() throws Exception {
        var users = mock(CustomUserDetailsService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.validateToken("access-token")).thenReturn(true);
        when(jwt.getUserIdFromToken("access-token")).thenReturn(7L);
        when(jwt.extractUsername("access-token")).thenReturn("member");
        when(users.loadUserById(7L)).thenReturn(User.withUsername("member")
                .password("hash").roles("USER").build());
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        var chain = new MockFilterChain();

        new JwtAuthenticationFilter(users, jwt, new ObjectMapper())
                .doFilter(request, new MockHttpServletResponse(), chain);

        assertSame(request, chain.getRequest());
        assertEquals("member", SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(request.getAttribute("userinfo"));
    }
}
