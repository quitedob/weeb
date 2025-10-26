# Weeb项目整治工作清单
---

### 2. 修复用户注册事务完整性 [HIGH] ✅
**文件**: `backend/src/main/java/com/web/service/Impl/AuthServiceImpl.java`
- [x] 在 `register()` 方法上添加 `@Transactional` 注解 (已存在于类级别)
- [x] 移除默认角色分配的 try-catch，让异常向上抛出
- [x] 如果角色分配失败，整个事务回滚，返回 500 错误
- [ ] 添加单元测试验证事务回滚行为

---

## 🟠 高优先级 (本周完成 - 架构性问题)

### 4. 统一聊天API架构 [HIGH]
**目标**: 废弃三套冗余API，创建统一接口

#### 4.1 创建统一服务层
- [ ] 创建 `UnifiedChatService.java` 接口
- [ ] 创建 `UnifiedChatServiceImpl.java` 实现
- [ ] 整合 `ChatService`、`MessageService`、`MessageThreadService` 的核心逻辑

#### 4.2 创建统一控制器
**文件**: `backend/src/main/java/com/web/controller/v2/UnifiedChatController.java`
```
新API设计:
GET    /api/v2/chats                    - 获取会话列表
POST   /api/v2/chats                    - 创建会话
GET    /api/v2/chats/{chatId}           - 获取会话详情
GET    /api/v2/chats/{chatId}/messages  - 获取消息历史(分页)
POST   /api/v2/chats/{chatId}/messages  - 发送消息
DELETE /api/v2/messages/{messageId}     - 撤回消息
GET    /api/v2/messages/{messageId}/thread - 获取消息线索
```

#### 4.3 标记旧API为废弃
- [ ] 在 `ChatController` 类上添加 `@Deprecated` 注解
- [ ] 在 `MessageController` 类上添加 `@Deprecated` 注解
- [ ] 在 `MessageThreadController` 类上添加 `@Deprecated` 注解
- [ ] 在每个方法中添加日志记录调用情况

#### 4.4 更新前端
- [ ] 更新 `frontend/src/stores/chatStore.js` 使用新API
- [ ] 更新 `frontend/src/views/ChatPage.vue`
- [ ] 测试所有聊天功能正常工作

#### 4.5 清理旧代码 (在前端完全迁移后)
- [ ] 删除 `ChatController.java`
- [ ] 删除 `MessageController.java`
- [ ] 删除 `MessageThreadController.java`
- [ ] 删除对应的 Service 实现

**验收标准**: 只有一套聊天API，前端功能完全正常

---

### 5. 统一用户信息API [HIGH]
**目标**: 解决用户信息获取端点冗余问题

#### 5.1 创建统一用户信息端点
**文件**: `backend/src/main/java/com/web/controller/v2/UserController.java`
- [ ] 创建 `GET /api/v2/users/me` 端点
- [ ] 返回完整用户信息（基本信息 + 统计数据 + 等级信息）
- [ ] 创建 `UserProfileVO` 包含所有必要字段
- [ ] 在后端计算 `registrationDays` 等业务逻辑

#### 5.2 废弃跨域API
- [ ] 标记 `ArticleCenterController.getUserInformByUsername()` 为 `@Deprecated`
- [ ] 在 `UserController` 中创建 `GET /api/v2/users/{userId}/profile` 替代

#### 5.3 更新前端
**文件**: `frontend/src/views/usermain.vue`
- [ ] 移除对 `/articles/userinform-by-username` 的调用
- [ ] 改为调用 `/api/v2/users/me`
- [ ] 移除前端的 `registrationDays` 计算逻辑

**验收标准**: 用户信息通过单次API调用获取，无跨域调用

---

### 6. 废弃UserLevel权限系统 [HIGH]
**目标**: 只保留RBAC权限模型

#### 6.1 数据迁移
- [ ] 创建迁移脚本 `MigrateUserLevelToRoles.java`
- [ ] 为每个UserLevel创建对应的Role（如"等级2用户"、"VIP用户"）
- [ ] 将UserLevel的权限写入 `role_permission` 表
- [ ] 为所有用户分配对应等级的角色

#### 6.2 代码重构
**文件**: `backend/src/main/java/com/web/model/UserLevel.java`
- [ ] 删除 `getLevelPermissions()` 方法
- [ ] 保留等级名称、颜色、升级条件等非权限属性

**文件**: `backend/src/main/java/com/web/service/Impl/RolePermissionServiceImpl.java`
- [ ] 移除对 `userLevelService.getUserPermissions()` 的调用
- [ ] 只从RBAC系统获取权限

#### 6.3 更新管理后台
- [ ] 在权限管理界面显示等级对应的角色
- [ ] 允许管理员修改等级角色的权限

**验收标准**: 权限只从RBAC系统获取，UserLevel不再影响权限

---

## 🟡 中优先级 (本月完成 - 质量改进)

### 7. 清理代码质量问题 [MEDIUM]
**目标**: 解决582个linter警告

