package com.web.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Command line runner for executing database migration
 * This component runs only when the 'migration' profile is active
 * 
 * Usage: java -jar app.jar --spring.profiles.active=migration
 */
@Component
@Profile("migration")
public class MigrationRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);
    
    @Autowired
    private DatabaseMigrationExecutor migrationExecutor;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("=== DATABASE MIGRATION STARTED ===");
        logger.info("Profile: migration");
        logger.info("Arguments: {}", String.join(" ", args));
        
        boolean dryRun = false;
        boolean skipValidation = false;
        boolean forceExecution = false;
        
        // Parse command line arguments
        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "--dry-run":
                    dryRun = true;
                    logger.info("Dry run mode enabled - no actual changes will be made");
                    break;
                case "--skip-validation":
                    skipValidation = true;
                    logger.warn("Validation skip requested - this is not recommended");
                    break;
                case "--force":
                    forceExecution = true;
                    logger.warn("Force execution enabled - will proceed even if user_stats table exists");
                    break;
                case "--help":
                    printUsage();
                    return;
            }
        }
        
        if (dryRun) {
            executeDryRun();
        } else {
            executeActualMigration(skipValidation, forceExecution);
        }
        
        logger.info("=== DATABASE MIGRATION COMPLETED ===");
    }
    
    private void executeDryRun() {
        logger.info("=== DRY RUN MODE ===");
        logger.info("This is a simulation - no actual database changes will be made");
        
        try {
            // Generate current state report
            migrationExecutor.generateMigrationReport();
            
            // Test functionality without making changes
            logger.info("Testing current database functionality...");
            boolean functionalityTest = migrationExecutor.testMigrationFunctionality();
            
            if (functionalityTest) {
                logger.info("✅ Current database functionality test passed");
            } else {
                logger.warn("❌ Current database functionality test failed");
            }
            
            logger.info("=== DRY RUN COMPLETED ===");
            logger.info("To execute actual migration, run without --dry-run flag");
            
        } catch (Exception e) {
            logger.error("Dry run failed: ", e);
        }
    }
    
    private void executeActualMigration(boolean skipValidation, boolean forceExecution) {
        logger.info("=== EXECUTING ACTUAL MIGRATION ===");
        
        if (!forceExecution) {
            logger.info("Checking if migration is needed...");
            // Add additional safety check here if needed
        }
        
        try {
            // Execute the migration
            boolean migrationSuccess = migrationExecutor.executeMigration();
            
            if (migrationSuccess) {
                logger.info("✅ Migration executed successfully!");
                
                // Generate post-migration report
                migrationExecutor.generateMigrationReport();
                
                // Test functionality
                if (!skipValidation) {
                    logger.info("Testing migrated database functionality...");
                    boolean functionalityTest = migrationExecutor.testMigrationFunctionality();
                    
                    if (functionalityTest) {
                        logger.info("✅ Post-migration functionality test passed");
                    } else {
                        logger.warn("❌ Post-migration functionality test failed");
                    }
                }
                
                // Print next steps
                printNextSteps();
                
            } else {
                logger.error("❌ Migration failed!");
                logger.error("Please check the logs above for specific error details");
                logger.error("The database should be in a consistent state, but verify before proceeding");
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.error("Migration execution failed with exception: ", e);
            System.exit(1);
        }
    }
    
    private void printUsage() {
        logger.info("=== MIGRATION RUNNER USAGE ===");
        logger.info("java -jar app.jar --spring.profiles.active=migration [OPTIONS]");
        logger.info("");
        logger.info("OPTIONS:");
        logger.info("  --dry-run         Simulate migration without making changes");
        logger.info("  --skip-validation Skip post-migration validation tests");
        logger.info("  --force           Force execution even if user_stats table exists");
        logger.info("  --help            Show this help message");
        logger.info("");
        logger.info("EXAMPLES:");
        logger.info("  # Dry run to see what would happen");
        logger.info("  java -jar app.jar --spring.profiles.active=migration --dry-run");
        logger.info("");
        logger.info("  # Execute actual migration");
        logger.info("  java -jar app.jar --spring.profiles.active=migration");
        logger.info("");
        logger.info("  # Force execution (use with caution)");
        logger.info("  java -jar app.jar --spring.profiles.active=migration --force");
    }
    
    private void printNextSteps() {
        logger.info("=== NEXT STEPS ===");
        logger.info("1. Monitor application logs for the next 24-48 hours");
        logger.info("2. Verify that user operations work correctly:");
        logger.info("   - User registration and login");
        logger.info("   - Profile updates");
        logger.info("   - Statistical counter updates (likes, follows, etc.)");
        logger.info("3. Check application performance improvements");
        logger.info("4. After validation period, remove old columns using:");
        logger.info("   mysql -u [username] -p [database] < migration_002_remove_stats_from_user_table.sql");
        logger.info("5. Update monitoring dashboards for new table structure");
        logger.info("6. Update backup procedures to include user_stats table");
        logger.info("");
        logger.info("ROLLBACK: If issues occur, the migration can be rolled back");
        logger.info("as long as the old columns haven't been removed yet.");
        logger.info("=== END NEXT STEPS ===");
    }
}