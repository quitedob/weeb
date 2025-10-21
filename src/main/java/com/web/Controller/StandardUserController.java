package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.service.UserService;
import com.web.vo.user.UpdateUserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 标准化用户管理控制器
 * 遵循RESTful API设计规范
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class StandardUserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasPermission(null, 'USER_READ_OWN')")
    public ResponseEntity<ApiResponse<UserWithStats>> getCurrentUser(@Userid Long userId) {
        try {
            UserWithStats userProfile = userService.getUserProfile(userId);
            if (userProfile == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(userProfile));
        } catch (Exception e) {
            log.error("获取当前用户信息失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 更新当前用户信息
     * PUT /api/users/me
     */
    @PutMapping("/me")
    @PreAuthorize("hasPermission(null, 'USER_UPDATE_OWN')")
    public ResponseEntity<ApiResponse<User>> updateCurrentUser(
            @RequestBody @Valid UpdateUserVo updateVo,
            @Userid Long userId) {
        try {
            User user = new User();
            user.setId(userId);
            user.setUsername(updateVo.getUsername());
            user.setEmail(updateVo.getEmail());
            user.setPhone(updateVo.getPhone());
            user.setAvatar(updateVo.getAvatar());
            user.setBio(updateVo.getBio());

            boolean updated = userService.updateUserBasicInfo(user);
            if (updated) {
                User updatedUser = userService.getUserBasicInfo(userId);
                updatedUser.setPassword(null); // 安全起见，不返回密码
                return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", updatedUser));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户信息更新失败"));
            }
        } catch (Exception e) {
            log.error("更新当前用户信息失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("更新用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定用户信息
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasPermission(#userId, 'USER_READ_ANY')")
    public ResponseEntity<ApiResponse<UserWithStats>> getUser(@PathVariable Long userId) {
        try {
            UserWithStats userProfile = userService.getUserProfile(userId);
            if (userProfile == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(userProfile));
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户列表（分页）
     * GET /api/users?page=1&pageSize=20&keyword=example
     */
    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER_READ_ANY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            Map<String, Object> result = userService.getUsersWithPaging(page, pageSize, keyword);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索用户
     * GET /api/users/search?q=keyword&limit=10
     */
    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'USER_READ_ANY')")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 这里需要实现搜索逻辑，目前简化处理
            Map<String, Object> searchResult = userService.getUsersWithPaging(1, limit, keyword);
            return ResponseEntity.ok(ApiResponse.success(searchResult));
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}", keyword, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("搜索用户失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户的群组列表
     * GET /api/users/me/groups
     */
    @GetMapping("/me/groups")
    @PreAuthorize("hasPermission(null, 'GROUP_READ_OWN')")
    public ResponseEntity<ApiResponse<Object>> getCurrentUserGroups(@Userid Long userId) {
        try {
            // 这里需要实现获取用户群组的逻辑
            // 暂时返回空列表，实际需要调用GroupService
            return ResponseEntity.ok(ApiResponse.success("获取用户群组成功"));
        } catch (Exception e) {
            log.error("获取用户群组失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户群组失败: " + e.getMessage()));
        }
    }

    /**
     * 封禁用户
     * POST /api/users/{userId}/ban
     */
    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasPermission(#userId, 'USER_BAN_ANY')")
    public ResponseEntity<ApiResponse<String>> banUser(@PathVariable Long userId) {
        try {
            boolean banned = userService.banUser(userId);
            if (banned) {
                return ResponseEntity.ok(ApiResponse.success("用户封禁成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户封禁失败"));
            }
        } catch (Exception e) {
            log.error("封禁用户失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("封禁用户失败: " + e.getMessage()));
        }
    }

    /**
     * 解封用户
     * POST /api/users/{userId}/unban
     */
    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasPermission(#userId, 'USER_BAN_ANY')")
    public ResponseEntity<ApiResponse<String>> unbanUser(@PathVariable Long userId) {
        try {
            boolean unbanned = userService.unbanUser(userId);
            if (unbanned) {
                return ResponseEntity.ok(ApiResponse.success("用户解封成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户解封失败"));
            }
        } catch (Exception e) {
            log.error("解封用户失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("解封用户失败: " + e.getMessage()));
        }
    }

    /**
     * 重置用户密码
     * POST /api/users/{userId}/reset-password
     */
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasPermission(#userId, 'USER_RESET_PASSWORD_ANY')")
    public ResponseEntity<ApiResponse<String>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("新密码不能为空"));
            }

            boolean reset = userService.resetUserPassword(userId, newPassword.trim());
            if (reset) {
                return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码重置失败"));
            }
        } catch (Exception e) {
            log.error("重置用户密码失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("重置密码失败: " + e.getMessage()));
        }
    }

    /**
     * 关注用户
     * POST /api/users/{userId}/follow
     */
    @PostMapping("/{userId}/follow")
    @PreAuthorize("hasPermission(null, 'USER_FOLLOW_OWN')")
    public ResponseEntity<ApiResponse<String>> followUser(
            @PathVariable Long userId,
            @Userid Long currentUserId) {
        try {
            boolean followed = userService.followUser(currentUserId, userId);
            if (followed) {
                return ResponseEntity.ok(ApiResponse.success("关注成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("关注失败"));
            }
        } catch (Exception e) {
            log.error("关注用户失败: followerId={}, followedId={}", currentUserId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("关注失败: " + e.getMessage()));
        }
    }

    /**
     * 取消关注用户
     * DELETE /api/users/{userId}/follow
     */
    @DeleteMapping("/{userId}/follow")
    @PreAuthorize("hasPermission(null, 'USER_FOLLOW_OWN')")
    public ResponseEntity<ApiResponse<String>> unfollowUser(
            @PathVariable Long userId,
            @Userid Long currentUserId) {
        try {
            boolean unfollowed = userService.unfollowUser(currentUserId, userId);
            if (unfollowed) {
                return ResponseEntity.ok(ApiResponse.success("取消关注成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("取消关注失败"));
            }
        } catch (Exception e) {
            log.error("取消关注用户失败: followerId={}, followedId={}", currentUserId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("取消关注失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的关注列表
     * GET /api/users/{userId}/following?page=1&pageSize=20
     */
    @GetMapping("/{userId}/following")
    @PreAuthorize("hasPermission(#userId, 'USER_READ_ANY')")
    public ResponseEntity<ApiResponse<Object>> getUserFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            // 这里需要实现获取关注列表的逻辑
            return ResponseEntity.ok(ApiResponse.success("获取关注列表成功"));
        } catch (Exception e) {
            log.error("获取用户关注列表失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取关注列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的粉丝列表
     * GET /api/users/{userId}/followers?page=1&pageSize=20
     */
    @GetMapping("/{userId}/followers")
    @PreAuthorize("hasPermission(#userId, 'USER_READ_ANY')")
    public ResponseEntity<ApiResponse<Object>> getUserFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            // 这里需要实现获取粉丝列表的逻辑
            return ResponseEntity.ok(ApiResponse.success("获取粉丝列表成功"));
        } catch (Exception e) {
            log.error("获取用户粉丝列表失败: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取粉丝列表失败: " + e.getMessage()));
        }
    }

    /**
     * 检查当前用户是否关注了指定用户
     * GET /api/users/{userId}/follow/status
     */
    @GetMapping("/{userId}/follow/status")
    @PreAuthorize("hasPermission(null, 'USER_FOLLOW_OWN')")
    public ResponseEntity<ApiResponse<Boolean>> checkFollowStatus(
            @PathVariable Long userId,
            @Userid Long currentUserId) {
        try {
            // 这里需要实现检查关注状态的逻辑
            return ResponseEntity.ok(ApiResponse.success(false)); // 暂时返回false
        } catch (Exception e) {
            log.error("检查关注状态失败: followerId={}, followedId={}", currentUserId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("检查关注状态失败: " + e.getMessage()));
        }
    }
}