-- 用户等级变更历史表
-- 用于记录用户等级变更的详细历史，支持审计和追踪

CREATE TABLE IF NOT EXISTS `user_level_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `old_level` INT COMMENT '原等级',
    `new_level` INT NOT NULL COMMENT '新等级',
    `change_reason` VARCHAR(500) COMMENT '变更原因',
    `change_type` INT NOT NULL COMMENT '变更类型 1:系统自动 2:管理员操作 3:用户行为触发',
    `operator_id` BIGINT COMMENT '操作者ID',
    `operator_name` VARCHAR(100) COMMENT '操作者名称',
    `change_time` DATETIME NOT NULL COMMENT '变更时间',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `remark` VARCHAR(500) COMMENT '备注',
    `status` INT DEFAULT 1 COMMENT '状态 0:无效 1:有效',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_change_type (change_type),
    INDEX idx_operator_id (operator_id),
    INDEX idx_change_time (change_time),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_user_time (user_id, change_time),
    INDEX idx_level_change (old_level, new_level),
    CONSTRAINT `fk_user_level_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户等级变更历史表';