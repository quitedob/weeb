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

/** Message search uses current SQL membership, including for pagination and totals. */
@Service
@Transactional(readOnly = true)
public class MessageSearchService {
    private final NamedParameterJdbcTemplate jdbc;

    public MessageSearchService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String CONTENT = "JSON_UNQUOTE(JSON_EXTRACT(m.content, '$.content'))";
    private static final String FROM = " FROM message m JOIN shared_chat sc ON sc.id = m.chat_id "
            + "LEFT JOIN `user` sender ON sender.id = m.sender_id "
            + "LEFT JOIN `group` g ON g.shared_chat_id = sc.id AND sc.chat_type = 'GROUP' "
            + "AND g.status = 1 AND EXISTS (SELECT 1 FROM group_member gm WHERE gm.group_id = g.id "
            + "AND gm.user_id = :actor AND gm.join_status = 'ACCEPTED' AND gm.kicked_at IS NULL) ";
    private static final String VISIBLE = " WHERE COALESCE(m.is_recalled, 0) = 0 AND "
            + "((sc.chat_type = 'PRIVATE' AND (sc.participant_1_id = :actor OR sc.participant_2_id = :actor)) "
            + "OR (sc.chat_type = 'GROUP' AND g.id IS NOT NULL)) ";

    public Map<String, Object> search(Long userId, String keyword, int page, int size,
                                      String startDate, String endDate, String messageTypes,
                                      String userIds, String groupIds, String sortBy) {
        validateRequest(userId, keyword, page, size);
        String order = switch (sortBy == null ? "relevance" : sortBy) {
            case "relevance", "time_desc" -> "m.created_at DESC, m.id DESC";
            case "time_asc" -> "m.created_at ASC, m.id ASC";
            case "username_asc" -> "sender.username ASC, m.created_at DESC, m.id DESC";
            case "username_desc" -> "sender.username DESC, m.created_at DESC, m.id DESC";
            default -> throw new IllegalArgumentException("Invalid message sort order");
        };
        LocalDate start = date(startDate);
        LocalDate end = date(endDate);
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not follow end date");
        }
        List<Long> types = ids(messageTypes);
        if (types.stream().anyMatch(type -> type > 3)) throw new IllegalArgumentException("Invalid message type");
        List<Long> senders = ids(userIds);
        List<Long> groups = ids(groupIds);
        MapSqlParameterSource params = new MapSqlParameterSource("actor", userId)
                .addValue("keyword", "%" + escapeLike(keyword.trim()) + "%")
                .addValue("size", size).addValue("offset", (long) page * size);
        StringBuilder where = new StringBuilder(VISIBLE).append(" AND ").append(CONTENT)
                .append(" LIKE :keyword ESCAPE '!' ");
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
        filter(where, params, "g.id", "groups", groups);
        Long total = jdbc.queryForObject("SELECT COUNT(DISTINCT m.id)" + FROM + where, params, Long.class);
        String select = "SELECT DISTINCT m.id, m.sender_id, sender.username AS sender_name, "
                + CONTENT + " AS message_content, m.created_at, m.message_type, sc.chat_type, sc.id AS shared_chat_id, "
                + "g.id AS group_id, g.group_name " + FROM + where + " ORDER BY " + order
                + " LIMIT :size OFFSET :offset";
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
        if (keyword == null || keyword.isBlank() || keyword.trim().length() > 100) {
            throw new IllegalArgumentException("Search keyword must contain 1 to 100 characters");
        }
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid search pagination");
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
