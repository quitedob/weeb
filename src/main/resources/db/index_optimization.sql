-- ========================================
-- 数据库索引优化脚本
-- 根据todo.txt中的索引优化需求
-- ========================================

-- 1. message表索引优化
-- 已有索引检查，如果不存在则创建

-- 优化私聊消息查询 (sender_id, receiver_id, created_at)
CREATE INDEX IF NOT EXISTS idx_message_private_chat_optimized 
ON message(sender_id, receiver_id, created_at DESC);

-- 优化群聊消息查询 (group_id, created_at)
CREATE INDEX IF NOT EXISTS idx_message_group_chat_optimized 
ON message(group_id, created_at DESC);

-- 优化chat_id相关查询 (chat_id, created_at)
CREATE INDEX IF NOT EXISTS idx_message_chat_time 
ON message(chat_id, created_at DESC);

-- 2. chat_list表索引优化
-- 优化用户聊天列表查询 (user_id, update_time)
CREATE INDEX IF NOT EXISTS idx_chat_list_user_update 
ON chat_list(user_id, update_time DESC);

-- 优化聊天类型查询 (user_id, type, update_time)
CREATE INDEX IF NOT EXISTS idx_chat_list_user_type 
ON chat_list(user_id, type, update_time DESC);

-- 3. group_member表索引优化
-- 优化群组成员查询 (group_id, user_id)
-- 注意：已有UNIQUE KEY `uk_group_user` (`group_id`, `user_id`)，无需重复创建

-- 优化用户所在群组查询 (user_id, join_status)
CREATE INDEX IF NOT EXISTS idx_group_member_user_status 
ON group_member(user_id, join_status);

-- 4. contact表索引优化
-- 优化联系人查询 (user_id, friend_id, status)
CREATE INDEX IF NOT EXISTS idx_contact_user_friend_status 
ON contact(user_id, friend_id, status);

-- 优化好友状态查询 (user_id, status)
CREATE INDEX IF NOT EXISTS idx_contact_user_status 
ON contact(user_id, status);

-- 5. article表索引优化
-- 优化文章状态查询 (status, created_at)
CREATE INDEX IF NOT EXISTS idx_article_status_time 
ON article(status, created_at DESC);

-- 优化文章作者查询 (author_id, status, created_at)
CREATE INDEX IF NOT EXISTS idx_article_author_status 
ON article(author_id, status, created_at DESC);

-- 优化文章审核优先级查询 (status, moderation_priority, created_at)
CREATE INDEX IF NOT EXISTS idx_article_moderation 
ON article(status, moderation_priority DESC, created_at ASC);

-- 6. user_stats表索引优化
-- 优化用户统计查询 (user_id)
-- 注意：user_id应该已经是主键或有唯一索引

-- 7. notification表索引优化
-- 优化用户通知查询 (user_id, is_read, created_at)
CREATE INDEX IF NOT EXISTS idx_notification_user_read 
ON notification(user_id, is_read, created_at DESC);

-- 8. article_comment表索引优化
-- 优化文章评论查询 (article_id, created_at)
CREATE INDEX IF NOT EXISTS idx_article_comment_time 
ON article_comment(article_id, created_at DESC);

-- 9. user_follow表索引优化
-- 优化关注关系查询 (follower_id, followee_id)
CREATE INDEX IF NOT EXISTS idx_user_follow_relation 
ON user_follow(follower_id, followee_id);

-- 优化粉丝查询 (followee_id, created_at)
CREATE INDEX IF NOT EXISTS idx_user_follow_followee 
ON user_follow(followee_id, created_at DESC);

-- ========================================
-- 索引使用说明
-- ========================================
-- 1. 复合索引遵循最左前缀原则
-- 2. 查询条件应该按照索引列的顺序使用
-- 3. 避免在索引列上使用函数或表达式
-- 4. 定期使用ANALYZE TABLE更新索引统计信息
-- 5. 使用EXPLAIN分析查询计划，确保索引被正确使用

-- ========================================
-- 索引维护命令
-- ========================================
-- 查看表的索引：
-- SHOW INDEX FROM table_name;

-- 分析表的索引使用情况：
-- ANALYZE TABLE table_name;

-- 优化表（重建索引）：
-- OPTIMIZE TABLE table_name;

-- 查看索引统计信息：
-- SELECT * FROM information_schema.STATISTICS 
-- WHERE TABLE_SCHEMA = 'weeb' AND TABLE_NAME = 'message';
