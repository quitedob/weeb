package com.web.integration;

import com.web.migration.SchemaMigrator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL DDL and historical data; each test owns a separate randomly named disposable database. */
@EnabledIfEnvironmentVariable(named="WEEB_TEST_MYSQL_URL", matches=".+weeb_audit.*")
class SchemaMigrationIntegrationTest {
    private String database;
    private JdbcTemplate admin;
    private DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private SchemaMigrator migrator;

    @BeforeEach
    void createOwnedDatabase() {
        String url = System.getenv("WEEB_TEST_MYSQL_URL");
        assertTrue(url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):23306/weeb_audit(?:\\?.*)?"));
        String username=System.getenv().getOrDefault("WEEB_TEST_MYSQL_USERNAME","root");
        String password=System.getenv("WEEB_TEST_MYSQL_PASSWORD");
        admin=new JdbcTemplate(new DriverManagerDataSource(url,username,password));
        database="weeb_audit_migration_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE DATABASE `"+database+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        source=new DriverManagerDataSource(url.replace("/weeb_audit","/"+database),username,password);
        jdbc=new JdbcTemplate(source);
        migrator=new SchemaMigrator(source);
    }

    @AfterEach
    void dropOnlyOwnedDatabase() {
        if (database!=null) {
            assertTrue(database.matches("weeb_audit_migration_[a-f0-9]{32}"));
            admin.execute("DROP DATABASE `"+database+"`");
        }
    }

    @Test
    void readOnlyPlanThenFreshInstallAndRepeatPreserveData() throws Exception {
        assertTrue(migrator.plan().stream().allMatch(row->row.state().equals("PENDING")));
        assertEquals(0,tableCount());
        migrator.migrate(false);
        migrator.validate();
        assertEquals(migrator.requiredTables().size(),tableCount());
        jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (101,'migration_survivor','fixture-hash','migration@example.invalid','USER',1)");
        migrator.migrate(false);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE id=101",Integer.class));
        assertEquals(SchemaMigrator.loadSteps().size(),jdbc.queryForObject("SELECT COUNT(*) FROM weeb_schema_history WHERE state='APPLIED'",Integer.class));
    }

