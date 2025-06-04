package com.web.controller;

import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.SecurityUtil;
import com.web.vo.user.UpdateUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户信息的获取和更新功能
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private AuthService authService;

    /**
     * 获取当前登录用户的详细信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public ApiResponse<User> getUserInfo() {
        Integer userId = SecurityUtil.getUserId(); // 从安全上下文中获取用户ID
        User user = authService.getUserById(userId);
        if (user != null) {
            user.setPassword(null); // 安全起见，不返回密码
        }
        return ApiResponse.success(user);
    }

    /**
     * 更新当前登录用户的信息
     * @param updateUserVo 包含待更新用户信息的VO
     * @return 更新后的用户信息
     */
    @PostMapping("/update")
    public ApiResponse<User> updateUserInfo(@Validated @RequestBody UpdateUserVo updateUserVo) {
        Integer userId = SecurityUtil.getUserId();
        User updatedUser = authService.updateUser(userId, updateUserVo);
        if (updatedUser != null) {
            updatedUser.setPassword(null); // 安全起见，不返回密码
        }
        return ApiResponse.success(updatedUser);
    }
}
