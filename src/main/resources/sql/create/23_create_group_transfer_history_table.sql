-- 群组转让历史表
-- 用于记录群主转让的历史，便于追踪和审计

CREATE TABLE IF NOT EXISTS `group_transfer_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `from_user_id` BIGINT NOT NULL COMMENT '原群主ID',
    `to_user_id` BIGINT NOT NULL COMMENT '新群主ID',
    `transfer_reason` VARCHAR(255) NULL COMMENT '转让原因',
    `transfer_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '转让时间',
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_from_user` (`from_user_id`),
    INDEX `idx_to_user` (`to_user_id`),
    INDEX `idx_transfer_time` (`transfer_at`),
    CONSTRAINT `fk_transfer_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transfer_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transfer_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组转让历史表';