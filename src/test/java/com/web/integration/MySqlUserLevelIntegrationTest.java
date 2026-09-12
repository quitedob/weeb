package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.exception.WeebException;
import com.web.mapper.UserLevelHistoryMapper;
import com.web.mapper.UserMapper;
import com.web.model.UserLevelHistory;
import com.web.service.impl.UserLevelHistoryServiceImpl;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlUserLevelIntegrationTest {
    private DriverManagerDataSource dataSource;
    private SqlSessionFactory factory;
    private SqlSession session;
    private Connection connection;
    private long author;
    private long reader;
    private long other;
    private int baselineBasic;

    @BeforeAll
    void schema() throws Exception {
        dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        try (var setup = dataSource.getConnection()) {
            for (String script : new String[] {"01_create_user_table.sql", "02_create_user_stats_table.sql",
                    "14_create_article_category_table.sql", "08_create_article_table.sql", "06_create_message_table.sql",
                    "11_create_article_comment_table.sql", "12_create_article_like_table.sql", "17_create_user_follow_table.sql",
                    "19_create_user_level_history_table.sql", "28_create_user_preferences_table.sql", "29_create_user_login_day_table.sql"}) {
                ScriptUtils.executeSqlScript(setup, new ClassPathResource("sql/create/" + script));
            }
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
    void fixture() throws Exception {
        session = factory.openSession(false);
        connection = session.getConnection();
        connection.setAutoCommit(false);
        author = ThreadLocalRandom.current().nextLong(100_000_000_000L, 900_000_000_000L);
        reader = author + 1;
        other = author + 2;
        baselineBasic = users().countUsersByLevel(1);
        for (long id : new long[] {author, reader, other}) {
            insertUser(connection, id, id == other ? "ADMIN" : "USER");
        }
        session.clearCache();
    }

    @AfterEach
    void rollback() throws Exception {
        if (session != null) {
            connection.rollback();
            session.rollback(true);
            session.close();
        }
    }

    @Test
    void countsUsePublishedAuthoredContentReceivedInteractionsAndSuccessfulMessages() throws Exception {
        execute(connection, "INSERT INTO articles (article_id,user_id,article_title,status,exposure_count) VALUES (?,?,?,2,100)", author, author, "published");
        execute(connection, "INSERT INTO articles (article_id,user_id,article_title,status,exposure_count) VALUES (?,?,?,1,999)", reader, author, "pending");
        execute(connection, "INSERT INTO articles (article_id,user_id,article_title,status,exposure_count) VALUES (?,?,?,2,50)", other, other, "other author");
        execute(connection, "INSERT INTO article_like (article_id,user_id) VALUES (?,?),(?,?),(?,?)", author, reader, reader, reader, other, author);
        execute(connection, "INSERT INTO article_comment (article_id,user_id,content) VALUES (?,?,'received'),(?,?,'pending'),(?,?,'outgoing')", author, reader, reader, reader, other, author);
        execute(connection, "INSERT INTO user_follow (follower_id,followee_id) VALUES (?,?)", reader, author);
        execute(connection, "INSERT INTO message (sender_id,receiver_id,content,status) VALUES (?,?,JSON_OBJECT('text','sent'),1),(?,?,JSON_OBJECT('text','read'),3),(?,?,JSON_OBJECT('text','failed'),4),(?,?,JSON_OBJECT('text','incoming'),1)", author, reader, author, reader, author, reader, reader, author);
        assertEquals(1, users().countUserArticles(author));
        assertEquals(2, users().countUserMessages(author));
        assertEquals(1, users().countUserLikes(author));
        assertEquals(1, users().countUserComments(author));
        assertEquals(1, users().countUserFollowers(author));
        assertEquals(100, users().countUserArticleViews(author));
    }

    @Test
    void loginDaysAreIdempotentPerDateAndUnrelatedToRegistrationAge() throws Exception {
        execute(connection, "UPDATE `user` SET registration_date='2000-01-01' WHERE id=?", author);
        assertEquals(0, users().countUserLoginDays(author));
        users().recordSuccessfulLoginDay(author);
        users().recordSuccessfulLoginDay(author);
        assertEquals(1, users().countUserLoginDays(author));
        execute(connection, "INSERT INTO user_login_day (user_id,login_date) VALUES (?,CURRENT_DATE - INTERVAL 1 DAY)", author);
        session.clearCache();
        assertEquals(2, users().countUserLoginDays(author));
        assertEquals(0, users().countUserLoginDays(reader));
    }

    @Test
    void latestValidHistoryHasDeterministicOrderAndSurvivesRetentionCleanup() throws Exception {
        var mapper = session.getMapper(UserLevelHistoryMapper.class);
        var timestamp = LocalDateTime.now().minusYears(2).withNano(0);
        UserLevelHistory first = history(author, 1, 2, timestamp, 1);
        UserLevelHistory latest = history(author, 2, 3, timestamp, 1);
        UserLevelHistory inactive = history(author, 3, 8, timestamp.plusDays(1), 0);
        UserLevelHistory invalid = history(author, 3, 10, timestamp.plusDays(2), 1);
        for (var row : List.of(first, latest, inactive, invalid)) assertEquals(1, mapper.insert(row));
        assertEquals(3, mapper.getCurrentLevelByUserId(author));
        assertEquals(invalid.getId(), mapper.findByUserIdWithPaging(author, 0, 1).get(0).getId());
        assertEquals(baselineBasic + 2, users().countUsersByLevel(1));
        assertEquals(author, users().lockUserForLevelChange(author));
        assertEquals(3, users().selectCurrentLevelForUpdate(author));
        mapper.cleanupExpiredRecords(LocalDateTime.now().minusYears(1));
        assertEquals(3, mapper.getCurrentLevelByUserId(author));
        assertEquals(1, mapper.countByUserId(author));
        assertEquals(latest.getId(), mapper.findByUserId(author).get(0).getId());
        assertNull(mapper.findById(first.getId()));
        assertNull(mapper.findById(inactive.getId()));
        assertNull(mapper.findById(invalid.getId()));
    }

    @Test
    void concurrentHistoryChangesAllowOnlyOneWriterWithTheSameOldLevel() throws Exception {
        long isolated = author + 100;
        try (var setup = dataSource.getConnection()) { insertUser(setup, isolated, "USER"); }
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = workers.submit(() -> changeInTransaction(isolated, 2, start));
            var second = workers.submit(() -> changeInTransaction(isolated, 3, start));
            start.countDown();
            assertEquals(1, (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0));
            try (var read = factory.openSession()) {
                var rows = read.getMapper(UserLevelHistoryMapper.class).findByUserId(isolated);
                assertEquals(1, rows.size());
                assertEquals(1, rows.get(0).getOldLevel());
            }
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            // The one committed fixture user owns its history by a cascading FK.
            try (var cleanup = dataSource.getConnection()) { execute(cleanup, "DELETE FROM `user` WHERE id=?", isolated); }
        }
    }

    private boolean changeInTransaction(long userId, int newLevel, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try (var tx = factory.openSession(false)) {
            var service = new UserLevelHistoryServiceImpl();
            ReflectionTestUtils.setField(service, "userMapper", tx.getMapper(UserMapper.class));
            ReflectionTestUtils.setField(service, "userLevelHistoryMapper", tx.getMapper(UserLevelHistoryMapper.class));
            try {
                boolean saved = service.recordLevelChange(userId, 1, newLevel, "concurrency test", 1, null, null, null);
                tx.commit();
                return saved;
            } catch (WeebException stale) {
                tx.rollback();
                return false;
            }
        }
    }

    private UserLevelHistory history(long userId, int oldLevel, int newLevel, LocalDateTime timestamp, int status) {
        return UserLevelHistory.builder().userId(userId).oldLevel(oldLevel).newLevel(newLevel).changeType(1)
                .changeReason("fixture").status(status).changeTime(timestamp).createdAt(timestamp).updatedAt(timestamp).build();
    }

    private UserMapper users() { return session.getMapper(UserMapper.class); }

    private void insertUser(Connection target, long id, String type) throws Exception {
        execute(target, "INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                id, "level_" + id, "fixture-hash", "level_" + id + "@example.invalid", type);
    }

    private void execute(Connection target, String sql, Object... values) throws Exception {
        try (var statement = target.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }
}
