package com.web.service;

import com.web.dto.UserDetailsDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 用户安全认证服务接口
 * 遵循项目规范的Service层设计，为CustomUserDetailsService提供业务逻辑支持
 * 注意：这不是Spring Security的UserDetailsService，而是我们自己的业务服务层
 */
public interface UserSecurityService {

    /**
     * 根据用户名加载用户安全信息
     * @param username 用户名
     * @return 用户安全认证信息DTO
     * @throws UsernameNotFoundException 用户不存在异常
     */
    UserDetailsDTO loadUserDetailsByUsername(String username) throws UsernameNotFoundException;

    /**
     * 根据用户ID加载用户安全信息
     * @param userId 用户ID
     * @return 用户安全认证信息DTO
     * @throws UsernameNotFoundException 用户不存在异常
     */
    UserDetailsDTO loadUserDetailsById(Long userId) throws UsernameNotFoundException;

    /**
     * 获取用户权限列表
     * @param userId 用户ID
     * @return 权限名称列表
     */
    java.util.List<String> getUserAuthorities(Long userId);

    /**
     * 检查用户状态是否正常
     * @param userId 用户ID
     * @return 是否正常（启用状态）
     */
    boolean isUserActive(Long userId);

    /**
     * 检查用户是否被锁定
     * @param userId 用户ID
     * @return 是否被锁定
     */
    boolean isUserLocked(Long userId);
}