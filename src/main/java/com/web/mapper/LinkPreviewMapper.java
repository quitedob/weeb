package com.web.mapper;

import com.web.model.LinkPreview;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 链接预览Mapper接口
 */
@Mapper
public interface LinkPreviewMapper {

    /**
     * 插入链接预览
     */
    @Insert("INSERT INTO link_previews (original_url, final_url, title, description, site_name, " +
            "site_icon, image_url, image_width, image_height, content_type, status, error_message, " +
            "generation_time, expires_at, og_tags, twitter_tags, message_id, created_by, " +
            "is_disabled, user_rating, metadata, created_at, updated_at) " +
            "VALUES (#{originalUrl}, #{finalUrl}, #{title}, #{description}, #{siteName}, " +
            "#{siteIcon}, #{imageUrl}, #{imageWidth}, #{imageHeight}, #{contentType}, #{status}, " +
            "#{errorMessage}, #{generationTime}, #{expiresAt}, #{ogTags}, #{twitterTags}, " +
            "#{messageId}, #{createdBy}, #{isDisabled}, #{userRating}, #{metadata}, " +
            "#{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LinkPreview preview);

    /**
     * 更新链接预览
     */
    @Update("UPDATE link_previews SET final_url = #{finalUrl}, title = #{title}, " +
            "description = #{description}, site_name = #{siteName}, site_icon = #{siteIcon}, " +
            "image_url = #{imageUrl}, image_width = #{imageWidth}, image_height = #{imageHeight}, " +
            "content_type = #{contentType}, status = #{status}, error_message = #{errorMessage}, " +
            "generation_time = #{generationTime}, expires_at = #{expiresAt}, og_tags = #{ogTags}, " +
            "twitter_tags = #{twitterTags}, is_disabled = #{isDisabled}, user_rating = #{userRating}, " +
            "metadata = #{metadata}, updated_at = #{updatedAt} WHERE id = #{id}")
    int update(LinkPreview preview);

    /**
     * 根据ID查找预览
     */
    @Select("SELECT * FROM link_previews WHERE id = #{previewId}")
    LinkPreview findById(Long previewId);

    /**
     * 根据消息ID查找预览
     */
    @Select("SELECT * FROM link_previews WHERE message_id = #{messageId} ORDER BY created_at ASC")
    List<LinkPreview> findByMessageId(Long messageId);

    /**
     * 根据用户ID查找预览
     */
    @Select("SELECT * FROM link_previews WHERE created_by = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<LinkPreview> findByUserId(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    /**
     * 统计用户创建的预览数量
     */
    @Select("SELECT COUNT(*) FROM link_previews WHERE created_by = #{userId}")
    int countByUserId(Long userId);

    /**
     * 根据状态统计用户预览数量
     */
    @Select("SELECT COUNT(*) FROM link_previews WHERE created_by = #{userId} AND status = #{status}")
    int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 根据URL和消息ID查找预览
     */
    @Select("SELECT * FROM link_previews WHERE original_url = #{url} AND message_id = #{messageId}")
    LinkPreview findByUrlAndMessage(@Param("url") String url, @Param("messageId") Long messageId);

    /**
     * 根据URL查找预览
     */
    @Select("SELECT * FROM link_previews WHERE original_url = #{url} " +
            "ORDER BY created_at DESC LIMIT 1")
    LinkPreview findByUrl(String url);

    /**
     * 搜索预览
     */
    @Select("SELECT * FROM link_previews WHERE " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "description LIKE CONCAT('%', #{keyword}, '%') OR " +
            "site_name LIKE CONCAT('%', #{keyword}, '%') OR " +
            "original_url LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<LinkPreview> searchPreviews(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    /**
     * 搜索预览总数
     */
    @Select("SELECT COUNT(*) FROM link_previews WHERE " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR " +
            "description LIKE CONCAT('%', #{keyword}, '%') OR " +
            "site_name LIKE CONCAT('%', #{keyword}, '%') OR " +
            "original_url LIKE CONCAT('%', #{keyword}, '%'))")
    int searchPreviewsCount(String keyword);

    /**
     * 删除预览
     */
    @Delete("DELETE FROM link_previews WHERE id = #{previewId}")
    int deleteById(Long previewId);

    /**
     * 获取热门域名
     */
    @Select("SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(original_url, '/', 3), '://', -1) as domain, " +
            "COUNT(*) as preview_count, " +
            "COUNT(CASE WHEN status = 'success' THEN 1 END) as success_count " +
            "FROM link_previews " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY domain " +
            "HAVING preview_count >= 5 " +
            "ORDER BY preview_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> findPopularDomains(@Param("limit") int limit);

    /**
     * 获取预览趋势
     */
    @Select("SELECT DATE(created_at) as date, " +
            "COUNT(*) as total_previews, " +
            "COUNT(CASE WHEN status = 'success' THEN 1 END) as successful_previews, " +
            "COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_previews, " +
            "AVG(generation_time) as avg_generation_time " +
            "FROM link_previews " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> findPreviewTrends(@Param("days") int days);

    /**
     * 删除过期预览
     */
    @Delete("DELETE FROM link_previews WHERE expires_at < NOW() OR status = 'failed'")
    int deleteExpiredPreviews();

    /**
     * 获取内容类型统计
     */
    @Select("SELECT content_type, COUNT(*) as count, " +
            "COUNT(CASE WHEN status = 'success' THEN 1 END) as success_count " +
            "FROM link_previews " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY content_type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getContentTypeStats();

    /**
     * 获取用户评分统计
     */
    @Select("SELECT user_rating, COUNT(*) as count " +
            "FROM link_previews " +
            "WHERE user_rating IS NOT NULL " +
            "GROUP BY user_rating " +
            "ORDER BY user_rating DESC")
    List<Map<String, Object>> getUserRatingStats();

    /**
     * 获取生成时间统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_previews, " +
            "AVG(generation_time) as avg_generation_time, " +
            "MIN(generation_time) as min_generation_time, " +
            "MAX(generation_time) as max_generation_time, " +
            "COUNT(CASE WHEN generation_time > 5000 THEN 1 END) as slow_previews " +
            "FROM link_previews " +
            "WHERE generation_time IS NOT NULL " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    Map<String, Object> getGenerationTimeStats();

    /**
     * 获取失败预览统计
     */
    @Select("SELECT " +
            "error_message, COUNT(*) as count " +
            "FROM link_previews " +
            "WHERE status = 'failed' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY error_message " +
            "ORDER BY count DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getFailureReasonStats();

    /**
     * 获取用户预览活动
     */
    @Select("SELECT lp.*, m.content as message_content " +
            "FROM link_previews lp " +
            "INNER JOIN messages m ON lp.message_id = m.id " +
            "WHERE lp.created_by = #{userId} " +
            "ORDER BY lp.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getUserPreviewActivity(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取域名预览历史
     */
    @Select("SELECT * FROM link_previews " +
            "WHERE original_url LIKE CONCAT('http://', #{domain}, '%') " +
            "   OR original_url LIKE CONCAT('https://', #{domain}, '%') " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<LinkPreview> getDomainPreviewHistory(@Param("domain") String domain, @Param("limit") int limit);

    /**
     * 批量更新预览状态
     */
    @Update("UPDATE link_previews SET status = #{status}, updated_at = NOW() " +
            "WHERE id IN #{previewIds}")
    int batchUpdateStatus(@Param("previewIds") List<Long> previewIds, @Param("status") String status);

    /**
     * 获取待生成的预览
     */
    @Select("SELECT * FROM link_previews " +
            "WHERE status = 'pending' " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<LinkPreview> getPendingPreviews(@Param("limit") int limit);

    /**
     * 获取重复的预览
     */
    @Select("SELECT original_url, COUNT(*) as duplicate_count " +
            "FROM link_previews " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY original_url " +
            "HAVING duplicate_count > 1 " +
            "ORDER BY duplicate_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getDuplicatePreviews(@Param("limit") int limit);

    /**
     * 清理失败预览
     */
    @Delete("DELETE FROM link_previews " +
            "WHERE status = 'failed' " +
            "AND created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    int cleanupFailedPreviews();

    /**
     * 获取预览缓存统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_previews, " +
            "COUNT(CASE WHEN expires_at > NOW() THEN 1 END) as valid_previews, " +
            "COUNT(CASE WHEN expires_at <= NOW() THEN 1 END) as expired_previews " +
            "FROM link_previews")
    Map<String, Object> getCacheStats();
}