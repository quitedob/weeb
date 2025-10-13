package com.web.service;

import com.web.model.ArticleComment;
import com.web.vo.article.ArticleCommentVo;
import java.util.List;

/**
 * 文章评论服务接口
 */
public interface ArticleCommentService {

    /**
     * 获取文章评论列表
     * @param articleId 文章ID
     * @return 评论列表
     */
    List<ArticleComment> getCommentsByArticleId(Long articleId);

    /**
     * 添加评论
     * @param articleId 文章ID
     * @param commentVo 评论内容
     * @param userId 用户ID
     * @return 评论ID
     */
    Long addComment(Long articleId, ArticleCommentVo commentVo, Long userId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);

    /**
     * 获取评论总数
     * @param articleId 文章ID
     * @return 评论总数
     */
    int getCommentCount(Long articleId);
}