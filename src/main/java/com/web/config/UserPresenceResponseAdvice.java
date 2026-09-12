package com.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web.common.ApiResponse;
import com.web.service.UserPreferencesService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashMap;
import java.util.Map;

/** Applies presence privacy consistently to nested profile, search and contact responses. */
@RestControllerAdvice(basePackages = "com.web.controller")
public class UserPresenceResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper;
    private final UserPreferencesService preferences;

    public UserPresenceResponseAdvice(ObjectMapper objectMapper, UserPreferencesService preferences) {
        this.objectMapper = objectMapper;
        this.preferences = preferences;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof ApiResponse<?>)) return body;
        JsonNode view = objectMapper.valueToTree(body);
        maskPresence(view, new HashMap<>());
        return view;
    }

    private void maskPresence(JsonNode node, Map<Long, Boolean> visibility) {
        if (node instanceof ObjectNode object && object.has("username") && object.has("id")
                && (object.has("onlineStatus") || object.has("loginTime") || object.has("online_status") || object.has("login_time"))) {
            long userId = object.path("id").asLong();
            if (userId > 0 && !visibility.computeIfAbsent(userId, preferences::isOnlineVisible)) {
                if (object.has("onlineStatus")) object.put("onlineStatus", 0);
                if (object.has("loginTime")) object.putNull("loginTime");
                if (object.has("online_status")) object.put("online_status", 0);
                if (object.has("login_time")) object.putNull("login_time");
            }
        }
        node.forEach(child -> maskPresence(child, visibility));
    }
}
