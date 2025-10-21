package com.web.service.impl;

import com.web.mapper.PermissionMapper;
import com.web.mapper.RoleMapper;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 角色服务实现类
 * 提供角色管理的业务逻辑实现
 */
@Slf4j
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public Role createRole(Role role) {
        try {
            // 检查角色名称是否已存在
            if (roleMapper.existsByName(role.getName())) {
                throw new RuntimeException("角色名称已存在: " + role.getName());
            }

            // 设置默认值
            if (role.getStatus() == null) {
                role.setStatus(1);
            }
            if (role.getType() == null) {
                role.setType(1);
            }
            if (role.getLevel() == null) {
                role.setLevel(100);
            }
            if (role.getIsDefault() == null) {
                role.setIsDefault(false);
            }
            role.setCreatedAt(LocalDateTime.now());
            role.setUpdatedAt(LocalDateTime.now());

            // 插入角色
            int result = roleMapper.insert(role);
            if (result > 0) {
                log.info("创建角色成功: {}", role.getName());
                return role;
            } else {
                throw new RuntimeException("创建角色失败");
            }
        } catch (Exception e) {
            log.error("创建角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建角色失败: " + e.getMessage());
        }
    }

    @Override
    public Role updateRole(Role role) {
        try {
            // 检查角色是否存在
            Role existingRole = roleMapper.selectById(role.getId());
            if (existingRole == null) {
                throw new RuntimeException("角色不存在: " + role.getId());
            }

            // 检查角色名称是否与其他角色冲突
            Role sameNameRole = roleMapper.selectByName(role.getName());
            if (sameNameRole != null && !sameNameRole.getId().equals(role.getId())) {
                throw new RuntimeException("角色名称已存在: " + role.getName());
            }

            // 系统角色不能修改类型
            if (existingRole.getType() == 0 && role.getType() != 0) {
                throw new RuntimeException("系统角色不能修改类型");
            }

            // 更新角色
            role.setUpdatedAt(LocalDateTime.now());
            int result = roleMapper.updateById(role);
            if (result > 0) {
                log.info("更新角色成功: {}", role.getName());
                return role;
            } else {
                throw new RuntimeException("更新角色失败");
            }
        } catch (Exception e) {
            log.error("更新角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteRole(Long roleId) {
        try {
            // 检查角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new RuntimeException("角色不存在: " + roleId);
            }

            // 系统角色不能删除
            if (role.getType() == 0) {
                throw new RuntimeException("系统角色不能删除: " + role.getName());
            }

            // 软删除：设置状态为禁用
            role.setStatus(0);
            role.setUpdatedAt(LocalDateTime.now());
            int result = roleMapper.updateById(role);

            if (result > 0) {
                log.info("删除角色成功: {}", roleId);
                return true;
            } else {
                throw new RuntimeException("删除角色失败");
            }
        } catch (Exception e) {
            log.error("删除角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除角色失败: " + e.getMessage());
        }
    }

    @Override
    public Role getRoleById(Long roleId) {
        try {
            Role role = roleMapper.selectRoleWithPermissions(roleId);
            if (role == null || role.getStatus() == 0) {
                throw new RuntimeException("角色不存在或已禁用: " + roleId);
            }

            // 加载权限列表
            List<Long> permissionIds = roleMapper.selectPermissionIdsByRoleId(roleId);
            if (permissionIds != null && !permissionIds.isEmpty()) {
                List<Permission> permissions = new ArrayList<>();
                for (Long permissionId : permissionIds) {
                    Permission permission = permissionMapper.selectById(permissionId);
                    if (permission != null && permission.getStatus() == 1) {
                        permissions.add(permission);
                    }
                }
                role.setPermissions(permissions);
            }

            return role;
        } catch (Exception e) {
            log.error("获取角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色失败: " + e.getMessage());
        }
    }

    @Override
    public Role getRoleByName(String name) {
        try {
            Role role = roleMapper.selectByName(name);
            if (role == null) {
                throw new RuntimeException("角色不存在: " + name);
            }

            // 加载权限列表
            List<Long> permissionIds = roleMapper.selectPermissionIdsByRoleId(role.getId());
            if (permissionIds != null && !permissionIds.isEmpty()) {
                List<Permission> permissions = new ArrayList<>();
                for (Long permissionId : permissionIds) {
                    Permission permission = permissionMapper.selectById(permissionId);
                    if (permission != null && permission.getStatus() == 1) {
                        permissions.add(permission);
                    }
                }
                role.setPermissions(permissions);
            }

            return role;
        } catch (Exception e) {
            log.error("获取角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色失败: " + e.getMessage());
        }
    }

    @Override
    public List<Role> getAllRoles() {
        try {
            List<Role> roles = roleMapper.selectRolesWithPaging(null, 0, Integer.MAX_VALUE);

            // 为每个角色加载权限
            for (Role role : roles) {
                List<Long> permissionIds = roleMapper.selectPermissionIdsByRoleId(role.getId());
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    List<Permission> permissions = new ArrayList<>();
                    for (Long permissionId : permissionIds) {
                        Permission permission = permissionMapper.selectById(permissionId);
                        if (permission != null && permission.getStatus() == 1) {
                            permissions.add(permission);
                        }
                    }
                    role.setPermissions(permissions);
                }
            }

            return roles;
        } catch (Exception e) {
            log.error("获取所有角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取所有角色失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRolesWithPaging(int page, int pageSize, String keyword) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询角色列表
            List<Role> roles = roleMapper.selectRolesWithPaging(keyword, offset, pageSize);

            // 为每个角色加载权限
            for (Role role : roles) {
                List<Long> permissionIds = roleMapper.selectPermissionIdsByRoleId(role.getId());
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    List<Permission> permissions = new ArrayList<>();
                    for (Long permissionId : permissionIds) {
                        Permission permission = permissionMapper.selectById(permissionId);
                        if (permission != null && permission.getStatus() == 1) {
                            permissions.add(permission);
                        }
                    }
                    role.setPermissions(permissions);
                }
            }

            // 查询总数量
            int total = roleMapper.countRoles(keyword);

            result.put("roles", roles);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            log.error("分页查询角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("分页查询角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            return roleMapper.existsByName(name);
        } catch (Exception e) {
            log.error("检查角色名称是否存在失败: {}", e.getMessage(), e);
            throw new RuntimeException("检查角色名称是否存在失败: " + e.getMessage());
        }
    }

    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        try {
            // 检查角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null || role.getStatus() == 0) {
                throw new RuntimeException("角色不存在或已禁用: " + roleId);
            }

            // 检查权限是否存在
            for (Long permissionId : permissionIds) {
                Permission permission = permissionMapper.selectById(permissionId);
                if (permission == null || permission.getStatus() == 0) {
                    throw new RuntimeException("权限不存在或已禁用: " + permissionId);
                }
            }

            // 分配权限
            int result = roleMapper.assignPermissionsToRole(roleId, permissionIds);
            if (result > 0) {
                log.info("为角色分配权限成功: roleId={}, permissionCount={}", roleId, permissionIds.size());
                return true;
            } else {
                throw new RuntimeException("分配权限失败");
            }
        } catch (Exception e) {
            log.error("分配权限失败: {}", e.getMessage(), e);
            throw new RuntimeException("分配权限失败: " + e.getMessage());
        }
    }

    @Override
    public boolean removePermissionsFromRole(Long roleId, List<Long> permissionIds) {
        try {
            // 检查角色是否存在
            Role role = roleMapper.selectById(roleId);
            if (role == null || role.getStatus() == 0) {
                throw new RuntimeException("角色不存在或已禁用: " + roleId);
            }

            // 移除权限
            int result = roleMapper.removePermissionsFromRole(roleId, permissionIds);
            if (result > 0) {
                log.info("从角色移除权限成功: roleId={}, permissionCount={}", roleId, permissionIds.size());
                return true;
            } else {
                throw new RuntimeException("移除权限失败");
            }
        } catch (Exception e) {
            log.error("移除权限失败: {}", e.getMessage(), e);
            throw new RuntimeException("移除权限失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> getRolePermissions(Long roleId) {
        try {
            return roleMapper.selectPermissionNamesByRoleId(roleId);
        } catch (Exception e) {
            log.error("获取角色权限失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色权限失败: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getRolePermissionNames(Long roleId) {
        try {
            List<String> permissionNames = roleMapper.selectPermissionNamesByRoleId(roleId);
            return new HashSet<>(permissionNames);
        } catch (Exception e) {
            log.error("获取角色权限名称失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取角色权限名称失败: " + e.getMessage());
        }
    }

    @Override
    public boolean copyRolePermissions(Long sourceRoleId, Long targetRoleId) {
        try {
            // 检查源角色和目标角色是否存在
            Role sourceRole = roleMapper.selectById(sourceRoleId);
            Role targetRole = roleMapper.selectById(targetRoleId);
            if (sourceRole == null || sourceRole.getStatus() == 0) {
                throw new RuntimeException("源角色不存在或已禁用: " + sourceRoleId);
            }
            if (targetRole == null || targetRole.getStatus() == 0) {
                throw new RuntimeException("目标角色不存在或已禁用: " + targetRoleId);
            }

            // 清空目标角色的现有权限
            roleMapper.clearRolePermissions(targetRoleId);

            // 复制权限
            int result = roleMapper.copyRolePermissions(sourceRoleId, targetRoleId);
            if (result > 0) {
                log.info("复制角色权限成功: sourceRoleId={}, targetRoleId={}, permissionCount={}",
                        sourceRoleId, targetRoleId, result);
                return true;
            } else {
                throw new RuntimeException("复制权限失败");
            }
        } catch (Exception e) {
            log.error("复制角色权限失败: {}", e.getMessage(), e);
            throw new RuntimeException("复制角色权限失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> initializeSystemRoles() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 创建默认角色
            List<Role> systemRoles = createDefaultRoles();
            int createdCount = 0;

            for (Role role : systemRoles) {
                try {
                    // 检查角色是否已存在
                    if (!roleMapper.existsByName(role.getName())) {
                        int insertResult = roleMapper.insert(role);
                        if (insertResult > 0) {
                            createdCount++;
                            log.info("创建系统角色成功: {}", role.getName());
                        }
                    }
                } catch (Exception e) {
                    log.warn("创建系统角色失败: {}, {}", role.getName(), e.getMessage());
                }
            }

            result.put("success", true);
            result.put("message", "系统角色初始化成功");
            result.put("createdCount", createdCount);
            result.put("totalCount", systemRoles.size());

            log.info("系统角色初始化完成: {} 个角色", createdCount);
            return result;
        } catch (Exception e) {
            log.error("系统角色初始化失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "系统角色初始化失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<Role> createDefaultRoles() {
        List<Role> defaultRoles = new ArrayList<>();

        // 超级管理员角色
        Role superAdmin = new Role("超级管理员", "系统最高权限管理员");
        superAdmin.setType(0);
        superAdmin.setLevel(1);
        superAdmin.setIsDefault(false);
        defaultRoles.add(superAdmin);

        // 管理员角色
        Role admin = new Role("管理员", "系统管理员");
        admin.setType(0);
        admin.setLevel(10);
        admin.setIsDefault(false);
        defaultRoles.add(admin);

        // 版主角色
        Role moderator = new Role("版主", "内容版主");
        moderator.setType(0);
        moderator.setLevel(50);
        moderator.setIsDefault(false);
        defaultRoles.add(moderator);

        // 普通用户角色
        Role user = new Role("用户", "普通用户");
        user.setType(0);
        user.setLevel(100);
        user.setIsDefault(true);
        defaultRoles.add(user);

        return defaultRoles;
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        try {
            return roleMapper.selectRolesByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户角色失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isSystemRole(Long roleId) {
        try {
            return roleMapper.isSystemRole(roleId);
        } catch (Exception e) {
            log.error("检查系统角色失败: {}", e.getMessage(), e);
            throw new RuntimeException("检查系统角色失败: " + e.getMessage());
        }
    }
}