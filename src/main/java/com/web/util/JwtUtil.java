package com.web.util;

import io.jsonwebtoken.*; // JWT相关库
import io.jsonwebtoken.security.Keys; // 用于生成密钥
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * JwtUtil 工具类，负责生成、验证和解析JWT令牌
 */
@Component
public class JwtUtil {
    @Value("${jwt.expiration}")
    private long expiration; // 令牌有效期（毫秒）

    @Value("${jwt.secret}") // 从配置文件注入密钥
    private String secret;

    private Key key; // 用于签名的密钥对象

    @PostConstruct
    public void init() {
        // 验证JWT secret安全性
        validateJwtSecret();

        // 初始化密钥对象
        this.key = Keys.hmacShaKeyFor(this.secret.getBytes());
    }

    /**
     * 验证JWT secret的安全性
     */
    private void validateJwtSecret() {
        // 检查是否为空或使用默认值
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret不能为空，请在配置文件中设置jwt.secret");
        }

        // 检查是否使用默认值（开发环境警告，生产环境报错）
        String defaultSecret = "mySecretKey123456789012345678901234567890";
        if (defaultSecret.equals(secret)) {
            String errorMsg = "检测到使用默认JWT secret，这是不安全的！请在配置文件中设置强密钥";
            if (isProductionEnvironment()) {
                throw new IllegalStateException(errorMsg);
            } else {
                System.err.println("警告: " + errorMsg);
            }
        }

        // 验证密钥长度（至少32字节）
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret长度不足，至少需要32个字符");
        }

        // 验证密钥复杂度
        boolean hasUpper = secret.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = secret.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = secret.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = secret.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            String warningMsg = "建议使用包含大小写字母、数字和特殊字符的强密钥";
            if (isProductionEnvironment()) {
                System.err.println("警告: " + warningMsg);
            } else {
                System.out.println("提示: " + warningMsg);
            }
        }
    }

    /**
     * 检查是否为生产环境
     */
    private boolean isProductionEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active", "");
        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        return "prod".equals(activeProfile) || "production".equals(activeProfile) ||
               "prod".equals(envProfile) || "production".equals(envProfile);
    }

    /**
     * 生成JWT令牌，subject为userId（Long类型，转为字符串）
     * @param userId 用户唯一ID
     * @return JWT令牌字符串
     */
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 以userId为主体
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(this.key) // 使用密钥签名
                .compact();
    }

    /**
     * 验证JWT令牌有效性
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析JWT令牌，获取Claims
     * @param token JWT令牌
     * @return Claims对象
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(this.key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从JWT令牌中获取userId（Long类型）
     * @param token JWT令牌
     * @return 用户ID（Long）
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject()); // subject为userId
    }

    /**
     * 获取令牌有效期
     * @return 有效期（毫秒）
     */
    public long getExpiration() {
        return expiration;
    }
}
