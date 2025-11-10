-- 用户基础信息表
-- 创建时间: 2025-11-10
-- 说明: 存储用户基础信息，统计数据分离至user_stats表以提高并发性能

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
COMMENT='用户基础信息表（统计数据分离至user_stats表）';