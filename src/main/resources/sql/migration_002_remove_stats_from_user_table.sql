-- Migration Script: Remove statistical columns from user table
-- This script should be executed AFTER validating that user_stats table is working correctly
-- and all application code has been updated to use the new schema

-- WARNING: This is a destructive operation. Ensure you have backups and that
-- all services are updated to use the user_stats table before running this script.

-- Step 1: Verify user_stats table has all expected data
SELECT 
    COUNT(*) as user_count,
    (SELECT COUNT(*) FROM user_stats) as stats_count,
    CASE 
        WHEN COUNT(*) = (SELECT COUNT(*) FROM user_stats) 
        THEN 'DATA_CONSISTENT' 
        ELSE 'DATA_MISMATCH' 
    END as data_status
FROM user;

-- Step 2: Remove statistical columns from user table
-- Note: MySQL doesn't support dropping multiple columns in one statement reliably
-- so we'll drop them one by one

ALTER TABLE `user` DROP COLUMN `fans_count`;
ALTER TABLE `user` DROP COLUMN `total_likes`;
ALTER TABLE `user` DROP COLUMN `total_favorites`;
ALTER TABLE `user` DROP COLUMN `total_sponsorship`;
ALTER TABLE `user` DROP COLUMN `total_article_exposure`;
ALTER TABLE `user` DROP COLUMN `website_coins`;

-- Step 3: Verify the columns have been removed
DESCRIBE `user`;

-- Step 4: Update the user table comment to reflect the change
ALTER TABLE `user` COMMENT='用户表（统计数据已迁移至user_stats表）';