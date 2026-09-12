package com.web.config;

import com.web.migration.SchemaMigrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Pattern;

/** Initializes only the configured database in development; production uses reviewed migrations. */
@Slf4j
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private Environment environment;
    @Value("${spring.datasource.url}") private String databaseUrl;
    @Value("${spring.datasource.username}") private String dbUsername;
    @Value("${spring.datasource.password}") private String dbPassword;

    @Override
    public void run(String... args) throws Exception {
        if (environment.matchesProfiles("migration")) {
            log.info("Migration profile: application initializer disabled; use the dedicated migration CLI");
            return;
        }
        if (environment.matchesProfiles("prod", "production")) {
            new SchemaMigrator(dataSource).validate();
            log.info("Production profile: schema registry validated without DDL");
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
        new SchemaMigrator(dataSource).migrate(false);
        log.info("Schema initialization complete for {}", databaseName);
    }

}
