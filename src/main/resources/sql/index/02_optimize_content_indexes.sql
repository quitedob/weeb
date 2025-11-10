-- 内容相关数据库索引优化
-- 创建时间: 2025-11-10
-- 说明: 优化文章、通知等内容相关的查询索引

-- 5. article表索引优化
-- 优化文章状态查询
CREATE INDEX IF NOT EXISTS idx_article_status_time
ON articles (status, created_at DESC);

-- 优化文章作者查询
CREATE INDEX IF NOT EXISTS idx_article_author_status
ON articles (user_id, status, created_at DESC);

-- 优化文章审核优先级查询
CREATE INDEX IF NOT EXISTS idx_article_moderation
ON articles (status, review_priority DESC, created_at ASC);

-- 6. notification表索引优化
-- 优化用户通知查询
CREATE INDEX IF NOT EXISTS idx_notification_user_read
ON notifications (recipient_id, is_read, created_at DESC);

-- 7. article_comment表索引优化
-- 优化文章评论查询
CREATE INDEX IF NOT EXISTS idx_article_comment_time
ON article_comment (article_id, created_at DESC);

-- 8. user_follow表索引优化
-- 优化关注关系查询
CREATE INDEX IF NOT EXISTS idx_user_follow_relation
ON user_follow (follower_id, followee_id);

-- 优化粉丝查询
CREATE INDEX IF NOT EXISTS idx_user_follow_followee
ON user_follow (followee_id, created_at DESC);