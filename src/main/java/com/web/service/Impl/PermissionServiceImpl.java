package com.web.service.impl;

import com.web.exception.WeebException;
import com.web.mapper.PermissionMapper;
import com.web.model.Permission;
import com.web.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 权限服务实现类
 * 提供权限管理的业务逻辑实现
 */
@Slf4j
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public Permission createPermission(Permission permission) {
        try {
            // 检查权限名称是否已存在
            if (permissionMapper.existsByName(permission.getName())) {
                throw new WeebException("权限名称已存在: " + permission.getName());
            }

            // 设置默认值
            if (permission.getStatus() == null) {
                permission.setStatus(1);
            }
            if (permission.getType() == null) {
                permission.setType(1);
            }
            if (permission.getSortOrder() == null) {
                permission.setSortOrder(0);
            }
            permission.setCreatedAt(LocalDateTime.now());
            permission.setUpdatedAt(LocalDateTime.now());

            // 插入权限
            int result = permissionMapper.insert(permission);
            if (result > 0) {
                log.info("创建权限成功: {}", permission.getName());
                return permission;
            } else {
                throw new WeebException("创建权限失败");
            }
        } catch (Exception e) {
            log.error("创建权限失败: {}", e.getMessage(), e);
            throw new WeebException("创建权限失败: " + e.getMessage());
        }
    }

    @Override
    public Permission updatePermission(Permission permission) {
        try {
            // 检查权限是否存在
            Permission existingPermission = permissionMapper.selectById(permission.getId());
            if (existingPermission == null) {
                throw new WeebException("权限不存在: " + permission.getId());
            }

            // 检查权限名称是否与其他权限冲突
            Permission sameNamePermission = permissionMapper.selectByName(permission.getName());
            if (sameNamePermission != null && !sameNamePermission.getId().equals(permission.getId())) {
                throw new WeebException("权限名称已存在: " + permission.getName());
            }

            // 更新权限
            permission.setUpdatedAt(LocalDateTime.now());
            int result = permissionMapper.updateById(permission);
            if (result > 0) {
                log.info("更新权限成功: {}", permission.getName());
                return permission;
            } else {
                throw new WeebException("更新权限失败");
            }
        } catch (Exception e) {
            log.error("更新权限失败: {}", e.getMessage(), e);
            throw new WeebException("更新权限失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deletePermission(Long permissionId) {
        try {
            // 检查权限是否存在
            Permission permission = permissionMapper.selectById(permissionId);
            if (permission == null) {
                throw new WeebException("权限不存在: " + permissionId);
            }

            // 软删除：设置状态为禁用
            permission.setStatus(0);
            permission.setUpdatedAt(LocalDateTime.now());
            int result = permissionMapper.updateById(permission);

            if (result > 0) {
                log.info("删除权限成功: {}", permissionId);
                return true;
            } else {
                throw new WeebException("删除权限失败");
            }
        } catch (Exception e) {
            log.error("删除权限失败: {}", e.getMessage(), e);
            throw new WeebException("删除权限失败: " + e.getMessage());
        }
    }

    @Override
    public Permission getPermissionById(Long permissionId) {
        try {
            Permission permission = permissionMapper.selectById(permissionId);
            if (permission == null || permission.getStatus() == 0) {
                throw new WeebException("权限不存在或已禁用: " + permissionId);
            }
            return permission;
        } catch (Exception e) {
            log.error("获取权限失败: {}", e.getMessage(), e);
            throw new WeebException("获取权限失败: " + e.getMessage());
        }
    }

    @Override
    public Permission getPermissionByName(String name) {
        try {
            Permission permission = permissionMapper.selectByName(name);
            if (permission == null) {
                throw new WeebException("权限不存在: " + name);
            }
            return permission;
        } catch (Exception e) {
            log.error("获取权限失败: {}", e.getMessage(), e);
            throw new WeebException("获取权限失败: " + e.getMessage());
        }
    }

    @Override
    public List<Permission> getAllPermissions() {
        try {
            return permissionMapper.selectPermissionsWithPaging(null, 0, Integer.MAX_VALUE);
        } catch (Exception e) {
            log.error("获取所有权限失败: {}", e.getMessage(), e);
            throw new WeebException("获取所有权限失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPermissionsWithPaging(int page, int pageSize, String keyword, String resource, String status) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 计算偏移量
            int offset = (page - 1) * pageSize;

            // 查询权限列表（带过滤条件）
            List<Permission> permissions = permissionMapper.selectPermissionsWithFilters(keyword, resource, status, offset, pageSize);

            // 查询总数量
            int total = permissionMapper.countPermissionsWithFilters(keyword, resource, status);

            result.put("permissions", permissions);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));

            return result;
        } catch (Exception e) {
            log.error("分页查询权限失败: {}", e.getMessage(), e);
            throw new WeebException("分页查询权限失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getPermissionsWithPaging(int page, int pageSize, String keyword) {
        // 调用5参数版本的方法，默认不设置resource和status过滤
        return getPermissionsWithPaging(page, pageSize, keyword, null, null);
    }

    @Override
    public boolean existsByName(String name) {
        try {
            return permissionMapper.existsByName(name);
        } catch (Exception e) {
            log.error("检查权限名称是否存在失败: {}", e.getMessage(), e);
            throw new WeebException("检查权限名称是否存在失败: " + e.getMessage());
        }
    }

    @Override
    public Permission getPermissionByResourceAndAction(String resource, String action) {
        try {
            Permission permission = permissionMapper.selectByResourceAndAction(resource, action);
            if (permission == null) {
                throw new WeebException("权限不存在: " + resource + "." + action);
            }
            return permission;
        } catch (Exception e) {
            log.error("获取权限失败: {}", e.getMessage(), e);
            throw new WeebException("获取权限失败: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getPermissionGroups() {
        try {
            return permissionMapper.selectPermissionGroups();
        } catch (Exception e) {
            log.error("获取权限组失败: {}", e.getMessage(), e);
            throw new WeebException("获取权限组失败: " + e.getMessage());
        }
    }

    @Override
    public List<Permission> getPermissionsByGroup(String group) {
        try {
            return permissionMapper.selectByGroup(group);
        } catch (Exception e) {
            log.error("获取权限组权限失败: {}", e.getMessage(), e);
            throw new WeebException("获取权限组权限失败: " + e.getMessage());
        }
    }

    @Override
    public int batchCreatePermissions(List<Permission> permissions) {
        try {
            // 检查权限名称是否重复
            Set<String> permissionNames = new HashSet<>();
            for (Permission permission : permissions) {
                if (permissionNames.contains(permission.getName())) {
                    throw new WeebException("权限名称重复: " + permission.getName());
                }
                permissionNames.add(permission.getName());

                // 检查权限是否已存在
                if (permissionMapper.existsByName(permission.getName())) {
                    throw new WeebException("权限名称已存在: " + permission.getName());
                }

                // 设置默认值
                if (permission.getStatus() == null) {
                    permission.setStatus(1);
                }
                if (permission.getType() == null) {
                    permission.setType(1);
                }
                if (permission.getSortOrder() == null) {
                    permission.setSortOrder(0);
                }
                permission.setCreatedAt(LocalDateTime.now());
                permission.setUpdatedAt(LocalDateTime.now());
            }

            // 批量插入
            int result = permissionMapper.batchInsert(permissions);
            log.info("批量创建权限成功: {} 个权限", result);
            return result;
        } catch (Exception e) {
            log.error("批量创建权限失败: {}", e.getMessage(), e);
            throw new WeebException("批量创建权限失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> initializeSystemPermissions() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取所有预定义的系统权限
            List<Permission> systemPermissions = SystemPermissionInitializer.getSystemPermissions();

            // 批量创建权限
            int createdCount = batchCreatePermissions(systemPermissions);

            result.put("success", true);
            result.put("message", "系统权限初始化成功");
            result.put("createdCount", createdCount);
            result.put("totalCount", systemPermissions.size());

            log.info("系统权限初始化完成: {} 个权限", createdCount);
            return result;
        } catch (Exception e) {
            log.error("系统权限初始化失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "系统权限初始化失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 根据资源获取权限组
     */
    private String getPermissionGroup(String resource) {
        switch (resource) {
            case "user":
                return "用户管理";
            case "role":
                return "角色管理";
            case "permission":
                return "权限管理";
            case "article":
                return "内容管理";
            case "group":
                return "群组管理";
            case "system":
                return "系统管理";
            default:
                return "其他";
        }
    }

    /**
     * 生成权限描述
     */
    private String getPermissionDescription(Permission permission) {
        String resourceName = getResourceName(permission.getResource());
        String actionName = getActionName(permission.getAction());
        return "允许" + actionName + resourceName;
    }

    /**
     * 获取资源中文名称
     */
    private String getResourceName(String resource) {
        switch (resource) {
            case "user":
                return "用户";
            case "role":
                return "角色";
            case "permission":
                return "权限";
            case "article":
                return "文章";
            case "group":
                return "群组";
            case "system":
                return "系统";
            default:
                return resource;
        }
    }

    /**
     * 获取操作中文名称
     */
    private String getActionName(String action) {
        switch (action) {
            case "create":
                return "创建";
            case "read":
                return "查看";
            case "update":
                return "更新";
            case "delete":
                return "删除";
            default:
                return action;
        }
    }
}