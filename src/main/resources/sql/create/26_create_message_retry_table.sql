-- 消息重试队列表
-- 用于存储发送失败的消息重试记录，支持消息重试机制

CREATE TABLE IF NOT EXISTS `message_retry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '重试记录ID',
    `message_id` BIGINT COMMENT '关联的消息ID（如果消息已创建）',
    `client_message_id` VARCHAR(100) NOT NULL COMMENT '客户端消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT COMMENT '接收者ID（私聊）',
    `group_id` BIGINT COMMENT '群组ID（群聊）',
    `content` JSON NOT NULL COMMENT '消息内容',
    `message_type` INT DEFAULT 1 COMMENT '消息类型',
    `retry_count` INT DEFAULT 0 COMMENT '已重试次数',
    `max_retries` INT DEFAULT 5 COMMENT '最大重试次数',
    `last_error` TEXT COMMENT '最后一次失败的错误信息',
    `next_retry_at` TIMESTAMP NULL COMMENT '下次重试时间',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '重试状态：PENDING=待重试，RETRYING=重试中，SUCCESS=成功，FAILED=永久失败',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_message_id` (`client_message_id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_status` (`status`),
    KEY `idx_next_retry_at` (`next_retry_at`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_retry_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_retry_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='消息重试队列表';