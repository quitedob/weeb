# JWT认证与文章API问题修复指南

## 问题分析与修复方案

基于对您项目的深入分析，我发现了JWT认证流程中的几个关键问题点，并提供了相应的修复方案。

## 一、发现的主要问题

### 1. 数据访问层问题
**问题**: `CustomUserDetailsService:37` 使用了 `authMapper.findByUsername()`，但可能存在以下风险：
- `AuthMapper` 和 `UserMapper` 可能有不同的数据源或查询逻辑
- 可能存在数据不一致的问题

**验证SQL**:
```sql
-- 检查用户是否存在且状态正常
SELECT id, username, password, status
FROM `user`
WHERE username = 'your_username' AND status = 1;
```

### 2. JWT过滤器实现问题
**问题**: `JwtAuthenticationFilter:63` 使用了 `jwtUtil.isTokenValid(jwt, userDetails.getUsername())` 但这个方法可能存在逻辑问题：
- `JwtUtil.isTokenValid()` 方法会调用 `extractUsername()` 来获取token中的用户名
- 然后与 `userDetails.getUsername()` 进行比较
- 但如果token解析失败或用户名不匹配，会导致认证失败

### 3. 密码编码问题
**当前配置**: 使用 `BCryptPasswordEncoder` ✅ 正确
**验证**: 确保数据库中的密码是以 `$2a$` 开头的BCrypt哈希值

**验证SQL**:
```sql
SELECT username, password,
       CASE
           WHEN password LIKE '$2a$%' THEN 'BCrypt加密'
           ELSE '密码未正确加密'
       END as password_status
FROM `user`;
```

## 二、修复方案

### 修复1: 统一数据访问层
**文件**: `src/main/java/com/web/security/CustomUserDetailsService.java`

**建议修改**:
```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // 移除AuthMapper，统一使用UserMapper
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);

            // 使用统一的UserMapper查询用户
            User user = userMapper.selectByUsername(username);
            if (user == null) {
                log.warn("User not found: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            // 其余逻辑保持不变...
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("加载用户信息失败: " + username, e);
        }
    }
}
```

### 修复2: 优化JWT过滤器逻辑
**文件**: `src/main/java/com/web/security/JwtAuthenticationFilter.java`

