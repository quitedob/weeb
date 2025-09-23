# Database Migration Execution Guide

## Overview
This guide provides step-by-step instructions for safely executing the database schema migration to separate user statistical data from the main user table.

## Prerequisites
1. **Backup**: Create a full database backup before starting
2. **Maintenance Window**: Schedule during low-traffic period
3. **Application Deployment**: Ensure new application code is ready to deploy
4. **Monitoring**: Have database monitoring tools ready

## Migration Steps

### Phase 1: Pre-Migration Validation
Execute these queries to understand current data state:

```sql
-- Check current user table structure
DESCRIBE user;

-- Count total users
SELECT COUNT(*) as total_users FROM user;

-- Check for any NULL values in statistical fields
SELECT 
    COUNT(*) as total_users,
    COUNT(fans_count) as non_null_fans,
    COUNT(total_likes) as non_null_likes,
    COUNT(website_coins) as non_null_coins
FROM user;

-- Sample current statistical data
SELECT 
    id, username, fans_count, total_likes, total_favorites, 
    total_sponsorship, total_article_exposure, website_coins
FROM user 
LIMIT 10;
```

### Phase 2: Execute Migration
Run the migration scripts in this exact order:

1. **Create user_stats table and migrate data**:
   ```bash
   mysql -u [username] -p [database_name] < migration_001_create_user_stats_table.sql
   ```

2. **Verify migration success**:
   ```sql
   -- Check that user_stats table was created
   DESCRIBE user_stats;
   
   -- Verify data was migrated correctly
   SELECT COUNT(*) as migrated_records FROM user_stats;
   
   -- Compare totals (should match)
   SELECT 
       (SELECT COUNT(*) FROM user) as user_count,
       (SELECT COUNT(*) FROM user_stats) as stats_count;
   
   -- Verify statistical data integrity
   SELECT 
       SUM(u.fans_count) as original_fans_total,
       SUM(us.fans_count) as migrated_fans_total,
       SUM(u.total_likes) as original_likes_total,
       SUM(us.total_likes) as migrated_likes_total
   FROM user u
   JOIN user_stats us ON u.id = us.user_id;
   ```

### Phase 3: Application Deployment
1. Deploy the new application code with dual-table support
2. Test critical user operations:
   - User registration
   - User profile updates
   - Statistical counter updates
   - User authentication

### Phase 4: Validation Period
Monitor the system for 24-48 hours to ensure:
- No application errors related to user operations
- Statistical updates are working correctly
- Performance improvements are evident
- No data inconsistencies

### Phase 5: Remove Old Columns (After Validation)
Only after confirming everything works correctly:

```bash
mysql -u [username] -p [database_name] < migration_002_remove_stats_from_user_table.sql
```

## Rollback Procedure
If issues are discovered, execute rollback:

```bash
mysql -u [username] -p [database_name] < rollback_001_drop_user_stats_table.sql
```

**Note**: Rollback is only possible before Phase 5 (column removal).

## Validation Queries

### Data Integrity Checks
```sql
-- Verify all users have stats records
SELECT COUNT(*) as users_without_stats
FROM user u 
LEFT JOIN user_stats us ON u.id = us.user_id 
WHERE us.user_id IS NULL;

-- Check for orphaned stats records
SELECT COUNT(*) as orphaned_stats
FROM user_stats us 
LEFT JOIN user u ON us.user_id = u.id 
WHERE u.id IS NULL;

-- Verify foreign key constraints
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_NAME = 'user_stats' 
AND CONSTRAINT_NAME = 'fk_user_stats_user_id';
```

### Performance Validation
```sql
-- Test JOIN performance
EXPLAIN SELECT u.username, us.fans_count, us.total_likes 
FROM user u 
JOIN user_stats us ON u.id = us.user_id 
WHERE u.id = 1;

-- Test concurrent update simulation
-- (Run in separate sessions to test lock contention)
-- Session 1:
START TRANSACTION;
UPDATE user SET nickname = 'test1' WHERE id = 1;
-- Don't commit yet

-- Session 2:
START TRANSACTION;
UPDATE user_stats SET fans_count = fans_count + 1 WHERE user_id = 1;
COMMIT;
-- This should not be blocked by Session 1

-- Session 1:
COMMIT;
```

## Monitoring Points
1. **Application Logs**: Watch for database-related errors
2. **Database Performance**: Monitor query execution times
3. **Lock Contention**: Check for reduced lock wait times
4. **User Operations**: Verify all user functions work correctly

## Success Criteria
- ✅ All users have corresponding user_stats records
- ✅ No orphaned records in either table
- ✅ Statistical data totals match pre-migration values
- ✅ Application functions normally with new schema
- ✅ Improved concurrent operation performance
- ✅ No increase in error rates

## Emergency Contacts
- Database Administrator: [Contact Info]
- Application Team Lead: [Contact Info]
- DevOps Engineer: [Contact Info]

## Post-Migration Tasks
1. Update monitoring dashboards for new table structure
2. Update backup procedures to include user_stats table
3. Update documentation with new schema
4. Schedule performance review after 1 week
5. Plan for old column removal after validation period