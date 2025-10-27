package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.service.RolePermissionService;
import com.web.vo.rolepermission.UpdateRolePermissionsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色权限Controller
 * 提供角色权限管理的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/role-permissions")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    /**
     * 为用户自动分配角色
     */
    @PostMapping("/auto-assign/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoAssignRoles(@PathVariable Long userId) {
        try {
            Map<String, Object> result = rolePermissionService.autoAssignRoles(userId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("自动分配角色失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("自动分配失败: " + e.getMessage()));
        }
    }

    /**
     * 批量自动分配角色
     */
    @PostMapping("/batch-auto-assign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchAutoAssignRoles(
            @RequestBody List<Long> userIds) {
        try {
            Map<String, Object> result = rolePermissionService.batchAutoAssignRoles(userIds);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("批量自动分配角色失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量分配失败: " + e.getMessage()));
        }
    }

    /**
     * 根据用户等级获取应该拥有的角色
     */
    @GetMapping("/roles-for-level/{userLevel}")
    public ResponseEntity<ApiResponse<List<Role>>> getRolesForLevel(@PathVariable int userLevel) {
        try {
            List<Role> roles = rolePermissionService.getRolesForUserLevel(userLevel);
            return ResponseEntity.ok(ApiResponse.success(roles));
        } catch (Exception e) {
            log.error("获取等级角色失败: userLevel={}", userLevel, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否拥有指定角色
     */
    @GetMapping("/user/{userId}/has-role/{roleName}")
    public ResponseEntity<ApiResponse<Boolean>> hasRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        try {
            boolean hasRole = rolePermissionService.hasRole(userId, roleName);
            return ResponseEntity.ok(ApiResponse.success(hasRole));
        } catch (Exception e) {
            log.error("检查用户角色失败: userId={}, roleName={}", userId, roleName, e);
            return ResponseEntity.ok(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<Boolean>> assignRole(
            @RequestParam Long userId,
            @RequestParam Long roleId,
            @RequestParam(required = false) Long operatorId) {
        try {
            boolean success = rolePermissionService.assignRole(userId, roleId, operatorId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            log.error("分配角色失败: userId={}, roleId={}", userId, roleId, e);
            return ResponseEntity.ok(ApiResponse.error("分配失败: " + e.getMessage()));
        }
    }

    /**
     * 移除用户角色
     */
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<Boolean>> removeRole(
            @RequestParam Long userId,
            @RequestParam Long roleId,
            @RequestParam(required = false) Long operatorId) {
        try {
            boolean success = rolePermissionService.removeRole(userId, roleId, operatorId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            log.error("移除角色失败: userId={}, roleId={}", userId, roleId, e);
            return ResponseEntity.ok(ApiResponse.error("移除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的所有角色
     */
    @GetMapping("/user/{userId}/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getUserRoles(@PathVariable Long userId) {
        try {
            List<Role> roles = rolePermissionService.getUserRoles(userId);
            return ResponseEntity.ok(ApiResponse.success(roles));
        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色的所有权限
     */
    @GetMapping("/role/{roleId}/permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getRolePermissions(@PathVariable Long roleId) {
        try {
            List<Permission> permissions = rolePermissionService.getRolePermissions(roleId);
            return ResponseEntity.ok(ApiResponse.success(permissions));
        } catch (Exception e) {
            log.error("获取角色权限失败: roleId={}", roleId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的所有权限
     */
    @GetMapping("/user/{userId}/all-permissions")
    public ResponseEntity<ApiResponse<Set<String>>> getAllUserPermissions(@PathVariable Long userId) {
        try {
            Set<String> permissions = rolePermissionService.getAllUserPermissions(userId);
            return ResponseEntity.ok(ApiResponse.success(permissions));
        } catch (Exception e) {
            log.error("获取用户所有权限失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否有特定权限
     */
    @GetMapping("/user/{userId}/has-permission/{permission}")
    public ResponseEntity<ApiResponse<Boolean>> hasPermission(
            @PathVariable Long userId,
            @PathVariable String permission) {
        try {
            boolean hasPermission = rolePermissionService.hasPermission(userId, permission);
            return ResponseEntity.ok(ApiResponse.success(hasPermission));
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return ResponseEntity.ok(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }

    /**
     * 更新角色权限映射
     */
    @PostMapping("/update-role-permissions")
    public ResponseEntity<ApiResponse<Boolean>> updateRolePermissions(
            @Valid @RequestBody UpdateRolePermissionsVo vo,
            @RequestParam(required = false) Long operatorId) {
        try {
            boolean success = rolePermissionService.updateRolePermissions(
                    vo.getRoleId(), vo.getPermissionIds(), operatorId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            log.error("更新角色权限失败: vo={}", vo, e);
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色分配统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> stats = rolePermissionService.getRoleAssignmentStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取角色分配统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 验证角色权限配置
     */
    @GetMapping("/validate/{roleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRolePermissions(
            @PathVariable Long roleId) {
        try {
            Map<String, Object> validation = rolePermissionService.validateRolePermissions(roleId);
            return ResponseEntity.ok(ApiResponse.success(validation));
        } catch (Exception e) {
            log.error("验证角色权限失败: roleId={}", roleId, e);
            return ResponseEntity.ok(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    /**
     * 同步用户角色（等级变更时）
     */
    @PostMapping("/sync-on-level-change")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncOnLevelChange(
            @RequestParam Long userId,
            @RequestParam int oldLevel,
            @RequestParam int newLevel) {
        try {
            Map<String, Object> result = rolePermissionService.syncUserRolesOnLevelChange(
                    userId, oldLevel, newLevel);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("同步用户角色失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            return ResponseEntity.ok(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    /**
     * 获取角色权限建议
     */
    @GetMapping("/recommendations/{userLevel}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendations(
            @PathVariable int userLevel) {
        try {
            Map<String, Object> recommendations = rolePermissionService.getRolePermissionRecommendations(userLevel);
            return ResponseEntity.ok(ApiResponse.success(recommendations));
        } catch (Exception e) {
            log.error("获取角色权限建议失败: userLevel={}", userLevel, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 应用角色权限模板
     */
    @PostMapping("/apply-template")
    public ResponseEntity<ApiResponse<Boolean>> applyTemplate(
            @RequestParam String templateName,
            @RequestParam Long targetRoleId,
            @RequestParam(required = false) Long operatorId) {
        try {
            boolean success = rolePermissionService.applyRolePermissionTemplate(
                    templateName, targetRoleId, operatorId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            log.error("应用角色权限模板失败: templateName={}, targetRoleId={}",
                    templateName, targetRoleId, e);
            return ResponseEntity.ok(ApiResponse.error("应用失败: " + e.getMessage()));
        }
    }
}
