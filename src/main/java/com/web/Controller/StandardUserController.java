package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.security.SecurityUtils;
import com.web.service.UserService;
import com.web.service.UserStatsService;
import com.web.util.ApiResponseUtil;
import com.web.vo.user.UpdateUserVo;
import com.web.vo.admin.AdminResetPasswordRequestVo;
import com.web.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
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

    @Autowired
    private UserStatsService userStatsService;

    /**
     * 获取当前用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserWithStats>> getCurrentUser() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestUserWithStats("用户未认证");
            }

            UserWithStats userProfile = userService.getUserProfile(userId);
            if (userProfile == null) {
                return ApiResponseUtil.badRequestUserWithStats("资源未找到");
            }
            return ApiResponseUtil.successUserWithStats(userProfile);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUserWithStats(e, "获取当前用户信息", SecurityUtils.getCurrentUserId());
        }
    }

    /**
     * 获取当前用户完整信息（包含统计数据）- 兼容UserController路径
     * GET /api/users/me/profile
     */
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserWithStats>> getCurrentUserProfile() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestUserWithStats("用户未认证");
            }

            UserWithStats userWithStats = userService.getUserProfile(userId);
            if (userWithStats != null && userWithStats.getUser() != null) {
                userWithStats.getUser().setPassword(null); // 安全起见，不返回密码
            }
            return ApiResponseUtil.successUserWithStats(userWithStats);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUserWithStats(e, "获取当前用户完整信息", SecurityUtils.getCurrentUserId());
        }
    }

    /**
     * 获取当前用户基本信息 - 兼容UserController路径
     * GET /api/users/me/info
     */
    @GetMapping("/me/info")
    public ResponseEntity<ApiResponse<User>> getCurrentUserInfo() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestUser("用户未认证");
            }

            User user = userService.getUserBasicInfo(userId);
            if (user != null) {
                user.setPassword(null); // 安全起见，不返回密码
            }
            return ApiResponseUtil.successUser(user);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUser(e, "获取当前用户基本信息", SecurityUtils.getCurrentUserId());
        }
    }

    /**
     * 更新当前用户信息
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateCurrentUser(
            @RequestBody @Valid UpdateUserVo updateVo) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestUser("用户未认证");
            }

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
                return ApiResponseUtil.successUser(updatedUser, "用户信息更新成功");
            } else {
                return ApiResponseUtil.badRequestUser("用户信息更新失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUser(e, "更新当前用户信息", SecurityUtils.getCurrentUserId());
        }
    }

    /**
     * 获取指定用户信息
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserWithStats>> getUser(@PathVariable Long userId) {
        try {
            UserWithStats userProfile = userService.getUserProfile(userId);
            if (userProfile == null) {
                return ApiResponseUtil.badRequestUserWithStats("资源未找到");
            }
            return ApiResponseUtil.successUserWithStats(userProfile);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUserWithStats( e, "获取用户信息", userId);
        }
    }

    /**
     * 通过用户名获取用户信息
     * GET /api/users/by-username/{username}
     */
    @GetMapping("/by-username/{username}")
    public ResponseEntity<ApiResponse<UserWithStats>> getUserByUsername(@PathVariable String username) {
        try {
            UserWithStats userProfile = userService.getUserProfileByUsername(username);
            if (userProfile == null) {
                return ApiResponseUtil.badRequestUserWithStats("用户不存在");
            }
            return ApiResponseUtil.successUserWithStats(userProfile);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUserWithStats(e, "通过用户名获取用户信息", username);
        }
    }

    /**
     * 获取用户列表（分页）
     * GET /api/users?page=1&pageSize=20&keyword=example
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            Map<String, Object> result = userService.getUsersWithPaging(page, pageSize, keyword);
            return ApiResponseUtil.successMap(result);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取用户列表", page, pageSize, keyword);
        }
    }

    /**
     * 搜索用户
     * GET /api/users/search?q=keyword&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 这里需要实现搜索逻辑，目前简化处理
            Map<String, Object> searchResult = userService.getUsersWithPaging(1, limit, keyword);
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) searchResult.get("users");
            return ApiResponseUtil.successUserList(users);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUserList(e, "搜索用户", keyword, limit);
        }
    }

    /**
     * 获取当前用户的群组列表
     * GET /api/users/me/groups
     */
    @GetMapping("/me/groups")
    public ResponseEntity<ApiResponse<String>> getCurrentUserGroups() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestString("用户未认证");
            }

            // 这里需要实现获取用户群组的逻辑
            // 暂时返回空列表，实际需要调用GroupService
            return ApiResponseUtil.successString("获取用户群组成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "获取用户群组", SecurityUtils.getCurrentUserId());
        }
    }

    /**
     * 封禁用户
     * POST /api/users/{userId}/ban
     */
    @PostMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<String>> banUser(@PathVariable Long userId) {
        try {
            boolean banned = userService.banUser(userId);
            if (banned) {
                return ApiResponseUtil.successString("用户封禁成功");
            } else {
                return ApiResponseUtil.badRequestString("用户封禁失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "封禁用户", userId);
        }
    }

    /**
     * 解封用户
     * POST /api/users/{userId}/unban
     */
    @PostMapping("/{userId}/unban")
    public ResponseEntity<ApiResponse<String>> unbanUser(@PathVariable Long userId) {
        try {
            boolean unbanned = userService.unbanUser(userId);
            if (unbanned) {
                return ApiResponseUtil.successString("用户解封成功");
            } else {
                return ApiResponseUtil.badRequestString("用户解封失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "解封用户", userId);
        }
    }

    /**
     * 重置用户密码
     * POST /api/users/{userId}/reset-password
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody @Valid AdminResetPasswordRequestVo resetRequest) {
        try {
            // 使用ValidationUtils进行额外的密码验证
            String newPassword = resetRequest.getNewPassword();
            if (!ValidationUtils.validatePassword(newPassword)) {
                return ApiResponseUtil.badRequestString("新密码不符合安全要求");
            }

            // 确认密码验证
            if (!newPassword.equals(resetRequest.getConfirmPassword())) {
                return ApiResponseUtil.badRequestString("两次输入的密码不一致");
            }

            // 对密码进行安全清理（使用搜索关键词清理方法作为通用字符串清理）
            String sanitizedPassword = ValidationUtils.sanitizeSearchKeyword(newPassword.trim());

            boolean reset = userService.resetUserPassword(userId, sanitizedPassword);
            if (reset) {
                return ApiResponseUtil.successString("密码重置成功");
            } else {
                return ApiResponseUtil.badRequestString("密码重置失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "重置用户密码", userId);
        }
    }

    /**
     * 关注用户
     * POST /api/users/{userId}/follow
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<String>> followUser(
            @PathVariable Long userId) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ApiResponseUtil.badRequestString("用户未认证");
            }

            boolean followed = userService.followUser(currentUserId, userId);
            if (followed) {
                return ApiResponseUtil.successString("关注成功");
            } else {
                return ApiResponseUtil.badRequestString("关注失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "关注用户", SecurityUtils.getCurrentUserId(), userId);
        }
    }

    /**
     * 取消关注用户
     * DELETE /api/users/{userId}/follow
     */
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<String>> unfollowUser(
            @PathVariable Long userId) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ApiResponseUtil.badRequestString("用户未认证");
            }

            boolean unfollowed = userService.unfollowUser(currentUserId, userId);
            if (unfollowed) {
                return ApiResponseUtil.successString("取消关注成功");
            } else {
                return ApiResponseUtil.badRequestString("取消关注失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "取消关注用户", SecurityUtils.getCurrentUserId(), userId);
        }
    }

    /**
     * 获取用户的关注列表
     * GET /api/users/{userId}/following?page=1&pageSize=20
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<String>> getUserFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            // 这里需要实现获取关注列表的逻辑
            return ApiResponseUtil.successString("获取关注列表成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "获取用户关注列表", userId, page, pageSize);
        }
    }

    /**
     * 获取用户的粉丝列表
     * GET /api/users/{userId}/followers?page=1&pageSize=20
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<String>> getUserFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            // 这里需要实现获取粉丝列表的逻辑
            return ApiResponseUtil.successString("获取粉丝列表成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "获取用户粉丝列表", userId, page, pageSize);
        }
    }

    /**
     * 检查当前用户是否关注了指定用户
     * GET /api/users/{userId}/follow/status
     */
    @GetMapping("/{userId}/follow/status")
    public ResponseEntity<ApiResponse<Boolean>> checkFollowStatus(
            @PathVariable Long userId) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ApiResponseUtil.badRequestBoolean("用户未认证");
            }

            // 这里需要实现检查关注状态的逻辑
            return ApiResponseUtil.successBoolean(false); // 暂时返回false
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionBoolean(e, "检查关注状态", SecurityUtils.getCurrentUserId(), userId);
        }
    }

    /**
     * 获取用户统计信息
     * GET /api/users/{userId}/stats
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(
            @PathVariable Long userId) {
        try {
            Map<String, Object> stats = userService.getUserStatistics(userId);
            return ApiResponseUtil.successMap(stats);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "获取用户统计信息", userId);
        }
    }

    /**
     * 获取用户最近活动
     * GET /api/users/{userId}/activities
     */
    @GetMapping("/{userId}/activities")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserRecentActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> activities = userService.getUserRecentActivities(userId, limit);
            return ApiResponseUtil.successMapList(activities);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMapList(e, "获取用户最近活动", userId, limit);
        }
    }

    /**
     * 更新个人资料
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @RequestBody @Valid UpdateUserVo updateVo) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ApiResponseUtil.badRequestUser("用户未认证");
            }

            User user = new User();
            user.setId(userId);
            user.setNickname(updateVo.getNickname());
            user.setBio(updateVo.getBio());
            user.setEmail(updateVo.getEmail());

            boolean updated = userService.updateUserProfile(user);
            if (updated) {
                User updatedUser = userService.getUserBasicInfo(userId);
                updatedUser.setPassword(null); // 安全起见，不返回密码
                return ApiResponseUtil.successUser(updatedUser, "个人资料更新成功");
            } else {
                return ApiResponseUtil.badRequestUser("个人资料更新失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionUser(e, "更新个人资料", SecurityUtils.getCurrentUserId());
        }
    }

  }