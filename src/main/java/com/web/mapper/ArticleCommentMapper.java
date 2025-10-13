package com.web.mapper;

import com.web.model.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文章评论相关的数据库访问层接口
 */
@Mapper
public interface ArticleCommentMapper {

    /**
     * 根据文章ID获取评论列表
     * @param articleId 文章ID
     * @return 评论列表
     */
    List<ArticleComment> getCommentsByArticleId(@Param("articleId") Long articleId);

    /**
     * 添加新评论
     * @param comment 评论实体
     * @return 影响的行数
     */
    int insertComment(ArticleComment comment);

    /**
     * 删除评论
     * @param id 评论ID
     * @param userId 用户ID（用于权限验证）
     * @return 影响的行数
     */
    int deleteComment(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 获取评论总数
     * @param articleId 文章ID
     * @return 评论总数
     */
    int countCommentsByArticleId(@Param("articleId") Long articleId);
}