#### 7.1 自动清理
- [ ] 运行IDE的"优化导入"功能，移除未使用的导入
- [ ] 使用IDE的"删除未使用代码"功能
- [ ] 配置 `.editorconfig` 统一代码格式

#### 7.2 手动修复
- [ ] 修复 `PasswordResetService` 接口与实现不匹配
- [ ] 检查所有Service接口，确保与实现类签名一致
- [ ] 修复潜在的循环依赖问题

#### 7.3 建立质量门禁
- [ ] 在 `pom.xml` 中配置 Checkstyle 插件
- [ ] 配置 Maven 构建失败如果有警告
- [ ] 添加 pre-commit hook 运行代码检查

**验收标准**: 0个linter警告，CI/CD包含代码质量检查

---

### 8. 实现AI服务真实功能 [MEDIUM]
**文件**: `backend/src/main/java/com/web/service/Impl/AIServiceImpl.java`

#### 8.1 集成真实AI服务
- [ ] 选择AI服务提供商（OpenAI、通义千问、文心一言等）
- [ ] 添加API密钥配置到 `application.yml`
- [ ] 实现 `generateArticleSummary()` - 调用真实API
- [ ] 实现 `chatWithAI()` - 调用真实API
- [ ] 实现 `analyzeArticleSentiment()` - 调用真实API

#### 8.2 添加功能开关
- [ ] 添加配置 `weeb.ai.enabled=true/false`
- [ ] 当AI服务不可用时，返回明确的错误信息（不返回假数据）
- [ ] 添加降级策略和错误处理

#### 8.3 补充缺失权限
**文件**: `backend/src/main/java/com/web/constants/Permissions.java`
- [ ] 添加 `AI_TRANSLATE_OWN` 权限常量
- [ ] 添加其他缺失的AI相关权限
- [ ] 在 `SystemSecurityInitializer` 中初始化这些权限

**验收标准**: AI功能调用真实服务或返回明确错误，不返回假数据

---

### 9. 统一搜索服务架构 [MEDIUM]
**目标**: 所有搜索使用Elasticsearch

#### 9.1 创建统一搜索服务
- [ ] 创建 `UnifiedSearchService.java` 接口
- [ ] 创建 `UnifiedSearchServiceImpl.java` 实现
- [ ] 创建 `SearchController.java` 统一入口

#### 9.2 为所有实体创建ES文档
- [ ] 创建 `UserDocument.java` (Elasticsearch实体)
- [ ] 创建 `ArticleDocument.java`
- [ ] 创建 `GroupDocument.java`
- [ ] 配置索引映射和分词器

#### 9.3 实现数据同步
- [ ] 在 `UserService` 中添加ES索引更新逻辑
- [ ] 在 `ArticleService` 中添加ES索引更新逻辑
- [ ] 在 `GroupService` 中添加ES索引更新逻辑
- [ ] 考虑使用异步事件或消息队列

#### 9.4 重构搜索方法
- [ ] 移除 `UserMapper.searchUsers()` 中的LIKE查询
- [ ] 移除 `ArticleMapper` 中的LIKE查询
- [ ] 改为调用 `UnifiedSearchService`

#### 9.5 配置优化
- [ ] 在 `application-dev.yml` 中设置 `spring.elasticsearch.enabled=true`
- [ ] 在 README.md 中说明生产环境必须启用ES

**验收标准**: 所有搜索通过Elasticsearch，无数据库LIKE查询

---

### 10. 前端架构优化 [MEDIUM]

#### 10.1 修复跨域API调用
- [ ] 审计所有 `.vue` 文件中的API调用
- [ ] 创建API调用规范文档
- [ ] 确保每个组件只调用对应领域的API

#### 10.2 增强认证状态管理
**文件**: `frontend/src/stores/authStore.js`
- [ ] 实现JWT自动刷新机制
- [ ] 添加刷新失败后的强制登出
- [ ] 使用 `BroadcastChannel` API 实现跨标签页同步
- [ ] 监听 `storage` 事件同步登出状态

#### 10.3 优化请求重试策略
**文件**: `frontend/src/api/axiosInstance.js`
- [ ] 只对瞬态错误重试（408, 502, 503, 504, ECONNABORTED）
- [ ] 4xx错误立即失败，不重试
- [ ] 500错误立即失败，不重试
- [ ] 添加重试次数限制（最多3次）

#### 10.4 前后端验证同步
**文件**: `frontend/src/views/Register.vue`
- [ ] 从后端获取验证规则配置（或硬编码同步）
- [ ] 实现实时验证反馈
- [ ] 确保前端验证规则与 `SecurityConstants.java` 一致

**验收标准**: 前端无跨域调用，认证状态健壮，请求重试合理

---

## 🟢 低优先级 (下月完成 - 长期改进)

### 11. 引入数据库迁移工具 [LOW]
**目标**: 使用Flyway管理数据库版本

#### 11.1 集成Flyway
- [ ] 在 `pom.xml` 中添加 Flyway 依赖
- [ ] 配置 `application.yml` 中的Flyway设置
- [ ] 创建 `src/main/resources/db/migration` 目录

