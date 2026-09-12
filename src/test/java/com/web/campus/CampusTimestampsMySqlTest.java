package com.web.campus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Production SQL/DTO regression: JDBC's configured zone must not shift SQL CURRENT_TIMESTAMP values. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
class CampusTimestampsMySqlTest extends CampusSchoolTestSupport {
    @ParameterizedTest(name = "SQL session UTC, JDBC timezone {0}")
    @ValueSource(strings = {"Asia/Shanghai", "UTC"})
    void allCampusResponseTimesAreUtcInstantsAcrossJdbcTimezones(String jdbcZone) throws Exception {
        DataSource source = utcSessionSource(jdbcZone);
        var timedJdbc = new JdbcTemplate(source);
        var timedTransactions = new DataSourceTransactionManager(source);
        var timedAccess = new CampusAccessService(timedJdbc);
        var timedAudit = new CampusAuditService(timedJdbc);
        var timedSchools = proxy(new CampusSchoolService(timedJdbc, timedAccess, timedAudit, notifications), timedTransactions);
        var media = mock(CampusMediaService.class);
        when(media.listForPost(anyLong())).thenReturn(List.of());
        var timedPosts = proxy(new CampusPostService(timedJdbc, timedAccess, media, notifications, timedAudit), timedTransactions);
        assertEquals("+00:00", timedJdbc.queryForObject("SELECT @@session.time_zone", String.class));

        Instant before = Instant.now().minusSeconds(2);
        List<Instant> observed = new ArrayList<>();
        var createdSchool = timedSchools.createSchool(siteAdmin,
                new CampusSchoolDtos.CreateSchool("Timestamp " + jdbcZone + " " + school, "Timezone fixture", true));
        ownedSchools.add(createdSchool.id());
        long campus = createdSchool.id();
        observed.add(createdSchool.createdAt());
        observed.add(timedSchools.getSchool(siteAdmin, campus).createdAt());
        observed.add(timedSchools.listSchools(siteAdmin, createdSchool.name(), false, 0, 20).list().get(0).createdAt());

        var application = timedSchools.apply(applicant, campus, applicationInput());
        observed.add(application.createdAt());
        assertNull(application.reviewedAt());
        observed.add(timedSchools.myApplications(applicant, campus, 0, 20).list().get(0).createdAt());
        var approved = timedSchools.reviewApplication(siteAdmin, campus, application.id(),
                new CampusSchoolDtos.Review("APPROVE", "Manually reviewed", application.version()));
        observed.add(approved.createdAt());
        observed.add(approved.reviewedAt());
        var reviewedHistory = timedSchools.applications(siteAdmin, campus, "APPROVED", 0, 20).list().get(0);
        observed.add(reviewedHistory.reviewedAt());
        var peerApplication = timedSchools.apply(outsider, campus, applicationInput());
        timedSchools.reviewApplication(siteAdmin, campus, peerApplication.id(),
                new CampusSchoolDtos.Review("APPROVE", "Manually reviewed", peerApplication.version()));
        timedSchools.members(siteAdmin, campus, null, "VERIFIED", 0, 20).list()
                .forEach(member -> observed.add(member.joinedAt()));

        var post = timedPosts.create(applicant, campus, new CampusPostDtos.PostWrite(
                "Timestamp post", "Body", "GENERAL", List.of(), true, null));
        long postId = ((Number) post.get("id")).longValue();
        observed.add(instant(post.get("createdAt")));
        observed.add(instant(post.get("updatedAt")));
        var published = timedPosts.review(siteAdmin, postId,
                new CampusPostDtos.Review("APPROVE", "", ((Number) post.get("version")).longValue()));
        observed.add(instant(published.get("updatedAt")));
        observed.add(instant(timedPosts.get(applicant, postId).get("createdAt")));
        observed.add(instant(timedPosts.list(applicant, campus, null, null, "latest", "feed", null, 0, 20)
                .list().get(0).get("createdAt")));

        var comment = timedPosts.comment(applicant, postId, new CampusPostDtos.CommentWrite("Time of comment", null));
        observed.add(instant(comment.get("createdAt")));
        observed.add(instant(timedPosts.comments(applicant, postId, 0, 20).list().get(0).get("createdAt")));
        timedPosts.report(outsider, postId, new CampusPostDtos.ReportWrite("Time of report"));
        var report = timedPosts.reports(siteAdmin, campus, null, 0, 20).list().get(0);
        observed.add(instant(report.get("createdAt")));
        assertNull(report.get("reviewedAt"));
        var dismissed = timedPosts.decideReport(siteAdmin, campus, ((Number) report.get("id")).longValue(),
                new CampusPostDtos.Review("DISMISS", "Reviewed", ((Number) report.get("version")).longValue()));
        observed.add(instant(dismissed.get("reviewedAt")));
        observed.add(instant(timedPosts.reports(siteAdmin, campus, "DISMISSED", 0, 20).list().get(0).get("reviewedAt")));
        timedSchools.audit(siteAdmin, campus, 0, 100).list().forEach(entry -> observed.add(entry.createdAt()));

        Instant after = Instant.now().plusSeconds(2);
        assertTrue(observed.size() >= 20);
        for (Instant timestamp : observed) {
            assertNotNull(timestamp);
            assertFalse(timestamp.isBefore(before), "Timestamp must not be shifted into the past by the JDBC timezone");
            assertFalse(timestamp.isAfter(after), "Timestamp must not be shifted into the future by the JDBC timezone");
        }
        var json = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String schoolTime = json.valueToTree(createdSchool).get("createdAt").asText();
        String postTime = json.valueToTree(published).get("updatedAt").asText();
        assertTrue(schoolTime.endsWith("Z"));
        assertTrue(postTime.endsWith("Z"));
        assertEquals(createdSchool.createdAt(), Instant.parse(schoolTime));

        // Exact milliseconds, rather than only a broad near-now check, catch rounding or truncation.
        timedJdbc.update("UPDATE campus_post SET created_at='2026-01-02 03:04:05.123',updated_at='2026-01-02 03:04:05.987' WHERE id=?", postId);
        var precise = timedPosts.get(applicant, postId);
        assertEquals(Instant.parse("2026-01-02T03:04:05.123Z"), precise.get("createdAt"));
        assertEquals(Instant.parse("2026-01-02T03:04:05.987Z"), precise.get("updatedAt"));
    }

    private static Instant instant(Object value) { return assertInstanceOf(Instant.class, value); }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(T target, DataSourceTransactionManager manager) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }

    private static DataSource utcSessionSource(String jdbcZone) {
        // The inherited fixture validates the exact audit endpoint before this method runs.
        var driver = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:23306/weeb_audit?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=" + jdbcZone + "&forceConnectionTimeZoneToSession=false",
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        return new DelegatingDataSource(driver) {
            @Override public Connection getConnection() throws SQLException { return utc(super.getConnection()); }
            @Override public Connection getConnection(String username, String password) throws SQLException {
                return utc(super.getConnection(username, password));
            }
            private Connection utc(Connection connection) throws SQLException {
                try (var statement = connection.createStatement()) {
                    statement.execute("SET time_zone='+00:00'");
                    return connection;
                } catch (SQLException failure) {
                    connection.close();
                    throw failure;
                }
            }
        };
    }
}
