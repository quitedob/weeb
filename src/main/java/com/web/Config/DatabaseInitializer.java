package com.web.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final String databaseName = "weeb"; // 数据库名

    @Override
    public void run(String... args) throws Exception {
        log.info("==================== WEEB 数据库初始化开始 ====================");

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

            // 3. 检查并创建表
            checkAndCreateTables();

            // 4. 插入初始数据
            insertInitialData();

            log.info("==================== WEEB 数据库初始化完成 ====================");

        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw e;
        }
    }

    /**
     * 检查并创建数据库
     */
    private void checkAndCreateDatabase() {
        // 先通过系统库检查数据库是否存在
        if (!databaseExists()) {
            log.info("数据库 {} 不存在，开始创建...", databaseName);
            createDatabase();
        } else {
            log.info("数据库 {} 已存在", databaseName);
        }
    }
    
    /**
     * 检查数据库是否存在
     */
    private boolean databaseExists() {
        String mysqlSystemUrl = getMysqlSystemUrl();
        
        try (Connection connection = java.sql.DriverManager.getConnection(mysqlSystemUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {
            
            String checkDbSql = String.format(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '%s'",
                databaseName
            );
            
            try (var resultSet = statement.executeQuery(checkDbSql)) {
                return resultSet.next();
            }
            
        } catch (SQLException e) {
            log.warn("检查数据库存在性时出错，假设数据库不存在: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建数据库
     */
    private void createDatabase() {
        // 解析原始URL，替换数据库名为mysql系统库
        String mysqlSystemUrl = getMysqlSystemUrl();
        
        try (Connection connection = java.sql.DriverManager.getConnection(mysqlSystemUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {
            
            // 首先检查数据库是否存在
            String checkDbSql = String.format(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '%s'",
                databaseName
            );
            
            boolean dbExists = false;
            try (var resultSet = statement.executeQuery(checkDbSql)) {
                dbExists = resultSet.next();
            }
            
            if (!dbExists) {
                // 创建数据库
                String createDbSql = String.format(
                    "CREATE DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    databaseName
                );
                statement.execute(createDbSql);
                log.info("✅ 数据库 {} 创建成功", databaseName);
            } else {
                log.info("数据库 {} 已存在", databaseName);
            }
            
        } catch (SQLException e) {
            log.error("❌ 创建数据库失败", e);
            throw new RuntimeException("创建数据库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取MySQL系统库连接URL
     */
    private String getMysqlSystemUrl() {
        // 从原始URL中提取主机、端口等信息，但连接到mysql系统库
        String originalUrl = databaseUrl;
        
        // 处理URL格式: jdbc:mysql://host:port/database?params
        if (originalUrl.contains("?")) {
            // 有参数的情况
            String[] parts = originalUrl.split("\\?");
            String baseUrl = parts[0];
            String params = parts[1];
            
            // 替换数据库名
            String hostPort = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            return hostPort + "/mysql?" + params;
        } else {
            // 没有参数的情况
            String hostPort = originalUrl.substring(0, originalUrl.lastIndexOf("/"));
            return hostPort + "/mysql";
        }
    }

    /**
     * 检查数据库连接
     */
    private void checkDatabaseConnection() {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 尝试连接到 weeb 数据库
                jdbcTemplate.execute("USE " + databaseName);
                jdbcTemplate.execute("SELECT 1");
                log.info("✅ 数据库连接验证成功");
                return;
            } catch (Exception e) {
                retryCount++;
                log.warn("数据库连接尝试 {}/{} 失败: {}", retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    log.error("❌ 数据库连接失败，已重试 {} 次", maxRetries);
                    throw new RuntimeException("数据库连接失败", e);
                }
                
                // 等待一秒后重试
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待重试时被中断", ie);
                }
            }
        }
    }

    /**
     * 检查并创建所有表
     */
    private void checkAndCreateTables() {
        log.info("开始检查数据库表...");

        // 按依赖关系顺序创建表
        
        // 1. 核心用户表
        if (!tableExists("user")) {
            createUserTable();
        }

        // 2. 用户统计表（依赖 user 表）
        if (!tableExists("user_stats")) {
            createUserStatsTable();
        }

        // 3. 文章表（依赖 user 表）
        if (!tableExists("articles")) {
            createArticleTable();
        }

        // 4. 群组表（依赖 user 表）
        if (!tableExists("`group`")) {
            createGroupTable();
        }

        // 5. 群组成员表（依赖 group 和 user 表）
        if (!tableExists("group_member")) {
            createGroupMemberTable();
        }

        // 6. 联系人表（依赖 user 表）
        if (!tableExists("contact")) {
            createContactTable();
        }

        // 7. 聊天列表表（依赖 user 表）
        if (!tableExists("chat_list")) {
            createChatListTable();
        }

        // 8. 消息表（依赖 user 和 group 表）
        if (!tableExists("message")) {
            createMessageTable();
        }

        // 9. 消息反应表（依赖 message 和 user 表）
        if (!tableExists("message_reaction")) {
            createMessageReactionTable();
        }

        // 10. 通知表（依赖 user 表）
        if (!tableExists("notifications")) {
            createNotificationTable();
        }

        // 11. 文件传输表（依赖 user 表）
        if (!tableExists("file_transfer")) {
            createFileTransferTable();
        }

        // 12. 文章评论表（依赖 article 和 user 表）
        if (!tableExists("article_comment")) {
            createArticleCommentTable();
        }

        // 13. 文章收藏表（依赖 article 和 user 表）
        if (!tableExists("article_favorite")) {
            createArticleFavoriteTable();
        }

        // 14. 文章分类表
        if (!tableExists("article_category")) {
            createArticleCategoryTable();
        }

        // 15. 文章标签表
        if (!tableExists("article_tag")) {
            createArticleTagTable();
        }

        // 16. 文章与标签关联表
        if (!tableExists("article_tag_relation")) {
            createArticleTagRelationTable();
        }

        // 17. 文件记录表（依赖 user 表）
        if (!tableExists("file_record")) {
            createFileRecordTable();
        }

        // 18. 用户关注表（依赖 user 表）
        if (!tableExists("user_follow")) {
            createUserFollowTable();
        }

        // 19. 文件分享表（依赖 file_record 表）
        if (!tableExists("file_share")) {
            createFileShareTable();
        }

        log.info("数据库表检查完成");
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
            jdbcTemplate.execute(sql);
            log.info("表 {} 已存在", tableName);
            return true;
        } catch (Exception e) {
            log.info("表 {} 不存在，将创建", tableName);
            return false;
        }
    }

    private void createUserTable() {
        log.info("创建用户表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `user` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
                `sex` TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0为女，1为男',
                `phone_number` VARCHAR(20) COMMENT '电话号码',
                `user_email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
                `unique_article_link` VARCHAR(255) COMMENT '唯一标识文章链接',
                `unique_video_link` VARCHAR(255) COMMENT '唯一标识视频链接',
                `registration_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册日期',
                `ip_ownership` VARCHAR(100) COMMENT '用户IP归属地',
                `type` VARCHAR(50) DEFAULT 'USER' COMMENT '用户类型：USER、ADMIN、VIP等',
                `avatar` VARCHAR(500) COMMENT '用户头像URL',
                `nickname` VARCHAR(100) COMMENT '用户昵称',
                `badge` VARCHAR(255) COMMENT '用户徽章信息',
                `login_time` DATETIME COMMENT '最后一次登录时间',
                `bio` TEXT COMMENT '个人简介',
                `online_status` TINYINT DEFAULT 0 COMMENT '在线状态：0离线，1在线，2忙碌，3离开',
                `status` TINYINT DEFAULT 1 COMMENT '用户状态：0-禁用，1-启用',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_username` (`username`),
                UNIQUE KEY `uk_user_email` (`user_email`),
                KEY `idx_registration_date` (`registration_date`),
                KEY `idx_login_time` (`login_time`),
                KEY `idx_type` (`type`),
                KEY `idx_online_status` (`online_status`),
                KEY `idx_status` (`status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='用户基础信息表（统计数据分离至user_stats表）'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 用户表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建用户表失败", e);
            throw new RuntimeException("创建用户表失败", e);
        }
    }

    private void createUserStatsTable() {
        log.info("创建用户统计表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `user_stats` (
                `user_id` BIGINT NOT NULL COMMENT '用户ID（外键）',
                `fans_count` BIGINT DEFAULT 0 COMMENT '粉丝数量',
                `total_likes` BIGINT DEFAULT 0 COMMENT '总点赞数',
                `total_favorites` BIGINT DEFAULT 0 COMMENT '总收藏数',
                `total_sponsorship` BIGINT DEFAULT 0 COMMENT '总赞助数',
                `total_article_exposure` BIGINT DEFAULT 0 COMMENT '文章总曝光数',
                `website_coins` BIGINT DEFAULT 0 COMMENT '网站金币',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`user_id`),
                CONSTRAINT `fk_user_stats_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                KEY `idx_fans_count` (`fans_count`),
                KEY `idx_total_likes` (`total_likes`),
                KEY `idx_website_coins` (`website_coins`),
                KEY `idx_updated_at` (`updated_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='用户统计数据表（分离以提高并发性能）'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 用户统计表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建用户统计表失败", e);
            throw new RuntimeException("创建用户统计表失败", e);
        }
    }

    private void createMessageTable() {
        log.info("创建消息表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `message` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
                `chat_id` BIGINT COMMENT '聊天列表ID',
                `content` JSON NOT NULL COMMENT '消息内容（JSON格式）',
                `message_type` INT DEFAULT 1 COMMENT '消息类型：1文本，2图片，3文件等',
                `read_status` INT DEFAULT 0 COMMENT '读取状态：0未读，1已读',
                `is_recalled` INT DEFAULT 0 COMMENT '是否撤回：0否，1是',
                `user_ip` VARCHAR(45) COMMENT '发送者IP地址',
                `source` VARCHAR(50) COMMENT '消息来源：WEB、MOBILE、API等',
                `is_show_time` INT DEFAULT 0 COMMENT '是否显示时间：0否，1是',
                `reply_to_message_id` BIGINT COMMENT '回复的消息ID',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_sender_id` (`sender_id`),
                KEY `idx_chat_id` (`chat_id`),
                KEY `idx_created_at` (`created_at`),
                KEY `idx_message_type` (`message_type`),
                KEY `idx_read_status` (`read_status`),
                KEY `idx_reply_to_message_id` (`reply_to_message_id`),
                CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_message_reply` FOREIGN KEY (`reply_to_message_id`) REFERENCES `message` (`id`) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='消息内容表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 消息表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建消息表失败", e);
            throw new RuntimeException("创建消息表失败", e);
        }
    }

    private void createGroupTable() {
        log.info("创建群组表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
                `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
                `owner_id` BIGINT NOT NULL COMMENT '群主ID',
                `group_avatar_url` VARCHAR(500) COMMENT '群组头像URL',
                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                KEY `idx_owner_id` (`owner_id`),
                KEY `idx_create_time` (`create_time`),
                CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组信息表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 群组表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建群组表失败", e);
            throw new RuntimeException("创建群组表失败", e);
        }
    }

    private void createGroupMemberTable() {
        log.info("创建群组成员表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group_member` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
                `group_id` BIGINT NOT NULL COMMENT '群组ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `role` INT NOT NULL DEFAULT 3 COMMENT '成员角色：1群主，2管理员，3普通成员',
                `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_role` (`role`),
                KEY `idx_join_time` (`join_time`),
                CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组成员关系表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 群组成员表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建群组成员表失败", e);
            throw new RuntimeException("创建群组成员表失败", e);
        }
    }

    private void createChatListTable() {
        log.info("创建聊天列表表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `chat_list` (
                `id` VARCHAR(255) NOT NULL COMMENT '聊天列表ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `target_id` BIGINT COMMENT '目标ID（用户ID或群组ID）',
                `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
                `target_info` TEXT NOT NULL COMMENT '目标信息（用户名或群组名）',
                `type` VARCHAR(255) NOT NULL COMMENT '聊天类型：PRIVATE、GROUP',
                `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
                `last_message` TEXT COMMENT '最后一条消息内容',
                `create_time` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                `update_time` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_target_id` (`target_id`),
                KEY `idx_group_id` (`group_id`),
                KEY `idx_type` (`type`),
                KEY `idx_update_time` (`update_time`),
                CONSTRAINT `fk_chat_list_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='用户聊天列表表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 聊天列表表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建聊天列表表失败", e);
            throw new RuntimeException("创建聊天列表表失败", e);
        }
    }

    private void createArticleTable() {
        log.info("创建文章表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `articles` (
                `article_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
                `user_id` BIGINT NOT NULL COMMENT '作者ID',
                `category_id` BIGINT COMMENT '分类ID',
                `article_title` VARCHAR(200) NOT NULL COMMENT '文章标题',
                `article_content` LONGTEXT COMMENT '文章内容',
                `article_link` VARCHAR(500) COMMENT '文章外部链接',
                `likes_count` INT DEFAULT 0 COMMENT '点赞数',
                `favorites_count` INT DEFAULT 0 COMMENT '收藏数',
                `sponsors_count` DECIMAL(10,2) DEFAULT 0.00 COMMENT '赞助金额',
                `exposure_count` BIGINT DEFAULT 0 COMMENT '曝光/阅读数',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                `status` TINYINT DEFAULT 1 COMMENT '状态：0草稿，1发布，2删除',
                PRIMARY KEY (`article_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_category_id` (`category_id`),
                KEY `idx_created_at` (`created_at`),
                KEY `idx_status` (`status`),
                KEY `idx_likes_count` (`likes_count`),
                KEY `idx_exposure_count` (`exposure_count`),
                FULLTEXT KEY `ft_title_content` (`article_title`, `article_content`),
                CONSTRAINT `fk_articles_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_articles_category` FOREIGN KEY (`category_id`) REFERENCES `article_category` (`id`) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章内容表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章表失败", e);
            throw new RuntimeException("创建文章表失败", e);
        }
    }

    private void createMessageReactionTable() {
        log.info("创建消息反应表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `message_reaction` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反应ID',
                `message_id` BIGINT NOT NULL COMMENT '消息ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `reaction_type` VARCHAR(50) NOT NULL COMMENT '反应类型：如👍、❤️、😂等',
                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_message_user_reaction` (`message_id`, `user_id`, `reaction_type`),
                KEY `idx_message_id` (`message_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_reaction_type` (`reaction_type`),
                CONSTRAINT `fk_reaction_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_reaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='消息反应表（点赞、表情等）'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 消息反应表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建消息反应表失败", e);
            throw new RuntimeException("创建消息反应表失败", e);
        }
    }

    private void createNotificationTable() {
        log.info("创建通知表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
                `recipient_id` BIGINT NOT NULL COMMENT '接收者ID',
                `actor_id` BIGINT COMMENT '操作者ID',
                `type` VARCHAR(50) NOT NULL COMMENT '通知类型：LIKE、COMMENT、FOLLOW、MESSAGE等',
                `entity_type` VARCHAR(50) COMMENT '实体类型：ARTICLE、USER、GROUP等',
                `entity_id` BIGINT COMMENT '实体ID',
                `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                KEY `idx_recipient_id` (`recipient_id`),
                KEY `idx_actor_id` (`actor_id`),
                KEY `idx_type` (`type`),
                KEY `idx_entity_type_id` (`entity_type`, `entity_id`),
                KEY `idx_is_read` (`is_read`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_notification_actor` FOREIGN KEY (`actor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='系统通知表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 通知表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建通知表失败", e);
            throw new RuntimeException("创建通知表失败", e);
        }
    }

    private void createContactTable() {
        log.info("创建联系人表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `contact` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `friend_id` BIGINT NOT NULL COMMENT '好友ID',
                `status` INT NOT NULL DEFAULT 0 COMMENT '关系状态：0待确认，1已确认，2已拒绝，3已删除',
                `remarks` VARCHAR(255) COMMENT '备注/申请附言',
                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
                KEY `idx_friend_id` (`friend_id`),
                KEY `idx_status` (`status`),
                KEY `idx_create_time` (`create_time`),
                CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_contact_friend` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='用户联系人关系表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 联系人表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建联系人表失败", e);
            throw new RuntimeException("创建联系人表失败", e);
        }
    }

    private void createFileTransferTable() {
        log.info("创建文件传输表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `file_transfer` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '传输ID',
                `initiator_id` BIGINT NOT NULL COMMENT '发起者ID',
                `target_id` BIGINT NOT NULL COMMENT '目标用户ID',
                `offer_sdp` TEXT COMMENT 'WebRTC Offer SDP',
                `answer_sdp` TEXT COMMENT 'WebRTC Answer SDP',
                `candidate` TEXT COMMENT 'ICE Candidate信息',
                `status` INT DEFAULT 0 COMMENT '传输状态：0邀请，1已提供，2已应答，3传输中，4完成，5失败',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_initiator_id` (`initiator_id`),
                KEY `idx_target_id` (`target_id`),
                KEY `idx_status` (`status`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_file_transfer_initiator` FOREIGN KEY (`initiator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_file_transfer_target` FOREIGN KEY (`target_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='P2P文件传输表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文件传输表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文件传输表失败", e);
            throw new RuntimeException("创建文件传输表失败", e);
        }
    }

    /**
     * 插入初始数据
     */
    private void insertInitialData() {
        log.info("开始插入初始数据...");

        try {
            // 检查是否已有管理员用户
            Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = 'admin'", Integer.class);
            
            if (adminCount == null || adminCount == 0) {
                // 插入管理员用户
                jdbcTemplate.update("""
                    INSERT INTO user (username, password, user_email, nickname, type, online_status)
                    VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKV6biekKXsE6X5w5R2fK5U2gO6', 'admin@example.com', '管理员', 'ADMIN', 0)
                    """);
                
                log.info("✅ 管理员用户创建成功");
            }
            
            // 检查是否已有文章分类
            Integer categoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_category", Integer.class);
            
            if (categoryCount == null || categoryCount == 0) {
                // 插入默认文章分类
                jdbcTemplate.update("""
                    INSERT INTO article_category (category_name, parent_id) VALUES
                    ('技术', NULL),
                    ('生活', NULL),
                    ('娱乐', NULL),
                    ('体育', NULL),
                    ('新闻', NULL),
                    ('教育', NULL),
                    ('前端开发', 1),
                    ('后端开发', 1),
                    ('移动开发', 1),
                    ('数据库', 1),
                    ('美食', 2),
                    ('旅游', 2),
                    ('健康', 2),
                    ('电影', 3),
                    ('音乐', 3),
                    ('游戏', 3),
                    ('足球', 4),
                    ('篮球', 4),
                    ('网球', 4),
                    ('国内新闻', 5),
                    ('国际新闻', 5),
                    ('科技新闻', 5),
                    ('编程教程', 6),
                    ('语言学习', 6),
                    ('职业发展', 6)
                    """);
                
                log.info("✅ 默认文章分类创建成功");
            }
            
            // 检查是否已有文章标签
            Integer tagCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_tag", Integer.class);
            
            if (tagCount == null || tagCount == 0) {
                // 插入默认文章标签
                jdbcTemplate.update("""
                    INSERT INTO article_tag (tag_name) VALUES
                    ('Java'),
                    ('Python'),
                    ('JavaScript'),
                    ('Vue'),
                    ('React'),
                    ('Spring Boot'),
                    ('MySQL'),
                    ('Redis'),
                    ('Docker'),
                    ('Linux'),
                    ('算法'),
                    ('数据结构'),
                    ('设计模式'),
                    ('前端'),
                    ('后端'),
                    ('全栈'),
                    ('微服务'),
                    ('分布式'),
                    ('高并发')
                    """);
                
                log.info("✅ 默认文章标签创建成功");
            }
            
        } catch (Exception e) {
            log.error("插入初始数据失败", e);
            throw new RuntimeException("插入初始数据失败", e);
        }
    }

    private void createArticleCommentTable() {
        log.info("创建文章评论表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_comment` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                `user_id` BIGINT NOT NULL COMMENT '评论者ID',
                `content` TEXT NOT NULL COMMENT '评论内容',
                `parent_id` BIGINT COMMENT '父评论ID（用于回复）',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_article_id` (`article_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_parent_id` (`parent_id`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE,
                CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `article_comment` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章评论表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章评论表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章评论表失败", e);
            throw new RuntimeException("创建文章评论表失败", e);
        }
    }

    private void createArticleFavoriteTable() {
        log.info("创建文章收藏表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_favorite` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_article_id` (`article_id`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_favorite_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章收藏记录表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章收藏表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章收藏表失败", e);
            throw new RuntimeException("创建文章收藏表失败", e);
        }
    }

    private void createArticleCategoryTable() {
        log.info("创建文章分类表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_category` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
                `parent_id` BIGINT COMMENT '父分类ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_category_name` (`category_name`),
                KEY `idx_parent_id` (`parent_id`),
                KEY `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章分类表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章分类表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章分类表失败", e);
            throw new RuntimeException("创建文章分类表失败", e);
        }
    }

    private void createArticleTagTable() {
        log.info("创建文章标签表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_tag` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
                `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_tag_name` (`tag_name`),
                KEY `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章标签表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章标签表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章标签表失败", e);
            throw new RuntimeException("创建文章标签表失败", e);
        }
    }

    private void createArticleTagRelationTable() {
        log.info("创建文章与标签关联表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_tag_relation` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                `tag_id` BIGINT NOT NULL COMMENT '标签ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
                KEY `idx_article_id` (`article_id`),
                KEY `idx_tag_id` (`tag_id`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_tag_relation_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE,
                CONSTRAINT `fk_tag_relation_tag` FOREIGN KEY (`tag_id`) REFERENCES `article_tag` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章与标签关联表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章与标签关联表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章与标签关联表失败", e);
            throw new RuntimeException("创建文章与标签关联表失败", e);
        }
    }

    private void createFileRecordTable() {
        log.info("创建文件记录表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `file_record` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
                `user_id` BIGINT NOT NULL COMMENT '上传者ID',
                `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
                `stored_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
                `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
                `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
                `mime_type` VARCHAR(100) COMMENT 'MIME类型',
                `file_hash` VARCHAR(64) NOT NULL COMMENT '文件哈希值（用于去重）',
                `is_public` BOOLEAN DEFAULT FALSE COMMENT '是否公开',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_file_hash` (`file_hash`),
                KEY `idx_is_public` (`is_public`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_file_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='通用文件管理表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文件记录表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文件记录表失败", e);
            throw new RuntimeException("创建文件记录表失败", e);
        }
    }

    private void createUserFollowTable() {
        log.info("创建用户关注表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `user_follow` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注关系ID',
                `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
                `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
                KEY `idx_follower_id` (`follower_id`),
                KEY `idx_followee_id` (`followee_id`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='用户关注关系表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 用户关注表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建用户关注表失败", e);
            throw new RuntimeException("创建用户关注表失败", e);
        }
    }

    private void createFileShareTable() {
        log.info("创建文件分享表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `file_share` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分享记录ID',
                `file_id` BIGINT NOT NULL COMMENT '文件记录ID',
                `sharer_id` BIGINT NOT NULL COMMENT '分享者用户ID',
                `shared_to_user_id` BIGINT COMMENT '被分享者用户ID（可为空，表示公开分享）',
                `share_token` VARCHAR(255) NOT NULL COMMENT '分享链接token',
                `permission` VARCHAR(50) NOT NULL COMMENT '分享权限：READ, DOWNLOAD',
                `expires_at` DATETIME COMMENT '过期时间（可为空，表示永不过期）',
                `status` VARCHAR(50) DEFAULT 'ACTIVE' COMMENT '分享状态：ACTIVE, EXPIRED, REVOKED',
                `access_count` BIGINT DEFAULT 0 COMMENT '访问次数',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_share_token` (`share_token`),
                KEY `idx_file_id` (`file_id`),
                KEY `idx_sharer_id` (`sharer_id`),
                KEY `idx_shared_to_user_id` (`shared_to_user_id`),
                KEY `idx_status` (`status`),
                KEY `idx_expires_at` (`expires_at`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_file_share_file` FOREIGN KEY (`file_id`) REFERENCES `file_record` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_file_share_sharer` FOREIGN KEY (`sharer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_file_share_shared_to` FOREIGN KEY (`shared_to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='文件分享记录表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文件分享表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文件分享表失败", e);
            throw new RuntimeException("创建文件分享表失败", e);
        }
    }
}