package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.ArticleComment;
import com.web.service.ArticleCommentService;
import com.web.vo.comment.CommentCreateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 文章评论控制器
 */
@RestController
@RequestMapping("/api/comments")
public class ArticleCommentController {

    @Autowired
    private ArticleCommentService commentService;

    /**
     * 创建评论
     * @param commentVo 评论信息
     * @param userId 评论者ID
     * @return 创建的评论
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArticleComment>> createComment(
            @RequestBody @Valid CommentCreateVo commentVo,
            @Userid Long userId) {
        ArticleComment comment = commentService.createComment(commentVo, userId);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long commentId,
            @Userid Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("评论已删除"));
    }

    /**
     * 获取文章评论列表
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComments(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> comments = commentService.getComments(articleId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}