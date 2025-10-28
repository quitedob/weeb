package com.web.Controller;

import com.web.service.RateLimitService;
import com.web.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "限流管理")
public class RateLimitController {

    @Autowired
    private RateLimitService rateLimitService;

    /**
     * 设置动态限流配置
     */
    @PostMapping("/config")
    @ApiOperation("设置动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> setDynamicRateLimit(
            @RequestParam String path,
            @RequestParam int maxRequests) {
        try {
            boolean success = rateLimitService.setDynamicRateLimit(path, maxRequests);
            return success ? Result.success(true, "配置成功") : Result.error("配置失败");
        } catch (Exception e) {
            log.error("设置动态限流配置失败", e);
            return Result.error("设置失败");
        }
    }

    /**
     * 获取动态限流配置
     */
    @GetMapping("/config")
    @ApiOperation("获取动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> getDynamicRateLimit(@RequestParam String path) {
        try {
            Integer maxRequests = rateLimitService.getDynamicRateLimit(path);
            return maxRequests != null ? Result.success(maxRequests) : Result.error("配置不存在");
        } catch (Exception e) {
            log.error("获取动态限流配置失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 删除动态限流配置
     */
    @DeleteMapping("/config")
    @ApiOperation("删除动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> removeDynamicRateLimit(@RequestParam String path) {
        try {
            boolean success = rateLimitService.removeDynamicRateLimit(path);
            return success ? Result.success(true, "删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除动态限流配置失败", e);
            return Result.error("删除失败");
        }
    }

    /**
     * 获取所有动态限流配置
     */
    @GetMapping("/config/all")
    @ApiOperation("获取所有动态限流配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Integer>> getAllDynamicRateLimits() {
        try {
            Map<String, Integer> configs = rateLimitService.getAllDynamicRateLimits();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取所有动态限流配置失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取限流统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取限流统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getRateLimitStatistics() {
        try {
            Map<String, Object> stats = rateLimitService.getRateLimitStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取限流统计信息失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取限流事件列表
     */
    @GetMapping("/events")
    @ApiOperation("获取限流事件列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getRateLimitEvents(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<Map<String, Object>> events = rateLimitService.getRateLimitEvents(limit);
            return Result.success(events);
        } catch (Exception e) {
            log.error("获取限流事件列表失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 获取限流告警列表
     */
    @GetMapping("/alerts")
    @ApiOperation("获取限流告警列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getRateLimitAlerts() {
        try {
            List<Map<String, Object>> alerts = rateLimitService.getRateLimitAlerts();
            return Result.success(alerts);
        } catch (Exception e) {
            log.error("获取限流告警列表失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 清除限流统计
     */
    @DeleteMapping("/statistics")
    @ApiOperation("清除限流统计")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> clearRateLimitStatistics() {
        try {
            boolean success = rateLimitService.clearRateLimitStatistics();
            return success ? Result.success(true, "清除成功") : Result.error("清除失败");
        } catch (Exception e) {
            log.error("清除限流统计失败", e);
            return Result.error("清除失败");
        }
    }

    /**
     * 手动解除限流
     */
    @PostMapping("/unlock")
    @ApiOperation("手动解除限流")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> unlockRateLimit(
            @RequestParam String identifier,
            @RequestParam String path,
            @RequestParam String type) {
        try {
            boolean success = rateLimitService.unlockRateLimit(identifier, path, type);
            return success ? Result.success(true, "解除成功") : Result.error("解除失败");
        } catch (Exception e) {
            log.error("解除限流失败", e);
            return Result.error("解除失败");
        }
    }
}
