package com.web.integration;

import com.web.service.MessageSearchService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Production SQL against random fixtures rolled back after every test; no shared data is deleted. */
@EnabledIfEnvironmentVariable(named = "WEEB_TEST_MYSQL_URL", matches = ".+weeb_audit.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageSearchRelevanceMySqlIntegrationTest {
    private JdbcTemplate jdbc;
    private MessageSearchService search;
    private DataSourceTransactionManager transactions;
    private TransactionStatus transaction;
    private long actor, receiver, outsider, privateChat, hiddenChat, groupChat, groupId, nextMessage;

    @BeforeAll
    void productionDatabaseAndSchema() throws Exception {
        var dataSource = new DriverManagerDataSource(System.getenv("WEEB_TEST_MYSQL_URL"),
                System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME", "root"),
                System.getenv("WEEB_TEST_MYSQL_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        transactions = new DataSourceTransactionManager(dataSource);
        search = new MessageSearchService(new NamedParameterJdbcTemplate(dataSource));
        try (var connection = dataSource.getConnection()) {
            for (String script : List.of("01_create_user_table.sql", "03_create_group_table.sql",
                    "04_create_shared_chat_table.sql", "06_create_message_table.sql", "07_create_group_member_table.sql")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/create/" + script));
            }
        }
    }

    @BeforeEach
    void beginIsolatedFixtureTransaction() {
        transaction = transactions.getTransaction(new DefaultTransactionDefinition());
        actor = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, 8_000_000_000_000L);
        receiver = actor + 1;
        outsider = actor + 2;
        privateChat = actor + 10;
        hiddenChat = actor + 11;
        groupChat = actor + 12;
        groupId = actor + 20;
        nextMessage = actor + 100;
        for (long id : List.of(actor, receiver, outsider)) {
            String name = (id == actor ? "z_search_" : "a_search_") + id;
            jdbc.update("INSERT INTO `user` (id,username,password,user_email,type,status) VALUES (?,?,?,?,?,1)",
                    id, name, "test-hash", name + "@example.invalid", "USER");
        }
        jdbc.update("INSERT INTO shared_chat (id,chat_type,participant_1_id,participant_2_id) VALUES (?,'PRIVATE',?,?)",
                privateChat, actor, receiver);
        jdbc.update("INSERT INTO shared_chat (id,chat_type,participant_1_id,participant_2_id) VALUES (?,'PRIVATE',?,?)",
                hiddenChat, receiver, outsider);
        jdbc.update("INSERT INTO `group` (id,group_name,owner_id,status,shared_chat_id) VALUES (?,'search fixture',?,1,?)",
                groupId, receiver, groupChat);
        jdbc.update("INSERT INTO shared_chat (id,chat_type,group_id) VALUES (?,'GROUP',?)", groupChat, groupId);
        jdbc.update("INSERT INTO group_member (group_id,user_id,role,join_status) VALUES (?,?,3,'ACCEPTED')", groupId, actor);
    }

    @AfterEach
    void rollbackOnlyThisTransaction() {
        if (transaction != null && !transaction.isCompleted()) transactions.rollback(transaction);
    }

    @Test
    void relevancePrefersExactThenPhraseThenAllTermsOverNewerPartialMatches() {
        long exact = message(privateChat, actor, "alpha beta", 1);
        long phrase = message(privateChat, actor, "prefix alpha beta suffix", 2);
        long allTerms = message(privateChat, actor, "beta appears before ALPHA", 4);
        long partial = message(privateChat, actor, "alpha only", 5);
        message(privateChat, actor, "unrelated", 6);

        var result = results("\u00a0ALPHA\t beta\u00a0", "relevance", 0, 20);
        assertEquals(List.of(exact, phrase, allTerms, partial), ids(result));
        assertEquals(4L, result.get("total"));
        assertEquals(List.of(partial, allTerms, phrase, exact), ids(results("alpha beta", "time_desc", 0, 20)));
        assertEquals(List.of(exact, phrase, allTerms, partial), ids(results("alpha beta", "time_asc", 0, 20)));
    }

    @Test
    void tokenAlternativesNeverBypassCurrentPrivateOrGroupMembershipOrRecall() {
        long allowed = message(privateChat, actor, "alpha", 1);
        message(hiddenChat, outsider, "beta", 5);
        long recalled = message(privateChat, actor, "alpha beta", 6);
        jdbc.update("UPDATE message SET is_recalled=1 WHERE id=?", recalled);
        long groupMessage = message(groupChat, receiver, "alpha beta", 2);

        assertEquals(List.of(groupMessage, allowed), ids(results("alpha beta", "relevance", 0, 20)));
        jdbc.update("UPDATE group_member SET kicked_at=CURRENT_TIMESTAMP WHERE group_id=? AND user_id=?", groupId, actor);
        var result = results("alpha beta", "relevance", 0, 20);
        assertEquals(List.of(allowed), ids(result));
        assertEquals(1L, result.get("total"));
        jdbc.update("UPDATE group_member SET kicked_at=NULL,join_status='PENDING' WHERE group_id=? AND user_id=?", groupId, actor);
        assertEquals(List.of(allowed), ids(results("alpha beta", "relevance", 0, 20)));
    }

    @Test
    void relevancePaginationUsesStableTimeAndIdTiesWithoutLosingTotals() {
        long exact = message(privateChat, actor, "alpha beta", 1);
        long firstPartial = message(privateChat, actor, "alpha only", 2);
        long secondPartial = message(privateChat, actor, "beta only", 2);
        long thirdPartial = message(privateChat, actor, "some alpha", 2);

        var pageOne = results("alpha beta", "relevance", 0, 2);
        var pageTwo = results("alpha beta", "relevance", 1, 2);
        assertEquals(List.of(exact, thirdPartial), ids(pageOne));
        assertEquals(List.of(secondPartial, firstPartial), ids(pageTwo));
        assertEquals(4L, pageOne.get("total"));
        assertEquals(4L, pageTwo.get("total"));
        var beyondLastPage = results("alpha beta", "relevance", 2, 2);
        assertEquals(List.of(), ids(beyondLastPage));
        assertEquals(4L, beyondLastPage.get("total"));
    }

    @Test
    void repeatedTokensDoNotOutweighDistinctMatchesAndWildcardsStayLiteral() {
        assertThrows(IllegalArgumentException.class, () -> results("\u00a0 \u00a0", "relevance", 0, 20));
        long both = message(privateChat, actor, "beta then alpha", 1);
        message(privateChat, actor, "alpha only", 3);
        assertEquals(both, ids(results("alpha ALPHA beta", "relevance", 0, 20)).get(0));

        long literal = message(privateChat, actor, "O'Reilly 10%_!", 4);
        message(privateChat, actor, "10anything", 5);
        assertEquals(List.of(literal), ids(results("10%_!", "relevance", 0, 20)));
        assertEquals(List.of(literal), ids(results("O'Reilly", "relevance", 0, 20)));
    }

    @Test
    void senderSortingAndCombinedFiltersArePreservedForTokenSearch() {
        long senderZ = message(privateChat, actor, "alpha", 1);
        long senderA = message(privateChat, receiver, "beta", 2);
        assertEquals(List.of(senderA, senderZ), ids(results("alpha beta", "username_asc", 0, 20)));
        assertEquals(List.of(senderZ, senderA), ids(results("alpha beta", "username_desc", 0, 20)));

        long groupMessage = message(groupChat, receiver, "beta then alpha", 3);
        var filtered = search.search(actor, "alpha beta", 0, 10, "2026-01-03", "2026-01-03",
                "1", Long.toString(receiver), Long.toString(groupId), "relevance");
        assertEquals(List.of(groupMessage), ids(filtered));
        assertEquals(1L, filtered.get("total"));
    }

    @Test
    void duplicateLegacyGroupMappingPreservesDisplayRowsWithoutInflatingMessageTotal() {
        long alias = groupId + 1;
        jdbc.update("INSERT INTO `group` (id,group_name,owner_id,status,shared_chat_id) VALUES (?,'legacy alias',?,1,?)",
                alias, receiver, groupChat);
        jdbc.update("INSERT INTO group_member (group_id,user_id,role,join_status) VALUES (?,?,3,'ACCEPTED')", alias, actor);
        long groupMessage = message(groupChat, receiver, "alpha beta", 2);
        long privateMessage = message(privateChat, actor, "alpha", 3);
        var result = results("alpha beta", "relevance", 0, 20);
        assertEquals(List.of(groupMessage, groupMessage, privateMessage), ids(result));
        assertEquals(2L, result.get("total"), "Two visible group labels must not count one message twice");
        for (int page = 0; page < 3; page++) {
            var singleRow = results("alpha beta", "relevance", page, 1);
            assertEquals(1, ids(singleRow).size());
            assertEquals(2L, singleRow.get("total"), "A display alias must not inflate the window total");
        }
        var aliasPagePastEnd = results("alpha beta", "relevance", 3, 1);
        assertEquals(List.of(), ids(aliasPagePastEnd));
        assertEquals(2L, aliasPagePastEnd.get("total"));
        var filtered = search.search(actor, "alpha beta", 0, 10, null, null, null, null, Long.toString(alias), "relevance");
        assertEquals(List.of(groupMessage), ids(filtered)); assertEquals(1L, filtered.get("total"));
        @SuppressWarnings("unchecked") var rows = (List<Map<String, Object>>) filtered.get("list");
        assertEquals(alias, rows.get(0).get("groupId"));
        jdbc.update("UPDATE group_member SET join_status='PENDING' WHERE group_id=? AND user_id=?", alias, actor);
        assertEquals(List.of(groupMessage, privateMessage), ids(results("alpha beta", "relevance", 0, 20)));
        assertEquals(0L, search.search(actor, "alpha beta", 0, 10, null, null, null, null, Long.toString(alias), "relevance").get("total"));
    }

    @Test
    void usernameSortRetainsMessagesWithMissingLegacySenderMetadata() {
        long senderZ = message(privateChat, actor, "alpha", 1);
        long senderA = message(privateChat, receiver, "alpha", 2);
        long orphan;
        // Simulate an already orphaned legacy row using only this transaction's connection; restore checks immediately.
        jdbc.execute("SET SESSION FOREIGN_KEY_CHECKS=0");
        try { orphan = message(privateChat, actor + 999, "alpha", 3); }
        finally { jdbc.execute("SET SESSION FOREIGN_KEY_CHECKS=1"); }
        var ascending = results("alpha", "username_asc", 0, 20);
        assertEquals(List.of(orphan, senderA, senderZ), ids(ascending));
        assertEquals(3L, ascending.get("total"));
        assertEquals(List.of(senderZ, senderA, orphan), ids(results("alpha", "username_desc", 0, 20)));
        @SuppressWarnings("unchecked") var rows = (List<Map<String, Object>>) ascending.get("list");
        assertNull(rows.get(0).get("senderName"));
        assertEquals(List.of(orphan, senderA, senderZ), ids(results("alpha", "time_desc", 0, 20)));
    }

    @Test
    void authorizedChatSetReturnsOnlyFewVisibleRowsAndAnEmptySetHasZeroTotal() {
        long allowed = message(privateChat, actor, "alpha", 1);
        long groupMessage = message(groupChat, receiver, "alpha", 2);
        for (int i = 0; i < 30; i++) message(hiddenChat, outsider, "alpha", 3);
        var few = results("alpha", "relevance", 0, 20);
        assertEquals(List.of(groupMessage, allowed), ids(few)); assertEquals(2L, few.get("total"));
        long nobody = actor + 3;
        jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (?,?,?,?,'USER',1)",
                nobody, "no_chats_" + nobody, "fixture-hash", "no_chats_" + nobody + "@example.invalid");
        var empty = search.search(nobody, "alpha", 0, 20, null, null, null, null, null, "relevance");
        assertEquals(List.of(), ids(empty)); assertEquals(0L, empty.get("total"));
        jdbc.update("UPDATE `group` SET status=0 WHERE id=?", groupId);
        assertEquals(List.of(allowed), ids(results("alpha", "relevance", 0, 20)));
        assertEquals(1L, results("alpha", "relevance", 0, 20).get("total"));
    }

    @Test
    void storedTextRetainsOriginalUnicodeAndLiteralMatchingAndRanking() {
        long accented = message(privateChat, actor, "CAFÉ", 1);
        message(privateChat, actor, "café", 1);
        long plain = message(privateChat, actor, "CAFE", 1);
        long decomposed = message(privateChat, actor, "cafe\u0301", 1);
        message(privateChat, actor, "prefix café suffix", 1);
        for (String text : List.of("👍", "👍🏽", "10%_!", "10anything", "ΩΜΕΓΑ", "ωμέγα")) {
            message(privateChat, actor, text, 1);
        }
        String original = "LOWER(JSON_UNQUOTE(JSON_EXTRACT(content, '$.content')))";
        for (String query : List.of("CAFÉ", "CAFE", "cafe\u0301", "👍", "10%_!", "ΩΜΕΓΑ")) {
            String pattern = "%" + query.toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            var expected = jdbc.queryForList("SELECT id FROM message WHERE chat_id=? AND COALESCE(is_recalled,0)=0 AND "
                            + original + " LIKE ? ESCAPE '!' ORDER BY CASE WHEN " + original
                            + "=LOWER(?) THEN 2 WHEN " + original
                            + " LIKE LOWER(?) ESCAPE '!' THEN 1 ELSE 0 END DESC,created_at DESC,id DESC",
                    Long.class, privateChat, pattern, query, pattern);
            assertFalse(expected.isEmpty(), "The legacy expression must exercise matches for " + query);
            var actual = results(query, "relevance", 0, 20);
            assertEquals(expected, ids(actual), "Stored text must retain the original expression's matching and ranking for " + query);
            assertEquals((long) expected.size(), actual.get("total"));
        }
        assertFalse(ids(results("CAFE", "relevance", 0, 20)).contains(accented), "Accents remain significant");
        assertTrue(ids(results("CAFE", "relevance", 0, 20)).contains(plain));
        assertEquals(List.of(decomposed), ids(results("cafe\u0301", "relevance", 0, 20)),
                "Composed and decomposed forms retain their original binary comparison behavior");
    }

    @Test
    void storedTextPreservesNullAndMissingContentAndRefreshesOnEveryEdit() {
        long changed = message(privateChat, actor, "OldNeedle", 1);
        long jsonNull = message(privateChat, actor, null, 1);
        long missing = message(privateChat, actor, "discarded", 1);
        jdbc.update("UPDATE message SET content=JSON_OBJECT('url','fixture.invalid/image') WHERE id=?", missing);
        assertEquals(List.of(jsonNull), ids(results("null", "relevance", 0, 20)),
                "JSON null remains the searchable literal null while an absent content field stays SQL NULL");
        assertEquals(List.of(changed), ids(results("OldNeedle", "relevance", 0, 20)));

        String updated = "長".repeat(30_000) + " NewNeedle 👍";
        jdbc.update("UPDATE message SET content=JSON_OBJECT('content',?) WHERE id=?", updated, changed);
        assertEquals(List.of(), ids(results("OldNeedle", "relevance", 0, 20)));
        var refreshed = results("NewNeedle", "relevance", 0, 20);
        assertEquals(List.of(changed), ids(refreshed));
        @SuppressWarnings("unchecked") var rows = (List<Map<String, Object>>) refreshed.get("list");
        assertEquals(updated, rows.get(0).get("content"), "Display content and search text must not be truncated");
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE chat_id=? AND NOT "
                        + "(search_text <=> LOWER(JSON_UNQUOTE(JSON_EXTRACT(content,'$.content'))))",
                Long.class, privateChat));
        jdbc.update("UPDATE message SET is_recalled=1 WHERE id=?", changed);
        var recalled = results("NewNeedle", "relevance", 0, 20);
        assertEquals(List.of(), ids(recalled));
        assertEquals(0L, recalled.get("total"));
    }

    @Test
    void nonemptyPagesReadWindowTotalAndEmptyPagesStillCountExactly() {
        var recordingJdbc = spy(new NamedParameterJdbcTemplate(jdbc.getDataSource()));
        var countedSearch = new MessageSearchService(recordingJdbc);
        long first = message(privateChat, actor, "windowneedle", 1);
        long second = message(privateChat, actor, "windowneedle", 2);
        var firstPage = countedSearch.search(actor, "windowneedle", 0, 1, null, null, null, null, null, "relevance");
        assertEquals(List.of(second), ids(firstPage));
        assertEquals(2L, firstPage.get("total"));
        verify(recordingJdbc, never()).queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));

        var ascendingPage = countedSearch.search(actor, "windowneedle", 0, 1, null, null, null, null, null, "time_asc");
        assertEquals(List.of(first), ids(ascendingPage));
        assertEquals(2L, ascendingPage.get("total"));
        verify(recordingJdbc, never()).queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));

        var pastEnd = countedSearch.search(actor, "windowneedle", 2, 1, null, null, null, null, null, "relevance");
        assertEquals(List.of(), ids(pastEnd));
        assertEquals(2L, pastEnd.get("total"));
        verify(recordingJdbc).queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));

        var noMatches = countedSearch.search(actor, "absentneedle", 0, 1, null, null, null, null, null, "relevance");
        assertEquals(List.of(), ids(noMatches));
        assertEquals(0L, noMatches.get("total"));
        verify(recordingJdbc, times(2)).queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class));
    }

    private long message(long chatId, long sender, String content, int day) {
        long id = nextMessage++;
        jdbc.update("INSERT INTO message (id,sender_id,chat_id,content,created_at) VALUES (?,?,?,JSON_OBJECT('content',?),?)",
                id, sender, chatId, content, Timestamp.valueOf(String.format("2026-01-%02d 12:00:00", day)));
        return id;
    }

    private Map<String, Object> results(String keyword, String sort, int page, int size) {
        return search.search(actor, keyword, page, size, null, null, null, null, null, sort);
    }

    @SuppressWarnings("unchecked")
    private List<Long> ids(Map<String, Object> result) {
        return ((List<Map<String, Object>>) result.get("list")).stream()
                .map(message -> ((Number) message.get("id")).longValue()).toList();
    }
}
