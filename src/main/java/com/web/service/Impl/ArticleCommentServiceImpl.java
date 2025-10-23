package com.web.service.Impl;

import com.web.exception.WeebException;
import com.web.mapper.ArticleCommentMapper;
import com.web.model.ArticleComment;
import com.web.service.ArticleCommentService;
import com.web.vo.article.ArticleCommentVo;
import com.web.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章评论服务实现类
 */
@Service
@Transactional
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    @Override
    public List<ArticleComment> getCommentsByArticleId(Long articleId) {
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        return articleCommentMapper.getCommentsByArticleId(articleId);
    }

    @Override
    public Long addComment(Long articleId, ArticleCommentVo commentVo, Long userId) {
        // 参数验证
        if (commentVo == null) {
            throw new WeebException("评论内容不能为空");
        }
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("用户ID无效");
        }
        if (!ValidationUtils.validateCommentContent(commentVo.getContent())) {
            throw new WeebException("评论内容不符合要求");
        }
        
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(commentVo.getContent().trim());
        comment.setParentId(commentVo.getParentId());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        
        int result = articleCommentMapper.insertComment(comment);
        if (result > 0) {
            return comment.getId();
        }
        return null;
    }

    @Override
    public boolean deleteComment(Long commentId, Long userId) {
        if (!ValidationUtils.validateId(commentId, "评论ID")) {
            throw new WeebException("评论ID无效");
        }
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("用户ID无效");
        }
        
        int result = articleCommentMapper.deleteComment(commentId, userId);
        return result > 0;
    }

    @Override
    public int getCommentCount(Long articleId) {
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        return articleCommentMapper.countCommentsByArticleId(articleId);
    }
}