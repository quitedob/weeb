package com.web.filter;

import com.web.util.JwtUtil;
import com.web.service.AuthService;
import com.web.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Jwt工具类，用于验证Token和解析Token
    private JwtUtil jwtUtil;
    
    // AuthService，用于获取用户信息和角色
    private AuthService authService;

    // 构造方法注入JwtUtil和AuthService实例
    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 获取HTTP请求头中的Authorization字段
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. 判断Authorization字段是否存在且以"Bearer "开头
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 3. 去除"Bearer "前缀，获取实际的Token
            String token = authHeader.substring(7);

            // 4. 验证Token是否有效
            if (jwtUtil.validateToken(token)) {
                try {
                    // 5. 从Token中解析出userId（作为用户标识）
                    Long userId = jwtUtil.getUserIdFromToken(token);

                    // 6. 从数据库获取用户信息，包括角色
                    User user = authService.findByUserID(userId);
                    
                    if (user != null) {
                        // 7. 根据用户类型创建权限列表
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        String userType = user.getType();
                        
                        if (userType != null) {
                            // 添加基于用户类型的角色权限
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase()));
                            
                            // 为所有认证用户添加基础权限
                            if (!"ADMIN".equals(userType.toUpperCase())) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            }
                        } else {
                            // 默认权限
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        }

                        // 8. 创建认证对象，设置userId和对应的权限列表
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId, null, authorities);

                        // 9. 将认证信息存入Spring Security上下文中
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 10. 将用户信息设置到HttpServletRequest中，供后续使用
                        Map<String, Object> userinfo = new HashMap<>();
                        userinfo.put("userId", userId);
                        userinfo.put("username", user.getUsername());
                        userinfo.put("userType", userType);
                        request.setAttribute("userinfo", userinfo);
                    }
                } catch (Exception e) {
                    // 如果获取用户信息失败，清除认证上下文
                    SecurityContextHolder.clearContext();
                    logger.warn("Failed to load user details for token: " + e.getMessage());
                }
            }
        }

        // 11. 将请求传递给过滤链中的下一个过滤器
        filterChain.doFilter(request, response);
    }
}
