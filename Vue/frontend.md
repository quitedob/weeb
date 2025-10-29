# 前端页面功能与设计文档

## 项目概述

这是一个基于Vue 3的前端社交平台项目，采用现代化的前端架构设计，提供聊天、联系人管理、文章发布、用户管理等完整功能。

### 技术栈
- **Vue 3**: 渐进式JavaScript框架
- **Vue Router 4**: 路由管理
- **Pinia**: 状态管理
- **Element Plus**: UI组件库
- **Axios**: HTTP客户端
- **Vite**: 构建工具

---

## 页面功能详解

### 1. 认证相关页面

#### 1.1 登录页面 (`/login`)
**文件路径**: `views/Login.vue`
**设计目的**:
- 提供用户身份验证入口
- 支持JWT令牌认证
- 记住登录状态和自动登录
- 友好的错误提示和验证反馈

**核心功能**:
- 用户名/密码登录
- 表单验证（必填、格式校验）
- 登录状态持久化
- 登录失败错误处理
- 跳转到注册页面链接

**对应后端API**:
- `POST /api/auth/login` - 用户登录
- `GET /api/users/me` - 获取用户信息（验证token）

**设计特点**:
- 简洁的登录表单
- 响应式布局适配移动端
- 加载状态指示器

#### 1.2 注册页面 (`/register`)
**文件路径**: `views/Register.vue`
**设计目的**:
- 新用户注册账号
- 收集必要用户信息
- 密码强度验证
- 防止恶意注册

**核心功能**:
- 用户名、邮箱、密码注册
- 密码确认验证
- 邮箱格式验证
- 注册协议同意
- 注册成功自动登录

**对应后端API**:
- `POST /api/auth/register` - 用户注册

**设计特点**:
- 分步骤注册流程
- 实时密码强度提示
- 邮箱验证状态显示

#### 1.3 忘记密码页面 (`/forget`)
**文件路径**: `views/Forget.vue`
**设计目的**:
- 帮助用户重置密码
- 通过邮箱验证身份
- 安全的密码重置流程

**核心功能**:
- 邮箱验证
- 发送重置邮件
- 新密码设置
- 重置链接有效性验证

**对应后端API**:
- `POST /api/auth/forgot-password` - 忘记密码
- `POST /api/auth/reset-password` - 重置密码

**设计特点**:
- 邮件发送状态反馈
- 安全的密码重置流程

### 2. 主功能页面

#### 2.1 聊天页面 (`/chat`)
**文件路径**: `views/chat/ChatPage.vue`
**设计目的**:
- 提供实时聊天功能
- 支持一对一和群组聊天
- 良好的用户体验和消息管理

**核心功能**:
- 聊天列表显示（私聊和群聊）
- 实时消息发送和接收
- 消息已读状态管理
- 消息搜索和历史记录
- 表情符号和文件发送
- 消息撤回功能
- 在线状态显示

**对应后端API**:
- `GET /api/chats` - 获取聊天列表
- `POST /api/chats` - 创建聊天会话
- `GET /api/chats/{chatId}/messages` - 获取聊天消息
- `POST /api/chats/{chatId}/messages` - 发送消息
- `POST /api/chats/{chatId}/read` - 标记已读
- `DELETE /api/chats/{chatId}` - 删除聊天
- `POST /api/chats/messages/{messageId}/react` - 添加反应

**设计特点**:
- 左右分栏布局（聊天列表+聊天窗口）
- 实时WebSocket消息推送
- 消息虚拟滚动（性能优化）
- 响应式设计适配移动端
- 消息时间分隔显示
- 未读消息数量提示

#### 2.2 联系人页面 (`/contact`)
**文件路径**: `contact/ContactPage.vue`
**设计目的**:
- 管理用户好友关系
- 处理好友申请
- 提供用户搜索和添加功能

**核心功能**:
- 好友列表展示
- 好友申请管理（接受/拒绝）
- 用户搜索和添加好友
- 联系人分组管理
- 好友删除和拉黑功能
- 好友状态显示

**对应后端API**:
- `GET /api/contacts` - 获取联系人列表
- `GET /api/contacts/requests` - 获取好友申请
- `POST /api/contacts/request` - 发送好友申请
- `POST /api/contacts/request/by-username` - 通过用户名发送申请
- `POST /api/contacts/accept/{contactId}` - 接受申请
- `POST /api/contacts/decline/{contactId}` - 拒绝申请
- `DELETE /api/contacts/{contactId}` - 删除联系人
- `POST /api/contacts/block/{contactId}` - 拉黑联系人
- `GET /api/contacts/groups` - 获取联系人分组
- `POST /api/contacts/groups` - 创建分组
- `DELETE /api/contacts/groups/{groupId}` - 删除分组

