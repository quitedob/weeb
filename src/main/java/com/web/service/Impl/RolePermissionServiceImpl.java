package com.web.service.Impl;

import com.web.constant.Permissions;
import com.web.constant.UserLevel;
import com.web.exception.WeebException;
import com.web.mapper.*;
import com.web.model.*;
import com.web.service.RolePermissionService;
import com.web.service.UserLevelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色权限服务实现类
 * 处理角色权限自动分配和管理逻辑
 */
@Slf4j
@Service
@Transactional
public class RolePermissionServiceImpl implements RolePermissionService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private UserLevelService userLevelService;

    @Override
    public Map<String, Object> autoAssignRoles(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            int userLevel = userLevelService.getUserLevel(userId);
            List<Role> targetRoles = getRolesForUserLevel(userLevel);
            List<Role> currentRoles = getUserRoles(userId);

            // 计算需要添加和移除的角色
            Set<Long> currentRoleIds = currentRoles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());
            Set<Long> targetRoleIds = targetRoles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            Set<Long> rolesToAdd = new HashSet<>(targetRoleIds);
            rolesToAdd.removeAll(currentRoleIds);

            Set<Long> rolesToRemove = new HashSet<>(currentRoleIds);
            rolesToRemove.removeAll(targetRoleIds);

            // 执行角色变更
            List<Map<String, Object>> addedRoles = new ArrayList<>();
            List<Map<String, Object>> removedRoles = new ArrayList<>();

            // 添加新角色
            for (Long roleId : rolesToAdd) {
                if (assignRole(userId, roleId, null)) { // 系统自动分配
                    Role role = roleMapper.selectById(roleId);
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("roleId", roleId);
                    roleInfo.put("roleName", role.getName());
                    roleInfo.put("roleDescription", role.getDescription());
                    addedRoles.add(roleInfo);
                }
            }

            // 移除不再符合的角色
            for (Long roleId : rolesToRemove) {
                if (removeRole(userId, roleId, null)) { // 系统自动移除
                    Role role = roleMapper.selectById(roleId);
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("roleId", roleId);
                    roleInfo.put("roleName", role.getName());
                    roleInfo.put("roleDescription", role.getDescription());
                    removedRoles.add(roleInfo);
                }
            }

            result.put("userId", userId);
            result.put("userLevel", userLevel);
            result.put("userLevelName", UserLevel.getLevelName(userLevel));
            result.put("targetRoles", targetRoles);
            result.put("currentRoles", currentRoles);
            result.put("addedRoles", addedRoles);
            result.put("removedRoles", removedRoles);
            result.put("totalChanges", addedRoles.size() + removedRoles.size());
            result.put("message", String.format("角色自动分配完成，新增 %d 个角色，移除 %d 个角色",
                    addedRoles.size(), removedRoles.size()));

            log.info("用户角色自动分配完成: userId={}, userLevel={}, added={}, removed={}",
                    userId, userLevel, addedRoles.size(), removedRoles.size());

            return result;

        } catch (Exception e) {
            log.error("自动分配角色失败: userId={}", userId, e);
            result.put("error", "自动分配角色失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<Role> getRolesForUserLevel(int userLevel) {
        List<Role> roles = new ArrayList<>();

        // 根据用户等级分配角色
        if (userLevel >= UserLevel.LEVEL_BASIC_USER) {
            // 基础用户角色
            roles.add(getOrCreateRole("普通用户", "基础用户角色，拥有基本使用权限"));
        }

        if (userLevel >= UserLevel.LEVEL_ADVANCED_USER) {
            // 高级用户角色
            roles.add(getOrCreateRole("高级用户", "高级用户角色，拥有扩展权限"));
        }

        if (userLevel >= UserLevel.LEVEL_ACTIVE_USER) {
            // 活跃用户角色
            roles.add(getOrCreateRole("活跃用户", "活跃用户角色，拥有更多社交和管理权限"));
        }

        if (userLevel >= UserLevel.LEVEL_VIP_USER) {
            // VIP用户角色
            roles.add(getOrCreateRole("VIP用户", "VIP用户角色，拥有高级功能和特权"));
        }

        if (userLevel >= UserLevel.LEVEL_CONTENT_CREATOR) {
            // 内容创作者角色
            roles.add(getOrCreateRole("内容创作者", "内容创作者角色，拥有内容变现和高级分析权限"));
        }

        if (userLevel >= UserLevel.LEVEL_COMMUNITY_MODERATOR) {
            // 社区管理员角色
            roles.add(getOrCreateRole("社区管理员", "社区管理员角色，协助管理社区内容"));
        }

        if (userLevel >= UserLevel.LEVEL_ADMIN) {
            // 管理员角色
            roles.add(getOrCreateRole("管理员", "系统管理员，拥有完整管理权限"));
        }

        if (userLevel >= UserLevel.LEVEL_SUPER_ADMIN) {
            // 超级管理员角色
            roles.add(getOrCreateRole("超级管理员", "超级管理员，拥有系统最高权限"));
        }

        return roles;
    }

    /**
     * 获取或创建角色
     */
    private Role getOrCreateRole(String roleName, String description) {
        // 先尝试查找现有角色
        Role existingRole = roleMapper.selectByName(roleName);
        if (existingRole != null) {
            return existingRole;
        }

        // 创建新角色
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        role.setCreatedAt(java.time.LocalDateTime.now());
        role.setUpdatedAt(java.time.LocalDateTime.now());

        roleMapper.insert(role);

        // 为新角色分配默认权限
        assignDefaultPermissionsToRole(role.getId(), roleName);

        log.info("创建新角色: roleId={}, roleName={}", role.getId(), roleName);
        return role;
    }

    /**
     * 为角色分配默认权限
     */
    private void assignDefaultPermissionsToRole(Long roleId, String roleName) {
        try {
            List<Long> permissionIds = new ArrayList<>();

            // 根据角色名称分配相应权限
            switch (roleName) {
                case "普通用户":
                    permissionIds.addAll(getBasicUserPermissions());
                    break;
                case "高级用户":
                    permissionIds.addAll(getBasicUserPermissions());
                    permissionIds.addAll(getAdvancedUserPermissions());
                    break;
                case "活跃用户":
                    permissionIds.addAll(getBasicUserPermissions());
                    permissionIds.addAll(getAdvancedUserPermissions());
                    permissionIds.addAll(getActiveUserPermissions());
                    break;
                case "VIP用户":
                    permissionIds.addAll(getBasicUserPermissions());
                    permissionIds.addAll(getAdvancedUserPermissions());
                    permissionIds.addAll(getActiveUserPermissions());
                    permissionIds.addAll(getVipUserPermissions());
                    break;
                case "内容创作者":
                    permissionIds.addAll(getBasicUserPermissions());
                    permissionIds.addAll(getAdvancedUserPermissions());
                    permissionIds.addAll(getActiveUserPermissions());
                    permissionIds.addAll(getContentCreatorPermissions());
                    break;
                case "社区管理员":
                    permissionIds.addAll(getBasicUserPermissions());
                    permissionIds.addAll(getAdvancedUserPermissions());
                    permissionIds.addAll(getActiveUserPermissions());
                    permissionIds.addAll(getCommunityModeratorPermissions());
                    break;
                case "管理员":
                    permissionIds.addAll(getAllAdminPermissions());
                    break;
                case "超级管理员":
                    permissionIds.addAll(getAllPermissions());
                    break;
            }

            // 批量插入角色权限映射
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreatedAt(java.time.LocalDateTime.now());
                rolePermissionMapper.insert(rolePermission);
            }

            log.info("为角色分配默认权限: roleId={}, roleName={}, permissionCount={}",
                    roleId, roleName, permissionIds.size());

        } catch (Exception e) {
            log.error("为角色分配默认权限失败: roleId={}, roleName={}", roleId, roleName, e);
        }
    }

    @Override
    public boolean hasRole(Long userId, String roleName) {
        try {
            List<Role> userRoles = getUserRoles(userId);
            return userRoles.stream()
                    .anyMatch(role -> roleName.equals(role.getName()));
        } catch (Exception e) {
            log.error("检查用户角色失败: userId={}, roleName={}", userId, roleName, e);
            return false;
        }
    }

    @Override
    public boolean assignRole(Long userId, Long roleId, Long operatorId) {
        try {
            // 验证用户和角色是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new WeebException("用户不存在: " + userId);
            }

            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new WeebException("角色不存在: " + roleId);
            }

            // 检查是否已经拥有该角色
            UserRole userRole = userRoleMapper.selectByUserAndRole(userId, roleId);
            if (userRole != null) {
                log.warn("用户已拥有该角色: userId={}, roleId={}", userId, roleId);
                return true;
            }

            // 创建用户角色关联
            userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setAssignedBy(operatorId);
            userRole.setAssignedAt(java.time.LocalDateTime.now());

            int result = userRoleMapper.insert(userRole);
            if (result > 0) {
                log.info("角色分配成功: userId={}, roleId={}, operatorId={}", userId, roleId, operatorId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("分配角色失败: userId={}, roleId={}, operatorId={}",
                     userId, roleId, operatorId, e);
            throw new WeebException("分配角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removeRole(Long userId, Long roleId, Long operatorId) {
        try {
            UserRole userRole = userRoleMapper.selectByUserAndRole(userId, roleId);
            if (userRole == null) {
                log.warn("用户不拥有该角色: userId={}, roleId={}", userId, roleId);
                return true;
            }

            int result = userRoleMapper.deleteById(userRole.getId());
            if (result > 0) {
                log.info("角色移除成功: userId={}, roleId={}, operatorId={}", userId, roleId, operatorId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("移除角色失败: userId={}, roleId={}, operatorId={}",
                     userId, roleId, operatorId, e);
            throw new WeebException("移除角色失败: " + e.getMessage());
        }
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        try {
            return userRoleMapper.selectRolesByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Permission> getRolePermissions(Long roleId) {
        try {
            List<RolePermission> rolePermissions = rolePermissionMapper.findByRoleId(roleId);
            List<Permission> permissions = new ArrayList<>();

            for (RolePermission rp : rolePermissions) {
                Permission permission = permissionMapper.selectById(rp.getPermissionId());
                if (permission != null) {
                    permissions.add(permission);
                }
            }

            return permissions;
        } catch (Exception e) {
            log.error("获取角色权限失败: roleId={}", roleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getAllUserPermissions(Long userId) {
        Set<String> allPermissions = new HashSet<>();

        try {
            // 获取用户等级权限
            allPermissions.addAll(userLevelService.getUserPermissions(userId));

            // 获取角色权限
            List<Role> userRoles = getUserRoles(userId);
            for (Role role : userRoles) {
                List<Permission> rolePermissions = getRolePermissions(role.getId());
                for (Permission permission : rolePermissions) {
                    allPermissions.add(permission.getName());
                }
            }

            return allPermissions;
        } catch (Exception e) {
            log.error("获取用户所有权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        try {
            Set<String> allPermissions = getAllUserPermissions(userId);
            return allPermissions.contains(permission);
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> batchAutoAssignRoles(List<Long> userIds) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successResults = new ArrayList<>();
        List<Map<String, Object>> failedResults = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                Map<String, Object> assignResult = autoAssignRoles(userId);
                if (!assignResult.containsKey("error")) {
                    successResults.add(assignResult);
                } else {
                    Map<String, Object> failedResult = new HashMap<>();
                    failedResult.put("userId", userId);
                    failedResult.put("error", assignResult.get("error"));
                    failedResults.add(failedResult);
                }
            } catch (Exception e) {
                Map<String, Object> failedResult = new HashMap<>();
                failedResult.put("userId", userId);
                failedResult.put("error", e.getMessage());
                failedResults.add(failedResult);
                log.error("批量自动分配角色失败: userId={}", userId, e);
            }
        }

        result.put("totalUsers", userIds.size());
        result.put("successCount", successResults.size());
        result.put("failCount", failedResults.size());
        result.put("successResults", successResults);
        result.put("failedResults", failedResults);

        return result;
    }

    @Override
    public boolean updateRolePermissions(Long roleId, List<Long> permissionIds, Long operatorId) {
        try {
            // 验证角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new WeebException("角色不存在: " + roleId);
            }

            // 删除现有权限映射
            rolePermissionMapper.deleteByRoleId(roleId);

            // 添加新的权限映射
            for (Long permissionId : permissionIds) {
                // 验证权限是否存在
                Permission permission = permissionMapper.selectById(permissionId);
                if (permission == null) {
                    log.warn("权限不存在，跳过: permissionId={}", permissionId);
                    continue;
                }

                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreatedAt(java.time.LocalDateTime.now());
                rolePermissionMapper.insert(rolePermission);
            }

            // 更新角色信息
            role.setUpdatedAt(java.time.LocalDateTime.now());
            roleMapper.updateById(role);

            log.info("角色权限更新成功: roleId={}, permissionCount={}, operatorId={}",
                    roleId, permissionIds.size(), operatorId);
            return true;

        } catch (Exception e) {
            log.error("更新角色权限失败: roleId={}, operatorId={}", roleId, operatorId, e);
            throw new WeebException("更新角色权限失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRoleAssignmentStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 获取所有角色统计
            List<Map<String, Object>> roleStats = new ArrayList<>();
            List<Role> allRoles = roleMapper.selectList(null);

            for (Role role : allRoles) {
                int userCount = (int) userRoleMapper.countUsersByRole(role.getId());
                int permissionCount = (int) rolePermissionMapper.countByRoleId(role.getId());

                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("roleId", role.getId());
                roleInfo.put("roleName", role.getName());
                roleInfo.put("roleDescription", role.getDescription());
                roleInfo.put("userCount", userCount);
                roleInfo.put("permissionCount", permissionCount);
                roleInfo.put("createdAt", role.getCreatedAt());
                roleInfo.put("updatedAt", role.getUpdatedAt());

                roleStats.add(roleInfo);
            }

            statistics.put("roleStatistics", roleStats);
            statistics.put("totalRoles", allRoles.size());
            statistics.put("totalRoleAssignments", userRoleMapper.selectCount(null));

        } catch (Exception e) {
            log.error("获取角色分配统计失败", e);
            statistics.put("error", "获取统计信息失败: " + e.getMessage());
        }

        return statistics;
    }

    @Override
    public Map<String, Object> validateRolePermissions(Long roleId) {
        Map<String, Object> validation = new HashMap<>();

        try {
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                validation.put("valid", false);
                validation.put("error", "角色不存在");
                return validation;
            }

            List<Permission> rolePermissions = getRolePermissions(roleId);
            List<String> issues = new ArrayList<>();

            // 检查权限是否合理
            if (rolePermissions.isEmpty()) {
                issues.add("角色没有任何权限");
            }

            // 检查是否有重复权限
            Set<Long> permissionIds = rolePermissions.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
            if (permissionIds.size() != rolePermissions.size()) {
                issues.add("角色中存在重复权限");
            }

            // 检查权限是否适合该角色
            List<String> inappropriatePermissions = checkInappropriatePermissions(role.getName(), rolePermissions);
            if (!inappropriatePermissions.isEmpty()) {
                issues.add("角色包含不合适的权限: " + String.join(", ", inappropriatePermissions));
            }

            validation.put("valid", issues.isEmpty());
            validation.put("roleId", roleId);
            validation.put("roleName", role.getName());
            validation.put("permissionCount", rolePermissions.size());
            validation.put("issues", issues);

            if (issues.isEmpty()) {
                validation.put("message", "角色权限配置正常");
            } else {
                validation.put("message", "角色权限配置存在问题");
            }

        } catch (Exception e) {
            log.error("验证角色权限失败: roleId={}", roleId, e);
            validation.put("valid", false);
            validation.put("error", "验证失败: " + e.getMessage());
        }

        return validation;
    }

    @Override
    public Map<String, Object> syncUserRolesOnLevelChange(Long userId, int oldLevel, int newLevel) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Role> oldRoles = getRolesForUserLevel(oldLevel);
            List<Role> newRoles = getRolesForUserLevel(newLevel);

            Set<Long> oldRoleIds = oldRoles.stream().map(Role::getId).collect(Collectors.toSet());
            Set<Long> newRoleIds = newRoles.stream().map(Role::getId).collect(Collectors.toSet());

            Set<Long> rolesToAdd = new HashSet<>(newRoleIds);
            rolesToAdd.removeAll(oldRoleIds);

            Set<Long> rolesToRemove = new HashSet<>(oldRoleIds);
            rolesToRemove.removeAll(newRoleIds);

            // 执行角色变更
            List<Map<String, Object>> addedRoles = new ArrayList<>();
            List<Map<String, Object>> removedRoles = new ArrayList<>();

            for (Long roleId : rolesToAdd) {
                if (assignRole(userId, roleId, null)) {
                    Role role = roleMapper.selectById(roleId);
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("roleId", roleId);
                    roleInfo.put("roleName", role.getName());
                    addedRoles.add(roleInfo);
                }
            }

            for (Long roleId : rolesToRemove) {
                if (removeRole(userId, roleId, null)) {
                    Role role = roleMapper.selectById(roleId);
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("roleId", roleId);
                    roleInfo.put("roleName", role.getName());
                    removedRoles.add(roleInfo);
                }
            }

            result.put("userId", userId);
            result.put("oldLevel", oldLevel);
            result.put("newLevel", newLevel);
            result.put("oldLevelName", UserLevel.getLevelName(oldLevel));
            result.put("newLevelName", UserLevel.getLevelName(newLevel));
            result.put("addedRoles", addedRoles);
            result.put("removedRoles", removedRoles);
            result.put("message", String.format("等级变更角色同步完成，新增 %d 个角色，移除 %d 个角色",
                    addedRoles.size(), removedRoles.size()));

            log.info("用户等级变更角色同步完成: userId={}, oldLevel={}, newLevel={}, added={}, removed={}",
                    userId, oldLevel, newLevel, addedRoles.size(), removedRoles.size());

            return result;

        } catch (Exception e) {
            log.error("同步用户角色失败: userId={}, oldLevel={}, newLevel={}", userId, oldLevel, newLevel, e);
            result.put("error", "角色同步失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getRolePermissionRecommendations(int userLevel) {
        Map<String, Object> recommendations = new HashMap<>();

        try {
            List<Role> recommendedRoles = getRolesForUserLevel(userLevel);
            Map<String, Object> permissionsByRole = new HashMap<>();

            for (Role role : recommendedRoles) {
                List<Permission> permissions = getRolePermissions(role.getId());
                List<String> permissionNames = permissions.stream()
                        .map(Permission::getName)
                        .collect(Collectors.toList());
                permissionsByRole.put(role.getName(), permissionNames);
            }

            recommendations.put("userLevel", userLevel);
            recommendations.put("userLevelName", UserLevel.getLevelName(userLevel));
            recommendations.put("recommendedRoles", recommendedRoles);
            recommendations.put("permissionsByRole", permissionsByRole);

            // 提供配置建议
            recommendations.put("suggestions", generateConfigurationSuggestions(userLevel));

        } catch (Exception e) {
            log.error("获取角色权限建议失败: userLevel={}", userLevel, e);
            recommendations.put("error", "获取建议失败: " + e.getMessage());
        }

        return recommendations;
    }

    @Override
    public boolean applyRolePermissionTemplate(String templateName, Long targetRoleId, Long operatorId) {
        try {
            List<Long> templatePermissions = getRolePermissionTemplate(templateName);
            return updateRolePermissions(targetRoleId, templatePermissions, operatorId);
        } catch (Exception e) {
            log.error("应用角色权限模板失败: templateName={}, targetRoleId={}", templateName, targetRoleId, e);
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    private List<Long> getBasicUserPermissions() {
        // 从权限常量中获取基础用户权限
        return Arrays.asList(
            getPermissionId(Permissions.USER_READ_OWN),
            getPermissionId(Permissions.USER_UPDATE_OWN),
            getPermissionId(Permissions.USER_EDIT_PROFILE_OWN),
            getPermissionId(Permissions.USER_SETTINGS_OWN),
            getPermissionId(Permissions.ARTICLE_CREATE_OWN),
            getPermissionId(Permissions.ARTICLE_READ_OWN),
            getPermissionId(Permissions.ARTICLE_LIKE),
            getPermissionId(Permissions.ARTICLE_COMMENT_OWN),
            getPermissionId(Permissions.MESSAGE_CREATE_OWN),
            getPermissionId(Permissions.MESSAGE_READ_OWN),
            getPermissionId(Permissions.MESSAGE_RECALL_OWN),
            getPermissionId(Permissions.GROUP_CREATE_OWN),
            getPermissionId(Permissions.GROUP_JOIN_OWN),
            getPermissionId(Permissions.SEARCH_BASIC),
            getPermissionId(Permissions.LEVEL_UP_VIEW_REQUIREMENTS),
            getPermissionId(Permissions.LEVEL_UP_TRACK_PROGRESS)
        );
    }

    private List<Long> getAdvancedUserPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.ADVANCED_USER_GROUP_CREATE_LIMITED),
            getPermissionId(Permissions.ADVANCED_USER_ARTICLE_FEATURE_OWN),
            getPermissionId(Permissions.ADVANCED_USER_MESSAGE_THREAD_OWN),
            getPermissionId(Permissions.ADVANCED_USER_CONTACT_LIMIT_INCREASED),
            getPermissionId(Permissions.ADVANCED_USER_SEARCH_ENHANCED),
            getPermissionId(Permissions.ADVANCED_USER_NOTIFICATION_PREFERENCES)
        );
    }

    private List<Long> getActiveUserPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.ACTIVE_USER_GROUP_CREATE_EXTENDED),
            getPermissionId(Permissions.ACTIVE_USER_ARTICLE_MODERATE_ASSIST),
            getPermissionId(Permissions.ACTIVE_USER_MESSAGE_BROADCAST_LIMITED),
            getPermissionId(Permissions.ACTIVE_USER_ANALYTICS_VIEW_OWN),
            getPermissionId(Permissions.ACTIVE_USER_API_ACCESS_BASIC),
            getPermissionId(Permissions.ACTIVE_USER_CUSTOM_PROFILE),
            getPermissionId(Permissions.ACTIVE_USER_INVITE_USERS)
        );
    }

    private List<Long> getVipUserPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.VIP_USER_GROUP_CREATE_UNLIMITED),
            getPermissionId(Permissions.VIP_USER_ARTICLE_PRIORITY),
            getPermissionId(Permissions.VIP_USER_MESSAGE_PIN_OWN),
            getPermissionId(Permissions.VIP_USER_ANALYTICS_ADVANCED),
            getPermissionId(Permissions.VIP_USER_API_ACCESS_EXTENDED),
            getPermissionId(Permissions.VIP_USER_CUSTOM_THEME),
            getPermissionId(Permissions.VIP_USER_PRIORITY_SUPPORT),
            getPermissionId(Permissions.VIP_USER_BETA_FEATURES)
        );
    }

    private List<Long> getContentCreatorPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.CONTENT_CREATOR_ARTICLE_MONETIZE),
            getPermissionId(Permissions.CONTENT_CREATOR_ANALYTICS_DETAILED),
            getPermissionId(Permissions.CONTENT_CREATOR_BRAND_CUSTOMIZATION),
            getPermissionId(Permissions.CONTENT_CREATOR_FOLLOWER_ANALYTICS),
            getPermissionId(Permissions.CONTENT_CREATOR_SCHEDULED_POSTING)
        );
    }

    private List<Long> getCommunityModeratorPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.COMMUNITY_MODERATE_CONTENT),
            getPermissionId(Permissions.COMMUNITY_MANAGE_REPORTS),
            getPermissionId(Permissions.COMMUNITY_WARN_USERS),
            getPermissionId(Permissions.COMMUNITY_FEATURE_CONTENT),
            getPermissionId(Permissions.COMMUNITY_ANNOUNCE_LOCAL),
            getPermissionId(Permissions.COMMUNITY_ANALYTICS_BASIC)
        );
    }

    private List<Long> getAllAdminPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.SYSTEM_ADMIN),
            getPermissionId(Permissions.SYSTEM_CONFIG),
            getPermissionId(Permissions.SYSTEM_LOG),
            getPermissionId(Permissions.SYSTEM_MONITOR),
            getPermissionId(Permissions.USER_CREATE),
            getPermissionId(Permissions.USER_UPDATE),
            getPermissionId(Permissions.USER_DELETE),
            getPermissionId(Permissions.USER_BAN),
            getPermissionId(Permissions.ROLE_ASSIGN),
            getPermissionId(Permissions.PERMISSION_UPDATE),
            getPermissionId(Permissions.NOTIFICATION_MANAGE),
            getPermissionId(Permissions.SECURITY_AUDIT),
            getPermissionId(Permissions.API_ADMIN)
        );
    }

    private List<Long> getAllPermissions() {
        return Arrays.asList(
            getPermissionId(Permissions.SYSTEM_ADMIN),
            getPermissionId(Permissions.SYSTEM_CONFIG),
            getPermissionId(Permissions.SYSTEM_LOG),
            getPermissionId(Permissions.SYSTEM_BACKUP),
            getPermissionId(Permissions.SYSTEM_RESTORE),
            getPermissionId(Permissions.SYSTEM_MONITOR),
            getPermissionId(Permissions.SECURITY_PASSWORD),
            getPermissionId(Permissions.API_ADMIN),
            getPermissionId(Permissions.PERMISSION_UPDATE),
            getPermissionId(Permissions.ROLE_ASSIGN),
            getPermissionId(Permissions.USER_CREATE),
            getPermissionId(Permissions.USER_UPDATE),
            getPermissionId(Permissions.USER_DELETE),
            getPermissionId(Permissions.USER_BAN),
            getPermissionId(Permissions.USER_RESET_PASSWORD),
            getPermissionId(Permissions.MESSAGE_SEND),
            getPermissionId(Permissions.MESSAGE_READ),
            getPermissionId(Permissions.ARTICLE_CREATE),
            getPermissionId(Permissions.SEARCH_USER),
            getPermissionId(Permissions.NOTIFICATION_SEND)
        );
    }

    private Long getPermissionId(String permissionName) {
        try {
            Permission permission = permissionMapper.selectByName(permissionName);
            return permission != null ? permission.getId() : null;
        } catch (Exception e) {
            log.warn("获取权限ID失败: permissionName={}", permissionName, e);
            return null;
        }
    }

    private List<String> checkInappropriatePermissions(String roleName, List<Permission> permissions) {
        List<String> inappropriate = new ArrayList<>();

        // 检查普通用户是否拥有管理员权限
        if ("普通用户".equals(roleName) || "高级用户".equals(roleName)) {
            for (Permission permission : permissions) {
                if (permission.getName().startsWith("SYSTEM_") ||
                    permission.getName().startsWith("USER_CREATE") ||
                    permission.getName().startsWith("USER_DELETE") ||
                    permission.getName().startsWith("USER_BAN")) {
                    inappropriate.add(permission.getName());
                }
            }
        }

        return inappropriate;
    }

    private List<Long> getRolePermissionTemplate(String templateName) {
        // 根据模板名称返回相应的权限ID列表
        switch (templateName.toLowerCase()) {
            case "basic":
                return getBasicUserPermissions();
            case "advanced":
                List<Long> advanced = new ArrayList<>(getBasicUserPermissions());
                advanced.addAll(getAdvancedUserPermissions());
                return advanced;
            case "vip":
                List<Long> vip = new ArrayList<>(getBasicUserPermissions());
                vip.addAll(getAdvancedUserPermissions());
                vip.addAll(getVipUserPermissions());
                return vip;
            case "admin":
                return getAllAdminPermissions();
            default:
                return Collections.emptyList();
        }
    }

    private Map<String, Object> generateConfigurationSuggestions(int userLevel) {
        Map<String, Object> suggestions = new HashMap<>();

        if (userLevel == UserLevel.LEVEL_ADVANCED_USER) {
            suggestions.put("权限范围", "建议限制在内容创建和基础社交功能");
            suggestions.put("群组管理", "允许创建有限数量的群组");
            suggestions.put("内容发布", "允许发布和推广自己的内容");
        }

        if (userLevel == UserLevel.LEVEL_ACTIVE_USER) {
            suggestions.put("权限范围", "可以参与社区协助管理");
            suggestions.put("广播消息", "允许在有限范围内广播消息");
            suggestions.put("数据分析", "可以查看自己的活动数据");
        }

        if (userLevel == UserLevel.LEVEL_VIP_USER) {
            suggestions.put("权限范围", "拥有大部分高级功能权限");
            suggestions.put("API访问", "可以访问扩展API接口");
            suggestions.put("定制功能", "可以使用主题定制等高级功能");
        }

        return suggestions;
    }
}