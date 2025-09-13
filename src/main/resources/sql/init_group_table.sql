-- 群组表初始化SQL
CREATE TABLE IF NOT EXISTS `group_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `owner_id` BIGINT NOT NULL COMMENT '群主ID',
    `description` TEXT COMMENT '群组简介',
    `avatar` VARCHAR(500) COMMENT '群组头像',
    `member_count` INT DEFAULT 1 COMMENT '成员数量',
    `max_members` INT DEFAULT 100 COMMENT '最大成员数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0解散，1正常',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组信息表';

-- 群组成员表
CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色：OWNER、ADMIN、MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0离开，1正常',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role` (`role`),
    CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `group_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';
