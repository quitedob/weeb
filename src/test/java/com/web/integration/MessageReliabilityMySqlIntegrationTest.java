package com.web.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.mapper.*;
import com.web.model.Message;
import com.web.service.*;
import com.web.service.impl.ChatServiceImpl;
import com.web.vo.message.TextMessageContent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real SQL, independent connections and fault injection; removes only this test's random fixtures. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageReliabilityMySqlIntegrationTest {
    private JdbcTemplate jdbc;
    private SqlSessionTemplate session;
    private DataSourceTransactionManager transactions;
    private MessageOutboxService outbox;
    private MessageBroadcastService transport;
    private ChatService chat;
    private ChatUnreadCountService unread;
    private long actor, receiver, outsider, chatId, otherChat, groupChat, groupId;

    @BeforeAll
    void realProductionMappers() throws Exception {
        // The audit bootstrap extension migrates the dedicated database before any fixture is created.
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"), System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setConfiguration(configuration);
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
    }

    @BeforeEach
    void committedOwnedFixture() {
        actor = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        receiver = actor + 1; outsider = actor + 2; chatId = actor + 10; otherChat = actor + 11;
        groupChat = actor + 12; groupId = actor + 20;
        for (long id : List.of(actor, receiver, outsider)) {
            jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (?,?,?,?, 'USER',1)",
                    id, "delivery_" + id, "fixture-hash", "delivery_" + id + "@example.invalid");
        }
        privateChat(chatId, actor, receiver);
        privateChat(otherChat, actor, outsider);
        jdbc.update("INSERT INTO `group`(id,group_name,owner_id,status,shared_chat_id) VALUES (?,'delivery fixture',?,1,?)",
                groupId, actor, groupChat);
        jdbc.update("INSERT INTO shared_chat(id,chat_type,group_id) VALUES (?,'GROUP',?)", groupChat, groupId);
        for (long id : List.of(actor, receiver)) {
            jdbc.update("INSERT INTO group_member(group_id,user_id,role,join_status) VALUES (?,?,?,'ACCEPTED')",
                    groupId, id, id == actor ? 1 : 3);
            jdbc.update("INSERT INTO chat_list(id,user_id,shared_chat_id,type,group_id,unread_count,target_info) VALUES (?,?,?,'GROUP',?,0,JSON_OBJECT())",
                    groupChat + "_" + id, id, groupChat, groupId);
        }
        unread = new ChatUnreadCountService();
        ReflectionTestUtils.setField(unread, "jdbcTemplate", jdbc);
        transport = mock(MessageBroadcastService.class);
        outbox = new MessageOutboxService(jdbc, new ObjectMapper().findAndRegisterModules(), session.getMapper(MessageMapper.class),
                session.getMapper(UserMapper.class), new ChatAccessService(session.getMapper(ChatListMapper.class)), transport, transactions);
        ReflectionTestUtils.setField(outbox, "autoDispatch", false);
        chat = service(outbox);
    }

    private void privateChat(long id, long first, long second) {
        jdbc.update("INSERT INTO shared_chat(id,chat_type,participant_1_id,participant_2_id) VALUES (?,'PRIVATE',?,?)", id, first, second);
        for (long user : List.of(first, second)) {
            jdbc.update("INSERT INTO chat_list(id,user_id,shared_chat_id,type,target_id,unread_count,target_info) VALUES (?,?,?,'PRIVATE',?,0,JSON_OBJECT())",
                    id + "_" + user, user, id, user == first ? second : first);
        }
    }

    private ChatService service(MessageOutboxService publisher) {
        var target = new ChatServiceImpl();
        ReflectionTestUtils.setField(target, "messageMapper", session.getMapper(MessageMapper.class));
        ReflectionTestUtils.setField(target, "chatListMapper", session.getMapper(ChatListMapper.class));
        ReflectionTestUtils.setField(target, "userMapper", session.getMapper(UserMapper.class));
        ReflectionTestUtils.setField(target, "messageReactionMapper", session.getMapper(MessageReactionMapper.class));
        ReflectionTestUtils.setField(target, "chatAccessService", new ChatAccessService(session.getMapper(ChatListMapper.class)));
        ReflectionTestUtils.setField(target, "chatUnreadCountService", unread);
        ReflectionTestUtils.setField(target, "messageOutboxService", publisher);
        var preferences = mock(UserPreferencesService.class);
        when(preferences.canReceiveMessages(anyLong())).thenReturn(true);
        ReflectionTestUtils.setField(target, "preferencesService", preferences);
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (ChatService) proxy.getProxy();
    }

    @AfterEach
    void removeOnlyOwnedRows() {
        if (outbox != null) outbox.close();
        if (actor == 0) return;
        jdbc.update("DELETE FROM message_outbox WHERE chat_id IN (?,?,?)", chatId, otherChat, groupChat);
        jdbc.update("DELETE FROM message WHERE chat_id IN (?,?,?)", chatId, otherChat, groupChat);
        jdbc.update("DELETE FROM chat_unread_count WHERE chat_id IN (?,?,?)", chatId, otherChat, groupChat);
        jdbc.update("DELETE FROM chat_list WHERE shared_chat_id IN (?,?,?)", chatId, otherChat, groupChat);
        jdbc.update("DELETE FROM shared_chat WHERE id IN (?,?,?)", chatId, otherChat, groupChat);
        jdbc.update("DELETE FROM `group` WHERE id=?", groupId);
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?,?)", actor, receiver, outsider);
    }

    @Test
    void concurrentRetryHasOneMessageOneUnreadAndOneEventPerRecipient() throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        try {
            CountDownLatch ready = new CountDownLatch(6), start = new CountDownLatch(1);
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) futures.add(pool.submit(() -> {
                ready.countDown(); assertTrue(start.await(5, TimeUnit.SECONDS));
                return chat.sendMessageBySharedChatId(actor, chatId, message("same-request", "hello")).getId();
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            Set<Long> ids = new HashSet<>();
            for (Future<Long> future : futures) ids.add(future.get(15, TimeUnit.SECONDS));
            assertEquals(1, ids.size());
            assertEquals(1, countMessages());
            assertEquals(1, unread.getUnreadCount(receiver, chatId));
            assertEquals(2, countEvents("MESSAGE"));
            // A new service instance/transport retry returns the same database record without Redis.
            assertEquals(ids.iterator().next(), service(outbox).sendMessage(actor, chatId + "_" + actor,
                    message("same-request", "hello")).getId());
            assertEquals(1, unread.getUnreadCount(receiver, chatId));
            verifyNoInteractions(transport);
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    @Test
    void senderScopedKeysRejectChangedContentOrConversationAndAllowLegacyNulls() {
        Message first = chat.sendMessageBySharedChatId(actor, chatId, message("shared-key", "one"));
        assertThrows(IllegalArgumentException.class, () -> chat.sendMessageBySharedChatId(actor, chatId, message("shared-key", "two")));
        assertThrows(IllegalArgumentException.class, () -> chat.sendMessageBySharedChatId(actor, otherChat, message("shared-key", "one")));
        Message otherSender = chat.sendMessageBySharedChatId(receiver, chatId, message("shared-key", "one"));
        assertNotEquals(first.getId(), otherSender.getId());
        chat.sendMessageBySharedChatId(actor, chatId, message(null, "legacy"));
        chat.sendMessageBySharedChatId(actor, chatId, message(null, "legacy"));
        assertEquals(4, countMessages());
    }

    @Test
    void failureAfterOutboxInsertionRollsBackMessageUnreadAndEventsWithoutDispatch() {
        var failing = mock(MessageOutboxService.class);
        doAnswer(call -> {
            outbox.enqueueMessage(call.getArgument(0), call.getArgument(1));
            throw new IllegalStateException("injected precommit failure");
        }).when(failing).enqueueMessage(any(), anyList());
        assertThrows(IllegalStateException.class, () -> service(failing).sendMessageBySharedChatId(actor, chatId,
                message("rollback", "hello")));
        assertEquals(0, countMessages()); assertEquals(0, countEvents("MESSAGE"));
        assertEquals(0, unread.getUnreadCount(receiver, chatId));
        assertEquals(0, dispatchOwnedEvents());
        verifyNoInteractions(transport);
    }

    @Test
    void committedMessageSurvivesTransportFailureAndExpiredWorkerLease() {
        Message saved = chat.sendMessageBySharedChatId(actor, chatId, message("retry-dispatch", "hello"));
        Set<Object> uncertainDispatchIds = new HashSet<>();
        doAnswer(call -> {
            uncertainDispatchIds.add(((Map<?, ?>) call.getArgument(2)).get("eventId"));
            throw new IllegalStateException("dispatch outcome unknown");
        }).when(transport).dispatchOutboxEvent(anyString(), anyString(), anyMap());
        assertEquals(0, dispatchOwnedEvents());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox WHERE message_id=? AND delivered_at IS NULL",
                Integer.class, saved.getId()));
        assertEquals(1, unread.getUnreadCount(receiver, chatId));
        reset(transport);
        jdbc.update("UPDATE message_outbox SET available_at=NOW(),lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND),lease_token='dead-worker' "
                + "WHERE message_id=?", saved.getId());
        assertEquals(2, dispatchOwnedEvents());
        verify(transport, times(2)).dispatchOutboxEvent(anyString(), eq("MESSAGE"), argThat(payload ->
                ((Number) payload.get("id")).longValue() == saved.getId()
                        && uncertainDispatchIds.contains(payload.get("eventId"))));
        assertEquals(1, countMessages()); assertEquals(1, unread.getUnreadCount(receiver, chatId));
    }

    @Test
    void dispatchRechecksRemovedGroupMember() {
        Message saved = chat.sendMessageBySharedChatId(actor, groupChat, message("group-dispatch", "group hello"));
        jdbc.update("UPDATE group_member SET kicked_at=NOW() WHERE group_id=? AND user_id=?", groupId, receiver);
        assertEquals(2, dispatchOwnedEvents());
        verify(transport, never()).dispatchOutboxEvent(eq("delivery_" + receiver), anyString(), anyMap());
        assertEquals("access revoked", jdbc.queryForObject("SELECT last_error FROM message_outbox WHERE message_id=? AND recipient_id=?",
                String.class, saved.getId(), receiver));
    }

    @Test
    void recipientDatastoreFailureRemainsPendingInsteadOfBeingClassifiedAsRevoked() {
        Message saved = chat.sendMessageBySharedChatId(actor, chatId, message("storage-retry", "hello"));
        UserMapper failingUsers = mock(UserMapper.class);
        when(failingUsers.selectById(anyLong())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("injected outage"));
        var worker = new MessageOutboxService(jdbc, new ObjectMapper().findAndRegisterModules(), session.getMapper(MessageMapper.class),
                failingUsers, new ChatAccessService(session.getMapper(ChatListMapper.class)), transport, transactions);
        ReflectionTestUtils.setField(worker, "autoDispatch", false);
        try {
            assertEquals(0, worker.dispatchDue());
            verifyNoInteractions(transport);
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox WHERE message_id=? AND delivered_at IS NULL",
                    Integer.class, saved.getId()));
            jdbc.update("UPDATE message_outbox SET available_at=NOW() WHERE message_id=?", saved.getId());
            assertEquals(2, dispatchOwnedEvents());
            verify(transport, times(2)).dispatchOutboxEvent(anyString(), eq("MESSAGE"), anyMap());
        } finally { worker.close(); }
    }

    @Test
    void publishBeforeCompletionFailureRetriesSameEventWithoutRepeatingMessageOrUnread() {
        Message saved = chat.sendMessageBySharedChatId(actor, chatId, message("after-publish", "hello"));
        var failOnce = new java.util.concurrent.atomic.AtomicBoolean(true);
        JdbcTemplate completionFailure = new JdbcTemplate(Objects.requireNonNull(jdbc.getDataSource())) {
            @Override public int update(String sql, Object... args) {
                if (sql.startsWith("UPDATE message_outbox SET delivered_at=") && failOnce.compareAndSet(true, false)) {
                    throw new org.springframework.dao.DataAccessResourceFailureException("injected post-publish failure");
                }
                return super.update(sql, args);
            }
        };
        List<Object> publishedIds = new ArrayList<>();
        doAnswer(call -> { publishedIds.add(((Map<?, ?>) call.getArgument(2)).get("eventId")); return null; })
                .when(transport).dispatchOutboxEvent(anyString(), anyString(), anyMap());
        var worker = new MessageOutboxService(completionFailure, new ObjectMapper().findAndRegisterModules(),
                session.getMapper(MessageMapper.class), session.getMapper(UserMapper.class),
                new ChatAccessService(session.getMapper(ChatListMapper.class)), transport, transactions);
        ReflectionTestUtils.setField(worker, "autoDispatch", false);
        try {
            assertEquals(1, worker.dispatchDue());
            assertEquals(2, publishedIds.size());
            Object uncertainEvent = publishedIds.get(0);
            jdbc.update("UPDATE message_outbox SET available_at=NOW() WHERE message_id=?", saved.getId());
            assertEquals(1, dispatchOwnedEvents());
            assertEquals(List.of(uncertainEvent, publishedIds.get(1), uncertainEvent), publishedIds);
            assertEquals(1, countMessages()); assertEquals(1, unread.getUnreadCount(receiver, chatId));
        } finally { worker.close(); }
    }

    @Test
    void delayedReadAcknowledgementNeverClearsNewerMessagesOrMovesCursorBackwards() {
        Message first = chat.sendMessageBySharedChatId(actor, chatId, message("read-first", "one"));
        Message second = chat.sendMessageBySharedChatId(actor, chatId, message("read-second", "two"));
        var initial = chat.markAsReadBySharedChatId(receiver, chatId, first.getId());
        assertEquals(first.getId(), initial.get("lastReadMessageId"));
        assertEquals(1, initial.get("unreadCount"));
        assertEquals(Message.STATUS_SENT, session.getMapper(MessageMapper.class).selectMessageById(second.getId()).getStatus());
        chat.markAsReadBySharedChatId(receiver, chatId, second.getId());
        var stale = chat.markAsReadBySharedChatId(receiver, chatId, first.getId());
        assertEquals(second.getId(), stale.get("lastReadMessageId"));
        Message third = chat.sendMessageBySharedChatId(actor, chatId, message("read-third", "three"));
        chat.markAsReadBySharedChatId(receiver, chatId, first.getId());
        assertEquals(1, unread.getUnreadCount(receiver, chatId));
        assertEquals(Message.STATUS_SENT, session.getMapper(MessageMapper.class).selectMessageById(third.getId()).getStatus());
        Message foreign = chat.sendMessageBySharedChatId(actor, otherChat, message("foreign", "hidden"));
        assertThrows(AccessDeniedException.class, () -> chat.markAsReadBySharedChatId(receiver, chatId, foreign.getId()));
        assertEquals(1, unread.getUnreadCount(receiver, chatId));
    }

    @Test
    void concurrentSendAndBoundedReadKeepNewMessageUnread() throws Exception {
        Message seen = chat.sendMessageBySharedChatId(actor, chatId, message("seen", "seen"));
        var pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> read = pool.submit(() -> { await(start); chat.markAsReadBySharedChatId(receiver, chatId, seen.getId()); });
            Future<?> send = pool.submit(() -> { await(start); chat.sendMessageBySharedChatId(actor, chatId, message("new", "unseen")); });
            start.countDown(); read.get(10, TimeUnit.SECONDS); send.get(10, TimeUnit.SECONDS);
            assertEquals(1, unread.getUnreadCount(receiver, chatId));
            assertEquals(1, unread.getTotalUnreadCount(receiver));
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    @Test
    void cursorSyncRecoversCommittedMessagesWithoutRedisOrDispatchAndRejectsOutsider() {
        Message first = chat.sendMessageBySharedChatId(actor, chatId, message("sync1", "one"));
        Message recalled = chat.sendMessageBySharedChatId(actor, chatId, message("sync2", "two"));
        Message third = chat.sendMessageBySharedChatId(actor, chatId, message("sync3", "three"));
        chat.recallMessage(actor, recalled.getId());
        var page = chat.syncMessages(receiver, chatId, 0L, 1);
        assertEquals(List.of(first.getId()), pageIds(page)); assertEquals(true, page.get("hasMore"));
        var next = chat.syncMessages(receiver, chatId, (Long) page.get("nextAfterMessageId"), 1);
        assertEquals(List.of(third.getId()), pageIds(next)); assertEquals(false, next.get("hasMore"));
        assertThrows(AccessDeniedException.class, () -> chat.syncMessages(outsider, chatId, 0L, 20));
        verifyNoInteractions(transport);
    }

    @Test
    void targetStateReactionRetriesAreIdempotentAndVersionsAdvanceOnlyOnChange() {
        Message saved = chat.sendMessageBySharedChatId(actor, chatId, message("reaction", "hello"));
        var first = chat.setReaction(receiver, saved.getId(), "thumb", true);
        var retry = chat.setReaction(receiver, saved.getId(), "thumb", true);
        assertEquals(1L, first.get("reactionVersion")); assertEquals(first, retry);
        assertEquals(2, countEvents("REACTION"));
        var removed = chat.setReaction(receiver, saved.getId(), "thumb", false);
        assertEquals(2L, removed.get("reactionVersion")); assertEquals(List.of(), removed.get("reactions"));
        assertEquals(removed, chat.setReaction(receiver, saved.getId(), "thumb", false));
        assertEquals(4, countEvents("REACTION"));
        assertEquals(2L, chat.getChatMessagesBySharedChatId(actor, chatId, 1, 20).get(0).getReactionVersion());
        assertThrows(AccessDeniedException.class, () -> chat.setReaction(outsider, saved.getId(), "thumb", true));
    }

    @Test
    void stateRefreshRecoversOldReadsReactionsAndRecallWithoutReturningForeignChatData() {
        Message first = chat.sendMessageBySharedChatId(actor, chatId, message("old-state", "hello"));
        Message recalled = chat.sendMessageBySharedChatId(actor, chatId, message("old-recall", "remove"));
        Message foreign = chat.sendMessageBySharedChatId(actor, otherChat, message("foreign-state", "hidden"));
        chat.markAsReadBySharedChatId(receiver, chatId, first.getId());
        chat.setReaction(receiver, first.getId(), "thumb", true);
        chat.recallMessage(actor, recalled.getId());
        List<Message> states = chat.getMessageStates(actor, chatId, List.of(first.getId(), recalled.getId(), foreign.getId()));
        assertEquals(List.of(first.getId(), recalled.getId()), states.stream().map(Message::getId).toList());
        assertEquals(Message.STATUS_READ, states.get(0).getStatus());
        assertEquals(1L, states.get(0).getReactionVersion());
        assertEquals(1, states.get(0).getReactions().size());
        assertEquals(1, states.get(1).getIsRecalled());
        assertThrows(AccessDeniedException.class, () -> chat.getMessageStates(outsider, chatId, List.of(first.getId())));
        assertThrows(IllegalArgumentException.class, () -> chat.getMessageStates(actor, chatId, Collections.nCopies(101, first.getId())));
        verifyNoInteractions(transport);
    }

    private int dispatchOwnedEvents() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox WHERE delivered_at IS NULL AND chat_id NOT IN (?,?,?)",
                Integer.class, chatId, otherChat, groupChat), "Do not dispatch another fixture's pending work");
        return outbox.dispatchDue();
    }
    private int countMessages() { return jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE chat_id IN (?,?,?)", Integer.class, chatId, otherChat, groupChat); }
    private int countEvents(String type) { return jdbc.queryForObject("SELECT COUNT(*) FROM message_outbox WHERE chat_id IN (?,?,?) AND event_type=?", Integer.class, chatId, otherChat, groupChat, type); }
    private Message message(String key, String content) {
        Message message = new Message(); message.setClientMessageId(key); message.setMessageType(1);
        TextMessageContent body = new TextMessageContent(); body.setContent(content); message.setContent(body); return message;
    }
    @SuppressWarnings("unchecked") private List<Long> pageIds(Map<String, Object> page) { return ((List<Message>) page.get("list")).stream().map(Message::getId).toList(); }
    private static void await(CountDownLatch latch) { try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); } }
}
