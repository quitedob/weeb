package com.web.security;

import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.Permission;
import com.web.model.User;
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
 * 修复：统一使用AuthMapper以避免数据访问不一致问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthMapper authMapper;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);

            User user = authMapper.findByUsername(username);
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
            // 直接使用UserMapper获取权限，保持数据访问一致性
            List<Permission> permissions = getUserPermissions(user.getId());

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
     * 获取用户权限（直接使用UserMapper）
     */
    private List<Permission> getUserPermissions(Long userId) {
        try {
            // 使用UserMapper的selectUserPermissions方法
            return userMapper.selectUserPermissions(userId);
        } catch (Exception e) {
            log.error("Error loading permissions for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据用户ID加载用户详情
     * 【核心修复三】直接根据用户ID构建UserDetails，避免二次查询
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by ID: {}", userId);

            // 直接使用AuthMapper根据ID查询用户
            User user = authMapper.findByUserID(userId);
            if (user == null) {
                log.warn("User not found by ID: {}", userId);
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }

            // 检查用户是否被禁用
            if (user.getStatus() != null && user.getStatus() == 0) {
                log.warn("User account is disabled for ID: {}", userId);
                throw new UsernameNotFoundException("用户账户已被禁用: " + userId);
            }

            // 获取用户权限
            List<SimpleGrantedAuthority> authorities = getUserAuthorities(user);

            // 直接创建Spring Security User对象，不再调用loadUserByUsername
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
            log.error("Error loading user details for userId: {}", userId, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + userId, e);
        }
    }
}