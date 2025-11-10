-- 文章点赞记录表
-- 用于存储用户对文章的点赞记录，防止重复点赞

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
COMMENT='文章点赞记录表';