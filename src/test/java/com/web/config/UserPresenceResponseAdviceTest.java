package com.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.common.ApiResponse;
import com.web.dto.UserDto;
import com.web.mapper.UserPreferencesMapper;
import com.web.model.User;
import com.web.model.UserPreferences;
import com.web.service.UserPreferencesService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPresenceResponseAdviceTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final UserPreferencesMapper preferences = mock(UserPreferencesMapper.class);
    private final UserPresenceResponseAdvice advice =
            new UserPresenceResponseAdvice(json, new UserPreferencesService(preferences));

    @Test
    void masksNestedUserAndDtoPresenceWithoutChangingCachedObjectsOrExposingPasswords() throws Exception {
        UserPreferences hidden = new UserPreferences();
        hidden.setOnlineVisible(false);
        when(preferences.findByUserId(7L)).thenReturn(hidden);
        User user = user(7L, "hidden");
        UserDto dto = new UserDto();
        dto.setId(7L);
        dto.setUsername("hidden");
        dto.setOnlineStatus(2);
        var envelope = ApiResponse.success(Map.of("profile", user, "results", List.of(Map.of("user", dto))));
        String cachedBefore = json.writeValueAsString(envelope);

        JsonNode response = serialize(envelope);

        assertEquals(0, response.path("code").intValue());
        JsonNode profile = response.path("data").path("profile");
        assertEquals(0, profile.path("onlineStatus").intValue());
        assertTrue(profile.path("loginTime").isNull());
        assertFalse(profile.has("password"));
        assertEquals("hidden", profile.path("username").textValue());
        JsonNode row = response.path("data").path("results").get(0).path("user");
        assertEquals(0, row.path("onlineStatus").intValue());
        assertFalse(row.has("loginTime"));
        assertEquals(cachedBefore, json.writeValueAsString(envelope));
        assertEquals(2, user.getOnlineStatus());
        assertEquals(2, dto.getOnlineStatus());
        assertEquals(new Date(123456789L), user.getLoginTime());
    }

    @Test
    void visibleAndDefaultProfilesKeepTheirPresenceWhenSerialized() throws Exception {
        UserPreferences visible = new UserPreferences();
        when(preferences.findByUserId(8L)).thenReturn(visible);
        User explicit = user(8L, "visible");
        User defaults = user(9L, "default");
        var envelope = ApiResponse.success(Map.of("users", List.of(explicit, defaults)));

        JsonNode response = serialize(envelope);

        for (JsonNode user : response.path("data").path("users")) {
            assertEquals(2, user.path("onlineStatus").intValue());
            assertEquals(123456789L, user.path("loginTime").longValue());
            assertFalse(user.has("password"));
        }
        // Compare the HTTP representation; valueToTree retains LongNode IDs while readTree may use IntNode.
        assertEquals(json.readTree(json.writeValueAsString(envelope)), response);
    }

    private JsonNode serialize(Object envelope) throws Exception {
        Object output = advice.beforeBodyWrite(envelope, null, MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, null, null);
        return json.readTree(json.writeValueAsString(output));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("private-password-hash");
        user.setOnlineStatus(2);
        user.setLoginTime(new Date(123456789L));
        return user;
    }
}
