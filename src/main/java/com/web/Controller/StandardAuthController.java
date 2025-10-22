package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.JwtUtil;
import com.web.vo.auth.LoginVo;
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
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码和确认密码不一致"));
            }

            // 检查用户名是否已存在
            if (authService.findByUsername(registrationVo.getUsername()) != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名已存在"));
            }

            // 创建用户
            User user = new User();
            user.setUsername(registrationVo.getUsername());
            user.setPassword(passwordEncoder.encode(registrationVo.getPassword()));
            user.setEmail(registrationVo.getEmail());
            user.setPhone(registrationVo.getPhone());
            user.setNickname(registrationVo.getNickname());
            user.setBio(registrationVo.getBio());

            User registeredUser = authService.register(user);
            if (registeredUser != null) {
                // 生成JWT令牌
                String token = jwtUtil.generateToken(registeredUser.getId());

                Map<String, Object> result = new HashMap<>();
                result.put("user", registeredUser);
                result.put("token", token);

                return ResponseEntity.status(201)
                        .body(ApiResponse.success("注册成功", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("注册失败"));
            }
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("注册失败: " + e.getMessage()));
        }
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody @Valid LoginVo loginVo) {
        try {
            // 验证用户凭据
            User user = authService.authenticate(loginVo.getUsername(), loginVo.getPassword());
            if (user != null) {
                // 生成JWT令牌
                String token = jwtUtil.generateToken(user.getId());

                Map<String, Object> result = new HashMap<>();
                result.put("user", user);
                result.put("token", token);
                result.put("expiresIn", jwtUtil.getExpirationTime());

                return ResponseEntity.ok(ApiResponse.success("登录成功", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名或密码错误"));
            }
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("登录失败: " + e.getMessage()));
        }
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                // 将令牌加入黑名单（需要实现令牌黑名单机制）
                jwtUtil.blacklistToken(token);
            }

            return ResponseEntity.ok(ApiResponse.success("登出成功"));
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("登出失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前认证用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少认证令牌"));
            }

            String token = authorization.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (jwtUtil.isTokenBlacklisted(token)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("令牌已失效"));
            }

            User user = authService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 不返回密码
            user.setPassword(null);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("获取用户信息失败: " + e.getMessage()));
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
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少认证令牌"));
            }

            String oldToken = authorization.substring(7);
            String username = jwtUtil.extractUsername(oldToken);

            if (jwtUtil.isTokenBlacklisted(oldToken) || jwtUtil.isTokenExpired(oldToken)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("令牌无效或已过期"));
            }

            // 生成新令牌
            String newToken = jwtUtil.generateToken(username);

            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("expiresIn", jwtUtil.getExpirationTime());

            // 将旧令牌加入黑名单
            jwtUtil.blacklistToken(oldToken);

            return ResponseEntity.ok(ApiResponse.success("令牌刷新成功", result));
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("刷新令牌失败: " + e.getMessage()));
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
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少认证令牌"));
            }

            String token = authorization.substring(7);
            boolean isValid = !jwtUtil.isTokenExpired(token) && !jwtUtil.isTokenBlacklisted(token);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("username", isValid ? jwtUtil.extractUsername(token) : null);
            result.put("expiresAt", isValid ? jwtUtil.getExpirationDate(token) : null);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("验证令牌失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("验证令牌失败: " + e.getMessage()));
        }
    }

    /**
     * 修改密码
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody @Valid Map<String, String> passwordRequest,
            @RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少认证令牌"));
            }

            String token = authorization.substring(7);
            String username = jwtUtil.extractUsername(token);

            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前密码和新密码不能为空"));
            }

            User user = authService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 验证当前密码
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前密码错误"));
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            boolean updated = authService.updateUser(user);

            if (updated) {
                // 将所有令牌加入黑名单，强制重新登录
                jwtUtil.blacklistAllUserTokens(username);

                return ResponseEntity.ok(ApiResponse.success("密码修改成功，请重新登录"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码修改失败"));
            }
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("修改密码失败: " + e.getMessage()));
        }
    }

    /**
     * 忘记密码 - 发送重置邮件
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("邮箱不能为空"));
            }

            boolean sent = authService.sendPasswordResetEmail(email.trim());
            if (sent) {
                return ResponseEntity.ok(ApiResponse.success("重置邮件已发送"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("发送重置邮件失败"));
            }
        } catch (Exception e) {
            log.error("发送重置邮件失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("发送重置邮件失败: " + e.getMessage()));
        }
    }

    /**
     * 重置密码
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid Map<String, String> resetRequest) {
        try {
            String token = resetRequest.get("token");
            String newPassword = resetRequest.get("newPassword");

            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("重置令牌和新密码不能为空"));
            }

            boolean reset = authService.resetPassword(token, newPassword);
            if (reset) {
                return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("重置令牌无效或已过期"));
            }
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("重置密码失败: " + e.getMessage()));
        }
    }

    /**
     * 验证重置令牌
     * GET /api/auth/verify-reset-token?token=xxx
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(@RequestParam String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("重置令牌不能为空"));
            }

            boolean valid = authService.verifyResetToken(token.trim());
            return ResponseEntity.ok(ApiResponse.success(valid));
        } catch (Exception e) {
            log.error("验证重置令牌失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.systemError("验证重置令牌失败: " + e.getMessage()));
        }
    }
}