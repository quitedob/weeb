package com.web.campus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Real MySQL, production SQL and Spring transaction interception; deletes only this test's IDs. */
abstract class CampusSchoolTestSupport {
    protected JdbcTemplate jdbc;
    protected DataSourceTransactionManager transactions;
    protected CampusAccessService access;
    protected CampusNotificationService notifications;
    protected CampusSchoolService schools;
    protected long siteAdmin, manager, applicant, outsider, secondManager, disabled, school, secondSchool;
    protected final List<Long> ownedSchools = new ArrayList<>();

    @BeforeEach
    void fixture() {
        String url = System.getenv("WEEB_TEST_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):23306/weeb_audit(?:\\?.*)?")) {
            throw new IllegalArgumentException("Campus tests require the isolated loopback audit database");
        }
        var source = new DriverManagerDataSource(url, System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        transactions = new DataSourceTransactionManager(source);
        access = new CampusAccessService(jdbc);
        notifications = mock(CampusNotificationService.class);
        var target = new CampusSchoolService(jdbc, access, new CampusAuditService(jdbc), notifications);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        schools = (CampusSchoolService) proxy.getProxy();
        siteAdmin = ThreadLocalRandom.current().nextLong(20_000_000_000_000L, 40_000_000_000_000L);
        manager = siteAdmin + 1;
        applicant = siteAdmin + 2;
        outsider = siteAdmin + 3;
        secondManager = siteAdmin + 4;
        disabled = siteAdmin + 5;
        school = siteAdmin + 100;
        secondSchool = siteAdmin + 101;
        for (long user : List.of(siteAdmin, manager, applicant, outsider, secondManager, disabled)) {
            jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status,nickname) VALUES (?,?,?,?,?,?,?)",
                    user, "campus_" + user, "non-login-test-hash", "campus_" + user + "@example.invalid",
                    user == siteAdmin || user == disabled ? "ADMIN" : "USER", user == disabled ? 0 : 1, "Fixture " + user);
        }
        for (long id : List.of(school, secondSchool)) {
            ownedSchools.add(id);
            jdbc.update("INSERT INTO campus_school(id,name,description,created_by) VALUES (?,?,'test school',?)",
                    id, "Campus fixture " + id, siteAdmin);
        }
        membership(school, manager, "VERIFIED", "ADMIN");
        membership(secondSchool, secondManager, "VERIFIED", "ADMIN");
    }

    @AfterEach
    void cleanupOnlyOwnedRows() {
        if (jdbc == null) return;
        for (long id : ownedSchools) {
            jdbc.update("DELETE FROM campus_media WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_report WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_post_like WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=?)", id);
            jdbc.update("DELETE FROM campus_post_bookmark WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=?)", id);
            jdbc.update("DELETE FROM campus_comment WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=?)", id);
            jdbc.update("DELETE FROM campus_post WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_audit WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_verification_application WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_membership WHERE school_id=?", id);
            jdbc.update("DELETE FROM campus_school WHERE id=?", id);
        }
        for (long id : List.of(siteAdmin, manager, applicant, outsider, secondManager, disabled)) {
            jdbc.update("DELETE FROM `user` WHERE id=?", id);
        }
        ownedSchools.clear();
    }

    protected void membership(long schoolId, long user, String status, String role) {
        jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,?,?)", schoolId, user, status, role);
    }

    protected CampusSchoolDtos.Apply applicationInput() {
        return new CampusSchoolDtos.Apply("Private name " + applicant, "STUDENT-PRIVATE-" + applicant,
                "Private department", 2024, "Private verification statement");
    }

    protected long value(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    protected static void failure(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(CampusException.class, action).getStatus());
    }
}
