-- 查看所有联系人记录及其状态
SELECT 
    c.id,
    c.user_id,
    u1.username as user_name,
    c.friend_id,
    u2.username as friend_name,
    c.status,
    CASE c.status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'ACCEPTED'
        WHEN 2 THEN 'REJECTED'
        WHEN 3 THEN 'BLOCKED'
        ELSE 'UNKNOWN'
    END as status_name,
    c.remarks,
    c.create_time,
    c.update_time
FROM contact c
LEFT JOIN user u1 ON c.user_id = u1.id
LEFT JOIN user u2 ON c.friend_id = u2.id
ORDER BY c.create_time DESC;

-- 查看testuser(ID=2)和admin(ID=1)之间的关系
SELECT 
    c.id,
    c.user_id,
    u1.username as user_name,
    c.friend_id,
    u2.username as friend_name,
    c.status,
    CASE c.status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'ACCEPTED'
        WHEN 2 THEN 'REJECTED'
        WHEN 3 THEN 'BLOCKED'
        ELSE 'UNKNOWN'
    END as status_name,
    c.remarks,
    c.create_time
FROM contact c
LEFT JOIN user u1 ON c.user_id = u1.id
LEFT JOIN user u2 ON c.friend_id = u2.id
WHERE (c.user_id = 2 AND c.friend_id = 1)
   OR (c.user_id = 1 AND c.friend_id = 2);

-- 如果需要删除这条记录，使用以下SQL（请谨慎使用）
-- DELETE FROM contact WHERE (user_id = 2 AND friend_id = 1) OR (user_id = 1 AND friend_id = 2);
