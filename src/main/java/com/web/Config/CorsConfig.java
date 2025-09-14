package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS配置类
 * 解决前端跨域访问问题
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 按环境控制来源：开发放开，生产收敛到变量
        String active = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
        if ("prod".equalsIgnoreCase(active) || "production".equalsIgnoreCase(active)) {
            // 生产环境：必须设置具体的ALLOWED_ORIGINS，不能使用通配符*
            String allowedOrigins = System.getenv().getOrDefault("ALLOWED_ORIGINS", "");
            if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
                throw new IllegalStateException("生产环境缺少必要的 CORS 环境变量: ALLOWED_ORIGINS。示例: ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com");
            }
            // 分割并清理每个origin
            String[] origins = allowedOrigins.split(",");
            for (int i = 0; i < origins.length; i++) {
                origins[i] = origins[i].trim();
            }
            configuration.setAllowedOrigins(Arrays.asList(origins));
        } else {
            // 开发环境：使用通配符，但禁用credentials（避免浏览器警告）
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            configuration.setAllowCredentials(false); // 开发环境禁用credentials以避免与*冲突
        }

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 预检请求的有效期
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
} 