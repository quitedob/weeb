-- 插入默认用户数据
-- 创建时间: 2025-11-10
-- 说明: 插入系统管理员和测试用户，包含BCrypt加密的密码

-- 检查并插入管理员用户 (用户名: admin, 密码: admin123)
INSERT IGNORE INTO user (username, password, user_email, nickname, type, online_status, status, registration_date)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKV6biekKXsE6X5w5R2fK5U2gO6', 'admin@weeb.com', '系统管理员', 'ADMIN', 0, 1, NOW());

-- 检查并插入测试用户 (用户名: testuser, 密码: test123)
INSERT IGNORE INTO user (username, password, user_email, nickname, type, online_status, status)
VALUES ('testuser', '$2a$10$T1q2gCReMhlPLkB4zBuZC.bIVAbBl/BpHBvurrI2NQkBqNHuHeiYe', 'test@weeb.com', '测试用户', 'USER', 1, 1);

-- 插入更多测试用户 (密码: password + 数字)
INSERT IGNORE INTO user (username, password, user_email, nickname, type, online_status, status)
VALUES
('alice', '$2a$10$hbTBtOr3hqz4NV7xItTtluXdgtH/XSmu2Sa3BGMex6BiP/PlQ/lZ2', 'alice@weeb.com', '爱丽丝', 'USER', 1, 1),
('bob', '$2a$10$HeB5dudFSSMQ6ICVunOlHuVoBFydle0x3tIAj9xMTKlvsNGZ2zbO6', 'bob@weeb.com', '鲍勃', 'USER', 1, 1),
('charlie', '$2a$10$q.3Zrap1HhMeNrarmTTWduJZNQ6mtWY8pe6k7uzkDmAo6ZRy6pbya', 'charlie@weeb.com', '查理', 'USER', 1, 1),
('diana', '$2a$10$vyksYpUq50fq1tD774bvMesIZsrPn43UMMfAZwIlEEpVvumvCtKwa', 'diana@weeb.com', '戴安娜', 'USER', 1, 1),
('eve', '$2a$10$bk5/XQfztGfK/1zuXNqKIOWxbbo1uwZFMh9Ws7/yIVZVwFpeiscgW', 'eve@weeb.com', '伊芙', 'USER', 1, 1);