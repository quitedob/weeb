package com.web.service.Impl;

import com.web.mapper.AuthMapper;
import com.web.model.User;
import com.web.service.UserTypeSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户类型安全服务实现类
 * 基于简化的用户类型系统（ADMIN/USER/BOT）进行权限检查
 * 替代复杂的RBAC权限系统
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserTypeSecurityServiceImpl implements UserTypeSecurityService {

    private final AuthMapper authMapper;

    public UserTypeSecurityServiceImpl(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public boolean isAdmin(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = authMapper.findByUsername(username.trim());
            if (user == null) {
                log.debug("User not found for admin check: {}", username);
                return false;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                log.debug("User account is disabled for admin check: {}", username);
                return false;
            }

            // 基于用户名模式识别管理员（简化方案）
            // 实际项目中应该有专门的userType字段
            return isUsernameAdmin(username.trim());

        } catch (Exception e) {
            log.error("Error checking admin status for username: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean isBot(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = authMapper.findByUsername(username.trim());
            if (user == null) {
                return false;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }

            // 基于用户名模式识别机器人
            return isUsernameBot(username.trim());

        } catch (Exception e) {
            log.error("Error checking bot status for username: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean isRegularUser(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = authMapper.findByUsername(username.trim());
            if (user == null) {
                return false;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }

            // 不是管理员也不是机器人的就是普通用户
            return !isAdmin(username.trim()) && !isBot(username.trim());

        } catch (Exception e) {
            log.error("Error checking regular user status for username: {}", username, e);
            return false;
        }
    }

    @Override
    public String getUserType(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return "UNKNOWN";
            }

            if (isAdmin(username.trim())) {
                return "ADMIN";
            } else if (isBot(username.trim())) {
                return "BOT";
            } else if (isRegularUser(username.trim())) {
                return "USER";
            } else {
                return "UNKNOWN";
            }

        } catch (Exception e) {
            log.error("Error getting user type for username: {}", username, e);
            return "UNKNOWN";
        }
    }

    @Override
    public boolean canAccessAdminFeatures(String username) {
        return isAdmin(username);
    }

    @Override
    public boolean canWrite(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = authMapper.findByUsername(username.trim());
            if (user == null) {
                return false;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }

            // 管理员和普通用户可以写，机器人通常不能写
            return isAdmin(username.trim()) || isRegularUser(username.trim());

        } catch (Exception e) {
            log.error("Error checking write permission for username: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean canRead(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = authMapper.findByUsername(username.trim());
            if (user == null) {
                return false;
            }

            // 检查用户状态
            if (user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }

            // 所有类型的用户都可以读
            return true;

        } catch (Exception e) {
            log.error("Error checking read permission for username: {}", username, e);
            return false;
        }
    }

    /**
     * 基于用户名模式判断是否为管理员
     * 这是一个临时的简化方案，实际应该有专门的userType字段
     */
    private boolean isUsernameAdmin(String username) {
        // 管理员用户名规则：包含admin、以特殊前缀开头等
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
     * 这是一个临时的简化方案，实际应该有专门的userType字段
     */
    private boolean isUsernameBot(String username) {
        // 机器人用户名规则：包含bot、以特殊前缀开头等
        return username.startsWith("bot_") ||
               username.startsWith("robot_") ||
               username.startsWith("auto_") ||
               username.startsWith("service_") ||
               username.equals("bot") ||
               username.equals("system_bot") ||
               username.contains("_bot") ||
               username.endsWith("_bot");
    }
}