package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.PasswordResetDeliveryService;
import com.web.service.PasswordResetService;
import com.web.util.JwtUtil;
import com.web.util.ValidationUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final String TOKEN_PREFIX = "auth:password-reset:";
    private static final String ATTEMPTS_PREFIX = "auth:password-reset-attempts:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final PasswordResetDeliveryService delivery;

    public PasswordResetServiceImpl(AuthMapper authMapper, UserMapper userMapper, JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder, StringRedisTemplate redis, PasswordResetDeliveryService delivery) {
        this.authMapper = authMapper;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
        this.delivery = delivery;
    }

    @Override
    public Map<String, Object> sendPasswordResetLink(String email) {
        if (email == null || !ValidationUtils.validateEmail(email.trim())) throw new WeebException("邮箱格式不正确");
        // Report configuration failures before account lookup to avoid account enumeration.
        if (!delivery.isAvailable()) throw new WeebException("密码重置邮件服务未配置");
        String address = email.trim();
        String attemptsKey = ATTEMPTS_PREFIX + digest(address.toLowerCase(java.util.Locale.ROOT));
        Long attempts = redis.opsForValue().increment(attemptsKey);
        if (Long.valueOf(1).equals(attempts)) redis.expire(attemptsKey, Duration.ofHours(1));
        if (attempts == null || attempts > 5) throw new WeebException("请求过于频繁，请稍后再试");

        User user = authMapper.findByEmail(address);
        if (user != null && Integer.valueOf(1).equals(user.getStatus())) {
            byte[] nonce = new byte[32];
            RANDOM.nextBytes(nonce);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
            String key = tokenKey(token);
            redis.opsForValue().set(key, user.getId() + ":" + digest(user.getPassword()), Duration.ofMinutes(15));
            try {
                delivery.send(address, token);
            } catch (RuntimeException e) {
                redis.delete(key);
                // Mail exceptions can contain the message body; never log or propagate them.
                throw new WeebException("重置邮件发送失败，请稍后重试");
            }
        }
        return Map.of("success", true, "message", "如果该邮箱已注册，重置链接已发送");
    }

    @Override
    public Map<String, Object> validateResetToken(String token) {
        if (!validFormat(token)) return Map.of("valid", false);
        return Map.of("valid", resolveUser(redis.opsForValue().get(tokenKey(token))) != null);
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword) {
        if (!ValidationUtils.validatePassword(newPassword) || !newPassword.equals(confirmPassword)) {
            throw new WeebException("新密码不符合要求或两次密码不一致");
        }
        if (!validFormat(token)) throw new WeebException("重置令牌无效或已过期");
        // GETDEL atomically consumes the credential before any password write.
        User user = resolveUser(redis.opsForValue().getAndDelete(tokenKey(token)));
        if (user == null) throw new WeebException("重置令牌无效或已过期");
        jwtUtil.blacklistAllUserTokens(user.getId());
        int updated = userMapper.compareAndSetPassword(user.getId(), passwordEncoder.encode(newPassword), user.getPassword());
        if (updated != 1) throw new WeebException("密码已更改，请重新申请重置链接");
        return Map.of("success", true, "message", "密码重置成功");
    }

    private User resolveUser(String value) {
        if (value == null) return null;
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return null;
        User user = authMapper.findByUserID(Long.valueOf(parts[0]));
        return user != null && Integer.valueOf(1).equals(user.getStatus())
                && digest(user.getPassword()).equals(parts[1]) ? user : null;
    }

    @Override
    public void invalidateResetToken(String token) {
        if (validFormat(token)) redis.delete(tokenKey(token));
    }

    private static boolean validFormat(String token) { return token != null && token.matches("[A-Za-z0-9_-]{43}"); }
    private static String tokenKey(String token) { return TOKEN_PREFIX + digest(token); }
    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}
