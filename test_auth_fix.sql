-- 验证注册登录修复效果的SQL脚本
-- 执行此脚本来验证数据库层面的修复

-- 1. 检查用户表是否有正确的结构
SELECT '=== 检查用户表结构 ===' as step;
DESCRIBE `user`;

-- 2. 检查是否需要修复用户状态字段
SELECT '=== 检查用户状态字段 ===' as step;
SELECT
    COUNT(*) as total_users,
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as disabled_users,
    SUM(CASE WHEN status IS NULL THEN 1 ELSE 0 END) as null_status_users
FROM `user`;

-- 3. 创建测试用户（如果不存在）
SELECT '=== 创建测试用户 ===' as step;

-- 先删除可能存在的测试用户
DELETE FROM `user` WHERE username IN ('testuser', 'testuser2');

-- 插入测试用户（密码是 "123456" 的BCrypt加密）
INSERT INTO `user` (
    username, password, sex, phone_number, user_email,
    registration_date, status, online_status, created_at, updated_at
) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 1, '13800138000', 'test@example.com', NOW(), 1, 0, NOW(), NOW()),
('testuser2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 0, '13800138001', 'test2@example.com', NOW(), 1, 0, NOW(), NOW());

-- 4. 验证用户是否创建成功
SELECT '=== 验证用户创建成功 ===' as step;
SELECT
    id,
    username,
    CASE
        WHEN password LIKE '$2a$%' THEN 'BCrypt加密 ✓'
        ELSE '密码格式错误 ✗'
    END as password_status,
    status,
    created_at
FROM `user`
WHERE username IN ('testuser', 'testuser2')
ORDER BY id;

-- 5. 测试查询验证（模拟UserMapper.findByUsername）
SELECT '=== 测试用户查询功能 ===' as step;
SELECT
    'findByUsername查询测试:' as test_info,
    u.id,
    u.username,
    u.password,
    u.status,
    CASE
        WHEN u.status = 0 THEN '用户已禁用'
        WHEN u.status = 1 THEN '用户正常'
        ELSE '未知状态'
    END as user_status
FROM `user` u
WHERE u.username = 'testuser';

-- 6. 验证JWT相关字段
SELECT '=== 验证JWT相关字段 ===' as step;
SELECT
    id,
    username,
    CASE
        WHEN password LIKE '$2a$%' THEN 'BCrypt密码✓'
        ELSE '密码错误✗'
    END as password_check,
    CASE
        WHEN status = 1 THEN '状态正常✓'
        ELSE '状态异常✗'
    END as status_check
FROM `user`
WHERE username IN ('testuser', 'testuser2');

-- 7. 如果有权限表，也验证权限数据
SELECT '=== 检查权限相关表 ===' as step;
SELECT
    TABLE_NAME as table_exists,
    TABLE_ROWS as row_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('permission', 'role', 'user_role', 'role_permission')
ORDER BY TABLE_NAME;

-- 8. 如果权限表存在，检查权限配置
SELECT '=== 检查权限配置（如果存在）===' as step;
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
ORDER BY p.resource, p.action
LIMIT 10;

-- 9. 测试insertUser方法所需的字段验证
SELECT '=== 验证insertUser所需字段 ===' as step;
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user'
  AND COLUMN_NAME IN ('username', 'password', 'status', 'created_at', 'updated_at')
ORDER BY COLUMN_NAME;

-- 10. 最终验证总结
SELECT '=== 修复验证总结 ===' as step;
SELECT
    '修复验证完成' as status,
    '请检查上述输出确认:' as instruction,
    '1. 用户表结构是否正确' as check1,
    '2. 测试用户是否创建成功' as check2,
    '3. 密码是否为BCrypt加密' as check3,
    '4. 用户状态是否为1（正常）' as check4,
    '5. 查询功能是否正常' as check5;