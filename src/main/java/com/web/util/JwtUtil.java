package com.web.util;

import com.web.mapper.AuthMapper;
import com.web.mapper.AuthTokenStateMapper;
import com.web.exception.AuthStateUnavailableException;
import com.web.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Access tokens are scoped, individually revocable and tied to a user's session generation. */
@Component
public class JwtUtil {
    private static final String SESSION_PREFIX = "auth:session-generation:";
    private static final String REVOKED_PREFIX = "auth:revoked:";
    private final StringRedisTemplate redis;
    private final AuthMapper authMapper;
    private final AuthTokenStateMapper tokenState;

    @Value("${jwt.expiration}")
    private long expiration;
    @Value("${jwt.secret}")
    private String secret;
    private Key key;

    public JwtUtil(StringRedisTemplate redis, AuthMapper authMapper, AuthTokenStateMapper tokenState) {
        this.redis = redis;
        this.authMapper = authMapper;
        this.tokenState = tokenState;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32 || expiration <= 0) {
            throw new IllegalStateException("Configure a JWT key of at least 32 bytes and a positive expiration");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    /** A login must still match the password hash that was actually verified before it acquires the user lock. */
    @Transactional
    public String generateToken(Long userId, String username, String verifiedPasswordHash) {
        if (userId == null || userId <= 0 || username == null || username.isBlank()) {
            throw new IllegalArgumentException("User identity is required");
        }
        try {
            User user = tokenState.lockUser(userId);
            if (user == null || !username.equals(user.getUsername()) || !Integer.valueOf(1).equals(user.getStatus())
                    || (verifiedPasswordHash != null && !verifiedPasswordHash.equals(user.getPassword()))) {
                throw new IllegalArgumentException("User credentials or account changed; sign in again");
            }
            tokenState.createGeneration(userId, UUID.randomUUID().toString());
            String generation = tokenState.lockGeneration(userId);
            if (generation == null) throw new IllegalStateException("Durable session state was not created");
            // A fresh credential-verified login can repair a missing/stale cache from the durable generation.
            redis.opsForValue().set(SESSION_PREFIX + userId, generation);
            return issue(user, generation);
        } catch (DataAccessException e) {
            throw new AuthStateUnavailableException(e);
        }
    }

    private String issue(User user, String generation) {
        return Jwts.builder().setSubject(user.getId().toString()).setId(UUID.randomUUID().toString())
                .claim("username", user.getUsername()).claim("purpose", "access").claim("generation", generation)
                .claim("credentialVersion", credentialVersion(user))
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key).compact();
    }

