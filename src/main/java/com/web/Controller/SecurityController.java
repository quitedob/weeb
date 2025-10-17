package com.web.Controller;

import com.web.common.ApiResponse;
import com.web.model.User;
import com.web.service.RBACService;
import com.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户安全中心控制器
 * 提供用户账户安全管理的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
public class SecurityController {

    @Autowired
    private RBACService rbacService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户的安全中心信息
     * @param request HTTP请求对象，用于获取当前用户ID
     * @return 安全中心数据
     */
    @GetMapping("/center")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityCenter(HttpServletRequest request) {
        // 从请求中获取当前用户ID（这里假设通过某种方式获取）
        Long userId = getCurrentUserId(request);

        Map<String, Object> securityData = Map.of(
            "userId", userId,
            "permissions", rbacService.getUserPermissions(userId),
            "roles", rbacService.getUserRoles(userId),
            "isAdmin", rbacService.isAdmin(userId),
            "isSuperAdmin", rbacService.isSuperAdmin(userId)
        );

        return ResponseEntity.ok(ApiResponse.success(securityData));
    }

    /**
     * 获取当前用户的登录会话列表
     * @param request HTTP请求对象
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserSessions(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        // 这里需要调用会话管理服务获取用户的所有活跃会话
        List<Map<String, Object>> sessions = List.of(
            Map.of(
                "sessionId", "session_1",
                "device", "Chrome on Windows",
                "ip", "192.168.1.100",
                "location", "北京市",
                "loginTime", "2023-10-01 10:00:00",
                "lastActivity", "2023-10-01 15:30:00",
                "isCurrent", true
            ),
            Map.of(
                "sessionId", "session_2",
                "device", "Safari on iPhone",
                "ip", "192.168.1.101",
                "location", "上海市",
                "loginTime", "2023-10-01 14:00:00",
                "lastActivity", "2023-10-01 14:30:00",
                "isCurrent", false
            )
        );

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * 强制登出指定会话
     * @param sessionId 会话ID
     * @param request HTTP请求对象
     * @return 登出结果
     */
    @PostMapping("/sessions/{sessionId}/logout")
    public ResponseEntity<ApiResponse<Boolean>> logoutSession(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        // 这里需要调用会话管理服务强制登出会话
        boolean loggedOut = true; // 假设登出成功

        return ResponseEntity.ok(ApiResponse.success(loggedOut));
    }

    /**
     * 强制登出所有其他会话（保留当前会话）
     * @param request HTTP请求对象
     * @return 登出结果
     */
    @PostMapping("/sessions/logout-all")
    public ResponseEntity<ApiResponse<Boolean>> logoutAllOtherSessions(HttpServletRequest request) {
        // 这里需要调用会话管理服务登出所有其他会话
        boolean loggedOut = true; // 假设登出成功

        return ResponseEntity.ok(ApiResponse.success(loggedOut));
    }

    /**
     * 获取登录历史记录
     * @param request HTTP请求对象
     * @param page 页码
     * @param pageSize 每页大小
     * @return 登录历史记录
     */
    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoginHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Long userId = getCurrentUserId(request);

        // 这里需要调用日志服务获取登录历史
        Map<String, Object> history = Map.of(
            "list", List.of(
                Map.of(
                    "loginTime", "2023-10-01 10:00:00",
                    "ip", "192.168.1.100",
                    "device", "Chrome on Windows",
                    "location", "北京市",
                    "status", "成功"
                ),
                Map.of(
                    "loginTime", "2023-10-01 09:00:00",
                    "ip", "192.168.1.101",
                    "device", "Safari on iPhone",
                    "location", "上海市",
                    "status", "成功"
                )
            ),
            "total", 2L
        );

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 获取安全事件记录
     * @param request HTTP请求对象
     * @param page 页码
     * @param pageSize 每页大小
     * @return 安全事件记录
     */
    @GetMapping("/security-events")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityEvents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Long userId = getCurrentUserId(request);

