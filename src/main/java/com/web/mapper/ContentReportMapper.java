package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.ContentReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 内容举报Mapper接口
 */
@Mapper
public interface ContentReportMapper extends BaseMapper<ContentReport> {

    /**
     * 获取待处理的举报列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param contentType 内容类型过滤（可选）
     * @param reason 举报原因过滤（可选）
     * @return 举报列表
     */
    @Select("<script>" +
            "SELECT cr.*, u1.username as reporter_name, u2.username as reviewer_name " +
            "FROM content_report cr " +
            "LEFT JOIN `user` u1 ON cr.reporter_id = u1.id " +
            "LEFT JOIN `user` u2 ON cr.reviewer_id = u2.id " +
            "WHERE cr.status = 'pending' " +
            "<if test='contentType != null and contentType != \"\"'>" +
            "AND cr.content_type = #{contentType} " +
            "</if>" +
            "<if test='reason != null and reason != \"\"'>" +
            "AND cr.reason = #{reason} " +
            "</if>" +
            "<if test='isUrgent != null'>" +
            "AND cr.is_urgent = #{isUrgent} " +
            "</if>" +
            "ORDER BY cr.is_urgent DESC, cr.created_at ASC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Map<String, Object>> selectPendingReports(
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("contentType") String contentType,
            @Param("reason") String reason,
            @Param("isUrgent") Boolean isUrgent
    );

    /**
     * 统计待处理的举报数量
     * @return 待处理举报数量
     */
    @Select("SELECT COUNT(*) FROM content_report WHERE status = 'pending'")
    int countPendingReports();

    /**
     * 统计按内容类型分组的举报数量
     * @return 分组统计结果
     */
    @Select("SELECT content_type, COUNT(*) as count " +
            "FROM content_report " +
            "WHERE status = 'pending' " +
            "GROUP BY content_type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> countReportsByContentType();

    /**
     * 统计按举报原因分组的举报数量
     * @return 分组统计结果
     */
    @Select("SELECT reason, COUNT(*) as count " +
            "FROM content_report " +
            "WHERE status = 'pending' " +
            "GROUP BY reason " +
            "ORDER BY count DESC")
    List<Map<String, Object>> countReportsByReason();

    /**
     * 检查用户是否已经举报过指定内容
     * @param reporterId 举报人ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否已举报
     */
    @Select("SELECT COUNT(*) > 0 FROM content_report " +
            "WHERE reporter_id = #{reporterId} " +
            "AND content_type = #{contentType} " +
            "AND content_id = #{contentId} " +
            "AND status != 'dismissed'")
    boolean hasUserReportedContent(
            @Param("reporterId") Long reporterId,
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId
    );

    /**
     * 获取指定内容的举报列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 举报列表
     */
    @Select("SELECT cr.*, u.username as reporter_name " +
            "FROM content_report cr " +
            "LEFT JOIN `user` u ON cr.reporter_id = u.id " +
            "WHERE cr.content_type = #{contentType} " +
            "AND cr.content_id = #{contentId} " +
            "ORDER BY cr.created_at DESC")
    List<Map<String, Object>> selectReportsByContent(
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId
    );

    /**
     * 获取用户举报历史
     * @param reporterId 举报人ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 举报历史
     */
    @Select("SELECT cr.*, " +
            "CASE cr.content_type " +
            "  WHEN 'article' THEN (SELECT title FROM articles WHERE id = cr.content_id) " +
            "  WHEN 'comment' THEN (SELECT SUBSTRING(content, 1, 50) FROM article_comment WHERE id = cr.content_id) " +
            "  WHEN 'message' THEN (SELECT SUBSTRING(msg_content, 1, 50) FROM message WHERE id = cr.content_id) " +
            "  ELSE 'Unknown Content' " +
            "END as content_preview " +
            "FROM content_report cr " +
            "WHERE cr.reporter_id = #{reporterId} " +
            "ORDER BY cr.created_at DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> selectUserReportHistory(
            @Param("reporterId") Long reporterId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 更新举报状态和处理结果
     * @param reportId 举报ID
     * @param status 新状态
     * @param reviewerId 处理人ID
     * @param action 处理结果
     * @param reviewNote 处理说明
     * @return 更新行数
     */
    @Update("UPDATE content_report SET " +
            "status = #{status}, " +
            "reviewer_id = #{reviewerId}, " +
            "action = #{action}, " +
            "review_note = #{reviewNote}, " +
            "reviewed_at = #{reviewedAt}, " +
            "updated_at = NOW() " +
            "WHERE id = #{reportId}")
    int updateReportStatus(
            @Param("reportId") Long reportId,
            @Param("status") String status,
            @Param("reviewerId") Long reviewerId,
            @Param("action") String action,
            @Param("reviewNote") String reviewNote,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    /**
     * 批量更新相同内容的举报状态
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param status 新状态
     * @param reviewerId 处理人ID
     * @param action 处理结果
     * @param reviewNote 处理说明
     * @return 更新行数
     */
    @Update("UPDATE content_report SET " +
            "status = #{status}, " +
            "reviewer_id = #{reviewerId}, " +
            "action = #{action}, " +
            "review_note = #{reviewNote}, " +
            "reviewed_at = #{reviewedAt}, " +
            "updated_at = NOW() " +
            "WHERE content_type = #{contentType} " +
            "AND content_id = #{contentId} " +
            "AND status = 'pending'")
    int batchUpdateContentReports(
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId,
            @Param("status") String status,
            @Param("reviewerId") Long reviewerId,
            @Param("action") String action,
            @Param("reviewNote") String reviewNote,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );

    /**
     * 增加内容的举报计数
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 更新行数
     */
    @Update("UPDATE content_report SET report_count = report_count + 1, updated_at = NOW() " +
            "WHERE content_type = #{contentType} " +
            "AND content_id = #{contentId}")
    int incrementReportCount(
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId
    );

    /**
     * 获取热门举报内容（被举报次数最多的）
     * @param limit 限制数量
     * @return 热门举报内容
     */
    @Select("SELECT content_type, content_id, COUNT(*) as report_count " +
            "FROM content_report " +
            "WHERE status = 'pending' " +
            "GROUP BY content_type, content_id " +
            "ORDER BY report_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopReportedContent(@Param("limit") int limit);

    /**
     * 获取管理员处理统计
     * @param reviewerId 管理员ID（可选）
     * @param days 天数
     * @return 处理统计
     */
    @Select("<script>" +
            "SELECT reviewer_id, u.username, " +
            "COUNT(*) as total_handled, " +
            "SUM(CASE WHEN action = 'remove_content' THEN 1 ELSE 0 END) as removals, " +
            "SUM(CASE WHEN action = 'warn_user' THEN 1 ELSE 0 END) as warnings, " +
            "SUM(CASE WHEN action = 'ban_user' THEN 1 ELSE 0 END) as bans " +
            "FROM content_report cr " +
            "LEFT JOIN `user` u ON cr.reviewer_id = u.id " +
            "WHERE cr.reviewed_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "<if test='reviewerId != null'>" +
            "AND cr.reviewer_id = #{reviewerId} " +
            "</if>" +
            "GROUP BY reviewer_id, u.username " +
            "ORDER BY total_handled DESC" +
            "</script>")
    List<Map<String, Object>> selectReviewerStats(
            @Param("reviewerId") Long reviewerId,
            @Param("days") int days
    );
}