-- 调试 testuser 的文章创建权限

-- 1. 检查 testuser 的基本信息
SELECT '=== 1. testuser 基本信息 ===' as step;
SELECT id, username, type, status FROM user WHERE username = 'testuser';

-- 2. 检查 testuser 的角色
SELECT '=== 2. testuser 的角色 ===' as step;
SELECT 
    u.id as user_id,
    u.username,
    r.id as role_id,
    r.name as role_name,
    r.status as role_status
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
WHERE u.username = 'testuser';

-- 3. 检查 ARTICLE_CREATE_OWN 权限是否存在
SELECT '=== 3. ARTICLE_CREATE_OWN 权限 ===' as step;
SELECT 
    id,
    name,
    resource,
    action,
    `condition`,
    status,
    description
FROM permission
WHERE name = 'ARTICLE_CREATE_OWN';

-- 4. 检查 USER 角色是否有 ARTICLE_CREATE_OWN 权限
SELECT '=== 4. USER 角色的 ARTICLE_CREATE_OWN 权限 ===' as step;
SELECT 
    r.id as role_id,
    r.name as role_name,
    p.id as permission_id,
    p.name as permission_name,
    p.resource,
    p.action,
    p.`condition`,
    rp.created_at
FROM role r
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name = 'USER' 
AND p.name = 'ARTICLE_CREATE_OWN';

-- 5. 检查 testuser 通过角色获得的所有 ARTICLE 相关权限
SELECT '=== 5. testuser 的所有 ARTICLE 权限 ===' as step;
SELECT 
    p.name as permission_name,
    p.resource,
    p.action,
    p.`condition`,
    p.status,
    p.description
FROM user u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'testuser'
AND p.resource = 'ARTICLE'
ORDER BY p.name;

-- 6. 统计 USER 角色的所有权限
SELECT '=== 6. USER 角色权限统计 ===' as step;
SELECT 
    COUNT(*) as total_permissions,
    SUM(CASE WHEN p.name LIKE '%_OWN' THEN 1 ELSE 0 END) as own_permissions,
    SUM(CASE WHEN p.name LIKE '%_ANY' THEN 1 ELSE 0 END) as any_permissions,
    SUM(CASE WHEN p.resource = 'ARTICLE' THEN 1 ELSE 0 END) as article_permissions
FROM role r
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name = 'USER' AND p.status = 1;

-- 7. 列出所有 _OWN 权限
SELECT '=== 7. 所有 _OWN 权限 ===' as step;
SELECT 
    p.id,
    p.name,
    p.resource,
    p.action,
    p.`condition`,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM role_permission rp 
            JOIN role r ON rp.role_id = r.id 
            WHERE r.name = 'USER' AND rp.permission_id = p.id
        ) THEN '✓ 已分配给USER'
        ELSE '✗ 未分配'
    END as assignment_status
FROM permission p
WHERE p.name LIKE '%_OWN'
AND p.status = 1
ORDER BY p.resource, p.action;
