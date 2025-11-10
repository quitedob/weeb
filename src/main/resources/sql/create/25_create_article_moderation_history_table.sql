-- 文章审核历史表
-- 用于记录文章审核的历史，支持审核流程追踪

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
COMMENT='文章审核历史表';