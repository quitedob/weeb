package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.util.JwtUtil;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.service.PasswordResetService;
import com.web.util.ResultUtil;
import com.web.common.ApiResponse;
import com.web.vo.auth.LoginVo;
import com.web.vo.user.UpdateUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 认证相关的控制器，提供用户注册、登录、登出、获取用户信息、更新用户等接口
 *
 * 注意：当前包名为 com.web.Controller（首字母大写），
 * 如果项目启动时扫描不到该包，请在主应用类中使用 @ComponentScan(basePackages = {"com.web.Controller", ...})
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService; // 注入认证服务

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入密码编码器

    @Autowired
    private PasswordResetService passwordResetService; // 注入密码重置服务

    /**
     * 用户注册接口，返回统一的ApiResponse格式
     * @param payload 前端传入的用户注册信息
     * @return 统一的ApiResponse响应格式
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody Map<String, Object> payload) {
        // 参数验证
        if (payload == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        String username = (String) payload.get("username");
        String password = (String) payload.get("password");
        String email = (String) payload.get("email");

        // 必填参数验证
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        // 参数长度验证
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在3-50个字符之间");
        }
        if (password.length() < 6 || password.length() > 100) {
            throw new IllegalArgumentException("密码长度必须在6-100个字符之间");
        }

        // 邮箱格式验证
        if (!email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        // 手动映射前端传入的字段到 User 对象
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(password.trim());
        user.setUserEmail(email.trim());

        // 将 gender 转换为 sex：male -> 1, female -> 0
        String genderStr = (String) payload.get("gender");
        user.setSex("male".equalsIgnoreCase(genderStr) ? 1 : 0);

        String phone = (String) payload.get("phone");
        if (phone != null && !phone.trim().isEmpty()) {
            // 手机号格式验证
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
            user.setPhoneNumber(phone.trim());
        }

        // 其它字段保持默认或由数据库默认值处理
        authService.register(user); // 调用服务进行注册

        return ResponseEntity.ok(ApiResponse.success("注册成功"));
    }

    /**
     * 用户登录接口
     * @param loginVo 前端传入的登录信息VO
     * @return 返回登录结果（成功则带有 token，失败则返回错误信息）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody @Valid LoginVo loginVo) {
        // 调用服务进行登录，并获取JWT令牌
        String token = authService.login(loginVo.getUsername(), loginVo.getPassword());
        // 返回包含令牌的响应
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取用户信息接口，需要提供 Authorization: Bearer <token>
     * 此接口用于快速获取部分信息（如 id, username）
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        User user = authService.getUserInfo(token);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 用户登出接口，需要提供 Authorization: Bearer <token>
     * @param authorizationHeader 从请求头中获取的令牌
     * @return 返回登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }

    /**
     * 获取验证码（占位实现：返回简单文本验证码）
     */
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCaptcha() {
        String code = String.format("%04d", new Random().nextInt(10000));
        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", java.util.UUID.randomUUID().toString());
        data.put("code", code); // TODO: 生产环境应返回图片Base64并结合Redis校验
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 发送密码重置链接（安全实现）
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendPasswordResetLink(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");

        Map<String, Object> result = passwordResetService.sendPasswordResetLink(email);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
        } else {
            throw new IllegalArgumentException((String) result.get("message"));
        }
    }

    /**
     * 验证重置令牌
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateResetToken(@RequestParam String token) {
        Map<String, Object> result = passwordResetService.validateResetToken(token);

        if ((Boolean) result.get("valid")) {
            return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
        } else {
            throw new IllegalArgumentException((String) result.get("message"));
        }
    }

    /**
     * 重置密码（安全实现：通过令牌验证）
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestBody Map<String, Object> payload) {
        String token = (String) payload.get("token");
        String newPassword = (String) payload.get("newPassword");
        String confirmPassword = (String) payload.get("confirmPassword");

        Map<String, Object> result = passwordResetService.resetPassword(token, newPassword, confirmPassword);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
        } else {
            throw new IllegalArgumentException((String) result.get("message"));
        }
    }

    /**
     * 执行密码重置（前端页面调用的接口）
     * @param payload 包含令牌和新密码的数据
     * @return 操作结果
     */
    @PostMapping("/password/execute-reset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executePasswordReset(@RequestBody Map<String, Object> payload) {
        String token = (String) payload.get("token");
        String newPassword = (String) payload.get("newPassword");

        Map<String, Object> result = passwordResetService.resetPassword(token, newPassword, newPassword);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, (String) result.get("message")));
        }
    }

    /**
     * 示例的RESTful API，需要认证
     * @return 返回一段测试性文字
     */
    @GetMapping("/restful")
    public ResponseEntity<ApiResponse<String>> restfulApi() {
        return ResponseEntity.ok(ApiResponse.success("这是一个受保护的RESTful API接口"));
    }

    /**
     * 通过用户名查找用户ID ----- 此接口目前放宽权限
     * @param username 用户名
     * @return 用户ID和用户名
     */
    @GetMapping("/findByUsername")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findByUsername(@RequestParam String username) {
        User user = authService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(1, "用户未找到"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 通过用户ID查找用户名 ----- 此接口目前放宽权限
     * @param userID 用户ID
     * @return 用户ID和用户名
     */
    @GetMapping("/findByUserID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findByUserID(@RequestParam Long userID) {
        User user = authService.findByUserID(userID);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(1, "用户未找到"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取所有用户列表，支持按类型降序排序（具体排序逻辑在 Mapper 中实现）
     * @return 返回用户列表（不包含密码）
     */
    @UrlLimit
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<User>>> listUser() {
        List<User> result = authService.listUser();
        result.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有用户的 Map，键为用户ID，支持按类型降序排序（具体排序逻辑在 Mapper 中实现）
     * @return 返回用户 Map（不包含密码）
     */
    @UrlLimit
    @GetMapping("/list/map")
    public ResponseEntity<ApiResponse<Map<Long, User>>> listMapUser() {
        Map<Long, User> result = authService.listMapUser();
        result.values().forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取在线用户列表
     * @return 返回在线用户列表（此处示例中返回的是字符串类型的用户ID集合）
     */
    @UrlLimit
    @GetMapping("/online/web")
    public ResponseEntity<ApiResponse<List<String>>> onlineWeb() {
        List<String> result = authService.onlineWeb();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新用户信息（只更新允许更新的字段）
     * @param userid 从请求头中获取的 userid（示例，非 JWT 令牌，此接口与下面不冲突）
     * @param updateVo 前端传入的更新用户信息VO
     * @return 更新结果（成功或失败）
     */
    @UrlLimit
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateUser(@RequestHeader("userid") String userid,
                                                         @RequestBody @Valid UpdateUserVo updateVo) {
        User userToUpdate = new User();
        userToUpdate.setId(Long.parseLong(userid));
        userToUpdate.setUsername(updateVo.getUsername());
        userToUpdate.setAvatar(updateVo.getAvatar());
        userToUpdate.setNickname(updateVo.getNickname());
        userToUpdate.setBio(updateVo.getBio());
        boolean result = authService.updateUser(userToUpdate);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "更新失败"));
        }
    }


    /**
     * 提取 Authorization 头中的 JWT 令牌（去掉 Bearer 前缀）
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        } else {
            throw new RuntimeException("无效的授权头信息");
        }
    }

    /**
     * 响应类，用于包装JWT令牌（如果需要进行更复杂的响应，可以扩展此类）
     */
    public static class AuthResponse {
        private String token;

        public AuthResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
        public void setToken(String token) {
            this.token = token;
        }
    }
}
