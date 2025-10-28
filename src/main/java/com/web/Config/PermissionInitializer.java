package com.web.Config;

import com.web.model.Permission;
import com.web.model.Role;
import com.web.service.PermissionService;
import com.web.service.RolePermissionService;
import com.web.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限初始化器
 * 在应用启动时自动初始化基本权限和角色权限关联
 */
@Slf4j
// @Component // 已禁用权限系统
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(2) // 在DatabaseInitializer之后执行
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionService permissionService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("开始初始化权限数据...");

        try {
            // 初始化基本权限
            initializePermissions();

            // 为普通用户角色分配权限
            assignPermissionsToUserRole();

            // 执行完整的用户权限修复（基于fix_user_permissions.sql）
            executeCompletePermissionFix();

            log.info("权限数据初始化完成");
        } catch (Exception e) {
            log.error("权限数据初始化失败", e);
        }
    }

    /**
     * 初始化基本权限
     */
    private void initializePermissions() {
        List<Permission> permissions = new ArrayList<>();

        // ========== 文章相关权限 ==========
        permissions.add(createPermission("ARTICLE_CREATE_OWN", "ARTICLE", "CREATE", "OWN", "创建自己的文章"));
        permissions.add(createPermission("ARTICLE_READ_OWN", "ARTICLE", "READ", "OWN", "查看自己的文章"));
        permissions.add(createPermission("ARTICLE_UPDATE_OWN", "ARTICLE", "UPDATE", "OWN", "更新自己的文章"));
        permissions.add(createPermission("ARTICLE_DELETE_OWN", "ARTICLE", "DELETE", "OWN", "删除自己的文章"));
        permissions.add(createPermission("ARTICLE_READ_ANY", "ARTICLE", "READ", "ANY", "查看所有文章"));

        // ========== 评论相关权限 ==========
        permissions.add(createPermission("COMMENT_CREATE_OWN", "COMMENT", "CREATE", "OWN", "发表评论"));
        permissions.add(createPermission("COMMENT_READ_OWN", "COMMENT", "READ", "OWN", "查看自己的评论"));
        permissions.add(createPermission("COMMENT_UPDATE_OWN", "COMMENT", "UPDATE", "OWN", "编辑自己的评论"));
        permissions.add(createPermission("COMMENT_DELETE_OWN", "COMMENT", "DELETE", "OWN", "删除自己的评论"));
        permissions.add(createPermission("COMMENT_READ_ANY", "COMMENT", "READ", "ANY", "查看所有评论"));

        // ========== 点赞相关权限 ==========
        permissions.add(createPermission("LIKE_CREATE_OWN", "LIKE", "CREATE", "OWN", "点赞"));
        permissions.add(createPermission("LIKE_DELETE_OWN", "LIKE", "DELETE", "OWN", "取消点赞"));
        permissions.add(createPermission("LIKE_READ_ANY", "LIKE", "READ", "ANY", "查看点赞信息"));

        // ========== 收藏相关权限 ==========
        permissions.add(createPermission("FAVORITE_CREATE_OWN", "FAVORITE", "CREATE", "OWN", "收藏文章"));
        permissions.add(createPermission("FAVORITE_DELETE_OWN", "FAVORITE", "DELETE", "OWN", "取消收藏"));
        permissions.add(createPermission("FAVORITE_READ_OWN", "FAVORITE", "READ", "OWN", "查看自己的收藏"));

        // ========== 关注相关权限 ==========
        permissions.add(createPermission("FOLLOW_CREATE_OWN", "FOLLOW", "CREATE", "OWN", "关注用户"));
        permissions.add(createPermission("FOLLOW_DELETE_OWN", "FOLLOW", "DELETE", "OWN", "取消关注"));
        permissions.add(createPermission("FOLLOW_READ_OWN", "FOLLOW", "READ", "OWN", "查看自己的关注列表"));
        permissions.add(createPermission("FOLLOW_READ_ANY", "FOLLOW", "READ", "ANY", "查看关注信息"));

        // ========== 好友/联系人相关权限 ==========
        permissions.add(createPermission("CONTACT_CREATE_OWN", "CONTACT", "CREATE", "OWN", "添加好友"));
        permissions.add(createPermission("CONTACT_READ_OWN", "CONTACT", "READ", "OWN", "查看好友列表"));
        permissions.add(createPermission("CONTACT_UPDATE_OWN", "CONTACT", "UPDATE", "OWN", "更新好友信息"));
        permissions.add(createPermission("CONTACT_DELETE_OWN", "CONTACT", "DELETE", "OWN", "删除好友"));
        permissions.add(createPermission("CONTACT_ACCEPT_OWN", "CONTACT", "ACCEPT", "OWN", "同意好友请求"));
        permissions.add(createPermission("CONTACT_REJECT_OWN", "CONTACT", "REJECT", "OWN", "拒绝好友请求"));

        // ========== 群组相关权限 ==========
        permissions.add(createPermission("GROUP_CREATE_OWN", "GROUP", "CREATE", "OWN", "创建群组"));
        permissions.add(createPermission("GROUP_READ_OWN", "GROUP", "READ", "OWN", "查看自己的群组"));
        permissions.add(createPermission("GROUP_UPDATE_OWN", "GROUP", "UPDATE", "OWN", "更新自己的群组"));
        permissions.add(createPermission("GROUP_DELETE_OWN", "GROUP", "DELETE", "OWN", "删除自己的群组"));
        permissions.add(createPermission("GROUP_JOIN_OWN", "GROUP", "JOIN", "OWN", "加入群组"));
        permissions.add(createPermission("GROUP_LEAVE_OWN", "GROUP", "LEAVE", "OWN", "退出群组"));
        permissions.add(createPermission("GROUP_MANAGE_MEMBERS_OWN", "GROUP", "MANAGE_MEMBERS", "OWN", "管理自己群组的成员"));
        permissions.add(createPermission("GROUP_READ_ANY", "GROUP", "READ", "ANY", "查看所有群组"));

        // ========== 用户相关权限 ==========
        permissions.add(createPermission("USER_READ_OWN", "USER", "READ", "OWN", "查看自己的信息"));
        permissions.add(createPermission("USER_UPDATE_OWN", "USER", "UPDATE", "OWN", "更新自己的信息"));
        permissions.add(createPermission("USER_READ_ANY", "USER", "READ", "ANY", "查看其他用户信息"));
        permissions.add(createPermission("USER_SEARCH_ANY", "USER", "SEARCH", "ANY", "搜索用户"));

        // ========== 消息相关权限 ==========
        permissions.add(createPermission("MESSAGE_CREATE_OWN", "MESSAGE", "CREATE", "OWN", "发送消息"));
        permissions.add(createPermission("MESSAGE_READ_OWN", "MESSAGE", "READ", "OWN", "查看自己的消息"));
        permissions.add(createPermission("MESSAGE_DELETE_OWN", "MESSAGE", "DELETE", "OWN", "删除自己的消息"));
        permissions.add(createPermission("MESSAGE_UPDATE_OWN", "MESSAGE", "UPDATE", "OWN", "编辑自己的消息"));

        // ========== 文件相关权限 ==========
        permissions.add(createPermission("FILE_UPLOAD_OWN", "FILE", "UPLOAD", "OWN", "上传文件"));
        permissions.add(createPermission("FILE_READ_OWN", "FILE", "READ", "OWN", "查看自己的文件"));
        permissions.add(createPermission("FILE_DELETE_OWN", "FILE", "DELETE", "OWN", "删除自己的文件"));
        permissions.add(createPermission("FILE_SHARE_OWN", "FILE", "SHARE", "OWN", "分享文件"));

        // ========== 通知相关权限 ==========
        permissions.add(createPermission("NOTIFICATION_READ_OWN", "NOTIFICATION", "READ", "OWN", "查看自己的通知"));
        permissions.add(createPermission("NOTIFICATION_UPDATE_OWN", "NOTIFICATION", "UPDATE", "OWN", "标记通知已读"));

        // 保存权限（如果不存在）
        for (Permission permission : permissions) {
            try {
                // 尝试获取权限，如果抛出异常说明不存在，则创建
                permissionService.getPermissionByName(permission.getName());
                log.debug("权限已存在: {}", permission.getName());
            } catch (Exception e) {
                // 权限不存在，创建新权限
                try {
                    permissionService.createPermission(permission);
                    log.info("创建权限: {}", permission.getName());
                } catch (Exception createEx) {
                    log.warn("创建权限失败: {}, 原因: {}", permission.getName(), createEx.getMessage());
                }
            }
        }
    }

    /**
     * 为普通用户角色分配权限
     */
    private void assignPermissionsToUserRole() {
        try {
            Role userRole = null;
            
            // 尝试查找普通用户角色（支持新旧两种命名方式）
            String[] possibleRoleNames = {"USER", "ROLE_USER"};
            for (String roleName : possibleRoleNames) {
                try {
                    userRole = roleService.getRoleByName(roleName);
                    log.info("找到现有角色: {}", roleName);
                    break;
                } catch (Exception e) {
                    log.debug("角色 {} 不存在", roleName);
                }
            }

            // 如果角色不存在，创建它（使用新命名方式）
            if (userRole == null) {
                try {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    newRole.setDescription("普通用户角色");
                    newRole.setStatus(1);
                    newRole.setType(0);
                    newRole.setLevel(100);
                    newRole.setIsDefault(true);
                    userRole = roleService.createRole(newRole);
                    log.info("创建角色: USER");
                } catch (Exception e) {
                    log.error("创建角色 USER 失败", e);
                    return;
                }
            }

            // 获取所有权限
            List<Permission> allPermissions = permissionService.getAllPermissions();
            
            if (allPermissions == null || allPermissions.isEmpty()) {
                log.warn("没有可用的权限，跳过权限分配");
                return;
            }
            
            // 筛选出需要分配给普通用户的权限
            // 包括：所有OWN级别的权限 + 所有READ_ANY权限 + SEARCH_ANY权限
            List<Long> permissionIds = allPermissions.stream()
                    .filter(p -> p.getName().endsWith("_OWN") 
                              || p.getName().endsWith("_ANY") 
                              || p.getName().contains("SEARCH"))
                    .map(Permission::getId)
                    .collect(Collectors.toList());
            
            // 使用updateRolePermissions方法批量分配权限
            if (!permissionIds.isEmpty()) {
                try {
                    boolean success = rolePermissionService.updateRolePermissions(userRole.getId(), permissionIds, 1L);
                    if (success) {
                        log.info("为角色 {} 分配了 {} 个权限", userRole.getName(), permissionIds.size());
                    } else {
                        log.warn("权限分配未成功");
                    }
                } catch (Exception e) {
                    log.error("批量分配权限失败", e);
                }
            } else {
                log.warn("没有符合条件的权限需要分配");
            }
        } catch (Exception e) {
            log.error("为普通用户角色分配权限失败", e);
        }
    }

    /**
     * 执行完整的用户权限修复逻辑（基于fix_user_permissions.sql）
     * =================================================================================================
     * 功能描述: 完整修复用户权限问题，确保testuser有正确的角色和权限。
     * 执行内容:
     *   1. 查找或创建USER角色
     *   2. 获取USER角色ID
     *   3. 确保testuser有USER角色
     *   4. 为USER角色分配所有OWN权限和ARTICLE_READ_ANY
     *   5. 验证结果
     * =================================================================================================
     */
    private void executeCompletePermissionFix() {
        log.info("开始执行完整的用户权限修复（基于fix_user_permissions.sql）...");

        try {
            // 1. 查找或创建USER角色
            log.info("步骤1: 检查并创建USER角色");
            ensureUserRoleExists();

            // 2. 获取USER角色ID
            Long userRoleId = getUserRoleId();
            if (userRoleId == null) {
                log.error("❌ 无法获取USER角色ID，跳过权限修复");
                return;
            }

            // 3. 确保testuser有USER角色
            log.info("步骤3: 确保testuser有USER角色");
            ensureTestuserHasUserRole(userRoleId);

            // 4. 为USER角色分配所有OWN权限和ARTICLE_READ_ANY
            log.info("步骤4: 为USER角色分配权限");
            assignAllOwnPermissionsToUserRole(userRoleId);

            // 5. 验证结果
            log.info("步骤5: 验证修复结果");
            verifyPermissionFixResults();

            log.info("✅ 完整用户权限修复执行完成");

        } catch (Exception e) {
            log.error("执行用户权限修复时出错", e);
            // 不抛出异常，因为这不是致命错误，只是权限修复失败
        }
    }

    /**
     * 确保USER角色存在（基于fix_user_permissions.sql的步骤1）
     */
    private void ensureUserRoleExists() {
        try {
            String insertRoleSql = """
                INSERT IGNORE INTO `role` (`name`, `description`, `status`, `type`, `level`, `is_default`, `created_at`, `updated_at`)
                VALUES ('USER', '普通用户角色', 1, 0, 100, 1, NOW(), NOW())
                """;

            int rowsAffected = jdbcTemplate.update(insertRoleSql);
            if (rowsAffected > 0) {
                log.info("✅ USER角色创建成功");
            } else {
                log.info("USER角色已存在");
            }
        } catch (Exception e) {
            log.error("创建USER角色时出错", e);
            throw e;
        }
    }

    /**
     * 获取USER角色ID（基于fix_user_permissions.sql的步骤2）
     */
    private Long getUserRoleId() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id FROM `role` WHERE name = 'USER' LIMIT 1",
                Long.class
            );
        } catch (Exception e) {
            log.error("获取USER角色ID时出错", e);
            return null;
        }
    }

    /**
     * 确保所有用户都有USER角色（修复版本 - 为所有用户分配）
     */
    private void ensureTestuserHasUserRole(Long userRoleId) {
        try {
            // 为所有没有USER角色的用户分配USER角色
            String insertUserRoleSql = """
                INSERT IGNORE INTO `user_role` (`user_id`, `role_id`, `created_at`)
                SELECT u.id, ?, NOW()
                FROM `user` u
                WHERE NOT EXISTS (
                    SELECT 1 FROM `user_role` ur
                    WHERE ur.user_id = u.id AND ur.role_id = ?
                )
                """;

            int rowsAffected = jdbcTemplate.update(insertUserRoleSql, userRoleId, userRoleId);
            if (rowsAffected > 0) {
                log.info("✅ 为 {} 个用户分配了USER角色", rowsAffected);
            } else {
                log.info("所有用户已有USER角色");
            }
        } catch (Exception e) {
            log.error("为用户分配USER角色时出错", e);
            throw e;
        }
    }

    /**
     * 为USER角色分配所有OWN权限和ARTICLE_READ_ANY（基于fix_user_permissions.sql的步骤4）
     */
    private void assignAllOwnPermissionsToUserRole(Long userRoleId) {
        try {
            String assignPermissionsSql = """
                INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`, `created_at`)
                SELECT ?, p.id, NOW()
                FROM `permission` p
                WHERE (p.name LIKE '%_OWN' OR p.name LIKE '%_ANY' OR p.name LIKE '%SEARCH%')
                AND p.status = 1
                AND NOT EXISTS (
                    SELECT 1 FROM `role_permission` rp
                    WHERE rp.role_id = ? AND rp.permission_id = p.id
                )
                """;

            int rowsAffected = jdbcTemplate.update(assignPermissionsSql, userRoleId, userRoleId);
            log.info("✅ 为USER角色分配了 {} 个权限", rowsAffected);
        } catch (Exception e) {
            log.error("为USER角色分配权限时出错", e);
            throw e;
        }
    }

    /**
     * 验证权限修复结果（基于fix_user_permissions.sql的步骤5）
     */
    private void verifyPermissionFixResults() {
        try {
            // 验证用户角色
            List<String> userRoleResults = jdbcTemplate.query(
                """
                SELECT
                    '用户角色' as 检查项,
                    u.username as 用户名,
                    r.name as 角色名
                FROM `user` u
                JOIN `user_role` ur ON u.id = ur.user_id
                JOIN `role` r ON ur.role_id = r.id
                WHERE u.username = 'testuser'
                """,
                (rs, rowNum) -> rs.getString("检查项") + ": " + rs.getString("用户名") + " -> " + rs.getString("角色名")
            );

            // 验证角色权限数量
            List<String> rolePermissionResults = jdbcTemplate.query(
                """
                SELECT
                    '角色权限' as 检查项,
                    r.name as 角色名,
                    COUNT(rp.id) as 权限数量
                FROM `role` r
                LEFT JOIN `role_permission` rp ON r.id = rp.role_id AND rp.status = 1
                WHERE r.name = 'USER'
                GROUP BY r.id, r.name
                """,
                (rs, rowNum) -> rs.getString("检查项") + ": " + rs.getString("角色名") + " -> " + rs.getInt("权限数量") + " 个权限"
            );

            // 验证权限详情
            List<String> permissionDetailResults = jdbcTemplate.query(
                """
                SELECT
                    '权限详情' as 检查项,
                    p.name as 权限名称,
                    p.description as 描述
                FROM `role` r
                JOIN `role_permission` rp ON r.id = rp.role_id
                JOIN `permission` p ON rp.permission_id = p.id
                WHERE r.name = 'USER' AND rp.status = 1
                ORDER BY p.name
                """,
                (rs, rowNum) -> rs.getString("权限名称") + ": " + rs.getString("描述")
            );

            log.info("=== 权限修复验证结果 ===");
            userRoleResults.forEach(log::info);
            rolePermissionResults.forEach(log::info);
            log.info("权限详情 (共{}个):", permissionDetailResults.size());
            permissionDetailResults.stream().limit(10).forEach(log::info); // 只显示前10个
            if (permissionDetailResults.size() > 10) {
                log.info("... 还有 {} 个权限", permissionDetailResults.size() - 10);
            }

        } catch (Exception e) {
            log.error("验证权限修复结果时出错", e);
            // 不抛出异常，继续执行
        }
    }

    /**
     * 创建权限对象
     */
    private Permission createPermission(String name, String resource, String action, String condition, String description) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setCondition(condition);
        permission.setDescription(description);
        permission.setStatus(1);
        permission.setType(1);
        return permission;
    }
}
