-- 系统操作日志表
-- 用于记录系统管理员的操作日志，便于审计和追踪

CREATE TABLE IF NOT EXISTS `system_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_id` BIGINT COMMENT '操作员ID',
    `action` VARCHAR(100) NOT NULL COMMENT '操作类型 (e.g., BAN_USER, CREATE_ROLE)',
    `details` TEXT COMMENT '操作详情',
    `ip_address` VARCHAR(45) COMMENT '操作员IP地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='系统操作日志表';