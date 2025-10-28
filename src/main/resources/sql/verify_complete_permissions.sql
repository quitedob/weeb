-- ========================================
-- 完整权限验证脚本
-- 验证用户是否拥有所有必要的权限
-- ========================================

-- 1. 检查 testuser 基本信息
SELECT '=== 1. testuser 基本信息 ===' as step;
SELECT id, username, user_email, type, status 
FROM `user` 
WHERE username = 'testuser';

-- 2. 检查 testuser 的角色
SELECT '=== 2. testuser 的角色 ===' as step;
SELECT 
    u.username,
    ur.role_id,
    r.name as role_name,
    r.description,
    ur.created_at as 分配时间
FROM `user` u
JOIN `user_role` ur ON u.id = ur.user_id
JOIN `role` r ON ur.role_id = r.id
WHERE u.username = 'testuser';

-- 3. 检查 USER 角色的权限数量
SELECT '=== 3. USER 角色权限统计 ===' as step;
SELECT 
    r.name as 角色名,
    COUNT(rp.id) as 权限总数,
    SUM(CASE WHEN p.resource = 'ARTICLE' THEN 1 ELSE 0 END) as 文章权限,
    SUM(CASE WHEN p.resource = 'COMMENT' THEN 1 ELSE 0 END) as 评论权限,
    SUM(CASE WHEN p.resource = 'LIKE' THEN 1 ELSE 0 END) as 点赞权限,
    SUM(CASE WHEN p.resource = 'FAVORITE' THEN 1 ELSE 0 END) as 收藏权限,
    SUM(CASE WHEN p.resource = 'FOLLOW' THEN 1 ELSE 0 END) as 关注权限,
    SUM(CASE WHEN p.resource = 'CONTACT' THEN 1 ELSE 0 END) as 好友权限,
    SUM(CASE WHEN p.resource = 'GROUP' THEN 1 ELSE 0 END) as 群组权限,
    SUM(CASE WHEN p.resource = 'MESSAGE' THEN 1 ELSE 0 END) as 消息权限,
    SUM(CASE WHEN p.resource = 'USER' THEN 1 ELSE 0 END) as 用户权限,
    SUM(CASE WHEN p.resource = 'FILE' THEN 1 ELSE 0 END) as 文件权限,
    SUM(CASE WHEN p.resource = 'NOTIFICATION' THEN 1 ELSE 0 END) as 通知权限
FROM `role` r
LEFT JOIN `role_permission` rp ON r.id = rp.role_id
LEFT JOIN `permission` p ON rp.permission_id = p.id AND p.status = 1
WHERE r.name = 'USER'
GROUP BY r.id, r.name;

-- 4. 按资源类型列出权限详情
SELECT '=== 4. USER 角色权限详情（按资源分类） ===' as step;
SELECT 
    p.resource as 资源类型,
    p.action as 操作,
    p.condition as 条件,
    p.name as 权限名称,
    p.description as 描述
FROM `role` r
JOIN `role_permission` rp ON r.id = rp.role_id
JOIN `permission` p ON rp.permission_id = p.id
WHERE r.name = 'USER' AND p.status = 1
ORDER BY p.resource, p.action, p.condition;

-- 5. 检查关键权限是否存在
SELECT '=== 5. 关键权限检查 ===' as step;
SELECT 
    '文章创建' as 功能,
    CASE WHEN EXISTS (
        SELECT 1 FROM `role` r
        JOIN `role_permission` rp ON r.id = rp.role_id
        JOIN `permission` p ON rp.permission_id = p.id
        WHERE r.name = 'USER' AND p.name = 'ARTICLE_CREATE_OWN'
    ) THEN '✓ 已配置' ELSE '✗ 缺失' END as 状态
UNION ALL
SELECT '好友添加', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'CONTACT_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '好友同意', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'CONTACT_ACCEPT_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '用户搜索', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'USER_SEARCH_ANY'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '文章评论', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'COMMENT_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '文章点赞', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'LIKE_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '文章收藏', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'FAVORITE_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '关注用户', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'FOLLOW_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '群组创建', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'GROUP_CREATE_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END
UNION ALL
SELECT '群组加入', CASE WHEN EXISTS (
    SELECT 1 FROM `role` r JOIN `role_permission` rp ON r.id = rp.role_id
    JOIN `permission` p ON rp.permission_id = p.id
    WHERE r.name = 'USER' AND p.name = 'GROUP_JOIN_OWN'
) THEN '✓ 已配置' ELSE '✗ 缺失' END;

-- 6. 检查所有用户的角色分配情况
SELECT '=== 6. 所有用户角色分配情况 ===' as step;
SELECT 
    u.username as 用户名,
    u.type as 用户类型,
    GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') as 已分配角色,
    COUNT(DISTINCT rp.permission_id) as 权限总数
FROM `user` u
LEFT JOIN `user_role` ur ON u.id = ur.user_id
LEFT JOIN `role` r ON ur.role_id = r.id
LEFT JOIN `role_permission` rp ON r.id = rp.role_id
WHERE u.status = 1
GROUP BY u.id, u.username, u.type
ORDER BY u.id;

-- 7. 总结
SELECT '=== 7. 权限配置总结 ===' as step;
SELECT 
    (SELECT COUNT(*) FROM `permission` WHERE status = 1) as 系统权限总数,
    (SELECT COUNT(*) FROM `role` WHERE status = 1) as 角色总数,
    (SELECT COUNT(*) FROM `user` WHERE status = 1) as 用户总数,
    (SELECT COUNT(*) FROM `user_role`) as 用户角色关联数,
    (SELECT COUNT(*) FROM `role_permission`) as 角色权限关联数;
