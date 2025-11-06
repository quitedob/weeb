package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 诊断和维护控制器
 * 提供系统诊断和维护功能
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    @Autowired
    private RedisCacheService redisCacheService;

    /**
     * 清除所有用户统计信息缓存
     * 用于缓存结构变更后清理旧数据
     */
    @PostMapping("/cache/clear-user-stats")
    public ResponseEntity<ApiResponse<String>> clearUserStatsCache() {
        try {
            redisCacheService.evictAllUserStatsCache();
            log.info("手动清除所有用户统计信息缓存");
            return ResponseEntity.ok(ApiResponse.success("用户统计信息缓存已清除"));
        } catch (Exception e) {
            log.error("清除用户统计信息缓存失败", e);
            return ResponseEntity.ok(ApiResponse.error("清除缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("系统运行正常"));
    }
}
