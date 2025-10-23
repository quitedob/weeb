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
     * 同时写入自定义声明 username，便于过滤器按用户名加载用户
     * @param userId 用户唯一ID
     * @param username 用户名
     * @return JWT令牌字符串
     */
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 以userId为主体
                .claim("username", username) // 附带用户名，供下游解析
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(this.key) // 使用密钥签名
                .compact();
    }

    /**
     * 兼容旧签名：仅传入用户ID时生成token（不推荐，仅为兼容）
     */
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(this.key)
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

    /**
     * 从JWT令牌中提取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            // 优先读取自定义username声明；若不存在，回退为subject（兼容旧token）
            String username = claims.get("username", String.class);
            return (username != null && !username.isEmpty()) ? username : claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取令牌过期时间
     * @param token JWT令牌 (可选参数，如果不提供则返回默认过期时间)
     * @return 过期时间（毫秒）
     */
    public long getExpirationTime(String... token) {
        try {
            // 如果没有提供token参数，返回默认过期时间
            if (token == null || token.length == 0) {
                return System.currentTimeMillis() + expiration;
            }
            String tokenString = token[0];
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(tokenString)
                    .getBody();
            return claims.getExpiration().getTime();
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * 获取令牌过期时间（重载方法）
     * @param token JWT令牌
     * @return 过期时间（毫秒）
     */
    public long getExpirationTimeSingle(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().getTime();
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * 获取令牌过期日期
     * @param token JWT令牌
     * @return 过期日期
     */
    public Date getExpirationDate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 检查令牌是否过期
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 检查令牌是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        // 简单实现，实际项目中应该使用Redis或数据库存储黑名单
        // 这里返回false，表示没有黑名单功能
        return false;
    }

    /**
     * 将令牌加入黑名单
     * @param token JWT令牌
     */
    public void blacklistToken(String token) {
        // 实际项目中应该将token加入Redis或数据库黑名单
        // 这里为空实现，因为当前没有黑名单存储
    }

    /**
     * 将用户所有令牌加入黑名单
     * @param username 用户名
     */
    public void blacklistAllUserTokens(String username) {
        // 实际项目中应该根据用户名将所有相关token加入黑名单
        // 这里为空实现，因为当前没有黑名单存储
    }

    /**
     * 发送密码重置邮件
     * @param email 邮箱地址
     */
    public void sendPasswordResetEmail(String email) {
        // 实际项目中应该调用邮件服务发送重置邮件
        // 这里为空实现，因为当前没有邮件服务集成
    }

    /**
     * 重置密码
     * @param token 重置令牌
     * @param newPassword 新密码
     */
    public void resetPassword(String token, String newPassword) {
        // 实际项目中应该验证token并更新用户密码
        // 这里为空实现，因为当前没有密码重置服务集成
    }

    /**
     * 验证重置令牌
     * @param token 重置令牌
     * @return 是否有效
     */
    public boolean verifyResetToken(String token) {
        // 实际项目中应该验证重置令牌的有效性
        // 这里返回false，表示没有重置令牌功能
        return false;
    }

    /**
     * 验证令牌是否有效（带用户名验证）
     * @param token JWT令牌
     * @param username 用户名
     * @return 是否有效
     */
    public boolean isTokenValid(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token) && !isTokenBlacklisted(token);
        } catch (Exception e) {
            return false;
        }
    }
}
