package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.ArticleVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文章版本Mapper接口
 */
@Mapper
public interface ArticleVersionMapper extends BaseMapper<ArticleVersion> {

    /**
     * 获取文章的所有版本
     * @param articleId 文章ID
     * @return 版本列表
     */
    @Select("SELECT av.*, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.article_id = #{articleId} " +
            "ORDER BY av.version_number DESC")
    List<Map<String, Object>> selectVersionsByArticleId(@Param("articleId") Long articleId);

    /**
     * 获取文章的指定版本
     * @param articleId 文章ID
     * @param versionNumber 版本号
     * @return 版本信息
     */
    @Select("SELECT av.*, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.article_id = #{articleId} AND av.version_number = #{versionNumber}")
    Map<String, Object> selectArticleVersion(@Param("articleId") Long articleId, @Param("versionNumber") Integer versionNumber);

    /**
     * 获取文章的最新版本号
     * @param articleId 文章ID
     * @return 最新版本号
     */
    @Select("SELECT MAX(version_number) FROM article_version WHERE article_id = #{articleId}")
    Integer selectLatestVersionNumber(@Param("articleId") Long articleId);

    /**
     * 获取文章的发布版本号
     * @param articleId 文章ID
     * @return 最新发布版本号
     */
    @Select("SELECT MAX(version_number) FROM article_version " +
            "WHERE article_id = #{articleId} AND status = 'published'")
    Integer selectLatestPublishedVersionNumber(@Param("articleId") Long articleId);

    /**
     * 获取文章版本总数
     * @param articleId 文章ID
     * @return 版本总数
     */
    @Select("SELECT COUNT(*) FROM article_version WHERE article_id = #{articleId}")
    int countVersionsByArticleId(@Param("articleId") Long articleId);

    /**
     * 获取文章的主要版本列表
     * @param articleId 文章ID
     * @return 主要版本列表
     */
    @Select("SELECT av.*, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.article_id = #{articleId} AND av.is_major_version = true " +
            "ORDER BY av.version_number DESC")
    List<Map<String, Object>> selectMajorVersionsByArticleId(@Param("articleId") Long articleId);

    /**
     * 检查内容是否已存在（通过内容哈希）
     * @param articleId 文章ID
     * @param contentHash 内容哈希
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM article_version " +
            "WHERE article_id = #{articleId} AND content_hash = #{contentHash}")
    boolean existsContentHash(@Param("articleId") Long articleId, @Param("contentHash") String contentHash);

    /**
     * 获取用户最近编辑的文章版本
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 版本列表
     */
    @Select("SELECT av.*, a.title as article_title, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN articles a ON av.article_id = a.id " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.created_by = #{userId} " +
            "ORDER BY av.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectRecentVersionsByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取指定时间范围内的版本统计
     * @param articleId 文章ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_versions, " +
            "COUNT(CASE WHEN is_major_version = true THEN 1 END) as major_versions, " +
            "COUNT(CASE WHEN is_auto_save = true THEN 1 END) as auto_save_versions, " +
            "MIN(created_at) as first_version_time, " +
            "MAX(created_at) as last_version_time " +
            "FROM article_version " +
            "WHERE article_id = #{articleId} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime}")
    Map<String, Object> selectVersionStatistics(
            @Param("articleId") Long articleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 清理旧版本（保留指定数量的最新版本）
     * @param articleId 文章ID
     * @param keepCount 保留数量
     * @return 删除的版本数量
     */
    @Update("DELETE FROM article_version " +
            "WHERE article_id = #{articleId} " +
            "AND id NOT IN (" +
            "  SELECT id FROM (" +
            "    SELECT id FROM article_version " +
            "    WHERE article_id = #{articleId} " +
            "    ORDER BY version_number DESC " +
            "    LIMIT #{keepCount}" +
            "  ) as recent_versions" +
            ")")
    int cleanupOldVersions(@Param("articleId") Long articleId, @Param("keepCount") int keepCount);

    /**
     * 批量删除文章的所有版本
     * @param articleId 文章ID
     * @return 删除的版本数量
     */
    @Update("DELETE FROM article_version WHERE article_id = #{articleId}")
    int deleteVersionsByArticleId(@Param("articleId") Long articleId);

    /**
     * 获取版本变更统计
     * @param articleId 文章ID
     * @return 变更统计
     */
    @Select("SELECT " +
            "change_type, " +
            "COUNT(*) as count, " +
            "SUM(character_change) as total_character_change " +
            "FROM article_version " +
            "WHERE article_id = #{articleId} " +
            "GROUP BY change_type " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectChangeStatistics(@Param("articleId") Long articleId);

    /**
     * 获取两个版本之间的差异
     * @param articleId 文章ID
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @return 版本差异
     */
    @Select("SELECT * FROM article_version " +
            "WHERE article_id = #{articleId} " +
            "AND version_number BETWEEN #{fromVersion} AND #{toVersion} " +
            "ORDER BY version_number ASC")
    List<ArticleVersion> selectVersionsBetween(@Param("articleId") Long articleId,
                                               @Param("fromVersion") Integer fromVersion,
                                               @Param("toVersion") Integer toVersion);

    /**
     * 获取自动保存的版本
     * @param articleId 文章ID
     * @param hours 小时数（获取多少小时内的自动保存版本）
     * @return 自动保存版本列表
     */
    @Select("SELECT av.*, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.article_id = #{articleId} " +
            "AND av.is_auto_save = true " +
            "AND av.created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "ORDER BY av.created_at DESC")
    List<Map<String, Object>> selectAutoSaveVersions(@Param("articleId") Long articleId, @Param("hours") int hours);

    /**
     * 搜索文章版本
     * @param articleId 文章ID
     * @param keyword 关键词
     * @param limit 限制数量
     * @return 匹配的版本列表
     */
    @Select("SELECT av.*, u.username as created_by_username " +
            "FROM article_version av " +
            "LEFT JOIN `user` u ON av.created_by = u.id " +
            "WHERE av.article_id = #{articleId} " +
            "AND (av.title LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR av.content LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR av.version_note LIKE CONCAT('%', #{keyword}, '%') " +
            "     OR av.change_summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY av.version_number DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> searchVersionsByKeyword(@Param("articleId") Long articleId,
                                                      @Param("keyword") String keyword,
                                                      @Param("limit") int limit);
}