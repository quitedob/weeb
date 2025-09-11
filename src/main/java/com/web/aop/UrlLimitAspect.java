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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * URL访问频率限制切面
 * 简化注释：URL限流切面
 */
@Aspect // 声明该类为AOP切面
@Component // 声明为Spring组件，支持依赖注入
@Slf4j // 自动为该类生成日志对象log
public class UrlLimitAspect {

    @Autowired
    private StringRedisTemplate redisTemplate; // 使用Redis实现分布式限流

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
        // 拼接用户标识和请求路径，形成唯一的Redis key（分钟粒度）
        String minuteKey = key + ":" + path + ":" + (System.currentTimeMillis() / 60000);

        // 使用Redis自增实现滑动窗口近似（分钟窗口）
        Long current = redisTemplate.opsForValue().increment(minuteKey);
        if (current != null && current == 1L) {
            // 设置过期时间为70秒，略大于1分钟
            redisTemplate.expire(minuteKey, 70, TimeUnit.SECONDS);
        }

        if (current != null && current > urlLimit.maxRequests()) {
            throw new WeebException("访问过快，请稍后再试~");
        }

        // 如果没有超过请求限制，继续执行被拦截的方法
        return joinPoint.proceed();
    }
}
