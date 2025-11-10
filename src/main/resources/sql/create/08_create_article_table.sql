-- 文章内容表
-- Creation Date: 2025-11-10
-- Description: 存储文章内容，支持审核流程、点赞收藏统计和全文搜索

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
COMMENT='文章内容表';