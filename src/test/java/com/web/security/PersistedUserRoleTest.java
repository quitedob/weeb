package com.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.service.impl.UserSecurityServiceImpl;
import com.web.service.impl.UserTypeSecurityServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistedUserRoleTest {
    @Test void namesCannotGrantRolesAndStoredRoleIsAuthoritative() {
        AuthMapper mapper = mock(AuthMapper.class);
        User user = new User();
        user.setId(1L); user.setUsername("evil_admin"); user.setType("USER"); user.setStatus(1);
        when(mapper.findByUsername(anyString())).thenReturn(user);
        when(mapper.findByUserID(1L)).thenReturn(user);
        var types = new UserTypeSecurityServiceImpl(mapper);
        var security = new UserSecurityServiceImpl(mock(UserMapper.class), mapper);
        for (String name : new String[]{"admin", "root", "system", "admin_a", "sys_a", "root_a", "evil_admin", "bot_test"}) {
            assertFalse(types.isAdmin(name));
            assertFalse(types.isBot(name));
        }
        assertEquals(java.util.List.of("ROLE_USER"), security.getUserAuthorities(1L));
        user.setUsername("ordinary_name"); user.setType("ADMIN");
        assertTrue(types.isAdmin(user.getUsername()));
        assertTrue(security.getUserAuthorities(1L).contains("ROLE_ADMIN"));
        user.setStatus(0);
        assertFalse(types.isAdmin(user.getUsername()));
        assertTrue(security.getUserAuthorities(1L).isEmpty());
    }

    @Test void passwordsAreNeverSerializedInDirectOrNestedResponses() throws Exception {
        User user = new User(); user.setUsername("test"); user.setPassword("private-hash");
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.writeValueAsString(user).contains("private-hash"));
        assertFalse(mapper.writeValueAsString(java.util.Map.of("user", user)).contains("password"));
        assertEquals("private-hash", user.getPassword());
    }
}
