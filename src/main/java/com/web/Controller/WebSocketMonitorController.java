package com.web.Controller;

import com.web.service.WebSocketConnectionService;
import com.web.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * WebSocket连接监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/websocket/monitor")
@Tag(name = "WebSocket连接监控", description = "WebSocket连接监控相关接口")
public class WebSocketMonitorController {

    @Autowired
    private WebSocketConnectionService connectionService;

    /**
     * 获取在线用户数
     */
    @GetMapping("/online-count")
    @Operation(summary = "获取在线用户数")
    public ApiResponse<Long> getOnlineUserCount() {
        try {
            long count = connectionService.getOnlineUserCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
            return ApiResponse.error("获取在线用户数失败");
        }
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    @Operation(summary = "获取在线用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Set<Long>> getOnlineUsers() {
        try {
            Set<Long> userIds = connectionService.getOnlineUserIds();
            return ApiResponse.success(userIds);
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return ApiResponse.error("获取在线用户列表失败");
        }
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/user/{userId}/online")
    @Operation(summary = "检查用户是否在线")
    public ApiResponse<Boolean> isUserOnline(@PathVariable Long userId) {
        try {
            boolean isOnline = connectionService.isUserOnline(userId);
            return ApiResponse.success(isOnline);
        } catch (Exception e) {
            log.error("检查用户在线状态失败: userId={}", userId, e);
            return ApiResponse.error("检查用户在线状态失败");
        }
    }

    /**
     * 获取用户连接信息
     */
    @GetMapping("/user/{userId}/info")
    @Operation(summary = "获取用户连接信息")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ApiResponse<Map<String, Object>> getUserConnectionInfo(@PathVariable Long userId) {
        try {
            Map<String, Object> info = connectionService.getUserConnectionInfo(userId);
            return ApiResponse.success(info);
        } catch (Exception e) {
            log.error("获取用户连接信息失败: userId={}", userId, e);
            return ApiResponse.error("获取用户连接信息失败");
        }
    }

    /**
     * 获取连接统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取连接统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getConnectionStatistics() {
        try {
            Map<String, Object> stats = connectionService.getConnectionStatistics();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取连接统计信息失败", e);
            return ApiResponse.error("获取连接统计信息失败");
        }
    }

    /**
     * 手动清理过期连接
     */
    @PostMapping("/clean-expired")
    @Operation(summary = "手动清理过期连接")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> cleanExpiredConnections() {
        try {
            int cleanedCount = connectionService.cleanExpiredConnections();
            return ApiResponse.success("清理完成，共清理 " + cleanedCount + " 个过期连接", cleanedCount);
        } catch (Exception e) {
            log.error("清理过期连接失败", e);
            return ApiResponse.error("清理过期连接失败");
        }
    }

    /**
     * 获取用户活跃会话列表
     */
    @GetMapping("/user/{userId}/sessions")
    @Operation(summary = "获取用户活跃会话列表")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ApiResponse<Set<String>> getUserActiveSessions(@PathVariable Long userId) {
        try {
            Set<String> sessions = connectionService.getUserActiveSessions(userId);
            return ApiResponse.success(sessions);
        } catch (Exception e) {
            log.error("获取用户活跃会话失败: userId={}", userId, e);
            return ApiResponse.error("获取用户活跃会话失败");
        }
    }
}
