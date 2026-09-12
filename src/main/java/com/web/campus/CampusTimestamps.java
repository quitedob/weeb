package com.web.campus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Campus DATETIME values are written by SQL CURRENT_TIMESTAMP. Interpret them in
 * the same stable database session timezone before JDBC can apply its own timezone.
 * Responses carry UTC instants; they never guess the JVM or browser's local zone.
 */
public final class CampusTimestamps {
    private CampusTimestamps() { }

    /** SQL projection for trusted column names supplied by the campus services. */
    static String select(String table, String... columns) {
        return Arrays.stream(columns).map(column -> "CAST(UNIX_TIMESTAMP(" + table + "." + column
                + ")*1000 AS SIGNED) AS " + column + "_epoch").collect(Collectors.joining(","));
    }

    public static Instant read(ResultSet result, String column) throws SQLException {
        long milliseconds = result.getLong(column + "_epoch");
        return result.wasNull() ? null : Instant.ofEpochMilli(milliseconds);
    }
}
