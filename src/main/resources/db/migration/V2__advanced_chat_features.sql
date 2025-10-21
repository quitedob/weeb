-- 高级聊天功能数据库迁移脚本
-- 创建时间: 2024-01-01

-- 1. 消息线索表
CREATE TABLE IF NOT EXISTS message_threads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    root_message_id BIGINT NOT NULL COMMENT '根消息ID',
    title VARCHAR(255) NOT NULL COMMENT '线索标题',
    description TEXT COMMENT '线索描述',
    created_by BIGINT NOT NULL COMMENT '线索创建者ID',
    created_by_username VARCHAR(50) NOT NULL COMMENT '线索创建者用户名',
    status ENUM('active', 'archived', 'closed', 'deleted') NOT NULL DEFAULT 'active' COMMENT '线索状态',
    participant_count INT NOT NULL DEFAULT 1 COMMENT '参与者数量',
    last_reply_at TIMESTAMP NULL COMMENT '最后回复时间',
    last_reply_by BIGINT NULL COMMENT '最后回复者ID',
    last_reply_by_username VARCHAR(50) NULL COMMENT '最后回复者用户名',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否置顶',
    is_locked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否锁定',
    tags JSON COMMENT '线索标签',
    metadata JSON COMMENT '附加元数据',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_root_message_id (root_message_id),
    INDEX idx_created_by (created_by),
    INDEX idx_status (status),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_last_reply_at (last_reply_at),
    INDEX idx_created_at (created_at),

    FOREIGN KEY (root_message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息线索表';

-- 2. 线索参与者表
CREATE TABLE IF NOT EXISTS thread_participants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thread_id BIGINT NOT NULL COMMENT '线索ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    left_at TIMESTAMP NULL COMMENT '离开时间',
    role ENUM('participant', 'moderator', 'owner') NOT NULL DEFAULT 'participant' COMMENT '角色',
    metadata JSON COMMENT '附加元数据',

    UNIQUE KEY uk_thread_user (thread_id, user_id),
    INDEX idx_thread_id (thread_id),
    INDEX idx_user_id (user_id),
    INDEX idx_joined_at (joined_at),

    FOREIGN KEY (thread_id) REFERENCES message_threads(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线索参与者表';

-- 3. 用户提及表
CREATE TABLE IF NOT EXISTS user_mentions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL COMMENT '消息ID',
    mentioner_id BIGINT NOT NULL COMMENT '提及者ID',
    mentioner_username VARCHAR(50) NOT NULL COMMENT '提及者用户名',
    mentioned_user_id BIGINT NOT NULL COMMENT '被提及用户ID',
    mentioned_username VARCHAR(50) NOT NULL COMMENT '被提及用户名',
    mention_text VARCHAR(100) NOT NULL COMMENT '提及文本',
    start_position INT NOT NULL DEFAULT 0 COMMENT '起始位置',
    end_position INT NOT NULL DEFAULT 0 COMMENT '结束位置',
    status ENUM('pending', 'delivered', 'read') NOT NULL DEFAULT 'pending' COMMENT '提及状态',
    mention_type ENUM('mention', 'reply', 'reply_all') NOT NULL DEFAULT 'mention' COMMENT '提及类型',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
    read_at TIMESTAMP NULL COMMENT '读取时间',
    is_auto_generated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否自动生成',
    metadata JSON COMMENT '附加元数据',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_message_id (message_id),
    INDEX idx_mentioner_id (mentioner_id),
    INDEX idx_mentioned_user_id (mentioned_user_id),
    INDEX idx_status (status),
    INDEX idx_is_read (is_read),
    INDEX idx_mention_type (mention_type),
    INDEX idx_created_at (created_at),
    INDEX idx_mentioned_user_unread (mentioned_user_id, is_read),

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (mentioner_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提及表';

-- 4. 链接预览表
CREATE TABLE IF NOT EXISTS link_previews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_url TEXT NOT NULL COMMENT '原始链接URL',
    final_url TEXT COMMENT '最终重定向URL',
    title VARCHAR(255) COMMENT '页面标题',
    description TEXT COMMENT '页面描述',
    site_name VARCHAR(100) COMMENT '网站名称',
    site_icon VARCHAR(500) COMMENT '网站图标URL',
    image_url VARCHAR(1000) COMMENT '预览图片URL',
    image_width INT COMMENT '图片宽度',
    image_height INT COMMENT '图片高度',
    content_type ENUM('website', 'article', 'image', 'video', 'audio', 'product', 'social', 'other')
             NOT NULL DEFAULT 'website' COMMENT '内容类型',
    status ENUM('pending', 'success', 'failed', 'no_preview') NOT NULL DEFAULT 'pending' COMMENT '预览状态',
    error_message TEXT COMMENT '错误信息',
    generation_time BIGINT COMMENT '预览生成时间（毫秒）',
    expires_at TIMESTAMP NOT NULL DEFAULT (DATE_ADD(NOW(), INTERVAL 24 HOUR)) COMMENT '预览缓存过期时间',
    og_tags JSON COMMENT 'Open Graph标签',
    twitter_tags JSON COMMENT 'Twitter Card标签',
    message_id BIGINT NOT NULL COMMENT '关联的消息ID',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    is_disabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否被禁用',
    user_rating TINYINT COMMENT '用户评分（1-5星）',
    metadata JSON COMMENT '额外元数据',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_message_id (message_id),
    INDEX idx_created_by (created_by),
    INDEX idx_status (status),
    INDEX idx_content_type (content_type),
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at),
    INDEX idx_original_url_hash (original_url(100)),

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='链接预览表';

-- 5. 更新现有消息表，添加线索关联
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS thread_id BIGINT NULL COMMENT '所属线索ID' AFTER chat_id,
ADD INDEX IF NOT EXISTS idx_thread_id (thread_id),
ADD FOREIGN KEY IF NOT EXISTS fk_message_thread (thread_id) REFERENCES message_threads(id) ON DELETE SET NULL;

-- 6. 创建触发器：当消息被回复时自动创建线索
DELIMITER //
CREATE TRIGGER IF NOT EXISTS auto_create_thread_on_reply
AFTER INSERT ON messages
FOR EACH ROW
BEGIN
    DECLARE thread_exists INT DEFAULT 0;

    -- 如果消息有回复目标且不属于现有线索
    IF NEW.reply_to_id IS NOT NULL AND NEW.thread_id IS NULL THEN
        -- 检查回复目标是否已有线索
        SELECT COUNT(*) INTO thread_exists
        FROM message_threads
        WHERE root_message_id = NEW.reply_to_id;

        -- 如果没有线索，创建新线索
        IF thread_exists = 0 THEN
            INSERT INTO message_threads (
                root_message_id, title, created_by, created_by_username
            ) VALUES (
                NEW.reply_to_id,
                IF(CHAR_LENGTH(NEW.content) > 50,
                   CONCAT(SUBSTRING(NEW.content, 1, 47), '...'),
                   NEW.content),
                NEW.user_id,
                (SELECT username FROM users WHERE id = NEW.user_id LIMIT 1)
            );

            -- 更新消息的线索ID
            UPDATE messages SET thread_id = LAST_INSERT_ID() WHERE id = NEW.id;
        END IF;
    END IF;
END//
DELIMITER ;

-- 7. 创建触发器：当线索收到新消息时更新最后回复信息
DELIMITER //
CREATE TRIGGER IF NOT EXISTS update_thread_last_reply
AFTER INSERT ON messages
FOR EACH ROW
BEGIN
    IF NEW.thread_id IS NOT NULL THEN
        UPDATE message_threads
        SET
            last_reply_at = NEW.created_at,
            last_reply_by = NEW.user_id,
            last_reply_by_username = (SELECT username FROM users WHERE id = NEW.user_id LIMIT 1),
            participant_count = (
                SELECT COUNT(DISTINCT user_id)
                FROM messages
                WHERE thread_id = NEW.thread_id
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.thread_id;
    END IF;
END//
DELIMITER ;

-- 8. 创建存储过程：清理过期的链接预览
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_expired_previews()
BEGIN
    DECLARE cleaned_count INT DEFAULT 0;

    -- 删除过期的预览
    DELETE FROM link_previews
    WHERE expires_at < NOW() OR status = 'failed';

    SET cleaned_count = ROW_COUNT();

    -- 记录清理日志
    INSERT INTO system_logs (log_type, message, created_at)
    VALUES ('CLEANUP', CONCAT('Cleaned up ', cleaned_count, ' expired previews'), NOW());

    SELECT cleaned_count as cleaned_previews;
END//
DELIMITER ;

-- 9. 创建存储过程：获取用户提及统计
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS get_user_mention_stats(
    IN user_id_param BIGINT,
    IN days_param INT
)
BEGIN
    SELECT
        COUNT(*) as total_mentions,
        COUNT(CASE WHEN is_read = FALSE THEN 1 END) as unread_mentions,
        COUNT(CASE WHEN DATE(created_at) = CURDATE() THEN 1 END) as today_mentions,
        COUNT(CASE WHEN mention_type = 'mention' THEN 1 END) as direct_mentions,
        COUNT(CASE WHEN mention_type = 'reply' THEN 1 END) as reply_mentions,
        COUNT(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL days_param DAY) THEN 1 END) as recent_mentions
    FROM user_mentions
    WHERE mentioned_user_id = user_id_param;
END//
DELIMITER ;

-- 10. 创建视图：热门线索
CREATE OR REPLACE VIEW hot_threads AS
SELECT
    t.*,
    COUNT(m.id) as message_count,
    COUNT(DISTINCT m.user_id) as unique_participants,
    MAX(m.created_at) as last_activity
FROM message_threads t
LEFT JOIN messages m ON t.id = m.thread_id
WHERE t.status = 'active' AND t.is_locked = FALSE
GROUP BY t.id
HAVING message_count > 1
ORDER BY message_count DESC, last_activity DESC;

-- 11. 创建视图：用户提及摘要
CREATE OR REPLACE VIEW user_mention_summary AS
SELECT
    mentioned_user_id,
    mentioned_username,
    COUNT(*) as total_mentions,
    COUNT(CASE WHEN is_read = FALSE THEN 1 END) as unread_mentions,
    COUNT(CASE WHEN DATE(created_at) = CURDATE() THEN 1 END) as today_mentions,
    MAX(created_at) as last_mention_at
FROM user_mentions
GROUP BY mentioned_user_id, mentioned_username;

-- 12. 创建视图：域名预览统计
CREATE OR REPLACE VIEW domain_preview_stats AS
SELECT
    SUBSTRING_INDEX(SUBSTRING_INDEX(original_url, '/', 3), '://', -1) as domain,
    COUNT(*) as total_previews,
    COUNT(CASE WHEN status = 'success' THEN 1 END) as successful_previews,
    COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_previews,
    AVG(generation_time) as avg_generation_time,
    MAX(created_at) as last_preview_at
FROM link_previews
WHERE original_url IS NOT NULL
GROUP BY domain
HAVING total_previews >= 3
ORDER BY total_previews DESC;

-- 13. 添加全文搜索索引（如果支持）
-- ALTER TABLE message_threads ADD FULLTEXT(title, description);
-- ALTER TABLE user_mentions ADD FULLTEXT(mention_text);
-- ALTER TABLE link_previews ADD FULLTEXT(title, description);

-- 14. 设置事件调度：每天清理过期预览
SET GLOBAL event_scheduler = ON;
CREATE EVENT IF NOT EXISTS daily_preview_cleanup
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '02:00:00')
DO CALL cleanup_expired_previews();

-- 15. 插入初始配置数据
INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('chat.thread.max_participants', '100', '线索最大参与者数量'),
('chat.thread.title_max_length', '255', '线索标题最大长度'),
('chat.mention.max_per_message', '20', '每条消息最大提及数量'),
('chat.preview.cache_duration_hours', '24', '链接预览缓存时长（小时）'),
('chat.preview.max_image_size_kb', '1024', '链接预览图片最大大小（KB）'),
('chat.thread.auto_archive_days', '30', '线索自动归档天数');

-- 添加权限配置
INSERT IGNORE INTO permissions (name, description, module) VALUES
('MESSAGE_THREAD_CREATE_OWN', '创建自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_READ_OWN', '查看自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_READ_ANY', '查看任意消息线索', 'CHAT'),
('MESSAGE_THREAD_UPDATE_OWN', '更新自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_DELETE_OWN', '删除自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_JOIN_OWN', '加入消息线索', 'CHAT'),
('MESSAGE_THREAD_LEAVE_OWN', '离开消息线索', 'CHAT'),
('MESSAGE_THREAD_ARCHIVE_OWN', '归档自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_CLOSE_OWN', '关闭自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_PIN_OWN', '置顶自己的消息线索', 'CHAT'),
('MESSAGE_THREAD_LOCK_OWN', '锁定自己的消息线索', 'CHAT'),
('MESSAGE_MENTION_CREATE_OWN', '创建用户提及', 'CHAT'),
('MESSAGE_MENTION_READ_OWN', '查看收到的提及', 'CHAT'),
('MESSAGE_REPLY_OWN', '回复消息', 'CHAT'),
('LINK_PREVIEW_CREATE_OWN', '创建链接预览', 'CHAT'),
('LINK_PREVIEW_READ_OWN', '查看链接预览', 'CHAT'),
('LINK_PREVIEW_UPDATE_OWN', '更新链接预览', 'CHAT'),
('LINK_PREVIEW_DELETE_OWN', '删除链接预览', 'CHAT');

COMMIT;