package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.dto.GroupDto;
import com.web.mapper.GroupMapper;
import com.web.model.Group;
import com.web.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroupPaginationMySqlIntegrationTest {
    private JdbcTemplate jdbc;
    private GroupMapper groups;
    private GroupServiceImpl service;
    private DataSourceTransactionManager transactions;
    private TransactionStatus transaction;
    private long actor, other, base;

    @BeforeAll
    void productionMappers() throws Exception {
        var source = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        transactions = new DataSourceTransactionManager(source);
        var configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        // JDBC fixture mutations bypass MyBatis cache invalidation; each assertion models a fresh request.
        configuration.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
        var factory = new MybatisSqlSessionFactoryBean(); factory.setConfiguration(configuration); factory.setDataSource(source);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        groups = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject())).getMapper(GroupMapper.class);
        service = new GroupServiceImpl(); ReflectionTestUtils.setField(service, "groupMapper", groups);
    }

    @BeforeEach
    void isolatedMixedMembershipFixture() {
        transaction = transactions.getTransaction(new DefaultTransactionDefinition());
        actor = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        other = actor + 1; base = actor + 100;
        for (long user : List.of(actor, other)) jdbc.update("INSERT INTO `user`(id,username,password,user_email,status,type) VALUES (?,?,?,?,1,'USER')",
                user, "group_page_" + user, "fixture-hash", "group_page_" + user + "@example.invalid");
        for (int i = 0; i < 9; i++) {
            jdbc.update("INSERT INTO `group`(id,group_name,owner_id,status,create_time) VALUES (?,?,?,?,'2026-01-01 00:00:00')",
                    base + i, "page fixture " + i, i < 2 ? actor : other, i == 8 ? 0 : 1);
            jdbc.update("INSERT INTO group_member(group_id,user_id,role,join_status,kicked_at) VALUES (?,?,?,?,?)",
                    base + i, actor, i < 2 || i == 7 ? 1 : i == 2 ? 2 : 3,
                    i == 4 ? "PENDING" : i == 6 ? "LEFT" : "ACCEPTED",
                    i == 5 ? java.sql.Timestamp.valueOf("2026-01-02 00:00:00") : null);
        }
    }

    @AfterEach
    void rollbackFixtureOnly() { if (transaction != null && !transaction.isCompleted()) transactions.rollback(transaction); }

    @Test
    void joinedPaginationExcludesOwnedAndInactiveMembershipBeforeCountAndLimit() {
        var first = service.getUserGroupsPage(actor, 0, 1, true);
        var next = service.getUserGroupsPage(actor, 1, 1, true);
        assertEquals(2L, first.get("total")); assertEquals(2L, next.get("total"));
        assertEquals(List.of(base + 3), ids(first)); assertEquals(List.of(base + 2), ids(next));
        assertEquals(List.of(), ids(service.getUserGroupsPage(actor, 2, 1, true)));
        var inclusive = service.getUserGroupsPage(actor, 0, 100, false);
        assertEquals(4L, inclusive.get("total")); assertEquals(List.of(base + 3, base + 2, base + 1, base), ids(inclusive));
        assertEquals(4, service.getUserGroupsWithDetails(actor).size());
    }

    @Test
    void ownedPaginationUsesStableTieBreakerAndCountsOnlyActiveOwnedGroups() {
        var first = service.getUserCreatedGroupsPage(actor, 0, 1);
        var second = service.getUserCreatedGroupsPage(actor, 1, 1);
        assertEquals(2L, first.get("total")); assertEquals(List.of(base + 1), ids(first));
        assertEquals(List.of(base), ids(second));
        jdbc.update("UPDATE `group` SET status=0 WHERE id=?", base + 1);
        assertEquals(1L, service.getUserCreatedGroupsPage(actor, 0, 1).get("total"));
        assertEquals(List.of(base), ids(service.getUserCreatedGroupsPage(actor, 0, 1)));
        assertEquals(1, service.getUserCreatedGroupsWithDetails(actor).size());
    }

    @Test
    void roleLookupCoversOffPageMembershipAndRevokedOrForgedRolesAreNonMember() {
        List<Group> page = new ArrayList<>();
        for (int i = 0; i < 9; i++) { Group group = new Group(); group.setId(base + i); page.add(group); }
        service.withCurrentUserRoles(actor, page);
        assertEquals(List.of("OWNER", "OWNER", "ADMIN", "MEMBER", "NON_MEMBER", "NON_MEMBER", "NON_MEMBER", "NON_MEMBER", "NON_MEMBER"),
                page.stream().map(Group::getCurrentUserRole).toList());
        assertEquals(0, groups.countUserGroups(other, false));
        service.withCurrentUserRoles(other, page);
        assertTrue(page.stream().allMatch(group -> "NON_MEMBER".equals(group.getCurrentUserRole())));
    }

    @SuppressWarnings("unchecked")
    private List<Long> ids(Map<String, Object> page) { return ((List<GroupDto>) page.get("list")).stream().map(GroupDto::getId).toList(); }
}
