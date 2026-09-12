package com.web.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/** Reject partial or drifted campus installations before the application serves private content. */
final class CampusSchemaValidator {
    private CampusSchemaValidator() {}
    static void validate(Connection connection) throws SQLException {
        Map<String, String> columns = Map.of(
                "campus_school", "id,name,description,active,pre_moderation,version,created_by,created_at,updated_at",
                "campus_membership", "school_id,user_id,status,role,version,joined_at,updated_at",
                "campus_verification_application", "id,school_id,user_id,real_name,student_number,department,enrollment_year,statement,status,reason,version,reviewed_by,created_at,reviewed_at,pending_user_id",
                "campus_post", "id,school_id,author_id,title,content,category,status,version,pinned,review_reason,reviewed_by,reviewed_at,created_at,updated_at",
                "campus_post_like", "post_id,user_id,created_at",
                "campus_post_bookmark", "post_id,user_id,created_at",
                "campus_comment", "id,post_id,author_id,content,reply_to_comment_id,deleted,created_at",
                "campus_report", "id,school_id,post_id,reporter_id,reason,status,decision_reason,version,reviewed_by,created_at,reviewed_at,pending_reporter_id",
                "campus_audit", "id,school_id,actor_id,action,target_type,target_id,details,created_at",
                "campus_media", "id,school_id,owner_id,post_id,width,height,size,position,created_at,detached_at");
        for (var entry : columns.entrySet()) {
            Set<String> actual = new HashSet<>();
            try (PreparedStatement query = connection.prepareStatement("SELECT COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,IS_NULLABLE,DATETIME_PRECISION,CHARACTER_SET_NAME FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                query.setString(1, entry.getKey());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        String column = rows.getString(1);
                        actual.add(column);
                        if (List.of(entry.getValue().split(",")).contains(column)) column(connection, entry.getKey(), column, rows);
                    }
                }
            }
            if (!actual.containsAll(List.of(entry.getValue().split(",")))) fail(entry.getKey() + " columns");
        }
        index(connection, "campus_school", "uk_campus_school_name", "name", true);
        index(connection, "campus_membership", "PRIMARY", "school_id,user_id", true);
        index(connection, "campus_verification_application", "uk_campus_application_pending", "school_id,pending_user_id", true);
        index(connection, "campus_post_like", "PRIMARY", "post_id,user_id", true);
        index(connection, "campus_post_bookmark", "PRIMARY", "post_id,user_id", true);
        index(connection, "campus_report", "uk_campus_report_pending", "post_id,pending_reporter_id", true);
        index(connection, "campus_post", "idx_campus_post_feed", "school_id,status,pinned,id", false);
        index(connection, "campus_comment", "idx_campus_comment_post", "post_id,id", false);
        index(connection, "campus_media", "idx_campus_media_cleanup", "post_id,detached_at", false);
        generated(connection, "campus_verification_application", "pending_user_id", "user_id");
        generated(connection, "campus_report", "pending_reporter_id", "reporter_id");
        for (String table : List.of("campus_school", "campus_verification_application", "campus_post", "campus_comment", "campus_report", "campus_audit")) {
            index(connection, table, "PRIMARY", "id", true);
            try (PreparedStatement query = connection.prepareStatement("SELECT DATA_TYPE,EXTRA FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME='id'")) {
                query.setString(1, table);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next() || !"bigint".equalsIgnoreCase(rows.getString(1)) || !rows.getString(2).contains("auto_increment")) fail(table + ".id");
                }
            }
        }
        for (String table : columns.keySet()) {
            try (PreparedStatement query = connection.prepareStatement("SELECT ENGINE FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                query.setString(1, table);
                try (ResultSet rows = query.executeQuery()) { if (!rows.next() || !"InnoDB".equalsIgnoreCase(rows.getString(1))) fail(table + " engine"); }
            }
        }
    }

    private static void column(Connection connection, String table, String column, ResultSet row) throws SQLException {
        Map<String, Integer> lengths = Map.ofEntries(Map.entry("name", 120), Map.entry("description", 2000),
                Map.entry("real_name", 80), Map.entry("student_number", 40), Map.entry("department", 100),
                Map.entry("statement", 1000), Map.entry("reason", 500), Map.entry("title", 120),
                Map.entry("category", 24), Map.entry("status", 16), Map.entry("role", 16),
                Map.entry("review_reason", 500), Map.entry("decision_reason", 500), Map.entry("action", 60),
                Map.entry("target_type", 32), Map.entry("target_id", 64), Map.entry("details", 1000));
        boolean nullable = Set.of("reviewed_by", "reviewed_at", "reply_to_comment_id", "pending_user_id", "pending_reporter_id").contains(column)
                || (table.equals("campus_media") && Set.of("post_id", "detached_at").contains(column));
        String type;
        Integer length = lengths.get(column);
        if (length != null) type = "varchar";
        else if (table.equals("campus_media") && column.equals("id")) { type = "char"; length = 36; }
        else if (column.equals("content")) { type = table.equals("campus_post") ? "text" : "varchar"; if (type.equals("varchar")) length = 1000; }
        else if (column.endsWith("_at")) type = "datetime";
        else if (Set.of("active", "pre_moderation", "pinned", "deleted").contains(column)) type = "tinyint";
        else if (Set.of("width", "height", "position").contains(column)) type = "int";
        else if (column.equals("enrollment_year")) type = "smallint";
        else type = "bigint";
        if (!type.equalsIgnoreCase(row.getString("DATA_TYPE")) || nullable != "YES".equals(row.getString("IS_NULLABLE"))
                || (length != null && length.longValue() != row.getLong("CHARACTER_MAXIMUM_LENGTH"))
                || (type.equals("datetime") && row.getInt("DATETIME_PRECISION") != 3)
                || (Set.of("varchar", "text").contains(type) && !"utf8mb4".equals(row.getString("CHARACTER_SET_NAME")))
                || (type.equals("char") && !"ascii".equals(row.getString("CHARACTER_SET_NAME")))) fail(table + "." + column + " definition");
    }

    private static void index(Connection connection, String table, String name, String expected, boolean unique) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("SELECT COLUMN_NAME,NON_UNIQUE,SUB_PART FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND INDEX_NAME=? ORDER BY SEQ_IN_INDEX")) {
            query.setString(1, table); query.setString(2, name);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    if ((rows.getInt(2) == 0) != unique || rows.getObject(3) != null) fail(table + "." + name);
                    columns.add(rows.getString(1));
                }
            }
        }
        if (!String.join(",", columns).equals(expected)) fail(table + "." + name);
    }

    private static void generated(Connection connection, String table, String column, String actorColumn) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT DATA_TYPE,EXTRA,GENERATION_EXPRESSION FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            query.setString(1, table); query.setString(2, column);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next() || !"bigint".equalsIgnoreCase(rows.getString(1)) || !"STORED GENERATED".equals(rows.getString(2))) fail(table + "." + column);
                String expression = rows.getString(3).replace("\\'", "'").replaceAll("(?i)_(?:utf8mb4|utf8mb3|utf8|latin1|ascii)", "")
                        .replaceAll("[`()\\s]", "").toLowerCase(Locale.ROOT);
                if (!expression.equals("casewhenstatus='pending'then" + actorColumn + "elsenullend")) fail(table + "." + column);
            }
        }
    }
    private static void fail(String component) { throw new IllegalStateException("Unexpected campus schema: " + component + "; reconcile before reviewed migration resume"); }
}
