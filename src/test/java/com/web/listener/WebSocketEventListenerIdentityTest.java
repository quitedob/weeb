package com.web.listener;

import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.WebSocketConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class WebSocketEventListenerIdentityTest {
    private final WebSocketEventListener listener = new WebSocketEventListener();
    private final WebSocketConnectionService connections = mock(WebSocketConnectionService.class);
    private final AuthService auth = mock(AuthService.class);

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(listener, "connectionService", connections);
        ReflectionTestUtils.setField(listener, "authService", auth);
    }

    @AfterEach
    void clearThreadContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"audit_socket_user", "123456"})
    void resolvesAuthenticatedEventUsernameToPersistedIdWithoutThreadAuthentication(String username) {
        User user = enabledUser(username);
        when(auth.findByUsername(username)).thenReturn(user);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        listener.handleWebSocketConnectListener(connected(
                new UsernamePasswordAuthenticationToken(username, null, List.of())));

        verify(connections).registerConnection("socket-session", 91L, username);
        verifyNoMoreInteractions(connections);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled", "null-id", "zero-id", "negative-id", "mismatched-name"})
    void unavailableOrInvalidPersistedIdentityIsNeverRegistered(String state) {
        User user = enabledUser("audit_socket_user");
        switch (state) {
            case "missing" -> user = null;
            case "disabled" -> user.setStatus(0);
            case "null-id" -> user.setId(null);
            case "zero-id" -> user.setId(0L);
            case "negative-id" -> user.setId(-1L);
            case "mismatched-name" -> user.setUsername("different_user");
        }
        when(auth.findByUsername("audit_socket_user")).thenReturn(user);

        listener.handleWebSocketConnectListener(connected(
                new UsernamePasswordAuthenticationToken("audit_socket_user", null, List.of())));

        verifyNoInteractions(connections);
    }

    @Test
    void missingPrincipalCannotCreateAnonymousPresence() {
        listener.handleWebSocketConnectListener(connected(null));
        verifyNoInteractions(auth, connections);
    }

    private SessionConnectedEvent connected(Principal principal) {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECTED);
        headers.setSessionId("socket-session");
        // The broker event carries its authenticated principal independently of request thread state.
        return new SessionConnectedEvent(this,
                MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), principal);
    }

    private User enabledUser(String username) {
        User user = new User();
        user.setId(91L);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
