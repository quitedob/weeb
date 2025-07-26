package com.web.aop;

import com.web.exception.WeebException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 消息发送频率限制切面
 * 简化注释：消息频率切面
 */
@Aspect
@Component
public class MessageRateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(MessageRateLimitAspect.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Using weeb: prefix as per user's example for consistency
    private static final int MAX_MESSAGES = 60; // 每分钟最大消息数
    private static final long TIME_WINDOW_MINUTES = 1; // 时间窗口，单位：分钟
    private static final long MUTE_DURATION_MINUTES = 5; // 禁言时长，单位：分钟
    private static final String MUTE_KEY_PREFIX = "weeb:mute:user:";
    private static final String RATE_LIMIT_KEY_PREFIX = "weeb:rate_limit:msg:user:";

    /**
     * 在消息发送服务执行前进行检查.
     * Targets com.web.service.MessageService.send(Long userId, Message messageBody)
     * @param joinPoint 连接点，用于获取方法参数
     */
    @Before("execution(* com.web.service.MessageService.send(java.lang.Long, ..)) && args(userId, ..)")
    public void checkRateLimit(JoinPoint joinPoint, Long userId) { // Correctly bind Long userId
        if (userId == null) {
            log.warn("User ID is null in MessageRateLimitAspect, skipping rate limit check.");
            return; // Cannot rate limit without a user ID
        }

        try {
            String muteKey = MUTE_KEY_PREFIX + userId;
            // 1. 检查用户是否正被禁言
            if (Boolean.TRUE.equals(redisTemplate.hasKey(muteKey))) {
                Long expireTimeSeconds = redisTemplate.getExpire(muteKey, TimeUnit.SECONDS);
                String timeLeftMessage = (expireTimeSeconds != null && expireTimeSeconds > 0) ?
                                         expireTimeSeconds + " 秒" : "一段时间";
                throw new WeebException("您已被禁言，剩余 " + timeLeftMessage);
            }

            // 2. 执行频率检查
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;
            // 使用 increment 方法，原子性地增加计数
            Long currentMessages = redisTemplate.opsForValue().increment(rateLimitKey);

            if (currentMessages != null) {
                if (currentMessages == 1) {
                    // 如果是窗口内的第一条消息，为这个计数器设置过期时间
                    redisTemplate.expire(rateLimitKey, TIME_WINDOW_MINUTES, TimeUnit.MINUTES);
                    log.debug("Rate limit window started for user {}. Count: 1", userId);
                }

                log.debug("User {} message count in current window: {}", userId, currentMessages);

                if (currentMessages > MAX_MESSAGES) {
                    // 如果消息数超过限制，将用户加入禁言名单
                    redisTemplate.opsForValue().set(muteKey, "1", MUTE_DURATION_MINUTES, TimeUnit.MINUTES);
                    // 删除频率计数器，以便禁言结束后可以重新开始计数
                    redisTemplate.delete(rateLimitKey);
                    log.warn("User {} exceeded message rate limit. Muted for {} minutes.", userId, MUTE_DURATION_MINUTES);
                    throw new WeebException("消息发送过于频繁，您已被禁言 " + MUTE_DURATION_MINUTES + " 分钟");
                }
            } else {
                // Should not happen with increment, but good to log if it does
                log.error("Redis increment returned null for key: {}. Rate limiting may not be effective.", rateLimitKey);
            }
        } catch (Exception e) {
            // Redis操作异常时，记录错误但不阻止消息发送
            log.error("Redis操作异常，用户ID: {}, 错误信息: {}", userId, e.getMessage());
            // 可以选择继续执行或抛出异常，这里选择继续执行
        }
    }
}
