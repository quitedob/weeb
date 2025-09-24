package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.web.filter.JwtAuthenticationFilter;
import com.web.util.JwtUtil;
import com.web.service.AuthService;

/**
 * 安全配置类
 * 提供密码编码器等安全相关的Bean
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource, JwtUtil jwtUtil, AuthService authService) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    /**
     * 密码编码器Bean
     * 使用BCrypt算法进行密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链配置
     * 配置哪些接口需要认证，哪些可以公开访问
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 启用CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // 禁用CSRF（因为使用JWT，不需要CSRF保护）
            .csrf(csrf -> csrf.disable())
            // 配置会话管理为无状态（JWT不需要session）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口，不需要认证
                .requestMatchers(
                    "/login",           // 登录接口
                    "/register",        // 注册接口
                    "/captcha",         // 验证码接口
                    "/forget",          // 忘记密码接口
                    "/reset",           // 重置密码接口
                    "/findByUsername",  // 查找用户接口
                    "/findByUserID",    // 查找用户ID接口
                    "/error",           // 错误页面
                    "/favicon.ico",     // 网站图标
                    "/actuator/**"      // Spring Boot Actuator endpoints (if any)
                ).permitAll()
                
                // 管理员专用接口
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 文章相关接口的细粒度权限控制
                .requestMatchers("/api/articles/create", "/api/articles/edit/**", "/api/articles/delete/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/articles/read/**", "/api/articles/list/**")
                    .permitAll()
                
                // 用户管理接口
                .requestMatchers("/api/user/profile/**", "/api/user/update/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 群组管理接口
                .requestMatchers("/api/group/create", "/api/group/invite/**", "/api/group/kick/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/group/list/**", "/api/group/details/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 消息和聊天接口
                .requestMatchers("/api/messages/**", "/api/chat/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 搜索接口
                .requestMatchers("/api/search/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 通知接口
                .requestMatchers("/api/notifications/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 文件上传接口
                .requestMatchers("/api/upload/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT认证过滤器
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, authService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}