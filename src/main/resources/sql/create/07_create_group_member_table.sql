-- 群组成员关系表（增强版：支持申请审批流程）
-- Creation Date: 2025-11-10
-- Description: 管理群组成员关系，支持角色权限管理、申请审批流程和成员状态跟踪

CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` INT NOT NULL DEFAULT 3 COMMENT '成员角色：1=群主，2=管理员，3=普通成员（角色值越小权限越高）',
    `join_status` VARCHAR(20) DEFAULT 'ACCEPTED' COMMENT '加入状态：PENDING=待审批，ACCEPTED=已接受，REJECTED=已拒绝，BLOCKED=已屏蔽',
    `invited_by` BIGINT COMMENT '邀请人ID',
    `invite_reason` VARCHAR(500) COMMENT '邀请/申请原因',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `kicked_at` DATETIME COMMENT '被移除时间',
    `kick_reason` VARCHAR(500) COMMENT '被移除原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role` (`role`),
    KEY `idx_join_status` (`join_status`),
    KEY `idx_invited_by` (`invited_by`),
    KEY `idx_join_time` (`join_time`),
    KEY `idx_group_role_status` (`group_id`, `role`, `join_status`),
    CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_member_inviter` FOREIGN KEY (`invited_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='群组成员关系表（增强版：支持申请审批流程）';