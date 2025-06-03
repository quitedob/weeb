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

    @Value("${jwt.secret}") // Injected from application properties
    private String secret; // Changed to instance field, removed static and hardcoded value

    private Key key; // Instance field for the key

    @PostConstruct
    public void init() {
        // Uses the injected instance field 'secret'
        this.key = Keys.hmacShaKeyFor(this.secret.getBytes());
    }

    // generateToken remains an instance method and uses this.key
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(this.key) // Uses instance key
                .compact();
    }

    // validateToken remains an instance method and uses this.key
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.key) // Uses instance key
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // parseToken is now an INSTANCE method and uses this.key
    public Claims parseToken(String token) {
        return Jwts.parserBuilder() // Use parserBuilder for consistency
                .setSigningKey(this.key) // Uses instance key
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // getUsernameFromToken remains an instance method and uses this.key
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.key) // Uses instance key
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public long getExpiration() {
        return expiration;
    }
}
