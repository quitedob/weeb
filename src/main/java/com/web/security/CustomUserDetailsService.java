package com.web.security;

import com.web.model.User;
import com.web.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 * 为Spring Security提供用户认证信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);

            User user = userService.findByUsername(username);
            if (user == null) {
                log.warn("User not found: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            // 检查用户是否被禁用
            if (user.getStatus() != null && user.getStatus() == 0) {
                log.warn("User account is disabled: {}", username);
                throw new UsernameNotFoundException("用户账户已被禁用: " + username);
            }

            // 获取用户权限
            List<SimpleGrantedAuthority> authorities = getUserAuthorities(user);

            // 创建Spring Security User对象
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(user.getStatus() == 0)
                    .build();

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + username, e);
        }
    }

    /**
     * 获取用户权限列表
     */
    private List<SimpleGrantedAuthority> getUserAuthorities(User user) {
        try {
            List<com.web.model.Permission> permissions = userService.getUserPermissions(user.getId());

            List<SimpleGrantedAuthority> authorities = permissions.stream()
                    .filter(permission -> permission.getStatus() == 1) // 只启用状态的权限
                    .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                    .collect(Collectors.toList());

            // 添加默认用户角色
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            log.debug("User {} has {} authorities", user.getUsername(), authorities.size());
            return authorities;

        } catch (Exception e) {
            log.error("Error loading user authorities for user: {}", user.getUsername(), e);
            // 返回默认角色
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        try {
            User user = userService.getUserBasicInfo(userId);
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }
            return loadUserByUsername(user.getUsername());
        } catch (Exception e) {
            throw new UsernameNotFoundException("加载用户信息失败: " + userId, e);
        }
    }
}