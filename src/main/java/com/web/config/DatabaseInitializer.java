package com.web.config;

import com.web.util.SqlFileLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Initializes only the configured database in development; production uses reviewed migrations. */
@Slf4j
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private Environment environment;
    @Autowired private SqlFileLoader sqlFileLoader;
    @Value("${spring.datasource.url}") private String databaseUrl;
    @Value("${spring.datasource.username}") private String dbUsername;
    @Value("${spring.datasource.password}") private String dbPassword;

    // SQL文件执行顺序
    private final List<String> CHECK_FILES = Arrays.asList(
        "check/01_check_database_connection.sql",
        "check/02_check_table_structure.sql"
    );

    private final List<String> CREATE_FILES = Arrays.asList(
        "create/01_create_user_table.sql",
        "create/02_create_user_stats_table.sql",
        "create/03_create_group_table.sql",
        "create/04_create_shared_chat_table.sql",
        "create/05_create_chat_list_table.sql",
        "create/06_create_message_table.sql",
        "create/06_5_create_chat_unread_count_table.sql",
        "create/07_create_group_member_table.sql",
        // 先创建 article_category 和 article_tag，因为 articles 表依赖它们
        "create/14_create_article_category_table.sql",
        "create/15_create_article_tag_table.sql",
        // 然后创建 articles 表
        "create/08_create_article_table.sql",
        "create/09_create_notification_table.sql",
        "create/10_create_contact_table.sql",
        // articles 相关的子表
        "create/11_create_article_comment_table.sql",
        "create/12_create_article_like_table.sql",
        "create/13_create_article_favorite_table.sql",
        "create/16_create_article_tag_relation_table.sql",
        "create/20_create_article_version_table.sql",
        "create/25_create_article_moderation_history_table.sql",
        // 其他表
        "create/17_create_user_follow_table.sql",
        "create/18_create_system_log_table.sql",
        "create/19_create_user_level_history_table.sql",
        "create/21_create_content_report_table.sql",
        "create/22_create_contact_group_table.sql",
        "create/23_create_group_transfer_history_table.sql",
        "create/24_create_group_application_table.sql",
        "create/26_create_message_retry_table.sql",
        "create/27_create_message_reaction_table.sql",
        "create/28_create_user_preferences_table.sql",
        "create/29_create_user_login_day_table.sql"
    );

    private final List<String> INSERT_FILES = Arrays.asList(
        "insert/01_insert_default_users.sql",
        "insert/02_insert_article_categories.sql",
        "insert/03_insert_article_tags.sql"
    );

    private final List<String> INDEX_FILES = Arrays.asList(
        "index/01_optimize_core_indexes.sql",
        "index/02_optimize_content_indexes.sql"
    );

    @Override
    public void run(String... args) throws Exception {
        if (environment.matchesProfiles("prod", "production")) {
            log.info("Production profile: automatic schema creation disabled");
            return;
        }
        var url = Pattern.compile("^(jdbc:mysql://[^/]+)/([A-Za-z0-9_]+)(\\?.*)?$").matcher(databaseUrl);
        if (!url.matches()) throw new IllegalArgumentException("MYSQL_URL must name one MySQL database");
        String databaseName = url.group(2);
        String systemUrl = url.group(1) + "/" + (url.group(3) == null ? "" : url.group(3));
        try (Connection connection = DriverManager.getConnection(systemUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + databaseName
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        executeFiles(CREATE_FILES);
        executeFiles(INSERT_FILES);
        for (String file : INDEX_FILES) executeIndexes(file);
        executeFiles(CHECK_FILES);
        log.info("Schema initialization complete for {}", databaseName);
    }

    private void executeFiles(List<String> files) {
        for (String file : files) {
            if (sqlFileLoader.loadSqlFile(file).isBlank()) continue;
            var populator = new ResourceDatabasePopulator(new ClassPathResource("sql/" + file));
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);
        }
    }

    private void executeIndexes(String file) {
        // These maintained files contain only simple CREATE INDEX statements.
        Pattern definition = Pattern.compile("(?is)^CREATE INDEX ([A-Za-z0-9_]+)\\s+ON ([A-Za-z0-9_]+)\\s*\\(.*$");
        for (String sql : sqlFileLoader.loadSqlFile(file).split(";")) {
            if (sql.isBlank()) continue;
            var match = definition.matcher(sql.trim());
            if (!match.matches()) throw new IllegalStateException("Unexpected index definition in " + file);
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=? AND index_name=?",
                    Integer.class, match.group(2), match.group(1));
            if (count == null || count == 0) jdbcTemplate.execute(sql);
        }
    }
}
