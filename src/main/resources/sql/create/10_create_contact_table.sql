-- 用户联系人关系表
-- Creation Date: 2025-11-10
-- Description: 管理用户之间的好友关系，支持好友申请、确认、拒绝和过期流程

CREATE TABLE IF NOT EXISTS `contact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `status` INT NOT NULL DEFAULT 0 COMMENT '关系状态：0待确认，1已确认，2已拒绝，3已删除，4已过期',
    `remarks` VARCHAR(255) COMMENT '备注/申请附言',
    `expire_at` TIMESTAMP NULL COMMENT '好友请求过期时间，PENDING状态下有效',
    `group_id` BIGINT NULL COMMENT '所属分组ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_contact_status_expire` (`status`, `expire_at`),
    KEY `idx_contact_group_id` (`group_id`),
    CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_contact_friend` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_not_self_contact` CHECK (`user_id` != `friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='用户联系人关系表';