package com.web.service;

import com.web.service.impl.WebSocketConnectionServiceImpl;
import com.web.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketConnectionIdentityTest {
    private static final String ONLINE_USERS = "ws:online:users";
    private final WebSocketConnectionServiceImpl service = new WebSocketConnectionServiceImpl();
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final SetOperations<String, Object> sets = mock(SetOperations.class);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "redisTemplate", redis);
    }

    @ParameterizedTest
    @MethodSource("invalidIdentities")
    void invalidRegistrationFailsBeforeAnyRedisAccess(String sessionId, Long userId, String username) {
        assertThrows(IllegalArgumentException.class, () -> service.registerConnection(sessionId, userId, username));
        verifyNoInteractions(redis);
    }

    private static Stream<Arguments> invalidIdentities() {
        return Stream.of(
                Arguments.of("session", null, "actor"),
                Arguments.of("session", 0L, "actor"),
                Arguments.of("session", -1L, "actor"),
                Arguments.of(null, 91L, "actor"),
                Arguments.of(" ", 91L, "actor"),
                Arguments.of("session", 91L, null),
                Arguments.of("session", 91L, " "));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validRegistrationWritesThePersistedUserIdentity() {
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        RedisTemplate<String, Object> configured = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        doReturn(configured.getKeySerializer()).when(redis).getKeySerializer();
        doReturn(configured.getValueSerializer()).when(redis).getValueSerializer();
        doReturn(configured.getHashValueSerializer()).when(redis).getHashValueSerializer();
        when(redis.execute(any(RedisCallback.class))).thenReturn(1L);

        service.registerConnection("session", 91L, "audit_actor");

        verify(hashes).putAll(eq("ws:session:session"), argThat(info ->
                Long.valueOf(91L).equals(info.get("userId")) && "audit_actor".equals(info.get("username"))));
        verify(sets).add(ONLINE_USERS, 91L);
        verify(sets).add("ws:user:sessions:91", "session");
        verify(redis).execute(any(RedisCallback.class));
    }

    @Test
    void validOnlineUsersSurviveAndLegacyInvalidEntriesAreRemovedIndividually() {
        when(redis.opsForSet()).thenReturn(sets);
        when(sets.members(ONLINE_USERS)).thenReturn(new LinkedHashSet<>(
                Arrays.asList(null, "", "broken", 0, -2L, 91L, "92", 93)));

        assertEquals(Set.of(91L, 92L, 93L), service.getOnlineUserIds());

        for (Object invalid : Arrays.asList(null, "", "broken", 0, -2L)) {
            verify(sets).remove(ONLINE_USERS, invalid);
        }
        verify(sets, never()).remove(ONLINE_USERS, 91L);
        verify(sets, never()).remove(ONLINE_USERS, "92");
        verify(sets, never()).remove(ONLINE_USERS, 93);
    }

    @Test
    void failedLegacyCleanupCannotDiscardValidOnlineUsers() {
        when(redis.opsForSet()).thenReturn(sets);
        when(sets.members(ONLINE_USERS)).thenReturn(new LinkedHashSet<>(Arrays.asList(null, 91L)));
        when(sets.remove(ONLINE_USERS, (Object) null)).thenThrow(new IllegalStateException("cleanup unavailable"));

        assertEquals(Set.of(91L), service.getOnlineUserIds());
    }
}
