-- 核心数据库索引优化
-- 创建时间: 2025-11-10
-- 说明: 优化核心表的关键查询索引，提升系统性能

-- 1. message表索引优化
-- 优化私聊消息查询
CREATE INDEX IF NOT EXISTS idx_message_private_chat_optimized
ON message (sender_id, receiver_id, created_at DESC);

-- 优化群聊消息查询
CREATE INDEX IF NOT EXISTS idx_message_group_chat_optimized
ON message (group_id, created_at DESC);

-- 优化chat_id相关查询
CREATE INDEX IF NOT EXISTS idx_message_chat_time
ON message (chat_id, created_at DESC);

-- 2. chat_list表索引优化
-- 优化用户聊天列表查询
CREATE INDEX IF NOT EXISTS idx_chat_list_user_update
ON chat_list (user_id, update_time DESC);

-- 优化聊天类型查询
CREATE INDEX IF NOT EXISTS idx_chat_list_user_type
ON chat_list (user_id, type, update_time DESC);

-- 3. group_member表索引优化
-- 优化用户所在群组查询
CREATE INDEX IF NOT EXISTS idx_group_member_user_status
ON group_member (user_id, join_status);

-- 4. contact表索引优化
-- 优化联系人查询
CREATE INDEX IF NOT EXISTS idx_contact_user_friend_status
ON contact (user_id, friend_id, status);

-- 优化好友状态查询
CREATE INDEX IF NOT EXISTS idx_contact_user_status
ON contact (user_id, status);