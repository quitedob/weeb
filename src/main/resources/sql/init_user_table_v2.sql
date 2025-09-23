-- 用户表初始化SQL (Version 2 - 统计数据分离后)
-- 此版本将统计数据移至独立的user_stats表以避免写锁竞争

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `sex` TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0为女，1为男',
    `phone_number` VARCHAR(20) COMMENT '电话',
    `user_email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `unique_article_link` VARCHAR(255) COMMENT '唯一标识文章链接',
    `unique_video_link` VARCHAR(255) COMMENT '唯一标识视频链接',
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
    KEY `idx_login_time` (`login_time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表（统计数据已迁移至user_stats表）';

-- 用户统计数据表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计数据表';