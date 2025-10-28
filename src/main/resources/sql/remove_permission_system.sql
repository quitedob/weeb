-- ========================================
-- 移除权限系统 - 删除所有权限相关的表
-- ========================================

-- 警告：此操作将删除所有权限、角色和用户角色关联数据
-- 请确保在执行前备份重要数据

SET FOREIGN_KEY_CHECKS = 0;

-- 删除关联表
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role_permission`;

-- 删除主表
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `permission`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 权限系统相关表已删除' as status;
