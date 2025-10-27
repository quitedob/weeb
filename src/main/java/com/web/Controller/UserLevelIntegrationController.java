package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.service.UserLevelIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户等级集成Controller
 * 提供等级变更的完整流程接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user-level-integration")
public class UserLevelIntegrationController {

    @Autowired
    private UserLevelIntegrationService userLevelIntegrationService;

    /**
     * 处理用户等级变更（完整流程）
     */
    @PostMapping("/handle-level-change")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleLevelChange(
            @RequestParam Long userId,
            @RequestParam Integer oldLevel,
            @RequestParam Integer newLevel,
            @RequestParam String changeReason,
            @RequestParam Integer changeType,
            @RequestParam(required = false) Long operatorId,
            HttpServletRequest request) {
        try {
            // 获取IP地址和User-Agent
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            Map<String, Object> result = userLevelIntegrationService.handleLevelChange(
                    userId, oldLevel, newLevel, changeReason, changeType,
                    operatorId, ipAddress, userAgent);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("处理等级变更失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            return ResponseEntity.ok(ApiResponse.error("处理失败: " + e.getMessage()));
        }
    }

    /**
     * 批量处理用户等级变更
     */
    @PostMapping("/batch-handle-level-changes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchHandleLevelChanges(
            @RequestBody List<Map<String, Object>> levelChanges) {
        try {
            Map<String, Object> result = userLevelIntegrationService.batchHandleLevelChanges(levelChanges);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("批量处理等级变更失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量处理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户等级完整信息
     */
    @GetMapping("/user/{userId}/complete-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCompleteInfo(
            @PathVariable Long userId) {
        try {
            Map<String, Object> info = userLevelIntegrationService.getUserLevelCompleteInfo(userId);
            return ResponseEntity.ok(ApiResponse.success(info));
        } catch (Exception e) {
            log.error("获取用户完整信息失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 验证等级变更的合法性
     */
    @GetMapping("/validate-level-change")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateLevelChange(
            @RequestParam Long userId,
            @RequestParam Integer oldLevel,
            @RequestParam Integer newLevel) {
        try {
            Map<String, Object> validation = userLevelIntegrationService.validateLevelChange(
                    userId, oldLevel, newLevel);
            return ResponseEntity.ok(ApiResponse.success(validation));
        } catch (Exception e) {
            log.error("验证等级变更失败: userId={}, oldLevel={}, newLevel={}",
                    userId, oldLevel, newLevel, e);
            return ResponseEntity.ok(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
