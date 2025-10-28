package com.web.service.Impl;

import com.web.dto.UserDetailsDTO;
import com.web.mapper.AuthMapper;
import com.web.mapper.UserMapper;
import com.web.model.User;
import com.web.service.UserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 用户安全认证服务实现类
 * 遵循项目规范的Service层设计，为CustomUserDetailsService提供业务逻辑支持
 * 统一管理用户安全认证相关的业务逻辑
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserSecurityServiceImpl implements UserSecurityService {

    private final UserMapper userMapper;
    private final AuthMapper authMapper;

    public UserSecurityServiceImpl(UserMapper userMapper, AuthMapper authMapper) {
        this.userMapper = userMapper;
        this.authMapper = authMapper;
    }

    @Override
    public UserDetailsDTO loadUserDetailsByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user security details by username: {}", username);

            // 使用AuthMapper查询用户（遵循现有架构）
            User user = authMapper.findByUsername(username);
            if (user == null) {
                log.warn("User not found: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            // 检查用户状态
            if (!isUserActive(user.getId())) {
                log.warn("User account is disabled: {}", username);
                throw new UsernameNotFoundException("用户账户已被禁用: " + username);
            }

            // 获取用户权限
            List<String> authorities = getUserAuthorities(user.getId());

            // 构建UserDetailsDTO
            return UserDetailsDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .status(user.getStatus())
                    .authorities(authorities)
                    .accountNonExpired(true)
                    .accountNonLocked(!isUserLocked(user.getId()))
                    .credentialsNonExpired(true)
                    .build();

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user security details for username: {}", username, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + username, e);
        }
    }

    @Override
    public UserDetailsDTO loadUserDetailsById(Long userId) throws UsernameNotFoundException {
        try {
            log.debug("Loading user security details by ID: {}", userId);

            // 使用AuthMapper查询用户（遵循现有架构）
            User user = authMapper.findByUserID(userId);
            if (user == null) {
                log.warn("User not found by ID: {}", userId);
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }

            // 检查用户状态
            if (!isUserActive(user.getId())) {
                log.warn("User account is disabled for ID: {}", userId);
                throw new UsernameNotFoundException("用户账户已被禁用: " + userId);
            }

            // 获取用户权限
            List<String> authorities = getUserAuthorities(user.getId());

            // 构建UserDetailsDTO
            return UserDetailsDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .status(user.getStatus())
                    .authorities(authorities)
                    .accountNonExpired(true)
                    .accountNonLocked(!isUserLocked(user.getId()))
                    .credentialsNonExpired(true)
                    .build();

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user security details for userId: {}", userId, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + userId, e);
        }
    }

    @Override
    public List<String> getUserAuthorities(Long userId) {
        try {
            // 获取用户信息
            User user = authMapper.findByUserID(userId);
            if (user == null) {
                log.debug("User not found for authority check: {}", userId);
                return Collections.singletonList("ROLE_USER");
            }

            // 基于简化的用户类型系统分配权限
            List<String> authorities = new java.util.ArrayList<>();

            // 检查用户名模式确定用户类型
            String username = user.getUsername();
            if (username != null) {
                if (isAdminUsername(username)) {
                    authorities.add("ROLE_ADMIN");
                    authorities.add("ROLE_USER"); // 管理员也是用户
                } else if (isBotUsername(username)) {
                    authorities.add("ROLE_BOT");
                    authorities.add("ROLE_USER"); // 机器人也有基本用户权限
                } else {
                    authorities.add("ROLE_USER");
                }
            } else {
                // 默认权限
                authorities.add("ROLE_USER");
            }

            log.debug("User {} has authorities: {}", userId, authorities);
            return authorities;

        } catch (Exception e) {
            log.error("Error loading authorities for userId: {}", userId, e);
            return Collections.singletonList("ROLE_USER");
        }
    }

    /**
     * 基于用户名模式判断是否为管理员
     */
    private boolean isAdminUsername(String username) {
        return username.startsWith("admin_") ||
               username.startsWith("root_") ||
               username.startsWith("sys_") ||
               username.equals("admin") ||
               username.equals("root") ||
               username.equals("system") ||
               username.contains("_admin");
    }

    /**
     * 基于用户名模式判断是否为机器人
     */
    private boolean isBotUsername(String username) {
        return username.startsWith("bot_") ||
               username.startsWith("robot_") ||
               username.startsWith("auto_") ||
               username.startsWith("service_") ||
               username.equals("bot") ||
               username.equals("system_bot") ||
               username.contains("_bot") ||
               username.endsWith("_bot");
    }

    @Override
    public boolean isUserActive(Long userId) {
        try {
            User user = authMapper.findByUserID(userId);
            return user != null && user.getStatus() != null && user.getStatus() == 1;
        } catch (Exception e) {
            log.error("Error checking user active status for userId: {}", userId, e);
            return false;
        }
    }

    @Override
    public boolean isUserLocked(Long userId) {
        try {
            // 这里可以根据业务需求实现锁定逻辑
            // 例如检查登录失败次数、账号是否被管理员锁定等
            User user = authMapper.findByUserID(userId);
            if (user == null) {
                return false;
            }

            // 简单的状态检查：如果status不是1，可能被锁定或禁用
            // 可以根据实际业务需求细化这个逻辑
            return user.getStatus() != null && user.getStatus() < 0;

        } catch (Exception e) {
            log.error("Error checking user locked status for userId: {}", userId, e);
            return true; // 出错时默认认为是锁定的，更安全
        }
    }
}