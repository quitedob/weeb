package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.service.DebugService;
import com.web.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 调试控制器
 * 提供系统调试和信息查询功能
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private DebugService debugService;

    /**
     * 调试用户权限信息
     * GET /api/debug/user-permissions?userId=1
     */
    @GetMapping("/user-permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPermissions(@RequestParam Long userId) {
        try {
            Map<String, Object> result = debugService.getUserPermissions(userId);
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取用户权限信息成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取用户权限信息", userId);
        }
    }

    /**
     * 获取所有权限列表
     * GET /api/debug/all-permissions
     */
    @GetMapping("/all-permissions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPermissions() {
        try {
            Map<String, Object> result = debugService.getAllPermissions();
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取权限列表成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取权限列表");
        }
    }

    /**
     * 获取所有角色
     * GET /api/debug/all-roles
     */
    @GetMapping("/all-roles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRoles() {
        try {
            Map<String, Object> result = debugService.getAllRoles();
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取角色列表成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取角色列表");
        }
    }

    /**
     * 获取系统健康状态
     * GET /api/debug/system-health
     */
    @GetMapping("/system-health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            Map<String, Object> result = debugService.getSystemHealth();
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取系统健康状态成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取系统健康状态");
        }
    }

    /**
     * 获取数据库状态
     * GET /api/debug/database-status
     */
    @GetMapping("/database-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseStatus() {
        try {
            Map<String, Object> result = debugService.getDatabaseStatus();
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取数据库状态成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取数据库状态");
        }
    }

    /**
     * 获取系统配置
     * GET /api/debug/system-config
     */
    @GetMapping("/system-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemConfiguration() {
        try {
            Map<String, Object> result = debugService.getSystemConfiguration();
            if ((Boolean) result.get("success")) {
                return ApiResponseUtil.successMap(result, "获取系统配置成功");
            } else {
                return ApiResponseUtil.badRequestMap((String) result.get("error"));
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取系统配置");
        }
    }
}