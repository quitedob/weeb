package com.web.service.Impl;

import com.web.service.PasswordResetService;
import com.web.service.AuthService;
import com.web.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 密码重置服务实现类
 * 提供基于令牌的安全密码重置功能
 */
@Slf4j
@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Redis键前缀
    private static final String RESET_TOKEN_PREFIX = "password_reset_token:";
    private static final String RESET_ATTEMPTS_PREFIX = "reset_attempts:";

    // 令牌过期时间（15分钟）
    private static final long TOKEN_EXPIRE_MINUTES = 15;

    // 最大重试次数
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public Map<String, Object> sendPasswordResetLink(String email) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证邮箱格式
            if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
                result.put("success", false);
                result.put("message", "邮箱格式不正确");
                return result;
            }

            // 检查请求频率限制
            String attemptsKey = RESET_ATTEMPTS_PREFIX + email;
            String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
            int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

            if (attempts >= MAX_ATTEMPTS) {
                result.put("success", false);
                result.put("message", "请求过于频繁，请稍后再试");
                return result;
            }

            // 查找用户
            User user = authService.findByEmail(email.trim());
            if (user == null) {
                // 为了安全，即使用户不存在也返回成功信息
                result.put("success", true);
                result.put("message", "如果该邮箱已注册，重置链接已发送");
                return result;
            }

            // 生成重置令牌
            String resetToken = generateSecureToken();
            String tokenKey = RESET_TOKEN_PREFIX + resetToken;

            // 存储令牌信息（包含用户ID和邮箱）
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("userId", user.getId().toString());
            tokenData.put("email", email);
            tokenData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 存储到Redis，设置过期时间
            redisTemplate.opsForHash().putAll(tokenKey, tokenData);
            redisTemplate.expire(tokenKey, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 更新请求次数
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts + 1), 1, TimeUnit.HOURS);

            // 模拟发送邮件（实际项目中应使用邮件服务）
            String resetLink = String.format("http://localhost:3000/reset-password?token=%s", resetToken);
            log.info("密码重置链接已生成: {} for user: {}", resetLink, user.getUsername());

            result.put("success", true);
            result.put("message", "重置链接已发送到您的邮箱");
            result.put("debugToken", resetToken); // 开发环境调试用，生产环境应删除

            return result;

        } catch (Exception e) {
            log.error("发送密码重置链接失败", e);
            result.put("success", false);
            result.put("message", "系统错误，请稍后重试");
            return result;
        }
    }

    @Override
    public Map<String, Object> validateResetToken(String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (token == null || token.trim().isEmpty()) {
                result.put("valid", false);
                result.put("message", "重置令牌不能为空");
                return result;
            }

            String tokenKey = RESET_TOKEN_PREFIX + token;
            Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(tokenKey);

            if (tokenData == null || tokenData.isEmpty()) {
                result.put("valid", false);
                result.put("message", "重置令牌无效或已过期");
                return result;
            }

            // 检查令牌是否过期
            String timestampStr = (String) tokenData.get("timestamp");
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (timestamp.plusMinutes(TOKEN_EXPIRE_MINUTES).isBefore(LocalDateTime.now())) {
                // 清理过期令牌
                redisTemplate.delete(tokenKey);
                result.put("valid", false);
                result.put("message", "重置令牌已过期");
                return result;
            }

            result.put("valid", true);
            result.put("message", "令牌有效");
            result.put("email", tokenData.get("email"));

            return result;

        } catch (Exception e) {
            log.error("验证重置令牌失败", e);
            result.put("valid", false);
            result.put("message", "系统错误，请稍后重试");
            return result;
        }
    }

    @Override
    public Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证令牌
            Map<String, Object> validationResult = validateResetToken(token);
            if (!(Boolean) validationResult.get("valid")) {
                result.put("success", false);
                result.put("message", validationResult.get("message"));
                return result;
            }

            // 验证密码
            String passwordError = validatePassword(newPassword, confirmPassword);
            if (passwordError != null) {
                result.put("success", false);
                result.put("message", passwordError);
                return result;
            }

            // 获取用户信息
            String tokenKey = RESET_TOKEN_PREFIX + token;
            Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(tokenKey);
            Long userId = Long.valueOf((String) tokenData.get("userId"));

            // 查找用户
            User user = authService.findByUserID(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            boolean updateResult = authService.updateAuth(user);

            if (updateResult) {
                // 清理重置令牌
                invalidateResetToken(token);

                // 清理请求次数记录
                String email = (String) tokenData.get("email");
                redisTemplate.delete(RESET_ATTEMPTS_PREFIX + email);

                log.info("密码重置成功: userId={}, email={}", userId, email);

                result.put("success", true);
                result.put("message", "密码重置成功");
                return result;
            } else {
                result.put("success", false);
                result.put("message", "密码重置失败，请稍后重试");
                return result;
            }

        } catch (Exception e) {
            log.error("重置密码失败", e);
            result.put("success", false);
            result.put("message", "系统错误，请稍后重试");
            return result;
        }
    }

    @Override
    public void invalidateResetToken(String token) {
        try {
            if (token != null && !token.trim().isEmpty()) {
                String tokenKey = RESET_TOKEN_PREFIX + token;
                redisTemplate.delete(tokenKey);
            }
        } catch (Exception e) {
            log.error("清理重置令牌失败", e);
        }
    }

    /**
     * 生成安全的随机令牌
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return UUID.nameUUIDFromBytes(bytes).toString().replace("-", "");
    }

    /**
     * 验证密码强度
     */
    private String validatePassword(String password, String confirmPassword) {
        if (password == null || password.trim().isEmpty()) {
            return "新密码不能为空";
        }

        if (password.length() < 8 || password.length() > 100) {
            return "密码长度必须在8-100个字符之间";
        }

        if (!password.equals(confirmPassword)) {
            return "两次输入的密码不一致";
        }

        // 密码强度验证：至少包含一个字母、一个数字
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$")) {
            return "密码必须包含至少一个字母和一个数字";
        }

        return null; // 验证通过
    }
}