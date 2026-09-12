package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.exception.WeebException;
import com.web.mapper.*;
import com.web.model.Group;
import com.web.service.*;
import com.web.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Exercises production SQL and transaction rollback using only the disposable audit database. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroupMembershipMySqlIntegrationTest {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate session;
    private DataSourceTransactionManager transactions;
    private GroupMapper groups;
    private long ownerId;
    private long applicantId;
    private long groupId;

    @BeforeAll
    void productionSchemaAndMappers() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        try (var connection = dataSource.getConnection()) {
            for (String script : new String[] {"01_create_user_table.sql", "03_create_group_table.sql",
                    "07_create_group_member_table.sql", "24_create_group_application_table.sql"}) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/create/" + script));
            }
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(GroupMemberMapper.class);
        configuration.addMapper(GroupApplicationMapper.class);
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfiguration(configuration);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        session = new SqlSessionTemplate(java.util.Objects.requireNonNull(bean.getObject()));
        groups = session.getMapper(GroupMapper.class);
    }

    @BeforeEach
    void committedFixtureForIndependentConnections() {
        ownerId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        applicantId = ownerId + 1;
        groupId = ownerId + 2;
        for (long id : new long[] {ownerId, applicantId}) {
            jdbc.update("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                    id, "group_" + id, "fixture-hash", "group_" + id + "@example.invalid", "USER");
        }
        jdbc.update("INSERT INTO `group` (id,group_name,group_description,owner_id,status,max_members,member_count) "
                + "VALUES (?,'counter fixture','preserve description',?,1,2,1)", groupId, ownerId);
        jdbc.update("INSERT INTO group_member (group_id,user_id,role,join_status) VALUES (?,?,1,'ACCEPTED')",
                groupId, ownerId);
    }

    @AfterEach
    void removeOnlyOwnedFixtures() {
        if (groupId != 0) jdbc.update("DELETE FROM `group` WHERE id=?", groupId);
        if (ownerId != 0) jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", ownerId, applicantId);
    }

    @Test
    void simultaneousReservationsCannotExceedCapacityOrLoseIncrements() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        try {
            for (int capacity : new int[] {2, 3}) {
                jdbc.update("UPDATE `group` SET member_count=1,max_members=? WHERE id=?", capacity, groupId);
                var ready = new CountDownLatch(2);
                var start = new CountDownLatch(1);
                Callable<Integer> reserve = () -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return groups.incrementMemberCountIfCapacity(groupId);
                };
                var first = workers.submit(reserve);
                var second = workers.submit(reserve);
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                assertEquals(capacity - 1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
                assertEquals(capacity, count());
                assertEquals("preserve description", jdbc.queryForObject(
                        "SELECT group_description FROM `group` WHERE id=?", String.class, groupId));
                assertEquals(ownerId, groups.selectById(groupId).getOwnerId());
            }
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void countersRespectFrozenGroupsNullCapacityAndZeroFloor() {
        jdbc.update("UPDATE `group` SET status=2 WHERE id=?", groupId);
        assertEquals(0, groups.incrementMemberCountIfCapacity(groupId));
        assertEquals(1, count());
        assertEquals(2, groups.selectById(groupId).getStatus());
        jdbc.update("UPDATE `group` SET status=1,max_members=NULL,member_count=NULL WHERE id=?", groupId);
        assertEquals(1, groups.incrementMemberCountIfCapacity(groupId));
        assertEquals(1, count());
        assertEquals(1, groups.decrementMemberCount(groupId));
        assertEquals(0, groups.decrementMemberCount(groupId));
        assertEquals(0, count());
    }

    @Test
    void failedCapacityReservationRollsBackApprovalAndHasNoMembershipOrNotificationEffects() {
        Group staleSnapshot = groups.selectById(groupId);
        jdbc.update("UPDATE `group` SET member_count=2 WHERE id=?", groupId);
        jdbc.update("INSERT INTO group_application (group_id,user_id,message) VALUES (?,?,'rollback fixture')",
                groupId, applicantId);
        long applicationId = jdbc.queryForObject("SELECT id FROM group_application WHERE group_id=? AND user_id=?",
                Long.class, groupId, applicantId);
        // Simulate a snapshot taken before another transaction filled the final slot. Writes use real SQL.
        var snapshotMapper = mock(GroupMapper.class, delegatesTo(groups));
        doReturn(staleSnapshot).when(snapshotMapper).selectById(groupId);
        var service = new GroupServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", snapshotMapper);
        ReflectionTestUtils.setField(service, "groupMapper", snapshotMapper);
        ReflectionTestUtils.setField(service, "groupMemberMapper", session.getMapper(GroupMemberMapper.class));
        ReflectionTestUtils.setField(service, "groupApplicationMapper", session.getMapper(GroupApplicationMapper.class));
        var auth = mock(AuthService.class);
        var owner = new com.web.model.User();
        owner.setId(ownerId);
        owner.setUsername("group_" + ownerId);
        owner.setStatus(1);
        owner.setType(com.web.constant.UserType.USER);
        when(auth.findByUserID(ownerId)).thenReturn(owner);
        ReflectionTestUtils.setField(service, "authService", auth);
        ReflectionTestUtils.setField(service, "userTypeSecurityService", mock(UserTypeSecurityService.class));
        var broadcasts = mock(MessageBroadcastService.class);
        var notifications = mock(NotificationService.class);
        var chats = mock(ChatListMapper.class);
        ReflectionTestUtils.setField(service, "messageBroadcastService", broadcasts);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        ReflectionTestUtils.setField(service, "chatListMapper", chats);
        var advice = new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(advice);
        var transactionalService = (GroupService) proxy.getProxy();

        assertThrows(WeebException.class,
                () -> transactionalService.approveApplication(groupId, applicationId, ownerId, "approve"));
        verify(snapshotMapper).incrementMemberCountIfCapacity(groupId);
        var application = jdbc.queryForMap("SELECT status,reviewer_id,reviewed_at FROM group_application WHERE id=?",
                applicationId);
        assertEquals("PENDING", application.get("status"));
        assertNull(application.get("reviewer_id"));
        assertNull(application.get("reviewed_at"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id=?",
                Integer.class, groupId, applicantId));
        assertEquals(2, count());
        verifyNoInteractions(broadcasts, notifications, chats);
    }

    private int count() {
        return jdbc.queryForObject("SELECT member_count FROM `group` WHERE id=?", Integer.class, groupId);
    }
}
