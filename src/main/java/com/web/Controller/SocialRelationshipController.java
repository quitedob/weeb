package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.SocialRelationshipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 社交关系控制器
 * 统一管理好友和关注关系
 */
@Slf4j
@RestController
@RequestMapping("/api/social")
public class SocialRelationshipController {

    @Autowired
    private SocialRelationshipService socialRelationshipService;

    /**
     * 获取与目标用户的关系状态
     * GET /api/social/relationship/{targetUserId}
     */
    @GetMapping("/relationship/{targetUserId}")
    public ResponseEntity<ApiResponse<String>> getRelationshipStatus(
            @PathVariable Long targetUserId,
            @Userid Long userId) {
        try {
            SocialRelationshipService.RelationshipStatus status = 
                socialRelationshipService.getRelationshipStatus(userId, targetUserId);
            return ResponseEntity.ok(ApiResponse.success(status.name()));
        } catch (Exception e) {
            log.error("获取关系状态失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取关系状态失败"));
        }
    }

    /**
     * 关注用户
     * POST /api/social/follow/{targetUserId}
     */
    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<ApiResponse<String>> followUser(
            @PathVariable Long targetUserId,
            @Userid Long userId) {
        try {
            boolean success = socialRelationshipService.followUser(userId, targetUserId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("关注成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("关注失败"));
            }
        } catch (Exception e) {
            log.error("关注用户失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("关注失败: " + e.getMessage()));
        }
    }

    /**
     * 取消关注
     * DELETE /api/social/follow/{targetUserId}
     */
    @DeleteMapping("/follow/{targetUserId}")
    public ResponseEntity<ApiResponse<String>> unfollowUser(
            @PathVariable Long targetUserId,
            @Userid Long userId) {
        try {
            boolean success = socialRelationshipService.unfollowUser(userId, targetUserId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("取消关注成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消关注失败"));
            }
        } catch (Exception e) {
            log.error("取消关注失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("取消关注失败: " + e.getMessage()));
        }
    }

    /**
     * 删除好友
     * DELETE /api/social/friend/{friendId}
     */
    @DeleteMapping("/friend/{friendId}")
    public ResponseEntity<ApiResponse<String>> removeFriend(
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "false") boolean keepFollow,
            @Userid Long userId) {
        try {
            boolean success = socialRelationshipService.removeFriend(userId, friendId, keepFollow);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("删除好友成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("删除好友失败"));
            }
        } catch (Exception e) {
            log.error("删除好友失败: userId={}, friendId={}", userId, friendId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除好友失败: " + e.getMessage()));
        }
    }

    /**
     * 获取好友列表
     * GET /api/social/friends
     */
    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFriendList(@Userid Long userId) {
        try {
            List<Map<String, Object>> friends = socialRelationshipService.getFriendList(userId);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (Exception e) {
            log.error("获取好友列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取好友列表失败"));
        }
    }

    /**
     * 获取关注列表
     * GET /api/social/following
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowingList(@Userid Long userId) {
        try {
            List<Map<String, Object>> following = socialRelationshipService.getFollowingList(userId);
            return ResponseEntity.ok(ApiResponse.success(following));
        } catch (Exception e) {
            log.error("获取关注列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取关注列表失败"));
        }
    }

    /**
     * 获取粉丝列表
     * GET /api/social/followers
     */
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowerList(@Userid Long userId) {
        try {
            List<Map<String, Object>> followers = socialRelationshipService.getFollowerList(userId);
            return ResponseEntity.ok(ApiResponse.success(followers));
        } catch (Exception e) {
            log.error("获取粉丝列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取粉丝列表失败"));
        }
    }

    /**
     * 获取互相关注列表
     * GET /api/social/mutual-follow
     */
    @GetMapping("/mutual-follow")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMutualFollowList(@Userid Long userId) {
        try {
            List<Map<String, Object>> mutualFollow = socialRelationshipService.getMutualFollowList(userId);
            return ResponseEntity.ok(ApiResponse.success(mutualFollow));
        } catch (Exception e) {
            log.error("获取互相关注列表失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取互相关注列表失败"));
        }
    }

    /**
     * 获取好友推荐
     * GET /api/social/recommendations/friends
     */
    @GetMapping("/recommendations/friends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFriendRecommendations(
            @Userid Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> recommendations = 
                socialRelationshipService.getFriendRecommendations(userId, limit);
            return ResponseEntity.ok(ApiResponse.success(recommendations));
        } catch (Exception e) {
            log.error("获取好友推荐失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取好友推荐失败"));
        }
    }

    /**
     * 获取关注推荐
     * GET /api/social/recommendations/follow
     */
    @GetMapping("/recommendations/follow")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowRecommendations(
            @Userid Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> recommendations = 
                socialRelationshipService.getFollowRecommendations(userId, limit);
            return ResponseEntity.ok(ApiResponse.success(recommendations));
        } catch (Exception e) {
            log.error("获取关注推荐失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取关注推荐失败"));
        }
    }

    /**
     * 获取社交统计信息
     * GET /api/social/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSocialStatistics(@Userid Long userId) {
        try {
            Map<String, Object> statistics = socialRelationshipService.getSocialStatistics(userId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取社交统计失败: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取社交统计失败"));
        }
    }
}
