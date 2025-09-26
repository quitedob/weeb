package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关的基础 Bean 配置类
 * 提供密码编码器等安全相关的基础 Bean，避免循环依赖
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * 密码编码器Bean
     * 使用BCrypt算法进行密码加密
     * 提取到独立配置类以打破 SecurityConfig 和 AuthServiceImpl 的循环依赖
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 如果还有其他基础 bean（如 JwtUtil 需要的 secret）也可以放这里
}
