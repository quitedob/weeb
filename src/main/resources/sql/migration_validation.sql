-- Migration Validation Script
-- This script performs comprehensive validation of the user/user_stats migration
-- Run this script after executing the migration to ensure data integrity

-- =============================================================================
-- SECTION 1: BASIC STRUCTURE VALIDATION
-- =============================================================================

SELECT '=== BASIC STRUCTURE VALIDATION ===' as section;

-- Check if user_stats table exists
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ user_stats table exists'
        ELSE '❌ user_stats table missing'
    END as table_check
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name = 'user_stats';

-- Check user_stats table structure
SELECT '--- user_stats table structure ---' as info;
DESCRIBE user_stats;

-- Check foreign key constraint
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ Foreign key constraint exists'
        ELSE '❌ Foreign key constraint missing'
    END as fk_check
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'user_stats' 
AND CONSTRAINT_NAME = 'fk_user_stats_user_id';

-- =============================================================================
-- SECTION 2: DATA INTEGRITY VALIDATION
-- =============================================================================

SELECT '=== DATA INTEGRITY VALIDATION ===' as section;

-- Count comparison
SELECT 
    (SELECT COUNT(*) FROM user) as user_count,
    (SELECT COUNT(*) FROM user_stats) as stats_count,
    CASE 
        WHEN (SELECT COUNT(*) FROM user) = (SELECT COUNT(*) FROM user_stats) 
        THEN '✅ Record counts match'
        ELSE '❌ Record counts mismatch'
    END as count_check;

-- Check for users without stats
SELECT 
    COUNT(*) as users_without_stats,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ All users have stats'
        ELSE CONCAT('❌ ', COUNT(*), ' users missing stats')
    END as missing_stats_check
FROM user u 
LEFT JOIN user_stats us ON u.id = us.user_id 
WHERE us.user_id IS NULL;

-- Check for orphaned stats
SELECT 
    COUNT(*) as orphaned_stats,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ No orphaned stats records'
        ELSE CONCAT('❌ ', COUNT(*), ' orphaned stats records')
    END as orphaned_check
FROM user_stats us 
LEFT JOIN user u ON us.user_id = u.id 
WHERE u.id IS NULL;

-- =============================================================================
-- SECTION 3: DATA CONSISTENCY VALIDATION
-- =============================================================================

SELECT '=== DATA CONSISTENCY VALIDATION ===' as section;

-- Compare statistical totals (only if old columns still exist)
-- This query will fail if columns have been removed, which is expected
SELECT 
    'Statistical Data Comparison' as comparison_type,
    SUM(COALESCE(u.fans_count, 0)) as original_fans_total,
    SUM(COALESCE(us.fans_count, 0)) as migrated_fans_total,
    SUM(COALESCE(u.total_likes, 0)) as original_likes_total,
    SUM(COALESCE(us.total_likes, 0)) as migrated_likes_total,
    SUM(COALESCE(u.website_coins, 0)) as original_coins_total,
    SUM(COALESCE(us.website_coins, 0)) as migrated_coins_total
FROM user u
LEFT JOIN user_stats us ON u.id = us.user_id;

-- Sample data verification (first 5 users)
SELECT '--- Sample Data Verification (First 5 Users) ---' as info;
SELECT 
    u.id,
    u.username,
    COALESCE(u.fans_count, 0) as original_fans,
    COALESCE(us.fans_count, 0) as migrated_fans,
    COALESCE(u.total_likes, 0) as original_likes,
    COALESCE(us.total_likes, 0) as migrated_likes,
    CASE 
        WHEN COALESCE(u.fans_count, 0) = COALESCE(us.fans_count, 0) 
        AND COALESCE(u.total_likes, 0) = COALESCE(us.total_likes, 0)
        THEN '✅ Match'
        ELSE '❌ Mismatch'
    END as data_match
FROM user u
LEFT JOIN user_stats us ON u.id = us.user_id
ORDER BY u.id
LIMIT 5;

-- =============================================================================
-- SECTION 4: PERFORMANCE VALIDATION
-- =============================================================================

SELECT '=== PERFORMANCE VALIDATION ===' as section;

-- Test JOIN query performance
SELECT '--- JOIN Query Performance Test ---' as info;
EXPLAIN FORMAT=JSON
SELECT u.id, u.username, u.avatar, us.fans_count, us.total_likes, us.website_coins
FROM user u 
LEFT JOIN user_stats us ON u.id = us.user_id 
WHERE u.id BETWEEN 1 AND 100;

-- Index usage check
SELECT '--- Index Usage Check ---' as info;
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('user', 'user_stats')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- =============================================================================
-- SECTION 5: FUNCTIONAL VALIDATION
-- =============================================================================

SELECT '=== FUNCTIONAL VALIDATION ===' as section;

-- Test statistical operations
SELECT '--- Testing Statistical Operations ---' as info;

-- Create a test transaction to verify operations work
START TRANSACTION;

-- Test: Create stats for a test user (if not exists)
INSERT IGNORE INTO user_stats (user_id, fans_count, total_likes, total_favorites, total_sponsorship, total_article_exposure, website_coins)
SELECT id, 0, 0, 0, 0, 0, 0 
FROM user 
WHERE id = (SELECT MIN(id) FROM user)
AND id NOT IN (SELECT user_id FROM user_stats);

-- Test: Update stats
UPDATE user_stats 
SET fans_count = fans_count + 1, 
    total_likes = total_likes + 5,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = (SELECT MIN(id) FROM user)
LIMIT 1;

-- Test: Read combined data
SELECT 
    u.username,
    us.fans_count,
    us.total_likes,
    us.updated_at
FROM user u
JOIN user_stats us ON u.id = us.user_id
WHERE u.id = (SELECT MIN(id) FROM user);

-- Rollback test transaction
ROLLBACK;

SELECT '✅ Functional tests completed (transaction rolled back)' as test_result;

-- =============================================================================
-- SECTION 6: SUMMARY REPORT
-- =============================================================================

SELECT '=== MIGRATION VALIDATION SUMMARY ===' as section;

SELECT 
    'Migration Validation Summary' as report_type,
    (SELECT COUNT(*) FROM user) as total_users,
    (SELECT COUNT(*) FROM user_stats) as total_stats_records,
    (SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL) as missing_stats,
    (SELECT COUNT(*) FROM user_stats us LEFT JOIN user u ON us.user_id = u.id WHERE u.id IS NULL) as orphaned_stats,
    CASE 
        WHEN (SELECT COUNT(*) FROM user) = (SELECT COUNT(*) FROM user_stats)
        AND (SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL) = 0
        AND (SELECT COUNT(*) FROM user_stats us LEFT JOIN user u ON us.user_id = u.id WHERE u.id IS NULL) = 0
        THEN '✅ MIGRATION SUCCESSFUL'
        ELSE '❌ MIGRATION ISSUES DETECTED'
    END as overall_status;

-- Final recommendations
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM user) = (SELECT COUNT(*) FROM user_stats)
        AND (SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL) = 0
        THEN 'RECOMMENDATION: Migration appears successful. Monitor application for 24-48 hours before removing old columns.'
        ELSE 'RECOMMENDATION: Migration issues detected. Review errors above and consider rollback if necessary.'
    END as recommendation;

-- Show current timestamp for record keeping
SELECT 
    'Validation completed at:' as info,
    NOW() as validation_timestamp;