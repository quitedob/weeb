package com.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.exception.AuthStateUnavailableException;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthRenewalContractTest {
    private final AuthService auth = mock(AuthService.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private MockMvc mvc;

    @BeforeEach
    void controllerWithActualHttpSerialization() {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", auth);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwt);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void loginReturnsAbsoluteMillisecondsAndRemainingSecondsAsDifferentFields() throws Exception {
        long expiresAt = System.currentTimeMillis() + 90_000L;
        User user = new User(); user.setId(7L); user.setUsername("member");
        when(auth.login("member", "password")).thenReturn("login-token");
        when(auth.getUserInfo("login-token")).thenReturn(user);
        when(jwt.getExpirationTimeSingle("login-token")).thenReturn(expiresAt);
        var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.expiresAt").value(expiresAt)).andReturn();
        long seconds = new ObjectMapper().readTree(response.getResponse().getContentAsString()).path("data").path("expiresIn").asLong();
        assertTrue(seconds >= 88 && seconds <= 90, "expiresIn is a duration in seconds, not an epoch timestamp");
    }

    @Test
    void renewalUsesOneAtomicServiceOperationAndReturnsTheSameExpiryUnits() throws Exception {
        long expiresAt = System.currentTimeMillis() + 90_000L;
        when(jwt.renewToken("old-token")).thenReturn("renewed-token");
        when(jwt.getExpirationTimeSingle("renewed-token")).thenReturn(expiresAt);
        var response = mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer old-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").value("renewed-token"))
                .andExpect(jsonPath("$.data.expiresAt").value(expiresAt)).andReturn();
        long seconds = new ObjectMapper().readTree(response.getResponse().getContentAsString()).path("data").path("expiresIn").asLong();
        assertTrue(seconds >= 88 && seconds <= 90);
        verify(jwt).renewToken("old-token");
        verify(jwt, never()).blacklistToken(anyString());
        verifyNoInteractions(auth);
    }

    @Test
    void consumedOrExpiredRenewalGetsUnauthorizedWithoutReturningAToken() throws Exception {
        when(jwt.renewToken("old-token")).thenThrow(new AccessDeniedException("Token already consumed"));
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer old-token"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "refresh", "logout", "validate"})
    void authenticationStorageFailuresAreTemporary503Responses(String operation) throws Exception {
        var failure = new AuthStateUnavailableException(new RedisConnectionFailureException("Injected test outage"));
        switch (operation) {
            case "login" -> when(auth.login("member", "password")).thenThrow(failure);
            case "refresh" -> when(jwt.renewToken("old-token")).thenThrow(failure);
            case "logout" -> doThrow(failure).when(jwt).blacklistToken("old-token");
            case "validate" -> when(jwt.validateToken("old-token")).thenThrow(failure);
        }
        mvc.perform(post("/api/auth/" + operation).header("Authorization", "Bearer old-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"member\",\"password\":\"password\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }
}
