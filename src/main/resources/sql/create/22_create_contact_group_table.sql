-- 联系人分组表
-- 用于用户管理联系人分组，支持自定义分组和排序

CREATE TABLE IF NOT EXISTS `contact_group` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `group_name` VARCHAR(50) NOT NULL COMMENT '分组名称',
    `group_order` INT DEFAULT 0 COMMENT '分组排序',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为默认分组',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_order` (`user_id`, `group_order`),
    CONSTRAINT `fk_contact_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='联系人分组表';