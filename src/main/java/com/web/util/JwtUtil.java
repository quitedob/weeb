package com.web.util;

import io.jsonwebtoken.*; // JWT相关库
import io.jsonwebtoken.security.Keys; // 用于生成密钥
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
        // 初始化密钥对象
        this.key = Keys.hmacShaKeyFor(this.secret.getBytes());
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
