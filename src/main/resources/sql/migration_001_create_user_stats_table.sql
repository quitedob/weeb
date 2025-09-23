-- Migration Script: Create user_stats table and migrate statistical data
-- This script separates statistical counters from the user table to prevent write contention
-- Execute this script during maintenance window to ensure data consistency

-- Step 1: Create user_stats table with proper foreign key constraints
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

-- Step 2: Migrate existing statistical data from user table to user_stats table
INSERT INTO `user_stats` (
    `user_id`,
    `fans_count`,
    `total_likes`,
    `total_favorites`,
    `total_sponsorship`,
    `total_article_exposure`,
    `website_coins`
)
SELECT 
    `id`,
    COALESCE(`fans_count`, 0),
    COALESCE(`total_likes`, 0),
    COALESCE(`total_favorites`, 0),
    COALESCE(`total_sponsorship`, 0),
    COALESCE(`total_article_exposure`, 0),
    COALESCE(`website_coins`, 0)
FROM `user`
WHERE `id` NOT IN (SELECT `user_id` FROM `user_stats`);

-- Step 3: Verify data integrity
-- This query should return 0 if migration was successful
SELECT COUNT(*) as missing_stats_records 
FROM `user` u 
LEFT JOIN `user_stats` us ON u.id = us.user_id 
WHERE us.user_id IS NULL;

-- Step 4: Create indexes for optimal JOIN performance
-- These indexes are already created in the table definition above
-- but listed here for reference:
-- - PRIMARY KEY on user_id for fast lookups
-- - INDEX on fans_count for ranking queries
-- - INDEX on total_likes for popularity queries
-- - INDEX on website_coins for financial queries
-- - INDEX on updated_at for temporal queries

-- Step 5: Validation queries to ensure data consistency
-- Compare totals before and after migration (run these manually to verify)
/*
-- Original data totals from user table
SELECT 
    COUNT(*) as user_count,
    SUM(fans_count) as total_fans,
    SUM(total_likes) as total_likes_sum,
    SUM(website_coins) as total_coins
FROM user;

-- Migrated data totals from user_stats table
SELECT 
    COUNT(*) as stats_count,
    SUM(fans_count) as total_fans,
    SUM(total_likes) as total_likes_sum,
    SUM(website_coins) as total_coins
FROM user_stats;
*/