package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.web.exception.WeebException;
import com.web.mapper.*;
import com.web.model.Article;
import com.web.model.Message;
import com.web.service.*;
import com.web.service.impl.ArticleCommentServiceImpl;
import com.web.service.impl.ChatServiceImpl;
import com.web.vo.article.ArticleCommentVo;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real production mapper contracts against isolated fixture IDs in the disposable audit database. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommentReactionMySqlIntegrationTest {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate session;
    private DataSourceTransactionManager transactions;
    private MessageReactionMapper reactions;
    private ChatService chat;
    private ArticleCommentService comments;
    private MessageBroadcastService broadcasts;
    private MessageOutboxService outbox;
    private long authorId, receiverId, outsiderId, chatId, messageId, emptyMessageId, articleId, otherArticleId;

    @BeforeAll
    void loadProductionSchemaAndMapperConfiguration() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        try (var connection = dataSource.getConnection()) {
            for (String script : List.of("01_create_user_table.sql", "03_create_group_table.sql",
                    "04_create_shared_chat_table.sql", "06_create_message_table.sql", "27_create_message_reaction_table.sql",
                    "14_create_article_category_table.sql", "08_create_article_table.sql", "11_create_article_comment_table.sql")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/create/" + script));
            }
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setConfiguration(configuration);
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        session = new SqlSessionTemplate(java.util.Objects.requireNonNull(bean.getObject()));
        reactions = session.getMapper(MessageReactionMapper.class);
    }

    @BeforeEach
    void committedOwnedFixtures() {
        authorId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        receiverId = authorId + 1;
        outsiderId = authorId + 2;
        chatId = authorId + 3;
        messageId = authorId + 4;
        emptyMessageId = authorId + 5;
        articleId = authorId + 6;
        otherArticleId = authorId + 7;
        for (long userId : List.of(authorId, receiverId, outsiderId)) {
            jdbc.update("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                    userId, "reaction_" + userId, "fixture-hash", "reaction_" + userId + "@example.invalid", "USER");
        }
        jdbc.update("INSERT INTO shared_chat (id,chat_type,participant_1_id,participant_2_id) VALUES (?,'PRIVATE',?,?)",
                chatId, authorId, receiverId);
        for (long id : List.of(messageId, emptyMessageId)) {
            jdbc.update("INSERT INTO message (id,sender_id,receiver_id,chat_id,content) VALUES (?,?,?,?,?)",
                    id, authorId, receiverId, chatId, "{\"content\":\"reaction fixture\",\"contentType\":1}");
        }
        for (long id : List.of(articleId, otherArticleId)) {
            jdbc.update("INSERT INTO articles (article_id,user_id,article_title,status) VALUES (?,?,'comment fixture',2)",
                    id, authorId);
        }

        broadcasts = mock(MessageBroadcastService.class);
        outbox = mock(MessageOutboxService.class);
        var chatTarget = new ChatServiceImpl();
        ReflectionTestUtils.setField(chatTarget, "messageMapper", session.getMapper(MessageMapper.class));
        ReflectionTestUtils.setField(chatTarget, "messageReactionMapper", reactions);
        ReflectionTestUtils.setField(chatTarget, "messageBroadcastService", broadcasts);
        ReflectionTestUtils.setField(chatTarget, "messageOutboxService", outbox);
        ReflectionTestUtils.setField(chatTarget, "chatAccessService", new ChatAccessService(session.getMapper(ChatListMapper.class)));
        chat = transactional(chatTarget, ChatService.class);

        var commentTarget = new ArticleCommentServiceImpl();
        ReflectionTestUtils.setField(commentTarget, "articleCommentMapper", session.getMapper(ArticleCommentMapper.class));
        // Authorization uses the real article service in ArticleCommentAuthorizationTest; this boundary permits our fixtures.
        var articleService = mock(ArticleService.class);
        for (long id : List.of(articleId, otherArticleId)) {
            Article article = new Article();
            article.setArticleId(id);
            when(articleService.getArticleById(id)).thenReturn(article);
        }
        ReflectionTestUtils.setField(commentTarget, "articleService", articleService);
        comments = transactional(commentTarget, ArticleCommentService.class);
    }

    @AfterEach
    void deleteOnlyOwnedFixtures() {
        if (authorId == 0) return;
        jdbc.update("DELETE FROM message WHERE id IN (?,?)", messageId, emptyMessageId);
        jdbc.update("DELETE FROM shared_chat WHERE id=?", chatId);
        jdbc.update("DELETE FROM articles WHERE article_id IN (?,?)", articleId, otherArticleId);
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?,?)", authorId, receiverId, outsiderId);
    }

    @Test
    void reactionInsertUsesExistingTimestampAndHistoryMatchesBroadcastAggregate() {
        chat.addReaction(authorId, messageId, "👍");

        assertNotNull(jdbc.queryForObject("SELECT create_time FROM message_reaction WHERE message_id=?",
                java.sql.Timestamp.class, messageId));
        assertNotNull(reactions.findByMessageUserAndType(messageId, authorId, "👍").getCreatedAt());
        assertNotNull(reactions.findByMessageId(messageId).get(0).getCreatedAt());
        assertNotNull(reactions.selectByMessageIdAndUserId(messageId, authorId).get(0).getCreatedAt());
        Map<String, Object> expected = Map.of("emoji", "👍", "reactionType", "👍", "count", 1, "userIds", List.of(authorId));
        assertEquals(List.of(expected), historyMessage(messageId).getReactions());
        assertEquals(List.of(), historyMessage(emptyMessageId).getReactions());
        verify(outbox).enqueue(anyString(), eq("REACTION"), eq(chatId), eq(messageId), anyList(), argThat(event ->
                List.of(expected).equals(event.get("reactions")) && "add".equals(event.get("action"))));
    }

    @Test
    void participantToggleRemovesOnlyOwnReactionAndReloadKeepsOthers() {
        chat.addReaction(authorId, messageId, "👍");
        chat.addReaction(receiverId, messageId, "👍");
        assertEquals(2, historyMessage(messageId).getReactions().get(0).get("count"));

        chat.addReaction(authorId, messageId, "👍");
        var remaining = historyMessage(messageId).getReactions().get(0);
        assertEquals(1, remaining.get("count"));
        assertEquals(List.of(receiverId), remaining.get("userIds"));
        chat.addReaction(receiverId, messageId, "👍");
        assertEquals(List.of(), historyMessage(messageId).getReactions());
        assertEquals(0, reactions.countByMessageId(messageId));
    }

    @Test
    void outsiderCannotReactToOrLoadPrivateMessageAggregates() {
        assertThrows(AccessDeniedException.class, () -> chat.addReaction(outsiderId, messageId, "👍"));
        assertThrows(AccessDeniedException.class, () -> chat.getChatMessagesBySharedChatId(outsiderId, chatId, 1, 20));
        assertEquals(0, reactions.countByMessageId(messageId));
        verifyNoInteractions(broadcasts);
    }

    @Test
    void commentDeletionRequiresBothMatchingArticleAndOriginalAuthor() {
        Long commentId = comments.addComment(articleId, request(null), authorId);
        assertFalse(comments.deleteComment(otherArticleId, commentId, authorId));
        assertFalse(comments.deleteComment(articleId, commentId, receiverId));
        assertEquals(1, comments.getCommentCount(articleId));
        assertTrue(comments.deleteComment(articleId, commentId, authorId));
        assertEquals(0, comments.getCommentCount(articleId));
    }

    @Test
    void foreignParentCannotCreateCrossArticleCascadeButSameArticleReplyPersists() {
        Long parentId = comments.addComment(articleId, request(null), authorId);
        assertThrows(WeebException.class, () -> comments.addComment(otherArticleId, request(parentId), receiverId));
        assertEquals(0, comments.getCommentCount(otherArticleId));
        Long replyId = comments.addComment(articleId, request(parentId), receiverId);
        assertEquals(parentId, session.getMapper(ArticleCommentMapper.class).selectById(replyId).getParentId());
        assertEquals(2, comments.getCommentCount(articleId));
    }

    private Message historyMessage(long id) {
        return chat.getChatMessagesBySharedChatId(receiverId, chatId, 1, 20).stream()
                .filter(message -> message.getId().equals(id)).findFirst().orElseThrow();
    }

    private ArticleCommentVo request(Long parentId) {
        var request = new ArticleCommentVo();
        request.setContent("A valid comment fixture");
        request.setParentId(parentId);
        return request;
    }

    private <T> T transactional(Object target, Class<T> contract) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return contract.cast(proxy.getProxy());
    }
}
