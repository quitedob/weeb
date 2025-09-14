-- WEEB 项目完整数据库初始化脚本
-- 执行方式：mysql -u root -p < init_database.sql

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `weeb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `weeb`;

-- 删除表（如果存在）- 按依赖关系逆序删除
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `chat_list`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `group_member`;
DROP TABLE IF EXISTS `group_info`;
DROP TABLE IF EXISTS `article`;
DROP TABLE IF EXISTS `user`;

-- 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `sex` TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0为女，1为男',
    `phone_number` VARCHAR(20) COMMENT '电话',
    `user_email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `fans_count` BIGINT DEFAULT 0 COMMENT '粉丝数量',
    `unique_article_link` VARCHAR(255) COMMENT '唯一标识文章链接',
    `unique_video_link` VARCHAR(255) COMMENT '唯一标识视频链接',
    `total_likes` BIGINT DEFAULT 0 COMMENT '总点赞数',
    `total_favorites` BIGINT DEFAULT 0 COMMENT '总收藏数',
    `total_sponsorship` BIGINT DEFAULT 0 COMMENT '总赞助数',
    `total_article_exposure` BIGINT DEFAULT 0 COMMENT '文章总曝光数',
    `website_coins` BIGINT DEFAULT 0 COMMENT '网站金币',
    `registration_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册日期',
    `ip_ownership` VARCHAR(100) COMMENT '用户IP归属地',
    `type` VARCHAR(50) COMMENT '用户类型',
    `avatar` VARCHAR(500) COMMENT '用户头像',
    `nickname` VARCHAR(100) COMMENT '用户昵称',
    `badge` VARCHAR(255) COMMENT '用户徽章信息',
    `login_time` DATETIME COMMENT '最后一次登录时间',
    `bio` TEXT COMMENT '个人简介',
    `online_status` TINYINT COMMENT '用户在线状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_user_email` (`user_email`),
    KEY `idx_registration_date` (`registration_date`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 文章表
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '作者ID',
    `article_title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `article_content` LONGTEXT COMMENT '文章内容',
    `article_link` VARCHAR(500) COMMENT '文章链接',
    `likes_count` BIGINT DEFAULT 0 COMMENT '点赞数',
    `exposure_count` BIGINT DEFAULT 0 COMMENT '阅读数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0草稿，1发布，2删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`),
    FULLTEXT KEY `ft_title_content` (`article_title`, `article_content`),
    CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 群组信息表
CREATE TABLE `group_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `description` TEXT COMMENT '群组简介',
    `avatar` VARCHAR(500) COMMENT '群组头像',
    `member_count` INT DEFAULT 1 COMMENT '成员数量',
    `max_members` INT DEFAULT 100 COMMENT '最大成员数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0解散，1正常',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组信息表';

-- 群组成员表
CREATE TABLE `group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色：OWNER、ADMIN、MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0离开，1正常',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role` (`role`),
    CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 消息表
CREATE TABLE `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT COMMENT '接收者ID（私聊时使用）',
    `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT、IMAGE、FILE等',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0撤回，1正常',
    PRIMARY KEY (`id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_message_type` (`message_type`),
    CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 聊天列表表
CREATE TABLE `chat_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '聊天列表ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_id` BIGINT COMMENT '目标ID（用户ID或群组ID）',
    `chat_type` VARCHAR(10) NOT NULL COMMENT '聊天类型：PRIVATE、GROUP',
    `last_message_time` DATETIME COMMENT '最后消息时间',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `chat_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_last_message_time` (`last_message_time`),
    CONSTRAINT `fk_chat_list_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天列表表';

-- 通知表
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收者ID',
    `sender_id` BIGINT COMMENT '发送者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_at` DATETIME COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_type` (`type`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 插入测试数据（可选）
-- 管理员用户
INSERT INTO `user` (`username`, `password`, `user_email`, `nickname`, `type`, `online_status`)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@weeb.com', '管理员', 'ADMIN', 1);

-- 普通用户
INSERT INTO `user` (`username`, `password`, `user_email`, `nickname`, `type`, `online_status`)
VALUES ('user1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user1@weeb.com', '测试用户1', 'USER', 1);

-- 示例文章
INSERT INTO `article` (`user_id`, `article_title`, `article_content`, `likes_count`, `exposure_count`, `status`)
VALUES (1, '欢迎使用WEEB', '这是WEEB项目的第一篇文章，欢迎各位用户使用我们的平台！', 10, 100, 1);

-- 示例群组
INSERT INTO `group_info` (`group_name`, `owner_id`, `description`, `member_count`, `max_members`)
VALUES ('WEEB官方群', 1, 'WEEB官方交流群，欢迎各位加入', 2, 100);

-- 群组成员
INSERT INTO `group_member` (`group_id`, `user_id`, `role`)
VALUES (1, 1, 'OWNER'), (1, 2, 'MEMBER');

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 输出成功信息
SELECT 'WEEB数据库初始化完成！' as '状态',
       (SELECT COUNT(*) FROM `user`) as '用户数',
       (SELECT COUNT(*) FROM `article`) as '文章数',
       (SELECT COUNT(*) FROM `group_info`) as '群组数',
       (SELECT COUNT(*) FROM `message`) as '消息数',
       (SELECT COUNT(*) FROM `notification`) as '通知数';
