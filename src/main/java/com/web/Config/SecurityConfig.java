package com.web.Config;

import com.web.filter.JwtAuthenticationFilter;
import com.web.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 保护（根据实际需要启用或禁用）
                .csrf(csrf -> csrf.disable())
                // 启用 CORS 支持
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        // 允许指定的 POST 请求无须认证
                        .requestMatchers(HttpMethod.POST,
                                "/register",
                                "/login",
                                "/articles/getall",
                                "/articles/{id}",
                                "/articles/{id}/read").permitAll()
                        // 允许指定的 GET 请求无须认证
                        .requestMatchers(HttpMethod.GET,
                                "/articles/{id}",
                                "/findByUserID").permitAll()
                        // Add new authenticated paths explicitly
                        .requestMatchers(
                                "/user/info",       // UserController is at /user
                                "/user/update",     // UserController is at /user
                                "/api/group/my-list" // GroupController is at /api/group
                        ).authenticated()
                        // 其余请求需要认证
                        .anyRequest().authenticated()
                );

        // 在 UsernamePasswordAuthenticationFilter 之前添加自定义的 JWT 认证过滤器
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的前端来源
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 是否允许发送 Cookie 凭证
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用此 CORS 配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
