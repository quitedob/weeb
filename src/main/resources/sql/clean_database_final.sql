-- ========================================
-- 最终数据库清理脚本
-- 移除权限系统和文件上传相关的表
-- ========================================

SET FOREIGN_KEY_CHECKS = 0;

-- 删除权限相关表
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role_permission`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `permission`;

-- 删除文件上传相关表
DROP TABLE IF EXISTS `file_share`;
DROP TABLE IF EXISTS `file_record`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 数据库清理完成' as status;
SELECT '已删除: 权限表(permission, role, role_permission, user_role)' as info;
SELECT '已删除: 文件表(file_record, file_share)' as info;
