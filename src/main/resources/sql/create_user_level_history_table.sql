-- 创建用户等级变更历史表
CREATE TABLE IF NOT EXISTS `user_level_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `old_level` INT NOT NULL COMMENT '原等级',
    `new_level` INT NOT NULL COMMENT '新等级',
    `change_reason` VARCHAR(500) COMMENT '变更原因',
    `change_type` INT NOT NULL DEFAULT 1 COMMENT '变更类型（1:自动升级 2:手动调整 3:系统调整 4:降级）',
    `operator_id` BIGINT COMMENT '操作者ID',
    `operator_name` VARCHAR(100) COMMENT '操作者姓名',
    `change_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    `ip_address` VARCHAR(45) COMMENT '操作IP地址',
    `user_agent` TEXT COMMENT '用户代理信息',
    `remark` TEXT COMMENT '备注',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态（1:有效 0:无效）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_change_time` (`change_time`),
    INDEX `idx_change_type` (`change_type`),
    INDEX `idx_operator_id` (`operator_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_user_time` (`user_id`, `change_time`),
    INDEX `idx_level_change` (`old_level`, `new_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户等级变更历史表';

-- 添加外键约束（如果需要关联到用户表）
-- ALTER TABLE `user_level_history` ADD CONSTRAINT `fk_user_level_history_user`
--     FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `user_level_history` ADD CONSTRAINT `fk_user_level_history_operator`
--     FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL;

-- 插入示例数据（可选）
-- INSERT INTO `user_level_history` (user_id, old_level, new_level, change_reason, change_type, operator_id, operator_name) VALUES
-- (1, 1, 2, '用户活跃度提升，自动升级', 1, NULL, '系统'),
-- (2, 2, 3, '管理员手动提升权限', 2, 1, 'admin'),
-- (3, 3, 2, '违规行为降级处理', 4, 1, 'admin');