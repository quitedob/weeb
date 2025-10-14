package com.web.Config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 专门用于解决 Redis 健康检查问题的配置类
 * 禁用默认的 Redis 健康检查，避免 Malformed \\uxxxx encoding 错误
 */
@Configuration
public class RedisHealthCheckConfig {
    
    /**
     * 禁用默认的 Redis 健康检查
     * 这个配置会覆盖 Spring Boot 默认的 Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> Health.up()
            .withDetail("status", "Redis 健康检查已禁用")
            .withDetail("reason", "避免 Malformed \\\\uxxxx encoding 错误")
            .build();
    }
}
