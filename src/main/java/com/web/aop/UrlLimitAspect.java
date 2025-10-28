package com.web.aop;

import com.web.annotation.UrlLimit;
import com.web.constant.LimitKeyType;
import com.web.exception.WeebException;
import com.web.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * URL访问频率限制切面
 * 增强版：支持用户限流、IP限流、动态配置、告警机制
 */
@Aspect
@Component
@Slf4j
public class UrlLimitAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;

    // Redis键前缀
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";
    private static final String RATE_LIMIT_STATS_KEY = "rate:limit:stats";
    private static final String RATE_LIMIT_ALERT_KEY = "rate:limit:alert:";
    private static final String RATE_LIMIT_CONFIG_KEY = "rate:limit:config:";

    // 告警阈值（达到限制次数的百分比）
    private static final double ALERT_THRESHOLD = 0.8;

    @Pointcut("@annotation(com.web.annotation.UrlLimit)")
    public void rateLimitPointcut() {
    }

    @Around("rateLimitPointcut() && @annotation(urlLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UrlLimit urlLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String path = request.getRequestURI();
        String ip = IpUtil.getIpAddr(request);
        String userId = null;

        // 获取用户ID
        if (urlLimit.keyType() == LimitKeyType.ID) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");

            if (userinfo != null && userinfo.get("userId") != null) {
                userId = userinfo.get("userId").toString();
            }
        }

        // 检查动态配置
        int maxRequests = getDynamicMaxRequests(path, urlLimit.maxRequests());
        long timeWindow = urlLimit.timeWindow();

        // 基于用户的限流
        if (userId != null) {
            checkRateLimit(userId, path, maxRequests, timeWindow, "USER");
        }

        // 基于IP的限流（总是执行）
        checkRateLimit(ip, path, maxRequests, timeWindow, "IP");

        // 记录统计信息
        recordStatistics(path, userId, ip);

        return joinPoint.proceed();
    }

    /**
     * 检查速率限制
     */
    private void checkRateLimit(String identifier, String path, int maxRequests, long timeWindow, String type) {
        long currentWindow = System.currentTimeMillis() / (timeWindow * 1000);
        String key = RATE_LIMIT_KEY_PREFIX + type + ":" + identifier + ":" + path + ":" + currentWindow;

        // 使用Redis自增
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, timeWindow + 10, TimeUnit.SECONDS);
        }

        if (current != null) {
            // 检查是否超过限制
            if (current > maxRequests) {
                // 记录限流事件
                recordRateLimitEvent(identifier, path, current, maxRequests, type);
                throw new WeebException("访问过快，请稍后再试~");
            }

            // 检查是否需要告警
            if (current >= maxRequests * ALERT_THRESHOLD) {
                triggerAlert(identifier, path, current, maxRequests, type);
            }
        }
    }

    /**
     * 获取动态最大请求数
     */
    private int getDynamicMaxRequests(String path, int defaultMax) {
        try {
            String configKey = RATE_LIMIT_CONFIG_KEY + path;
            Object config = objectRedisTemplate.opsForValue().get(configKey);

            if (config != null) {
                return Integer.parseInt(config.toString());
            }
        } catch (Exception e) {
            log.warn("获取动态限流配置失败: path={}", path, e);
        }

        return defaultMax;
    }

    /**
     * 记录限流事件
     */
    private void recordRateLimitEvent(String identifier, String path, long current, int maxRequests, String type) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("identifier", identifier);
            event.put("path", path);
            event.put("current", current);
            event.put("maxRequests", maxRequests);
            event.put("type", type);
            event.put("timestamp", LocalDateTime.now().toString());

            String eventKey = "rate:limit:event:" + System.currentTimeMillis();
            objectRedisTemplate.opsForValue().set(eventKey, event, 24, TimeUnit.HOURS);

            // 更新统计
            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "totalBlocked", 1);
            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "blocked:" + type, 1);

            log.warn("限流触发: type={}, identifier={}, path={}, current={}, max={}",
                    type, identifier, path, current, maxRequests);

        } catch (Exception e) {
            log.error("记录限流事件失败", e);
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(String identifier, String path, long current, int maxRequests, String type) {
        try {
            String alertKey = RATE_LIMIT_ALERT_KEY + type + ":" + identifier + ":" + path;

            // 检查是否已经告警过（避免重复告警）
            Boolean alerted = objectRedisTemplate.hasKey(alertKey);
            if (Boolean.TRUE.equals(alerted)) {
                return;
            }

            // 记录告警
            Map<String, Object> alert = new HashMap<>();
            alert.put("identifier", identifier);
            alert.put("path", path);
            alert.put("current", current);
            alert.put("maxRequests", maxRequests);
            alert.put("type", type);
            alert.put("threshold", ALERT_THRESHOLD);
            alert.put("timestamp", LocalDateTime.now().toString());

            objectRedisTemplate.opsForValue().set(alertKey, alert, 5, TimeUnit.MINUTES);

            // 更新统计
            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "totalAlerts", 1);

            log.warn("限流告警: type={}, identifier={}, path={}, current={}, max={}, threshold={}%",
                    type, identifier, path, current, maxRequests, (int)(ALERT_THRESHOLD * 100));

        } catch (Exception e) {
            log.error("触发限流告警失败", e);
        }
    }

    /**
     * 记录统计信息
     */
    private void recordStatistics(String path, String userId, String ip) {
        try {
            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "totalRequests", 1);
            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "requests:" + path, 1);

            if (userId != null) {
                objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "userRequests", 1);
            }

            objectRedisTemplate.opsForHash().increment(RATE_LIMIT_STATS_KEY, "ipRequests", 1);
            objectRedisTemplate.opsForHash().put(RATE_LIMIT_STATS_KEY, "lastUpdate", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.error("记录限流统计失败", e);
        }
    }
}
