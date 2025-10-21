package com.web.service;

import com.web.model.ContentReport;

import java.util.List;
import java.util.Map;

/**
 * 内容举报服务接口
 */
public interface ContentReportService {

    /**
     * 创建内容举报
     * @param report 举报对象
     * @return 创建的举报对象
     */
    ContentReport createReport(ContentReport report);

    /**
     * 获取待处理的举报列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param contentType 内容类型过滤（可选）
     * @param reason 举报原因过滤（可选）
     * @param isUrgent 是否只显示紧急举报
     * @return 举报列表和总数
     */
    Map<String, Object> getPendingReports(int page, int pageSize, String contentType, String reason, Boolean isUrgent);

    /**
     * 处理举报
     * @param reportId 举报ID
     * @param reviewerId 处理人ID
     * @param action 处理结果
     * @param reviewNote 处理说明
     * @return 是否处理成功
     */
    boolean processReport(Long reportId, Long reviewerId, String action, String reviewNote);

    /**
     * 批量处理相同内容的举报
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param reviewerId 处理人ID
     * @param action 处理结果
     * @param reviewNote 处理说明
     * @return 处理的举报数量
     */
    int batchProcessContentReports(String contentType, Long contentId, Long reviewerId, String action, String reviewNote);

    /**
     * 检查用户是否可以举报指定内容
     * @param reporterId 举报人ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否可以举报
     */
    boolean canUserReportContent(Long reporterId, String contentType, Long contentId);

    /**
     * 获取用户举报历史
     * @param reporterId 举报人ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 举报历史和总数
     */
    Map<String, Object> getUserReportHistory(Long reporterId, int page, int pageSize);

    /**
     * 获取指定内容的举报详情
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 举报详情列表
     */
    List<Map<String, Object>> getContentReportDetails(String contentType, Long contentId);

    /**
     * 获取举报统计信息
     * @return 统计信息
     */
    Map<String, Object> getReportStatistics();

    /**
     * 获取热门举报内容
     * @param limit 限制数量
     * @return 热门举报内容列表
     */
    List<Map<String, Object>> getTopReportedContent(int limit);

    /**
     * 获取管理员处理统计
     * @param reviewerId 管理员ID（可选，为空时返回所有管理员统计）
     * @param days 统计天数
     * @return 处理统计
     */
    List<Map<String, Object>> getReviewerStats(Long reviewerId, int days);

    /**
     * 撤销举报
     * @param reportId 举报ID
     * @param reporterId 举报人ID
     * @return 是否撤销成功
     */
    boolean withdrawReport(Long reportId, Long reporterId);

    /**
     * 将举报标记为紧急
     * @param reportId 举报ID
     * @param reviewerId 操作人ID
     * @return 是否标记成功
     */
    boolean markReportAsUrgent(Long reportId, Long reviewerId);

    /**
     * 获取举报详情
     * @param reportId 举报ID
     * @return 举报详情
     */
    Map<String, Object> getReportDetails(Long reportId);

    /**
     * 根据内容ID删除相关举报记录
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 删除的记录数
     */
    int deleteReportsByContent(String contentType, Long contentId);
}