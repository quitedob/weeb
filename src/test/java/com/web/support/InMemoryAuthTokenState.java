package com.web.support;

import com.web.mapper.AuthMapper;
import com.web.mapper.AuthTokenStateMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit/Redis-contract fixture only. Database transaction and recovery semantics have separate real-MySQL tests. */
public final class InMemoryAuthTokenState {
    private InMemoryAuthTokenState() {}

    public static AuthTokenStateMapper create(AuthMapper users) {
        AuthTokenStateMapper state = mock(AuthTokenStateMapper.class);
        Map<Long, String> generations = new ConcurrentHashMap<>();
        Set<String> revoked = ConcurrentHashMap.newKeySet();
        when(state.lockUser(anyLong())).thenAnswer(call -> users.findByUserID(call.getArgument(0)));
        when(state.selectGeneration(anyLong())).thenAnswer(call -> generations.get(call.getArgument(0)));
        when(state.lockGeneration(anyLong())).thenAnswer(call -> generations.get(call.getArgument(0)));
        when(state.createGeneration(anyLong(), anyString())).thenAnswer(call ->
                generations.putIfAbsent(call.getArgument(0), call.getArgument(1)) == null ? 1 : 0);
        when(state.replaceGeneration(anyLong(), anyString())).thenAnswer(call -> {
            generations.put(call.getArgument(0), call.getArgument(1));
            return 1;
        });
        when(state.isRevoked(anyString())).thenAnswer(call -> revoked.contains(call.getArgument(0)));
        when(state.revoke(anyString(), anyLong(), any())).thenAnswer(call -> revoked.add(call.getArgument(0)) ? 1 : 0);
        return state;
    }
}
