package com.web.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.annotation.Userid;
import com.web.controller.UserPreferencesController;
import com.web.exception.WeebException;
import com.web.mapper.UserPreferencesMapper;
import com.web.model.UserPreferences;
import com.web.vo.user.preferences.NotificationPreferencesVo;
import com.web.vo.user.preferences.PrivacyPreferencesVo;
import jakarta.validation.Validation;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserPreferencesServiceTest {
    private UserPreferencesMapper mapper;
    private UserPreferencesService service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserPreferencesMapper.class);
        service = new UserPreferencesService(mapper);
    }

    @Test
    void missingRowUsesEnabledDefaultsWithoutWritingToTheDatabase() {
        var preferences = service.get(7L);
        assertEquals(new PrivacyPreferencesVo(true, true, true), preferences.getPrivacy());
        assertEquals(new NotificationPreferencesVo(true, true, true, true, true), preferences.getNotifications());
        assertTrue(service.canReceiveMessages(7L));
        assertTrue(service.isOnlineVisible(7L));
        assertTrue(service.isFollowingVisible(7L));
        verify(mapper, never()).upsertPrivacy(anyLong(), any());
        verify(mapper, never()).upsertNotifications(anyLong(), any());
    }

    @Test
    void privacySaveReturnsPersistedValuesAndUsesOnlyThePrivacyStatement() {
        UserPreferences stored = new UserPreferences();
        stored.setOnlineVisible(false);
        stored.setAllowMessages(false);
        stored.setShowFollows(false);
        stored.setLikes(false);
        when(mapper.findByUserId(7L)).thenReturn(stored);

        PrivacyPreferencesVo requested = new PrivacyPreferencesVo(false, false, false);
        assertEquals(requested, service.savePrivacy(7L, requested));
        verify(mapper).upsertPrivacy(7L, requested);
        verify(mapper, never()).upsertNotifications(anyLong(), any());
        assertFalse(service.canReceiveMessages(7L));
        assertFalse(service.isOnlineVisible(7L));
        assertFalse(service.isFollowingVisible(7L));
        assertFalse(service.get(7L).getNotifications().getLikes());
    }

    @Test
    void notificationSaveReturnsPersistedValuesWithoutResettingPrivacy() {
        UserPreferences stored = new UserPreferences();
        stored.setAllowMessages(false);
        stored.setNewMessages(false);
        stored.setFollows(false);
        stored.setLikes(false);
        stored.setComments(false);
        stored.setGroupInvites(false);
        when(mapper.findByUserId(7L)).thenReturn(stored);

        NotificationPreferencesVo requested = new NotificationPreferencesVo(false, false, false, false, false);
        assertEquals(requested, service.saveNotifications(7L, requested));
        verify(mapper).upsertNotifications(7L, requested);
        verify(mapper, never()).upsertPrivacy(anyLong(), any());
        assertFalse(service.canReceiveMessages(7L));
    }

    @Test
    void rejectsMissingActorsAndIncompleteSectionsBeforeAnyWrite() {
        assertThrows(WeebException.class, () -> service.get(null));
        assertThrows(WeebException.class, () -> service.get(0L));
        assertThrows(WeebException.class, () -> service.savePrivacy(7L, null));
        assertThrows(WeebException.class,
                () -> service.savePrivacy(7L, new PrivacyPreferencesVo(true, null, true)));
        assertThrows(WeebException.class,
                () -> service.saveNotifications(7L, new NotificationPreferencesVo(true, true, true, true, null)));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "ARTICLE_LIKE", "follow", "NEW_FOLLOWER", "comment", "COMMENT_MENTION",
            "MESSAGE", "NEW_MESSAGE", "GROUP_INVITE", "GROUP_INVITATION", "GROUP_APPLICATION",
            "GROUP_APPLICATION_APPROVED", "GROUP_APPLICATION_REJECTED"})
    void recognizedNotificationTypesRespectDisabledPreferences(String type) {
        UserPreferences stored = new UserPreferences();
        stored.setNewMessages(false);
        stored.setFollows(false);
        stored.setLikes(false);
        stored.setComments(false);
        stored.setGroupInvites(false);
        when(mapper.findByUserId(7L)).thenReturn(stored);
        assertFalse(service.isNotificationEnabled(7L, type));
    }

    @Test
    void notificationCategoriesAreIndependentAndContactSynchronizationRemainsEnabled() {
        UserPreferences stored = new UserPreferences();
        stored.setLikes(false);
        stored.setGroupInvites(false);
        when(mapper.findByUserId(7L)).thenReturn(stored);
        assertFalse(service.isNotificationEnabled(7L, "like"));
        assertTrue(service.isNotificationEnabled(7L, "follow"));
        assertTrue(service.isNotificationEnabled(7L, "comment"));
        assertTrue(service.isNotificationEnabled(7L, "NEW_MESSAGE"));
        for (String type : new String[]{"FRIEND_REQUEST", "FRIEND_ACCEPTED", "CONTACT_UPDATED", "TEST_NOTIFICATION"}) {
            assertTrue(service.isNotificationEnabled(7L, type));
        }
        assertTrue(service.isNotificationEnabled(7L, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"false\"", "1", "0", "[]", "{}"})
    void rejectsValuesThatJacksonWouldOtherwiseCoerceIntoBooleans(String value) {
        var json = new ObjectMapper();
        assertThrows(JsonProcessingException.class, () -> json.readValue(
                "{\"onlineVisible\":" + value + ",\"allowMessages\":true,\"showFollows\":true}",
                PrivacyPreferencesVo.class));
    }

    @Test
    void dtoValidationRequiresEveryFieldAndAcceptsExplicitFalse() throws Exception {
        var json = new ObjectMapper();
        var supplied = json.readValue("{\"onlineVisible\":false,\"allowMessages\":false,\"showFollows\":false}",
                PrivacyPreferencesVo.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(supplied).isEmpty());
            assertEquals(3, validator.validate(new PrivacyPreferencesVo()).size());
            assertEquals(5, validator.validate(new NotificationPreferencesVo()).size());
        }
    }

    @Test
    void controllerUsesAuthenticatedActorAndRejectsBodyActorForgery() throws Exception {
        var preferences = mock(UserPreferencesService.class);
        when(preferences.savePrivacy(eq(7L), any())).thenReturn(new PrivacyPreferencesVo(false, true, false));
        var mvc = standaloneSetup(new UserPreferencesController(preferences))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(Userid.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory binder) {
                        return 7L;
                    }
                }).build();
        mvc.perform(put("/api/users/me/settings/privacy").contentType("application/json")
                .content("{\"onlineVisible\":false,\"allowMessages\":true,\"showFollows\":false}"))
                .andExpect(status().isOk());
        verify(preferences).savePrivacy(7L, new PrivacyPreferencesVo(false, true, false));

        clearInvocations(preferences);
        mvc.perform(put("/api/users/me/settings/privacy").contentType("application/json")
                .content("{\"userId\":9,\"onlineVisible\":false,\"allowMessages\":true,\"showFollows\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/me/settings/notifications").contentType("application/json")
                .content("{\"newMessages\":false}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(preferences);
    }

    @Nested
    @EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
    class MySqlPersistence {
        @Test
        void concurrentFirstSavesPreserveBothSectionsAndSubsequentSectionUpdates() throws Exception {
            var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                    System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                    System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
            long userId;
            try (var connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/create/01_create_user_table.sql"));
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/create/28_create_user_preferences_table.sql"));
                String fixtureName = "preferences_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                try (var insert = connection.prepareStatement(
                        "INSERT INTO `user`(username,password,user_email) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, fixtureName);
                    insert.setString(2, "test-hash");
                    insert.setString(3, fixtureName + "@example.invalid");
                    insert.executeUpdate();
                    try (var keys = insert.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        userId = keys.getLong(1);
                    }
                }
            }

            var executor = Executors.newFixedThreadPool(2);
            try {
                var configuration = new MybatisConfiguration();
                configuration.addMapper(UserPreferencesMapper.class);
                var bean = new MybatisSqlSessionFactoryBean();
                bean.setDataSource(dataSource);
                bean.setConfiguration(configuration);
                SqlSessionFactory factory = bean.getObject();
                assertNotNull(factory);
                var barrier = new CyclicBarrier(2);
                var privacy = new PrivacyPreferencesVo(false, false, false);
                var notifications = new NotificationPreferencesVo(false, true, false, true, false);
                var first = executor.submit(() -> {
                    try (var session = factory.openSession(true)) {
                        barrier.await(10, TimeUnit.SECONDS);
                        session.getMapper(UserPreferencesMapper.class).upsertPrivacy(userId, privacy);
                        return null;
                    }
                });
                var second = executor.submit(() -> {
                    try (var session = factory.openSession(true)) {
                        barrier.await(10, TimeUnit.SECONDS);
                        session.getMapper(UserPreferencesMapper.class).upsertNotifications(userId, notifications);
                        return null;
                    }
                });
                first.get(20, TimeUnit.SECONDS);
                second.get(20, TimeUnit.SECONDS);
                try (var session = factory.openSession(true)) {
                    var preferences = new UserPreferencesService(session.getMapper(UserPreferencesMapper.class));
                    assertEquals(privacy, preferences.get(userId).getPrivacy());
                    assertEquals(notifications, preferences.get(userId).getNotifications());
                    var updated = new PrivacyPreferencesVo(true, true, false);
                    assertEquals(updated, preferences.savePrivacy(userId, updated));
                    assertEquals(notifications, preferences.get(userId).getNotifications());
                }
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(20, TimeUnit.SECONDS);
                try (var connection = dataSource.getConnection();
                     var delete = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }
            }
        }
    }
}
