package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.model.UserWithStats;
import com.web.service.AuthService;
import com.web.service.UserStatsService;
import com.web.vo.user.UpdateUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 用户管理控制器
 * 简化注释：用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserStatsService userStatsService;

    /**
     * 获取当前用户信息
     * @param userId 当前用户ID
     * @return 用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<User>> getUserInfo(@Userid Long userId) {
        User user = authService.getUserById(userId.intValue());
        if (user != null) {
            user.setPassword(null); // 安全起见，不返回密码
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 更新用户信息
     * @param updateVo 更新信息
     * @param userId 当前用户ID
     * @return 更新结果
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<User>> updateUserInfo(@RequestBody @Valid UpdateUserVo updateVo, @Userid Long userId) {
        User updatedUser = authService.updateUser(userId.intValue(), updateVo);
        if (updatedUser != null) {
            updatedUser.setPassword(null); // 安全起见，不返回密码
        }
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * [新增] 获取指定用户基本信息（供前端显示群主信息等）
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserInfoById(@PathVariable Long userId) {
        User user = authService.getUserById(userId.intValue());
        if (user != null) {
            user.setPassword(null);
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * [新增] 标准化更新接口：PUT /api/user/info
     */
    @PutMapping("/info")
    public ResponseEntity<ApiResponse<User>> putUserInfo(@RequestBody @Valid UpdateUserVo updateVo, @Userid Long userId) {
        User updatedUser = authService.updateUser(userId.intValue(), updateVo);
        if (updatedUser != null) {
            updatedUser.setPassword(null);
        }
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 获取用户完整信息（包含统计数据）
     * @param userId 当前用户ID
     * @return 用户完整信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserWithStats>> getUserProfile(@Userid Long userId) {
        UserWithStats userWithStats = authService.getUserWithStats(userId);
        if (userWithStats != null && userWithStats.getUser() != null) {
            userWithStats.getUser().setPassword(null); // 安全起见，不返回密码
        }
        return ResponseEntity.ok(ApiResponse.success(userWithStats));
    }

    /**
     * 获取指定用户的完整信息（包含统计数据）
     * @param userId 目标用户ID
     * @return 用户完整信息
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserWithStats>> getUserProfileById(@PathVariable Long userId) {
        UserWithStats userWithStats = authService.getUserWithStats(userId);
        if (userWithStats != null && userWithStats.getUser() != null) {
            userWithStats.getUser().setPassword(null); // 安全起见，不返回密码
        }
        return ResponseEntity.ok(ApiResponse.success(userWithStats));
    }

    /**
     * 增加用户粉丝数（用于关注操作）
     * @param userId 目标用户ID
     * @return 操作结果
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<String>> followUser(@PathVariable Long userId) {
        boolean success = userStatsService.incrementFansCount(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("关注成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("关注失败"));
        }
    }

    /**
     * 减少用户粉丝数（用于取消关注操作）
     * @param userId 目标用户ID
     * @return 操作结果
     */
    @PostMapping("/{userId}/unfollow")
    public ResponseEntity<ApiResponse<String>> unfollowUser(@PathVariable Long userId) {
        boolean success = userStatsService.decrementFansCount(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("取消关注成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("取消关注失败"));
        }
    }
}
