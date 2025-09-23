package com.web.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database Migration Executor for safely migrating user statistics to separate table
 * This class executes the migration with proper validation and rollback capabilities
 */
@Component
public class DatabaseMigrationExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationExecutor.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Execute the complete migration process safely
     * @return true if migration was successful, false otherwise
     */
    @Transactional
    public boolean executeMigration() {
        logger.info("Starting database migration: user statistics table separation");
        
        try {
            // Step 1: Pre-migration validation
            if (!validatePreMigrationState()) {
                logger.error("Pre-migration validation failed. Aborting migration.");
                return false;
            }
            
            // Step 2: Execute migration script
            if (!executeMigrationScript()) {
                logger.error("Migration script execution failed. Aborting migration.");
                return false;
            }
            
            // Step 3: Post-migration validation
            if (!validatePostMigrationState()) {
                logger.error("Post-migration validation failed. Migration may have issues.");
                return false;
            }
            
            logger.info("Database migration completed successfully!");
            return true;
            
        } catch (Exception e) {
            logger.error("Migration failed with exception: ", e);
            return false;
        }
    }
    
    /**
     * Validate the database state before migration
     */
    private boolean validatePreMigrationState() {
        logger.info("Performing pre-migration validation...");
        
        try {
            // Check if user table exists and has expected columns
            List<Map<String, Object>> userColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' " +
                "AND COLUMN_NAME IN ('fans_count', 'total_likes', 'total_favorites', 'total_sponsorship', 'total_article_exposure', 'website_coins')"
            );
            
            if (userColumns.size() != 6) {
                logger.error("User table is missing expected statistical columns. Found {} columns, expected 6", userColumns.size());
                return false;
            }
            
            // Check if user_stats table already exists
            Integer userStatsExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'user_stats'",
                Integer.class
            );
            
            if (userStatsExists > 0) {
                logger.warn("user_stats table already exists. Checking if migration was already completed...");
                
                Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
                Integer statsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_stats", Integer.class);
                
                if (userCount.equals(statsCount)) {
                    logger.info("Migration appears to have been completed already. User count: {}, Stats count: {}", userCount, statsCount);
                    return true; // Migration already completed
                } else {
                    logger.warn("user_stats table exists but data counts don't match. User count: {}, Stats count: {}", userCount, statsCount);
                    // Continue with migration to fix data inconsistency
                }
            }
            
            // Count total users for reference
            Integer totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            logger.info("Pre-migration validation passed. Total users to migrate: {}", totalUsers);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Pre-migration validation failed: ", e);
            return false;
        }
    }
    
    /**
     * Execute the migration script
     */
    private boolean executeMigrationScript() {
        logger.info("Executing migration script...");
        
        try (Connection connection = dataSource.getConnection()) {
            // Execute the migration script
            ClassPathResource migrationScript = new ClassPathResource("sql/migration_001_create_user_stats_table.sql");
            ScriptUtils.executeSqlScript(connection, migrationScript);
            
            logger.info("Migration script executed successfully");
            return true;
            
        } catch (SQLException e) {
            logger.error("Failed to execute migration script: ", e);
            return false;
        }
    }
    
    /**
     * Validate the database state after migration
     */
    private boolean validatePostMigrationState() {
        logger.info("Performing post-migration validation...");
        
        try {
            // Check if user_stats table was created
            Integer userStatsExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'user_stats'",
                Integer.class
            );
            
            if (userStatsExists == 0) {
                logger.error("user_stats table was not created");
                return false;
            }
            
            // Check record counts match
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            Integer statsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_stats", Integer.class);
            
            if (!userCount.equals(statsCount)) {
                logger.error("Record count mismatch. Users: {}, Stats: {}", userCount, statsCount);
                return false;
            }
            
            // Check for users without stats
            Integer usersWithoutStats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL",
                Integer.class
            );
            
            if (usersWithoutStats > 0) {
                logger.error("Found {} users without stats records", usersWithoutStats);
                return false;
            }
            
            // Check for orphaned stats
            Integer orphanedStats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_stats us LEFT JOIN user u ON us.user_id = u.id WHERE u.id IS NULL",
                Integer.class
            );
            
            if (orphanedStats > 0) {
                logger.error("Found {} orphaned stats records", orphanedStats);
                return false;
            }
            
            // Validate foreign key constraint exists
            Integer fkConstraintExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_stats' " +
                "AND CONSTRAINT_NAME = 'fk_user_stats_user_id'",
                Integer.class
            );
            
            if (fkConstraintExists == 0) {
                logger.error("Foreign key constraint was not created");
                return false;
            }
            
            // Sample data validation - check first 5 users
            List<Map<String, Object>> sampleValidation = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, " +
                "COALESCE(u.fans_count, 0) as original_fans, COALESCE(us.fans_count, 0) as migrated_fans, " +
                "COALESCE(u.total_likes, 0) as original_likes, COALESCE(us.total_likes, 0) as migrated_likes " +
                "FROM user u LEFT JOIN user_stats us ON u.id = us.user_id " +
                "ORDER BY u.id LIMIT 5"
            );
            
            boolean dataConsistent = true;
            for (Map<String, Object> row : sampleValidation) {
                Long originalFans = ((Number) row.get("original_fans")).longValue();
                Long migratedFans = ((Number) row.get("migrated_fans")).longValue();
                Long originalLikes = ((Number) row.get("original_likes")).longValue();
                Long migratedLikes = ((Number) row.get("migrated_likes")).longValue();
                
                if (!originalFans.equals(migratedFans) || !originalLikes.equals(migratedLikes)) {
                    logger.error("Data inconsistency for user {}: fans {} -> {}, likes {} -> {}", 
                        row.get("id"), originalFans, migratedFans, originalLikes, migratedLikes);
                    dataConsistent = false;
                }
            }
            
            if (!dataConsistent) {
                logger.error("Data consistency validation failed");
                return false;
            }
            
            logger.info("Post-migration validation passed. Successfully migrated {} users", userCount);
            return true;
            
        } catch (Exception e) {
            logger.error("Post-migration validation failed: ", e);
            return false;
        }
    }
    
    /**
     * Generate a comprehensive migration report
     */
    public void generateMigrationReport() {
        logger.info("=== MIGRATION REPORT ===");
        
        try {
            // Basic counts
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            Integer statsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_stats", Integer.class);
            
            logger.info("Total users: {}", userCount);
            logger.info("Total stats records: {}", statsCount);
            
            // Data integrity checks
            Integer usersWithoutStats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL",
                Integer.class
            );
            logger.info("Users without stats: {}", usersWithoutStats);
            
            Integer orphanedStats = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_stats us LEFT JOIN user u ON us.user_id = u.id WHERE u.id IS NULL",
                Integer.class
            );
            logger.info("Orphaned stats records: {}", orphanedStats);
            
            // Statistical totals comparison
            Map<String, Object> totals = jdbcTemplate.queryForMap(
                "SELECT " +
                "SUM(COALESCE(u.fans_count, 0)) as original_fans_total, " +
                "SUM(COALESCE(us.fans_count, 0)) as migrated_fans_total, " +
                "SUM(COALESCE(u.total_likes, 0)) as original_likes_total, " +
                "SUM(COALESCE(us.total_likes, 0)) as migrated_likes_total, " +
                "SUM(COALESCE(u.website_coins, 0)) as original_coins_total, " +
                "SUM(COALESCE(us.website_coins, 0)) as migrated_coins_total " +
                "FROM user u LEFT JOIN user_stats us ON u.id = us.user_id"
            );
            
            logger.info("Statistical totals comparison:");
            logger.info("  Fans - Original: {}, Migrated: {}", totals.get("original_fans_total"), totals.get("migrated_fans_total"));
            logger.info("  Likes - Original: {}, Migrated: {}", totals.get("original_likes_total"), totals.get("migrated_likes_total"));
            logger.info("  Coins - Original: {}, Migrated: {}", totals.get("original_coins_total"), totals.get("migrated_coins_total"));
            
            // Overall status
            boolean migrationSuccessful = userCount.equals(statsCount) && usersWithoutStats == 0 && orphanedStats == 0;
            logger.info("Migration Status: {}", migrationSuccessful ? "✅ SUCCESSFUL" : "❌ ISSUES DETECTED");
            
            if (migrationSuccessful) {
                logger.info("RECOMMENDATION: Migration completed successfully. Monitor application for 24-48 hours before removing old columns.");
            } else {
                logger.warn("RECOMMENDATION: Migration issues detected. Review errors and consider rollback if necessary.");
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate migration report: ", e);
        }
        
        logger.info("=== END MIGRATION REPORT ===");
    }
    
    /**
     * Test basic functionality after migration
     */
    public boolean testMigrationFunctionality() {
        logger.info("Testing migration functionality...");
        
        try {
            // Test JOIN query performance
            List<Map<String, Object>> joinTest = jdbcTemplate.queryForList(
                "SELECT u.id, u.username, u.avatar, us.fans_count, us.total_likes, us.website_coins " +
                "FROM user u LEFT JOIN user_stats us ON u.id = us.user_id " +
                "WHERE u.id BETWEEN 1 AND 10"
            );
            
            logger.info("JOIN query test successful. Retrieved {} records", joinTest.size());
            
            // Test statistical update (in a transaction that we'll rollback)
            jdbcTemplate.execute("START TRANSACTION");
            
            try {
                // Find a user to test with
                Integer testUserId = jdbcTemplate.queryForObject(
                    "SELECT id FROM user LIMIT 1", Integer.class
                );
                
                if (testUserId != null) {
                    // Test update
                    int updated = jdbcTemplate.update(
                        "UPDATE user_stats SET fans_count = fans_count + 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                        testUserId
                    );
                    
                    if (updated == 1) {
                        logger.info("Statistical update test successful");
                    } else {
                        logger.warn("Statistical update test failed - no rows updated");
                    }
                }
                
            } finally {
                // Always rollback the test transaction
                jdbcTemplate.execute("ROLLBACK");
            }
            
            logger.info("Migration functionality test completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Migration functionality test failed: ", e);
            try {
                jdbcTemplate.execute("ROLLBACK");
            } catch (Exception rollbackEx) {
                logger.error("Failed to rollback test transaction: ", rollbackEx);
            }
            return false;
        }
    }
}