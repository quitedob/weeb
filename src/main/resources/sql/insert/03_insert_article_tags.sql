-- 插入默认文章标签
-- 创建时间: 2025-11-10
-- 说明: 插入系统默认的文章标签，用于文章标记和搜索

INSERT IGNORE INTO article_tag (tag_name) VALUES
('Java'),
('Python'),
('JavaScript'),
('Vue'),
('React'),
('Spring Boot'),
('MySQL'),
('Redis'),
('Docker'),
('Linux'),
('算法'),
('数据结构'),
('设计模式'),
('前端'),
('后端'),
('全栈'),
('微服务'),
('分布式'),
('高并发');