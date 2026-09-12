package com.web.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Message search uses current SQL membership, including for pagination and totals.
 * Whitespace separates case-insensitive literal substring terms; any term can match. Relevance prefers
 * the complete query, then its contiguous phrase, then more distinct matching terms.
 * Equal relevance uses newest message time and ID. This does not use ES stemming.
 */
@Service
@Transactional(readOnly = true)
public class MessageSearchService {
    public static final int MAX_KEYWORD_LENGTH = 100;
    public static final int MAX_DISTINCT_TERMS = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_RESULT_WINDOW = 10_000;
    private final NamedParameterJdbcTemplate jdbc;

    public MessageSearchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String CONTENT = "JSON_UNQUOTE(JSON_EXTRACT(m.content, '$.content'))";
    private static final String MATCH_CONTENT = "m.search_text";

    /** UNION materializes current access once per chat; duplicate legacy group mappings remain visible. */
    private static String visibleChats(boolean details, boolean groupFiltered) {
        String privateColumns = details ? ", 'PRIVATE' AS chat_type, NULL AS group_id" : "";
        String groupColumns = details ? ", 'GROUP' AS chat_type, g.id AS group_id" : "";
        return "SELECT sc.id AS chat_id" + privateColumns + " FROM shared_chat sc "
                + "WHERE sc.chat_type = 'PRIVATE' AND (sc.participant_1_id = :actor OR sc.participant_2_id = :actor) "
                + (groupFiltered ? "AND 1 = 0 " : "")
                + (details ? "UNION ALL " : "UNION ")
                + "SELECT sc.id AS chat_id" + groupColumns + " FROM group_member gm "
                + "JOIN `group` g ON g.id = gm.group_id JOIN shared_chat sc ON sc.id = g.shared_chat_id "
                + "WHERE gm.user_id = :actor AND gm.join_status = 'ACCEPTED' AND gm.kicked_at IS NULL "
                + "AND g.status = 1 AND sc.chat_type = 'GROUP' "
                + (groupFiltered ? "AND g.id IN (:groups) " : "");
    }

