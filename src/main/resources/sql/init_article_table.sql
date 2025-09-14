-- 文章表初始化SQL
CREATE TABLE IF NOT EXISTS `article` (
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
