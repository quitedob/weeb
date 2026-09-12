package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.ArticleCommentMapper;
import com.web.model.ArticleComment;
import com.web.service.ArticleCommentService;
import com.web.service.ArticleService;
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

    @Autowired
    private ArticleService articleService;

    private void requireVisibleArticle(Long articleId) {
        if (articleService.getArticleById(articleId) == null) {
            throw new WeebException("文章不存在");
        }
    }

    @Override
    public List<ArticleComment> getCommentsByArticleId(Long articleId) {
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        requireVisibleArticle(articleId);
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
        
        requireVisibleArticle(articleId);
        if (commentVo.getParentId() != null) {
            ArticleComment parent = commentVo.getParentId() > 0
                    ? articleCommentMapper.selectById(commentVo.getParentId()) : null;
            if (parent == null || !articleId.equals(parent.getArticleId())) {
                throw new WeebException("父评论不存在或不属于当前文章");
            }
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
    public boolean deleteComment(Long articleId, Long commentId, Long userId) {
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        if (!ValidationUtils.validateId(commentId, "评论ID")) {
            throw new WeebException("评论ID无效");
        }
        if (!ValidationUtils.validateId(userId, "用户ID")) {
            throw new WeebException("用户ID无效");
        }
        
        int result = articleCommentMapper.deleteComment(articleId, commentId, userId);
        return result > 0;
    }

    @Override
    public int getCommentCount(Long articleId) {
        if (!ValidationUtils.validateId(articleId, "文章ID")) {
            throw new WeebException("文章ID无效");
        }
        requireVisibleArticle(articleId);
        return articleCommentMapper.countCommentsByArticleId(articleId);
    }
}