#### 11.2 创建基线迁移
- [ ] 创建 `V1__Initial_Schema.sql` - 包含所有表结构
- [ ] 创建 `V2__Initial_Data.sql` - 包含初始权限和角色数据
- [ ] 测试在空数据库上执行迁移

#### 11.3 重构DatabaseInitializer
- [ ] 移除所有表创建逻辑
- [ ] 只保留数据库创建逻辑（如果不存在）
- [ ] 依赖Flyway管理表结构

**验收标准**: 数据库变更通过版本化SQL脚本管理

---

### 12. 合并社交关系模型 [LOW]
**目标**: 统一为"关注"模型

#### 12.1 数据迁移
- [ ] 将 `contact` 表中的ACCEPTED关系迁移到 `user_follow` 表
- [ ] 为双向好友创建两条关注记录

#### 12.2 废弃联系人系统
- [ ] 标记 `ContactController` 为 `@Deprecated`
- [ ] 标记 `ContactService` 为 `@Deprecated`

#### 12.3 更新前端
- [ ] 统一为"关注"/"取消关注"交互
- [ ] 在应用层判断"互相关注"显示为"好友"

#### 12.4 清理旧代码
- [ ] 删除 `ContactController`
- [ ] 删除 `ContactService` 和实现
- [ ] 删除 `contact` 表（在确认迁移成功后）

**验收标准**: 只有关注系统，好友通过互相关注判断

---

### 13. 实现数据一致性校准任务 [LOW]
**目标**: 定期修正反规范化数据

#### 13.1 创建校准任务
**文件**: `backend/src/main/java/com/web/task/DataConsistencyTask.java`
- [ ] 创建定时任务类，使用 `@Scheduled`
- [ ] 实现 `calibrateFansCount()` 方法
- [ ] 实现 `calibrateLikesCount()` 方法
- [ ] 实现 `calibrateFavoritesCount()` 方法

#### 13.2 配置调度
- [ ] 设置每天凌晨3点执行
- [ ] 添加任务执行日志
- [ ] 添加异常通知机制

**验收标准**: 定时任务自动修正数据不一致

---

## 📋 文档更新任务

### 14. 更新项目文档 [ONGOING]
- [ ] 更新 `README.md` - 移除DatabaseInitializer警告
- [ ] 更新 `backend.md` - 同步新的API设计
- [ ] 创建 `API_MIGRATION_GUIDE.md` - 旧API到新API的迁移指南
- [ ] 更新 `rule.txt` - 添加新的开发规范
- [ ] 创建 `ARCHITECTURE.md` - 记录架构决策

---

## 🧪 测试任务

### 15. 补充测试覆盖 [ONGOING]
- [ ] 为 `UnifiedChatService` 编写单元测试
- [ ] 为权限系统编写集成测试
- [ ] 为用户注册事务编写测试
- [ ] 为API端点编写E2E测试
- [ ] 目标：测试覆盖率达到80%

---

## 📊 进度跟踪

### 完成情况统计
- 🔴 紧急修复: 1/3 (33%)
- 🟠 高优先级: 0/6 (0%)
- 🟡 中优先级: 0/4 (0%)
- 🟢 低优先级: 0/3 (0%)
- 📋 文档更新: 0/1 (0%)
- 🧪 测试任务: 0/1 (0%)

**总体进度: 1/18 (6%)**

---

## 🎯 里程碑

### 第一阶段 (Week 1-2): 紧急修复
- 完成任务 1-3
- 系统稳定性得到保障

### 第二阶段 (Week 3-6): 架构重构
- 完成任务 4-6
- 核心架构问题解决

### 第三阶段 (Week 7-10): 质量提升
- 完成任务 7-10
- 代码质量和用户体验改善

### 第四阶段 (Week 11-12): 长期优化
- 完成任务 11-13
- 建立可持续发展基础

---

## ⚠️ 注意事项

1. **每个任务完成后必须**:
   - 运行完整测试套件
   - 更新相关文档
   - 提交代码并标注任务编号
   - 在本文档中标记为完成

2. **代码审查要求**:
   - 所有架构性变更需要团队评审
   - 权限相关变更需要安全评审
   - API变更需要前后端协调

3. **回滚计划**:
   - 每个重大变更前创建Git分支
   - 保留旧代码至少一个版本周期
   - 准备数据库回滚脚本

4. **沟通机制**:
   - 每周同步进度
   - 遇到阻塞问题立即上报
   - 架构决策需要文档记录

---

## 📞 负责人分配

- **后端架构师**: 任务 4, 5, 6, 9, 11
- **后端开发团队**: 任务 1, 2, 3, 7, 8, 13
- **前端团队**: 任务 10, 前端部分的任务 4, 5
- **DBA**: 任务 11, 数据迁移部分
- **测试团队**: 任务 15
- **技术文档**: 任务 14

---

**最后更新**: 2025-10-26
**文档版本**: 1.0
