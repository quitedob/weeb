package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.model.SystemLog;
import com.web.model.User;
import com.web.annotation.AdminLog;
import com.web.service.PermissionService;
import com.web.service.RBACService;
import com.web.service.RoleService;
import com.web.service.UserService;
import com.web.service.LogService;
import com.web.service.RedisCacheService;
import com.web.service.ElasticsearchSearchService;
import com.web.service.ArticleService;
import com.web.util.ValidationUtils;
import com.web.vo.admin.ArticleRejectRequestVo;
import com.web.vo.admin.ArticleAdminDeleteRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理员管理控制器
 * 提供系统管理相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RBACService rbacService;

    @Autowired
    private UserService userService;

    @Autowired
    private LogService logService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ElasticsearchSearchService elasticsearchSearchService;

    @Autowired
    private ArticleService articleService;

    /**
     * 获取权限管理页面数据
     * @return 权限列表和统计信息
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPermissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String status) {

        Map<String, Object> data = permissionService.getPermissionsWithPaging(page, pageSize, keyword, resource, status);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建权限
     * @param permission 权限对象
     * @return 创建结果
     */
    @PostMapping("/permissions")
    @AdminLog(action = "CREATE_PERMISSION")
    public ResponseEntity<ApiResponse<Permission>> createPermission(@Valid @RequestBody Permission permission) {
        Permission created = permissionService.createPermission(permission);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * 更新权限
     * @param permissionId 权限ID
     * @param permission 权限对象
     * @return 更新结果
     */
    @PutMapping("/permissions/{permissionId}")
    @AdminLog(action = "UPDATE_PERMISSION")
    public ResponseEntity<ApiResponse<Permission>> updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody Permission permission) {

        permission.setId(permissionId);
        Permission updated = permissionService.updatePermission(permission);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 删除权限
     * @param permissionId 权限ID
     * @return 删除结果
     */
    @DeleteMapping("/permissions/{permissionId}")
    @AdminLog(action = "DELETE_PERMISSION")
    public ResponseEntity<ApiResponse<Boolean>> deletePermission(@PathVariable Long permissionId) {
        boolean deleted = permissionService.deletePermission(permissionId);
        return ResponseEntity.ok(ApiResponse.success(deleted));
    }

    /**
     * 获取角色管理页面数据
     * @return 角色列表和统计信息
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        Map<String, Object> data = roleService.getRolesWithPaging(page, pageSize, keyword, status);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建角色
     * @param role 角色对象
     * @return 创建结果
     */
    @PostMapping("/roles")
    @AdminLog(action = "CREATE_ROLE")
    public ResponseEntity<ApiResponse<Role>> createRole(@Valid @RequestBody Role role) {
        Role created = roleService.createRole(role);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * 更新角色
     * @param roleId 角色ID
     * @param role 角色对象
     * @return 更新结果
     */
    @PutMapping("/roles/{roleId}")
    @AdminLog(action = "UPDATE_ROLE")
    public ResponseEntity<ApiResponse<Role>> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody Role role) {

        role.setId(roleId);
        Role updated = roleService.updateRole(role);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 删除角色
     * @param roleId 角色ID
     * @return 删除结果
     */
    @DeleteMapping("/roles/{roleId}")
    @AdminLog(action = "DELETE_ROLE")
    public ResponseEntity<ApiResponse<Boolean>> deleteRole(@PathVariable Long roleId) {
        boolean deleted = roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success(deleted));
    }

    /**
     * 为角色分配权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 分配结果
     */
    @PostMapping("/roles/{roleId}/permissions")
    @AdminLog(action = "ASSIGN_PERMISSIONS_TO_ROLE")
    public ResponseEntity<ApiResponse<Boolean>> assignPermissionsToRole(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds) {

        boolean assigned = roleService.assignPermissionsToRole(roleId, permissionIds);
        return ResponseEntity.ok(ApiResponse.success(assigned));
    }

    /**
     * 从角色移除权限
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 移除结果
     */
    @DeleteMapping("/roles/{roleId}/permissions")
    @AdminLog(action = "REMOVE_PERMISSIONS_FROM_ROLE")
    public ResponseEntity<ApiResponse<Boolean>> removePermissionsFromRole(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds) {

        boolean removed = roleService.removePermissionsFromRole(roleId, permissionIds);
        return ResponseEntity.ok(ApiResponse.success(removed));
    }

    /**
     * 获取角色权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getRolePermissions(@PathVariable Long roleId) {
        List<String> permissions = roleService.getRolePermissions(roleId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    /**
     * 初始化系统权限
     * @return 初始化结果
     */
    @PostMapping("/initialize-permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializePermissions() {
        Map<String, Object> result = permissionService.initializeSystemPermissions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 初始化系统角色
     * @return 初始化结果
     */
    @PostMapping("/initialize-roles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeRoles() {
        Map<String, Object> result = roleService.initializeSystemRoles();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取用户管理页面数据
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词
     * @param status 用户状态过滤（active, banned, all）
     * @return 用户列表和统计信息
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        Map<String, Object> data = userService.getUsersWithPaging(page, pageSize, keyword, status);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 封禁用户
     * @param userId 用户ID
     * @return 封禁结果
     */
    @PostMapping("/users/{userId}/ban")
    @AdminLog(action = "BAN_USER")
    public ResponseEntity<ApiResponse<Boolean>> banUser(@PathVariable Long userId) {
        // 这里需要调用用户服务的封禁方法
        boolean banned = userService.banUser(userId);
        return ResponseEntity.ok(ApiResponse.success(banned));
    }

    /**
     * 解封用户
     * @param userId 用户ID
     * @return 解封结果
     */
    @PostMapping("/users/{userId}/unban")
    @AdminLog(action = "UNBAN_USER")
    public ResponseEntity<ApiResponse<Boolean>> unbanUser(@PathVariable Long userId) {
        boolean unbanned = userService.unbanUser(userId);
        return ResponseEntity.ok(ApiResponse.success(unbanned));
    }

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param passwordData 密码数据
     * @return 重置结果
     */
    @PostMapping("/users/{userId}/reset-password")
    @AdminLog(action = "RESET_USER_PASSWORD")
    public ResponseEntity<ApiResponse<Boolean>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {

        String newPassword = passwordData.get("newPassword");

        // 验证密码格式
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("新密码不能为空"));
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("密码格式不正确：必须包含至少一个字母和一个数字，长度在6-100个字符之间"));
        }

        boolean reset = userService.resetUserPassword(userId, newPassword);
        return ResponseEntity.ok(ApiResponse.success(reset));
    }

    /**
     * 为用户分配角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 分配结果
     */
    @PostMapping("/users/{userId}/roles/{roleId}")
    @AdminLog(action = "ASSIGN_ROLE_TO_USER")
    public ResponseEntity<ApiResponse<Boolean>> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        boolean assigned = rbacService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(assigned));
    }

    /**
     * 从用户移除角色
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 移除结果
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @AdminLog(action = "REMOVE_ROLE_FROM_USER")
    public ResponseEntity<ApiResponse<Boolean>> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        boolean removed = rbacService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(removed));
    }

    /**
     * 获取用户角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getUserRoles(@PathVariable Long userId) {
        List<Role> roles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * 获取系统统计信息
     * @return 系统统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatistics() {
        Map<String, Object> statistics = userService.getSystemStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取系统日志
     * @param page 页码
     * @param pageSize 每页大小
     * @param operatorId 操作者ID（可选）
     * @param action 操作类型（可选）
     * @param ipAddress IP地址（可选）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式：yyyy-MM-dd）
     * @param keyword 关键词搜索（可选，搜索操作详情）
     * @return 系统日志列表
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> logs = logService.getSystemLogsWithFilters(
                page, pageSize, operatorId, action, ipAddress, startDate, endDate, keyword);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 刷新权限缓存
     * @return 刷新结果
     */
    @PostMapping("/refresh-cache")
    @AdminLog(action = "REFRESH_PERMISSION_CACHE")
    public ResponseEntity<ApiResponse<Boolean>> refreshPermissionCache() {
        rbacService.refreshAllPermissionCache();
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    /**
     * 获取系统健康状态
     * @return 系统健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        Map<String, Object> healthStatus = new HashMap<>();

        // 检查数据库连接
        try {
            // 通过简单的查询来检查数据库连接
            userService.getSystemStatistics(); // 这个方法会访问数据库
            healthStatus.put("database", Map.of(
                "status", "healthy",
                "message", "数据库连接正常"
            ));
        } catch (Exception e) {
            healthStatus.put("database", Map.of(
                "status", "unhealthy",
                "message", "数据库连接失败: " + e.getMessage()
            ));
        }

        // 检查Redis连接
        try {
            boolean redisConnected = redisCacheService.exists("health_check");
            healthStatus.put("redis", Map.of(
                "status", redisConnected ? "healthy" : "warning",
                "message", redisConnected ? "Redis连接正常" : "Redis连接异常"
            ));
        } catch (Exception e) {
            healthStatus.put("redis", Map.of(
                "status", "unhealthy",
                "message", "Redis连接失败: " + e.getMessage()
            ));
        }

        // 检查Elasticsearch连接
        try {
            boolean esConnected = elasticsearchSearchService.messageIndexExists();
            healthStatus.put("elasticsearch", Map.of(
                "status", esConnected ? "healthy" : "warning",
                "message", esConnected ? "Elasticsearch连接正常" : "Elasticsearch连接异常"
            ));
        } catch (Exception e) {
            healthStatus.put("elasticsearch", Map.of(
                "status", "unhealthy",
                "message", "Elasticsearch连接失败: " + e.getMessage()
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(healthStatus));
    }

    // ==================== 内容审核相关API ====================

    /**
     * 获取待审核的文章列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 文章状态 (可选)
     * @param keyword 关键词搜索 (可选)
     * @return 待审核文章列表
     */
    @GetMapping("/content/articles/pending")
    @AdminLog(action = "VIEW_PENDING_ARTICLES")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = articleService.getPendingArticlesForModeration(page, pageSize, status, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 审核文章 - 通过
     * @param articleId 文章ID
     * @return 操作结果
     */
    @PostMapping("/content/articles/{articleId}/approve")
    @AdminLog(action = "APPROVE_ARTICLE")
    public ResponseEntity<ApiResponse<Boolean>> approveArticle(@PathVariable Long articleId) {
        boolean approved = articleService.approveArticle(articleId);
        return ResponseEntity.ok(ApiResponse.success(approved));
    }

    /**
     * 审核文章 - 拒绝
     * @param articleId 文章ID
     * @param rejectRequest 拒绝请求
     * @return 操作结果
     */
    @PostMapping("/content/articles/{articleId}/reject")
    @AdminLog(action = "REJECT_ARTICLE")
    public ResponseEntity<ApiResponse<Boolean>> rejectArticle(
            @PathVariable Long articleId,
            @RequestBody @Valid ArticleRejectRequestVo rejectRequest) {

        // 使用ValidationUtils进行额外的拒绝原因验证
        String reason = rejectRequest.getReason();
        if (!ValidationUtils.validateArticleContent(reason)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "拒绝原因格式不正确"));
        }

        // 对拒绝原因进行安全清理
        String sanitizedReason = ValidationUtils.sanitizeSearchKeyword(reason.trim());

        boolean rejected = articleService.rejectArticle(articleId, sanitizedReason);
        return ResponseEntity.ok(ApiResponse.success(rejected));
    }

    /**
     * 删除文章 (管理员权限)
     * @param articleId 文章ID
     * @param deleteRequest 删除请求
     * @return 操作结果
     */
    @DeleteMapping("/content/articles/{articleId}")
    @AdminLog(action = "DELETE_ARTICLE_ADMIN")
    public ResponseEntity<ApiResponse<Boolean>> deleteArticleByAdmin(
            @PathVariable Long articleId,
            @RequestBody(required = false) @Valid ArticleAdminDeleteRequestVo deleteRequest) {

        // 使用ValidationUtils进行删除原因验证
        String reason = "管理员删除"; // 默认原因
        if (deleteRequest != null && deleteRequest.getReason() != null) {
            String inputReason = deleteRequest.getReason();
            if (!ValidationUtils.validateArticleContent(inputReason)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "删除原因格式不正确"));
            }
            // 对删除原因进行安全清理
            reason = ValidationUtils.sanitizeSearchKeyword(inputReason.trim());
        }

        boolean deleted = articleService.deleteArticleByAdmin(articleId, reason);
        return ResponseEntity.ok(ApiResponse.success(deleted));
    }

    /**
     * 获取内容审核统计
     * @return 内容审核统计数据
     */
    @GetMapping("/content/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContentModerationStatistics() {
        Map<String, Object> statistics = articleService.getContentModerationStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    // ==================== 系统监控相关API ====================

    /**
     * 获取实时系统监控数据
     * @return 系统监控数据
     */
    @GetMapping("/monitor/realtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtimeMonitoringData() {
        Map<String, Object> monitoringData = new HashMap<>();

        try {
            // 获取系统基础统计
            Map<String, Object> systemStats = userService.getSystemStatistics();
            monitoringData.put("systemStats", systemStats);

            // 获取数据库状态
            monitoringData.put("database", getDatabaseStatus());

            // 获取Redis状态
            monitoringData.put("redis", getRedisStatus());

            // 获取Elasticsearch状态
            monitoringData.put("elasticsearch", getElasticsearchStatus());

            // 获取最近15分钟的系统活动
            monitoringData.put("recentActivity", getRecentSystemActivity());

            // 获取用户在线统计
            monitoringData.put("onlineUsers", getOnlineUserStatistics());

            return ResponseEntity.ok(ApiResponse.success(monitoringData));

        } catch (Exception e) {
            log.error("获取系统监控数据失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(500, "获取监控数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户行为分析数据
     * @param days 统计天数 (默认7天)
     * @return 用户行为分析数据
     */
    @GetMapping("/monitor/user-behavior")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserBehaviorAnalysis(
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> behaviorData = userService.getUserBehaviorAnalysis(days);
        return ResponseEntity.ok(ApiResponse.success(behaviorData));
    }

    /**
     * 获取用户行为事件列表
     * @param days 统计天数 (默认1天)
     * @param eventType 事件类型过滤 (可选)
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户行为事件列表
     */
    @GetMapping("/monitor/user-events")
    @AdminLog(action = "VIEW_USER_EVENTS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserBehaviorEvents(
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        Map<String, Object> events = userService.getUserBehaviorEvents(days, eventType, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 获取用户分群统计数据
     * @param days 统计天数 (默认30天)
     * @return 用户分群统计
     */
    @GetMapping("/monitor/user-segments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserSegmentAnalysis(
            @RequestParam(defaultValue = "30") int days) {

        Map<String, Object> segments = userService.getUserSegmentAnalysis(days);
        return ResponseEntity.ok(ApiResponse.success(segments));
    }

    /**
     * 获取热门页面统计
     * @param days 统计天数 (默认7天)
     * @return 热门页面数据
     */
    @GetMapping("/monitor/popular-pages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPopularPages(
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> pages = userService.getPopularPages(days);
        return ResponseEntity.ok(ApiResponse.success(pages));
    }

    /**
     * 获取异常行为检测结果
     * @param hours 检测时间范围 (默认24小时)
     * @return 异常行为列表
     */
    @GetMapping("/monitor/anomalies")
    @AdminLog(action = "VIEW_ANOMALIES")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBehaviorAnomalies(
            @RequestParam(defaultValue = "24") int hours) {

        Map<String, Object> anomalies = userService.getBehaviorAnomalies(hours);
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    /**
     * 运行异常行为检测
     * @return 检测结果
     */
    @PostMapping("/monitor/run-anomaly-detection")
    @AdminLog(action = "RUN_ANOMALY_DETECTION")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runAnomalyDetection() {
        Map<String, Object> result = userService.runAnomalyDetection();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 导出用户行为数据
     * @param format 导出格式 (csv, xlsx, json)
     * @param days 统计天数
     * @return 导出文件
     */
    @GetMapping("/monitor/export-behavior-data")
    @AdminLog(action = "EXPORT_BEHAVIOR_DATA")
    public ResponseEntity<?> exportBehaviorData(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "30") int days) {

        try {
            byte[] exportData = userService.exportBehaviorData(format, days);
            String filename = "user-behavior-" + days + "days." + format;

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", getContentType(format))
                .body(exportData);
        } catch (Exception e) {
            log.error("导出用户行为数据失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(500, "导出失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户活动热力图数据
     * @param type 热力图类型 (hourly, weekly, monthly)
     * @param days 统计天数
     * @return 热力图数据
     */
    @GetMapping("/monitor/activity-heatmap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivityHeatmap(
            @RequestParam(defaultValue = "hourly") String type,
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> heatmapData = userService.getActivityHeatmap(type, days);
        return ResponseEntity.ok(ApiResponse.success(heatmapData));
    }

    /**
     * 获取用户留存分析
     * @param cohortType 队列类型 (daily, weekly, monthly)
     * @param periods 分析期数
     * @return 留存数据
     */
    @GetMapping("/monitor/user-retention")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRetention(
            @RequestParam(defaultValue = "weekly") String cohortType,
            @RequestParam(defaultValue = "12") int periods) {

        Map<String, Object> retentionData = userService.getUserRetention(cohortType, periods);
        return ResponseEntity.ok(ApiResponse.success(retentionData));
    }

    // ==================== 系统日志查看器相关API ====================

    /**
     * 获取系统日志统计信息
     * @return 日志统计数据
     */
    @GetMapping("/logs/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogStatistics() {
        Map<String, Object> statistics = logService.getLogStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取错误日志统计
     * @param hours 统计小时数 (默认24小时)
     * @return 错误日志统计数据
     */
    @GetMapping("/logs/errors")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getErrorStatistics(
            @RequestParam(defaultValue = "24") int hours) {

        Map<String, Object> errorStats = logService.getErrorStatistics(hours);
        return ResponseEntity.ok(ApiResponse.success(errorStats));
    }

    /**
     * 获取最近活动统计
     * @param minutes 统计分钟数 (默认15分钟)
     * @return 活动统计数据
     */
    @GetMapping("/logs/recent-activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentActivityStatistics(
            @RequestParam(defaultValue = "15") int minutes) {

        Map<String, Object> activityStats = logService.getRecentActivityStatistics(minutes);
        return ResponseEntity.ok(ApiResponse.success(activityStats));
    }

    /**
     * 获取可用操作类型列表
     * @return 操作类型列表
     */
    @GetMapping("/logs/actions")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableActions() {
        List<String> actions = logService.getAvailableActions();
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    /**
     * 获取可用操作员列表
     * @param days 最近天数 (默认30天)
     * @return 操作员列表
     */
    @GetMapping("/logs/operators")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableOperators(
            @RequestParam(defaultValue = "30") int days) {

        List<Map<String, Object>> operators = logService.getAvailableOperators(days);
        return ResponseEntity.ok(ApiResponse.success(operators));
    }

    /**
     * 导出系统日志
     * @param format 导出格式 (csv, xlsx, json)
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @param operatorId 操作者ID (可选)
     * @param action 操作类型 (可选)
     * @param ipAddress IP地址 (可选)
     * @param keyword 关键词 (可选)
     * @return 导出文件
     */
    @GetMapping("/logs/export")
    @AdminLog(action = "EXPORT_LOGS")
    public ResponseEntity<?> exportLogs(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String keyword) {

        try {
            // 设置默认时间范围
            if (startDate == null) {
                startDate = java.time.LocalDate.now().minusDays(30).toString();
            }
            if (endDate == null) {
                endDate = java.time.LocalDate.now().toString();
            }

            // 构建过滤条件
            Map<String, Object> filters = new HashMap<>();
            if (operatorId != null) filters.put("operatorId", operatorId);
            if (action != null) filters.put("action", action);
            if (ipAddress != null) filters.put("ipAddress", ipAddress);
            if (keyword != null) filters.put("keyword", keyword);

            byte[] exportData = logService.exportLogs(format, startDate, endDate, filters);
            String filename = "system-logs-" + startDate + "-to-" + endDate + "." + format;

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", getLogContentType(format))
                .body(exportData);

        } catch (Exception e) {
            log.error("导出系统日志失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(500, "导出失败: " + e.getMessage()));
        }
    }

    /**
     * 清理过期日志
     * @param days 保留天数
     * @return 清理结果
     */
    @PostMapping("/logs/cleanup")
    @AdminLog(action = "CLEANUP_LOGS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupOldLogs(
            @RequestParam(defaultValue = "30") int days) {

        Map<String, Object> result = logService.cleanupOldLogs(days);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 批量删除日志
     * @param logIds 日志ID列表
     * @return 删除结果
     */
    @DeleteMapping("/logs/batch")
    @AdminLog(action = "BATCH_DELETE_LOGS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDeleteLogs(
            @RequestBody List<Long> logIds,
            @RequestAttribute Long operatorId) {

        Map<String, Object> result = logService.batchDeleteLogs(logIds, operatorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取日志详情
     * @param logId 日志ID
     * @return 日志详情
     */
    @GetMapping("/logs/{logId}")
    public ResponseEntity<ApiResponse<SystemLog>> getLogDetails(
            @PathVariable Long logId) {

        SystemLog log = logService.getLogDetails(logId);
        return ResponseEntity.ok(ApiResponse.success(log));
    }

    /**
     * 获取日志级别分布
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 日志级别分布
     */
    @GetMapping("/logs/distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogLevelDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        if (startDate == null) {
            startDate = java.time.LocalDate.now().minusDays(7).toString();
        }
        if (endDate == null) {
            endDate = java.time.LocalDate.now().toString();
        }

        Map<String, Object> distribution = logService.getLogLevelDistribution(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * 获取每小时日志统计
     * @param date 日期 (yyyy-MM-dd)
     * @return 每小时日志统计
     */
    @GetMapping("/logs/hourly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHourlyLogStatistics(
            @RequestParam String date) {

        Map<String, Object> statistics = logService.getHourlyLogStatistics(date);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 搜索日志
     * @param keyword 搜索关键词
     * @param limit 返回数量限制 (默认100)
     * @return 搜索结果
     */
    @GetMapping("/logs/search")
    public ResponseEntity<ApiResponse<List<SystemLog>>> searchLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "100") int limit) {

        List<SystemLog> results = logService.searchLogs(keyword, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 获取导出文件的Content-Type
     */
    private String getLogContentType(String format) {
        switch (format.toLowerCase()) {
            case "csv":
                return "text/csv";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 获取系统性能指标
     * @return 系统性能指标
     */
    @GetMapping("/monitor/performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemPerformanceMetrics() {
        Map<String, Object> performanceData = new HashMap<>();

        try {
            // JVM内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            performanceData.put("memory", Map.of(
                "total", totalMemory / 1024 / 1024 + " MB",
                "used", usedMemory / 1024 / 1024 + " MB",
                "free", freeMemory / 1024 / 1024 + " MB",
                "usagePercent", Math.round((double) usedMemory / totalMemory * 100)
            ));

            // 系统负载 (简化版本)
            performanceData.put("system", Map.of(
                "loadAverage", "N/A", // 需要OS特定的实现
                "cpuUsage", "N/A"    // 需要OS特定的实现
            ));

            // 数据库连接池状态
            performanceData.put("database", Map.of(
                "activeConnections", "N/A", // 需要数据源配置
                "idleConnections", "N/A",
                "totalConnections", "N/A"
            ));

            return ResponseEntity.ok(ApiResponse.success(performanceData));

        } catch (Exception e) {
            log.error("获取系统性能指标失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(500, "获取性能指标失败: " + e.getMessage()));
        }
    }


    // ==================== 私有辅助方法 ====================

    private Map<String, Object> getDatabaseStatus() {
        try {
            userService.getSystemStatistics(); // 测试数据库连接
            return Map.of(
                "status", "healthy",
                "responseTime", "< 10ms",
                "message", "数据库连接正常"
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "responseTime", "N/A",
                "message", "数据库连接失败: " + e.getMessage()
            );
        }
    }

    private Map<String, Object> getRedisStatus() {
        try {
            boolean connected = redisCacheService.exists("test");
            return Map.of(
                "status", connected ? "healthy" : "warning",
                "responseTime", "< 5ms",
                "message", connected ? "Redis连接正常" : "Redis连接异常"
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "responseTime", "N/A",
                "message", "Redis连接失败: " + e.getMessage()
            );
        }
    }

    private Map<String, Object> getElasticsearchStatus() {
        try {
            boolean connected = elasticsearchSearchService.messageIndexExists();
            return Map.of(
                "status", connected ? "healthy" : "warning",
                "responseTime", "< 50ms",
                "message", connected ? "Elasticsearch连接正常" : "Elasticsearch连接异常"
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "responseTime", "N/A",
                "message", "Elasticsearch连接失败: " + e.getMessage()
            );
        }
    }

    private Map<String, Object> getRecentSystemActivity() {
        try {
            // 获取最近15分钟的活动统计
            return logService.getRecentActivityStatistics(15);
        } catch (Exception e) {
            log.warn("获取最近系统活动失败", e);
            return Map.of("error", "获取活动数据失败");
        }
    }

    private Map<String, Object> getOnlineUserStatistics() {
        try {
            // 获取在线用户统计 (简化实现)
            return Map.of(
                "currentOnline", userService.getCurrentOnlineUserCount(),
                "peakToday", userService.getPeakOnlineUsersToday(),
                "averageSessionDuration", "25 minutes"
            );
        } catch (Exception e) {
            log.warn("获取在线用户统计失败", e);
            return Map.of("error", "获取在线数据失败");
        }
    }

    /**
     * 获取导出文件的Content-Type
     */
    private String getContentType(String format) {
        switch (format.toLowerCase()) {
            case "csv":
                return "text/csv";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }
}
