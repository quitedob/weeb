package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.mapper.ContactMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactAcceptanceMySqlIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2001, 5, 1, 12, 0);
    private SqlSessionFactory factory;
    private JdbcTemplate jdbc;
    private long applicant;
    private long recipient;
    private long contact;

    @BeforeAll
    void schema() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        try (var setup = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(setup, new ClassPathResource("sql/create/01_create_user_table.sql"));
            ScriptUtils.executeSqlScript(setup, new ClassPathResource("sql/create/10_create_contact_table.sql"));
        }
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTransactionFactory(new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory());
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        factory = bean.getObject();
        assertNotNull(factory);
    }

    @BeforeEach
    void fixture() {
        // A historical synthetic clock ensures the cleanup race cannot expire current runtime fixtures.
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM contact WHERE status=0 AND "
                        + "((expire_at IS NOT NULL AND expire_at<=?) OR (expire_at IS NULL AND create_time<=?))",
                Integer.class, NOW.plusSeconds(2), NOW.plusSeconds(2).minusDays(7)));
        applicant = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        recipient = applicant + 1;
        contact = applicant + 2;
        for (long id : new long[] {applicant, recipient}) {
            jdbc.update("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                    id, "contact_" + id, "fixture-hash", "contact_" + id + "@example.invalid", "USER");
        }
        jdbc.update("INSERT INTO contact (id,user_id,friend_id,status,expire_at,create_time) VALUES (?,?,?,0,?,?)",
                contact, applicant, recipient, NOW.plusSeconds(1), NOW.minusDays(1));
    }

    @AfterEach
    void cleanupKnownFixtures() {
        if (applicant != 0) jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", applicant, recipient);
    }

    @Test
    void onlyTheRecipientCanAcceptAnUnexpiredPendingRequestExactlyOnce() {
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(ContactMapper.class);
            assertEquals(0, mapper.acceptPendingRequest(contact, applicant, NOW, NOW.minusDays(7)));
            assertEquals(1, mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7)));
            assertEquals(0, mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7)));
            assertEquals(0, mapper.expirePendingRequests(NOW.plusSeconds(2), NOW.minusDays(7)));
        }
        assertEquals(1, status());
    }

    @Test
    void expiredBlockedRejectedAndLegacyExpiredRowsCannotBeRevived() {
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(ContactMapper.class);
            assertEquals(0, mapper.acceptPendingRequest(contact, recipient, NOW.plusSeconds(1), NOW.minusDays(7)));
            assertEquals(1, mapper.expirePendingRequests(NOW.plusSeconds(1), NOW.minusDays(7)));
            for (int status : new int[] {1, 2, 3, 4}) {
                jdbc.update("UPDATE contact SET status=?, expire_at=? WHERE id=?", status, NOW.plusDays(1), contact);
                assertEquals(0, mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7)));
            }
            jdbc.update("UPDATE contact SET status=0, expire_at=NULL, create_time=? WHERE id=?", NOW.minusDays(7), contact);
            assertEquals(0, mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7)));
            jdbc.update("UPDATE contact SET create_time=? WHERE id=?", NOW.minusDays(6), contact);
            assertEquals(1, mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7)));
        }
    }

    @Test
    void acceptanceAndExpiryCompeteAtomicallyWithoutOverwritingTheWinner() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 5; attempt++) {
                jdbc.update("UPDATE contact SET status=0 WHERE id=?", contact);
                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> acceptance = workers.submit(() -> raceUpdate(true, start));
                Future<Integer> expiry = workers.submit(() -> raceUpdate(false, start));
                start.countDown();
                int accepted = acceptance.get(10, TimeUnit.SECONDS);
                int expired = expiry.get(10, TimeUnit.SECONDS);
                assertEquals(1, accepted + expired);
                assertEquals(accepted == 1 ? 1 : 4, status());
            }
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private int raceUpdate(boolean accept, CountDownLatch start) throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        for (int attempt = 1; ; attempt++) {
            try (var session = factory.openSession(false)) {
                try {
                    var mapper = session.getMapper(ContactMapper.class);
                    int changed = accept ? mapper.acceptPendingRequest(contact, recipient, NOW, NOW.minusDays(7))
                            : mapper.expirePendingRequests(NOW.plusSeconds(2), NOW.minusDays(7));
                    session.commit();
                    return changed;
                } catch (RuntimeException failure) {
                    session.rollback();
                    if (attempt >= 3 || !isMysqlDeadlock(failure)) throw failure;
                    // Close this deadlock victim's session and retry the whole operation in a fresh transaction.
                }
            }
        }
    }

    private boolean isMysqlDeadlock(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1213) return true;
        }
        return false;
    }

    private int status() {
        return jdbc.queryForObject("SELECT status FROM contact WHERE id=?", Integer.class, contact);
    }
}