**设计特点**:
- 标签页切换（我的联系人/好友申请/查找用户）
- 卡片式联系人展示
- 搜索功能实时过滤
- 批量操作支持

### 3. 文章管理页面

#### 3.1 文章中心 (`/article`)
**文件路径**: `article/ArticleMain.vue`
**设计目的**:
- 展示文章列表和内容预览
- 提供文章搜索和筛选功能
- 用户文章发布入口

**核心功能**:
- 文章列表分页展示
- 文章搜索（标题、内容）
- 多种排序方式（时间、阅读量、点赞数）
- 文章标签分类
- 文章点赞和收藏
- 作者信息显示
- 文章统计信息

**对应后端API**:
- `GET /api/articles` - 获取文章列表
- `POST /api/articles/{id}/like` - 点赞文章
- `GET /api/articles/search` - 搜索文章
- `GET /api/articles/categories` - 获取分类
- `GET /api/articles/tags` - 获取标签

**设计特点**:
- 网格布局文章卡片
- 搜索和筛选组合
- 无限滚动加载更多
- 响应式卡片设计

#### 3.2 文章阅读 (`/article/read/:articleId`)
**文件路径**: `article/ArticleRead.vue`
**设计目的**:
- 提供完整的文章阅读体验
- 支持文章交互功能
- 展示文章评论和讨论

**核心功能**:
- 文章内容完整展示
- 阅读进度跟踪
- 文章点赞、收藏、分享
- 评论系统（发表、回复、点赞）
- 相关文章推荐
- 作者信息展示
- 阅读统计

**对应后端API**:
- `GET /api/articles/{id}` - 获取文章详情
- `POST /api/articles/{id}/like` - 点赞文章
- `POST /api/articles/{id}/collect` - 收藏文章
- `GET /api/articles/{id}/comments` - 获取评论
- `POST /api/articles/{id}/comments` - 发表评论

**设计特点**:
- 沉浸式阅读界面
- 文章目录导航
- 夜间阅读模式
- 字体大小调节

#### 3.3 文章发布 (`/article/write`)
**文件路径**: `article/ArticleWrite.vue`
**设计目的**:
- 提供富文本编辑器
- 支持多种媒体内容插入
- 文章发布和管理流程

**核心功能**:
- 富文本编辑器
- 图片、视频、链接插入
- 文章标签管理
- 草稿保存
- 发布设置（公开/私密）
- 文章预览

**对应后端API**:
- `POST /api/articles` - 发布文章
- `PUT /api/articles/{id}` - 更新文章
- `POST /api/upload/image` - 上传图片

**设计特点**:
- Markdown和富文本双模式
- 实时预览
- 自动保存草稿

#### 3.4 文章管理 (`/article/manage`)
**文件路径**: `article/ArticleManage.vue`
**设计目的**:
- 用户管理自己的文章
- 提供文章编辑、删除功能
- 文章统计数据展示

**核心功能**:
- 个人文章列表
- 文章状态管理（草稿/已发布）
- 批量操作（删除、分类）
- 文章统计数据
- 文章编辑入口

**对应后端API**:
- `GET /api/articles/user/{userId}/published` - 获取用户文章
- `DELETE /api/articles/{id}` - 删除文章
- `PUT /api/articles/{id}` - 更新文章

**设计特点**:
- 表格式管理界面
- 批量操作支持
- 状态筛选和搜索

### 4. 群组管理页面

#### 4.1 群组列表 (`/groups`)
**文件路径**: `views/Groups.vue`
**设计目的**:
- 展示用户加入的群组
- 提供群组搜索和加入功能

**核心功能**:
- 群组列表展示
- 群组搜索
- 加入群组申请
- 群组信息预览

**对应后端API**:
- `GET /api/groups/my-groups` - 获取我的群组
- `GET /api/groups/search` - 搜索群组
- `POST /api/groups/{groupId}/applications` - 申请加入群组

**设计特点**:
- 卡片式群组展示
- 搜索和分类筛选

#### 4.2 群组管理 (`/groups/manage`)
**文件路径**: `group/GroupPage.vue`
**设计目的**:
- 用户管理自己的群组
- 群组创建、编辑、成员管理

**核心功能**:
- 创建新群组
- 管理现有群组
- 群组成员管理
- 群组设置修改

**对应后端API**:
- `GET /api/groups/my-created` - 获取创建的群组
- `POST /api/groups` - 创建群组
- `PUT /api/groups/{groupId}` - 更新群组
- `DELETE /api/groups/{groupId}` - 删除群组
- `GET /api/groups/{groupId}/members` - 获取群组成员

