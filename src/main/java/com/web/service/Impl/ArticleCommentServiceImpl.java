package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.ArticleCommentMapper;
import com.web.mapper.ArticleMapper;
import com.web.model.Article;
import com.web.model.ArticleComment;
import com.web.service.ArticleCommentService;
import com.web.service.NotificationService;
import com.web.vo.comment.CommentCreateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章评论服务实现类
 */
@Service
@Transactional
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper commentMapper;
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public ArticleComment createComment(CommentCreateVo commentVo, Long userId) {
        // 验证文章是否存在
        if (articleMapper.selectArticleById(commentVo.getArticleId()) == null) {
            throw new WeebException("文章不存在");
        }
        
        // 如果是回复评论，验证父评论是否存在
        if (commentVo.getParentId() != null) {
            if (commentMapper.selectById(commentVo.getParentId()) == null) {
                throw new WeebException("父评论不存在");
            }
        }
        
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(commentVo.getArticleId());
        comment.setUserId(userId);
        comment.setContent(commentVo.getContent());
        comment.setParentId(commentVo.getParentId());
        comment.setCreatedAt(new Date());
        comment.setUpdatedAt(new Date());
        
        commentMapper.insert(comment);
        
        // 创建评论通知
        try {
            // 获取文章作者ID
            Article article = articleMapper.selectArticleById(commentVo.getArticleId());
            if (article != null) {
                Long authorId = article.getUserId();
                if (!authorId.equals(userId)) { // 不给自己发通知
                    notificationService.createCommentNotification(
                        commentVo.getArticleId(), 
                        comment.getId(), 
                        userId, 
                        authorId
                    );
                }
            }
        } catch (Exception e) {
            // 通知创建失败不影响评论创建
            System.err.println("创建评论通知失败: " + e.getMessage());
        }
        
        return comment;
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        ArticleComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new WeebException("评论不存在");
        }
        
        if (!comment.getUserId().equals(userId)) {
            throw new WeebException("只能删除自己的评论");
        }
        
        commentMapper.deleteById(commentId);
    }

    @Override
    public Map<String, Object> getComments(Long articleId, int page, int size) {
        int offset = (page - 1) * size;
        
        List<Map<String, Object>> comments = commentMapper.getCommentsWithUserInfo(articleId, offset, size);
        int total = commentMapper.getCommentCount(articleId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("comments", comments);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
}