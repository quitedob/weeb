package com.web.campus;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.web.campus.CampusPostDtos.*;

@RestController
@RequestMapping("/api/campus")
public class CampusPostController {
    private final CampusPostService posts;
    public CampusPostController(CampusPostService posts) { this.posts = posts; }

    @GetMapping("/schools/{schoolId}/posts")
    public ApiResponse<CampusPage<Map<String, Object>>> list(@Userid Long actor, @PathVariable long schoolId,
            @RequestParam(required = false) String q, @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort, @RequestParam(defaultValue = "feed") String scope,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(posts.list(actor(actor), schoolId, q, category, sort, scope, status, page, size));
    }

    @PostMapping("/schools/{schoolId}/posts")
    public ApiResponse<Map<String, Object>> create(@Userid Long actor, @PathVariable long schoolId, @RequestBody PostWrite request) {
        return ApiResponse.success(posts.create(actor(actor), schoolId, request));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> get(@Userid Long actor, @PathVariable long postId) { return ApiResponse.success(posts.get(actor(actor), postId)); }

    @PutMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> update(@Userid Long actor, @PathVariable long postId, @RequestBody PostWrite request) {
        return ApiResponse.success(posts.update(actor(actor), postId, request));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Map<String, Object>> delete(@Userid Long actor, @PathVariable long postId, @RequestParam Long version) {
        posts.delete(actor(actor), postId, version); return success();
    }

    @PutMapping("/posts/{postId}/like")
    public ApiResponse<Map<String, Object>> like(@Userid Long actor, @PathVariable long postId) { return ApiResponse.success(posts.like(actor(actor), postId, true)); }
    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<Map<String, Object>> unlike(@Userid Long actor, @PathVariable long postId) { return ApiResponse.success(posts.like(actor(actor), postId, false)); }
    @PutMapping("/posts/{postId}/bookmark")
    public ApiResponse<Map<String, Object>> bookmark(@Userid Long actor, @PathVariable long postId) { return ApiResponse.success(posts.bookmark(actor(actor), postId, true)); }
    @DeleteMapping("/posts/{postId}/bookmark")
    public ApiResponse<Map<String, Object>> unbookmark(@Userid Long actor, @PathVariable long postId) { return ApiResponse.success(posts.bookmark(actor(actor), postId, false)); }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<CampusPage<Map<String, Object>>> comments(@Userid Long actor, @PathVariable long postId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(posts.comments(actor(actor), postId, page, size));
    }
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Map<String, Object>> comment(@Userid Long actor, @PathVariable long postId, @RequestBody CommentWrite request) {
        return ApiResponse.success(posts.comment(actor(actor), postId, request));
    }
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Map<String, Object>> deleteComment(@Userid Long actor, @PathVariable long postId, @PathVariable long commentId) {
        posts.deleteComment(actor(actor), postId, commentId); return success();
    }
    @PostMapping("/posts/{postId}/reports")
    public ApiResponse<Map<String, Object>> report(@Userid Long actor, @PathVariable long postId, @RequestBody ReportWrite request) {
        posts.report(actor(actor), postId, request); return success();
    }

    @GetMapping("/schools/{schoolId}/moderation/posts")
    public ApiResponse<CampusPage<Map<String, Object>>> moderation(@Userid Long actor, @PathVariable long schoolId,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(posts.moderationPosts(actor(actor), schoolId, status, page, size));
    }
    @PutMapping("/posts/{postId}/review")
    public ApiResponse<Map<String, Object>> review(@Userid Long actor, @PathVariable long postId, @RequestBody Review request) {
        return ApiResponse.success(posts.review(actor(actor), postId, request));
    }
    @PutMapping("/posts/{postId}/pin")
    public ApiResponse<Map<String, Object>> pin(@Userid Long actor, @PathVariable long postId, @RequestBody Pin request) {
        return ApiResponse.success(posts.pin(actor(actor), postId, request));
    }
    @GetMapping("/schools/{schoolId}/reports")
    public ApiResponse<CampusPage<Map<String, Object>>> reports(@Userid Long actor, @PathVariable long schoolId,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(posts.reports(actor(actor), schoolId, status, page, size));
    }
    @PutMapping("/schools/{schoolId}/reports/{reportId}")
    public ApiResponse<Map<String, Object>> decideReport(@Userid Long actor, @PathVariable long schoolId,
            @PathVariable long reportId, @RequestBody Review request) {
        return ApiResponse.success(posts.decideReport(actor(actor), schoolId, reportId, request));
    }

    private static long actor(Long actor) { if (actor == null || actor <= 0) throw new CampusException(401, "请先登录"); return actor; }
    private static ApiResponse<Map<String, Object>> success() { return ApiResponse.success(Map.of("success", true)); }
}
