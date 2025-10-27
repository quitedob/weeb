package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.UserLevelHistory;
import com.web.service.UserLevelHistoryService;
import com.web.vo.userlevel.UserLevelHistoryQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户等级历史Controller
 * 提供用户等级变更历史的查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user-level-history")
public class UserLevelHistoryController {

    @Autowired
    private UserLevelHistoryService userLevelHistoryService;

    /**
     * 根据ID查询等级变更记录
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserLevelHistory>> getById(@PathVariable Long id) {
        try {
            UserLevelHistory history = userLevelHistoryService.getById(id);
            if (history == null) {
                return ResponseEntity.ok(ApiResponse.error("记录不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            log.error("查询等级变更记录失败: id={}", id, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户的等级变更历史
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Map<String, Object> result = userLevelHistoryService.getUserLevelHistory(userId, page, pageSize);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("查询用户等级历史失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 多条件查询等级变更历史
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryHistory(
            @Valid @RequestBody UserLevelHistoryQueryVo queryVo) {
        try {
            Map<String, Object> result = userLevelHistoryService.queryLevelHistory(queryVo);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("查询等级历史失败: queryVo={}", queryVo, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户最近的等级变更记录
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<ApiResponse<List<UserLevelHistory>>> getRecentHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<UserLevelHistory> histories = userLevelHistoryService.getRecentHistory(userId, limit);
            return ResponseEntity.ok(ApiResponse.success(histories));
        } catch (Exception e) {
            log.error("查询最近等级历史失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户当前等级
     */
    @GetMapping("/user/{userId}/current-level")
    public ResponseEntity<ApiResponse<Integer>> getCurrentLevel(@PathVariable Long userId) {
        try {
            Integer currentLevel = userLevelHistoryService.getCurrentLevel(userId);
            return ResponseEntity.ok(ApiResponse.success(currentLevel));
        } catch (Exception e) {
            log.error("获取用户当前等级失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户等级统计信息
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> stats = userLevelHistoryService.getUserLevelStats(userId, days);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取用户等级统计失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取等级提升记录
     */
    @GetMapping("/level-up")
    public ResponseEntity<ApiResponse<List<UserLevelHistory>>> getLevelUpRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<UserLevelHistory> records = userLevelHistoryService.getLevelUpRecords(
                    userId, startTime, endTime, limit);
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("查询等级提升记录失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取等级降低记录
     */
    @GetMapping("/level-down")
    public ResponseEntity<ApiResponse<List<UserLevelHistory>>> getLevelDownRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<UserLevelHistory> records = userLevelHistoryService.getLevelDownRecords(
                    userId, startTime, endTime, limit);
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("查询等级降低记录失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 统计用户等级变更次数
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse<Long>> countUserChanges(@PathVariable Long userId) {
        try {
            long count = userLevelHistoryService.countUserLevelChanges(userId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("统计用户等级变更次数失败: userId={}", userId, e);
            return ResponseEntity.ok(ApiResponse.error("统计失败: " + e.getMessage()));
        }
    }
}
