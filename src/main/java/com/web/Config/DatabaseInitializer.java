package com.web.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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

    private String databaseName = "weeb"; // 默认数据库名

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查数据库...");

        // 检查当前环境，只有开发环境才执行自动创建
        boolean isProduction = environment.matchesProfiles("prod") ||
                              environment.matchesProfiles("production");

        if (isProduction) {
            log.info("生产环境，跳过数据库表自动创建");
            return;
        }

        try {
            // 检查数据库连接
            checkDatabaseConnection();

            // 检查并创建表
            checkAndCreateTables();

            log.info("数据库检查完成");

        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw e;
        }
    }

    private void checkDatabaseConnection() {
        try {
            jdbcTemplate.execute("SELECT 1");
            log.info("数据库连接成功");
        } catch (Exception e) {
            log.error("数据库连接失败", e);
            throw e;
        }
    }

    private void checkAndCreateTables() {
        log.info("检查数据库表...");

        // 检查并创建用户表
        if (!tableExists("user")) {
            createUserTable();
        }

        // 检查并创建消息表
        if (!tableExists("message")) {
            createMessageTable();
        }

        // 检查并创建群组表
        if (!tableExists("`group`")) {
            createGroupTable();
        }

        // 检查并创建群组成员表
        if (!tableExists("group_member")) {
            createGroupMemberTable();
        }

        // 检查并创建会话列表表
        if (!tableExists("chat_list")) {
            createChatListTable();
        }

        // 检查并创建文章表
        if (!tableExists("article")) {
            createArticleTable();
        }

        // 检查并创建消息反应表
        if (!tableExists("message_reaction")) {
            createMessageReactionTable();
        }

        // 检查并创建通知表
        if (!tableExists("notifications")) {
            createNotificationTable();
        }

        // 检查并创建联系人表
        if (!tableExists("contact")) {
            createContactTable();
        }

        // 检查并创建文件传输表
        if (!tableExists("file_transfer")) {
            createFileTransferTable();
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
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
                `sex` INT NOT NULL DEFAULT 0 COMMENT '性别：0为女，1为男',
                `phone_number` VARCHAR(20) COMMENT '电话',
                `user_email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
                `fans_count` BIGINT DEFAULT 0 COMMENT '粉丝数量',
                `total_likes` BIGINT DEFAULT 0 COMMENT '总点赞数',
                `total_favorites` BIGINT DEFAULT 0 COMMENT '总收藏数',
                `website_coins` BIGINT DEFAULT 0 COMMENT '网站金币',
                `registration_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册日期',
                `ip_ownership` VARCHAR(100) COMMENT '用户IP归属地',
                `type` VARCHAR(50) COMMENT '用户类型',
                `avatar` VARCHAR(500) COMMENT '用户头像',
                `nickname` VARCHAR(100) COMMENT '用户昵称',
                `badge` VARCHAR(100) COMMENT '用户徽章信息',
                `login_time` TIMESTAMP COMMENT '最后一次登录时间',
                `bio` TEXT COMMENT '个人简介',
                `online_status` INT DEFAULT 0 COMMENT '用户在线状态'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("用户表创建成功");
        } catch (Exception e) {
            log.error("创建用户表失败", e);
        }
    }

    private void createMessageTable() {
        log.info("创建消息表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `message` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
                `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
                `chat_id` BIGINT NOT NULL COMMENT '会话列表ID',
                `content` JSON COMMENT '消息内容（JSON格式）',
                `message_type` INT NOT NULL COMMENT '消息类型',
                `read_status` INT DEFAULT 0 COMMENT '已读状态',
                `is_recalled` INT DEFAULT 0 COMMENT '是否撤回',
                `user_ip` VARCHAR(50) COMMENT '用户IP',
                `source` VARCHAR(50) COMMENT '消息来源',
                `is_show_time` INT DEFAULT 0 COMMENT '是否显示时间',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_sender_id` (`sender_id`),
                INDEX `idx_chat_id` (`chat_id`),
                INDEX `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("消息表创建成功");
        } catch (Exception e) {
            log.error("创建消息表失败", e);
        }
    }

    private void createGroupTable() {
        log.info("创建群组表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
                `group_name` VARCHAR(100) NOT NULL COMMENT '群名称',
                `owner_id` BIGINT NOT NULL COMMENT '群主的用户ID',
                `group_avatar_url` VARCHAR(500) COMMENT '群头像URL',
                `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                INDEX `idx_owner_id` (`owner_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("群组表创建成功");
        } catch (Exception e) {
            log.error("创建群组表失败", e);
        }
    }

    private void createGroupMemberTable() {
        log.info("创建群组成员表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `group_member` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员关系ID',
                `group_id` BIGINT NOT NULL COMMENT '群组ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `role` INT NOT NULL COMMENT '成员角色 (1: 群主, 2: 管理员, 3: 普通成员)',
                `join_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
                `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
                INDEX `idx_group_id` (`group_id`),
                INDEX `idx_user_id` (`user_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("群组成员表创建成功");
        } catch (Exception e) {
            log.error("创建群组成员表失败", e);
        }
    }

    private void createChatListTable() {
        log.info("创建会话列表表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `chat_list` (
                `id` VARCHAR(255) PRIMARY KEY COMMENT '主键ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `target_id` BIGINT COMMENT '目标用户ID',
                `target_info` TEXT NOT NULL COMMENT '会话标题或目标信息',
                `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
                `last_message` TEXT COMMENT '最后一条消息',
                `type` VARCHAR(255) COMMENT '会话类型',
                `create_time` VARCHAR(255) NOT NULL COMMENT '创建时间',
                `update_time` VARCHAR(255) NOT NULL COMMENT '最后更新时间',
                `group_id` BIGINT COMMENT '群组ID',
                INDEX `idx_user_id` (`user_id`),
                INDEX `idx_group_id` (`group_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话列表表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("会话列表表创建成功");
        } catch (Exception e) {
            log.error("创建会话列表表失败", e);
        }
    }

    private void createArticleTable() {
        log.info("创建文章表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `article` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `article_title` VARCHAR(200) NOT NULL COMMENT '文章标题',
                `article_content` TEXT NOT NULL COMMENT '文章内容',
                `likes_count` INT DEFAULT 0 COMMENT '点赞数',
                `favorites_count` INT DEFAULT 0 COMMENT '收藏数',
                `sponsors_count` DECIMAL(10,2) DEFAULT 0.00 COMMENT '赞助数',
                `exposure_count` BIGINT DEFAULT 0 COMMENT '曝光数',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_user_id` (`user_id`),
                INDEX `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("文章表创建成功");
        } catch (Exception e) {
            log.error("创建文章表失败", e);
        }
    }

    private void createMessageReactionTable() {
        log.info("创建消息反应表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `message_reaction` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反应ID',
                `message_id` BIGINT NOT NULL COMMENT '消息ID',
                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                `reaction_type` VARCHAR(50) NOT NULL COMMENT '反应类型 (如: 👍, ❤️, 😂)',
                `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                UNIQUE KEY `uk_message_user_reaction` (`message_id`, `user_id`, `reaction_type`),
                INDEX `idx_message_id` (`message_id`),
                INDEX `idx_user_id` (`user_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息反应表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("消息反应表创建成功");
        } catch (Exception e) {
            log.error("创建消息反应表失败", e);
        }
    }

    private void createNotificationTable() {
        log.info("创建通知表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知的唯一ID',
                `recipient_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
                `actor_id` BIGINT NOT NULL COMMENT '触发通知的用户的ID',
                `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
                `entity_type` VARCHAR(50) NOT NULL COMMENT '关联实体的类型',
                `entity_id` BIGINT NOT NULL COMMENT '关联实体的ID',
                `is_read` TINYINT(1) DEFAULT 0 COMMENT '通知是否已读 (0: 未读, 1: 已读)',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
                INDEX `idx_recipient_id` (`recipient_id`),
                INDEX `idx_actor_id` (`actor_id`),
                INDEX `idx_entity` (`entity_type`, `entity_id`),
                INDEX `idx_created_at` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("通知表创建成功");
        } catch (Exception e) {
            log.error("创建通知表失败", e);
        }
    }

    private void createContactTable() {
        log.info("创建联系人表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `contact` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
                `user_id` BIGINT NOT NULL COMMENT '用户自身のID',
                `friend_id` BIGINT NOT NULL COMMENT '好友的ID',
                `status` INT NOT NULL COMMENT '关系状态',
                `remarks` VARCHAR(255) COMMENT '备注/附言',
                `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
                INDEX `idx_user_id` (`user_id`),
                INDEX `idx_friend_id` (`friend_id`),
                INDEX `idx_status` (`status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("联系人表创建成功");
        } catch (Exception e) {
            log.error("创建联系人表失败", e);
        }
    }

    private void createFileTransferTable() {
        log.info("创建文件传输表...");

        String sql = """
            CREATE TABLE IF NOT EXISTS `file_transfer` (
                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件传输ID',
                `initiator_id` BIGINT NOT NULL COMMENT '发起者ID',
                `target_id` BIGINT NOT NULL COMMENT '目标用户ID',
                `offer_sdp` TEXT COMMENT 'WebRTC Offer SDP',
                `answer_sdp` TEXT COMMENT 'WebRTC Answer SDP',
                `candidate` TEXT COMMENT 'WebRTC ICE候选',
                `status` INT DEFAULT 0 COMMENT '状态：0=INVITE,1=OFFERED,2=ANSWERED等',
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                INDEX `idx_initiator_id` (`initiator_id`),
                INDEX `idx_target_id` (`target_id`),
                INDEX `idx_status` (`status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件传输表'
            """;

        try {
            jdbcTemplate.execute(sql);
            log.info("文件传输表创建成功");
        } catch (Exception e) {
            log.error("创建文件传输表失败", e);
        }
    }
}