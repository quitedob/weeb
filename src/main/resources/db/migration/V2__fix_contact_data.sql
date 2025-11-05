-- 修复联系人数据
-- 1. 删除自己加自己的错误记录
DELETE FROM contact WHERE user_id = friend_id;

-- 2. 为所有 ACCEPTED 状态的单向关系创建反向关系
INSERT INTO contact (user_id, friend_id, status, remarks, create_time, update_time)
SELECT 
    c.friend_id as user_id,
    c.user_id as friend_id,
    c.status,
    '好友' as remarks,
    NOW() as create_time,
    NOW() as update_time
FROM contact c
WHERE c.status = 1  -- ACCEPTED
  AND NOT EXISTS (
    SELECT 1 FROM contact c2 
    WHERE c2.user_id = c.friend_id 
      AND c2.friend_id = c.user_id
  );

-- 3. 添加约束防止自己加自己
ALTER TABLE contact 
ADD CONSTRAINT chk_not_self_contact 
CHECK (user_id != friend_id);
