-- 消息表初始化SQL
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT COMMENT '接收者ID（私聊时使用）',
    `group_id` BIGINT COMMENT '群组ID（群聊时使用）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT、IMAGE、FILE等',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0撤回，1正常',
    PRIMARY KEY (`id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_message_type` (`message_type`),
    CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 聊天列表表
CREATE TABLE IF NOT EXISTS `chat_list` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '聊天列表ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_id` BIGINT COMMENT '目标ID（用户ID或群组ID）',
    `chat_type` VARCHAR(10) NOT NULL COMMENT '聊天类型：PRIVATE、GROUP',
    `last_message_time` DATETIME COMMENT '最后消息时间',
    `unread_count` INT DEFAULT 0 COMMENT '未读消息数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `chat_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_last_message_time` (`last_message_time`),
    CONSTRAINT `fk_chat_list_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天列表表';
