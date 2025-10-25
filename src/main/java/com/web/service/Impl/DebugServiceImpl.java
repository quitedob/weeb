package com.web.service.Impl;

import com.web.mapper.PermissionMapper;
import com.web.mapper.RoleMapper;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.service.DebugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试服务实现类
 * 提供系统调试和信息查询功能
 */
@Service
public class DebugServiceImpl implements DebugService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private DataSource dataSource;

    @Override
    public Map<String, Object> getUserPermissions(Long userId) {
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
            result.put("userId", userId);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("userId", userId);
        }

        return result;
    }

    @Override
    public Map<String, Object> getAllPermissions() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Permission> permissions = permissionMapper.selectList(null);
            result.put("permissions", permissions);
            result.put("total", permissions.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("permissions", List.of());
            result.put("total", 0);
        }

        return result;
    }

    @Override
    public Map<String, Object> getAllRoles() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Role> roles = roleMapper.selectList(null);
            result.put("roles", roles);
            result.put("total", roles.size());
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("roles", List.of());
            result.put("total", 0);
        }

        return result;
    }

    @Override
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查数据库连接
            Map<String, Object> dbStatus = getDatabaseStatus();
            result.put("database", dbStatus);

            // 检查内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("total", totalMemory / 1024 / 1024 + " MB");
            memoryInfo.put("used", usedMemory / 1024 / 1024 + " MB");
            memoryInfo.put("free", freeMemory / 1024 / 1024 + " MB");
            memoryInfo.put("max", maxMemory / 1024 / 1024 + " MB");
            memoryInfo.put("usagePercentage", (double) usedMemory / maxMemory * 100);
            result.put("memory", memoryInfo);

            // 检查JVM信息
            Map<String, Object> jvmInfo = new HashMap<>();
            jvmInfo.put("javaVersion", System.getProperty("java.version"));
            jvmInfo.put("javaVendor", System.getProperty("java.vendor"));
            jvmInfo.put("osName", System.getProperty("os.name"));
            jvmInfo.put("osVersion", System.getProperty("os.version"));
            jvmInfo.put("availableProcessors", runtime.availableProcessors());
            result.put("jvm", jvmInfo);

            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 尝试获取数据库连接
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    result.put("connected", true);
                    result.put("url", connection.getMetaData().getURL());
                    result.put("username", connection.getMetaData().getUserName());
                    result.put("driverName", connection.getMetaData().getDriverName());
                    result.put("driverVersion", connection.getMetaData().getDriverVersion());
                    result.put("databaseProductName", connection.getMetaData().getDatabaseProductName());
                    result.put("databaseProductVersion", connection.getMetaData().getDatabaseProductVersion());
                } else {
                    result.put("connected", false);
                    result.put("error", "数据库连接无效");
                }
            }

            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("connected", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getSystemConfiguration() {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> config = new HashMap<>();
            config.put("app.name", "Weeb Application");
            config.put("app.version", "1.0.0");
            config.put("spring.profiles.active", System.getProperty("spring.profiles.active", "default"));
            config.put("server.port", System.getProperty("server.port", "8080"));

            result.put("configuration", config);
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("configuration", new HashMap<>());
        }

        return result;
    }
}