-- 共享聊天表（管理私聊和群聊的共享ID）
-- Creation Date: 2025-11-10
-- Description: 用于管理私聊和群聊的共享ID，统一聊天会话管理

CREATE TABLE IF NOT EXISTS `shared_chat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '共享聊天ID',
    `chat_type` VARCHAR(50) NOT NULL COMMENT '聊天类型：PRIVATE、GROUP',
    `participant_1_id` BIGINT COMMENT '参与者1的用户ID（私聊时使用，较小的ID）',
    `participant_2_id` BIGINT COMMENT '参与者2的用户ID（私聊时使用，较大的ID）',
    `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_private_chat` (`participant_1_id`, `participant_2_id`, `chat_type`),
    UNIQUE KEY `uk_group_chat` (`group_id`, `chat_type`),
    KEY `idx_participant_1` (`participant_1_id`),
    KEY `idx_participant_2` (`participant_2_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_chat_type` (`chat_type`),
    CONSTRAINT `fk_shared_chat_participant_1` FOREIGN KEY (`participant_1_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_shared_chat_participant_2` FOREIGN KEY (`participant_2_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_shared_chat_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='共享聊天表（管理私聊和群聊的共享ID）';