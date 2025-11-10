package com.web.Config;

import com.web.utils.SqlFileLoader;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * 重构后的数据库初始化器
 * 使用外部SQL文件进行数据库初始化，便于维护和版本控制
 */
@Slf4j
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private SqlFileLoader sqlFileLoader;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final String databaseName = "weeb";

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
        "create/27_create_message_reaction_table.sql"
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
        log.info("==================== WEEB 数据库初始化开始 (重构版) ====================");

        // 检查当前环境，只有开发环境才执行自动创建
        boolean isProduction = environment.matchesProfiles("prod") ||
                              environment.matchesProfiles("production");

        if (isProduction) {
            log.info("生产环境，跳过数据库自动创建");
            return;
        }

        try {
            // 1. 检查并创建数据库
            checkAndCreateDatabase();

            // 2. 检查数据库连接
            checkDatabaseConnection();

            // 3. 执行检查脚本
            executeSqlFiles("检查脚本", CHECK_FILES);

            // 4. 执行表创建脚本
            executeSqlFiles("表创建", CREATE_FILES);

            // 5. 执行数据插入脚本
            executeSqlFiles("数据插入", INSERT_FILES);

            // 6. 执行索引优化脚本
            executeSqlFiles("索引优化", INDEX_FILES);

            log.info("==================== WEEB 数据库初始化完成 (重构版) ====================");

        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw e;
        }
    }

    /**
     * 执行指定类型的SQL文件
     */
    private void executeSqlFiles(String fileType, List<String> sqlFiles) {
        log.info("开始执行{}脚本...", fileType);

        for (String sqlFile : sqlFiles) {
            try {
                if (sqlFileLoader.sqlFileExists(sqlFile)) {
                    String sqlContent = sqlFileLoader.loadSqlFile(sqlFile);
                    executeSqlScript(sqlContent, sqlFile);
                } else {
                    log.warn("SQL文件不存在，跳过: {}", sqlFile);
                }
            } catch (Exception e) {
                log.error("执行SQL文件失败: {}", sqlFile, e);
                // 对于非关键文件，记录错误但继续执行
                if (fileType.contains("迁移") || fileType.contains("索引")) {
                    log.warn("跳过失败的{}，继续执行", fileType);
                } else {
                    throw new RuntimeException("执行SQL文件失败: " + sqlFile, e);
                }
            }
        }

        log.info("✅ {}脚本执行完成", fileType);
    }

    /**
     * 执行单个SQL脚本
     */
    private void executeSqlScript(String sqlContent, String fileName) {
        try {
            // 分割SQL语句（按分号分割）
            String[] sqlStatements = sqlContent.split(";");

            int executedCount = 0;
            int skippedCount = 0;
            
            for (String statement : sqlStatements) {
                String trimmedStatement = statement.trim();
                if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
                    try {
                        // 检查是否是CREATE TABLE语句
                        if (trimmedStatement.toUpperCase().contains("CREATE TABLE")) {
                            String tableName = extractTableName(trimmedStatement);
                            if (tableName != null && tableExists(tableName)) {
                                log.debug("表 {} 已存在，跳过创建", tableName);
                                skippedCount++;
                                continue;
                            }
                        }
                        
                        jdbcTemplate.execute(trimmedStatement);
                        executedCount++;
                    } catch (Exception e) {
                        // 某些语句可能因为已存在而失败，这是正常的
                        log.debug("SQL语句执行失败（可能是预期的）: {}", e.getMessage());
                        skippedCount++;
                    }
                }
            }

            log.info("✅ 成功执行SQL文件: {}, 执行了 {} 条语句, 跳过 {} 条", fileName, executedCount, skippedCount);

        } catch (Exception e) {
            log.error("执行SQL文件失败: {}", fileName, e);
            throw e;
        }
    }
    
    /**
     * 从CREATE TABLE语句中提取表名
     */
    private String extractTableName(String createTableSql) {
        try {
            // 匹配 CREATE TABLE `table_name` 或 CREATE TABLE IF NOT EXISTS `table_name`
            String upperSql = createTableSql.toUpperCase();
            int tableIndex = upperSql.indexOf("TABLE");
            if (tableIndex < 0) return null;
            
            String afterTable = createTableSql.substring(tableIndex + 5).trim();
            
            // 跳过 IF NOT EXISTS
            if (afterTable.toUpperCase().startsWith("IF NOT EXISTS")) {
                afterTable = afterTable.substring(13).trim();
            }
            
            // 提取表名（可能带反引号）
            if (afterTable.startsWith("`")) {
                int endIndex = afterTable.indexOf("`", 1);
                if (endIndex > 0) {
                    return afterTable.substring(1, endIndex);
                }
            } else {
                // 没有反引号，提取到空格或括号
                int spaceIndex = afterTable.indexOf(" ");
                int parenIndex = afterTable.indexOf("(");
                int endIndex = Math.min(
                    spaceIndex > 0 ? spaceIndex : Integer.MAX_VALUE,
                    parenIndex > 0 ? parenIndex : Integer.MAX_VALUE
                );
                if (endIndex < Integer.MAX_VALUE) {
                    return afterTable.substring(0, endIndex).trim();
                }
            }
        } catch (Exception e) {
            log.debug("提取表名失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            String checkSql = String.format(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s'",
                databaseName, tableName
            );
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("检查表存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查并创建数据库
     */
    private void checkAndCreateDatabase() {
        if (!databaseExists()) {
            log.info("数据库 {} 不存在，开始创建...", databaseName);
            createDatabase();
        } else {
            log.info("数据库 {} 已存在", databaseName);
        }
    }

    /**
     * 检查数据库是否存在
     * 注意：必须连接到MySQL系统（不指定数据库），而不是连接到weeb数据库
     */
    private boolean databaseExists() {
        String mysqlSystemUrl = getMysqlSystemUrl();
        
        log.debug("检查数据库是否存在: {}, 使用URL: {}", databaseName, mysqlSystemUrl);

        try (Connection connection = java.sql.DriverManager.getConnection(mysqlSystemUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {

            String checkDbSql = String.format(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '%s'",
                databaseName
            );

            try (var resultSet = statement.executeQuery(checkDbSql)) {
                boolean exists = resultSet.next();
                log.debug("数据库 {} 存在性检查结果: {}", databaseName, exists);
                return exists;
            }

        } catch (SQLException e) {
            log.error("检查数据库存在性时出错: {}", e.getMessage(), e);
            // 如果连接失败，假设数据库不存在，尝试创建
            return false;
        }
    }

    /**
     * 创建数据库
     */
    private void createDatabase() {
        String mysqlSystemUrl = getMysqlSystemUrl();

        try (Connection connection = java.sql.DriverManager.getConnection(mysqlSystemUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {

            String createDbSql = String.format("CREATE DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", databaseName);
            statement.executeUpdate(createDbSql);
            log.info("✅ 数据库 {} 创建成功", databaseName);

        } catch (SQLException e) {
            log.error("创建数据库失败", e);
            throw new RuntimeException("创建数据库失败", e);
        }
    }

    /**
     * 获取MySQL系统连接URL（不包含数据库名）
     * 例如：jdbc:mysql://localhost:3306/weeb?useSSL=false
     * 转换为：jdbc:mysql://localhost:3306?useSSL=false
     */
    private String getMysqlSystemUrl() {
        // 方法1: 查找数据库名并移除
        int dbNameIndex = databaseUrl.indexOf("/" + databaseName);
        if (dbNameIndex > 0) {
            // 检查是否有参数
            int paramIndex = databaseUrl.indexOf("?", dbNameIndex);
            if (paramIndex > 0) {
                // 有参数：jdbc:mysql://localhost:3306/weeb?xxx -> jdbc:mysql://localhost:3306?xxx
                return databaseUrl.substring(0, dbNameIndex) + databaseUrl.substring(paramIndex);
            } else {
                // 无参数：jdbc:mysql://localhost:3306/weeb -> jdbc:mysql://localhost:3306
                return databaseUrl.substring(0, dbNameIndex);
            }
        }
        
        // 方法2: 如果没找到数据库名，尝试移除最后一个/之后到?之前的内容
        int lastSlash = databaseUrl.lastIndexOf("/");
        if (lastSlash > 0) {
            int paramIndex = databaseUrl.indexOf("?", lastSlash);
            if (paramIndex > 0) {
                return databaseUrl.substring(0, lastSlash) + databaseUrl.substring(paramIndex);
            } else {
                return databaseUrl.substring(0, lastSlash);
            }
        }
        
        log.warn("无法解析数据库URL，返回原始URL: {}", databaseUrl);
        return databaseUrl;
    }

    /**
     * 检查数据库连接
     */
    private void checkDatabaseConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("✅ 数据库连接检查成功");
        } catch (Exception e) {
            log.error("❌ 数据库连接检查失败", e);
            throw new RuntimeException("数据库连接检查失败", e);
        }
    }
}