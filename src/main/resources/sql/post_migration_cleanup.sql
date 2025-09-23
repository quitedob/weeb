-- Post-Migration Cleanup Script
-- This script should be run ONLY after the migration has been validated
-- and the application has been running successfully for at least 24-48 hours

-- WARNING: This script will permanently remove the old statistical columns
-- Ensure you have a backup and that the new system is working correctly

-- =============================================================================
-- SECTION 1: PRE-CLEANUP VALIDATION
-- =============================================================================

-- Verify that user_stats table is working correctly
SELECT 'Pre-cleanup validation starting...' as status;

-- Check that all users have stats
SELECT 
    COUNT(*) as users_without_stats,
    CASE 
        WHEN COUNT(*) = 0 THEN 'SAFE TO PROCEED'
        ELSE 'STOP - Users missing stats records'
    END as safety_check
FROM user u 
LEFT JOIN user_stats us ON u.id = us.user_id 
WHERE us.user_id IS NULL;

-- Verify recent activity in user_stats table
SELECT 
    COUNT(*) as recent_updates,
    MAX(updated_at) as last_update,
    CASE 
        WHEN MAX(updated_at) > DATE_SUB(NOW(), INTERVAL 1 HOUR) THEN 'ACTIVE - Safe to proceed'
        WHEN MAX(updated_at) > DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 'RECENT ACTIVITY - Probably safe'
        ELSE 'NO RECENT ACTIVITY - Verify system is working'
    END as activity_check
FROM user_stats 
WHERE updated_at > DATE_SUB(NOW(), INTERVAL 7 DAY);

-- =============================================================================
-- SECTION 2: BACKUP STATISTICAL DATA (SAFETY MEASURE)
-- =============================================================================

-- Create a backup table with current statistical data before cleanup
CREATE TABLE IF NOT EXISTS user_stats_backup_before_cleanup AS
SELECT 
    user_id,
    fans_count,
    total_likes,
    total_favorites,
    total_sponsorship,
    total_article_exposure,
    website_coins,
    created_at,
    updated_at,
    NOW() as backup_timestamp
FROM user_stats;

SELECT 
    COUNT(*) as backup_records,
    'Statistical data backed up to user_stats_backup_before_cleanup' as backup_status
FROM user_stats_backup_before_cleanup;

-- =============================================================================
-- SECTION 3: COLUMN REMOVAL (IRREVERSIBLE)
-- =============================================================================

-- IMPORTANT: Uncomment the following lines ONLY when you are absolutely sure
-- that the migration is successful and the application is working correctly

-- Remove statistical columns from user table
-- ALTER TABLE user DROP COLUMN fans_count;
-- ALTER TABLE user DROP COLUMN total_likes;
-- ALTER TABLE user DROP COLUMN total_favorites;
-- ALTER TABLE user DROP COLUMN total_sponsorship;
-- ALTER TABLE user DROP COLUMN total_article_exposure;
-- ALTER TABLE user DROP COLUMN website_coins;

SELECT 'Column removal commands are commented out for safety.' as safety_notice;
SELECT 'Uncomment the ALTER TABLE statements above when ready to proceed.' as instruction;

-- =============================================================================
-- SECTION 4: POST-CLEANUP VALIDATION
-- =============================================================================

-- Verify columns have been removed (uncomment after running ALTER TABLE commands)
-- SELECT 'Verifying column removal...' as status;
-- 
-- SELECT 
--     COLUMN_NAME,
--     DATA_TYPE
-- FROM information_schema.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() 
-- AND TABLE_NAME = 'user'
-- AND COLUMN_NAME IN ('fans_count', 'total_likes', 'total_favorites', 'total_sponsorship', 'total_article_exposure', 'website_coins');

-- =============================================================================
-- SECTION 5: OPTIMIZATION
-- =============================================================================

-- Optimize tables after structural changes
-- OPTIMIZE TABLE user;
-- OPTIMIZE TABLE user_stats;

-- Update table statistics
-- ANALYZE TABLE user;
-- ANALYZE TABLE user_stats;

SELECT 'Optimization commands are commented out.' as optimization_notice;
SELECT 'Uncomment OPTIMIZE and ANALYZE commands after column removal.' as optimization_instruction;

-- =============================================================================
-- SECTION 6: FINAL VERIFICATION
-- =============================================================================

-- Test that the application still works with the cleaned-up schema
SELECT 'Testing basic operations after cleanup...' as test_status;

-- Test JOIN query still works
SELECT 
    COUNT(*) as successful_joins,
    'JOIN operations working correctly' as join_status
FROM user u 
JOIN user_stats us ON u.id = us.user_id 
LIMIT 1000;

-- Verify indexes are still optimal
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('user', 'user_stats')
AND INDEX_NAME != 'PRIMARY'
ORDER BY TABLE_NAME, INDEX_NAME;

-- =============================================================================
-- SECTION 7: CLEANUP SUMMARY
-- =============================================================================

SELECT 'POST-MIGRATION CLEANUP SUMMARY' as summary_title;

SELECT 
    'Cleanup Status' as item,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'user' 
            AND COLUMN_NAME = 'fans_count'
        ) THEN 'PENDING - Statistical columns still exist in user table'
        ELSE 'COMPLETED - Statistical columns removed from user table'
    END as status;

SELECT 
    'Backup Status' as item,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_stats_backup_before_cleanup')
        THEN CONCAT('CREATED - ', (SELECT COUNT(*) FROM user_stats_backup_before_cleanup), ' records backed up')
        ELSE 'NOT CREATED'
    END as status;

SELECT 
    'System Status' as item,
    'Monitor application logs and performance for next 24 hours' as status;

-- Record cleanup completion
SELECT 
    'Cleanup script executed at:' as info,
    NOW() as cleanup_timestamp;

-- =============================================================================
-- MANUAL STEPS REMINDER
-- =============================================================================

SELECT '=== MANUAL STEPS REQUIRED ===' as reminder_title;
SELECT '1. Uncomment ALTER TABLE statements when ready to remove columns' as step_1;
SELECT '2. Uncomment OPTIMIZE and ANALYZE statements after column removal' as step_2;
SELECT '3. Update application documentation with new schema' as step_3;
SELECT '4. Update monitoring dashboards for new table structure' as step_4;
SELECT '5. Update backup procedures to include user_stats table' as step_5;
SELECT '6. Schedule performance review after 1 week' as step_6;