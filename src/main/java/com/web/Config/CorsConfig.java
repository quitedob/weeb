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
        if ("prod".equalsIgnoreCase(active)) {
            String allowedOrigins = System.getenv().getOrDefault("ALLOWED_ORIGINS", "");
            if (!allowedOrigins.isEmpty()) {
                configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
            } else {
                configuration.setAllowedOriginPatterns(Arrays.asList("https://your-frontend-domain.example"));
            }
        } else {
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        }

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许发送Cookie
        configuration.setAllowCredentials(true);

        // 预检请求的有效期
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
} 