    @Transactional
    public String generateToken(Long userId) {
        User user = authMapper.findByUserID(userId);
        if (user == null) throw new IllegalArgumentException("User does not exist");
        return generateToken(userId, user.getUsername());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            User user = authMapper.findByUserID(Long.valueOf(claims.getSubject()));
            return matchesUser(claims, user) && !isRevoked(claims);
        } catch (DataAccessException e) {
            throw new AuthStateUnavailableException(e);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean matchesUser(Claims claims, User user) {
        return "access".equals(claims.get("purpose", String.class))
                && claims.getId() != null && claims.getId().length() == 36 && claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && claims.get("username", String.class) != null
                && user != null && Integer.valueOf(1).equals(user.getStatus())
                && user.getUsername().equals(claims.get("username", String.class))
                && credentialVersion(user).equals(claims.get("credentialVersion", String.class))
                && Long.parseLong(claims.getSubject()) > 0;
    }

    /** Existing Bearer renewal remains valid-token-only; consuming the old JTI permits exactly one winner. */
    @Transactional
    public String renewToken(String oldToken) {
        try {
            Claims claims = parseToken(oldToken);
            Long userId = Long.valueOf(claims.getSubject());
            User user = tokenState.lockUser(userId);
            String generation = claims.get("generation", String.class);
            if (!matchesUser(claims, user) || generation == null
                    || !generation.equals(tokenState.lockGeneration(userId)) || isRevoked(claims)) {
                throw new AccessDeniedException("Token is invalid or expired");
            }
            if (tokenState.revoke(claims.getId(), userId, claims.getExpiration()) != 1) {
                throw new AccessDeniedException("Token was already renewed or revoked");
            }
            cacheRevocation(claims);
            return issue(user, generation);
        } catch (DataAccessException e) {
            throw new AuthStateUnavailableException(e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("Token is invalid or expired");
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public Long getUserIdFromToken(String token) { return Long.valueOf(parseToken(token).getSubject()); }
    public long getExpiration() { return expiration; }

    public String extractUsername(String token) {
        try { return parseToken(token).get("username", String.class); }
        catch (JwtException | IllegalArgumentException e) { return null; }
    }

    public long getExpirationTime(String... token) {
        return token == null || token.length == 0 ? System.currentTimeMillis() + expiration
                : getExpirationTimeSingle(token[0]);
    }

    public long getExpirationTimeSingle(String token) {
        Date expires = getExpirationDate(token);
        return expires == null ? 0 : expires.getTime();
    }

    public Date getExpirationDate(String token) {
        try { return parseToken(token).getExpiration(); }
        catch (JwtException | IllegalArgumentException e) { return null; }
    }

    public boolean isTokenExpired(String token) {
        Date expires = getExpirationDate(token);
        return expires == null || !expires.after(new Date());
    }

    private boolean isRevoked(Claims claims) {
        String generation = claims.get("generation", String.class);
        return generation == null
                || !generation.equals(tokenState.selectGeneration(Long.valueOf(claims.getSubject())))
                || tokenState.isRevoked(claims.getId())
                || !generation.equals(redis.opsForValue().get(SESSION_PREFIX + claims.getSubject()))
                || Boolean.TRUE.equals(redis.hasKey(REVOKED_PREFIX + claims.getId()));
    }

    public boolean isTokenBlacklisted(String token) {
        try { return isRevoked(parseToken(token)); }
        catch (DataAccessException e) { throw new AuthStateUnavailableException(e); }
        catch (RuntimeException e) { return true; }
    }

    @Transactional
    public void blacklistToken(String token) {
        Claims claims;
        try { claims = parseToken(token); }
        catch (JwtException | IllegalArgumentException e) { return; }
        if (claims.getId() == null || claims.getId().length() != 36 || claims.getExpiration() == null) return;
        try {
            Long userId = Long.valueOf(claims.getSubject());
            if (tokenState.lockUser(userId) == null) return;
            tokenState.revoke(claims.getId(), userId, claims.getExpiration());
            cacheRevocation(claims);
        } catch (DataAccessException e) {
            throw new AuthStateUnavailableException(e);
        }
    }

    private void cacheRevocation(Claims claims) {
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) redis.opsForValue().set(REVOKED_PREFIX + claims.getId(), "1", Duration.ofMillis(ttl));
    }

    @Transactional
    public void blacklistAllUserTokens(String username) {
        User user = authMapper.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("User does not exist");
        blacklistAllUserTokens(user.getId());
    }

    @Transactional
    public void blacklistAllUserTokens(Long userId) {
        try {
            if (tokenState.lockUser(userId) == null) throw new IllegalArgumentException("User does not exist");
            String generation = UUID.randomUUID().toString();
            tokenState.replaceGeneration(userId, generation);
            redis.opsForValue().set(SESSION_PREFIX + userId, generation);
        } catch (DataAccessException e) {
            throw new AuthStateUnavailableException(e);
        }
    }

    public boolean isTokenValid(String token, String username) {
        return validateToken(token) && username != null && username.equals(extractUsername(token));
    }

    private String credentialVersion(User user) {
        // A password change also invalidates a login racing with the Redis revocation write.
        // HMAC hides the stored password hash while changing whenever that hash changes.
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(user.getPassword().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is required", e);
        }
    }
}
