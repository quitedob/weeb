package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.model.User;
import com.web.annotation.AdminLog;
import com.web.service.PermissionService;
import com.web.service.RBACService;
import com.web.service.RoleService;
import com.web.service.UserService;
import com.web.service.LogService;
import com.web.service.RedisCacheService;
import com.web.service.ElasticsearchSearchService;
import com.web.util.ValidationUtils;
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
}
