package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.ArticleComment;
import com.web.service.ArticleCommentService;
import com.web.vo.article.ArticleCommentVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 文章评论控制器
 */
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class ArticleCommentController {

    private static final Logger logger = LoggerFactory.getLogger(ArticleCommentController.class);
    
    @Autowired
    private ArticleCommentService articleCommentService;

    /**
     * 获取文章评论列表
     * GET /api/articles/{articleId}/comments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleComment>>> getComments(@PathVariable Long articleId) {
        try {
            List<ArticleComment> comments = articleCommentService.getCommentsByArticleId(articleId);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (Exception e) {
            logger.error("获取文章评论失败，文章ID: {}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 添加评论
     * POST /api/articles/{articleId}/comments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> addComment(
            @PathVariable Long articleId,
            @RequestBody @Valid ArticleCommentVo commentVo,
            @Userid Long userId) {
        try {
            Long commentId = articleCommentService.addComment(articleId, commentVo, userId);
            if (commentId != null) {
                return ResponseEntity.ok(ApiResponse.success("评论成功", commentId));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(1, "评论失败"));
            }
        } catch (Exception e) {
            logger.error("添加评论失败，文章ID: {}, 用户ID: {}", articleId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError("系统错误"));
        }
    }

    /**
     * 删除评论
     * DELETE /api/articles/{articleId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            @Userid Long userId) {
        try {
            boolean success = articleCommentService.deleteComment(commentId, userId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("评论不存在或无权删除"));
            }
        } catch (Exception e) {
            logger.error("删除评论失败，评论ID: {}, 用户ID: {}", commentId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }

    /**
     * 获取评论总数
     * GET /api/articles/{articleId}/comments/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCommentCount(@PathVariable Long articleId) {
        try {
            int count = articleCommentService.getCommentCount(articleId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            logger.error("获取评论总数失败，文章ID: {}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.systemError(ApiResponse.Messages.SYSTEM_ERROR));
        }
    }
}