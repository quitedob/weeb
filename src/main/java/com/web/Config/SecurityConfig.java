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
                        // 允许预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 公开的认证端点：注册、登录、验证码、忘记/重置密码
                        .requestMatchers("/register", "/login", "/captcha", "/forget", "/reset").permitAll()
                        // 公开的文章阅读端点：仅允许获取文章与列表（符合最小权限原则）
                        .requestMatchers(HttpMethod.GET,
                                "/articles/getall",
                                "/articles/*" // 只读文章详情
                        ).permitAll()
                        // API 路径需要认证
                        .requestMatchers("/api/**").authenticated()
                        // 其余所有请求默认需要认证
                        .anyRequest().authenticated()
                );

        // 在 UsernamePasswordAuthenticationFilter 之前添加自定义的 JWT 认证过滤器
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