**设计特点**:
- 群组操作面板
- 成员管理界面

#### 4.3 群组详情 (`/group/:groupId`)
**文件路径**: `views/group/GroupDetail.vue`
**设计目的**:
- 展示群组详细信息
- 群组成员管理
- 群组聊天入口

**核心功能**:
- 群组信息展示
- 成员列表和管理
- 群组公告
- 加入/退出群组

**对应后端API**:
- `GET /api/groups/{groupId}` - 获取群组详情
- `GET /api/groups/{groupId}/members` - 获取成员列表
- `DELETE /api/groups/{groupId}/members/me` - 退出群组
- `POST /api/groups/{groupId}/members` - 邀请成员

**设计特点**:
- 详细的群组信息面板
- 成员权限管理

### 5. 用户管理页面

#### 5.1 个人资料 (`/profile`)
**文件路径**: `views/UserProfile.vue`
**设计目的**:
- 用户查看和编辑个人资料
- 展示用户统计信息
- 个性化设置

**核心功能**:
- 基本信息编辑
- 头像上传
- 个人简介设置
- 隐私设置
- 账号安全设置

**对应后端API**:
- `GET /api/users/me/profile` - 获取个人资料
- `PUT /api/users/me` - 更新资料
- `POST /api/users/avatar` - 上传头像
- `GET /api/users/me/stats` - 获取统计信息

**设计特点**:
- 标签页组织信息
- 实时预览修改效果

#### 5.2 用户详情 (`/user/:userId`)
**文件路径**: `views/UserDetail.vue`
**设计目的**:
- 查看其他用户信息
- 提供社交互动功能

**核心功能**:
- 用户信息展示
- 关注/取消关注
- 发送私信
- 查看用户动态

**对应后端API**:
- `GET /api/users/{userId}` - 获取用户信息
- `POST /api/users/{userId}/follow` - 关注用户
- `GET /api/users/{userId}/follow/status` - 获取关注状态
- `GET /api/users/{userId}/stats` - 获取用户统计

**设计特点**:
- 第三方视角的用户界面
- 社交互动按钮

#### 5.3 设置页面 (`/setting`)
**文件路径**: `views/Settings.vue`
**设计目的**:
- 集中管理用户设置
- 系统偏好配置
- 账号安全管理

**核心功能**:
- 通知设置
- 隐私设置
- 界面主题设置
- 语言设置
- 密码修改
- 账号注销

**对应后端API**:
- `GET /api/users/me` - 获取当前设置
- `PUT /api/users/me` - 更新设置
- `POST /api/auth/change-password` - 修改密码

**设计特点**:
- 分模块设置组织
- 设置生效状态反馈

### 6. 系统功能页面

#### 6.1 搜索页面 (`/search`)
**文件路径**: `search/SearchPage.vue`
**设计目的**:
- 全局搜索功能入口
- 搜索结果聚合展示

**核心功能**:
- 多类型搜索（用户、文章、群组、消息）
- 搜索建议和历史
- 结果筛选和排序
- 高级搜索选项

**对应后端API**:
- `GET /api/search/all` - 全局搜索
- `GET /api/search/messages` - 搜索消息
- `GET /api/search/users` - 搜索用户
- `GET /api/search/articles` - 搜索文章

**设计特点**:
- 搜索结果分类展示
- 搜索历史管理

#### 6.2 通知中心 (`/notifications`)
**文件路径**: `views/NotificationListPage.vue`
**设计目的**:
- 集中展示系统通知
- 用户消息管理

**核心功能**:
- 通知列表展示
- 通知标记已读
- 通知删除管理
- 通知类型筛选

**对应后端API**:
- `GET /api/notifications` - 获取通知列表
- `PUT /api/notifications/{id}/read` - 标记已读
- `DELETE /api/notifications/{id}` - 删除通知
- `PUT /api/notifications/read-all` - 全部标记已读

**设计特点**:
- 通知分类和状态标识
- 批量操作支持

#### 6.3 等级历史 (`/level-history`)
**文件路径**: `views/UserLevelHistory.vue`
**设计目的**:
- 展示用户等级变化历史
- 用户成长激励

**核心功能**:
- 等级历史时间线
- 积分获取记录
- 等级特权展示
- 升级进度显示

**对应后端API**:
- `GET /api/user-level-history/user/{userId}` - 获取等级历史
- `GET /api/user-levels/user/{userId}` - 获取当前等级
- `GET /api/user-levels/levels` - 获取等级定义

**设计特点**:
- 时间线可视化展示
- 进度条和成就徽章

### 7. 管理员页面

