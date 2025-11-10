-- 用户聊天列表表（使用shared_chat_id实现消息共享）
-- Creation Date: 2025-11-10
-- Description: 用户的聊天列表，管理每个用户的聊天会话，通过shared_chat_id实现消息共享

CREATE TABLE IF NOT EXISTS `chat_list` (
    `id` VARCHAR(255) NOT NULL COMMENT '聊天列表ID（用户维度）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `shared_chat_id` BIGINT NOT NULL COMMENT '共享聊天ID（私聊双方共用，群聊所有成员共用）',
    `target_id` BIGINT COMMENT '目标ID（用户ID或群组ID）',
    `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
    `target_info` TEXT NOT NULL COMMENT '目标信息（用户名或群组名）',
    `type` VARCHAR(255) NOT NULL COMMENT '聊天类型：PRIVATE、GROUP',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `last_message` TEXT COMMENT '最后一条消息内容',
    `create_time` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_time` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_shared_chat_id` (`shared_chat_id`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_type` (`type`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_user_shared` (`user_id`, `shared_chat_id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `type`),
    CONSTRAINT `fk_chat_list_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='用户聊天列表表（使用shared_chat_id实现消息共享）';