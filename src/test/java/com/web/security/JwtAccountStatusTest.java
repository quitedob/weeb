package com.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.util.JwtUtil;
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
