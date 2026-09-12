package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.mapper.ChatListMapper;
import com.web.mapper.ContactMapper;
import com.web.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Runs only against the explicitly supplied disposable audit database. Each test rolls back its data. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlContractIntegrationTest {
    private SqlSessionFactory factory;
    private SqlSession session;
    private Connection connection;

    @BeforeAll
    void loadProductionSchemaAndMappers() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        var initializer = new com.web.config.DatabaseInitializer();
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "dataSource", dataSource);
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "jdbcTemplate", new org.springframework.jdbc.core.JdbcTemplate(dataSource));
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "environment", new org.springframework.mock.env.MockEnvironment());
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "sqlFileLoader", new com.web.util.SqlFileLoader());
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "databaseUrl", System.getenv("WEEB_TEST_MYSQL_URL"));
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "dbUsername", dataSource.getUsername());
        org.springframework.test.util.ReflectionTestUtils.setField(initializer, "dbPassword", System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        initializer.run();
        initializer.run(); // Repeated development startup must preserve the schema and seed data.
        assertEquals(30, new org.springframework.jdbc.core.JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class));
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTransactionFactory(new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory());
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(com.web.mapper.GroupMemberMapper.class);
        configuration.addMapper(com.web.mapper.GroupApplicationMapper.class);
        bean.setConfiguration(configuration);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        factory = bean.getObject();
        assertNotNull(factory);
    }

    @BeforeEach
    void seedTransactionalFixture() throws Exception {
        session = factory.openSession(false);
        connection = session.getConnection();
        connection.setAutoCommit(false);
        for (int id = 91001; id <= 91008; id++) {
            execute("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                    id, "audit_" + id, "old-password-hash", "audit_" + id + "@example.invalid", "USER");
            execute("INSERT INTO user_stats (user_id,website_coins) VALUES (?,100)", id);
        }
    }

    @AfterEach
    void rollbackFixture() throws Exception {
        if (session != null) {
            connection.rollback();
            session.rollback(true);
            session.close();
        }
    }

    @Test
    void searchFiltersMembershipBeforeTotalsAndPagination() throws Exception {
        execute("INSERT INTO shared_chat (id,chat_type,participant_1_id,participant_2_id) VALUES (92001,'PRIVATE',91001,91002)");
        execute("INSERT INTO `group` (id,group_name,owner_id,status,shared_chat_id) VALUES (93001,'search group',91001,1,92002)");
        execute("INSERT INTO shared_chat (id,chat_type,group_id) VALUES (92002,'GROUP',93001)");
        execute("INSERT INTO group_member (group_id,user_id,join_status) VALUES (93001,91001,'ACCEPTED'),(93001,91002,'PENDING'),(93001,91003,'ACCEPTED')");
        execute("UPDATE group_member SET kicked_at=NOW() WHERE group_id=93001 AND user_id=91003");
        execute("INSERT INTO message (sender_id,chat_id,content,is_recalled) VALUES (91001,92001,JSON_OBJECT('content','needle private'),0),(91001,92001,JSON_OBJECT('content','needle recalled'),1),(91001,92002,JSON_OBJECT('content','needle group'),0)");
        var search = new com.web.service.MessageSearchService(new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true)));
        assertEquals(2L, search.search(91001L, "needle", 0, 1, null, null, null, null, null, "time_desc").get("total"));
        var first = (java.util.List<?>) search.search(91001L, "needle", 0, 1, null, null, null, null, null, "time_desc").get("list");
        assertEquals(1, first.size());
        var group = (java.util.Map<?, ?>) first.get(0);
        assertEquals(92002L, group.get("sharedChatId"));
        assertEquals(93001L, group.get("targetId"));
        assertEquals(1L, search.search(91002L, "needle", 0, 10, null, null, null, null, null, "time_desc").get("total"));
        assertEquals(0L, search.search(91003L, "needle", 0, 10, null, null, null, null, null, "time_desc").get("total"));
        assertEquals(0L, search.search(91004L, "needle", 0, 10, null, null, null, "91001", "93001", "time_desc").get("total"));
        execute("UPDATE `group` SET status=0 WHERE id=93001");
        assertEquals(1L, search.search(91001L, "needle", 0, 10, null, null, null, null, null, "time_desc").get("total"));
    }

    @Test
    void articlePublicationAndCoinsUseTheSamePersistedContract() throws Exception {
        for (int status = 0; status < 4; status++) {
            execute("INSERT INTO articles (article_id,user_id,article_title,status) VALUES (?,?,?,?)", 94000 + status, 91002, "audit article " + status, status);
        }
        var articles = session.getMapper(com.web.mapper.ArticleMapper.class);
        assertEquals(1, articles.countAllArticles());
        assertEquals(94002L, articles.getAllArticles(0, 10, "created_at", "desc").get(0).getArticleId());
        assertEquals(1, articles.reviewArticle(94001L, 2, 91003L, "approved"));
        assertEquals(0, articles.reviewArticle(94001L, 3, 91004L, "stale rejection"));
        assertEquals("91003", scalar("SELECT reviewer_id FROM articles WHERE article_id=94001"));
        var service = new com.web.service.impl.ArticleServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "articleMapper", articles);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userStatsMapper", session.getMapper(com.web.mapper.UserStatsMapper.class));
        assertThrows(com.web.exception.WeebException.class, () -> service.addCoin(94002L, -1.0, 91001L));
        assertThrows(com.web.exception.WeebException.class, () -> service.addCoin(94002L, 101.0, 91001L));
        assertThrows(com.web.exception.WeebException.class, () -> service.addCoin(94000L, 1.0, 91001L));
        assertEquals("100", scalar("SELECT website_coins FROM user_stats WHERE user_id=91001"));
        assertTrue(service.addCoin(94002L, 5.0, 91001L));
        assertEquals("95", scalar("SELECT website_coins FROM user_stats WHERE user_id=91001"));
        assertEquals("5.00", scalar("SELECT sponsors_count FROM articles WHERE article_id=94002"));
    }

    @Test
    void loginTimestampDoesNotOverwriteConcurrentAccountChanges() throws Exception {
        execute("UPDATE `user` SET status=0,password='reset-password-hash' WHERE id=91001");
        var auth = session.getMapper(com.web.mapper.AuthMapper.class);
        var loginTime = new java.util.Date(1_789_171_200_000L);
        assertEquals(1, auth.updateLoginTime(91001L, loginTime));
        assertEquals("0", scalar("SELECT status FROM `user` WHERE id=91001"));
        assertEquals("reset-password-hash", scalar("SELECT password FROM `user` WHERE id=91001"));
        assertEquals("USER", scalar("SELECT type FROM `user` WHERE id=91001"));
        assertNotNull(auth.selectAuthById(91001L).getLoginTime());
    }

    @Test
    void favoritesFilterPrivateArticlesBeforeCountingAndPagination() throws Exception {
        for (int status = 0; status < 4; status++) {
            execute("INSERT INTO articles (article_id,user_id,article_title,status) VALUES (?,?,?,?)",
                    94000 + status, 91002, "favorite fixture " + status, status);
            execute("INSERT INTO article_favorite (article_id,user_id,created_at) VALUES (?,?,?)",
                    94000 + status, 91001, LocalDateTime.of(2026, 9, 11, 1, status));
        }
        execute("INSERT INTO articles (article_id,user_id,article_title,status) VALUES (94004,91001,'own draft',0)");
        execute("INSERT INTO article_favorite (article_id,user_id,created_at) VALUES (94004,91001,'2026-09-11 02:00:00')");
        var articles = session.getMapper(com.web.mapper.ArticleMapper.class);
        assertEquals(2, articles.countUserFavoriteArticles(91001L, false));
        assertEquals(94004L, articles.getUserFavoriteArticles(91001L, 0, 1, false).get(0).getArticleId());
        assertEquals(94002L, articles.getUserFavoriteArticles(91001L, 1, 1, false).get(0).getArticleId());
        assertTrue(articles.getUserFavoriteArticles(91001L, 2, 1, false).isEmpty());
        assertEquals(5, articles.countUserFavoriteArticles(91001L, true));
        assertEquals(5, articles.getUserFavoriteArticles(91001L, 0, 10, true).size());
    }

    @Test
    void messageRoutingAndStateSurviveDatabaseRoundTrip() throws Exception {
        var messages = session.getMapper(com.web.mapper.MessageMapper.class);
        var message = new com.web.model.Message();
        message.setSenderId(91001L);
        message.setReceiverId(91002L);
        message.setChatId(92001L);
        message.setMessageType(1);
        message.setStatus(1);
        message.setContent(com.web.vo.message.TextMessageContent.builder().content("audit message").build());
        message.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        message.setUpdatedAt(message.getCreatedAt());
        messages.insertMessage(message);
        var stored = messages.selectMessageById(message.getId());
        assertEquals(91002L, stored.getReceiverId());
        assertEquals(1, stored.getStatus());
        assertEquals("audit message", stored.getContent().getContent());
        assertNull(stored.getGroupId());
        assertEquals(stored.getChatId(), messages.selectById(message.getId()).getChatId(), "BaseMapper must use the live schema too");
        message.setId(null);
        message.setReceiverId(null);
        message.setGroupId(93001L);
        messages.insertGroupMessage(message);
        assertEquals(93001L, messages.selectMessageById(message.getId()).getGroupId());
    }

    @Test
    void passwordAndStatusChangesPersistWithoutGrantingPrivileges() throws Exception {
        UserMapper users = session.getMapper(UserMapper.class);
        assertEquals(1, users.updatePassword(91001L, "new-password-hash"));
        assertEquals(1, users.updateStatus(91001L, 0));
        assertEquals("new-password-hash", scalar("SELECT password FROM `user` WHERE id=91001"));
        assertEquals("0", scalar("SELECT status FROM `user` WHERE id=91001"));
        assertEquals("USER", scalar("SELECT type FROM `user` WHERE id=91001"));
        assertEquals(0, users.compareAndSetPassword(91001L, "stale-reset", "old-password-hash"));
        assertEquals(1, users.compareAndSetPassword(91001L, "valid-reset", "new-password-hash"));
        assertEquals("valid-reset", scalar("SELECT password FROM `user` WHERE id=91001"));
    }

    @Test
    void onlyPersistedPrivateParticipantsAndActiveAcceptedGroupMembersHaveAccess() throws Exception {
        execute("INSERT INTO shared_chat (id,chat_type,participant_1_id,participant_2_id) VALUES (92001,'PRIVATE',91001,91002)");
        execute("INSERT INTO `group` (id,group_name,owner_id,status) VALUES (93001,'audit group',91001,1)");
        execute("INSERT INTO shared_chat (id,chat_type,group_id) VALUES (92002,'GROUP',93001)");
        execute("UPDATE `group` SET shared_chat_id=92002 WHERE id=93001");
        execute("INSERT INTO group_member (group_id,user_id,join_status) VALUES (93001,91001,'ACCEPTED'),(93001,91002,'PENDING'),(93001,91003,'ACCEPTED')");
        execute("UPDATE group_member SET kicked_at=NOW() WHERE group_id=93001 AND user_id=91003");
        var chats = session.getMapper(ChatListMapper.class);

        assertTrue(chats.canUserAccessSharedChat(91001L, 92001L));
        assertTrue(chats.canUserAccessSharedChat(91002L, 92001L));
        assertFalse(chats.canUserAccessSharedChat(91003L, 92001L));
        assertTrue(chats.canUserAccessSharedChat(91001L, 92002L));
        assertFalse(chats.canUserAccessSharedChat(91002L, 92002L));
        assertFalse(chats.canUserAccessSharedChat(91003L, 92002L));
        assertFalse(chats.canUserAccessSharedChat(91004L, 92002L));
        execute("UPDATE `group` SET status=0 WHERE id=93001");
        session.clearCache();
        assertFalse(chats.canUserAccessSharedChat(91001L, 92002L));
    }

    @Test
    void expiryKeepsAcceptedAndFutureRequestsAndRetainsExpiredRows() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 2, 0);
        execute("INSERT INTO contact (user_id,friend_id,status,expire_at,create_time) VALUES (91001,91002,0,?,?)", now.minusMinutes(1), now.minusDays(1));
        execute("INSERT INTO contact (user_id,friend_id,status,expire_at,create_time) VALUES (91001,91003,0,?,?)", now.plusMinutes(1), now.minusDays(20));
        execute("INSERT INTO contact (user_id,friend_id,status,expire_at,create_time) VALUES (91001,91004,0,NULL,?)", now.minusDays(8));
        execute("INSERT INTO contact (user_id,friend_id,status,expire_at,create_time) VALUES (91001,91005,0,NULL,?)", now.minusDays(1));
        execute("INSERT INTO contact (user_id,friend_id,status,expire_at,create_time) VALUES (91001,91006,1,?,?)", now.minusDays(1), now.minusDays(8));

        var contacts = session.getMapper(ContactMapper.class);
        assertEquals(2, contacts.expirePendingRequests(now, now.minusDays(7)));
        assertEquals(0, contacts.expirePendingRequests(now, now.minusDays(7)), "Expiration is idempotent");
        assertEquals("5", scalar("SELECT COUNT(*) FROM contact WHERE user_id=91001"));
        assertEquals("4", scalar("SELECT status FROM contact WHERE user_id=91001 AND friend_id=91002"));
        assertEquals("0", scalar("SELECT status FROM contact WHERE user_id=91001 AND friend_id=91003"));
        assertEquals("1", scalar("SELECT status FROM contact WHERE user_id=91001 AND friend_id=91006"));
    }

    @Test
    void groupAndPrivateCreationResolveGeneratedIdentityInsteadOfAffectedRowCount() throws Exception {
        var chats = session.getMapper(ChatListMapper.class);
        execute("INSERT INTO `group` (id,group_name,owner_id,status) VALUES (93001,'identity group',91001,1)");
        assertEquals(1, chats.createGroupSharedChat(93001L));
        Long groupChatId = chats.findGroupSharedChatId(93001L);
        assertNotNull(groupChatId);
        assertEquals(groupChatId.toString(), scalar("SELECT id FROM shared_chat WHERE group_id=93001"));
        chats.createGroupSharedChat(93001L);
        assertEquals(groupChatId, chats.findGroupSharedChatId(93001L));
        assertEquals("1", scalar("SELECT COUNT(*) FROM shared_chat WHERE group_id=93001"));

        assertEquals(1, chats.createSharedChat(91001L, 91002L, "PRIVATE"));
        Long privateChatId = chats.findSharedChatId(91001L, 91002L);
        assertNotNull(privateChatId);
        assertNotEquals(groupChatId, privateChatId);
        assertEquals(privateChatId.toString(), scalar("SELECT id FROM shared_chat WHERE participant_1_id=91001 AND participant_2_id=91002"));
    }

    @Test
    void followCountsUseRealRelationsAndDoNotRequireAMissingColumn() throws Exception {
        execute("INSERT INTO user_follow (follower_id,followee_id) VALUES (91001,91002)");
        assertEquals(1, session.getMapper(UserMapper.class).syncFollowCounts(91002L));
        assertEquals("1", scalar("SELECT fans_count FROM user_stats WHERE user_id=91002"));
    }

    @Test
    void groupCreationPersistsDescriptionVisibilityAndDistinctInitialMembers() throws Exception {
        var service = groupService();
        var request = new com.web.vo.group.GroupCreateVo();
        request.setGroupName("Initial group");
        request.setGroupDescription("Persist this description");
        request.setGroupType("PRIVATE");
        request.setInitialMemberIds(java.util.List.of(91001L, 91002L, 91002L));
        var group = service.createGroup(request, 91001L);
        var stored = session.getMapper(com.web.mapper.GroupMapper.class).selectById(group.getId());
        assertEquals("Persist this description", stored.getGroupDescription());
        assertEquals(0, stored.getIsVisible());
        assertEquals(2, stored.getMemberCount());
        assertEquals(2, group.getMemberCount());
        assertNotNull(stored.getSharedChatId());
        assertEquals("2", scalar("SELECT COUNT(*) FROM group_member WHERE group_id=" + group.getId()));
        assertEquals("2", scalar("SELECT COUNT(*) FROM chat_list WHERE group_id=" + group.getId()));
        var memberList = service.getUserGroupsWithDetails(91002L);
        assertEquals(1, memberList.size());
        assertEquals("MEMBER", memberList.get(0).getCurrentUserRole());
        assertEquals(stored.getSharedChatId(), memberList.get(0).getSharedChatId());

        service.quitGroup(group.getId(), 91002L);
        var invitation = new com.web.vo.group.GroupInviteVo();
        invitation.setGroupId(group.getId());
        invitation.setMemberIds(java.util.List.of(91002L));
        assertTrue(service.inviteMembers(invitation, 91001L));
        assertEquals("2", scalar("SELECT COUNT(*) FROM chat_list WHERE group_id=" + group.getId()),
                "Rejoining must reuse the existing conversation instead of duplicating its sidebar row");
        assertEquals("2", scalar("SELECT member_count FROM `group` WHERE id=" + group.getId()));
    }

    @Test
    void groupListsAndDetailDoNotConferMembershipFromPendingKickedOrInvalidRoles() throws Exception {
        execute("INSERT INTO `group` (id,group_name,owner_id,status,shared_chat_id) VALUES (93001,'members',91001,1,92002)");
        execute("INSERT INTO group_member (group_id,user_id,role,join_status) VALUES "
                + "(93001,91001,1,'ACCEPTED'),(93001,91002,2,'ACCEPTED'),(93001,91003,2,'PENDING'),"
                + "(93001,91004,2,'ACCEPTED'),(93001,91005,1,'ACCEPTED'),(93001,91006,0,'ACCEPTED')");
        execute("UPDATE group_member SET kicked_at=NOW() WHERE group_id=93001 AND user_id=91004");
        var groups = session.getMapper(com.web.mapper.GroupMapper.class);
        assertEquals("OWNER", groups.selectUserCreatedGroupsWithDetails(91001L).get(0).getCurrentUserRole());
        assertEquals(92002L, groups.selectUserGroupsWithDetails(91002L).get(0).getSharedChatId());
        assertEquals("ADMIN", groups.selectUserGroupsWithDetails(91002L).get(0).getCurrentUserRole());
        var service = groupService();
        for (long userId : new long[] {91003, 91004, 91005, 91006}) {
            assertTrue(groups.findGroupsByUserId(userId).isEmpty());
            assertTrue(groups.selectUserGroupsWithDetails(userId).isEmpty());
            assertEquals("NON_MEMBER", service.getGroupWithDetails(93001L, userId).getCurrentUserRole());
        }
        execute("UPDATE `group` SET status=0 WHERE id=93001");
        session.clearCache(); // This fixture write bypasses MyBatis' normal update cache invalidation.
        assertTrue(groups.selectUserGroupsWithDetails(91002L).isEmpty());
        assertTrue(groups.selectUserCreatedGroupsWithDetails(91001L).isEmpty());
    }

    @Test
    void groupDiscoveryFiltersPrivateAndInactiveRowsBeforeCountingAndHonorsSortAndExactId() throws Exception {
        execute("INSERT INTO `group` (id,group_name,owner_id,status,is_visible,create_time) VALUES "
                + "(93001,'needle alpha',91001,1,1,'2026-01-01'),"
                + "(93002,'needle beta',91001,1,1,'2026-02-01'),"
                + "(93003,'needle hidden',91001,1,0,'2026-03-01'),"
                + "(93004,'needle deleted',91001,0,1,'2026-04-01'),"
                + "(93005,'needle frozen',91001,2,1,'2026-05-01')");
        var groups = session.getMapper(com.web.mapper.GroupMapper.class);
        assertEquals(2, groups.countSearchGroups("needle"));
        assertEquals(2, groups.countSearchGroupsWithFilters("needle", null, null));
        assertEquals(93001L, groups.searchGroups("93001", 0, 10).get(0).getId());
        assertTrue(groups.searchGroups("93003", 0, 10).isEmpty());
        assertEquals(93001L, groups.searchGroupsWithFilters("needle", 0, 1, null, null,
                "g.create_time ASC, g.id ASC").get(0).getId());
        assertEquals(93002L, groups.searchGroupsWithFilters("needle", 0, 1, null, null,
                "g.group_name DESC, g.id DESC").get(0).getId());
        assertEquals(93002L, groups.searchGroupsWithFilters("needle", 1, 1, null, null,
                "g.group_name ASC, g.id ASC").get(0).getId());
        assertEquals(1, groups.countSearchGroupsWithFilters("needle", "2026-02-01", "2026-02-28"));
    }

    private com.web.service.impl.GroupServiceImpl groupService() {
        var service = new com.web.service.impl.GroupServiceImpl();
        var groups = session.getMapper(com.web.mapper.GroupMapper.class);
        var users = session.getMapper(UserMapper.class);
        var auth = org.mockito.Mockito.mock(com.web.service.AuthService.class);
        org.mockito.Mockito.when(auth.findByUserID(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(call -> users.selectById((Long) call.getArgument(0)));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", groups);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "groupMapper", groups);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userMapper", users);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "authService", auth);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userTypeSecurityService",
                org.mockito.Mockito.mock(com.web.service.UserTypeSecurityService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "groupMemberMapper", session.getMapper(com.web.mapper.GroupMemberMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chatListMapper", session.getMapper(ChatListMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notificationService",
                org.mockito.Mockito.mock(com.web.service.NotificationService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "messageBroadcastService",
                org.mockito.Mockito.mock(com.web.service.MessageBroadcastService.class));
        return service;
    }

    private void execute(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private String scalar(String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
