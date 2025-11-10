-- 消息反应表（点赞、表情等）
-- 用于存储用户对消息的反应（表情回应、点赞等）

CREATE TABLE IF NOT EXISTS `message_reaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反应ID',
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `reaction_type` VARCHAR(50) NOT NULL COMMENT '反应类型：如👍、❤️、😂等',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user_reaction` (`message_id`, `user_id`, `reaction_type`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_reaction_type` (`reaction_type`),
    CONSTRAINT `fk_reaction_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_reaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='消息反应表（点赞、表情等）';