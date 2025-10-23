package com.web.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
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
@Order(1) // 设置最高优先级，确保最先执行
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
        log.info("开始检查数据库表结构...");

        // 定义所有需要检查的表及其结构
        String[][] tables = {
            {"user", "id,username,password,sex,phone_number,user_email,unique_article_link,unique_video_link,registration_date,ip_ownership,type,avatar,nickname,badge,login_time,bio,online_status,status"},
            {"user_stats", "user_id,fans_count,total_likes,total_favorites,total_sponsorship,total_article_exposure,website_coins,created_at,updated_at"},
            {"`group`", "id,group_name,owner_id,group_avatar_url,create_time"},
            {"group_member", "id,group_id,user_id,role,join_time,update_time"},
            {"contact", "id,user_id,friend_id,status,remarks,create_time,update_time"},
            {"chat_list", "id,user_id,target_id,group_id,target_info,type,unread_count,last_message,create_time,update_time"},
            {"message", "id,sender_id,chat_id,content,message_type,read_status,is_recalled,user_ip,source,is_show_time,reply_to_message_id,created_at,updated_at"},
            {"message_reaction", "id,message_id,user_id,reaction_type,create_time"},
            {"notifications", "id,recipient_id,actor_id,type,entity_type,entity_id,is_read,created_at"},
            {"file_transfer", "id,initiator_id,target_id,offer_sdp,answer_sdp,candidate,status,created_at,updated_at"},
            {"article_category", "id,category_name,parent_id,created_at"},
            {"articles", "article_id,user_id,category_id,article_title,article_content,article_link,likes_count,favorites_count,sponsors_count,exposure_count,created_at,updated_at,status"},
            {"article_comment", "id,article_id,user_id,content,parent_id,created_at,updated_at"},
            {"article_favorite", "id,user_id,article_id,created_at"},
            {"article_tag", "id,tag_name,created_at"},
            {"article_tag_relation", "id,article_id,tag_id,created_at"},
            {"file_record", "id,user_id,file_name,stored_name,file_path,file_size,mime_type,file_hash,is_public,created_at,updated_at"},
            {"user_follow", "id,follower_id,followee_id,created_at"},
            {"file_share", "id,file_id,sharer_id,shared_to_user_id,share_token,permission,expires_at,status,access_count,created_at,updated_at"},
            {"system_logs", "id,operator_id,action,details,ip_address,created_at"},
            {"permission", "id,name,description,resource,action,condition,status,type,group,sort_order,created_at,updated_at"},
            {"role", "id,name,description,status,type,level,is_default,created_at,updated_at"},
            {"role_permission", "id,role_id,permission_id,created_at"},
            {"user_role", "id,user_id,role_id,created_at"}
        };

        boolean allTablesValid = true;

        // 检查每个表的结构
        for (String[] tableInfo : tables) {
            String tableName = tableInfo[0];
            String[] expectedColumns = tableInfo[1].split(",");

            if (!validateTableStructure(tableName, expectedColumns)) {
                log.warn("表 {} 结构不符合预期，将重建数据库", tableName);
                allTablesValid = false;
                break;
            }
        }

        // 如果有任何表结构不符合要求，重建整个数据库
        if (!allTablesValid) {
            log.info("开始重建数据库...");
            rebuildDatabase();
        } else {
            log.info("✅ 所有表结构验证通过");
        }

        log.info("数据库表结构检查完成");
    }

    /**
     * 验证表结构是否符合预期
     */
    private boolean validateTableStructure(String tableName, String[] expectedColumns) {
        try {
            // 检查表是否存在
            if (!tableExists(tableName)) {
                log.info("表 {} 不存在", tableName);
                return false;
            }

            // 获取表的列信息
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" +
                        databaseName + "' AND TABLE_NAME = " +
                        (tableName.startsWith("`") ? tableName : "'" + tableName + "'");

            java.util.List<String> actualColumns = jdbcTemplate.queryForList(sql, String.class);

            // 检查每个预期的列是否存在
            for (String expectedColumn : expectedColumns) {
                expectedColumn = expectedColumn.trim();
                if (!actualColumns.contains(expectedColumn)) {
                    log.warn("表 {} 缺少列: {}", tableName, expectedColumn);
                    return false;
                }
            }

            log.debug("表 {} 结构验证通过", tableName);
            return true;

        } catch (Exception e) {
            log.error("验证表 {} 结构时出错: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 重建整个数据库
     */
    private void rebuildDatabase() {
        try {
            log.info("删除旧数据库...");
            String mysqlSystemUrl = getMysqlSystemUrl();

            try (Connection connection = java.sql.DriverManager.getConnection(mysqlSystemUrl, dbUsername, dbPassword);
                 Statement statement = connection.createStatement()) {

                // 删除数据库
                String dropDbSql = "DROP DATABASE IF EXISTS `" + databaseName + "`";
                statement.execute(dropDbSql);
                log.info("✅ 旧数据库删除成功");

                // 创建新数据库
                String createDbSql = "CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
                statement.execute(createDbSql);
                log.info("✅ 新数据库创建成功");
            }

            // 重新连接到新数据库并创建所有表
            checkDatabaseConnection();
            createAllTables();
            insertInitialData();

            log.info("✅ 数据库重建完成");

        } catch (Exception e) {
            log.error("❌ 数据库重建失败", e);
            throw new RuntimeException("数据库重建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建所有表
     */
    private void createAllTables() {
        log.info("开始创建所有表...");

        // 按依赖关系顺序创建表
        createUserTable();
        createUserStatsTable();
        createPermissionTable(); // 权限表必须在角色表之前
        createRoleTable();
        createRolePermissionTable();
        createUserRoleTable();
        createGroupTable();
        createGroupMemberTable();
        createContactTable();
        createChatListTable();
        createMessageTable();
        createMessageReactionTable();
        createNotificationTable();
        createFileTransferTable();
        createArticleCategoryTable();
        createArticleTable();
        createArticleCommentTable();
        createArticleFavoriteTable();
        createArticleTagTable();
        createArticleTagRelationTable();
        createFileRecordTable();
        createUserFollowTable();
        createFileShareTable();
        createSystemLogTable(); // 新增调用

        log.info("✅ 所有表创建完成");
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

    private void createPermissionTable() {
        log.info("创建权限表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `permission` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                `name` VARCHAR(100) NOT NULL COMMENT '权限名称（e.g., USER_CREATE）',
                `description` VARCHAR(255) COMMENT '权限描述',
                `resource` VARCHAR(100) NOT NULL COMMENT '资源 (e.g., user, article)',
                `action` VARCHAR(100) NOT NULL COMMENT '操作 (e.g., create, read, update, delete)',
                `condition` VARCHAR(255) COMMENT '条件 (e.g., own, any)',
                `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                `type` TINYINT DEFAULT 1 COMMENT '类型：0-系统权限，1-业务权限',
                `group` VARCHAR(100) COMMENT '权限分组',
                `sort_order` INT DEFAULT 0 COMMENT '排序号',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_name` (`name`),
                KEY `idx_resource` (`resource`),
                KEY `idx_action` (`action`),
                KEY `idx_status` (`status`),
                KEY `idx_type` (`type`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='系统权限表';
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 权限表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建权限表失败", e);
            throw new RuntimeException("创建权限表失败", e);
        }
    }

    private void createRoleTable() {
        log.info("创建角色表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `role` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                `name` VARCHAR(100) NOT NULL COMMENT '角色名称',
                `description` VARCHAR(255) COMMENT '角色描述',
                `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                `type` TINYINT DEFAULT 0 COMMENT '类型：0-系统角色，1-自定义角色',
                `level` INT DEFAULT 100 COMMENT '角色等级（数字越小权限越大）',
                `is_default` BOOLEAN DEFAULT FALSE COMMENT '是否为新用户默认角色',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_name` (`name`),
                KEY `idx_status` (`status`),
                KEY `idx_type` (`type`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='用户角色表';
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 角色表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建角色表失败", e);
            throw new RuntimeException("创建角色表失败", e);
        }
    }

    private void createRolePermissionTable() {
        log.info("创建角色权限关联表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `role_permission` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `role_id` BIGINT NOT NULL COMMENT '角色ID',
                `permission_id` BIGINT NOT NULL COMMENT '权限ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
                CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_rp_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='角色与权限关联表';
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 角色权限关联表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建角色权限关联表失败", e);
            throw new RuntimeException("创建角色权限关联表失败", e);
        }
    }

    private void createUserRoleTable() {
        log.info("创建用户角色关联表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `user_role` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `role_id` BIGINT NOT NULL COMMENT '角色ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
                CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='用户与角色关联表';
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 用户角色关联表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建用户角色关联表失败", e);
            throw new RuntimeException("创建用户角色关联表失败", e);
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
            log.info("检查并创建默认用户...");

            // 检查是否已有管理员用户
            Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = 'admin'", Integer.class);

            if (adminCount == null || adminCount == 0) {
                // 插入管理员用户 (密码: admin123)
                jdbcTemplate.update("""
                    INSERT INTO user (username, password, user_email, nickname, type, online_status, status, registration_date)
                    VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKV6biekKXsE6X5w5R2fK5U2gO6', 'admin@weeb.com', '系统管理员', 'ADMIN', 0, 1, NOW())
                    """);

                // 获取管理员用户ID并插入统计信息
                Long adminId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                jdbcTemplate.update("""
                    INSERT INTO user_stats (user_id, fans_count, total_likes, total_favorites, total_sponsorship, total_article_exposure, website_coins)
                    VALUES (?, 100, 50, 25, 1000.00, 5000, 1000)
                    """, adminId);

                log.info("✅ 管理员用户创建成功 (用户名: admin, 密码: admin123)");
            } else {
                log.info("管理员用户已存在，跳过创建");
            }

            // 检查是否已有测试用户
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = 'testuser'", Integer.class);

            if (userCount == null || userCount == 0) {
                // 插入测试用户 (密码: test123)
                // 使用BCrypt加密密码: test123 -> $2a$10$T1q2gCReMhlPLkB4zBuZC.bIVAbBl/BpHBvurrI2NQkBqNHuHeiYe
                jdbcTemplate.update("""
                    INSERT INTO user (username, password, user_email, nickname, type, online_status, status)
                    VALUES ('testuser', '$2a$10$T1q2gCReMhlPLkB4zBuZC.bIVAbBl/BpHBvurrI2NQkBqNHuHeiYe', 'test@weeb.com', '测试用户', 'USER', 1, 1)
                    """);

                // 获取测试用户ID并插入统计信息
                Long testUserId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                jdbcTemplate.update("""
                    INSERT INTO user_stats (user_id, fans_count, total_likes, total_favorites, total_sponsorship, total_article_exposure, website_coins)
                    VALUES (?, 10, 5, 3, 100.00, 500, 100)
                    """, testUserId);

                log.info("✅ 测试用户创建成功 (用户名: testuser, 密码: test123)");
            }

            // 插入更多测试用户 (密码: password + 数字)
            String[][] testUsers = {
                {"alice", "alice@weeb.com", "爱丽丝", "$2a$10$hbTBtOr3hqz4NV7xItTtluXdgtH/XSmu2Sa3BGMex6BiP/PlQ/lZ2"}, // password100
                {"bob", "bob@weeb.com", "鲍勃", "$2a$10$HeB5dudFSSMQ6ICVunOlHuVoBFydle0x3tIAj9xMTKlvsNGZ2zbO6"}, // password200
                {"charlie", "charlie@weeb.com", "查理", "$2a$10$q.3Zrap1HhMeNrarmTTWduJZNQ6mtWY8pe6k7uzkDmAo6ZRy6pbya"}, // password300
                {"diana", "diana@weeb.com", "戴安娜", "$2a$10$vyksYpUq50fq1tD774bvMesIZsrPn43UMMfAZwIlEEpVvumvCtKwa"}, // password400
                {"eve", "eve@weeb.com", "伊芙", "$2a$10$bk5/XQfztGfK/1zuXNqKIOWxbbo1uwZFMh9Ws7/yIVZVwFpeiscgW"} // password500
            };

            for (String[] userInfo : testUsers) {
                String username = userInfo[0];
                String email = userInfo[1];
                String nickname = userInfo[2];
                String passwordHash = userInfo[3];

                Integer existingUser = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE username = '" + username + "'", Integer.class);

                if (existingUser == null || existingUser == 0) {
                    jdbcTemplate.update("""
                        INSERT INTO user (username, password, user_email, nickname, type, online_status, status)
                        VALUES (?, ?, ?, ?, 'USER', 1, 1)
                        """, username, passwordHash, email, nickname);

                    Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                    jdbcTemplate.update("""
                        INSERT INTO user_stats (user_id, fans_count, total_likes, total_favorites, total_sponsorship, total_article_exposure, website_coins)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, userId, (int)(Math.random() * 50), (int)(Math.random() * 25), (int)(Math.random() * 10),
                           Math.random() * 500, Math.random() * 1000, Math.random() * 200);

                    log.info("✅ 测试用户 {} 创建成功", nickname);
                }
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

    private void createSystemLogTable() {
        log.info("创建系统日志表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `system_logs` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                `operator_id` BIGINT COMMENT '操作员ID',
                `action` VARCHAR(100) NOT NULL COMMENT '操作类型 (e.g., BAN_USER, CREATE_ROLE)',
                `details` TEXT COMMENT '操作详情',
                `ip_address` VARCHAR(45) COMMENT '操作员IP地址',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                KEY `idx_operator_id` (`operator_id`),
                KEY `idx_action` (`action`),
                KEY `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            COMMENT='系统操作日志表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 系统日志表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建系统日志表失败", e);
            throw new RuntimeException("创建系统日志表失败", e);
        }
    }
}