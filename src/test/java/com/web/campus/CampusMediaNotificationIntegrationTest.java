package com.web.campus;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.mapper.NotificationMapper;
import com.web.service.UserPreferencesService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real MySQL + filesystem + transaction synchronizations; only the transport is substituted. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
class CampusMediaNotificationIntegrationTest {
    @TempDir Path directory;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private CampusMediaService media;
    private CampusNotificationService notifications;
    private NotificationMapper inbox;
    private SimpMessagingTemplate socket;
    private UserPreferencesService preferences;
    private long admin, author, member, outsider, school, otherSchool, post, otherPost;

    @BeforeEach void fixture() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        var manager = new DataSourceTransactionManager(dataSource);
        tx = new TransactionTemplate(manager);
        var access = new CampusAccessService(jdbc);
        var factory = new ProxyFactory(new CampusMediaService(jdbc, access, manager, directory.toString()));
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        media = (CampusMediaService) factory.getProxy();
        socket = mock(SimpMessagingTemplate.class);
        preferences = mock(UserPreferencesService.class);
        when(preferences.isNotificationEnabled(anyLong(), anyString())).thenReturn(true);
        var beans = new StaticListableBeanFactory(Map.of("messaging", socket));
        notifications = new CampusNotificationService(jdbc, access, preferences, beans.getBeanProvider(SimpMessagingTemplate.class), manager);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var sqlFactory = new MybatisSqlSessionFactoryBean();
        sqlFactory.setConfiguration(configuration); sqlFactory.setDataSource(dataSource);
        sqlFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/NotificationMapper.xml"));
        inbox = new SqlSessionTemplate(Objects.requireNonNull(sqlFactory.getObject())).getMapper(NotificationMapper.class);
        admin = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        author = admin + 1; member = admin + 2; outsider = admin + 3;
        school = admin + 10; otherSchool = admin + 11; post = admin + 20; otherPost = admin + 21;
        for (long id : List.of(admin, author, member, outsider)) {
            jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (?,?,?,?,'USER',1)",
                    id, "campus_media_" + id, "fixture-hash", "campus_media_" + id + "@example.invalid");
        }
        for (long id : List.of(school, otherSchool)) jdbc.update("INSERT INTO campus_school(id,name,created_by) VALUES (?,?,?)", id, "Media fixture " + id, admin);
        for (long id : List.of(admin, author, member)) jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED',?)", school, id, id == admin ? "ADMIN" : "MEMBER");
        jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED','MEMBER')", otherSchool, outsider);
        for (long id : List.of(post, otherPost)) jdbc.update("INSERT INTO campus_post(id,school_id,author_id,title,content,status) VALUES (?,?,?,'Fixture','Private campus body','PUBLISHED')", id, school, author);
    }

    @AfterEach void cleanupOwnedRows() {
        if (notifications != null) notifications.close();
        if (jdbc == null || school == 0) return;
        for (String table : List.of("campus_media", "campus_audit", "campus_report", "campus_verification_application", "campus_membership")) {
            jdbc.update("DELETE FROM " + table + " WHERE school_id IN (?,?)", school, otherSchool);
        }
        jdbc.update("DELETE FROM notifications WHERE recipient_id IN (?,?,?,?)", admin, author, member, outsider);
        jdbc.update("DELETE FROM campus_post WHERE id IN (?,?)", post, otherPost);
        jdbc.update("DELETE FROM campus_school WHERE id IN (?,?)", school, otherSchool);
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?,?,?)", admin, author, member, outsider);
    }

    @Test void privatePixelsFollowCurrentMembershipAndPostState() throws Exception {
        Map<String, Object> uploaded = media.upload(author, school, png(3, 2));
        String id = uploaded.get("id").toString();
        assertEquals(3, ImageIO.read(new java.io.ByteArrayInputStream(media.read(author, id))).getWidth());
        denied(403, () -> media.read(member, id));
        denied(403, () -> media.read(outsider, id));
        attach(post, id);
        assertEquals(2, ImageIO.read(new java.io.ByteArrayInputStream(media.read(member, id))).getHeight());
        jdbc.update("UPDATE campus_membership SET status='SUSPENDED' WHERE school_id=? AND user_id=?", school, member);
        denied(403, () -> media.read(member, id));
        jdbc.update("UPDATE campus_post SET status='PENDING' WHERE id=?", post);
        assertTrue(media.read(author, id).length > 0);
        assertTrue(media.read(admin, id).length > 0);
        jdbc.update("UPDATE campus_post SET status='REMOVED' WHERE id=?", post);
        denied(404, () -> media.read(author, id));
        denied(404, () -> media.read(admin, id));
    }

    @Test void attachmentFailuresRollbackAndCannotStealAnotherUpload() throws Exception {
        String own = media.upload(author, school, png(2, 2)).get("id").toString();
        String theirs = media.upload(member, school, png(2, 2)).get("id").toString();
        attach(post, own);
        denied(400, () -> attach(otherPost, own));
        denied(400, () -> attach(post, own, theirs));
        assertEquals(List.of(own), media.listForPost(post).stream().map(m -> m.get("id")).toList());
        denied(409, () -> media.delete(author, own));
        attach(post);
        media.delete(author, own);
        assertFalse(Files.exists(directory.resolve(own + ".png")));
    }

    @Test void rollbackRemovesFileAndNoPublicFilenameOrMimeIsTrusted() throws Exception {
        long before;
        try (var files = Files.list(directory)) { before = files.count(); }
        tx.executeWithoutResult(status -> {
            try { media.upload(author, school, png(1, 1)); }
            catch (Exception failure) { throw new RuntimeException(failure); }
            status.setRollbackOnly();
        });
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE school_id=?", Long.class, school));
        try (var files = Files.list(directory)) { assertEquals(before, files.count()); }
        denied(400, () -> media.upload(author, school, new MockMultipartFile("file", "safe.png", "image/png", "<svg onload='alert(1)'/>".getBytes())));
        denied(400, () -> media.upload(author, school, png(4097, 1)));
        denied(400, () -> media.upload(author, school, new MockMultipartFile("file", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
        denied(400, () -> media.read(author, "../../file"));
    }

    @Test void quotasAndExpiredFilesAreRecoverableWhileAttachedImagesRemain() throws Exception {
        String attached = media.upload(author, school, png(1, 1)).get("id").toString();
        attach(post, attached);
        for (int i = 0; i < 12; i++) media.upload(author, school, png(1, 1));
        denied(409, () -> media.upload(author, school, png(1, 1)));
        jdbc.update("UPDATE campus_media SET detached_at=CURRENT_TIMESTAMP(3)-INTERVAL 25 HOUR WHERE school_id=? AND post_id IS NULL", school);
        String expired = jdbc.queryForObject("SELECT id FROM campus_media WHERE school_id=? AND post_id IS NULL LIMIT 1", String.class, school);
        denied(404, () -> media.read(author, expired));
        media.cleanup();
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE school_id=?", Long.class, school));
        assertTrue(Files.exists(directory.resolve(attached + ".png")));
        jdbc.update("UPDATE campus_post SET status='REMOVED',updated_at=CURRENT_TIMESTAMP(3)-INTERVAL 25 HOUR WHERE id=?", post);
        jdbc.update("UPDATE campus_school SET active=FALSE WHERE id=?", school);
        media.cleanup();
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE school_id=?", Long.class, school));
        assertFalse(Files.exists(directory.resolve(attached + ".png")));
    }

    @Test void notificationsCommitAtomicallyAndTransportFailureKeepsInbox() {
        tx.executeWithoutResult(status -> {
            notifications.send(member, author, "CAMPUS_COMMENT", "campus_post", post);
            verifyNoInteractions(socket);
            status.setRollbackOnly();
        });
        assertEquals(0, inbox.countTotalNotifications(author));
        verifyNoInteractions(socket);
        doThrow(new IllegalStateException("offline")).when(socket).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        tx.executeWithoutResult(status -> notifications.send(member, author, "CAMPUS_COMMENT", "campus_post", post));
        assertEquals(1, inbox.countTotalNotifications(author));
        assertEquals(1, inbox.countUnreadNotifications(author));
        verify(socket, timeout(2000)).convertAndSendToUser(eq("campus_media_" + author), eq("/queue/notifications"), argThat((Object payload) ->
                payload instanceof Map<?, ?> map && !map.containsKey("content") && !map.containsKey("realName") && map.get("entityId").equals(post)));
    }

    @Test void notificationListsCountsAndAfterCommitDeliveryUseCurrentPermission() {
        tx.executeWithoutResult(status -> notifications.send(author, member, "CAMPUS_LIKE", "campus_post", post));
        verify(socket, timeout(2000)).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        assertEquals(1, inbox.findNotificationsByRecipientId(member, 0, 20).size());
        reset(socket);
        tx.executeWithoutResult(status -> {
            notifications.send(author, member, "CAMPUS_COMMENT", "campus_post", post);
            jdbc.update("UPDATE campus_membership SET status='LEFT' WHERE school_id=? AND user_id=?", school, member);
        });
        notifications.close();
        verifyNoInteractions(socket);
        assertEquals(0, inbox.countTotalNotifications(member));
        assertEquals(0, inbox.countUnreadNotifications(member));
        assertTrue(inbox.findNotificationsByRecipientId(member, 0, 20).isEmpty());
        // Generic notices remain available; campus revocation must not remove unrelated inbox entries.
        jdbc.update("INSERT INTO notifications(recipient_id,actor_id,type,entity_type,entity_id) VALUES (?,?,'NEW_FOLLOWER','user',?)", member, author, author);
        assertEquals(1, inbox.countTotalNotifications(member));
        jdbc.update("UPDATE campus_membership SET status='VERIFIED' WHERE school_id=? AND user_id=?", school, member);
        assertEquals(3, inbox.countTotalNotifications(member));
        jdbc.update("UPDATE campus_post SET status='PENDING' WHERE id=?", post);
        assertEquals(1, inbox.countTotalNotifications(member));
        jdbc.update("UPDATE campus_post SET status='PUBLISHED' WHERE id=?", post);
        jdbc.update("UPDATE campus_school SET active=FALSE WHERE id=?", school);
        assertEquals(1, inbox.countTotalNotifications(member));
    }

    @Test void notificationPreferencesAndSelfActionsDoNotCreateInboxNoise() {
        when(preferences.isNotificationEnabled(member, "CAMPUS_LIKE")).thenReturn(false);
        tx.executeWithoutResult(status -> {
            notifications.send(author, member, "CAMPUS_LIKE", "campus_post", post);
            notifications.send(author, author, "CAMPUS_COMMENT", "campus_post", post);
        });
        assertEquals(0, inbox.countTotalNotifications(member));
        assertEquals(0, inbox.countTotalNotifications(author));
        verifyNoInteractions(socket);
    }

    @Test void orphanCleanupAdvancesPastAFullBatchOfRecentFiles() throws Exception {
        for (int i = 0; i < 1000; i++) Files.write(directory.resolve(String.format("00000000-0000-0000-0000-%012x.png", i)), new byte[]{1});
        Path older = Files.write(directory.resolve("ffffffff-ffff-ffff-ffff-ffffffffffff.png"), new byte[]{1});
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minus(25, java.time.temporal.ChronoUnit.HOURS)));
        media.cleanup();
        assertTrue(Files.exists(older), "The first batch is bounded to 1000 recent files");
        media.cleanup();
        assertFalse(Files.exists(older), "The next batch must reach and reclaim the older orphan");
        try (var files = Files.list(directory)) { assertEquals(1000, files.count(), "Recent uploads remain available"); }
    }

    @Test void singleConnectionPoolDoesNotBlockTheCommittingRequest() {
        var config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(System.getenv("WEEB_TEST_MYSQL_URL"));
        config.setUsername(System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"));
        config.setPassword(System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        config.setMaximumPoolSize(1); config.setMinimumIdle(1); config.setConnectionTimeout(3000);
        try (var pool = new com.zaxxer.hikari.HikariDataSource(config)) {
            var pooledJdbc = new JdbcTemplate(pool);
            var manager = new DataSourceTransactionManager(pool);
            var pooled = new CampusNotificationService(pooledJdbc, new CampusAccessService(pooledJdbc), preferences,
                    new StaticListableBeanFactory(Map.of("messaging", socket)).getBeanProvider(SimpMessagingTemplate.class), manager);
            try {
                long started = System.nanoTime();
                new TransactionTemplate(manager).executeWithoutResult(status -> pooled.send(member, author, "CAMPUS_COMMENT", "campus_post", post));
                assertTrue(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1500,
                        "Commit must not wait for a second connection while holding the only connection");
                verify(socket, timeout(2000)).convertAndSendToUser(anyString(), anyString(), any(Object.class));
                assertEquals(1, inbox.countTotalNotifications(author));
            } finally { pooled.close(); }
        }
    }

    private void attach(long destination, String... ids) {
        tx.executeWithoutResult(status -> {
            jdbc.queryForList("SELECT id FROM campus_school WHERE id=? FOR UPDATE", school);
            media.replaceAttachments(author, school, destination, List.of(ids));
        });
    }
    private static MockMultipartFile png(int width, int height) throws Exception {
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", buffer);
        return new MockMultipartFile("file", "../../unexpected.html", "text/html", buffer.toByteArray());
    }
    private static void denied(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(CampusException.class, action).getStatus());
    }
}
