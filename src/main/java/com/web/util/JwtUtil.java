package com.web.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.secret}") // 注入配置的密钥

    // 令牌秘钥
    private static String secret = "weeb2333weeb2333weeb2333weeb2333";

    private Key key;

    @PostConstruct
    public void init() {
        // 使用配置的密钥生成 HS512 签名密钥
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    // 生成JWT令牌
    /**
     * 生成 JWT 令牌
     * @param username 用户名作为令牌主体
     * @return 生成的 JWT 字符串
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // 主题
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(key) // 使用密钥和算法签名
                .compact();
    }

    // 验证JWT令牌
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key) // 设置签名密钥
                    .build()
                    .parseClaimsJws(token); // 解析令牌
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 令牌无效
            return false;
        }
    }
    /**
     * 解析token
     *
     * @param token
     * @return
     */
    public static Claims parseToken(String token) {
        JwtParser jwtParser = Jwts.parser().setSigningKey(secret);
        Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
        Claims body = claimsJws.getBody();
        return body;
    }

    // 从令牌中获取用户名
    /**
     * 从 JWT 中解析出用户名（主题）
     * @param token JWT 字符串
     * @return 解析得到的用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 获取JWT的过期时间
    /**
     * 获取配置的 JWT 过期时间
     * @return JWT 过期时间（毫秒）
     */
    public long getExpiration() {
        return expiration;
    }

}
