package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.util.JwtUtil;
import com.web.model.User;
import com.web.service.AuthService;
import com.web.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证相关的控制器，提供用户注册、登录、登出、获取用户信息、更新用户等接口
 *
 * 注意：当前包名为 com.web.Controller（首字母大写），
 * 如果项目启动时扫描不到该包，请在主应用类中使用 @ComponentScan(basePackages = {"com.web.Controller", ...})
 */
@RestController
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService; // 注入认证服务

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> payload) {
        try {
            // 手动映射前端传入的字段到 User 对象
            User user = new User();
            user.setUsername((String) payload.get("username"));
            user.setPassword((String) payload.get("password"));
            // 将 gender 转换为 sex：male -> 1, female -> 0
            String genderStr = (String) payload.get("gender");
            user.setSex("male".equalsIgnoreCase(genderStr) ? 1 : 0);
            user.setPhoneNumber((String) payload.get("phone"));
            user.setUserEmail((String) payload.get("email"));
            // 其它字段保持默认或由数据库默认值处理

            authService.register(user); // 调用服务进行注册
            System.out.println("Payload: " + payload);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "注册成功"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 用户登录接口
     * @param user 前端传入的用户信息（包括用户名和密码）
     * @return 返回登录结果（成功则带有 token，失败则返回错误信息）
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            // 调用服务进行登录，并获取JWT令牌
            String token = authService.login(user.getUsername(), user.getPassword());
            // 返回包含令牌的响应
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户信息接口，需要提供 Authorization: Bearer <token>
     * 此接口用于快速获取部分信息（如 id, username）
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String token = extractToken(authorizationHeader);
            User user = authService.getUserInfo(token);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 用户登出接口，需要提供 Authorization: Bearer <token>
     * @param authorizationHeader 从请求头中获取的令牌
     * @return 返回登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String token = extractToken(authorizationHeader);
            authService.logout(token);
            return ResponseEntity.ok("登出成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * 示例的RESTful API，需要认证
     * @return 返回一段测试性文字
     */
    @GetMapping("/restful")
    public ResponseEntity<?> restfulApi() {
        return ResponseEntity.ok("这是一个受保护的RESTful API接口");
    }

    /**
     * 通过用户名查找用户ID ----- 此接口目前放宽权限
     * @param username 用户名
     * @return 用户ID和用户名
     */
    @GetMapping("/findByUsername")
    public ResponseEntity<?> findByUsername(@RequestParam String username) {
        User user = authService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户未找到");
        }
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    /**
     * 通过用户ID查找用户名 ----- 此接口目前放宽权限
     * @param userID 用户ID
     * @return 用户ID和用户名
     */
    @GetMapping("/findByUserID")
    public ResponseEntity<?> findByUserID(@RequestParam Long userID) {
        User user = authService.findByUserID(userID);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户未找到");
        }
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    /**
     * 获取所有用户列表，支持按类型降序排序（具体排序逻辑在 Mapper 中实现）
     * @return 返回用户列表（不包含密码）
     */
    @UrlLimit
    @GetMapping("/list")
    public Object listUser() {
        List<User> result = authService.listUser();
        result.forEach(user -> user.setPassword(null));
        return ResultUtil.Succeed(result);
    }

    /**
     * 获取所有用户的 Map，键为用户ID，支持按类型降序排序（具体排序逻辑在 Mapper 中实现）
     * @return 返回用户 Map（不包含密码）
     */
    @UrlLimit
    @GetMapping("/list/map")
    public Object listMapUser() {
        Map<Long, User> result = authService.listMapUser();
        result.values().forEach(user -> user.setPassword(null));
        return ResultUtil.Succeed(result);
    }

    /**
     * 获取在线用户列表
     * @return 返回在线用户列表（此处示例中返回的是字符串类型的用户ID集合）
     */
    @UrlLimit
    @GetMapping("/online/web")
    public Object onlineWeb() {
        List<String> result = authService.onlineWeb();
        return ResultUtil.Succeed(result);
    }

    /**
     * 更新用户信息（只更新允许更新的字段）
     * @param userid 从请求头中获取的 userid（示例，非 JWT 令牌，此接口与下面不冲突）
     * @param updateUser 前端传入的更新用户信息
     * @return 更新结果（成功或失败）
     */
    @UrlLimit
    @PostMapping("/update")
    public Object updateUser(@RequestHeader("userid") String userid,
                             @RequestBody @Valid User updateUser) {
        User userToUpdate = new User();
        userToUpdate.setId(Long.parseLong(userid));
        userToUpdate.setUsername(updateUser.getUsername());
        userToUpdate.setAvatar(updateUser.getAvatar());
        boolean result = authService.updateUser(userToUpdate);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 根据令牌获取当前用户基本信息
     * 返回字段：username, userEmail, phoneNumber, sex
     * 需要在请求头中添加 Authorization: Bearer <token>
     */
    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfoByToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // 提取令牌
            String token = extractToken(authorizationHeader);
            // 从 token 中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.badRequest().body(ResultUtil.Fail("用户不存在"));
            }
            // 根据用户ID获取用户基本信息（只返回必要字段）
            User user = authService.getAuthById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ResultUtil.Fail("用户不存在"));
            }
            // 使用 HashMap 允许 null 值
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("userEmail", user.getUserEmail());
            data.put("phoneNumber", user.getPhoneNumber());
            data.put("sex", user.getSex());
            return ResponseEntity.ok(ResultUtil.Succeed("获取用户信息成功", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResultUtil.Fail("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 根据令牌更新当前用户基本信息
     * 请求体中只需包含需要更新的字段（username, userEmail, phoneNumber, sex）
     * 需要在请求头中添加 Authorization: Bearer <token>
     */
    @PutMapping("/user/info")
    public ResponseEntity<?> updateUserInfoByToken(@RequestHeader("Authorization") String authorizationHeader,
                                                   @RequestBody User userInfo) {
        try {
            String token = extractToken(authorizationHeader);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.badRequest().body(ResultUtil.Fail("用户不存在"));
            }
            // 强制使用当前用户的 ID，防止客户端恶意修改
            userInfo.setId(userId);
            authService.updateAuth(userInfo);
            return ResponseEntity.ok(ResultUtil.Succeed("更新用户信息成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResultUtil.Fail("更新用户信息失败: " + e.getMessage()));
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
