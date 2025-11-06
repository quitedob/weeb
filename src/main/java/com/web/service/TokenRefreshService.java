package com.web.service;

import com.web.exception.WeebException;
import com.web.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token刷新服务
 * 实现Access Token和Refresh Token机制
 */
@Slf4j
@Service
public class TokenRefreshService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final long ACCESS_TOKEN_EXPIRE_MINUTES = 15; // 15分钟
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 7; // 7天

    /**
     * 生成Token对
     */
    public Map<String, String> generateTokenPair(Long userId, String username) {
        Map<String, String> tokens = new HashMap<>();
        
        // 生成Access Token（15分钟）
        String accessToken = jwtUtil.generateToken(userId, username);
        tokens.put("accessToken", accessToken);
        
        // 生成Refresh Token（7天）
        String refreshToken = UUID.randomUUID().toString();
        tokens.put("refreshToken", refreshToken);
        
        // 存储Refresh Token到Redis
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("userId", userId);
        tokenData.put("username", username);
        tokenData.put("used", false);
        
        redisTemplate.opsForHash().putAll(key, tokenData);
        redisTemplate.expire(key, REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
        
        log.info("✅ 生成Token对: userId={}, username={}", userId, username);
        return tokens;
    }

    /**
     * 刷新Access Token
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        
        // 检查Refresh Token是否存在
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new WeebException("Refresh Token无效或已过期");
        }
        
        // 获取Token数据
        Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(key);
        Boolean used = (Boolean) tokenData.get("used");
        
        // 检查是否已使用
        if (Boolean.TRUE.equals(used)) {
            throw new WeebException("Refresh Token已使用，请重新登录");
        }
        
        // 标记为已使用
        redisTemplate.opsForHash().put(key, "used", true);
        
        // 生成新的Token对
        Long userId = tokenData.get("userId") instanceof Long 
            ? (Long) tokenData.get("userId") 
            : Long.valueOf(tokenData.get("userId").toString());
        String username = (String) tokenData.get("username");
        
        Map<String, String> newTokens = generateTokenPair(userId, username);
        
        log.info("✅ 刷新Token成功: userId={}, username={}", userId, username);
        return newTokens;
    }

    /**
     * 撤销Token（登出）
     */
    public void revokeToken(String accessToken, String refreshToken) {
        try {
            // 将Access Token加入黑名单
            if (accessToken != null && !accessToken.isEmpty()) {
                String blacklistKey = TOKEN_BLACKLIST_PREFIX + accessToken;
                long remainingTime = jwtUtil.getExpirationTime(accessToken) - System.currentTimeMillis();
                
                if (remainingTime > 0) {
                    redisTemplate.opsForValue().set(blacklistKey, true, remainingTime, TimeUnit.MILLISECONDS);
                }
            }
            
            // 删除Refresh Token
            if (refreshToken != null && !refreshToken.isEmpty()) {
                String refreshKey = REFRESH_TOKEN_PREFIX + refreshToken;
                redisTemplate.delete(refreshKey);
            }
            
            log.info("✅ Token已撤销");
        } catch (Exception e) {
            log.error("❌ 撤销Token失败", e);
        }
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String accessToken) {
        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + accessToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("检查Token黑名单失败", e);
            return false;
        }
    }

    /**
     * 验证Refresh Token
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            String key = REFRESH_TOKEN_PREFIX + refreshToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("验证Refresh Token失败", e);
            return false;
        }
    }
}
