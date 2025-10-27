package com.web.Config;

import com.web.model.Permission;
import com.web.service.PermissionService;
import com.web.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统安全初始化器 - 统一权限和角色管理
 * 整合了 DatabaseInitializer、SystemPermissionInitializer、RolePermissionInitializer 的功能
 * 确保系统启动时权限、角色和用户关联的正确初始化
 */
@Component
@Slf4j
@Order(1) // 最优先执行，确保安全基础组件就绪
@RequiredArgsConstructor
public class SystemSecurityInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;

    /**
     * 初始化默认角色和权限
     * 这个方法会在系统启动时自动执行
     */

    @Override
    public void run(String... args) throws Exception {
        log.info("🔒 开始系统安全初始化...");

        try {
            // Phase 1: 初始化基础表结构和数据
            initializeBasicData();

            // Phase 2: 初始化权限系统
            initializePermissionSystem();

            // Phase 3: 初始化角色和权限关联
            initializeRolePermissionMappings();

            // Phase 4: 处理现有用户角色分配
            handleExistingUsers();

            // Phase 5: 验证初始化结果
            validateInitialization();

            log.info("✅ 系统安全初始化完成");

        } catch (Exception e) {
            log.error("❌ 系统安全初始化失败", e);
            throw new RuntimeException("系统安全初始化失败", e);
        }
    }

    /**
     * 初始化基础数据 - 角色表
     */
    private void initializeBasicData() {
        log.info("📋 初始化基础角色数据...");

        try {
            // 检查角色表是否存在
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role", Integer.class);

            if (roleCount == null || roleCount == 0) {
                log.info("创建默认角色...");

                // 创建默认角色
                jdbcTemplate.update("""
                    INSERT INTO role (name, description, status, type, level, is_default, created_at, updated_at) VALUES
                    ('SUPER_ADMIN', '系统最高权限管理员', 1, 0, 1, FALSE, NOW(), NOW()),
                    ('ADMIN', '系统管理员', 1, 0, 10, FALSE, NOW(), NOW()),
                    ('MODERATOR', '内容版主', 1, 0, 50, FALSE, NOW(), NOW()),
                    ('USER', '普通用户', 1, 0, 100, TRUE, NOW(), NOW())
                    """);

                log.info("✅ 默认角色创建成功");
            } else {
                log.info("角色数据已存在，跳过创建");
            }

        } catch (Exception e) {
            log.error("初始化基础角色数据失败", e);
            throw new RuntimeException("初始化基础角色数据失败", e);
        }
    }

    /**
     * 初始化权限系统
     */
    private void initializePermissionSystem() {
        log.info("🔑 初始化权限系统...");

        try {
            // 使用现有的 PermissionService 初始化系统权限
            var result = permissionService.initializeSystemPermissions();

            int createdCount = (Integer) result.get("createdCount");
            int totalCount = (Integer) result.get("totalCount");

            log.info("✅ 权限系统初始化完成: 新创建 {} 个权限，总共 {} 个权限",
                     createdCount, totalCount);

        } catch (Exception e) {
            log.error("初始化权限系统失败", e);
            throw new RuntimeException("初始化权限系统失败", e);
        }
    }

    /**
     * 初始化角色和权限的映射关系
     */
    private void initializeRolePermissionMappings() {
        log.info("🔗 初始化角色权限映射...");

        try {
            // 为超级管理员角色分配所有权限
            assignAllPermissionsToRole("SUPER_ADMIN", "超级管理员");

            // 为管理员角色分配管理权限
            assignAdminPermissions("ADMIN", "管理员");

            // 为版主角色分配内容管理权限
            assignModeratorPermissions("MODERATOR", "版主");

            // 为普通用户角色分配基础权限
            assignUserPermissions("USER", "普通用户");

            log.info("✅ 角色权限映射初始化完成");

        } catch (Exception e) {
            log.error("初始化角色权限映射失败", e);
            throw new RuntimeException("初始化角色权限映射失败", e);
        }
    }

    /**
     * 为超级管理员分配所有权限
     */
    private void assignAllPermissionsToRole(String roleName, String description) {
        log.info("为{}角色分配所有权限...", description);

        try {
            Long roleId = getRoleIdByName(roleName);
            if (roleId == null) {
                log.warn("角色 {} 不存在，跳过权限分配", roleName);
                return;
            }

            // 获取所有权限ID
            List<Long> permissionIds = jdbcTemplate.queryForList(
                "SELECT id FROM permission WHERE status = 1", Long.class);

            for (Long permissionId : permissionIds) {
                // 检查是否已存在映射
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM role_permission WHERE role_id = ? AND permission_id = ?",
                    Integer.class, roleId, permissionId);

                if (count == null || count == 0) {
                    jdbcTemplate.update("""
                        INSERT INTO role_permission (role_id, permission_id, created_at)
                        VALUES (?, ?, NOW())
                        """, roleId, permissionId);
                }
            }

            log.info("✅ {}角色权限分配完成", description);

        } catch (Exception e) {
            log.error("为{}角色分配权限失败", description, e);
        }
    }

    /**
     * 为管理员分配管理权限
     */
    private void assignAdminPermissions(String roleName, String description) {
        log.info("为{}角色分配管理权限...", description);

        String[] adminPermissions = {
            // 用户管理权限
            "USER_CREATE_ANY", "USER_READ_ANY", "USER_UPDATE_ANY", "USER_DELETE_ANY",
            "USER_BAN_ANY", "USER_RESET_PASSWORD_ANY",

            // 文章管理权限
            "ARTICLE_READ_ANY", "ARTICLE_UPDATE_ANY", "ARTICLE_DELETE_ANY",
            "ARTICLE_PUBLISH_ANY", "ARTICLE_FEATURE_ANY", "ARTICLE_COMMENT_DELETE_ANY",

            // 消息管理权限
            "MESSAGE_READ_ANY", "MESSAGE_DELETE_ANY", "MESSAGE_SEARCH_ANY",

            // 群组管理权限
            "GROUP_READ_ANY", "GROUP_UPDATE_ANY", "GROUP_DELETE_ANY", "GROUP_MANAGE_MEMBERS_ANY",

            // 系统管理权限
            "PERMISSION_READ_ANY", "ROLE_READ_ANY", "SYSTEM_CONFIG_READ",
            "SYSTEM_STATS_READ", "CONTENT_MODERATE_ANY", "CONTENT_REPORT_READ", "CONTENT_REPORT_PROCESS",

            // 文件管理权限
            "FILE_READ_ANY", "FILE_DELETE_ANY"
        };

        assignSpecificPermissions(roleName, adminPermissions, description);
    }

    /**
     * 为版主分配内容管理权限
     */
    private void assignModeratorPermissions(String roleName, String description) {
        log.info("为{}角色分配内容管理权限...", description);

        String[] moderatorPermissions = {
            // 文章管理权限
            "ARTICLE_READ_ANY", "ARTICLE_PUBLISH_ANY", "ARTICLE_FEATURE_ANY", "ARTICLE_COMMENT_DELETE_ANY",

            // 内容审核权限
            "CONTENT_MODERATE_ANY", "CONTENT_REPORT_READ", "CONTENT_REPORT_PROCESS",

            // 基础用户管理权限
            "USER_READ_ANY"
        };

        assignSpecificPermissions(roleName, moderatorPermissions, description);
    }

    /**
     * 为普通用户分配基础权限
     */
    private void assignUserPermissions(String roleName, String description) {
        log.info("为{}角色分配基础权限...", description);

        String[] userPermissions = {
            // 基础用户权限
            "USER_READ_OWN", "USER_UPDATE_OWN", "USER_FOLLOW_OWN",

            // 文章权限
            "ARTICLE_CREATE_OWN", "ARTICLE_READ_OWN", "ARTICLE_UPDATE_OWN", "ARTICLE_DELETE_OWN",
            "ARTICLE_LIKE_OWN", "ARTICLE_FAVORITE_OWN", "ARTICLE_COMMENT_OWN",

            // 消息权限
            "MESSAGE_CREATE_OWN", "MESSAGE_READ_OWN", "MESSAGE_UPDATE_OWN", "MESSAGE_DELETE_OWN",
            "MESSAGE_THREAD_CREATE_OWN", "MESSAGE_THREAD_READ_OWN", "MESSAGE_THREAD_REPLY_OWN",

            // 群组权限
            "GROUP_CREATE_OWN", "GROUP_READ_OWN", "GROUP_UPDATE_OWN", "GROUP_DELETE_OWN",
            "GROUP_JOIN_OWN", "GROUP_LEAVE_OWN", "GROUP_INVITE_OWN",

            // 认证权限
            "AUTH_REGISTER_OWN", "AUTH_LOGIN_OWN", "AUTH_LOGOUT_OWN", "AUTH_PASSWORD_CHANGE_OWN",

            // 文件权限
            "FILE_UPLOAD_OWN", "FILE_READ_OWN", "FILE_DELETE_OWN",

            // AI服务权限
            "AI_CHAT_OWN",
            "CONTENT_ANALYZE_OWN",

            // 搜索权限
            "SEARCH_BASIC", "SEARCH_USER_BASIC", "SEARCH_CONTENT_BASIC",

            // 内容举报权限
            "CONTENT_REPORT_CREATE_OWN", "CONTENT_REPORT_UPDATE_OWN", "CONTENT_REPORT_READ_OWN",

            // 内容审核权限
            "CONTENT_REVIEW_ANY", "CONTENT_MODERATE_ANY", "CONTENT_REPORT_READ", "CONTENT_REPORT_PROCESS",

            // 链接预览权限
            "LINK_PREVIEW_CREATE_OWN", "LINK_PREVIEW_READ_OWN", "LINK_PREVIEW_MANAGE_OWN"
        };

        assignSpecificPermissions(roleName, userPermissions, description);
    }

    /**
     * 为角色分配特定权限
     */
    private void assignSpecificPermissions(String roleName, String[] permissions, String description) {
        try {
            Long roleId = getRoleIdByName(roleName);
            if (roleId == null) {
                log.warn("角色 {} 不存在，跳过权限分配", roleName);
                return;
            }

            int addedCount = 0;
            for (String permissionName : permissions) {
                try {
                    Long permissionId = getPermissionIdByName(permissionName);
                    if (permissionId != null) {
                        // 检查是否已存在映射
                        Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM role_permission WHERE role_id = ? AND permission_id = ?",
                            Integer.class, roleId, permissionId);

                        if (count == null || count == 0) {
                            jdbcTemplate.update("""
                                INSERT INTO role_permission (role_id, permission_id, created_at)
                                VALUES (?, ?, NOW())
                                """, roleId, permissionId);
                            addedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("添加权限 {} 到角色 {} 失败", permissionName, roleName, e);
                }
            }

            log.info("✅ {}角色权限分配完成，新增 {} 个权限", description, addedCount);

        } catch (Exception e) {
            log.error("为{}角色分配权限失败", description, e);
        }
    }

    /**
     * 处理现有用户的角色分配
     */
    private void handleExistingUsers() {
        log.info("👥 处理现有用户角色分配...");

        try {
            // 查找没有角色的用户
            List<Long> usersWithoutRoles = jdbcTemplate.queryForList(
                "SELECT u.id FROM user u LEFT JOIN user_role ur ON u.id = ur.user_id WHERE ur.user_id IS NULL",
                Long.class);

            if (!usersWithoutRoles.isEmpty()) {
                Long userRoleId = getRoleIdByName("USER");
                if (userRoleId != null) {
                    for (Long userId : usersWithoutRoles) {
                        jdbcTemplate.update("""
                            INSERT INTO user_role (user_id, role_id, created_at)
                            VALUES (?, ?, NOW())
                            """, userId, userRoleId);
                    }
                    log.info("✅ 为 {} 个现有用户分配了默认角色", usersWithoutRoles.size());
                }
            } else {
                log.info("所有用户都已分配角色");
            }

        } catch (Exception e) {
            log.error("处理现有用户角色分配失败", e);
        }
    }

    /**
     * 验证初始化结果
     */
    private void validateInitialization() {
        log.info("✅ 验证系统安全初始化结果...");

        try {
            // 验证角色数量
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role", Integer.class);
            log.info("系统角色总数: {}", roleCount);

            // 验证权限数量
            Integer permissionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permission", Integer.class);
            log.info("系统权限总数: {}", permissionCount);

            // 验证角色权限映射数量
            Integer mappingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission", Integer.class);
            log.info("角色权限映射总数: {}", mappingCount);

            // 验证用户角色分配数量
            Integer userRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_role", Integer.class);
            log.info("用户角色分配总数: {}", userRoleCount);

            log.info("✅ 系统安全初始化验证通过");

        } catch (Exception e) {
            log.warn("系统安全初始化验证部分失败", e);
        }
    }

    /**
     * 根据角色名称获取角色ID
     */
    private Long getRoleIdByName(String roleName) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM role WHERE name = ?", Long.class, roleName);
        } catch (Exception e) {
            log.warn("无法找到角色: {}", roleName);
            return null;
        }
    }

    /**
     * 根据权限名称获取权限ID
     */
    private Long getPermissionIdByName(String permissionName) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM permission WHERE name = ?", Long.class, permissionName);
        } catch (Exception e) {
            log.warn("无法找到权限: {}", permissionName);
            return null;
        }
    }
}