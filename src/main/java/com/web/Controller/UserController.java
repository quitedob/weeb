package com.web.Controller;

import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.AuthService;
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
}
