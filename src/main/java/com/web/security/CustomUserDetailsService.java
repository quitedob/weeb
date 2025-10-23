package com.web.security;

import com.web.dto.UserDetailsDTO;
import com.web.service.UserSecurityService;
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
 * 【架构修复】遵循项目规范，使用Service层而非直接访问Mapper层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserSecurityService userSecurityService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);

            // 使用Service层获取用户安全信息
            UserDetailsDTO userDTO = userSecurityService.loadUserDetailsByUsername(username);

            // 转换权限列表
            List<SimpleGrantedAuthority> authorities = userDTO.getAuthorityArray() != null ?
                    java.util.Arrays.stream(userDTO.getAuthorityArray())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList()) :
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

            // 创建Spring Security User对象
            return org.springframework.security.core.userdetails.User.builder()
                    .username(userDTO.getUsername())
                    .password(userDTO.getPassword())
                    .authorities(authorities)
                    .accountExpired(!userDTO.isAccountNonExpired())
                    .accountLocked(!userDTO.isAccountNonLocked())
                    .credentialsExpired(!userDTO.isCredentialsNonExpired())
                    .disabled(!userDTO.isEnabled())
                    .build();

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + username, e);
        }
    }

    /**
     * 根据用户ID加载用户详情
     * 【架构修复】使用Service层而非直接访问Mapper层
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by ID: {}", userId);

            // 使用Service层获取用户安全信息
            UserDetailsDTO userDTO = userSecurityService.loadUserDetailsById(userId);

            // 转换权限列表
            List<SimpleGrantedAuthority> authorities = userDTO.getAuthorityArray() != null ?
                    java.util.Arrays.stream(userDTO.getAuthorityArray())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList()) :
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

            // 创建Spring Security User对象
            return org.springframework.security.core.userdetails.User.builder()
                    .username(userDTO.getUsername())
                    .password(userDTO.getPassword())
                    .authorities(authorities)
                    .accountExpired(!userDTO.isAccountNonExpired())
                    .accountLocked(!userDTO.isAccountNonLocked())
                    .credentialsExpired(!userDTO.isCredentialsNonExpired())
                    .disabled(!userDTO.isEnabled())
                    .build();

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user details for userId: {}", userId, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + userId, e);
        }
    }
}