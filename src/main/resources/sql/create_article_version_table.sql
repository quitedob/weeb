-- 创建文章版本表
CREATE TABLE IF NOT EXISTS `article_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    UNIQUE KEY `uk_article_version` (`article_id`, `version_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章版本表';

-- 添加外键约束（如果需要）
-- ALTER TABLE `article_version` ADD CONSTRAINT `fk_version_article`
--     FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE;
-- ALTER TABLE `article_version` ADD CONSTRAINT `fk_version_creator`
--     FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE;

-- 创建版本状态枚举检查约束
ALTER TABLE `article_version` ADD CONSTRAINT `chk_status`
    CHECK (status IN ('draft', 'published', 'archived', 'deleted'));

-- 创建变更类型枚举检查约束
ALTER TABLE `article_version` ADD CONSTRAINT `chk_change_type`
    CHECK (change_type IN ('create', 'update', 'minor_edit', 'major_edit', 'title_change', 'content_change', 'auto_save'));

-- 插入示例数据（可选）
-- INSERT INTO `article_version` (article_id, version_number, title, content, created_by, created_by_username) VALUES
-- (1, 1, 'Sample Article', 'This is the first version of the article.', 1, 'admin'),
-- (1, 2, 'Updated Sample Article', 'This is the updated version with more content.', 1, 'admin');

-- 创建视图：文章版本概览
CREATE OR REPLACE VIEW `article_version_overview` AS
SELECT
    a.id as article_id,
    a.title as current_title,
    av.latest_version,
    av.latest_published_version,
    av.total_versions,
    av.major_versions,
    av.last_modified,
    av.last_modified_by
FROM articles a
LEFT JOIN (
    SELECT
        article_id,
        MAX(CASE WHEN status = 'published' THEN version_number END) as latest_published_version,
        MAX(version_number) as latest_version,
        COUNT(*) as total_versions,
        COUNT(CASE WHEN is_major_version = true THEN 1 END) as major_versions,
        MAX(created_at) as last_modified,
        MAX(CASE WHEN created_at = MAX(created_at) OVER (PARTITION BY article_id) THEN created_by END) as last_modified_by
    FROM article_version
    GROUP BY article_id
) av ON a.id = av.article_id;