        // 这里需要调用安全审计服务获取安全事件
        Map<String, Object> events = Map.of(
            "list", List.of(
                Map.of(
                    "eventTime", "2023-10-01 10:00:00",
                    "eventType", "密码修改",
                    "ip", "192.168.1.100",
                    "device", "Chrome on Windows",
                    "location", "北京市",
                    "description", "用户成功修改密码"
                ),
                Map.of(
                    "eventTime", "2023-10-01 09:00:00",
                    "eventType", "登录失败",
                    "ip", "192.168.1.102",
                    "device", "Unknown",
                    "location", "未知",
                    "description", "密码错误，登录失败"
                )
            ),
            "total", 2L
        );

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 启用两因素认证
     * @param request HTTP请求对象
     * @return 两因素认证配置信息
     */
    @PostMapping("/enable-2fa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enable2FA(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        // 这里需要调用两因素认证服务启用2FA
        Map<String, Object> twoFAData = Map.of(
            "enabled", true,
            "qrCodeUrl", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
            "secret", "JBSWY3DPEHPK3PXP",
            "backupCodes", List.of("1111-2222", "3333-4444", "5555-6666")
        );

        return ResponseEntity.ok(ApiResponse.success(twoFAData));
    }

    /**
     * 禁用两因素认证
     * @param request HTTP请求对象
     * @param verificationCode 验证码
     * @return 禁用结果
     */
    @PostMapping("/disable-2fa")
    public ResponseEntity<ApiResponse<Boolean>> disable2FA(
            HttpServletRequest request,
            @RequestBody Map<String, String> requestData) {

        String verificationCode = requestData.get("verificationCode");
        Long userId = getCurrentUserId(request);

        // 这里需要调用两因素认证服务禁用2FA
        boolean disabled = true; // 假设禁用成功

        return ResponseEntity.ok(ApiResponse.success(disabled));
    }

    /**
     * 验证两因素认证码
     * @param request HTTP请求对象
     * @param verificationCode 验证码
     * @return 验证结果
     */
    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<Boolean>> verify2FA(
            HttpServletRequest request,
            @RequestBody Map<String, String> requestData) {

        String verificationCode = requestData.get("verificationCode");
        Long userId = getCurrentUserId(request);

        // 这里需要调用两因素认证服务验证验证码
        boolean verified = true; // 假设验证成功

        return ResponseEntity.ok(ApiResponse.success(verified));
    }

    /**
     * 修改密码
     * @param request HTTP请求对象
     * @param passwordData 密码数据
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Boolean>> changePassword(
            HttpServletRequest request,
            @RequestBody Map<String, String> passwordData) {

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");
        Long userId = getCurrentUserId(request);

        // 这里需要调用用户服务修改密码
        boolean changed = userService.changePassword(userId, currentPassword, newPassword);

        return ResponseEntity.ok(ApiResponse.success(changed));
    }

    /**
     * 获取用户权限列表
     * @param request HTTP请求对象
     * @return 权限列表
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Set<String>>> getUserPermissions(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Set<String> permissions = rbacService.getUserPermissions(userId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    /**
     * 检查用户是否有指定权限
     * @param permission 权限名称
     * @param request HTTP请求对象
     * @return 是否有权限
     */
    @GetMapping("/has-permission/{permission}")
    public ResponseEntity<ApiResponse<Boolean>> hasPermission(
            @PathVariable String permission,
            HttpServletRequest request) {

        Long userId = getCurrentUserId(request);
        boolean hasPermission = rbacService.hasPermission(userId, permission);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }

    /**
     * 获取用户角色列表
     * @param request HTTP请求对象
     * @return 角色列表
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<Set<String>>> getUserRoles(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Set<String> roles = rbacService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * 检查用户是否为管理员
     * @param request HTTP请求对象
     * @return 是否为管理员
     */
    @GetMapping("/is-admin")
    public ResponseEntity<ApiResponse<Boolean>> isAdmin(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean isAdmin = rbacService.isAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success(isAdmin));
    }

    /**
     * 获取账户安全评分
     * @param request HTTP请求对象
     * @return 安全评分和建议
     */
    @GetMapping("/security-score")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityScore(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        // 这里需要计算安全评分
        Map<String, Object> scoreData = Map.of(
            "score", 85,
            "level", "良好",
            "suggestions", List.of(
                "建议启用两因素认证",
                "建议定期修改密码",
                "建议检查登录设备"
            ),
            "checks", Map.of(
                "has2FA", false,
                "passwordStrength", "强",
                "recentLogin", true,
                "multipleDevices", true
            )
        );

        return ResponseEntity.ok(ApiResponse.success(scoreData));
    }

    /**
     * 从HTTP请求中获取当前用户ID的辅助方法
     * @param request HTTP请求对象
     * @return 用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 这里应该从JWT token或session中获取用户ID
        // 暂时返回一个示例用户ID
        return 1L;
    }
}
