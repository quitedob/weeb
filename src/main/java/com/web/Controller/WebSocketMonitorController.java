package com.web.Controller;

import com.web.service.WebSocketConnectionService;
import com.web.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "WebSocket连接监控")
public class WebSocketMonitorController {

    @Autowired
    private WebSocketConnectionService connectionService;

    /**
     * 获取在线用户数
     */
    @GetMapping("/online-count")
    @ApiOperation("获取在线用户数")
    public Result<Long> getOnlineUserCount() {
        try {
            long count = connectionService.getOnlineUserCount();
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
            return Result.error("获取在线用户数失败");
        }
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    @ApiOperation("获取在线用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Set<Long>> getOnlineUsers() {
        try {
            Set<Long> userIds = connectionService.getOnlineUserIds();
            return Result.success(userIds);
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return Result.error("获取在线用户列表失败");
        }
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/user/{userId}/online")
    @ApiOperation("检查用户是否在线")
    public Result<Boolean> isUserOnline(@PathVariable Long userId) {
        try {
            boolean isOnline = connectionService.isUserOnline(userId);
            return Result.success(isOnline);
        } catch (Exception e) {
            log.error("检查用户在线状态失败: userId={}", userId, e);
            return Result.error("检查用户在线状态失败");
        }
    }

    /**
     * 获取用户连接信息
     */
    @GetMapping("/user/{userId}/info")
    @ApiOperation("获取用户连接信息")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public Result<Map<String, Object>> getUserConnectionInfo(@PathVariable Long userId) {
        try {
            Map<String, Object> info = connectionService.getUserConnectionInfo(userId);
            return Result.success(info);
        } catch (Exception e) {
            log.error("获取用户连接信息失败: userId={}", userId, e);
            return Result.error("获取用户连接信息失败");
        }
    }

    /**
     * 获取连接统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取连接统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getConnectionStatistics() {
        try {
            Map<String, Object> stats = connectionService.getConnectionStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取连接统计信息失败", e);
            return Result.error("获取连接统计信息失败");
        }
    }

    /**
     * 手动清理过期连接
     */
    @PostMapping("/clean-expired")
    @ApiOperation("手动清理过期连接")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> cleanExpiredConnections() {
        try {
            int cleanedCount = connectionService.cleanExpiredConnections();
            return Result.success(cleanedCount, "清理完成，共清理 " + cleanedCount + " 个过期连接");
        } catch (Exception e) {
            log.error("清理过期连接失败", e);
            return Result.error("清理过期连接失败");
        }
    }

    /**
     * 获取用户活跃会话列表
     */
    @GetMapping("/user/{userId}/sessions")
    @ApiOperation("获取用户活跃会话列表")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public Result<Set<String>> getUserActiveSessions(@PathVariable Long userId) {
        try {
            Set<String> sessions = connectionService.getUserActiveSessions(userId);
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取用户活跃会话失败: userId={}", userId, e);
            return Result.error("获取用户活跃会话失败");
        }
    }
}
