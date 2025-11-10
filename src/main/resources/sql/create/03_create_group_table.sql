-- 群组信息表
-- 创建时间: 2025-11-10
-- 说明: 存储群组基础信息，支持分类和标签功能

CREATE TABLE IF NOT EXISTS `group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `group_avatar_url` VARCHAR(500) COMMENT '群组头像URL',
    `group_description` TEXT NULL COMMENT '群组描述',
    `status` INT DEFAULT 1 COMMENT '群组状态: 0=已解散, 1=正常, 2=冻结',
    `max_members` INT DEFAULT 500 COMMENT '最大成员数',
    `member_count` INT DEFAULT 0 COMMENT '当前成员数',
    `is_visible` INT DEFAULT 1 COMMENT '是否可见: 0=私密, 1=公开',
    `category_id` BIGINT COMMENT '群组分类ID',
    `tags` VARCHAR(500) COMMENT '群组标签（逗号分隔）',
    `last_transfer_at` DATETIME NULL COMMENT '最后转让时间',
    `transfer_count` INT DEFAULT 0 COMMENT '转让次数',
    `shared_chat_id` BIGINT NULL COMMENT '关联的共享聊天ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_update_time` (`update_time`),
    KEY `idx_group_status` (`status`),
    KEY `idx_group_owner` (`owner_id`, `status`),
    KEY `idx_is_visible` (`is_visible`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_shared_chat_id` (`shared_chat_id`),
    CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组信息表（增强版：支持分类和标签）';