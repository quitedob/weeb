-- 聊天未读计数表
-- 创建时间: 2025-11-10
-- 说明: 优化未读消息性能，独立管理未读计数

CREATE TABLE IF NOT EXISTS `chat_unread_count` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `chat_id` BIGINT NOT NULL COMMENT '聊天ID（私聊或群聊）',
    `unread_count` INT NOT NULL DEFAULT 0 COMMENT '未读消息数',
    `last_read_message_id` BIGINT COMMENT '最后已读消息ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_chat` (`user_id`, `chat_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_chat_id` (`chat_id`),
    KEY `idx_unread_count` (`unread_count`),
    KEY `idx_updated_at` (`updated_at`),
    CONSTRAINT `fk_unread_count_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='聊天未读计数表（优化未读消息性能）';