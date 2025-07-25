package com.web.filter;

import com.web.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Jwt工具类，用于验证Token和解析Token
    private JwtUtil jwtUtil;

    // 构造方法注入JwtUtil实例
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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
                // 5. 从Token中解析出userId（作为用户标识）
                Long userId = jwtUtil.getUserIdFromToken(token);

                // 6. 创建认证对象，设置userId和对应的权限列表
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null,
                                Collections.singletonList(new SimpleGrantedAuthority("USER")));

                // 7. 将认证信息存入Spring Security上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 8. 新增代码：将用户信息设置到HttpServletRequest中，供后续使用
                Map<String, Object> userinfo = new HashMap<>(); // 创建一个Map保存用户信息
                userinfo.put("userId", userId); // 直接存储userId
                request.setAttribute("userinfo", userinfo); // 将用户信息放入请求属性中
            }
        }

        // 9. 将请求传递给过滤链中的下一个过滤器
        filterChain.doFilter(request, response);
    }
}
