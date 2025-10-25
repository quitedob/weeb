-- ============================================
-- 修复普通用户权限配置
-- 执行时间：2025-10-25
-- 目的：为USER角色添加缺失的基础权限
-- ============================================

-- 1. 确保USER角色存在
INSERT INTO role (name, description, status, type, level, is_default, created_at, updated_at)
VALUES ('USER', '普通用户', 1, 'SYSTEM', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. 插入联系人相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('CONTACT_CREATE_OWN', '申请添加好友', 'CONTACT', 'CREATE_OWN', 1, 'USER', 'CONTACT', 100, NOW(), NOW()),
('CONTACT_READ_OWN', '查看自己的联系人列表', 'CONTACT', 'READ_OWN', 1, 'USER', 'CONTACT', 101, NOW(), NOW()),
('CONTACT_UPDATE_OWN', '更新自己的联系人信息', 'CONTACT', 'UPDATE_OWN', 1, 'USER', 'CONTACT', 102, NOW(), NOW()),
('CONTACT_DELETE_OWN', '删除自己的联系人', 'CONTACT', 'DELETE_OWN', 1, 'USER', 'CONTACT', 103, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 3. 插入文章相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('ARTICLE_CREATE_OWN', '创建自己的文章', 'ARTICLE', 'CREATE_OWN', 1, 'USER', 'ARTICLE', 200, NOW(), NOW()),
('ARTICLE_READ_OWN', '读取自己的文章', 'ARTICLE', 'READ_OWN', 1, 'USER', 'ARTICLE', 201, NOW(), NOW()),
('ARTICLE_UPDATE_OWN', '更新自己的文章', 'ARTICLE', 'UPDATE_OWN', 1, 'USER', 'ARTICLE', 202, NOW(), NOW()),
('ARTICLE_DELETE_OWN_USER', '删除自己的文章', 'ARTICLE', 'DELETE_OWN', 1, 'USER', 'ARTICLE', 203, NOW(), NOW()),
('ARTICLE_FAVORITE_OWN', '收藏文章', 'ARTICLE', 'FAVORITE_OWN', 1, 'USER', 'ARTICLE', 204, NOW(), NOW()),
('ARTICLE_COMMENT_OWN', '评论文章', 'ARTICLE', 'COMMENT_OWN', 1, 'USER', 'ARTICLE', 205, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4. 插入消息相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('MESSAGE_CREATE_OWN', '发送消息', 'MESSAGE', 'CREATE_OWN', 1, 'USER', 'MESSAGE', 300, NOW(), NOW()),
('MESSAGE_READ_OWN', '读取自己的消息', 'MESSAGE', 'READ_OWN', 1, 'USER', 'MESSAGE', 301, NOW(), NOW()),
('MESSAGE_UPDATE_OWN', '更新自己的消息', 'MESSAGE', 'UPDATE_OWN', 1, 'USER', 'MESSAGE', 302, NOW(), NOW()),
('MESSAGE_DELETE_OWN_USER', '删除自己的消息', 'MESSAGE', 'DELETE_OWN', 1, 'USER', 'MESSAGE', 303, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 5. 插入群组相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('GROUP_CREATE_OWN', '创建群组', 'GROUP', 'CREATE_OWN', 1, 'USER', 'GROUP', 400, NOW(), NOW()),
('GROUP_READ_OWN', '查看自己的群组', 'GROUP', 'READ_OWN', 1, 'USER', 'GROUP', 401, NOW(), NOW()),
('GROUP_UPDATE_OWN', '更新自己的群组', 'GROUP', 'UPDATE_OWN', 1, 'USER', 'GROUP', 402, NOW(), NOW()),
('GROUP_JOIN_OWN', '加入群组', 'GROUP', 'JOIN_OWN', 1, 'USER', 'GROUP', 403, NOW(), NOW()),
('GROUP_LEAVE_OWN', '退出群组', 'GROUP', 'LEAVE_OWN', 1, 'USER', 'GROUP', 404, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 6. 插入用户自有权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('USER_READ_OWN', '查看自己的信息', 'USER', 'READ_OWN', 1, 'USER', 'USER', 500, NOW(), NOW()),
('USER_UPDATE_OWN', '更新自己的信息', 'USER', 'UPDATE_OWN', 1, 'USER', 'USER', 501, NOW(), NOW()),
('USER_EDIT_PROFILE_OWN', '编辑自己的资料', 'USER', 'EDIT_PROFILE_OWN', 1, 'USER', 'USER', 502, NOW(), NOW()),
('USER_SETTINGS_OWN', '管理自己的设置', 'USER', 'SETTINGS_OWN', 1, 'USER', 'USER', 503, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 7. 插入搜索相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('SEARCH_BASIC', '基础搜索', 'SEARCH', 'BASIC', 1, 'USER', 'SEARCH', 600, NOW(), NOW()),
('SEARCH_USER_BASIC', '搜索用户', 'SEARCH', 'USER_BASIC', 1, 'USER', 'SEARCH', 601, NOW(), NOW()),
('SEARCH_CONTENT_BASIC', '搜索内容', 'SEARCH', 'CONTENT_BASIC', 1, 'USER', 'SEARCH', 602, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 8. 插入关注相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('FOLLOW_CREATE_OWN', '关注用户', 'FOLLOW', 'CREATE_OWN', 1, 'USER', 'FOLLOW', 700, NOW(), NOW()),
('FOLLOW_READ_OWN', '查看关注列表', 'FOLLOW', 'READ_OWN', 1, 'USER', 'FOLLOW', 701, NOW(), NOW()),
('FOLLOW_DELETE_OWN', '取消关注', 'FOLLOW', 'DELETE_OWN', 1, 'USER', 'FOLLOW', 702, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 9. 插入文件相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('FILE_CREATE_OWN', '上传文件', 'FILE', 'CREATE_OWN', 1, 'USER', 'FILE', 800, NOW(), NOW()),
('FILE_READ_OWN', '查看自己的文件', 'FILE', 'READ_OWN', 1, 'USER', 'FILE', 801, NOW(), NOW()),
('FILE_UPDATE_OWN', '更新自己的文件', 'FILE', 'UPDATE_OWN', 1, 'USER', 'FILE', 802, NOW(), NOW()),
('FILE_DELETE_OWN', '删除自己的文件', 'FILE', 'DELETE_OWN', 1, 'USER', 'FILE', 803, NOW(), NOW()),
('FILE_UPLOAD_OWN', '上传文件', 'FILE', 'UPLOAD_OWN', 1, 'USER', 'FILE', 804, NOW(), NOW()),
('FILE_SHARE_OWN', '分享文件', 'FILE', 'SHARE_OWN', 1, 'USER', 'FILE', 805, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 10. 插入认证相关权限（如果不存在）
INSERT INTO permission (name, description, resource, action, status, type, `group`, sort_order, created_at, updated_at)
VALUES 
('AUTH_LOGIN_OWN', '登录', 'AUTH', 'LOGIN_OWN', 1, 'USER', 'AUTH', 900, NOW(), NOW()),
('AUTH_LOGOUT_OWN', '登出', 'AUTH', 'LOGOUT_OWN', 1, 'USER', 'AUTH', 901, NOW(), NOW()),
('AUTH_PASSWORD_CHANGE_OWN', '修改密码', 'AUTH', 'PASSWORD_CHANGE_OWN', 1, 'USER', 'AUTH', 902, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 11. 为USER角色关联所有基础权限
INSERT INTO role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM role r, permission p
WHERE r.name = 'USER'
AND p.name IN (
    -- 联系人权限
    'CONTACT_CREATE_OWN', 'CONTACT_READ_OWN', 'CONTACT_UPDATE_OWN', 'CONTACT_DELETE_OWN',
    -- 文章权限
    'ARTICLE_CREATE_OWN', 'ARTICLE_READ_OWN', 'ARTICLE_UPDATE_OWN', 'ARTICLE_DELETE_OWN_USER',
    'ARTICLE_FAVORITE_OWN', 'ARTICLE_COMMENT_OWN',
    -- 消息权限
    'MESSAGE_CREATE_OWN', 'MESSAGE_READ_OWN', 'MESSAGE_UPDATE_OWN', 'MESSAGE_DELETE_OWN_USER',
    -- 群组权限
    'GROUP_CREATE_OWN', 'GROUP_READ_OWN', 'GROUP_UPDATE_OWN', 'GROUP_JOIN_OWN', 'GROUP_LEAVE_OWN',
    -- 用户权限
    'USER_READ_OWN', 'USER_UPDATE_OWN', 'USER_EDIT_PROFILE_OWN', 'USER_SETTINGS_OWN',
    -- 搜索权限
    'SEARCH_BASIC', 'SEARCH_USER_BASIC', 'SEARCH_CONTENT_BASIC',
    -- 关注权限
    'FOLLOW_CREATE_OWN', 'FOLLOW_READ_OWN', 'FOLLOW_DELETE_OWN',
    -- 文件权限
    'FILE_CREATE_OWN', 'FILE_READ_OWN', 'FILE_UPDATE_OWN', 'FILE_DELETE_OWN', 
    'FILE_UPLOAD_OWN', 'FILE_SHARE_OWN',
    -- 认证权限
    'AUTH_LOGIN_OWN', 'AUTH_LOGOUT_OWN', 'AUTH_PASSWORD_CHANGE_OWN'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2
    WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
);

-- 12. 验证结果
SELECT 
    '权限配置完成' as status,
    COUNT(*) as total_permissions
FROM role_permission rp
JOIN role r ON rp.role_id = r.id
WHERE r.name = 'USER';

-- 13. 显示USER角色的所有权限
SELECT 
    r.name as role_name,
    p.name as permission_name,
    p.description as permission_description,
    p.resource,
    p.action
FROM role r
JOIN role_permission rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name = 'USER'
ORDER BY p.`group`, p.sort_order;

-- ============================================
-- 执行说明：
-- 1. 备份数据库：mysqldump -u root -p weeb > weeb_backup_$(date +%Y%m%d).sql
-- 2. 执行脚本：mysql -u root -p weeb < fix_user_permissions.sql
-- 3. 重启应用以刷新权限缓存
-- 4. 测试功能：登录普通用户，尝试发布文章和添加好友
-- ============================================
