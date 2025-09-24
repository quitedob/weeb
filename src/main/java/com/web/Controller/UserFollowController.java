package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户关注控制器
 */
@RestController
@RequestMapping("/api/follow")
public class UserFollowController {

    @Autowired
    private UserFollowService userFollowService;

    /**
     * 关注用户
     * @param followeeId 被关注者ID
     * @param userId 关注者ID
     * @return 操作结果
     */
    @PostMapping("/{followeeId}")
    public ResponseEntity<ApiResponse<String>> followUser(
            @PathVariable Long followeeId,
            @Userid Long userId) {
        userFollowService.followUser(userId, followeeId);
        return ResponseEntity.ok(ApiResponse.success("关注成功"));
    }

    /**
     * 取消关注
     * @param followeeId 被关注者ID
     * @param userId 关注者ID
     * @return 操作结果
     */
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<ApiResponse<String>> unfollowUser(
            @PathVariable Long followeeId,
            @Userid Long userId) {
        userFollowService.unfollowUser(userId, followeeId);
        return ResponseEntity.ok(ApiResponse.success("取消关注成功"));
    }

    /**
     * 检查是否已关注
     * @param followeeId 被关注者ID
     * @param userId 关注者ID
     * @return 是否已关注
     */
    @GetMapping("/check/{followeeId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFollowing(
            @PathVariable Long followeeId,
            @Userid Long userId) {
        boolean isFollowing = userFollowService.isFollowing(userId, followeeId);
        return ResponseEntity.ok(ApiResponse.success(isFollowing));
    }

    /**
     * 获取关注列表
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID
     * @return 关注列表
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Userid Long userId) {
        Map<String, Object> result = userFollowService.getFollowingList(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取粉丝列表
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID
     * @return 粉丝列表
     */
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowersList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Userid Long userId) {
        Map<String, Object> result = userFollowService.getFollowersList(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取关注统计
     * @param userId 用户ID
     * @return 关注统计
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowStats(@Userid Long userId) {
        Map<String, Object> stats = userFollowService.getFollowStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取指定用户的关注统计
     * @param targetUserId 目标用户ID
     * @return 关注统计
     */
    @GetMapping("/stats/{targetUserId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserFollowStats(
            @PathVariable Long targetUserId) {
        Map<String, Object> stats = userFollowService.getFollowStats(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}