    @Test
    void knownLegacyColumnsAndRowsUpgradeWithoutInferringAdminFromUsername() throws Exception {
        // Versioned repository DDL, plus the earlier optional reaction schema with created_at.
        try(Connection c=source.getConnection()) {
            for(String path:List.of("sql/legacy/da3cf23/01_create_user_table.sql","sql/legacy/da3cf23/03_create_group_table.sql","sql/legacy/da3cf23/04_create_shared_chat_table.sql",
                    "sql/legacy/da3cf23/06_create_message_table.sql","db/migration/V1.0.8__Create_Message_Reaction_Table.sql")) {
                ScriptUtils.executeSqlScript(c,new org.springframework.core.io.support.EncodedResource(new ClassPathResource(path), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (101,'admin_legacy_name','unchanged-hash','one@example.invalid','USER',1),(102,'persisted_admin','other-hash','two@example.invalid','ADMIN',0)");
        jdbc.update("INSERT INTO shared_chat(id,chat_type,participant_1_id,participant_2_id) VALUES(201,'PRIVATE',101,102)");
        jdbc.update("INSERT INTO message(id,sender_id,chat_id,content) VALUES(301,101,201,JSON_OBJECT('content','retained historical message'))");
        jdbc.update("INSERT INTO message_reaction(message_id,user_id,reaction_type,created_at) VALUES(301,102,'like','2025-01-02 03:04:05')");
        migrator.migrate(false);
        assertEquals("USER",jdbc.queryForObject("SELECT type FROM `user` WHERE id=101",String.class));
        assertEquals("ADMIN",jdbc.queryForObject("SELECT type FROM `user` WHERE id=102",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT status FROM `user` WHERE id=102",Integer.class));
        assertEquals("unchanged-hash",jdbc.queryForObject("SELECT password FROM `user` WHERE id=101",String.class));
        assertEquals("retained historical message",jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.content')) FROM message WHERE id=301",String.class));
        assertEquals("2025-01-02 03:04:05",jdbc.queryForObject("SELECT DATE_FORMAT(create_time,'%Y-%m-%d %H:%i:%s') FROM message_reaction WHERE message_id=301",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='message' AND index_name='uk_client_message_id'",Integer.class));
        migrator.validate();
    }

    @Test
    void legacyCategoryIdsArePreservedAndNewChildrenResolveParentsByName() throws Exception {
        try (Connection c = source.getConnection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("sql/create/14_create_article_category_table.sql"));
        }
        jdbc.update("INSERT INTO article_category(id,category_name,parent_id) VALUES (1,'custom preserved category',NULL),(77,'技术',NULL)");
        migrator.migrate(false);
        assertEquals("custom preserved category", jdbc.queryForObject("SELECT category_name FROM article_category WHERE id=1", String.class));
        assertEquals(77L, jdbc.queryForObject("SELECT parent_id FROM article_category WHERE category_name='前端开发'", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM article_category WHERE parent_id=1", Integer.class));
    }

    @Test
    void searchUpgradeBackfillsOldRowsAndDatabaseMaintainsContentChanges() throws Exception {
        var oldSteps = SchemaMigrator.loadSteps().stream().filter(step -> step.version().compareTo("005") < 0).toList();
        new SchemaMigrator(source, oldSteps).migrate(false);
        jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (101,'search_migration','fixture-hash','search@example.invalid','USER',1)");
        jdbc.update("INSERT INTO message(id,sender_id,content) VALUES (301,101,JSON_OBJECT('content',?)),(302,101,JSON_OBJECT('missing','value'))", "CAFÉ 校园 😀 %_!");
        migrator.migrate(false);
        assertEquals("café 校园 😀 %_!", jdbc.queryForObject("SELECT search_text FROM message WHERE id=301", String.class));
        assertNull(jdbc.queryForObject("SELECT search_text FROM message WHERE id=302", String.class));
        jdbc.update("UPDATE message SET content=JSON_OBJECT('content','CHANGED') WHERE id=301");
        assertEquals("changed", jdbc.queryForObject("SELECT search_text FROM message WHERE id=301", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE NOT (search_text <=> LOWER(JSON_UNQUOTE(JSON_EXTRACT(content,'$.content'))))", Integer.class));
        migrator.migrate(false);
        migrator.validate();
    }

    @Test
    void campusUpgradeFromV005PreservesPublicDataAndRejectsMissingUniqueness() throws Exception {
        var oldSteps = SchemaMigrator.loadSteps().stream().filter(step -> step.version().compareTo("006") < 0).toList();
        new SchemaMigrator(source, oldSteps).migrate(false);
        jdbc.update("INSERT INTO `user`(id,username,password,user_email,type,status) VALUES (101,'campus_upgrade','fixture-hash','campus@example.invalid','USER',1)");
        migrator.migrate(false);
        migrator.validate();
        jdbc.update("INSERT INTO campus_school(id,name,created_by) VALUES (201,'Upgrade fixture',101)");
        jdbc.update("INSERT INTO campus_verification_application(school_id,user_id,real_name,student_number,department,enrollment_year) VALUES (201,101,'Private name','S101','Department',2026)");
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.update("INSERT INTO campus_verification_application(school_id,user_id,real_name,student_number,department,enrollment_year) VALUES (201,101,'Private name','S101','Department',2026)"));
        jdbc.update("UPDATE campus_verification_application SET status='REJECTED' WHERE school_id=201 AND user_id=101");
        jdbc.update("INSERT INTO campus_verification_application(school_id,user_id,real_name,student_number,department,enrollment_year) VALUES (201,101,'Private name','S101','Department',2026)");
        migrator.migrate(false);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM campus_verification_application WHERE school_id=201", Integer.class));
        assertEquals("USER", jdbc.queryForObject("SELECT type FROM `user` WHERE id=101", String.class));
        jdbc.execute("ALTER TABLE campus_verification_application DROP INDEX uk_campus_application_pending");
        assertThrows(IllegalStateException.class, () -> migrator.validate());
    }

    @Test
    void partialCampusInstallationRequiresReviewedRepairBeforeResume() throws Exception {
        var oldSteps = SchemaMigrator.loadSteps().stream().filter(step -> step.version().compareTo("006") < 0).toList();
        new SchemaMigrator(source, oldSteps).migrate(false);
        jdbc.execute("CREATE TABLE campus_school(id BIGINT AUTO_INCREMENT PRIMARY KEY) ENGINE=InnoDB");
        assertThrows(IllegalStateException.class, () -> migrator.migrate(false));
        assertEquals("FAILED", jdbc.queryForObject("SELECT state FROM weeb_schema_history WHERE version='006'", String.class));
        assertThrows(IllegalStateException.class, () -> migrator.migrate(false));
        // Repair only this deliberately malformed, empty test-owned table; other committed campus DDL stays intact.
        jdbc.execute("ALTER TABLE campus_school ADD name VARCHAR(120) NOT NULL, ADD description VARCHAR(2000) NOT NULL DEFAULT '', ADD active BOOLEAN NOT NULL DEFAULT TRUE, ADD pre_moderation BOOLEAN NOT NULL DEFAULT TRUE, ADD version BIGINT NOT NULL DEFAULT 1, ADD created_by BIGINT NOT NULL, ADD created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), ADD updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), ADD UNIQUE KEY uk_campus_school_name(name)");
        migrator.migrate(true);
        migrator.validate();
    }

    @Test
    void campusColumnDriftFailsValidationAndOnlyReviewedRepairCanResume() throws Exception {
        var oldSteps = SchemaMigrator.loadSteps().stream().filter(step -> step.version().compareTo("006") < 0).toList();
        new SchemaMigrator(source, oldSteps).migrate(false);
        try (Connection connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.support.EncodedResource(
                    new ClassPathResource("sql/migration/V006__campus_space.sql"), java.nio.charset.StandardCharsets.UTF_8));
        }
        jdbc.execute("ALTER TABLE campus_post MODIFY title VARCHAR(10) NOT NULL");
        assertThrows(IllegalStateException.class, () -> migrator.migrate(false));
        assertEquals("FAILED", jdbc.queryForObject("SELECT state FROM weeb_schema_history WHERE version='006'", String.class));
        jdbc.execute("ALTER TABLE campus_post MODIFY title VARCHAR(120) NOT NULL");
        migrator.migrate(true);
        jdbc.execute("ALTER TABLE campus_post MODIFY content VARCHAR(100) NOT NULL");
        assertThrows(IllegalStateException.class, () -> migrator.validate());
        jdbc.execute("ALTER TABLE campus_post MODIFY content TEXT NOT NULL");
        jdbc.execute("ALTER TABLE campus_membership MODIFY status VARCHAR(16) NULL");
        assertThrows(IllegalStateException.class, () -> migrator.validate());
    }

    @Test
    void wrongSearchColumnFailsWithoutDiscardingDataAndExplicitResumeRequiresCorrectDefinition() throws Exception {
        var oldSteps = SchemaMigrator.loadSteps().stream().filter(step -> step.version().compareTo("005") < 0).toList();
        new SchemaMigrator(source, oldSteps).migrate(false);
        jdbc.execute("ALTER TABLE message ADD COLUMN search_text LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
        assertThrows(IllegalStateException.class, () -> migrator.migrate(false));
        assertEquals("FAILED", jdbc.queryForObject("SELECT state FROM weeb_schema_history WHERE version='005'", String.class));
        assertEquals("", jdbc.queryForObject("SELECT GENERATION_EXPRESSION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='message' AND COLUMN_NAME='search_text'", String.class));
        // Simulate an operator's reviewed repair of this empty, test-owned database.
        jdbc.execute("ALTER TABLE message DROP COLUMN search_text");
        jdbc.execute("ALTER TABLE message ADD COLUMN search_text LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin GENERATED ALWAYS AS (LOWER(JSON_UNQUOTE(JSON_EXTRACT(content,_latin1'$.content')))) STORED");
        assertThrows(IllegalStateException.class, () -> migrator.migrate(false));
        migrator.migrate(true);
        migrator.validate();
        jdbc.execute("ALTER TABLE message MODIFY search_text LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (LOWER(JSON_UNQUOTE(JSON_EXTRACT(content,'$.content')))) STORED");
        assertThrows(IllegalStateException.class, () -> migrator.validate());
        for (String wrongPath : List.of("$.CONTENT", "$.content_utf8mb4")) {
            jdbc.execute("ALTER TABLE message MODIFY search_text LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin GENERATED ALWAYS AS (LOWER(JSON_UNQUOTE(JSON_EXTRACT(content,'" + wrongPath + "')))) STORED");
            assertThrows(IllegalStateException.class, () -> migrator.validate(), wrongPath);
        }
    }

    @Test
    void checksumMismatchAndNewerSchemaBlockExecutionWithoutRewritingHistory() throws Exception {
        migrator.migrate(false);
        var oldRegistry=SchemaMigrator.loadSteps().subList(0,SchemaMigrator.loadSteps().size()-1);
        assertThrows(IllegalStateException.class,()->new SchemaMigrator(source,oldRegistry).migrate(false));
        jdbc.update("UPDATE weeb_schema_history SET checksum=REPEAT('0',64) WHERE version='001'");
        assertThrows(IllegalStateException.class,()->migrator.plan());
        assertThrows(IllegalStateException.class,()->migrator.migrate(true));
        assertEquals("0".repeat(64),jdbc.queryForObject("SELECT checksum FROM weeb_schema_history WHERE version='001'",String.class));
    }

    @Test
    void committedDdlFailureRequiresExplicitSameChecksumResume() throws Exception {
        var steps=new ArrayList<>(SchemaMigrator.loadSteps());
        steps.add(SchemaMigrator.step("900","resumable failure fixture",List.of("sql/test/V900__resume_fixture.sql")));
        var withFailure=new SchemaMigrator(source,steps);
        assertThrows(IllegalStateException.class,()->withFailure.migrate(false));
        assertEquals("FAILED",jdbc.queryForObject("SELECT state FROM weeb_schema_history WHERE version='900'",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM migration_resume_fixture",Integer.class));
        assertThrows(IllegalStateException.class,()->withFailure.migrate(false));
        jdbc.execute("CREATE TABLE migration_resume_dependency(id INT PRIMARY KEY,value VARCHAR(30))");
        jdbc.update("INSERT INTO migration_resume_dependency VALUES (1,'recovered')");
        withFailure.migrate(true);
        withFailure.migrate(false);
        assertEquals("recovered",jdbc.queryForObject("SELECT value FROM migration_resume_fixture WHERE id=1",String.class));
        assertEquals("APPLIED",jdbc.queryForObject("SELECT state FROM weeb_schema_history WHERE version='900'",String.class));
    }

    @Test
    void concurrentMigratorCannotWriteBeforeAcquiringDatabaseLock() throws Exception {
        try(Connection c=source.getConnection();var lock=c.prepareStatement("SELECT GET_LOCK(?,0)")) {
            lock.setString(1,SchemaMigrator.lockName(database));
            try(var result=lock.executeQuery()){assertTrue(result.next());assertEquals(1,result.getInt(1));}
            assertThrows(IllegalStateException.class,()->migrator.migrate(false));
            assertEquals(0,tableCount());
        }
        migrator.migrate(false);
    }

    @Test
    void productionValidationUsesReadOnlyConnectionAndRejectsPendingSchema() throws Exception {
        assertThrows(IllegalStateException.class,()->migrator.validate());
        migrator.migrate(false);
        try(Connection c=source.getConnection()) {
            c.setReadOnly(true);
            new SchemaMigrator(new org.springframework.jdbc.datasource.SingleConnectionDataSource(c,true)).validate();
        }
    }

    private int tableCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",Integer.class);
    }
}
