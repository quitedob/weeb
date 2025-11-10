-- 群组申请表
-- 用于存储用户申请加入群组的记录，支持审核流程

CREATE TABLE IF NOT EXISTS `group_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `message` VARCHAR(500) COMMENT '申请留言',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING=待审批，APPROVED=已通过，REJECTED=已拒绝',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `review_note` VARCHAR(500) COMMENT '审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `reviewed_at` DATETIME COMMENT '审核时间',
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at` DESC),
    INDEX `idx_group_status` (`group_id`, `status`),
    CONSTRAINT `fk_application_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_application_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组申请表';