-- ========================================
-- 强制修复 testuser 权限问题
-- ========================================

-- 步骤 1: 确保 USER 角色存在
INSERT IGNORE INTO `role` (`name`, `description`, `status`, `type`, `level`, `is_default`, `created_at`, `updated_at`)
VALUES ('USER', '普通用户角色', 1, 0, 100, 1, NOW(), NOW());

-- 步骤 2: 获取 USER 角色 ID
SET @user_role_id = (SELECT id FROM `role` WHERE name = 'USER' LIMIT 1);

SELECT CONCAT('USER 角色 ID: ', @user_role_id) as info;

-- 步骤 3: 确保 testuser 有 USER 角色
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`, `created_at`)
SELECT u.id, @user_role_id, NOW()
FROM `user` u
WHERE u.username = 'testuser';

SELECT '✅ testuser 已分配 USER 角色' as status;

-- 步骤 4: 为 USER 角色分配所有 _OWN 和 _ANY 权限
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`, `created_at`)
SELECT @user_role_id, p.id, NOW()
FROM `permission` p
WHERE (p.name LIKE '%_OWN' OR p.name LIKE '%_ANY' OR p.name LIKE '%SEARCH%')
AND p.status = 1;

SELECT CONCAT('✅ 为 USER 角色分配了权限，当前总数: ', 
    (SELECT COUNT(*) FROM role_permission WHERE role_id = @user_role_id)
) as status;

-- 步骤 5: 验证 testuser 的 ARTICLE_CREATE_OWN 权限
SELECT '=== 验证结果 ===' as step;

SELECT 
    u.username as 用户名,
    r.name as 角色名,
    p.name as 权限名,
    p.description as 权限描述
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'testuser'
AND p.name IN ('ARTICLE_CREATE_OWN', 'ARTICLE_READ_OWN', 'ARTICLE_UPDATE_OWN', 'ARTICLE_DELETE_OWN', 'ARTICLE_READ_ANY')
ORDER BY p.name;

-- 步骤 6: 统计结果
SELECT 
    '权限统计' as 类型,
    COUNT(*) as 总权限数,
    SUM(CASE WHEN p.resource = 'ARTICLE' THEN 1 ELSE 0 END) as 文章权限数,
    SUM(CASE WHEN p.resource = 'COMMENT' THEN 1 ELSE 0 END) as 评论权限数,
    SUM(CASE WHEN p.resource = 'CONTACT' THEN 1 ELSE 0 END) as 好友权限数
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'testuser';
