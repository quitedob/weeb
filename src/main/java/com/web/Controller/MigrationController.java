package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.util.ApiResponseUtil;
import com.web.util.DatabaseMigrationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据库迁移控制器
 * 提供迁移执行和验证的API接口
 * 
 * 注意：此控制器仅用于迁移期间，生产环境中应该移除或限制访问
 */
@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    @Autowired
    private DatabaseMigrationUtil migrationUtil;

    /**
     * 验证迁移前的数据状态
     * @return 验证结果
     */
    @GetMapping("/validate/pre")
    public ResponseEntity<ApiResponse<DatabaseMigrationUtil.MigrationValidationReport>> validatePreMigration() {
        try {
            DatabaseMigrationUtil.MigrationValidationReport report = migrationUtil.validatePreMigration();

            if (report.isValid()) {
                return ApiResponseUtil.success(report, "Pre-migration validation passed");
            } else {
                return ApiResponseUtil.badRequestMigration("Pre-migration validation failed");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMigration(e, "Pre-migration validation");
        }
    }

    /**
     * 验证迁移后的数据状态
     * @return 验证结果
     */
    @GetMapping("/validate/post")
    public ResponseEntity<ApiResponse<DatabaseMigrationUtil.MigrationValidationReport>> validatePostMigration() {
        try {
            DatabaseMigrationUtil.MigrationValidationReport report = migrationUtil.validatePostMigration();

            if (report.isValid()) {
                return ApiResponseUtil.success(report, "Post-migration validation passed");
            } else {
                return ApiResponseUtil.badRequestMigration("Post-migration validation failed");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMigration(e, "Post-migration validation");
        }
    }

    /**
     * 为缺失统计数据的用户创建记录
     * @return 创建结果
     */
    @PostMapping("/create-missing-stats")
    public ResponseEntity<ApiResponse<String>> createMissingUserStats() {
        try {
            int createdCount = migrationUtil.createMissingUserStats();

            String message = String.format("Successfully created %d user stats records", createdCount);
            return ApiResponseUtil.successString(message);

        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "创建缺失用户统计数据");
        }
    }

    /**
     * 获取迁移状态概览
     * @return 迁移状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<MigrationStatus>> getMigrationStatus() {
        try {
            DatabaseMigrationUtil.MigrationValidationReport report = migrationUtil.validatePostMigration();

            MigrationStatus status = new MigrationStatus();
            status.setUserStatsTableExists(report.isUserStatsTableExists());
            status.setUserCount(report.getUserCount());
            status.setUserStatsCount(report.getUserStatsCount());
            status.setDataIntegrityValid(report.isValid());
            status.setErrorCount(report.getErrors().size());
            status.setWarningCount(report.getWarnings().size());

            // 确定整体迁移状态
            if (!report.isUserStatsTableExists()) {
                status.setOverallStatus("NOT_STARTED");
            } else if (!report.isValid()) {
                status.setOverallStatus("INCOMPLETE");
            } else {
                status.setOverallStatus("COMPLETED");
            }

            return ApiResponseUtil.success(status);

        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMigrationStatus(e, "获取迁移状态");
        }
    }

    /**
     * 迁移状态DTO
     */
    public static class MigrationStatus {
        private boolean userStatsTableExists;
        private Integer userCount;
        private Integer userStatsCount;
        private boolean dataIntegrityValid;
        private int errorCount;
        private int warningCount;
        private String overallStatus; // NOT_STARTED, IN_PROGRESS, COMPLETED, INCOMPLETE

        // Getters and setters
        public boolean isUserStatsTableExists() { return userStatsTableExists; }
        public void setUserStatsTableExists(boolean userStatsTableExists) { this.userStatsTableExists = userStatsTableExists; }

        public Integer getUserCount() { return userCount; }
        public void setUserCount(Integer userCount) { this.userCount = userCount; }

        public Integer getUserStatsCount() { return userStatsCount; }
        public void setUserStatsCount(Integer userStatsCount) { this.userStatsCount = userStatsCount; }

        public boolean isDataIntegrityValid() { return dataIntegrityValid; }
        public void setDataIntegrityValid(boolean dataIntegrityValid) { this.dataIntegrityValid = dataIntegrityValid; }

        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }

        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    }
}