package com.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.util.JwtUtil;
import com.web.util.SecurityAuditUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器
 * 处理JWT令牌的验证和用户认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        // 检查Authorization头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取JWT令牌
            jwt = authHeader.substring(7);

            // 首先验证token是否有效（不依赖用户）
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid JWT token format or signature");
                SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Invalid token format");
                filterChain.doFilter(request, response);
                return;
            }

            final Long userId = jwtUtil.getUserIdFromToken(jwt);

            // 验证用户ID和SecurityContext
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.customUserDetailsService.loadUserById(userId);

                    // 验证JWT令牌与用户的匹配性
                    String tokenUsername = jwtUtil.extractUsername(jwt);
                    if (tokenUsername != null && tokenUsername.equals(userDetails.getUsername())) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        // 设置userinfo属性供UserInfoArgumentResolver使用
                        Map<String, Object> userinfo = new HashMap<>();
                        userinfo.put("userId", userId);
                        userinfo.put("username", userDetails.getUsername());
                        request.setAttribute("userinfo", userinfo);

                        // 记录成功的认证
                        SecurityAuditUtils.logAuthenticationSuccess(userDetails.getUsername(), request.getRemoteAddr());
                    } else {
                        log.warn("Token username mismatch: token={}, user={}", tokenUsername, userDetails.getUsername());
                        SecurityAuditUtils.logAuthenticationFailure(userDetails.getUsername(), request.getRemoteAddr(), "Username mismatch");
                    }
                } catch (UsernameNotFoundException e) {
                    log.warn("User not found for token userId: {}", userId);
                    SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "User not found");
                    // 不抛出异常，继续处理请求，让后续的认证过滤器处理
                }
            }

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            handleAuthenticationException(response, "令牌已过期，请重新登录", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Malformed token");
            handleAuthenticationException(response, "无效的令牌", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Invalid signature");
            handleAuthenticationException(response, "令牌签名无效", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token", e);
            SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Processing error");
            handleAuthenticationException(response, "令牌处理失败", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 处理认证异常
     */
    private void handleAuthenticationException(HttpServletResponse response, String message, int status)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}