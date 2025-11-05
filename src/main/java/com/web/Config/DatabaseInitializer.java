package com.web.Config;

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
            {"`group`", "id,group_name,owner_id,group_avatar_url,group_description,status,max_members,member_count,is_visible,category_id,tags,last_transfer_at,transfer_count,create_time,update_time"},
            {"group_member", "id,group_id,user_id,role,join_status,invited_by,invite_reason,join_time,update_time,kicked_at,kick_reason"},
            {"group_transfer_history", "id,group_id,from_user_id,to_user_id,transfer_reason,transfer_at"},
            {"group_application", "id,group_id,user_id,message,status,reviewer_id,review_note,created_at,reviewed_at"},
            {"contact", "id,user_id,friend_id,status,remarks,expire_at,group_id,create_time,update_time"},
            {"contact_group", "id,user_id,group_name,group_order,is_default,created_at,updated_at"},
            {"chat_list", "id,user_id,target_id,group_id,target_info,type,unread_count,last_message,create_time,update_time"},
            {"message", "id,client_message_id,sender_id,receiver_id,group_id,chat_id,content,message_type,read_status,is_read,is_recalled,status,user_ip,source,is_show_time,reply_to_message_id,created_at,updated_at"},
            {"message_reaction", "id,message_id,user_id,reaction_type,create_time"},
            {"notifications", "id,recipient_id,actor_id,type,entity_type,entity_id,is_read,created_at"},
            {"file_transfer", "id,initiator_id,target_id,offer_sdp,answer_sdp,candidate,status,created_at,updated_at"},
            {"article_category", "id,category_name,parent_id,created_at"},
            {"articles", "article_id,user_id,category_id,article_title,article_content,article_link,likes_count,favorites_count,sponsors_count,exposure_count,status,reviewer_id,reviewed_at,review_note,has_sensitive_words,review_priority,created_at,updated_at"},
            {"article_moderation_history", "id,article_id,reviewer_id,action,reason,previous_status,new_status,created_at"},
            {"article_comment", "id,article_id,user_id,content,parent_id,created_at,updated_at"},
            {"article_like", "id,user_id,article_id,created_at"},
            {"article_favorite", "id,user_id,article_id,created_at"},
            {"article_tag", "id,tag_name,created_at"},
            {"article_tag_relation", "id,article_id,tag_id,created_at"},
            {"user_follow", "id,follower_id,followee_id,created_at"},
            {"system_logs", "id,operator_id,action,details,ip_address,created_at"},
            {"user_level_history", "id,user_id,old_level,new_level,change_reason,change_type,operator_id,operator_name,change_time,ip_address,user_agent,remark,status,created_at,updated_at"},
            {"article_version", "id,article_id,version_number,title,content,summary,article_link,category_id,status,tags,cover_image,version_note,created_by,created_by_username,change_type,change_summary,character_change,is_major_version,is_auto_save,size,content_hash,created_at,updated_at"},
            {"content_report", "id,reporter_id,content_type,content_id,reason,description,status,reviewer_id,action,review_note,reviewed_at,metadata,report_count,is_urgent,created_at,updated_at"}
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
            String metaTableName = tableName.replace("`", "");
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

            java.util.List<String> actualColumns = jdbcTemplate.queryForList(sql, String.class, databaseName, metaTableName);

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
        createArticleLikeTable();
        createArticleFavoriteTable();
        createArticleTagTable();
        createArticleTagRelationTable();
        // 文件上传功能已禁用
        // createFileRecordTable();
        // createFileShareTable();
        createUserFollowTable();
        createSystemLogTable(); // 新增调用
        createUserLevelHistoryTable(); // 用户等级历史表
        createArticleVersionTable(); // 文章版本表
        createContentReportTable(); // 内容举报表
        createContactGroupTable(); // 联系人分组表
        createGroupTransferHistoryTable(); // 群组转让历史表
        createGroupApplicationTable(); // 群组申请表
        createArticleModerationHistoryTable(); // 文章审核历史表

        log.info("✅ 所有表创建完成");
    }

    private boolean tableExists(String tableName) {
        try {
            String rawName = tableName.replace("`", "");
            String identifier = "`" + rawName + "`";
            String sql = "SELECT 1 FROM " + identifier + " LIMIT 1";
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

    // RBAC table creation methods have been removed

    private void createMessageTable() {
        log.info("创建消息表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `message` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                `client_message_id` VARCHAR(100) NULL COMMENT '客户端消息ID（用于幂等性）',
                `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
                `receiver_id` BIGINT COMMENT '接收者ID（私聊时使用）',
                `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
                `chat_id` BIGINT COMMENT '聊天列表ID',
                `content` JSON NOT NULL COMMENT '消息内容（JSON格式）',
                `message_type` INT DEFAULT 1 COMMENT '消息类型：1文本，2图片，3文件等',
                `read_status` INT DEFAULT 0 COMMENT '读取状态：0未读，1已读',
                `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读：0未读，1已读',
                `is_recalled` INT DEFAULT 0 COMMENT '是否撤回：0否，1是',
                `status` INT DEFAULT 0 COMMENT '消息状态：0正常，1已删除，2已撤回',
                `user_ip` VARCHAR(45) COMMENT '发送者IP地址',
                `source` VARCHAR(50) COMMENT '消息来源：WEB、MOBILE、API等',
                `is_show_time` INT DEFAULT 0 COMMENT '是否显示时间：0否，1是',
                `reply_to_message_id` BIGINT COMMENT '回复的消息ID',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_client_message_id` (`client_message_id`),
                KEY `idx_sender_id` (`sender_id`),
                KEY `idx_chat_id` (`chat_id`),
                KEY `idx_created_at` (`created_at`),
                KEY `idx_message_type` (`message_type`),
                KEY `idx_read_status` (`read_status`),
                KEY `idx_reply_to_message_id` (`reply_to_message_id`),
                KEY `idx_sender_client_msg` (`sender_id`, `client_message_id`),
                KEY `idx_message_private_chat` (`sender_id`, `receiver_id`, `created_at` DESC),
                KEY `idx_message_group_chat` (`group_id`, `created_at` DESC),
                KEY `idx_message_receiver_read` (`receiver_id`, `is_read`, `created_at` DESC),
                KEY `idx_message_type_time` (`message_type`, `created_at` DESC),
                KEY `idx_message_status` (`status`, `created_at` DESC),
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
                `group_description` TEXT NULL COMMENT '群组描述',
                `status` INT DEFAULT 1 COMMENT '群组状态: 0=已解散, 1=正常, 2=冻结',
                `max_members` INT DEFAULT 500 COMMENT '最大成员数',
                `member_count` INT DEFAULT 0 COMMENT '当前成员数',
                `is_visible` INT DEFAULT 1 COMMENT '是否可见: 0=私密, 1=公开',
                `category_id` BIGINT COMMENT '群组分类ID',
                `tags` VARCHAR(500) COMMENT '群组标签（逗号分隔）',
                `last_transfer_at` DATETIME NULL COMMENT '最后转让时间',
                `transfer_count` INT DEFAULT 0 COMMENT '转让次数',
                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_owner_id` (`owner_id`),
                KEY `idx_create_time` (`create_time`),
                KEY `idx_update_time` (`update_time`),
                KEY `idx_group_status` (`status`),
                KEY `idx_group_owner` (`owner_id`, `status`),
                KEY `idx_is_visible` (`is_visible`),
                KEY `idx_category_id` (`category_id`),
                CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组信息表（增强版：支持分类和标签）'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 群组表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建群组表失败", e);
            throw new RuntimeException("创建群组表失败", e);
        }
    }

    private void createGroupTransferHistoryTable() {
        log.info("创建群组转让历史表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group_transfer_history` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
                `group_id` BIGINT NOT NULL COMMENT '群组ID',
                `from_user_id` BIGINT NOT NULL COMMENT '原群主ID',
                `to_user_id` BIGINT NOT NULL COMMENT '新群主ID',
                `transfer_reason` VARCHAR(255) NULL COMMENT '转让原因',
                `transfer_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '转让时间',
                INDEX `idx_group_id` (`group_id`),
                INDEX `idx_from_user` (`from_user_id`),
                INDEX `idx_to_user` (`to_user_id`),
                INDEX `idx_transfer_time` (`transfer_at`),
                CONSTRAINT `fk_transfer_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_transfer_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_transfer_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组转让历史表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 群组转让历史表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建群组转让历史表失败", e);
            throw new RuntimeException("创建群组转让历史表失败", e);
        }
    }

    private void createGroupApplicationTable() {
        log.info("创建群组申请表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group_application` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
                `group_id` BIGINT NOT NULL COMMENT '群组ID',
                `user_id` BIGINT NOT NULL COMMENT '申请人ID',
                `message` VARCHAR(500) COMMENT '申请留言',
                `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING=待审批，APPROVED=已通过，REJECTED=已拒绝',
                `reviewer_id` BIGINT COMMENT '审核人ID',
                `review_note` VARCHAR(500) COMMENT '审核备注',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
                `reviewed_at` DATETIME COMMENT '审核时间',
                INDEX `idx_group_id` (`group_id`),
                INDEX `idx_user_id` (`user_id`),
                INDEX `idx_status` (`status`),
                INDEX `idx_created_at` (`created_at` DESC),
                INDEX `idx_group_status` (`group_id`, `status`),
                CONSTRAINT `fk_application_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_application_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组申请表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 群组申请表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建群组申请表失败", e);
            throw new RuntimeException("创建群组申请表失败", e);
        }
    }

    private void createGroupMemberTable() {
        log.info("创建群组成员表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group_member` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
                `group_id` BIGINT NOT NULL COMMENT '群组ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `role` INT NOT NULL DEFAULT 3 COMMENT '成员角色：1=群主，2=管理员，3=普通成员（角色值越小权限越高）',
                `join_status` VARCHAR(20) DEFAULT 'ACCEPTED' COMMENT '加入状态：PENDING=待审批，ACCEPTED=已接受，REJECTED=已拒绝，BLOCKED=已屏蔽',
                `invited_by` BIGINT COMMENT '邀请人ID',
                `invite_reason` VARCHAR(500) COMMENT '邀请/申请原因',
                `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                `kicked_at` DATETIME COMMENT '被移除时间',
                `kick_reason` VARCHAR(500) COMMENT '被移除原因',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_role` (`role`),
                KEY `idx_join_status` (`join_status`),
                KEY `idx_invited_by` (`invited_by`),
                KEY `idx_join_time` (`join_time`),
                KEY `idx_group_role_status` (`group_id`, `role`, `join_status`),
                CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_group_member_inviter` FOREIGN KEY (`invited_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='群组成员关系表（增强版：支持申请审批流程）'
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
                `status` TINYINT(1) DEFAULT 1 COMMENT '文章状态: 0=待审核, 1=审核通过, 2=审核拒绝, 3=已删除',
                `reviewer_id` BIGINT NULL COMMENT '审核人ID',
                `reviewed_at` TIMESTAMP NULL COMMENT '审核时间',
                `review_note` TEXT NULL COMMENT '审核备注（拒绝原因等）',
                `has_sensitive_words` TINYINT(1) DEFAULT 0 COMMENT '是否包含敏感词',
                `review_priority` INT DEFAULT 0 COMMENT '审核优先级: 0=普通, 1=高, 2=紧急',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`article_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_category_id` (`category_id`),
                KEY `idx_created_at` (`created_at`),
                KEY `idx_status` (`status`),
                KEY `idx_likes_count` (`likes_count`),
                KEY `idx_exposure_count` (`exposure_count`),
                KEY `idx_article_status_created` (`status`, `created_at` DESC),
                KEY `idx_article_reviewer` (`reviewer_id`, `reviewed_at`),
                KEY `idx_article_priority` (`review_priority` DESC, `created_at` DESC),
                FULLTEXT KEY `ft_title_content` (`article_title`, `article_content`),
                CONSTRAINT `fk_articles_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_articles_category` FOREIGN KEY (`category_id`) REFERENCES `article_category` (`id`) ON DELETE SET NULL,
                CONSTRAINT `fk_articles_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
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

    private void createArticleModerationHistoryTable() {
        log.info("创建文章审核历史表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_moderation_history` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                `reviewer_id` BIGINT NOT NULL COMMENT '审核人ID',
                `action` VARCHAR(20) NOT NULL COMMENT '审核动作: APPROVE, REJECT, DELETE',
                `reason` TEXT NULL COMMENT '审核原因',
                `previous_status` TINYINT(1) NULL COMMENT '之前的状态',
                `new_status` TINYINT(1) NULL COMMENT '新状态',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
                INDEX `idx_article_id` (`article_id`),
                INDEX `idx_reviewer_id` (`reviewer_id`),
                INDEX `idx_created_at` (`created_at` DESC),
                CONSTRAINT `fk_moderation_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE,
                CONSTRAINT `fk_moderation_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章审核历史表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章审核历史表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章审核历史表失败", e);
            throw new RuntimeException("创建文章审核历史表失败", e);
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
                `status` INT NOT NULL DEFAULT 0 COMMENT '关系状态：0待确认，1已确认，2已拒绝，3已删除，4已过期',
                `remarks` VARCHAR(255) COMMENT '备注/申请附言',
                `expire_at` TIMESTAMP NULL COMMENT '好友请求过期时间，PENDING状态下有效',
                `group_id` BIGINT NULL COMMENT '所属分组ID',
                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
                KEY `idx_friend_id` (`friend_id`),
                KEY `idx_status` (`status`),
                KEY `idx_create_time` (`create_time`),
                KEY `idx_contact_status_expire` (`status`, `expire_at`),
                KEY `idx_contact_group_id` (`group_id`),
                CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_contact_friend` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `chk_not_self_contact` CHECK (`user_id` != `friend_id`)
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

    private void createContactGroupTable() {
        log.info("创建联系人分组表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `contact_group` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分组ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `group_name` VARCHAR(50) NOT NULL COMMENT '分组名称',
                `group_order` INT DEFAULT 0 COMMENT '分组排序',
                `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为默认分组',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_user_id` (`user_id`),
                INDEX `idx_user_order` (`user_id`, `group_order`),
                CONSTRAINT `fk_contact_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='联系人分组表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 联系人分组表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建联系人分组表失败", e);
            throw new RuntimeException("创建联系人分组表失败", e);
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

            // 权限系统已完全移除，不再初始化权限相关数据
            log.info("✅ 权限系统已禁用，跳过权限初始化");

            // 初始化测试好友关系
            initializeTestContacts();

            // 初始化测试群组
            initializeTestGroups();

        } catch (Exception e) {
            log.error("插入初始数据失败", e);
            throw new RuntimeException("插入初始数据失败", e);
        }
    }

    /**
     * 初始化测试好友关系
     */
    private void initializeTestContacts() {
        log.info("开始初始化测试好友关系...");

        try {
            // 检查是否已有好友关系
            Integer contactCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM contact", Integer.class);

            if (contactCount != null && contactCount > 0) {
                log.info("好友关系已存在，跳过初始化");
                return;
            }

            // 获取测试用户ID
            Long adminId = getUserIdByUsername("admin");
            Long testUserId = getUserIdByUsername("testuser");
            Long aliceId = getUserIdByUsername("alice");
            Long bobId = getUserIdByUsername("bob");
            Long charlieId = getUserIdByUsername("charlie");
            Long dianaId = getUserIdByUsername("diana");
            Long eveId = getUserIdByUsername("eve");

            if (aliceId == null || bobId == null || charlieId == null) {
                log.warn("测试用户不存在，跳过好友关系初始化");
                return;
            }

            // 创建双向好友关系（status: 1=已接受）
            // alice 和 bob 是好友 (双向关系)
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '我们是同事', NOW(), NOW())",
                aliceId, bobId);
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '技术交流伙伴', NOW(), NOW())",
                bobId, aliceId);

            // alice 和 charlie 是好友 (双向关系)
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '大学同学', NOW(), NOW())",
                aliceId, charlieId);
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '老朋友', NOW(), NOW())",
                charlieId, aliceId);

            // bob 和 charlie 是好友 (双向关系)
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '项目合作伙伴', NOW(), NOW())",
                bobId, charlieId);
            jdbcTemplate.update(
                "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '好朋友', NOW(), NOW())",
                charlieId, bobId);

            // admin 和其他用户的好友关系
            if (adminId != null && testUserId != null) {
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '系统管理员', NOW(), NOW())",
                    adminId, testUserId);
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 1, '测试用户', NOW(), NOW())",
                    testUserId, adminId);
            }

            // 创建待处理的好友申请（status: 0=待处理）
            if (dianaId != null) {
                // diana 向 alice 发送待处理的好友申请
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 0, '你好，我想加你为好友，我们有共同的兴趣爱好', NOW(), NOW())",
                    dianaId, aliceId);
                
                // diana 向 charlie 发送待处理的好友申请
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 0, '认识一下，可以一起讨论技术问题', NOW(), NOW())",
                    dianaId, charlieId);
            }

            if (eveId != null) {
                // eve 向 bob 发送待处理的好友申请
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 0, '认识一下，希望能成为朋友', NOW(), NOW())",
                    eveId, bobId);
                
                // eve 向 alice 发送待处理的好友申请
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 0, '看到你的文章很棒，想和你交流', NOW(), NOW())",
                    eveId, aliceId);
            }

            // 创建一些被拒绝的好友申请（status: 2=已拒绝）用于测试
            if (dianaId != null && bobId != null) {
                jdbcTemplate.update(
                    "INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time) VALUES (?, ?, 2, '抱歉，我们不太熟悉', NOW(), NOW())",
                    dianaId, bobId);
            }

            log.info("✅ 测试好友关系初始化成功 - 创建了多种状态的好友关系用于开发测试");

        } catch (Exception e) {
            log.error("❌ 初始化测试好友关系失败", e);
            // 不抛出异常，允许继续初始化其他数据
        }
    }

    /**
     * 初始化测试群组
     */
    private void initializeTestGroups() {
        log.info("开始初始化测试群组...");

        try {
            // 检查是否已有群组
            Integer groupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `group`", Integer.class);

            if (groupCount != null && groupCount > 0) {
                log.info("群组已存在，跳过初始化");
                return;
            }

            // 获取测试用户ID
            Long adminId = getUserIdByUsername("admin");
            Long testUserId = getUserIdByUsername("testuser");
            Long aliceId = getUserIdByUsername("alice");
            Long bobId = getUserIdByUsername("bob");
            Long charlieId = getUserIdByUsername("charlie");
            Long dianaId = getUserIdByUsername("diana");
            Long eveId = getUserIdByUsername("eve");

            if (aliceId == null || bobId == null) {
                log.warn("测试用户不存在，跳过群组初始化");
                return;
            }

            // 创建测试群组1：技术交流群（alice 是群主）
            jdbcTemplate.update(
                "INSERT INTO `group` (group_name, owner_id, group_description, status, max_members, member_count, is_visible, create_time, update_time) " +
                "VALUES ('技术交流群', ?, '讨论前端、后端、数据库等技术问题的专业群组', 1, 500, 4, 1, NOW(), NOW())",
                aliceId);
            Long group1Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

            // 添加群成员 - 技术交流群（使用统一的角色定义：1=群主，2=管理员，3=普通成员）
            jdbcTemplate.update(
                "INSERT INTO group_member (group_id, user_id, role, join_status, join_time, update_time) VALUES (?, ?, 1, 'ACCEPTED', NOW(), NOW())", // 1=群主
                group1Id, aliceId);
            jdbcTemplate.update(
                "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time, update_time) VALUES (?, ?, 2, 'ACCEPTED', ?, NOW(), NOW())", // 2=管理员
                group1Id, bobId, aliceId);
            jdbcTemplate.update(
                "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time, update_time) VALUES (?, ?, 3, 'ACCEPTED', ?, NOW(), NOW())", // 3=普通成员
                group1Id, charlieId, aliceId);
            if (testUserId != null) {
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time, update_time) VALUES (?, ?, 3, 'ACCEPTED', ?, NOW(), NOW())",
                    group1Id, testUserId, aliceId);
            }

            // 创建测试群组2：生活分享群（bob 是群主）
            jdbcTemplate.update(
                "INSERT INTO `group` (group_name, owner_id, group_description, status, max_members, member_count, is_visible, create_time, update_time) " +
                "VALUES ('生活分享群', ?, '分享生活点滴、美食、旅游等日常生活内容', 1, 200, 3, 1, NOW(), NOW())",
                bobId);
            Long group2Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

            // 添加群成员 - 生活分享群
            jdbcTemplate.update(
                "INSERT INTO group_member (group_id, user_id, role, join_status, join_time, update_time) VALUES (?, ?, 1, 'ACCEPTED', NOW(), NOW())", // 1=群主
                group2Id, bobId);
            if (dianaId != null) {
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time, update_time) VALUES (?, ?, 2, 'ACCEPTED', ?, NOW(), NOW())", // 2=管理员
                    group2Id, dianaId, bobId);
            }
            if (eveId != null) {
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time, update_time) VALUES (?, ?, 3, 'ACCEPTED', ?, NOW(), NOW())", // 3=普通成员
                    group2Id, eveId, bobId);
            }

            // 创建测试群组3：项目协作群（charlie 是群主）
            if (charlieId != null) {
                jdbcTemplate.update(
                    "INSERT INTO `group` (group_name, owner_id, group_description, status, max_members, member_count, create_time) " +
                    "VALUES ('项目协作群', ?, '团队项目协作、任务分配、进度跟踪专用群', 1, 50, 3, NOW())",
                    charlieId);
                Long group3Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

                // 添加群成员 - 项目协作群
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, join_time) VALUES (?, ?, 1, 'ACCEPTED', NOW())", // 1=群主
                    group3Id, charlieId);
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time) VALUES (?, ?, 2, 'ACCEPTED', ?, NOW())", // 2=管理员
                    group3Id, aliceId, charlieId);
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time) VALUES (?, ?, 3, 'ACCEPTED', ?, NOW())", // 3=普通成员
                    group3Id, bobId, charlieId);
            }

            // 创建测试群组4：管理员群（admin 是群主）
            if (adminId != null) {
                jdbcTemplate.update(
                    "INSERT INTO `group` (group_name, owner_id, group_description, status, max_members, member_count, create_time) " +
                    "VALUES ('系统管理员群', ?, '系统管理员专用群，讨论系统维护、用户管理等事务', 1, 10, 2, NOW())",
                    adminId);
                Long group4Id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

                // 添加群成员 - 管理员群
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, join_time) VALUES (?, ?, 1, 'ACCEPTED', NOW())", // 1=群主
                    group4Id, adminId);
                if (testUserId != null) {
                    jdbcTemplate.update(
                        "INSERT INTO group_member (group_id, user_id, role, join_status, invited_by, join_time) VALUES (?, ?, 3, 'ACCEPTED', ?, NOW())", // 3=普通成员
                        group4Id, testUserId, adminId);
                }
            }

            // 创建一个冻结状态的群组用于测试（status: 2=冻结）
            if (dianaId != null) {
                jdbcTemplate.update(
                    "INSERT INTO `group` (group_name, owner_id, group_description, status, max_members, member_count, create_time) " +
                    "VALUES ('已冻结测试群', ?, '这是一个用于测试冻结状态的群组', 2, 100, 1, NOW())",
                    dianaId);
                Long frozenGroupId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

                // 添加群主
                jdbcTemplate.update(
                    "INSERT INTO group_member (group_id, user_id, role, join_status, join_time) VALUES (?, ?, 1, 'ACCEPTED', NOW())",
                    frozenGroupId, dianaId);
            }

            log.info("✅ 测试群组初始化成功 - 创建了多个不同类型和状态的群组，包含不同角色的成员用于开发测试");

        } catch (Exception e) {
            log.error("❌ 初始化测试群组失败", e);
            // 不抛出异常，允许继续初始化其他数据
        }
    }

    /**
     * 根据用户名获取用户ID
     */
    private Long getUserIdByUsername(String username) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM user WHERE username = ?", Long.class, username);
        } catch (Exception e) {
            log.warn("用户 {} 不存在", username);
            return null;
        }
    }

    // ========================================
    // 🔒 权限系统已完全移除
    // 所有权限相关的初始化方法已被删除
    // ========================================

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

    private void createArticleLikeTable() {
        log.info("创建文章点赞表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_like` (
                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_user_article_like` (`user_id`, `article_id`),
                KEY `idx_user_id` (`user_id`),
                KEY `idx_article_id` (`article_id`),
                KEY `idx_created_at` (`created_at`),
                CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_like_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
            COMMENT='文章点赞记录表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 文章点赞表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章点赞表失败", e);
            throw new RuntimeException("创建文章点赞表失败", e);
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

    /**
     * 创建用户等级历史表
     */
    private void createUserLevelHistoryTable() {
        log.info("创建用户等级历史表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `user_level_history` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `old_level` INT COMMENT '原等级',
                `new_level` INT NOT NULL COMMENT '新等级',
                `change_reason` VARCHAR(500) COMMENT '变更原因',
                `change_type` INT NOT NULL COMMENT '变更类型 1:系统自动 2:管理员操作 3:用户行为触发',
                `operator_id` BIGINT COMMENT '操作者ID',
                `operator_name` VARCHAR(100) COMMENT '操作者名称',
                `change_time` DATETIME NOT NULL COMMENT '变更时间',
                `ip_address` VARCHAR(50) COMMENT 'IP地址',
                `user_agent` VARCHAR(500) COMMENT '用户代理',
                `remark` VARCHAR(500) COMMENT '备注',
                `status` INT DEFAULT 1 COMMENT '状态 0:无效 1:有效',
                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX idx_user_id (user_id),
                INDEX idx_change_type (change_type),
                INDEX idx_operator_id (operator_id),
                INDEX idx_change_time (change_time),
                INDEX idx_status (status),
                INDEX idx_created_at (created_at),
                INDEX idx_user_time (user_id, change_time),
                INDEX idx_level_change (old_level, new_level),
                CONSTRAINT `fk_user_level_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户等级变更历史表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 用户等级历史表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建用户等级历史表失败", e);
            throw new RuntimeException("创建用户等级历史表失败", e);
        }
    }

    /**
     * 创建文章版本表
     */
    private void createArticleVersionTable() {
        log.info("创建文章版本表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article_version` (
                `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                `article_id` BIGINT NOT NULL COMMENT '文章ID（关联到articles表）',
                `version_number` INT NOT NULL COMMENT '版本号',
                `title` VARCHAR(500) NOT NULL COMMENT '文章标题',
                `content` LONGTEXT COMMENT '文章内容',
                `summary` TEXT COMMENT '文章摘要',
                `article_link` VARCHAR(1000) COMMENT '文章链接',
                `category_id` BIGINT COMMENT '分类ID',
                `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '文章状态（draft, published, archived等）',
                `tags` JSON COMMENT '标签（JSON格式）',
                `cover_image` VARCHAR(1000) COMMENT '封面图片URL',
                `version_note` TEXT COMMENT '版本说明',
                `created_by` BIGINT NOT NULL COMMENT '创建者ID',
                `created_by_username` VARCHAR(100) COMMENT '创建者用户名',
                `change_type` VARCHAR(50) NOT NULL DEFAULT 'create' COMMENT '变更类型（create, update, minor_edit, major_edit等）',
                `change_summary` TEXT COMMENT '变更摘要（主要修改点）',
                `character_change` INT COMMENT '字符数变化',
                `is_major_version` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否为主要版本',
                `is_auto_save` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否自动保存版本',
                `size` INT COMMENT '版本大小（字符数）',
                `content_hash` VARCHAR(64) COMMENT '版本哈希值（用于内容去重）',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_article_id` (`article_id`),
                INDEX `idx_version_number` (`version_number`),
                INDEX `idx_created_by` (`created_by`),
                INDEX `idx_status` (`status`),
                INDEX `idx_change_type` (`change_type`),
                INDEX `idx_created_at` (`created_at`),
                INDEX `idx_is_major_version` (`is_major_version`),
                INDEX `idx_is_auto_save` (`is_auto_save`),
                INDEX `idx_content_hash` (`content_hash`),
                UNIQUE KEY `uk_article_version` (`article_id`, `version_number`),
                CONSTRAINT `fk_version_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE,
                CONSTRAINT `fk_version_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章版本表'
            """;

        try {
            jdbcTemplate.execute(sql);

            // 添加检查约束
            try {
                jdbcTemplate.execute("ALTER TABLE `article_version` ADD CONSTRAINT `chk_status` CHECK (status IN ('draft', 'published', 'archived', 'deleted'))");
            } catch (Exception e) {
                log.warn("添加状态检查约束失败，可能已存在: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `article_version` ADD CONSTRAINT `chk_change_type` CHECK (change_type IN ('create', 'update', 'minor_edit', 'major_edit', 'title_change', 'content_change', 'auto_save'))");
            } catch (Exception e) {
                log.warn("添加变更类型检查约束失败，可能已存在: {}", e.getMessage());
            }

            log.info("✅ 文章版本表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建文章版本表失败", e);
            throw new RuntimeException("创建文章版本表失败", e);
        }
    }

    /**
     * 创建内容举报表
     */
    private void createContentReportTable() {
        log.info("创建内容举报表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `content_report` (
                `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
                `content_type` VARCHAR(50) NOT NULL COMMENT '被举报内容类型（article, comment, message, user等）',
                `content_id` BIGINT NOT NULL COMMENT '被举报内容ID',
                `reason` VARCHAR(50) NOT NULL COMMENT '举报原因（spam, harassment, inappropriate_content, violence, copyright等）',
                `description` TEXT COMMENT '举报描述',
                `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '举报状态（pending, reviewing, resolved, dismissed）',
                `reviewer_id` BIGINT COMMENT '处理人ID（管理员）',
                `action` VARCHAR(50) COMMENT '处理结果（remove_content, warn_user, ban_user, no_action）',
                `review_note` TEXT COMMENT '处理说明',
                `reviewed_at` TIMESTAMP NULL COMMENT '处理时间',
                `metadata` JSON COMMENT '附加信息（截图、链接等证据）',
                `report_count` INT NOT NULL DEFAULT 1 COMMENT '举报计数（同一内容被多少人举报）',
                `is_urgent` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为紧急举报',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_reporter_id` (`reporter_id`),
                INDEX `idx_content` (`content_type`, `content_id`),
                INDEX `idx_status` (`status`),
                INDEX `idx_reason` (`reason`),
                INDEX `idx_created_at` (`created_at`),
                INDEX `idx_is_urgent` (`is_urgent`),
                INDEX `idx_reviewer_id` (`reviewer_id`),
                CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
                CONSTRAINT `fk_report_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容举报表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("✅ 内容举报表创建成功");
        } catch (Exception e) {
            log.error("❌ 创建内容举报表失败", e);
            throw new RuntimeException("创建内容举报表失败", e);
        }
    }

    // ========================================
    // 🔒 权限系统已完全移除
    // 所有权限相关的初始化方法已被删除
    // ========================================
}