**建议修改**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    final String jwt;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        jwt = authHeader.substring(7);

        // 首先验证token是否有效（不依赖用户名）
        if (!jwtUtil.validateToken(jwt)) {
            log.warn("Invalid JWT token");
            SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Invalid token");
            handleAuthenticationException(response, "无效的令牌", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 从token中提取用户ID
        final Long userId = jwtUtil.getUserIdFromToken(jwt);

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.customUserDetailsService.loadUserById(userId);

            // 再次验证token和用户的匹配性
            String tokenUsername = jwtUtil.extractUsername(jwt);
            if (tokenUsername != null && tokenUsername.equals(userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                SecurityAuditUtils.logAuthenticationSuccess(userDetails.getUsername(), request.getRemoteAddr());
            } else {
                log.warn("Token username mismatch for user: {}", userDetails.getUsername());
                SecurityAuditUtils.logAuthenticationFailure(userDetails.getUsername(), request.getRemoteAddr(), "Username mismatch");
            }
        }
    } catch (ExpiredJwtException e) {
        log.warn("JWT token expired: {}", e.getMessage());
        handleAuthenticationException(response, "令牌已过期，请重新登录", HttpServletResponse.SC_UNAUTHORIZED);
        return;
    } catch (Exception e) {
        log.error("Error processing JWT token", e);
        SecurityAuditUtils.logAuthenticationFailure("unknown", request.getRemoteAddr(), "Processing error");
        handleAuthenticationException(response, "令牌处理失败", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
    }

    filterChain.doFilter(request, response);
}
```

### 修复3: 数据库数据一致性检查
**执行以下SQL验证数据**:

```sql
-- 1. 检查用户密码是否已正确加密
SELECT id, username, password, status,
       CASE
           WHEN password LIKE '$2a$%' OR password LIKE '$2b$%' THEN 'BCrypt加密✓'
           ELSE '密码未加密✗'
       END as password_status
FROM `user`
WHERE status = 1;

-- 2. 检查用户权限是否正确配置
SELECT u.username, p.name as permission_name, p.resource, p.action, p.condition
FROM `user` u
INNER JOIN user_role ur ON u.id = ur.user_id
INNER JOIN role_permission rp ON ur.role_id = rp.role_id
INNER JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'your_test_username' AND p.status = 1;

-- 3. 检查文章所有者关系
SELECT a.id as article_id, a.article_title, a.user_id, u.username as owner_name
FROM articles a
LEFT JOIN `user` u ON a.user_id = u.id
WHERE a.id = your_article_id;
```

## 三、文章API权限问题修复

### 1. 权限评估器问题
**文件**: `src/main/java/com/web/security/CustomPermissionEvaluator.java`

**问题**: 权限检查可能失败，因为：
- `userService.findByUsername()` 可能返回null
- 权限数据可能不存在

**修复建议**:
```java
@Override
public boolean hasPermission(Authentication authentication, Object targetId, Object permission) {
    if (authentication == null || !authentication.isAuthenticated()) {
        return false;
    }

    try {
        String username = authentication.getName();
        log.debug("Checking permission {} for user {} on target {}", permission, username, targetId);

        User user = userService.findByUsername(username);
        if (user == null) {
            log.warn("User not found: {}", username);
            return false;
        }

        String permissionStr = permission.toString();

        // 对于文章操作，进行特殊处理
        if (permissionStr.startsWith("ARTICLE_")) {
            return checkArticlePermission(user, permissionStr, targetId);
        }

        return checkPermission(user, targetId, permissionStr);
    } catch (Exception e) {
        log.error("Error checking permission for user {} on target {}", authentication.getName(), targetId, e);
        return false;
    }
}

private boolean checkArticlePermission(User user, String permissionStr, Object targetId) {
    try {
        // 解析权限: ARTICLE_CREATE_OWN, ARTICLE_UPDATE_OWN, ARTICLE_DELETE_OWN
        if ("ARTICLE_CREATE_OWN".equals(permissionStr)) {
            return user.getStatus() == 1; // 只要用户正常就可创建文章
        }

        if (targetId != null && (permissionStr.contains("_UPDATE_") || permissionStr.contains("_DELETE_"))) {
            Long articleId = Long.valueOf(targetId.toString());
            // 检查是否是文章所有者
            return userService.isArticleOwner(user.getId(), articleId);
        }

        return false;
    } catch (Exception e) {
        log.error("Error checking article permission: {}", permissionStr, e);
        return false;
    }
}
```

### 2. 文章Controller修复
**文件**: `src/main/java/com/web/Controller/ArticleCenterController.java`

**确保安全验证**:
```java
@PostMapping("/new")
@PreAuthorize("hasPermission(null, 'ARTICLE_CREATE_OWN')")
public ResponseEntity<ApiResponse<Map<String, Object>>> createArticle(
        @RequestBody @Valid ArticleCreateVo createVo,
        @Userid Long authenticatedUserId) {
    try {
        // 确保使用认证用户ID，而不是请求中的ID
        Article article = new Article();
        article.setUserId(authenticatedUserId); // 强制使用认证用户ID
        article.setArticleTitle(createVo.getArticleTitle());
        article.setArticleContent(createVo.getArticleContent());

        // 验证用户状态
        User currentUser = userService.findById(authenticatedUserId);
        if (currentUser == null || currentUser.getStatus() != 1) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("用户状态异常，无法创建文章"));
        }

        int result = articleService.createArticle(article);
        // 其余逻辑...
    } catch (Exception e) {
        logger.error("创建文章失败", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.systemError("创建文章失败"));
    }
}
```

## 四、测试验证方案

### 1. 创建测试数据
```sql
-- 创建测试用户（密码123456的BCrypt加密）
INSERT INTO `user` (username, password, user_email, status, created_at, updated_at)
VALUES ('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'test@example.com', 1, NOW(), NOW());
```

### 2. 测试JWT认证流程
1. **登录测试**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "123456"}'
```

2. **JWT Token验证测试**:
```bash
# 使用返回的JWT token访问需要认证的接口
curl -X GET http://localhost:8080/api/articles/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

3. **文章创建测试**:
```bash
curl -X POST http://localhost:8080/api/articles/new \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"articleTitle": "测试文章", "articleContent": "这是测试内容", "categoryId": 1}'
```

## 五、常见问题排查

### 1. 认证失败
- 检查数据库用户密码是否为BCrypt加密格式
- 验证JWT secret配置是否正确
- 确认用户状态是否为启用(status=1)

### 2. 权限被拒绝
- 检查用户是否有所需权限
- 验证文章所有者关系
- 确认权限评估器逻辑正确

### 3. 数据库连接问题
- 检查Hikari连接池配置
- 验证数据库连接参数
- 确认表结构和字段匹配

## 六、监控与日志

建议添加以下监控点：
1. JWT token解析成功率
2. 用户认证失败次数
3. 权限检查耗时
4. 数据库查询性能

通过以上修复方案，应该能够解决JWT认证和文章API的主要问题。请根据实际情况调整具体实现细节。