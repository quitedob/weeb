package com.web.aop;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.web.annotation.UrlLimit;
import com.web.constant.LimitKeyType;
import com.web.dto.UrlLimitStats;
import com.web.exception.WeebException;
import com.web.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * URL访问频率限制切面
 * 简化注释：URL限流切面
 */
@Aspect // 声明该类为AOP切面
@Component // 声明为Spring组件，支持依赖注入
@Slf4j // 自动为该类生成日志对象log
public class UrlLimitAspect {

    // 定义Caffeine缓存，用于保存请求计数（key为唯一标识，value为原子计数器）
    private final Cache<String, AtomicInteger> requestCountCache;

    // 定义Caffeine缓存，用于保存访问统计信息（key为唯一标识，value为统计信息对象）
    private final Cache<String, UrlLimitStats> statsCache;

    // 构造方法，初始化两个Caffeine缓存
    public UrlLimitAspect() {
        // 初始化请求计数缓存，设置过期时间为1分钟
        this.requestCountCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES) // 设置缓存过期时间
                .build();

        // 初始化统计信息缓存，设置过期时间为1小时
        this.statsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存过期时间
                .build();
    }

    // 定义切入点，匹配所有被@UrlLimit注解标记的方法
    @Pointcut("@annotation(com.web.annotation.UrlLimit)")
    public void rateLimitPointcut() {
        // 切入点方法体可以为空
    }

    // 环绕通知，拦截被@UrlLimit注解标记的方法，并执行限流逻辑
    @Around("rateLimitPointcut() && @annotation(urlLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UrlLimit urlLimit) throws Throwable {
        // 通过RequestContextHolder获取当前HTTP请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        // 定义用于限流的唯一key
        String key = "";

        // 根据注解中配置的keyType决定使用用户ID还是IP地址作为限流标识
        if (urlLimit.keyType() == LimitKeyType.ID) {
            // 当keyType为ID时，从请求属性中获取用户信息（存储在"userinfo"中）
            @SuppressWarnings("unchecked")
            Map<String, Object> userinfo = (Map<String, Object>) request.getAttribute("userinfo");

            // 增加空值检查，防止userinfo为null
            if (userinfo == null || userinfo.get("userId") == null) {
                // 记录警告日志，但不抛出异常，允许请求继续执行
                log.warn("用户信息为空，IP: {}, 允许请求继续执行", IpUtil.getIpAddr(request));
                // 使用IP地址作为备选方案
                key = IpUtil.getIpAddr(request);
            } else {
            // 使用用户ID作为限流key
            key = userinfo.get("userId").toString();
            }
        } else {
            // 当keyType不是ID时，使用请求者的IP地址作为限流标识
            key = IpUtil.getIpAddr(request);
        }

        // 获取当前请求的URI路径
        String path = request.getRequestURI();
        // 拼接用户标识和请求路径，形成唯一的缓存key
        key = key + ":" + path;

        // 从统计信息缓存中获取对应的统计信息，如果不存在则初始化一个新的统计对象
        UrlLimitStats stats = statsCache.get(key, k -> new UrlLimitStats());
        // 检查当前请求是否已被封禁
        if (stats.isBlocked()) {
            // 如果已封禁，则抛出异常提示访问过于频繁
            throw new WeebException("访问过于频繁，您已被封禁~");
        }

        // 从请求计数缓存中获取对应的计数器，如果不存在则初始化为0
        AtomicInteger count = requestCountCache.get(key, k -> new AtomicInteger(0));

        // 将当前请求计数加1，并获取加1后的值
        int currentCount = count.incrementAndGet();

        // 判断当前请求次数是否超过最大允许的请求数
        if (currentCount > urlLimit.maxRequests()) {
            // 超过最大请求数，则记录违规次数
            stats.setViolationCount(stats.getViolationCount() + 1);
            // 更新最后违规时间为当前时间
            stats.setLastViolationTime(LocalDateTime.now());

            // 如果违规次数达到一定阈值，则标记该请求为封禁状态
            if (stats.getViolationCount() >= urlLimit.maxRequests() + 100) {
                stats.setBlocked(true); // 标记为封禁
                throw new WeebException("访问过于频繁，您已被封禁~");
            }

            // 更新统计信息缓存
            statsCache.put(key, stats);
            // 抛出异常提示用户访问过快
            throw new WeebException("访问过快，请稍后再试~");
        }

        // 如果没有超过请求限制，继续执行被拦截的方法
        return joinPoint.proceed();
    }
}
