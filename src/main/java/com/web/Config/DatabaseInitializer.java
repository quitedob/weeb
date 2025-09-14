package com.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 数据库初始化器
 * 在开发环境启动时检查数据库表结构并自动创建缺失的表
 * 生产环境禁用自动创建功能，确保数据库安全
 */
@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${MYSQL_DATABASE:weeb}")
    private String databaseName;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查数据库...");

        // 检查当前环境，只有开发环境才执行自动创建
        boolean isProduction = environment.matchesProfiles("prod") ||
                              environment.matchesProfiles("production") ||
                              System.getProperty("spring.profiles.active", "").equals("prod") ||
                              System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "").equals("prod");

        if (isProduction) {
            log.info("生产环境检测：跳过数据库自动创建，请确保数据库和表结构已正确配置");
            // 仅检查连接，不执行创建操作
            try {
                checkDatabaseConnection();
                log.info("数据库连接正常");
            } catch (Exception e) {
                log.error("生产环境数据库连接失败，请检查数据库配置: {}", e.getMessage());
                throw e;
            }
            return;
        }

        log.info("开发环境检测：执行数据库自动初始化");

        try {
            // 首先尝试创建数据库（如果不存在）
            ensureDatabaseExists();

            // 检查数据库连接
            checkDatabaseConnection();

            // 检查并创建用户表
            checkAndCreateUserTable();

            // 检查并创建文章表
            checkAndCreateArticleTable();

            // 检查并创建群组表
            checkAndCreateGroupTable();

            // 检查并创建消息表
            checkAndCreateMessageTable();

            // 检查并创建通知表
            checkAndCreateNotificationTable();

            log.info("数据库初始化完成！所有表结构已准备就绪。");

        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage());
            printDatabaseSetupInstructions();
            throw e;
        }
    }

    private void ensureDatabaseExists() {
        try {
            // 尝试连接到MySQL服务器（不指定数据库）
            String mysqlUrl = databaseUrl.replaceAll("/" + databaseName + ".*", "/mysql");

            try (Connection conn = DriverManager.getConnection(mysqlUrl, dbUsername, dbPassword);
                 Statement stmt = conn.createStatement()) {

                // 检查数据库是否存在
                String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + databaseName + "'";
                var resultSet = stmt.executeQuery(checkDbSql);

                if (!resultSet.next()) {
                    // 数据库不存在，创建数据库
                    log.info("数据库 '{}' 不存在，正在创建...", databaseName);
                    String createDbSql = "CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
                    stmt.executeUpdate(createDbSql);
                    log.info("数据库 '{}' 创建成功！", databaseName);
                } else {
                    log.info("数据库 '{}' 已存在", databaseName);
                }
            }
        } catch (SQLException e) {
            log.error("创建数据库时出错: {}", e.getMessage());
            throw new RuntimeException("无法创建数据库，请手动创建数据库或检查MySQL连接", e);
        }
    }

    private void checkDatabaseConnection() {
        try {
            jdbcTemplate.execute("SELECT 1");
            log.info("数据库连接正常");
        } catch (Exception e) {
            log.error("数据库连接失败: {}", e.getMessage());
            throw new RuntimeException("无法连接到数据库，请检查数据库配置", e);
        }
    }

    private void checkAndCreateUserTable() {
        if (!tableExists("user")) {
            log.info("用户表不存在，开始创建...");
            executeSqlFile("sql/init_user_table.sql");
            log.info("用户表创建完成");
        } else {
            log.info("用户表已存在");
        }
    }

    private void checkAndCreateArticleTable() {
        if (!tableExists("article")) {
            log.info("文章表不存在，开始创建...");
            executeSqlFile("sql/init_article_table.sql");
            log.info("文章表创建完成");
        } else {
            log.info("文章表已存在");
        }
    }

    private void checkAndCreateGroupTable() {
        if (!tableExists("group_info")) {
            log.info("群组表不存在，开始创建...");
            executeSqlFile("sql/init_group_table.sql");
            log.info("群组表创建完成");
        } else {
            log.info("群组表已存在");
        }
    }

    private void checkAndCreateMessageTable() {
        if (!tableExists("message")) {
            log.info("消息表不存在，开始创建...");
            executeSqlFile("sql/init_message_table.sql");
            log.info("消息表创建完成");
        } else {
            log.info("消息表已存在");
        }
    }

    private void checkAndCreateNotificationTable() {
        if (!tableExists("notification")) {
            log.info("通知表不存在，开始创建...");
            executeSqlFile("sql/init_notification_table.sql");
            log.info("通知表创建完成");
        } else {
            log.info("通知表已存在");
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SHOW TABLES LIKE ?";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, tableName);
            return !result.isEmpty();
        } catch (Exception e) {
            log.warn("检查表是否存在时出错: {}", e.getMessage());
            return false;
        }
    }

    private void executeSqlFile(String sqlFilePath) {
        try {
            ClassPathResource resource = new ClassPathResource(sqlFilePath);
            if (!resource.exists()) {
                log.warn("SQL文件不存在: {}", sqlFilePath);
                return;
            }

            StringBuilder sql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("--")) {
                        sql.append(line);
                        if (line.endsWith(";")) {
                            executeSql(sql.toString());
                            sql.setLength(0);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取SQL文件失败: {}", sqlFilePath, e);
        }
    }

    private void executeSql(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.error("执行SQL失败: {}", sql, e);
            throw e;
        }
    }

    private void printDatabaseSetupInstructions() {
        log.info("\n" + "=".repeat(80));
        log.info("🚀 WEEB 项目数据库自动初始化说明");
        log.info("=".repeat(80));

        log.info("\n📋 自动初始化流程：");
        log.info("1. ✅ 检查MySQL服务连接");
        log.info("2. 🏗️  自动创建数据库（如果不存在）");
        log.info("3. 📊 检查并创建数据表");
        log.info("4. 🎯 初始化完成，应用启动");

        log.info("\n⚙️  当前配置信息：");
        log.info("   数据库主机: {}", extractHostFromUrl(databaseUrl));
        log.info("   数据库端口: {}", extractPortFromUrl(databaseUrl));
        log.info("   数据库名称: {}", databaseName);
        log.info("   数据库用户: {}", dbUsername);

        log.info("\n🔧 如果自动创建失败，请手动执行：");

        log.info("\n1. 连接MySQL：");
        log.info("   mysql -h {} -P {} -u {} -p", extractHostFromUrl(databaseUrl), extractPortFromUrl(databaseUrl), dbUsername);

        log.info("\n2. 创建数据库：");
        log.info("   CREATE DATABASE `{}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;", databaseName);

        log.info("\n3. 或者使用以下完整命令：");
        log.info("   mysql -h {} -P {} -u {} -p -e \"CREATE DATABASE `{}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\"",
                extractHostFromUrl(databaseUrl), extractPortFromUrl(databaseUrl), dbUsername, databaseName);

        log.info("\n💡 提示：");
        log.info("   - 确保MySQL服务正在运行");
        log.info("   - 检查数据库用户权限（需要CREATE DATABASE权限）");
        log.info("   - 如果使用Docker，请确保端口映射正确");
        log.info("   - 默认配置使用root用户和空密码（仅用于开发环境）");

        log.info("\n🎯 环境变量配置（生产环境）：");
        log.info("   MYSQL_HOST=your_mysql_host");
        log.info("   MYSQL_PORT=3306");
        log.info("   MYSQL_DATABASE=weeb");
        log.info("   MYSQL_USERNAME=your_username");
        log.info("   MYSQL_PASSWORD=your_password");

        log.info("\n" + "=".repeat(80));
    }

    private String extractHostFromUrl(String url) {
        try {
            // 从JDBC URL中提取主机名
            // jdbc:mysql://localhost:3306/weeb -> localhost
            String host = url.replaceAll("jdbc:mysql://", "").replaceAll(":.*", "");
            return host.isEmpty() ? "localhost" : host;
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String extractPortFromUrl(String url) {
        try {
            // 从JDBC URL中提取端口
            // jdbc:mysql://localhost:3306/weeb -> 3306
            String port = url.replaceAll(".*:", "").replaceAll("/.*", "");
            return port.matches("\\d+") ? port : "3306";
        } catch (Exception e) {
            return "3306";
        }
    }
}
