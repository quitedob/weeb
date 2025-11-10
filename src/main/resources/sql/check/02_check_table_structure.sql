-- 数据库表结构检查
-- 创建时间: 2025-11-10
-- 说明: 检查所有表的结构是否正确创建

-- 检查用户相关表
SHOW CREATE TABLE user;
SHOW CREATE TABLE user_stats;
SHOW CREATE TABLE user_follow;
SHOW CREATE TABLE user_level_history;

-- 检查聊天相关表
SHOW CREATE TABLE shared_chat;
SHOW CREATE TABLE chat_list;
SHOW CREATE TABLE message;
SHOW CREATE TABLE chat_unread_count;
SHOW CREATE TABLE message_retry;
SHOW CREATE TABLE message_reaction;

-- 检查群组相关表
SHOW CREATE TABLE `group`;
SHOW CREATE TABLE group_member;
SHOW CREATE TABLE group_transfer_history;
SHOW CREATE TABLE group_application;

-- 检查文章相关表
SHOW CREATE TABLE articles;
SHOW CREATE TABLE article_category;
SHOW CREATE TABLE article_tag;
SHOW CREATE TABLE article_tag_relation;
SHOW CREATE TABLE article_comment;
SHOW CREATE TABLE article_like;
SHOW CREATE TABLE article_favorite;
SHOW CREATE TABLE article_version;
SHOW CREATE TABLE article_moderation_history;

-- 检查联系人相关表
SHOW CREATE TABLE contact;
SHOW CREATE TABLE contact_group;

-- 检查通知表
SHOW CREATE TABLE notifications;

-- 检查系统相关表
SHOW CREATE TABLE system_logs;
SHOW CREATE TABLE content_report;