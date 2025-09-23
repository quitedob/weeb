-- Rollback Script: Remove user_stats table and restore statistical data to user table
-- This script reverses the migration in case of issues
-- WARNING: Only run this if the migration needs to be rolled back

-- Step 1: Verify that user_stats table exists and has data
SELECT COUNT(*) as user_stats_count FROM `user_stats`;

-- Step 2: Update user table with current statistical data from user_stats
-- (Only if statistical columns still exist in user table)
UPDATE `user` u
INNER JOIN `user_stats` us ON u.id = us.user_id
SET 
    u.fans_count = us.fans_count,
    u.total_likes = us.total_likes,
    u.total_favorites = us.total_favorites,
    u.total_sponsorship = us.total_sponsorship,
    u.total_article_exposure = us.total_article_exposure,
    u.website_coins = us.website_coins;

-- Step 3: Drop the user_stats table
DROP TABLE IF EXISTS `user_stats`;

-- Step 4: Verification query
-- This should return the same count as the original user table
SELECT COUNT(*) as user_count FROM `user`;