    public Map<String, Object> search(Long userId, String keyword, int page, int size,
                                      String startDate, String endDate, String messageTypes,
                                      String userIds, String groupIds, String sortBy) {
        validateMessageRequest(userId, keyword, page, size);
        String sort = sortBy == null ? "relevance" : sortBy;
        String order = switch (sort) {
            case "relevance" -> "match_quality DESC, matched_terms DESC, m.created_at DESC, m.id DESC";
            case "time_desc" -> "m.created_at DESC, m.id DESC";
            case "time_asc" -> "m.created_at ASC, m.id ASC";
            case "username_asc" -> "sender_sort_name ASC, m.created_at DESC, m.id DESC";
            case "username_desc" -> "sender_sort_name DESC, m.created_at DESC, m.id DESC";
            default -> throw new IllegalArgumentException("Invalid message sort order");
        };
        boolean relevance = "relevance".equals(sort);
        boolean usernameOrder = sort.startsWith("username_");
        LocalDate start = date(startDate);
        LocalDate end = date(endDate);
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not follow end date");
        }
        List<Long> types = ids(messageTypes);
        if (types.stream().anyMatch(type -> type > 3)) throw new IllegalArgumentException("Invalid message type");
        List<Long> senders = ids(userIds);
        List<Long> groups = ids(groupIds);
        String phrase = keyword.replaceAll("(?U)\\s+", " ").strip();
        if (phrase.isEmpty()) throw new IllegalArgumentException("Search keyword must contain a non-whitespace term");
        MapSqlParameterSource params = new MapSqlParameterSource("actor", userId)
                .addValue("exactKeyword", phrase)
                .addValue("keyword", "%" + escapeLike(phrase) + "%")
                .addValue("size", size).addValue("offset", (long) page * size);
        List<String> conditions = new ArrayList<>();
        List<String> termScores = new ArrayList<>();
        for (String term : new LinkedHashSet<>(List.of(phrase.toLowerCase(Locale.ROOT).split(" ")))) {
            String parameter = "term" + conditions.size();
            params.addValue(parameter, "%" + escapeLike(term) + "%");
            String condition = MATCH_CONTENT + " LIKE :" + parameter + " ESCAPE '!'";
            conditions.add(condition);
            termScores.add("CASE WHEN " + condition + " THEN 1 ELSE 0 END");
        }
        StringBuilder where = new StringBuilder(" WHERE COALESCE(m.is_recalled, 0) = 0 AND (")
                .append(String.join(" OR ", conditions)).append(") ");
        if (start != null) {
            where.append(" AND m.created_at >= :start ");
            params.addValue("start", Timestamp.valueOf(start.atStartOfDay()));
        }
        if (end != null) {
            where.append(" AND m.created_at < :end ");
            params.addValue("end", Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
        }
        filter(where, params, "m.message_type", "types", types);
        filter(where, params, "m.sender_id", "senders", senders);
        if (!groups.isEmpty()) params.addValue("groups", groups);
        // The counting chat set is unique, so each message is counted once without metadata joins or DISTINCT message IDs.
        String countFrom = " FROM (" + visibleChats(false, !groups.isEmpty())
                + ") visible JOIN message m ON m.chat_id = visible.chat_id ";
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + countFrom + where, params, Long.class);
        String scores = relevance ? ", CASE WHEN " + MATCH_CONTENT + " = LOWER(:exactKeyword) THEN 2 WHEN "
                + MATCH_CONTENT + " LIKE LOWER(:keyword) ESCAPE '!' THEN 1 ELSE 0 END AS match_quality, ("
                + String.join(" + ", termScores) + ") AS matched_terms" : "";
        // Rank only IDs and small sort columns. Content and display metadata are fetched for the requested page.
        String candidates = "SELECT m.id, m.sender_id, m.created_at, m.message_type, visible.chat_type, "
                + "visible.chat_id AS shared_chat_id, visible.group_id" + scores
                + (usernameOrder ? ", sort_sender.username AS sender_sort_name" : "")
                + " FROM (" + visibleChats(true, !groups.isEmpty()) + ") visible JOIN message m ON m.chat_id = visible.chat_id "
                + (usernameOrder ? "LEFT JOIN `user` sort_sender ON sort_sender.id = m.sender_id " : "")
                + where + " ORDER BY " + order + ", visible.group_id ASC LIMIT :size OFFSET :offset";
        String outerOrder = order.replace("m.", "ranked.").replace("match_quality", "ranked.match_quality")
                .replace("matched_terms", "ranked.matched_terms").replace("sender_sort_name", "ranked.sender_sort_name");
        String select = "SELECT ranked.id, ranked.sender_id, sender.username AS sender_name, "
                + CONTENT + " AS message_content, ranked.created_at, ranked.message_type, ranked.chat_type, "
                + "ranked.shared_chat_id, ranked.group_id, display_group.group_name FROM (" + candidates + ") ranked "
                + "JOIN message m ON m.id = ranked.id LEFT JOIN `user` sender ON sender.id = ranked.sender_id "
                + "LEFT JOIN `group` display_group ON display_group.id = ranked.group_id "
                + "ORDER BY " + outerOrder + ", ranked.group_id ASC";
        List<Map<String, Object>> list = jdbc.query(select, params, (rs, index) -> {
            Map<String, Object> message = new LinkedHashMap<>();
            long sharedChatId = rs.getLong("shared_chat_id");
            String type = rs.getString("chat_type");
            Long groupId = rs.getObject("group_id", Long.class);
            String senderName = rs.getString("sender_name");
            message.put("id", rs.getLong("id"));
            message.put("senderId", rs.getLong("sender_id"));
            message.put("senderName", senderName);
            message.put("content", rs.getString("message_content"));
            message.put("createTime", rs.getTimestamp("created_at"));
            message.put("messageType", rs.getInt("message_type"));
            message.put("type", type);
            message.put("targetId", "GROUP".equals(type) ? groupId : sharedChatId);
            message.put("targetName", "GROUP".equals(type) ? rs.getString("group_name") : senderName);
            message.put("sharedChatId", sharedChatId);
            message.put("chatId", sharedChatId);
            message.put("groupId", groupId);
            return message;
        });
        return Map.of("list", list, "total", total == null ? 0L : total, "page", page, "size", size);
    }

    public static void validateRequest(Long userId, String keyword, int page, int size) {
        if (userId == null || userId <= 0) throw new AccessDeniedException("Authenticated user required");
        if (keyword == null || keyword.isBlank() || keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("Search keyword must contain 1 to 100 characters");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("Invalid search pagination");
        if ((long) page * size + size > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("Search result window exceeds 10000; narrow the search or date filters");
        }
    }

    public static void validateMessageRequest(Long userId, String keyword, int page, int size) {
        validateRequest(userId, keyword, page, size);
        String phrase = keyword.replaceAll("(?U)\\s+", " ").strip();
        if (phrase.isEmpty()) throw new IllegalArgumentException("Search keyword must contain a non-whitespace term");
        if (new LinkedHashSet<>(List.of(phrase.toLowerCase(Locale.ROOT).split(" "))).size() > MAX_DISTINCT_TERMS) {
            throw new IllegalArgumentException("Message search supports at most 10 distinct terms");
        }
    }

    private static void filter(StringBuilder sql, MapSqlParameterSource params, String column,
                               String name, List<Long> values) {
        if (!values.isEmpty()) {
            sql.append(" AND ").append(column).append(" IN (:").append(name).append(") ");
            params.addValue(name, values);
        }
    }

    private static List<Long> ids(String value) {
        if (value == null || value.isBlank()) return List.of();
        String[] parts = value.split(",", -1);
        if (parts.length > 100) throw new IllegalArgumentException("Too many search filters");
        Set<Long> ids = new LinkedHashSet<>();
        for (String part : parts) {
            if (!part.trim().matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Invalid search ID filter");
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid search ID filter");
            }
        }
        return List.copyOf(ids);
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate parsed = LocalDate.parse(value);
            if (parsed.getYear() < 1000 || parsed.getYear() >= 9999) throw new IllegalArgumentException("Invalid search date");
            return parsed;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Search dates must use YYYY-MM-DD");
        }
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