#### 7.1 角色权限管理 (`/admin/role-permission`)
**文件路径**: `views/admin/RolePermissionManagement.vue`
**设计目的**:
- 管理系统角色和权限
- 用户权限分配

**核心功能**:
- 角色创建和管理
- 权限分配
- 用户角色设置

**对应后端API**:
- `GET /api/admin/roles` - 获取角色列表
- `POST /api/admin/roles` - 创建角色
- `PUT /api/admin/roles/{id}` - 更新角色
- `DELETE /api/admin/roles/{id}` - 删除角色

**设计特点**:
- 权限树形结构
- 角色权限矩阵

#### 7.2 等级历史管理 (`/admin/level-history`)
**文件路径**: `views/admin/UserLevelHistoryManagement.vue`
**设计目的**:
- 管理用户等级系统
- 等级数据维护

**核心功能**:
- 用户等级调整
- 等级历史查询
- 积分手动调整

**对应后端API**:
- `GET /api/admin/user-level-history` - 获取等级历史
- `POST /api/admin/user-levels/adjust` - 调整用户等级
- `POST /api/admin/user-levels/points/add` - 增加积分

**设计特点**:
- 数据表格管理界面
- 批量操作功能

---

## 组件库说明

### 1. 通用组件 (`components/common/`)

#### AppleButton
- 用途：统一按钮组件
- 特点：多种样式变体（primary, secondary, danger等）
- 支持：加载状态、大小调整、图标集成

#### AppleInput
- 用途：统一输入框组件
- 特点：表单验证集成、多种输入类型
- 支持：前缀/后缀图标、清除功能

#### AppleCard
- 用途：卡片容器组件
- 特点：阴影效果、悬停交互
- 支持：头部、内容、操作区域划分

#### AppleModal
- 用途：模态对话框
- 特点：多种尺寸、动画效果
- 支持：确认/取消操作、表单集成

#### AppleTable
- 用途：数据表格组件
- 特点：分页、排序、筛选
- 支持：自定义列、操作按钮

### 2. 聊天组件 (`Chat/components/`)

#### MessageInput
- 用途：消息输入组件
- 功能：文本输入、表情选择、文件上传

#### MessageList
- 用途：消息列表展示
- 功能：虚拟滚动、消息类型渲染、时间分组

#### EmojiSelector
- 用途：表情符号选择器
- 功能：分类展示、搜索、最近使用

### 3. 消息组件 (`components/message/`)

#### VirtualMessageList
- 用途：高性能消息列表
- 功能：虚拟化渲染、消息懒加载

#### MessageThread
- 用途：消息线程展示
- 功能：回复链、线程参与者

---

## 工具库说明

### 1. API模块 (`api/modules/`)
- **auth.js**: 认证相关API
- **user.js**: 用户管理API
- **chat.js**: 聊天功能API
- **contact.js**: 联系人管理API
- **group.js**: 群组管理API
- **article.js**: 文章管理API
- **search.js**: 搜索功能API
- **notification.js**: 通知管理API

### 2. 工具函数 (`utils/`)
- **appleMessage.js**: 消息处理工具
- **appleIcons.js**: 图标管理
- **appleConfirm.js**: 确认对话框
- **logger.js**: 日志记录
- **uuid.js**: UUID生成
- **message.js**: 消息格式化

### 3. 状态管理 (`stores/`)
- **authStore.js**: 用户认证状态
- **chatStore.js**: 聊天状态管理
- **notificationStore.js**: 通知状态管理

---

## 设计原则

### 1. 用户体验原则
- **一致性**: 统一的UI组件和交互模式
- **响应式**: 适配各种设备屏幕
- **直观性**: 清晰的导航和操作反馈

### 2. 性能优化原则
- **懒加载**: 路由和组件按需加载
- **虚拟滚动**: 大列表性能优化
- **缓存策略**: API响应和静态资源缓存

### 3. 可维护性原则
- **组件化**: 高度复用的组件设计
- **模块化**: 清晰的代码组织结构
- **类型安全**: 完整的类型定义

### 4. 安全性原则
- **身份验证**: JWT令牌验证
- **权限控制**: 基于角色的访问控制
- **数据验证**: 前后端数据校验

---

## 页面关系图

```
登录/注册 → 主布局
    ↓
聊天页面 ←→ 联系人页面 ←→ 群组页面
    ↓
文章中心 → 文章详情 → 文章编辑
    ↓
用户资料 ←→ 设置页面 ←→ 通知中心
    ↓
管理员面板
```

这个前端架构提供了完整的社交平台功能，涵盖了用户管理、实时通信、内容发布、社交互动等核心特性。
