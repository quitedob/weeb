package com.web.service;

import com.web.model.ArticleComment;
import com.web.vo.comment.CommentCreateVo;

import java.util.Map;

/**
 * 文章评论服务接口
 */
public interface ArticleCommentService {
    
    /**
     * 创建评论
     * @param commentVo 评论信息
     * @param userId 评论者ID
     * @return 创建的评论
     */
    ArticleComment createComment(CommentCreateVo commentVo, Long userId);
    
    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    void deleteComment(Long commentId, Long userId);
    
    /**
     * 获取文章评论列表
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表和分页信息
     */
    Map<String, Object> getComments(Long articleId, int page, int size);
}