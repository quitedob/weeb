package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.ArticleVersion;
import com.web.service.ArticleVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 文章版本控制器
 */
@RestController
@RequestMapping("/api/articles/{articleId}/versions")
public class ArticleVersionController {

    private static final Logger logger = LoggerFactory.getLogger(ArticleVersionController.class);

    @Autowired
    private ArticleVersionService articleVersionService;

    /**
     * 获取文章的所有版本
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
     * 获取文章的指定版本
     * GET /api/articles/{articleId}/versions/{versionNumber}
     */
    @GetMapping("/{versionNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getArticleVersion(
            @PathVariable Long articleId,
            @PathVariable Integer versionNumber) {
        try {
            Map<String, Object> version = articleVersionService.getArticleVersion(articleId, versionNumber);
            return ResponseEntity.ok(ApiResponse.success(version));
        } catch (Exception e) {
            logger.error("获取文章版本失败: articleId={}, version={}", articleId, versionNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取版本失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文章的最新版本
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
     * 获取文章的最新发布版本
     * GET /api/articles/{articleId}/versions/latest-published
     */
    @GetMapping("/latest-published")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestPublishedVersion(
            @PathVariable Long articleId) {
        try {
            Map<String, Object> version = articleVersionService.getLatestPublishedVersion(articleId);
            return ResponseEntity.ok(ApiResponse.success(version));
        } catch (Exception e) {
            logger.error("获取文章最新发布版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取最新发布版本失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文章的主要版本列表
     * GET /api/articles/{articleId}/versions/major
     */
    @GetMapping("/major")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMajorVersions(
            @PathVariable Long articleId) {
        try {
            List<Map<String, Object>> versions = articleVersionService.getMajorVersions(articleId);
            return ResponseEntity.ok(ApiResponse.success(versions));
        } catch (Exception e) {
            logger.error("获取文章主要版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取主要版本失败: " + e.getMessage()));
        }
    }

    /**
     * 回滚文章到指定版本
     * POST /api/articles/{articleId}/versions/{versionNumber}/rollback
     */
    @PostMapping("/{versionNumber}/rollback")
    public ResponseEntity<ApiResponse<String>> rollbackToVersion(
            @PathVariable Long articleId,
            @PathVariable Integer versionNumber,
            @RequestBody Map<String, String> rollbackRequest,
            @Userid Long userId) {
        try {
            String rollbackNote = rollbackRequest.getOrDefault("rollbackNote", "回滚到版本 " + versionNumber);

            boolean success = articleVersionService.rollbackToVersion(articleId, versionNumber, userId, rollbackNote);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("文章回滚成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("文章回滚失败"));
            }
        } catch (Exception e) {
            logger.error("文章回滚失败: articleId={}, version={}", articleId, versionNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("文章回滚失败: " + e.getMessage()));
        }
    }

    /**
     * 自动保存文章版本
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
     * 创建主要版本
     * POST /api/articles/{articleId}/versions/major
     */
    @PostMapping("/major")
    public ResponseEntity<ApiResponse<Integer>> createMajorVersion(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> versionRequest,
            @Userid Long userId) {
        try {
            String title = versionRequest.get("title");
            String content = versionRequest.get("content");
            String changeNote = versionRequest.getOrDefault("changeNote", "创建主要版本");

            if (title == null || content == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("标题和内容不能为空"));
            }

            Integer versionNumber = articleVersionService.createMajorVersion(articleId, title, content, userId, changeNote);
            if (versionNumber != null) {
                return ResponseEntity.ok(ApiResponse.success("创建主要版本成功", versionNumber));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("创建主要版本失败"));
            }
        } catch (Exception e) {
            logger.error("创建主要版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("创建主要版本失败: " + e.getMessage()));
        }
    }

    /**
     * 比较两个版本
     * GET /api/articles/{articleId}/versions/compare?from=1&to=2
     */
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareVersions(
            @PathVariable Long articleId,
            @RequestParam Integer from,
            @RequestParam Integer to) {
        try {
            Map<String, Object> comparison = articleVersionService.compareVersions(articleId, from, to);
            return ResponseEntity.ok(ApiResponse.success(comparison));
        } catch (Exception e) {
            logger.error("比较版本失败: articleId={}, from={}, to={}", articleId, from, to, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("比较版本失败: " + e.getMessage()));
        }
    }

    /**
     * 获取版本统计信息
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

    /**
     * 清理旧版本
     * DELETE /api/articles/{articleId}/versions/cleanup?keepCount=10
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupOldVersions(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "10") int keepCount) {
        try {
            int deletedCount = articleVersionService.cleanupOldVersions(articleId, keepCount);
            return ResponseEntity.ok(ApiResponse.success("清理完成，删除了 " + deletedCount + " 个旧版本"));
        } catch (Exception e) {
            logger.error("清理旧版本失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("清理旧版本失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索文章版本
     * GET /api/articles/{articleId}/versions/search?keyword=example
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchVersions(
            @PathVariable Long articleId,
            @RequestParam String keyword) {
        try {
            if (keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("搜索关键词不能为空"));
            }

            List<Map<String, Object>> versions = articleVersionService.searchVersions(articleId, keyword.trim());
            return ResponseEntity.ok(ApiResponse.success(versions));
        } catch (Exception e) {
            logger.error("搜索版本失败: articleId={}, keyword={}", articleId, keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("搜索版本失败: " + e.getMessage()));
        }
    }

    /**
     * 获取自动保存的版本
     * GET /api/articles/{articleId}/versions/auto-saves?hours=24
     */
    @GetMapping("/auto-saves")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAutoSaveVersions(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<Map<String, Object>> versions = articleVersionService.getAutoSaveVersions(articleId, hours);
            return ResponseEntity.ok(ApiResponse.success(versions));
        } catch (Exception e) {
            logger.error("获取自动保存版本失败: articleId={}, hours={}", articleId, hours, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取自动保存版本失败: " + e.getMessage()));
        }
    }

    /**
     * 获取版本变更统计
     * GET /api/articles/{articleId}/versions/change-stats
     */
    @GetMapping("/change-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChangeStatistics(
            @PathVariable Long articleId) {
        try {
            List<Map<String, Object>> statistics = articleVersionService.getChangeStatistics(articleId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            logger.error("获取变更统计失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取变更统计失败: " + e.getMessage()));
        }
    }

    /**
     * 导出版本历史
     * GET /api/articles/{articleId}/versions/export?format=json
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportVersionHistory(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "json") String format) {
        try {
            String exportData = articleVersionService.exportVersionHistory(articleId, format);

            // 设置Content-Type based on format
            String contentType;
            switch (format.toLowerCase()) {
                case "json":
                    contentType = "application/json";
                    break;
                case "xml":
                    contentType = "application/xml";
                    break;
                case "txt":
                    contentType = "text/plain";
                    break;
                default:
                    contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"article_" + articleId + "_versions." + format + "\"")
                    .body(exportData);
        } catch (Exception e) {
            logger.error("导出版本历史失败: articleId={}, format={}", articleId, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("导出版本历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取版本时间线
     * GET /api/articles/{articleId}/versions/timeline
     */
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVersionTimeline(
            @PathVariable Long articleId) {
        try {
            List<Map<String, Object>> timeline = articleVersionService.getVersionTimeline(articleId);
            return ResponseEntity.ok(ApiResponse.success(timeline));
        } catch (Exception e) {
            logger.error("获取版本时间线失败: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取版本时间线失败: " + e.getMessage()));
        }
    }
}