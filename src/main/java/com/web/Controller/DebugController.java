package com.web.Controller;

import com.web.mapper.PermissionMapper;
import com.web.mapper.RoleMapper;
import com.web.model.Permission;
import com.web.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 调试用户权限信息
     */
    @GetMapping("/user-permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取用户角色
            List<Role> userRoles = roleMapper.selectRolesByUserId(userId);
            result.put("userRoles", userRoles);

            // 获取每个角色的权限
            Map<String, List<Permission>> rolePermissions = new HashMap<>();
            for (Role role : userRoles) {
                List<Permission> permissions = permissionMapper.selectPermissionsByRoleId(role.getId());
                rolePermissions.put(role.getName(), permissions);
            }
            result.put("rolePermissions", rolePermissions);

            // 检查USER_READ_OWN权限是否存在
            Permission userReadOwnPermission = permissionMapper.selectByName("USER_READ_OWN");
            result.put("USER_READ_OWN_permission", userReadOwnPermission);

            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping("/all-permissions")
    public ResponseEntity<Map<String, Object>> getAllPermissions() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Permission> permissions = permissionMapper.selectList(null);
            result.put("permissions", permissions);
            result.put("total", permissions.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有角色
     */
    @GetMapping("/all-roles")
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Role> roles = roleMapper.selectList(null);
            result.put("roles", roles);
            result.put("total", roles.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}