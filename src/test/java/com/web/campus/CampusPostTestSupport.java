package com.web.campus;

import com.web.migration.SchemaMigrator;
import com.web.service.UserPreferencesService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.web.campus.CampusPostDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Only random, explicitly owned fixture rows are committed; cleanup never touches unrelated records. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CampusPostTestSupport {
    @TempDir Path mediaDirectory;
    JdbcTemplate jdbc;
    DataSourceTransactionManager transactions;
    CampusAccessService access;
    CampusAuditService audit;
    CampusPostService posts;
    CampusMediaService media;
    CampusNotificationService notifications;
    SimpMessagingTemplate transport;
    long author, peer, manager, outsider, siteAdmin, secondManager, school, otherSchool;

    @BeforeAll
    void connectOnlyToDisposableAuditDatabase() throws Exception {
        String url = System.getenv("WEEB_TEST_MYSQL_URL");
        assertNotNull(url);
        assertTrue(url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):23306/weeb_audit(?:\\?.*)?"));
        var source = new DriverManagerDataSource(url, System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        new SchemaMigrator(source).migrate(false);
        jdbc = new JdbcTemplate(source);
        transactions = new DataSourceTransactionManager(source);
        access = new CampusAccessService(jdbc);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void createIndependentCommittedFixturesAndRealServiceProxy() {
        author = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        peer = author + 1; manager = author + 2; outsider = author + 3; siteAdmin = author + 4; secondManager = author + 5;
        school = author + 10; otherSchool = author + 11;
        for (long id : List.of(author, peer, manager, outsider, siteAdmin, secondManager)) {
            jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status,nickname) VALUES (?,?,?,?,?,1,?)",
                    id, "campus_post_" + id, "fixture-hash", "campus_post_" + id + "@example.invalid",
                    id == siteAdmin ? "ADMIN" : "USER", "Campus fixture " + id);
        }
        jdbc.update("INSERT INTO campus_school(id,name,description,created_by) VALUES (?,?,'private fixture',?),(?,?,'other fixture',?)",
                school, "campus post " + school, manager, otherSchool, "campus other " + otherSchool, outsider);
        for (long id : List.of(author, peer, manager, secondManager)) {
            jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED',?)",
                    school, id, id == manager || id == secondManager ? "ADMIN" : "MEMBER");
        }
        jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED','ADMIN')", otherSchool, outsider);
        transport = mock(SimpMessagingTemplate.class);
        ObjectProvider<SimpMessagingTemplate> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(transport);
        UserPreferencesService preferences = mock(UserPreferencesService.class);
        when(preferences.isNotificationEnabled(anyLong(), anyString())).thenReturn(true);
        notifications = new CampusNotificationService(jdbc, access, preferences, provider, transactions);
        audit = spy(new CampusAuditService(jdbc));
        media = new CampusMediaService(jdbc, access, transactions, mediaDirectory.toString());
        var target = new CampusPostService(jdbc, access, media, notifications, audit);
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        posts = (CampusPostService) proxy.getProxy();
    }

    @AfterEach
    void removeOnlyOwnedFixtureRows() {
        if (notifications != null) notifications.close();
        if (school == 0) return;
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM notifications WHERE actor_id BETWEEN ? AND ? OR recipient_id BETWEEN ? AND ?", author, secondManager, author, secondManager);
            jdbc.update("DELETE FROM campus_report WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_media WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("UPDATE campus_comment c JOIN campus_post p ON p.id=c.post_id SET c.reply_to_comment_id=NULL WHERE p.school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE c FROM campus_comment c JOIN campus_post p ON p.id=c.post_id WHERE p.school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE l FROM campus_post_like l JOIN campus_post p ON p.id=l.post_id WHERE p.school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE b FROM campus_post_bookmark b JOIN campus_post p ON p.id=b.post_id WHERE p.school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_post WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_audit WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_verification_application WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_membership WHERE school_id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM campus_school WHERE id IN (?,?)", school, otherSchool);
            jdbc.update("DELETE FROM `user` WHERE id BETWEEN ? AND ?", author, secondManager);
        });
    }

    void immediatePublishing() { jdbc.update("UPDATE campus_school SET pre_moderation=FALSE WHERE id=?", school); }
    Map<String, Object> create(long actor, String title, String content, boolean submit) {
        return posts.create(actor, school, write(title, content, submit, null));
    }
    static PostWrite write(String title, String content, boolean submit, Long version) {
        return new PostWrite(title, content, "GENERAL", List.of(), submit, version);
    }
    static long id(Map<String, Object> value) { return ((Number) value.get("id")).longValue(); }
    static long version(Map<String, Object> value) { return ((Number) value.get("version")).longValue(); }
    CampusPage<Map<String, Object>> list(long actor, String scope, String query, String status, int page, int size) {
        return posts.list(actor, school, query, null, "latest", scope, status, page, size);
    }
    static void fails(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(CampusException.class, action).getStatus());
    }
    long count(String sql, Object... parameters) { return jdbc.queryForObject(sql, Long.class, parameters); }
    Map<String, Object> pendingReport(long actor, long postId, String reason) {
        posts.report(actor, postId, new ReportWrite(reason));
        return posts.reports(manager, school, "PENDING", 0, 100).list().stream()
                .filter(item -> ((Number) item.get("postId")).longValue() == postId).findFirst().orElseThrow();
    }
}
