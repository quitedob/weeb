-- 系统通知表
-- Creation Date: 2025-11-10
-- Description: 存储系统通知，支持点赞、评论、关注、消息等多种通知类型

CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `recipient_id` BIGINT NOT NULL COMMENT '接收者ID',
    `actor_id` BIGINT COMMENT '操作者ID',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型：LIKE、COMMENT、FOLLOW、MESSAGE等',
    `entity_type` VARCHAR(50) COMMENT '实体类型：ARTICLE、USER、GROUP等',
    `entity_id` BIGINT COMMENT '实体ID',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipient_id` (`recipient_id`),
    KEY `idx_actor_id` (`actor_id`),
    KEY `idx_type` (`type`),
    KEY `idx_entity_type_id` (`entity_type`, `entity_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notification_actor` FOREIGN KEY (`actor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='系统通知表';