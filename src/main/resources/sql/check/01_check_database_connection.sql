-- 数据库连接检查
-- 创建时间: 2025-11-10
-- 说明: 检查数据库连接是否正常

-- 检查数据库连接状态
SELECT 1 as connection_test;

-- 检查数据库版本信息
SELECT VERSION() as mysql_version;

-- 检查当前数据库名称
SELECT DATABASE() as current_database;

-- 检查字符集设置
SHOW VARIABLES LIKE 'character_set%';

-- 检查排序规则设置
SHOW VARIABLES LIKE 'collation%';