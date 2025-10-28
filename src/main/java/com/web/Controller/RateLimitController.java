package com.web.Controller;

import com.web.service.RateLimitService;
import com.web.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 限流管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rate-limit")
@Tag(name = "限流管理", description = "限流管理相关接口")
public class RateLimitController {

    @Autowired
    private RateLimitService rateLimitService;

    /**
     * 设置动态限流配置
     */
    @PostMapping("/config")
    @Operation(summary = "设置动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> setDynamicRateLimit(
            @RequestParam String path,
            @RequestParam int maxRequests) {
        try {
            boolean success = rateLimitService.setDynamicRateLimit(path, maxRequests);
            return success ? ApiResponse.success("配置成功", true) : ApiResponse.error("配置失败");
        } catch (Exception e) {
            log.error("设置动态限流配置失败", e);
            return ApiResponse.error("设置失败");
        }
    }

    /**
     * 获取动态限流配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> getDynamicRateLimit(@RequestParam String path) {
        try {
            Integer maxRequests = rateLimitService.getDynamicRateLimit(path);
            return maxRequests != null ? ApiResponse.success(maxRequests) : ApiResponse.error("配置不存在");
        } catch (Exception e) {
            log.error("获取动态限流配置失败", e);
            return ApiResponse.error("获取失败");
        }
    }

    /**
     * 删除动态限流配置
     */
    @DeleteMapping("/config")
    @Operation(summary = "删除动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> removeDynamicRateLimit(@RequestParam String path) {
        try {
            boolean success = rateLimitService.removeDynamicRateLimit(path);
            return success ? ApiResponse.success("删除成功", true) : ApiResponse.error("删除失败");
        } catch (Exception e) {
            log.error("删除动态限流配置失败", e);
            return ApiResponse.error("删除失败");
        }
    }

    /**
     * 获取所有动态限流配置
     */
    @GetMapping("/config/all")
    @Operation(summary = "获取所有动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Integer>> getAllDynamicRateLimits() {
        try {
            Map<String, Integer> configs = rateLimitService.getAllDynamicRateLimits();
            return ApiResponse.success(configs);
        } catch (Exception e) {
            log.error("获取所有动态限流配置失败", e);
            return ApiResponse.error("获取失败");
        }
    }

    /**
     * 获取限流统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取限流统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getRateLimitStatistics() {
        try {
            Map<String, Object> stats = rateLimitService.getRateLimitStatistics();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取限流统计信息失败", e);
            return ApiResponse.error("获取失败");
        }
    }

    /**
     * 获取限流事件列表
     */
    @GetMapping("/events")
    @Operation(summary = "获取限流事件列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getRateLimitEvents(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<Map<String, Object>> events = rateLimitService.getRateLimitEvents(limit);
            return ApiResponse.success(events);
        } catch (Exception e) {
            log.error("获取限流事件列表失败", e);
            return ApiResponse.error("获取失败");
        }
    }

    /**
     * 获取限流告警列表
     */
    @GetMapping("/alerts")
    @Operation(summary = "获取限流告警列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getRateLimitAlerts() {
        try {
            List<Map<String, Object>> alerts = rateLimitService.getRateLimitAlerts();
            return ApiResponse.success(alerts);
        } catch (Exception e) {
            log.error("获取限流告警列表失败", e);
            return ApiResponse.error("获取失败");
        }
    }

    /**
     * 清除限流统计
     */
    @DeleteMapping("/statistics")
    @Operation(summary = "清除限流统计")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> clearRateLimitStatistics() {
        try {
            boolean success = rateLimitService.clearRateLimitStatistics();
            return success ? ApiResponse.success("清除成功", true) : ApiResponse.error("清除失败");
        } catch (Exception e) {
            log.error("清除限流统计失败", e);
            return ApiResponse.error("清除失败");
        }
    }

    /**
     * 手动解除限流
     */
    @PostMapping("/unlock")
    @Operation(summary = "手动解除限流")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> unlockRateLimit(
            @RequestParam String identifier,
            @RequestParam String path,
            @RequestParam String type) {
        try {
            boolean success = rateLimitService.unlockRateLimit(identifier, path, type);
            return success ? ApiResponse.success("解除成功", true) : ApiResponse.error("解除失败");
        } catch (Exception e) {
            log.error("解除限流失败", e);
            return ApiResponse.error("解除失败");
        }
    }
}
