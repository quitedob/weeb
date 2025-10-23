## Access Denied 问题修复验证

### 问题分析
用户注册成功后，登录成功但访问 `/api/users/me` 时遇到 "Access Denied" 错误。

**根本原因**：新注册的用户没有被分配任何角色权限，导致无法访问需要认证的接口。

### 修复方案
在 `AuthServiceImpl.register()` 方法中添加了默认角色分配逻辑：

1. **依赖注入**：添加了 `UserRoleMapper` 和 `RoleMapper` 的依赖
2. **角色分配逻辑**：
   - 首先查找标记为 `is_default = true` 的角色
   - 如果没找到，查找名为"用户"的角色
   - 如果都没找到，记录警告但不影响注册流程

### 关键代码变更
```java
// 为新用户分配默认角色
try {
    // 查找默认角色（is_default = true）
    Role defaultRole = roleMapper.selectDefaultRole();
    if (defaultRole != null) {
        userRoleMapper.assignRoleToUser(user.getId(), defaultRole.getId());
        log.info("为用户分配默认角色成功: userId={}, roleId={}, roleName={}",
                user.getId(), defaultRole.getId(), defaultRole.getName());
    } else {
        // 如果没有找到默认角色，尝试查找"用户"角色
        Role userRole = roleMapper.selectByName("用户");
        if (userRole != null) {
            userRoleMapper.assignRoleToUser(user.getId(), userRole.getId());
            log.info("为用户分配'用户'角色成功: userId={}, roleId={}",
                    user.getId(), userRole.getId());
        } else {
            // 如果还是没有找到，记录警告但不影响注册流程
            log.warn("未找到默认角色或'用户'角色，新用户注册后无角色权限: userId={}", user.getId());
        }
    }
} catch (Exception e) {
    // 角色分配失败不应该阻断用户注册，只记录错误
    log.error("为新用户分配角色失败: userId={}, error={}", user.getId(), e.getMessage(), e);
}
```

### 测试步骤
1. 重新启动应用
2. 注册一个新用户（例如：testuser2）
3. 使用新用户登录
4. 尝试访问 `/api/users/me` 接口

### 预期结果
- 新用户注册成功后自动获得默认角色
- 登录后可以正常访问 `/api/users/me` 接口
- 不再出现 "Access Denied" 错误

### 备用机制
如果角色分配失败，系统还有备用机制：
- `CustomUserDetailsService` 中会为没有特定权限的用户默认分配 `ROLE_USER` 权限
- 确保即使角色分配失败，用户也能有基本的访问权限

### 数据库要求
确保数据库中存在以下角色之一：
1. 标记为 `is_default = true` 的角色
2. 或者名为"用户"的角色

如果没有这些角色，请联系管理员先创建相应的角色数据。