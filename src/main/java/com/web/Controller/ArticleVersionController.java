package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.ArticleVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文章版本控制器（简化版）
 * 只提供基本的版本查看和自动保存功能，移除复杂的版本操作
 */
@RestController
@RequestMapping("/api/articles/{articleId}/versions")
public class ArticleVersionController {

    private static final Logger logger = LoggerFactory.getLogger(ArticleVersionController.class);

    @Autowired
    private ArticleVersionService articleVersionService;

    /**
     * 获取文章的所有版本（简化版）
     * GET /api/articles/{articleId}/versions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getArticleVersions(
            @PathVariable Long articleId) {
        try {
            List<Map<String, Object>> versions = articleVersionService.getArticleVersions(articleId);
            return ResponseEntity.ok(ApiResponse.success(versions));
        } catch (Exception e) {
            logger.error("获取文章版本列表失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取版本列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文章的最新版本（简化版）
     * GET /api/articles/{articleId}/versions/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestVersion(
            @PathVariable Long articleId) {
        try {
            Map<String, Object> version = articleVersionService.getLatestVersion(articleId);
            return ResponseEntity.ok(ApiResponse.success(version));
        } catch (Exception e) {
            logger.error("获取文章最新版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取最新版本失败: " + e.getMessage()));
        }
    }

    /**
     * 自动保存文章版本（简化版）
     * POST /api/articles/{articleId}/versions/auto-save
     */
    @PostMapping("/auto-save")
    public ResponseEntity<ApiResponse<String>> autoSaveVersion(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> autoSaveRequest,
            @Userid Long userId) {
        try {
            String title = autoSaveRequest.get("title");
            String content = autoSaveRequest.get("content");

            if (title == null || content == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("标题和内容不能为空"));
            }

            boolean success = articleVersionService.autoSaveVersion(articleId, title, content, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("自动保存成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.success("内容未变化，无需保存"));
            }
        } catch (Exception e) {
            logger.error("自动保存版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("自动保存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取版本统计信息（简化版）
     * GET /api/articles/{articleId}/versions/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersionStatistics(
            @PathVariable Long articleId) {
        try {
            Map<String, Object> statistics = articleVersionService.getVersionStatistics(articleId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            logger.error("获取版本统计失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取统计信息失败: " + e.getMessage()));
        }
    }

    // ==================== 已移除的复杂接口 ====================
    // 以下接口因使用率低且复杂度高而被移除：
    // - 获取指定版本 (/{versionNumber})
    // - 获取最新发布版本 (/latest-published)
    // - 获取主要版本列表 (/major)
    // - 版本回滚 (/{versionNumber}/rollback)
    // - 创建主要版本 (/major)
    // - 版本比较 (/compare)
    // - 版本搜索 (/search)
    // - 版本导出 (/export)
    // - 版本时间线 (/timeline)
    // - 清理旧版本 (/cleanup)
}