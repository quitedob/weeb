package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.ApiResponseUtil;
import com.web.util.JwtUtil;
import com.web.util.ValidationUtils;
import com.web.vo.auth.ForgotPasswordVo;
import com.web.vo.auth.LoginVo;
import com.web.vo.auth.PasswordChangeVo;
import com.web.vo.auth.PasswordResetVo;
import com.web.vo.auth.RegistrationVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 标准化认证控制器
 * 遵循RESTful API设计规范
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class StandardAuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody @Valid RegistrationVo registrationVo) {
        try {
            // 验证密码和确认密码是否一致
            if (!registrationVo.getPassword().equals(registrationVo.getConfirmPassword())) {
                return ApiResponseUtil.badRequestMap("密码和确认密码不一致");
            }

            // 检查用户名是否已存在
            if (authService.findByUsername(registrationVo.getUsername()) != null) {
                return ApiResponseUtil.badRequestMap("用户名已存在");
            }

            // 创建用户（密码不在此处加密，交由服务层统一加密，避免二次加密）
            User user = new User();
            user.setUsername(registrationVo.getUsername());
            user.setPassword(registrationVo.getPassword());
            user.setUserEmail(registrationVo.getEmail());
            user.setPhoneNumber(registrationVo.getPhone());
            user.setNickname(registrationVo.getNickname());
            user.setBio(registrationVo.getBio());

            // 注册用户并自动分配默认角色
            authService.register(user);
            User registeredUser = authService.findByUsername(user.getUsername());
            if (registeredUser != null) {
                log.info("用户注册成功: userId={}, username={}", registeredUser.getId(), registeredUser.getUsername());
                
                // 生成JWT令牌
                String token = jwtUtil.generateToken(registeredUser.getId(), registeredUser.getUsername());

                Map<String, Object> result = new HashMap<>();
                result.put("user", registeredUser);
                result.put("token", token);

                return ApiResponseUtil.successMap(result, "注册成功");
            } else {
                return ApiResponseUtil.badRequestMap("注册失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "用户注册");
        }
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody @Valid LoginVo loginVo) {
        try {
            // 验证用户凭据 - 通过登录获取用户和令牌
            String token = authService.login(loginVo.getUsername(), loginVo.getPassword());
            User user = authService.getUserInfo(token);
            if (user != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("user", user);
                result.put("token", token);
                result.put("expiresIn", jwtUtil.getExpirationTimeSingle(token));

                return ApiResponseUtil.successMap(result, "登录成功");
            } else {
                return ApiResponseUtil.badRequestMap("用户名或密码错误");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "用户登录");
        }
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                // 将令牌加入黑名单（需要实现令牌黑名单机制）
                jwtUtil.blacklistToken(token);
            }

            return ApiResponseUtil.successString("登出成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "用户登出");
        }
    }

  
    /**
     * 刷新令牌
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ApiResponseUtil.badRequestMap("缺少认证令牌");
            }

            String oldToken = authorization.substring(7);
            String username = jwtUtil.extractUsername(oldToken);

            if (jwtUtil.isTokenBlacklisted(oldToken) || jwtUtil.isTokenExpired(oldToken)) {
                return ApiResponseUtil.unauthorizedMap("令牌无效或已过期");
            }

            // 根据用户名获取用户ID
            User user = authService.findByUsername(username);
            if (user == null) {
                return ApiResponseUtil.notFoundMap("用户不存在");
            }

            // 生成新令牌
            String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());

            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("expiresIn", jwtUtil.getExpirationTimeSingle(newToken));

            // 将旧令牌加入黑名单
            jwtUtil.blacklistToken(oldToken);

            return ApiResponseUtil.successMap(result, "令牌刷新成功");
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "刷新令牌");
        }
    }

    /**
     * 验证令牌
     * POST /api/auth/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ApiResponseUtil.badRequestMap("缺少认证令牌");
            }

            String token = authorization.substring(7);
            boolean isValid = !jwtUtil.isTokenExpired(token) && !jwtUtil.isTokenBlacklisted(token);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("username", isValid ? jwtUtil.extractUsername(token) : null);
            result.put("expiresAt", isValid ? jwtUtil.getExpirationDate(token) : null);

            return ApiResponseUtil.successMap(result);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionMap(e, "验证令牌");
        }
    }

    /**
     * 修改密码
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid PasswordChangeVo passwordChangeVo,
            @RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ApiResponseUtil.badRequestString("缺少认证令牌");
            }

            String token = authorization.substring(7);
            String username = jwtUtil.extractUsername(token);

            // 验证确认密码
            if (!passwordChangeVo.getNewPassword().equals(passwordChangeVo.getConfirmPassword())) {
                return ApiResponseUtil.badRequestString("新密码和确认密码不一致");
            }

            User user = authService.findByUsername(username);
            if (user == null) {
                return ApiResponseUtil.notFoundString("用户不存在");
            }

            // 验证当前密码
            if (!passwordEncoder.matches(passwordChangeVo.getCurrentPassword(), user.getPassword())) {
                return ApiResponseUtil.badRequestString("当前密码错误");
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(passwordChangeVo.getNewPassword()));
            boolean updated = authService.updateUser(user);

            if (updated) {
                // 将所有令牌加入黑名单，强制重新登录
                jwtUtil.blacklistAllUserTokens(username);

                return ApiResponseUtil.successString("密码修改成功，请重新登录");
            } else {
                return ApiResponseUtil.badRequestString("密码修改失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "修改密码");
        }
    }

    /**
     * 忘记密码 - 发送重置邮件
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPasswordVo forgotPasswordVo) {
        try {
            boolean sent = authService.sendPasswordResetEmail(forgotPasswordVo.getEmail().trim());
            if (sent) {
                return ApiResponseUtil.successString("重置邮件已发送");
            } else {
                return ApiResponseUtil.badRequestString("发送重置邮件失败");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "发送重置邮件");
        }
    }

    /**
     * 重置密码
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid PasswordResetVo passwordResetVo) {
        try {
            // 验证确认密码
            if (!passwordResetVo.getNewPassword().equals(passwordResetVo.getConfirmPassword())) {
                return ApiResponseUtil.badRequestString("新密码和确认密码不一致");
            }

            boolean reset = authService.resetPassword(passwordResetVo.getResetToken(), passwordResetVo.getNewPassword());
            if (reset) {
                return ApiResponseUtil.successString("密码重置成功");
            } else {
                return ApiResponseUtil.badRequestString("重置令牌无效或已过期");
            }
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionString(e, "重置密码");
        }
    }

    /**
     * 验证重置令牌
     * GET /api/auth/verify-reset-token?token=xxx
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(@RequestParam String token) {
        try {
            // 使用ValidationUtils进行令牌验证（使用搜索关键词验证方法）
            if (!ValidationUtils.validateSearchKeyword(token)) {
                return ApiResponseUtil.badRequestBoolean("重置令牌格式不正确");
            }

            // 对令牌进行安全清理
            String sanitizedToken = ValidationUtils.sanitizeSearchKeyword(token.trim());

            boolean valid = authService.verifyResetToken(sanitizedToken);
            return ApiResponseUtil.successBoolean(valid);
        } catch (Exception e) {
            return ApiResponseUtil.handleServiceExceptionBoolean(e, "验证重置令牌");
        }
    }
}