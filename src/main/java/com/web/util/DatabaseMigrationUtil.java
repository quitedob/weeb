package com.web.util;

import com.web.mapper.UserStatsMapper;
import com.web.model.UserStats;
import com.web.service.UserStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 数据库迁移工具类
 * 提供程序化的迁移执行和验证功能
 */
@Component
public class DatabaseMigrationUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationUtil.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private UserStatsMapper userStatsMapper;

    /**
     * 验证迁移前的数据状态
     * @return 验证结果报告
     */
    public MigrationValidationReport validatePreMigration() {
        logger.info("Starting pre-migration validation...");
        
        MigrationValidationReport report = new MigrationValidationReport();
        
        try {
            // 检查用户表记录数
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            report.setUserCount(userCount);
            
            // 检查是否存在user_stats表
            Integer statsTableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_stats'", 
                Integer.class
            );
            report.setUserStatsTableExists(statsTableExists > 0);
            
            // 如果user_stats表存在，检查记录数
            if (report.isUserStatsTableExists()) {
                Integer statsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_stats", Integer.class);
                report.setUserStatsCount(statsCount);
            }
            
            // 检查统计字段的数据完整性
            validateStatisticalData(report);
            
            report.setValid(true);
            logger.info("Pre-migration validation completed successfully");
            
        } catch (Exception e) {
            logger.error("Pre-migration validation failed", e);
            report.setValid(false);
            report.addError("Pre-migration validation failed: " + e.getMessage());
        }
        
        return report;
    }

    /**
     * 验证迁移后的数据状态
     * @return 验证结果报告
     */
    public MigrationValidationReport validatePostMigration() {
        logger.info("Starting post-migration validation...");
        
        MigrationValidationReport report = new MigrationValidationReport();
        
        try {
            // 基本结构验证
            validateTableStructure(report);
            
            // 数据完整性验证
            validateDataIntegrity(report);
            
            // 外键约束验证
            validateForeignKeyConstraints(report);
            
            // 功能性验证
            validateFunctionality(report);
            
            report.setValid(report.getErrors().isEmpty());
            
            if (report.isValid()) {
                logger.info("Post-migration validation completed successfully");
            } else {
                logger.error("Post-migration validation failed with {} errors", report.getErrors().size());
            }
            
        } catch (Exception e) {
            logger.error("Post-migration validation failed", e);
            report.setValid(false);
            report.addError("Post-migration validation failed: " + e.getMessage());
        }
        
        return report;
    }

    /**
     * 为缺失统计数据的用户创建记录
     * @return 创建的记录数
     */
    @Transactional
    public int createMissingUserStats() {
        logger.info("Creating missing user stats records...");
        
        try {
            // 查找没有统计数据的用户
            List<Long> usersWithoutStats = jdbcTemplate.queryForList(
                "SELECT u.id FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL",
                Long.class
            );
            
            int createdCount = 0;
            for (Long userId : usersWithoutStats) {
                try {
                    userStatsService.createStatsForUser(userId);
                    createdCount++;
                } catch (Exception e) {
                    logger.warn("Failed to create stats for user {}: {}", userId, e.getMessage());
                }
            }
            
            logger.info("Created {} user stats records", createdCount);
            return createdCount;
            
        } catch (Exception e) {
            logger.error("Failed to create missing user stats", e);
            throw new RuntimeException("Failed to create missing user stats", e);
        }
    }

    /**
     * 验证表结构
     */
    private void validateTableStructure(MigrationValidationReport report) {
        // 检查user_stats表是否存在
        Integer statsTableExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_stats'",
            Integer.class
        );
        
        if (statsTableExists == 0) {
            report.addError("user_stats table does not exist");
            return;
        }
        
        report.setUserStatsTableExists(true);
        
        // 检查必要的列是否存在
        List<String> requiredColumns = List.of(
            "user_id", "fans_count", "total_likes", "total_favorites",
            "total_sponsorship", "total_article_exposure", "website_coins"
        );
        
        for (String column : requiredColumns) {
            Integer columnExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_stats' AND column_name = ?",
                Integer.class, column
            );
            
            if (columnExists == 0) {
                report.addError("Required column '" + column + "' missing from user_stats table");
            }
        }
    }

    /**
     * 验证数据完整性
     */
    private void validateDataIntegrity(MigrationValidationReport report) {
        // 检查记录数匹配
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
        Integer statsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_stats", Integer.class);
        
        report.setUserCount(userCount);
        report.setUserStatsCount(statsCount);
        
        if (!userCount.equals(statsCount)) {
            report.addError(String.format("Record count mismatch: %d users vs %d stats records", userCount, statsCount));
        }
        
        // 检查缺失的统计记录
        Integer missingStats = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user u LEFT JOIN user_stats us ON u.id = us.user_id WHERE us.user_id IS NULL",
            Integer.class
        );
        
        if (missingStats > 0) {
            report.addError(String.format("%d users are missing stats records", missingStats));
        }
        
        // 检查孤立的统计记录
        Integer orphanedStats = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_stats us LEFT JOIN user u ON us.user_id = u.id WHERE u.id IS NULL",
            Integer.class
        );
        
        if (orphanedStats > 0) {
            report.addError(String.format("%d orphaned stats records found", orphanedStats));
        }
    }

    /**
     * 验证外键约束
     */
    private void validateForeignKeyConstraints(MigrationValidationReport report) {
        Integer fkExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_stats' AND CONSTRAINT_NAME = 'fk_user_stats_user_id'",
            Integer.class
        );
        
        if (fkExists == 0) {
            report.addError("Foreign key constraint 'fk_user_stats_user_id' is missing");
        }
    }

    /**
     * 验证功能性
     */
    private void validateFunctionality(MigrationValidationReport report) {
        try {
            // 测试JOIN查询
            Integer joinResult = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user u JOIN user_stats us ON u.id = us.user_id LIMIT 100",
                Integer.class
            );
            
            if (joinResult == 0) {
                report.addError("JOIN query between user and user_stats returned no results");
            }
            
            // 测试统计服务功能
            List<Long> sampleUserIds = jdbcTemplate.queryForList(
                "SELECT id FROM user LIMIT 3",
                Long.class
            );
            
            for (Long userId : sampleUserIds) {
                UserStats stats = userStatsService.getStatsByUserId(userId);
                if (stats == null) {
                    report.addError("UserStatsService failed to retrieve stats for user " + userId);
                }
            }
            
        } catch (Exception e) {
            report.addError("Functionality validation failed: " + e.getMessage());
        }
    }

    /**
     * 验证统计数据
     */
    private void validateStatisticalData(MigrationValidationReport report) {
        try {
            // 检查统计字段是否有NULL值
            Map<String, Object> nullCounts = jdbcTemplate.queryForMap(
                "SELECT " +
                "SUM(CASE WHEN fans_count IS NULL THEN 1 ELSE 0 END) as null_fans, " +
                "SUM(CASE WHEN total_likes IS NULL THEN 1 ELSE 0 END) as null_likes, " +
                "SUM(CASE WHEN website_coins IS NULL THEN 1 ELSE 0 END) as null_coins " +
                "FROM user"
            );
            
            for (Map.Entry<String, Object> entry : nullCounts.entrySet()) {
                Long nullCount = ((Number) entry.getValue()).longValue();
                if (nullCount > 0) {
                    report.addWarning(String.format("%d records have NULL values in %s", nullCount, entry.getKey()));
                }
            }
            
        } catch (Exception e) {
            // 如果统计字段已被删除，这是正常的
            logger.info("Statistical fields may have been removed from user table: {}", e.getMessage());
        }
    }

    /**
     * 迁移验证报告类
     */
    public static class MigrationValidationReport {
        private boolean valid = false;
        private boolean userStatsTableExists = false;
        private Integer userCount = 0;
        private Integer userStatsCount = 0;
        private List<String> errors = new java.util.ArrayList<>();
        private List<String> warnings = new java.util.ArrayList<>();

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public boolean isUserStatsTableExists() { return userStatsTableExists; }
        public void setUserStatsTableExists(boolean userStatsTableExists) { this.userStatsTableExists = userStatsTableExists; }

        public Integer getUserCount() { return userCount; }
        public void setUserCount(Integer userCount) { this.userCount = userCount; }

        public Integer getUserStatsCount() { return userStatsCount; }
        public void setUserStatsCount(Integer userStatsCount) { this.userStatsCount = userStatsCount; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Migration Validation Report:\n");
            sb.append("Valid: ").append(valid).append("\n");
            sb.append("User Count: ").append(userCount).append("\n");
            sb.append("User Stats Count: ").append(userStatsCount).append("\n");
            sb.append("User Stats Table Exists: ").append(userStatsTableExists).append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}