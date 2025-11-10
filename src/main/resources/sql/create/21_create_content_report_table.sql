-- 内容举报表
-- 用于存储用户对各种内容的举报记录，支持内容审核功能

CREATE TABLE IF NOT EXISTS `content_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
    `content_type` VARCHAR(50) NOT NULL COMMENT '被举报内容类型（article, comment, message, user等）',
    `content_id` BIGINT NOT NULL COMMENT '被举报内容ID',
    `reason` VARCHAR(50) NOT NULL COMMENT '举报原因（spam, harassment, inappropriate_content, violence, copyright等）',
    `description` TEXT COMMENT '举报描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '举报状态（pending, reviewing, resolved, dismissed）',
    `reviewer_id` BIGINT COMMENT '处理人ID（管理员）',
    `action` VARCHAR(50) COMMENT '处理结果（remove_content, warn_user, ban_user, no_action）',
    `review_note` TEXT COMMENT '处理说明',
    `reviewed_at` TIMESTAMP NULL COMMENT '处理时间',
    `metadata` JSON COMMENT '附加信息（截图、链接等证据）',
    `report_count` INT NOT NULL DEFAULT 1 COMMENT '举报计数（同一内容被多少人举报）',
    `is_urgent` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为紧急举报',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_reporter_id` (`reporter_id`),
    INDEX `idx_content` (`content_type`, `content_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reason` (`reason`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_is_urgent` (`is_urgent`),
    INDEX `idx_reviewer_id` (`reviewer_id`),
    CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_report_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容举报表';