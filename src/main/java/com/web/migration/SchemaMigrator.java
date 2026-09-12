package com.web.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/** One checksummed schema registry for development, explicit migrations and disposable tests. */
public final class SchemaMigrator {
    public static final String MANIFEST = "sql/schema-manifest.json";
    private static final String HISTORY = "weeb_schema_history";
    private static final Pattern INDEX = Pattern.compile("(?is)^CREATE INDEX ([A-Za-z0-9_]+)\\s+ON ([A-Za-z0-9_]+)\\s*\\(.*$");
    private static final Pattern TABLE = Pattern.compile("(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?([a-z0-9_]+)`?");
    private final DataSource dataSource;
    private final List<Step> steps;

    public record Step(String version, String description, List<String> resources, String checksum) {}
    public record Entry(String version, String description, String checksum, String state) {}
    private record Applied(String checksum, String state) {}

    public SchemaMigrator(DataSource dataSource) { this(dataSource, loadSteps()); }

    public SchemaMigrator(DataSource dataSource, List<Step> steps) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.steps = List.copyOf(steps);
        if (steps.isEmpty()) throw new IllegalArgumentException("Schema registry must not be empty");
        String previous = "";
        for (Step step : steps) {
            if (!step.version().matches("[0-9]{3}") || step.version().compareTo(previous) <= 0) {
                throw new IllegalArgumentException("Migration versions must be unique and ascending");
            }
            if (step.resources().isEmpty() || !step.checksum().matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("Every migration requires resources and a SHA-256 checksum");
            }
            previous = step.version();
        }
    }

    public static List<Step> loadSteps() {
        try (var stream = new ClassPathResource(MANIFEST).getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(stream);
            List<Step> result = new ArrayList<>();
            for (JsonNode item : root.path("migrations")) {
                List<String> resources = new ArrayList<>();
                item.path("resources").forEach(value -> resources.add(value.asText()));
                result.add(step(item.path("version").asText(), item.path("description").asText(), resources));
            }
            return List.copyOf(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load the complete schema registry", failure);
        }
    }

    public static Step step(String version, String description, List<String> resources) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String path : resources) {
                if (!path.startsWith("sql/") || path.contains("..")) throw new IllegalArgumentException("Invalid SQL resource");
                digest.update(path.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(read(path).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return new Step(version, description, List.copyOf(resources), HexFormat.of().formatHex(digest.digest()));
        } catch (Exception failure) { throw new IllegalStateException("Cannot checksum migration " + version, failure); }
    }

    /** Read-only: does not create even the history table, acquire a lease, or run schema scripts. */
    public List<Entry> plan() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Applied> applied = history(connection);
            validateHistory(applied, false);
            return steps.stream().map(step -> new Entry(step.version(), step.description(), step.checksum(),
                    applied.containsKey(step.version()) ? applied.get(step.version()).state() : "PENDING")).toList();
        }
    }

    public List<Entry> migrate(boolean resumeFailed) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getAutoCommit()) throw new IllegalStateException("Migration requires a dedicated autocommit connection");
            String database = connection.getCatalog();
            if (database == null || !database.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("A named MySQL database is required");
            String lock = lockName(database);
            try (PreparedStatement acquire = connection.prepareStatement("SELECT GET_LOCK(?,0)")) {
                acquire.setString(1, lock);
                try (ResultSet result = acquire.executeQuery()) {
                    if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Another schema migration is running");
                }
            }
            try {
                Map<String, Applied> applied = history(connection);
                validateHistory(applied, !resumeFailed);
                execute(connection, "CREATE TABLE IF NOT EXISTS " + HISTORY + " ("
                        + "version VARCHAR(3) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,"
                        + "description VARCHAR(200) NOT NULL, checksum CHAR(64) CHARACTER SET ascii NOT NULL,"
                        + "state VARCHAR(16) NOT NULL, started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                        + "finished_at DATETIME(3) NULL, failure_code VARCHAR(100) NULL) ENGINE=InnoDB");
                for (Step step : steps) {
                    Applied previous = applied.get(step.version());
                    if (previous != null && "APPLIED".equals(previous.state())) continue;
                    mark(connection, step, "APPLYING", null);
                    try {
                        for (String resource : step.resources()) executeResource(connection, resource);
                        mark(connection, step, "APPLIED", null);
                    } catch (Exception failure) {
                        String code = failure instanceof SQLException sql ? sql.getSQLState() + ":" + sql.getErrorCode()
                                : failure.getClass().getSimpleName();
                        mark(connection, step, "FAILED", code);
                        throw new IllegalStateException("Migration " + step.version()
                                + " failed. DDL may already be committed; review before same-checksum resume or backup restore.", failure);
                    }
                }
                validateRequiredTables(connection);
            } finally {
                try (PreparedStatement release = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    release.setString(1, lock); release.execute();
                }
            }
        }
        return plan();
    }

    private void validateHistory(Map<String, Applied> applied, boolean rejectIncomplete) {
        Set<String> known = new HashSet<>();
        boolean pendingSeen = false;
        for (Step step : steps) {
            known.add(step.version());
            Applied row = applied.get(step.version());
            if (row == null) { pendingSeen = true; continue; }
            if (pendingSeen) throw new IllegalStateException("Migration history contains an out-of-order version: " + step.version());
            if (!row.checksum().equals(step.checksum())) throw new IllegalStateException("Migration checksum mismatch: " + step.version());
            if (!Set.of("APPLIED", "APPLYING", "FAILED").contains(row.state())) throw new IllegalStateException("Unknown migration state");
            if (rejectIncomplete && !"APPLIED".equals(row.state())) throw new IllegalStateException("Incomplete migration " + step.version() + "; explicit reviewed resume required");
            if (!"APPLIED".equals(row.state())) pendingSeen = true;
        }
        if (!known.containsAll(applied.keySet())) throw new IllegalStateException("Database schema is newer than this application's registry");
    }

    private Map<String, Applied> history(Connection connection) throws SQLException {
        Map<String, Applied> result = new LinkedHashMap<>();
        if (!tableExists(connection, HISTORY)) return result;
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT version,checksum,state FROM " + HISTORY + " ORDER BY version")) {
            while (rows.next()) result.put(rows.getString(1), new Applied(rows.getString(2), rows.getString(3)));
        }
        return result;
    }

    /** MySQL limits named locks to 64 characters, including the longest supported database name. */
    public static String lockName(String database) {
        try {
            return "weeb-schema:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(database.getBytes(StandardCharsets.UTF_8))).substring(0, 48);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void mark(Connection connection, Step step, String state, String failure) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("INSERT INTO " + HISTORY
                + " (version,description,checksum,state,finished_at,failure_code) VALUES (?,?,?,?,IF(?='APPLIED',CURRENT_TIMESTAMP(3),NULL),?)"
                + " ON DUPLICATE KEY UPDATE state=VALUES(state),finished_at=VALUES(finished_at),failure_code=VALUES(failure_code)")) {
            update.setString(1, step.version()); update.setString(2, step.description()); update.setString(3, step.checksum());
            update.setString(4, state); update.setString(5, state); update.setString(6, failure); update.executeUpdate();
        }
    }

    private static String read(String path) throws Exception {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static void executeResource(Connection connection, String path) throws Exception {
        // The retired default-account seed intentionally contains comments only.
        if (read(path).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^\\s*--.*$", "").isBlank()) return;
        if (path.endsWith("V002__legacy_compatibility.sql")) {
            try (Statement query = connection.createStatement(); ResultSet columns = query.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()"
                    + " AND table_name='message_reaction' AND column_name IN ('created_at','create_time')")) {
                columns.next();
                if (columns.getInt(1) != 1) throw new IllegalStateException(
                        "Ambiguous reaction timestamp schema; reconcile legacy columns before reviewed resume");
            }
        }
        if (!path.startsWith("sql/index/")) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(path), StandardCharsets.UTF_8));
            return;
        }
        for (String sql : read(path).replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            if (sql.isBlank()) continue;
            var definition = INDEX.matcher(sql.trim());
            if (!definition.matches()) throw new IllegalStateException("Unexpected index statement in " + path);
            try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=? AND index_name=?")) {
                query.setString(1, definition.group(2)); query.setString(2, definition.group(1));
                try (ResultSet result = query.executeQuery()) { result.next(); if (result.getInt(1) == 0) execute(connection, sql); }
            }
        }
    }

    public Set<String> requiredTables() {
        Set<String> tables = new TreeSet<>();
        tables.add(HISTORY);
        try {
            for (Step step : steps) for (String path : step.resources()) {
                var matches = TABLE.matcher(read(path));
                while (matches.find()) tables.add(matches.group(1));
            }
            return Collections.unmodifiableSet(tables);
        } catch (Exception failure) { throw new IllegalStateException("Cannot inspect schema resources", failure); }
    }

    public void validate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Applied> rows = history(connection);
            validateHistory(rows, true);
            if (rows.size() != steps.size()) throw new IllegalStateException("Pending schema migrations; run the dedicated migration CLI first");
            validateRequiredTables(connection);
        }
    }

    private void validateRequiredTables(Connection connection) throws SQLException {
        for (String table : requiredTables()) if (!tableExists(connection, table)) throw new IllegalStateException("Required schema table is missing: " + table);
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            query.setString(1, table);
            try (ResultSet rows = query.executeQuery()) { return rows.next() && rows.getInt(1) == 1; }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }
}
