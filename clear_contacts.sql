-- 清空所有联系人关系（谨慎使用！）
TRUNCATE TABLE contact;

-- 或者只删除testuser和admin之间的关系
-- DELETE FROM contact WHERE (user_id = 2 AND friend_id = 1) OR (user_id = 1 AND friend_id = 2);

-- 验证是否清空成功
SELECT COUNT(*) as total_contacts FROM contact;
