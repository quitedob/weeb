-- JWT认证与文章API问题诊断SQL脚本
-- 用于验证数据库层面的用户认证和权限问题

-- 1. 验证用户表结构和数据
SELECT
    '=== 用户表结构验证 ===' as info;

-- 检查用户表是否存在
SHOW TABLES LIKE 'user';

-- 检查用户表结构
DESCRIBE `user`;

-- 2. 验证用户数据（包括密码是否已加密）
SELECT
    '=== 用户数据验证 ===' as info;

-- 检查用户数量
SELECT COUNT(*) as user_count FROM `user`;

-- 检查具体用户数据（密码应该是BCrypt加密格式，以$2a$开头）
SELECT
    id,
    username,
    password,
    status,
    created_at,
    updated_at,
    CASE
        WHEN password IS NULL OR password = '' THEN '密码为空'
        WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' OR password LIKE '$2x$%' OR password LIKE '$2y$%' THEN '密码已加密(BCrypt)'
        ELSE '密码可能未加密: ' || SUBSTRING(password, 1, 20) || '...'
    END as password_status
FROM `user`
WHERE username IS NOT NULL
ORDER BY id
LIMIT 10;

-- 3. 验证权限相关表
SELECT
    '=== 权限表验证 ===' as info;

-- 检查权限表是否存在
SHOW TABLES LIKE 'permission';
SHOW TABLES LIKE 'role';
SHOW TABLES LIKE 'user_role';
SHOW TABLES LIKE 'role_permission';

-- 检查权限数据
SELECT
    '权限数据:' as info,
    p.id,
    p.name,
    p.resource,
    p.action,
    p.condition,
    p.status
FROM permission p
WHERE p.status = 1
ORDER BY p.resource, p.action;

-- 检查用户角色关联
SELECT
    u.username,
    r.name as role_name,
    ur.user_id,
    ur.role_id
FROM `user` u
LEFT JOIN user_role ur ON u.id = ur.user_id
LEFT JOIN role r ON ur.role_id = r.id
WHERE u.id IN (
    SELECT DISTINCT user_id FROM (
        SELECT user_id FROM user_role
        UNION
        SELECT user_id FROM `user` WHERE status = 1
    ) as active_users
)
ORDER BY u.id;

-- 4. 验证文章表和所有者关系
SELECT
    '=== 文章表验证 ===' as info;

-- 检查文章表是否存在
SHOW TABLES LIKE 'articles';

-- 检查文章表结构
DESCRIBE articles;

-- 检查文章所有者关系
SELECT
    a.id as article_id,
    a.article_title,
    a.user_id,
    u.username as owner_username,
    a.status as article_status,
    a.created_at
FROM articles a
LEFT JOIN `user` u ON a.user_id = u.id
ORDER BY a.id DESC
LIMIT 10;

-- 5. 验证自定义用户详情服务相关查询
SELECT
    '=== CustomUserDetailsService查询验证 ===' as info;

-- 模拟AuthMapper.findByUsername查询
-- 注意：这里假设AuthMapper使用的是user表，如果不是，请调整表名
SELECT
    'findByUsername查询测试:' as test_info,
    u.id,
    u.username,
    u.password,
    u.status,
    CASE
        WHEN u.status = 0 THEN '用户已禁用'
        WHEN u.status = 1 THEN '用户正常'
        ELSE '未知状态: ' || COALESCE(u.status, 'NULL')
    END as user_status
FROM `user` u
WHERE u.username = 'admin'  -- 请替换为实际存在的用户名
LIMIT 1;

-- 6. 权限查询验证
SELECT
    '=== 权限查询验证 ===' as info;

-- 模拟CustomUserDetailsService.getUserPermissions查询
SELECT
    p.id,
    p.name,
    p.resource,
    p.action,
    p.condition,
    p.status
FROM permission p
INNER JOIN role_permission rp ON p.id = rp.permission_id
INNER JOIN user_role ur ON rp.role_id = ur.role_id
WHERE ur.user_id = 1  -- 请替换为实际存在的用户ID
  AND p.status = 1;

-- 7. 检查数据库连接和字符集
SELECT
    '=== 数据库配置验证 ===' as info;

SELECT
    @@hostname as server_name,
    @@port as port,
    DATABASE() as current_database,
    @@character_set_server as charset,
    @@collation_server as collation;

-- 8. 创建测试用户的SQL（如果需要）
SELECT
    '=== 测试用户创建SQL ===' as info,
    '如果需要创建测试用户，请执行以下SQL（请先注释掉其他查询）' as instruction;

-- 测试用户创建SQL（请根据需要取消注释并修改参数）
/*
-- 创建测试用户（密码是 "123456" 的BCrypt加密结果）
INSERT INTO `user` (username, password, sex, phone_number, user_email, status, created_at, updated_at)
VALUES ('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 1, '13800138000', 'test@example.com', 1, NOW(), NOW());

-- 为测试用户分配基本角色（假设存在user_role表和基础角色）
-- 假设存在id=2的USER角色
INSERT INTO user_role (user_id, role_id) VALUES (LAST_INSERT_ID(), 2);
*/

-- 9. 常见问题排查
SELECT
    '=== 常见问题排查 ===' as info;

-- 检查是否有重复用户名
SELECT
    username,
    COUNT(*) as count
FROM `user`
GROUP BY username
HAVING COUNT(*) > 1;

-- 检查是否有密码为空的用户
SELECT
    id,
    username,
    '密码为空' as issue
FROM `user`
WHERE password IS NULL OR password = '';

-- 检查是否有用户状态异常
SELECT
    id,
    username,
    status,
    CASE
        WHEN status IS NULL THEN '状态为NULL'
        WHEN status NOT IN (0, 1) THEN '状态值异常'
        ELSE '状态正常'
    END as status_check
FROM `user`
WHERE status IS NULL OR status NOT IN (0, 1);

-- 检查文章所有者是否存在
SELECT
    a.id as article_id,
    a.user_id,
    '文章所有者不存在' as issue
FROM articles a
LEFT JOIN `user` u ON a.user_id = u.id
WHERE u.id IS NULL;