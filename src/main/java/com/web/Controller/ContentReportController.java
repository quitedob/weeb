package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.ContentReport;
import com.web.service.ContentReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 内容举报控制器
 */
@RestController
@RequestMapping("/api/content-reports")
public class ContentReportController {

    private static final Logger logger = LoggerFactory.getLogger(ContentReportController.class);

    @Autowired
    private ContentReportService contentReportService;

    /**
     * 创建内容举报
     * POST /api/content-reports
     */
    @PostMapping
    @PreAuthorize("hasPermission(null, 'CONTENT_REPORT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<ContentReport>> createReport(
            @RequestBody @Valid ContentReport report,
            @Userid Long userId) {
        try {
            // 设置举报人ID
            report.setReporterId(userId);

            // 检查用户是否可以举报该内容
            if (!contentReportService.canUserReportContent(userId, report.getContentType(), report.getContentId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("您已经举报过此内容"));
            }

            ContentReport createdReport = contentReportService.createReport(report);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("举报成功", createdReport));
        } catch (Exception e) {
            logger.error("创建内容举报失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("举报失败: " + e.getMessage()));
        }
    }

    /**
     * 获取待处理的举报列表
     * GET /api/content-reports/pending?page=1&pageSize=20&contentType=article&reason=spam
     */
    @GetMapping("/pending")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Boolean isUrgent) {
        try {
            Map<String, Object> result = contentReportService.getPendingReports(page, pageSize, contentType, reason, isUrgent);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("获取待处理举报列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取举报列表失败: " + e.getMessage()));
        }
    }

    /**
     * 处理举报
     * PUT /api/content-reports/{reportId}/process
     */
    @PutMapping("/{reportId}/process")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<String>> processReport(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> processRequest,
            @Userid Long reviewerId) {
        try {
            String action = (String) processRequest.get("action");
            String reviewNote = (String) processRequest.get("reviewNote");

            if (action == null || action.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("处理结果不能为空"));
            }

            boolean success = contentReportService.processReport(reportId, reviewerId, action, reviewNote);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("举报处理成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("举报处理失败"));
            }
        } catch (Exception e) {
            logger.error("处理举报失败: reportId={}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("处理举报失败: " + e.getMessage()));
        }
    }

    /**
     * 批量处理相同内容的举报
     * PUT /api/content-reports/batch-process
     */
    @PutMapping("/batch-process")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<String>> batchProcessReports(
            @RequestBody Map<String, Object> batchRequest,
            @Userid Long reviewerId) {
        try {
            String contentType = (String) batchRequest.get("contentType");
            Long contentId = Long.valueOf(batchRequest.get("contentId").toString());
            String action = (String) batchRequest.get("action");
            String reviewNote = (String) batchRequest.get("reviewNote");

            int processedCount = contentReportService.batchProcessContentReports(
                    contentType, contentId, reviewerId, action, reviewNote);

            return ResponseEntity.ok(ApiResponse.success("批量处理成功，处理了 " + processedCount + " 条举报"));
        } catch (Exception e) {
            logger.error("批量处理举报失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("批量处理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户举报历史
     * GET /api/content-reports/my-reports?page=1&pageSize=20
     */
    @GetMapping("/my-reports")
    @PreAuthorize("hasPermission(null, 'CONTENT_REPORT_READ_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserReportHistory(
            @Userid Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = contentReportService.getUserReportHistory(userId, page, pageSize);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("获取用户举报历史失败: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取举报历史失败: " + e.getMessage()));
        }
    }

    /**
     * 获取内容举报详情
     * GET /api/content-reports/content/{contentType}/{contentId}
     */
    @GetMapping("/content/{contentType}/{contentId}")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getContentReportDetails(
            @PathVariable String contentType,
            @PathVariable Long contentId) {
        try {
            List<Map<String, Object>> details = contentReportService.getContentReportDetails(contentType, contentId);
            return ResponseEntity.ok(ApiResponse.success(details));
        } catch (Exception e) {
            logger.error("获取内容举报详情失败: contentType={}, contentId={}", contentType, contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取举报详情失败: " + e.getMessage()));
        }
    }

    /**
     * 获取举报统计信息
     * GET /api/content-reports/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportStatistics() {
        try {
            Map<String, Object> statistics = contentReportService.getReportStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            logger.error("获取举报统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门举报内容
     * GET /api/content-reports/top-reported?limit=10
     */
    @GetMapping("/top-reported")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopReportedContent(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> topContent = contentReportService.getTopReportedContent(limit);
            return ResponseEntity.ok(ApiResponse.success(topContent));
        } catch (Exception e) {
            logger.error("获取热门举报内容失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取热门内容失败: " + e.getMessage()));
        }
    }

    /**
     * 获取管理员处理统计
     * GET /api/content-reports/reviewer-stats?reviewerId=123&days=7
     */
    @GetMapping("/reviewer-stats")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReviewerStats(
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<Map<String, Object>> stats = contentReportService.getReviewerStats(reviewerId, days);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("获取管理员处理统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取处理统计失败: " + e.getMessage()));
        }
    }

    /**
     * 撤销举报
     * PUT /api/content-reports/{reportId}/withdraw
     */
    @PutMapping("/{reportId}/withdraw")
    @PreAuthorize("hasPermission(null, 'CONTENT_REPORT_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<String>> withdrawReport(
            @PathVariable Long reportId,
            @Userid Long userId) {
        try {
            boolean success = contentReportService.withdrawReport(reportId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("举报撤销成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("举报撤销失败"));
            }
        } catch (Exception e) {
            logger.error("撤销举报失败: reportId={}, userId={}", reportId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("撤销举报失败: " + e.getMessage()));
        }
    }

    /**
     * 标记举报为紧急
     * PUT /api/content-reports/{reportId}/mark-urgent
     */
    @PutMapping("/{reportId}/mark-urgent")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<String>> markReportAsUrgent(
            @PathVariable Long reportId,
            @Userid Long reviewerId) {
        try {
            boolean success = contentReportService.markReportAsUrgent(reportId, reviewerId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("已标记为紧急举报"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("标记失败"));
            }
        } catch (Exception e) {
            logger.error("标记举报为紧急失败: reportId={}, reviewerId={}", reportId, reviewerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("标记失败: " + e.getMessage()));
        }
    }

    /**
     * 获取举报详情
     * GET /api/content-reports/{reportId}
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasPermission(null, 'CONTENT_REVIEW_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportDetails(@PathVariable Long reportId) {
        try {
            Map<String, Object> details = contentReportService.getReportDetails(reportId);
            return ResponseEntity.ok(ApiResponse.success(details));
        } catch (Exception e) {
            logger.error("获取举报详情失败: reportId={}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("获取举报详情失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否可以举报指定内容
     * GET /api/content-reports/can-report?contentType=article&contentId=123
     */
    @GetMapping("/can-report")
    @PreAuthorize("hasPermission(null, 'CONTENT_REPORT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canUserReportContent(
            @RequestParam String contentType,
            @RequestParam Long contentId,
            @Userid Long userId) {
        try {
            boolean canReport = contentReportService.canUserReportContent(userId, contentType, contentId);
            Map<String, Object> result = Map.of(
                    "canReport", canReport,
                    "message", canReport ? "可以举报" : "您已经举报过此内容"
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("检查举报权限失败: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.systemError("检查举报权限失败: " + e.getMessage()));
        }
    }
}