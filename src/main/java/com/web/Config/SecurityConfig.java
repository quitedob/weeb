package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.web.filter.JwtAuthenticationFilter;
import com.web.util.JwtUtil;
import com.web.service.AuthService;

/**
 * 安全配置类
 * 提供密码编码器、安全策略等安全相关的Bean
 */
@Configuration
@EnableWebSecurity
@Component
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource, JwtUtil jwtUtil, AuthService authService, PasswordEncoder passwordEncoder) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
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
                .requestMatchers("/api/user/info", "/api/user/update", "/api/user/profile/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 群组管理接口
                .requestMatchers("/api/group/create", "/api/group/invite/**", "/api/group/kick/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/group/list/**", "/api/group/details/**")
                    .hasAnyRole("USER", "ADMIN")
                
                // 消息和聊天接口
                .requestMatchers("/api/v1/message/**", "/api/v1/chats/**")
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

    /**
     * 密码编码器Bean
     * 使用BCrypt算法进行密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 密码策略配置
     */
    public static class PasswordPolicy {
        public static final int MIN_LENGTH = 6;
        public static final int MAX_LENGTH = 100;
        public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$";
        public static final String REQUIREMENT = "密码必须包含至少一个字母和一个数字，长度在6-100个字符之间";
    }

    /**
     * 登录安全策略
     */
    public static class LoginSecurity {
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final long LOCK_TIME_MINUTES = 30;
        public static final String REQUIREMENT = "连续登录失败5次后，账号将被锁定30分钟";
    }

    /**
     * 用户名策略
     */
    public static class UsernamePolicy {
        public static final int MIN_LENGTH = 3;
        public static final int MAX_LENGTH = 50;
        public static final String PATTERN = "^[a-zA-Z0-9_]{3,50}$";
        public static final String REQUIREMENT = "用户名只能包含字母、数字和下划线，长度在3-50个字符之间";
    }

    /**
     * 邮箱策略
     */
    public static class EmailPolicy {
        public static final String PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        public static final String REQUIREMENT = "请输入有效的邮箱地址";
    }

    /**
     * 手机号策略
     */
    public static class PhonePolicy {
        public static final String PATTERN = "^1[3-9]\\d{9}$";
        public static final String REQUIREMENT = "请输入有效的11位手机号码";
    }

    /**
     * 验证密码是否符合策略
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.length() < PasswordPolicy.MIN_LENGTH || password.length() > PasswordPolicy.MAX_LENGTH) {
            return false;
        }
        return password.matches(PasswordPolicy.PATTERN);
    }

    /**
     * 验证用户名是否符合策略
     */
    public static boolean validateUsername(String username) {
        if (username == null || username.length() < UsernamePolicy.MIN_LENGTH || username.length() > UsernamePolicy.MAX_LENGTH) {
            return false;
        }
        return username.matches(UsernamePolicy.PATTERN);
    }

    /**
     * 验证邮箱是否符合策略
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches(EmailPolicy.PATTERN);
    }

    /**
     * 验证手机号是否符合策略
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.matches(PhonePolicy.PATTERN);
    }
}