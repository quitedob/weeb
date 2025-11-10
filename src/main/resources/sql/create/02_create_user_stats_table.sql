-- 用户统计数据表
-- 创建时间: 2025-11-10
-- 说明: 存储用户统计数据，与user表分离以提高并发性能

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
COMMENT='用户统计数据表（分离以提高并发性能）';