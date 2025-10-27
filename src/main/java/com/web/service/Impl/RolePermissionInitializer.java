package com.web.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 角色权限初始化器
 * 在应用启动时自动为USER角色添加必要的权限
 */
@Component
@Slf4j
@Order(3) // 确保在SystemPermissionInitializer之后执行
@Deprecated // 已废弃，功能迁移至 SystemSecurityInitializer
public class RolePermissionInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RolePermissionInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.warn("⚠️  RolePermissionInitializer 已废弃，角色权限初始化功能已迁移至 SystemSecurityInitializer");
        // 不再执行角色权限初始化，避免与 SystemSecurityInitializer 冲突
        return;
    }

    /**
     * 确保USER角色存在
     */
    private void ensureUserRoleExists() {
        log.info("检查USER角色是否存在...");
        
        Integer userRoleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM role WHERE name = 'USER'", Integer.class);
        
        if (userRoleCount == null || userRoleCount == 0) {
            // 创建USER角色
            jdbcTemplate.update("""
                INSERT INTO role (name, description, status, type, level, is_default, created_at, updated_at)
                VALUES ('USER', '普通用户', 1, 0, 100, TRUE, NOW(), NOW())
                """);
            log.info("✅ USER角色创建成功");
        } else {
            log.info("USER角色已存在");
        }
    }

    /**
     * 为USER角色分配必要的权限
     */
    private void assignPermissionsToUserRole() {
        log.info("开始为USER角色分配权限...");
        
        // 获取USER角色ID
        Long userRoleId = jdbcTemplate.queryForObject(
            "SELECT id FROM role WHERE name = 'USER'", Long.class);
        
        if (userRoleId == null) {
            log.error("无法找到USER角色ID");
            return;
        }
        
        // 为USER角色添加联系人权限
        addContactPermissions(userRoleId);
        
        // 为USER角色添加文章权限
        addArticlePermissions(userRoleId);
        
        // 为USER角色添加消息权限
        addMessagePermissions(userRoleId);
        
        // 为USER角色添加群组权限
        addGroupPermissions(userRoleId);
        
        // 为USER角色添加用户基础权限
        addUserPermissions(userRoleId);
        
        // 为USER角色添加搜索权限
        addSearchPermissions(userRoleId);
        
        // 为USER角色添加关注权限
        addFollowPermissions(userRoleId);
        
        // 为USER角色添加文件权限
        addFilePermissions(userRoleId);
        
        // 为USER角色添加认证权限
        addAuthPermissions(userRoleId);
        
        log.info("✅ USER角色权限分配完成");
    }
    
    /**
     * 添加联系人权限
     */
    private void addContactPermissions(Long userRoleId) {
        String[] permissions = {
            "CONTACT_CREATE_OWN", "CONTACT_READ_OWN", "CONTACT_UPDATE_OWN", "CONTACT_DELETE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "联系人");
    }
    
    /**
     * 添加文章权限
     */
    private void addArticlePermissions(Long userRoleId) {
        String[] permissions = {
            "ARTICLE_CREATE_OWN", "ARTICLE_READ_OWN", "ARTICLE_UPDATE_OWN", "ARTICLE_DELETE_OWN",
            "ARTICLE_FAVORITE_OWN", "ARTICLE_COMMENT_OWN", "ARTICLE_LIKE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "文章");
    }
    
    /**
     * 添加消息权限
     */
    private void addMessagePermissions(Long userRoleId) {
        String[] permissions = {
            "MESSAGE_CREATE_OWN", "MESSAGE_READ_OWN", "MESSAGE_UPDATE_OWN", "MESSAGE_DELETE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "消息");
    }
    
    /**
     * 添加群组权限
     */
    private void addGroupPermissions(Long userRoleId) {
        String[] permissions = {
            "GROUP_CREATE_OWN", "GROUP_READ_OWN", "GROUP_UPDATE_OWN", "GROUP_JOIN_OWN", "GROUP_LEAVE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "群组");
    }
    
    /**
     * 添加用户基础权限
     */
    private void addUserPermissions(Long userRoleId) {
        String[] permissions = {
            "USER_READ_OWN", "USER_UPDATE_OWN", "USER_FOLLOW_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "用户");
    }
    
    /**
     * 添加搜索权限
     */
    private void addSearchPermissions(Long userRoleId) {
        String[] permissions = {
            "SEARCH_BASIC", "SEARCH_USER_BASIC", "SEARCH_CONTENT_BASIC"
        };
        addPermissionsToRole(userRoleId, permissions, "搜索");
    }
    
    /**
     * 添加关注权限
     */
    private void addFollowPermissions(Long userRoleId) {
        String[] permissions = {
            "FOLLOW_CREATE_OWN", "FOLLOW_READ_OWN", "FOLLOW_DELETE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "关注");
    }
    
    /**
     * 添加文件权限
     */
    private void addFilePermissions(Long userRoleId) {
        String[] permissions = {
            "FILE_UPLOAD_OWN", "FILE_READ_OWN", "FILE_DELETE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "文件");
    }
    
    /**
     * 添加认证权限
     */
    private void addAuthPermissions(Long userRoleId) {
        String[] permissions = {
            "AUTH_REGISTER_OWN", "AUTH_LOGIN_OWN", "AUTH_LOGOUT_OWN", "AUTH_PASSWORD_CHANGE_OWN"
        };
        addPermissionsToRole(userRoleId, permissions, "认证");
    }
    
    /**
     * 为角色添加权限
     * @param roleId 角色ID
     * @param permissions 权限名称数组
     * @param category 权限类别（用于日志）
     */
    private void addPermissionsToRole(Long roleId, String[] permissions, String category) {
        int addedCount = 0;
        
        for (String permissionName : permissions) {
            try {
                // 检查权限是否存在
                Integer permissionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM permission WHERE name = ?", Integer.class, permissionName);
                
                if (permissionCount != null && permissionCount > 0) {
                    // 检查角色是否已有该权限
                    Integer rolePermissionCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM role_permission WHERE role_id = ? AND permission_id = " +
                        "(SELECT id FROM permission WHERE name = ?)", 
                        Integer.class, roleId, permissionName);
                    
                    if (rolePermissionCount == null || rolePermissionCount == 0) {
                        // 添加权限到角色
                        jdbcTemplate.update("""
                            INSERT INTO role_permission (role_id, permission_id, created_at)
                            VALUES (?, (SELECT id FROM permission WHERE name = ?), NOW())
                            """, roleId, permissionName);
                        addedCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("添加{}权限失败: {}", category, permissionName, e);
            }
        }
        
        if (addedCount > 0) {
            log.info("✅ 为USER角色添加了{}个{}权限", addedCount, category);
        }
    }
}
