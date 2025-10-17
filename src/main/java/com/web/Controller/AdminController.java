package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.model.User;
import com.web.service.PermissionService;
import com.web.service.RBACService;
import com.web.service.RoleService;
import com.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    /**
     * 获取权限管理页面数据
     * @return 权限列表和统计信息
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPermissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> data = permissionService.getPermissionsWithPaging(page, pageSize, keyword);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建权限
     * @param permission 权限对象
     * @return 创建结果
     */
    @PostMapping("/permissions")
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
            @RequestParam(required = false) String keyword) {

        Map<String, Object> data = roleService.getRolesWithPaging(page, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 创建角色
     * @param role 角色对象
     * @return 创建结果
     */
    @PostMapping("/roles")
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
     * @return 用户列表和统计信息
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> data = userService.getUsersWithPaging(page, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 封禁用户
     * @param userId 用户ID
     * @return 封禁结果
     */
    @PostMapping("/users/{userId}/ban")
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
    public ResponseEntity<ApiResponse<Boolean>> unbanUser(@PathVariable Long userId) {
        boolean unbanned = userService.unbanUser(userId);
        return ResponseEntity.ok(ApiResponse.success(unbanned));
    }

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 重置结果
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {

        String newPassword = passwordData.get("newPassword");
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
     * @return 系统日志列表
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 这里需要调用日志服务的相关方法
        Map<String, Object> logs = Map.of(
            "list", List.of(),
            "total", 0L
        );
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 刷新权限缓存
     * @return 刷新结果
     */
    @PostMapping("/refresh-cache")
    public ResponseEntity<ApiResponse<Boolean>> refreshPermissionCache() {
        rbacService.refreshAllPermissionCache();
        return ResponseEntity.ok(ApiResponse.success(true));
    }
}
