-- 文章版本表
-- 用于存储文章的版本历史，支持版本控制和回滚功能

CREATE TABLE IF NOT EXISTS `article_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID（关联到articles表）',
    `version_number` INT NOT NULL COMMENT '版本号',
    `title` VARCHAR(500) NOT NULL COMMENT '文章标题',
    `content` LONGTEXT COMMENT '文章内容',
    `summary` TEXT COMMENT '文章摘要',
    `article_link` VARCHAR(1000) COMMENT '文章链接',
    `category_id` BIGINT COMMENT '分类ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '文章状态（draft, published, archived等）',
    `tags` JSON COMMENT '标签（JSON格式）',
    `cover_image` VARCHAR(1000) COMMENT '封面图片URL',
    `version_note` TEXT COMMENT '版本说明',
    `created_by` BIGINT NOT NULL COMMENT '创建者ID',
    `created_by_username` VARCHAR(100) COMMENT '创建者用户名',
    `change_type` VARCHAR(50) NOT NULL DEFAULT 'create' COMMENT '变更类型（create, update, minor_edit, major_edit等）',
    `change_summary` TEXT COMMENT '变更摘要（主要修改点）',
    `character_change` INT COMMENT '字符数变化',
    `is_major_version` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否为主要版本',
    `is_auto_save` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否自动保存版本',
    `size` INT COMMENT '版本大小（字符数）',
    `content_hash` VARCHAR(64) COMMENT '版本哈希值（用于内容去重）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_article_id` (`article_id`),
    INDEX `idx_version_number` (`version_number`),
    INDEX `idx_created_by` (`created_by`),
    INDEX `idx_status` (`status`),
    INDEX `idx_change_type` (`change_type`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_is_major_version` (`is_major_version`),
    INDEX `idx_is_auto_save` (`is_auto_save`),
    INDEX `idx_content_hash` (`content_hash`),
    UNIQUE KEY `uk_article_version` (`article_id`, `version_number`),
    CONSTRAINT `fk_version_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`article_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_version_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章版本表';

-- 添加检查约束
ALTER TABLE `article_version` ADD CONSTRAINT `chk_status` CHECK (status IN ('draft', 'published', 'archived', 'deleted'));
ALTER TABLE `article_version` ADD CONSTRAINT `chk_change_type` CHECK (change_type IN ('create', 'update', 'minor_edit', 'major_edit', 'title_change', 'content_change', 'auto_save'));