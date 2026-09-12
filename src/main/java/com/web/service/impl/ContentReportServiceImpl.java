package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.ContentReportMapper;
import com.web.model.ContentReport;
import com.web.service.ContentReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容举报服务实现类
 */
@Slf4j
@Service
@Transactional
public class ContentReportServiceImpl implements ContentReportService {

    @Autowired
    private ContentReportMapper contentReportMapper;

    @Override
    public ContentReport createReport(ContentReport report) {
        try {
            // 检查用户是否已经举报过该内容
            if (contentReportMapper.hasUserReportedContent(report.getReporterId(), report.getContentType(), report.getContentId())) {
                throw new WeebException("您已经举报过此内容");
            }

            // 检查该内容是否已经有待处理的举报，如果有则增加计数
            List<Map<String, Object>> existingReports = contentReportMapper.selectReportsByContent(report.getContentType(), report.getContentId());
            boolean hasPendingReport = existingReports.stream()
                    .anyMatch(r -> "pending".equals(r.get("status")));

            if (hasPendingReport) {
                // 增加现有举报的计数
                contentReportMapper.incrementReportCount(report.getContentType(), report.getContentId());
                log.info("增加内容举报计数: contentType={}, contentId={}", report.getContentType(), report.getContentId());
                return report; // 返回原始报告对象，但不插入新记录
            }

            // 插入新的举报记录
            int result = contentReportMapper.insert(report);
            if (result > 0) {
                log.info("创建内容举报成功: reportId={}, reporterId={}, contentType={}, contentId={}",
                        report.getId(), report.getReporterId(), report.getContentType(), report.getContentId());

                // 如果是紧急举报，可以在这里发送通知给管理员
                if (Boolean.TRUE.equals(report.getIsUrgent())) {
                    sendUrgentReportNotification(report);
                }

                return report;
            } else {
                throw new WeebException("创建内容举报失败");
            }
        } catch (Exception e) {
            log.error("创建内容举报失败: reporterId={}, contentType={}, contentId={}",
                    report.getReporterId(), report.getContentType(), report.getContentId(), e);
            throw new WeebException("创建内容举报失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPendingReports(int page, int pageSize, String contentType, String reason, Boolean isUrgent) {
        try {
            int offset = (page - 1) * pageSize;
            List<Map<String, Object>> reports = contentReportMapper.selectPendingReports(offset, pageSize, contentType, reason, isUrgent);
            int totalCount = contentReportMapper.countPendingReports();

            Map<String, Object> result = new HashMap<>();
            result.put("list", reports);
            result.put("total", totalCount);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

            return result;
        } catch (Exception e) {
            log.error("获取待处理举报列表失败", e);
            throw new WeebException("获取待处理举报列表失败: " + e.getMessage());
        }
    }

    @Override
    public boolean processReport(Long reportId, Long reviewerId, String action, String reviewNote) {
        try {
            ContentReport report = contentReportMapper.selectById(reportId);
            if (report == null) {
                throw new WeebException("举报记录不存在");
            }

            if (!"pending".equals(report.getStatus())) {
                throw new WeebException("该举报已被处理");
            }

            // 更新举报状态
            int result = contentReportMapper.updateReportStatus(
                    reportId,
                    "resolved",
                    reviewerId,
                    action,
                    reviewNote,
                    LocalDateTime.now()
            );

            if (result > 0) {
                log.info("处理举报成功: reportId={}, reviewerId={}, action={}", reportId, reviewerId, action);

                // 执行相应的处理动作
                executeModerationAction(report, action, reviewerId);

                return true;
            } else {
                throw new WeebException("处理举报失败");
            }
        } catch (Exception e) {
            log.error("处理举报失败: reportId={}, reviewerId={}", reportId, reviewerId, e);
            throw new WeebException("处理举报失败: " + e.getMessage());
        }
    }

    @Override
    public int batchProcessContentReports(String contentType, Long contentId, Long reviewerId, String action, String reviewNote) {
        try {
            int result = contentReportMapper.batchUpdateContentReports(
                    contentType,
                    contentId,
                    "resolved",
                    reviewerId,
                    action,
                    reviewNote,
                    LocalDateTime.now()
            );

            if (result > 0) {
                log.info("批量处理举报成功: contentType={}, contentId={}, action={}, count={}",
                        contentType, contentId, action, result);

                // 执行相应的处理动作
                ContentReport sampleReport = new ContentReport();
                sampleReport.setContentType(contentType);
                sampleReport.setContentId(contentId);
                executeModerationAction(sampleReport, action, reviewerId);
            }

            return result;
        } catch (Exception e) {
            log.error("批量处理举报失败: contentType={}, contentId={}", contentType, contentId, e);
            throw new WeebException("批量处理举报失败: " + e.getMessage());
        }
    }

    @Override
    public boolean canUserReportContent(Long reporterId, String contentType, Long contentId) {
        try {
            // 检查用户是否已经举报过该内容
            return !contentReportMapper.hasUserReportedContent(reporterId, contentType, contentId);
        } catch (Exception e) {
            log.error("检查用户举报权限失败: reporterId={}, contentType={}, contentId={}",
                    reporterId, contentType, contentId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserReportHistory(Long reporterId, int page, int pageSize) {
        try {
            int offset = (page - 1) * pageSize;
            List<Map<String, Object>> reports = contentReportMapper.selectUserReportHistory(reporterId, offset, pageSize);

            // 计算总数（这里简化处理，实际应该有专门的count方法）
            int totalCount = reports.size();

            Map<String, Object> result = new HashMap<>();
            result.put("list", reports);
            result.put("total", totalCount);
            result.put("page", page);
            result.put("pageSize", pageSize);

            return result;
        } catch (Exception e) {
            log.error("获取用户举报历史失败: reporterId={}", reporterId, e);
            throw new WeebException("获取用户举报历史失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getContentReportDetails(String contentType, Long contentId) {
        try {
            return contentReportMapper.selectReportsByContent(contentType, contentId);
        } catch (Exception e) {
            log.error("获取内容举报详情失败: contentType={}, contentId={}", contentType, contentId, e);
            throw new WeebException("获取内容举报详情失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getReportStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 基础统计
            statistics.put("pendingCount", contentReportMapper.countPendingReports());
            statistics.put("reportsByContentType", contentReportMapper.countReportsByContentType());
            statistics.put("reportsByReason", contentReportMapper.countReportsByReason());
            statistics.put("topReportedContent", contentReportMapper.selectTopReportedContent(10));

            return statistics;
        } catch (Exception e) {
            log.error("获取举报统计失败", e);
            throw new WeebException("获取举报统计失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTopReportedContent(int limit) {
        try {
            return contentReportMapper.selectTopReportedContent(limit);
        } catch (Exception e) {
            log.error("获取热门举报内容失败", e);
            throw new WeebException("获取热门举报内容失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getReviewerStats(Long reviewerId, int days) {
        try {
            return contentReportMapper.selectReviewerStats(reviewerId, days);
        } catch (Exception e) {
            log.error("获取管理员处理统计失败", e);
            throw new WeebException("获取管理员处理统计失败: " + e.getMessage());
        }
    }

    @Override
    public boolean withdrawReport(Long reportId, Long reporterId) {
        try {
            ContentReport report = contentReportMapper.selectById(reportId);
            if (report == null) {
                throw new WeebException("举报记录不存在");
            }

            if (!report.getReporterId().equals(reporterId)) {
                throw new WeebException("您只能撤销自己的举报");
            }

            if (!"pending".equals(report.getStatus())) {
                throw new WeebException("只能撤销待处理的举报");
            }

            report.setStatus("dismissed");
            report.setUpdatedAt(LocalDateTime.now());

            int result = contentReportMapper.updateById(report);
            if (result > 0) {
                log.info("撤销举报成功: reportId={}, reporterId={}", reportId, reporterId);
                return true;
            } else {
                throw new WeebException("撤销举报失败");
            }
        } catch (Exception e) {
            log.error("撤销举报失败: reportId={}, reporterId={}", reportId, reporterId, e);
            throw new WeebException("撤销举报失败: " + e.getMessage());
        }
    }

    @Override
    public boolean markReportAsUrgent(Long reportId, Long reviewerId) {
        try {
            ContentReport report = contentReportMapper.selectById(reportId);
            if (report == null) {
                throw new WeebException("举报记录不存在");
            }

            report.setIsUrgent(true);
            report.setUpdatedAt(LocalDateTime.now());

            int result = contentReportMapper.updateById(report);
            if (result > 0) {
                log.info("标记举报为紧急成功: reportId={}, reviewerId={}", reportId, reviewerId);
                sendUrgentReportNotification(report);
                return true;
            } else {
                throw new WeebException("标记举报为紧急失败");
            }
        } catch (Exception e) {
            log.error("标记举报为紧急失败: reportId={}, reviewerId={}", reportId, reviewerId, e);
            throw new WeebException("标记举报为紧急失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getReportDetails(Long reportId) {
        try {
            ContentReport report = contentReportMapper.selectById(reportId);
            if (report == null) {
                throw new WeebException("举报记录不存在");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("report", report);
            details.put("contentDetails", getContentReportDetails(report.getContentType(), report.getContentId()));

            return details;
        } catch (Exception e) {
            log.error("获取举报详情失败: reportId={}", reportId, e);
            throw new WeebException("获取举报详情失败: " + e.getMessage());
        }
    }

    @Override
    public int deleteReportsByContent(String contentType, Long contentId) {
        try {
            // 这里应该实现逻辑删除，而不是物理删除
            // 将所有相关举报标记为已删除
            List<Map<String, Object>> reports = contentReportMapper.selectReportsByContent(contentType, contentId);
            int deletedCount = 0;

            for (Map<String, Object> reportData : reports) {
                ContentReport report = new ContentReport();
                report.setId(((Number) reportData.get("id")).longValue());
                report.setStatus("deleted");
                report.setUpdatedAt(LocalDateTime.now());

                if (contentReportMapper.updateById(report) > 0) {
                    deletedCount++;
                }
            }

            log.info("删除内容相关举报成功: contentType={}, contentId={}, count={}",
                    contentType, contentId, deletedCount);

            return deletedCount;
        } catch (Exception e) {
            log.error("删除内容相关举报失败: contentType={}, contentId={}", contentType, contentId, e);
            throw new WeebException("删除内容相关举报失败: " + e.getMessage());
        }
    }

    /**
     * 执行审核动作
     */
    private void executeModerationAction(ContentReport report, String action, Long reviewerId) {
        try {
            switch (action) {
                case ContentReport.Action.REMOVE_CONTENT:
                    removeContent(report.getContentType(), report.getContentId(), reviewerId);
                    break;
                case ContentReport.Action.WARN_USER:
                    warnUser(report.getReporterId(), reviewerId);
                    break;
                case ContentReport.Action.BAN_USER:
                    banUser(report.getReporterId(), reviewerId);
                    break;
                case ContentReport.Action.NO_ACTION:
                    // 无需处理
                    break;
                default:
                    log.warn("未知的处理动作: action={}", action);
            }
        } catch (Exception e) {
            log.error("执行审核动作失败: report={}, action={}", report, action, e);
        }
    }

    /**
     * 删除内容
     */
    private void removeContent(String contentType, Long contentId, Long reviewerId) {
        // 这里应该调用相应的服务来删除内容
        log.info("删除内容: contentType={}, contentId={}, reviewerId={}", contentType, contentId, reviewerId);
        // 实际实现中应该调用ArticleService, MessageService等
    }

    /**
     * 警告用户
     */
    private void warnUser(Long userId, Long reviewerId) {
        log.info("警告用户: userId={}, reviewerId={}", userId, reviewerId);
        // 实际实现中应该发送警告通知
    }

    /**
     * 封禁用户
     */
    private void banUser(Long userId, Long reviewerId) {
        log.info("封禁用户: userId={}, reviewerId={}", userId, reviewerId);
        // 实际实现中应该调用UserService来封禁用户
    }

    /**
     * 发送紧急举报通知
     */
    private void sendUrgentReportNotification(ContentReport report) {
        log.info("发送紧急举报通知: reportId={}, contentType={}, contentId={}",
                report.getId(), report.getContentType(), report.getContentId());
        // 实际实现中应该发送邮件、短信或站内信给管理员
    }
}