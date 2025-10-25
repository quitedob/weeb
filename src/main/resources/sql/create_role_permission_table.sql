-- 创建角色权限关联表
CREATE TABLE IF NOT EXISTS `role_permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:禁用）',
    `created_by` BIGINT COMMENT '创建者ID',
    `updated_by` BIGINT COMMENT '更新者ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` TEXT COMMENT '备注',

    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 添加外键约束（如果需要关联到相应的表）
-- ALTER TABLE `role_permission` ADD CONSTRAINT `fk_role_permission_role`
--     FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `role_permission` ADD CONSTRAINT `fk_role_permission_permission`
--     FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `role_permission` ADD CONSTRAINT `fk_role_permission_creator`
--     FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;
-- ALTER TABLE `role_permission` ADD CONSTRAINT `fk_role_permission_updater`
--     FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`) ON DELETE SET NULL;

-- 插入示例数据（可选）
-- INSERT INTO `role_permission` (role_id, permission_id, created_by, remark) VALUES
-- (1, 1, 1, '管理员拥有用户管理权限'),
-- (1, 2, 1, '管理员拥有角色管理权限'),
-- (2, 3, 1, '普通用户拥有基本权限');