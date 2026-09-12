package com.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.UserWithStats;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Published maintained contracts are checked against compiled Spring mappings and serialized DTOs. */
class PublishedContractTest {
    private final ObjectMapper json = new ObjectMapper();
    private JsonNode contract() throws Exception { return json.readTree(Path.of("docs/contracts.json").toFile()); }

    @Test
    void documentedHttpPathsAndFollowAliasesExistInCompiledControllers() throws Exception {
        Set<String> mappings = new HashSet<>();
        for (Class<?> controller : List.of(AuthController.class, ChatController.class, UserController.class,
                UserFollowController.class, SearchController.class, NotificationController.class, GroupController.class)) {
            RequestMapping prefix = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            for (var method : controller.getDeclaredMethods()) {
                RequestMapping route = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (route == null) continue;
                for (String base : paths(prefix)) for (String path : paths(route)) for (var verb : route.method()) {
                    mappings.add(normalize(verb.name() + " " + base + path));
                }
            }
        }
        JsonNode http = contract().path("http");
        for (String key : List.of("history", "send", "sync", "loadedMessageState", "read")) {
            assertTrue(mappings.contains(normalize(http.path(key).path("method").asText() + " " + http.path(key).path("path").asText())), key);
        }
        for (String key : List.of("create", "remove")) assertTrue(mappings.contains(normalize(http.path("follow").path(key).asText())), key);
        for (JsonNode alias : http.path("follow").path("aliases")) assertTrue(mappings.contains(normalize(alias.asText())), alias.asText());
        assertTrue(mappings.contains(normalize(http.path("auth").path("renew").asText())));
        for (String verb : List.of("PUT", "DELETE", "POST")) assertTrue(mappings.contains(normalize(verb + " " + http.path("reaction").path("path").asText())));
        for (String key : List.of("profile", "groupSearch", "messageSearch", "notifications")) assertTrue(mappings.contains(normalize("GET " + http.path(key).path("path").asText())), key);
        for (String key : List.of("joinedPath", "createdPath")) assertTrue(mappings.contains(normalize("GET " + http.path("groupLists").path(key).asText())), key);
    }

    @Test
    void documentedStompSendsExistAndProfileFieldsMatchSerialization() throws Exception {
        Set<String> mappings = new HashSet<>();
        for (var method : WebSocketMessageController.class.getDeclaredMethods()) {
            MessageMapping mapping = method.getAnnotation(MessageMapping.class);
            if (mapping != null) for (String value : mapping.value()) mappings.add("/app" + value);
        }
        for (JsonNode destination : contract().path("stomp").path("send")) assertTrue(mappings.contains(destination.asText()), destination.asText());
        JsonNode profile = json.valueToTree(new UserWithStats());
        for (JsonNode field : contract().path("http").path("profile").path("response")) assertTrue(profile.has(field.asText()), field.asText());
    }

    private static String[] paths(RequestMapping mapping) {
        return mapping == null || mapping.path().length == 0 ? new String[]{""} : mapping.path();
    }
    private static String normalize(String value) { return value.replaceAll("\\{[^}]+}", "{id}"); }
}
