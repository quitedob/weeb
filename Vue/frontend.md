# 前端页面功能与设计文档

## 项目概述

这是一个基于Vue 3.5.13的前端社交平台项目，采用现代化的前端架构设计，提供实时聊天、内容管理、AI对话、用户社交等完整功能。项目使用Vite作为构建工具，Element Plus作为UI组件库，实现了响应式设计和TypeScript支持。

### 技术栈
- **Vue 3.5.13**: 渐进式JavaScript框架
- **Vue Router 4.5.0**: 路由管理
- **Pinia 2.1.7**: 状态管理
- **Element Plus 2.11.5**: UI组件库
- **Axios 1.7.9**: HTTP客户端
- **Vite 5.4.11**: 构建工具
- **Quill 2.0.3**: 富文本编辑器
- **@tanstack/vue-virtual 3.13.12**: 虚拟滚动
- **@stomp/stompjs 7.2.1**: WebSocket通信
- **Day.js 1.11.18**: 日期处理库
- **Lodash-es 4.17.21**: 工具函数库
- **DOMPurify 3.3.0**: HTML内容净化
- **Marked 16.4.1**: Markdown解析器

---

## 项目结构

```
Vue/src/
├── api/                          # API接口封装
│   ├── axiosInstance.js          # Axios实例配置
│   ├── index.js                  # API模块统一导出
│   └── modules/                  # API模块
│       ├── admin.js              # 管理员功能API
│       ├── ai.js                 # AI功能API
│       ├── article.js            # 文章管理API
│       ├── auth.js               # 认证相关API
│       ├── chat.js               # 聊天功能API
│       ├── comment.js            # 评论管理API
│       ├── contact.js            # 联系人管理API
│       ├── follow.js             # 关注管理API
│       ├── group.js              # 群组管理API
│       ├── message.js            # 消息管理API
│       ├── messageThread.js      # 消息线程API
│       ├── notification.js       # 通知管理API
│       ├── rolePermission.js     # 角色权限API
│       ├── search.js             # 搜索功能API
│       ├── searchEnhanced.js     # 增强搜索API
│       ├── user.js               # 用户管理API
│       ├── userLevel.js          # 用户等级API
│       ├── userLevelHistory.js   # 等级历史API
│       └── userLevelIntegration.js # 等级积分API
├── components/                   # 公共组件
│   ├── common/                   # Apple系列组件
│   │   ├── AppleButton.vue       # 统一按钮组件
│   │   ├── AppleInput.vue        # 统一输入框
│   │   ├── AppleCard.vue         # 卡片容器
│   │   ├── AppleModal.vue        # 模态对话框
│   │   └── ...
│   ├── message/                  # 消息相关组件
│   │   ├── MessageInput.vue      # 消息输入组件
│   │   ├── MessageList.vue       # 消息列表
│   │   └── EmojiSelector.vue     # 表情选择器
│   ├── article/                  # 文章相关组件
│   └── group/                    # 群组相关组件
├── views/                        # 页面组件
│   ├── chat/                     # 聊天相关页面
│   │   ├── ChatPage.vue          # 聊天页面
│   │   ├── ChatWindow.vue        # 聊天窗口
│   │   └── NewChatWindow.vue     # 新聊天窗口
│   ├── Login.vue                 # 登录页面
│   ├── Register.vue              # 注册页面
│   ├── Forget.vue                # 忘记密码页面
│   ├── UserProfile.vue           # 用户资料
│   ├── Settings.vue              # 设置页面
│   ├── UserDetail.vue            # 用户详情
│   ├── Groups.vue                # 群组列表
│   ├── NotificationListPage.vue  # 通知列表
│   ├── UserLevelHistory.vue      # 等级历史
│   ├── SearchPage.vue            # 搜索页面
│   ├── group/                    # 群组相关页面
│   │   └── GroupDetail.vue       # 群组详情
│   └── ...
├── stores/                       # Pinia状态管理
│   ├── authStore.js              # 用户认证状态
│   ├── chatStore.js              # 聊天状态管理
│   └── notificationStore.js      # 通知状态管理
├── router/                       # 路由配置
│   ├── index.js                  # 主路由配置
│   └── admin-routes.js           # 管理员路由
├── utils/                        # 工具函数
│   ├── appleMessage.js           # 消息处理工具
│   ├── appleIcons.js             # 图标管理
│   ├── logger.js                 # 日志记录
│   └── uuid.js                   # UUID生成
├── layout/                       # 布局组件
│   ├── Layout.vue                # 主布局
│   └── AsideMenu.vue             # 侧边菜单
├── assets/                       # 静态资源
│   ├── apple-style.css           # Apple风格样式
│   └── main.css                  # 主样式文件
├── constant/                     # 常量定义
│   └── emoji/                    # 表情相关常量
│       └── emoji.js              # 表情数据
├── auth/                         # 认证相关页面
│   ├── Register.vue              # 注册页面（备用）
│   ├── HelpCenter.vue            # 帮助中心
│   ├── UserInform.vue            # 用户信息
│   └── usermain.vue              # 用户主页面
├── value/                        # 值相关组件
│   ├── usevalue.vue              # 值使用组件
│   └── value.vue                 # 值组件
├── video/                        # 视频相关功能
│   └── Video.vue                 # 视频组件
└── main.js                       # 应用入口
```

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
- 提供完整的实时聊天体验
- 支持私聊和群聊消息收发
- 集成消息搜索和历史记录功能

**核心功能**:
- **聊天列表管理**: 显示所有聊天会话，支持搜索过滤
- **实时消息通信**: WebSocket + STOMP实现即时消息
- **消息类型支持**: 文本、表情、文件、图片消息
- **消息状态管理**: 发送中、已发送、已读状态
- **消息操作**: 撤回、回复、反应表情
- **消息搜索**: 本地消息内容搜索
- **在线状态**: 用户在线状态实时显示
- **消息线程**: 支持消息线程讨论
- **文件传输**: 消息附件上传和下载

**对应后端API**:
- `GET /api/chats` - 获取聊天列表
- `POST /api/chats` - 创建聊天会话
- `GET /api/chats/{chatId}/messages` - 获取聊天消息历史
- `POST /api/chats/{chatId}/messages` - 发送消息
- `POST /api/chats/{chatId}/read` - 标记消息已读
- `DELETE /api/chats/{chatId}` - 删除聊天会话
- `POST /api/chats/messages/{messageId}/react` - 添加消息反应
- `DELETE /api/chats/messages/{messageId}` - 撤回消息
- `GET /api/messages/search` - 搜索消息内容

**设计特点**:
- **响应式布局**: 桌面端左右分栏，移动端自适应
- **虚拟滚动**: 使用@tanstack/vue-virtual优化长列表性能
- **实时同步**: WebSocket实时消息推送和状态同步
- **消息分组**: 按日期分组显示消息
- **打字指示器**: 显示对方正在输入状态
- **消息预览**: 聊天列表显示最新消息预览
- **未读计数**: 实时显示未读消息数量

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
- 提供文章浏览和发现的主要入口
- 支持多维度文章搜索和筛选
- 展示文章统计数据和用户互动

**核心功能**:
- **文章列表展示**: 分页展示文章卡片，包含标题、摘要、作者、发布时间
- **高级搜索**: 支持标题、内容全文搜索，关键词高亮显示
- **多维度排序**: 按发布时间、阅读量、点赞数、收藏数排序
- **分类标签**: 文章标签分类筛选，支持多标签组合
- **用户互动**: 点赞、收藏、分享功能，一键操作
- **文章统计**: 显示阅读量、点赞数、评论数、收藏数
- **作者信息**: 显示作者头像、昵称、等级、文章数
- **快速操作**: 发布文章、管理文章、刷新列表等快捷入口

**对应后端API**:
- `GET /api/articles` - 获取文章列表（支持分页、排序、筛选）
- `POST /api/articles/{id}/like` - 点赞/取消点赞文章
- `POST /api/articles/{id}/collect` - 收藏/取消收藏文章
- `GET /api/articles/search` - 高级搜索文章
- `GET /api/articles/advanced-search` - 更详细的搜索条件
- `GET /api/articles/categories` - 获取文章分类列表
- `GET /api/articles/tags` - 获取文章标签列表
- `GET /api/articles/hot` - 获取热门文章
- `GET /api/articles/recommend` - 获取推荐文章

**设计特点**:
- **Element Plus集成**: 使用el-card、el-input、el-select等组件
- **响应式网格**: 自适应不同屏幕尺寸的卡片布局
- **搜索优化**: 实时搜索建议，搜索历史记录
- **加载状态**: 骨架屏加载效果，分页加载更多
- **交互反馈**: 操作成功/失败的Toast提示
- **数据缓存**: 合理使用浏览器缓存提升性能

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

#### AppleButton.vue
- **用途**: 统一按钮组件，基于Element Plus el-button封装
- **特点**: Apple风格设计，多种样式变体（primary, secondary, danger等）
- **支持**: 加载状态、大小调整、图标集成、禁用状态
- **使用场景**: 表单提交、操作按钮、链接按钮

#### AppleInput.vue
- **用途**: 统一输入框组件，基于Element Plus el-input封装
- **特点**: 表单验证集成、多种输入类型（text, password, email等）
- **支持**: 前缀/后缀图标、清除功能、长度限制、自定义验证规则
- **使用场景**: 登录表单、搜索框、表单输入

#### AppleCard.vue
- **用途**: 卡片容器组件，基于Element Plus el-card封装
- **特点**: Apple风格阴影效果、悬停交互动画
- **支持**: 头部、内容、操作区域划分，卡片阴影等级
- **使用场景**: 文章卡片、用户信息卡片、功能模块展示

#### AppleModal.vue
- **用途**: 模态对话框组件，基于Element Plus el-dialog封装
- **特点**: Apple风格设计、多种尺寸（small, medium, large, fullscreen）
- **支持**: 确认/取消操作、表单集成、拖拽移动、键盘事件
- **使用场景**: 确认对话框、表单弹窗、详情展示

#### AppleTable.vue
- **用途**: 数据表格组件，基于Element Plus el-table封装
- **特点**: 分页、排序、筛选功能集成
- **支持**: 自定义列配置、操作按钮、数据导出、行选择
- **使用场景**: 用户列表、文章管理、数据展示表格

#### AppleSelect.vue
- **用途**: 下拉选择组件，基于Element Plus el-select封装
- **特点**: 支持单选、多选、搜索过滤
- **支持**: 自定义选项渲染、分组显示、远程搜索
- **使用场景**: 分类选择、状态筛选、标签选择

#### AppleTextarea.vue
- **用途**: 多行文本输入组件
- **特点**: 自适应高度、字符计数、格式验证
- **支持**: Markdown预览、表情输入、文件拖拽
- **使用场景**: 文章编辑、评论输入、消息发送

#### ApplePagination.vue
- **用途**: 分页组件，基于Element Plus el-pagination封装
- **特点**: 多种分页样式，总数显示，页面跳转
- **支持**: 每页条数选择、快速跳转、总数统计
- **使用场景**: 列表分页、搜索结果分页

#### AppleTabs.vue & SimpleTabs.vue
- **用途**: 标签页组件
- **特点**: 标签切换、内容懒加载
- **支持**: 标签关闭、拖拽排序、自定义样式
- **使用场景**: 设置页面、功能模块切换

#### AppleTag.vue
- **用途**: 标签组件
- **特点**: 多种颜色主题、大小调整
- **支持**: 可关闭标签、链接标签、状态标签
- **使用场景**: 文章标签、用户状态、功能标识

#### AppleSwitch.vue
- **用途**: 开关组件
- **特点**: 自定义颜色、动画效果
- **支持**: 状态变更回调、禁用状态
- **使用场景**: 设置开关、状态切换

#### AppleDropdown.vue & AppleDropdownItem.vue
- **用途**: 下拉菜单组件
- **特点**: 触发方式多样（点击、悬停）
- **支持**: 菜单分组、图标支持、分隔线
- **使用场景**: 用户菜单、操作菜单、导航菜单

### 2. 聊天组件 (`Chat/components/`)

#### CustomStatus.vue
- **用途**: 自定义状态显示组件
- **功能**: 用户在线状态指示器、状态文本显示
- **特点**: 实时状态更新、状态颜色编码

#### LinkPreview.vue
- **用途**: 链接预览组件
- **功能**: 自动解析URL、生成预览卡片
- **特点**: 图片缩略图、标题描述、链接安全性检查

#### MessageThread.vue
- **用途**: 消息线程展示组件
- **功能**: 线程消息链展示、参与者列表
- **特点**: 展开/折叠控制、回复导航

#### UserMention.vue
- **用途**: 用户@提及组件
- **功能**: 用户名自动补全、提及高亮显示
- **特点**: 实时搜索、权限控制

### 3. 消息组件 (`components/message/`)

#### MessageInput.vue
- **用途**: 消息输入组件
- **功能**: 文本输入、表情插入、文件上传、语音输入
- **特点**: 自动调整高度、发送快捷键、草稿保存
- **集成**: Quill富文本编辑器、表情选择器

#### MessageList.vue
- **用途**: 消息列表展示组件
- **功能**: 消息渲染、时间分组、已读状态
- **特点**: 虚拟滚动优化、消息搜索高亮

#### EmojiSelector.vue
- **用途**: 表情符号选择器
- **功能**: 表情分类展示、搜索过滤、最近使用
- **特点**: 表情数据缓存、本地表情包支持

#### MessageThread.vue
- **用途**: 消息线程组件
- **功能**: 线程创建、回复管理、参与者管理
- **特点**: 线程嵌套、引用消息、权限控制

#### MessageThreadDisplay.vue
- **用途**: 消息线程显示组件
- **功能**: 线程消息渲染、展开控制
- **特点**: 递归渲染、性能优化

### 4. 增强搜索组件 (`components/EnhancedSearch.vue`)
- **用途**: 全局搜索组件
- **功能**: 多类型搜索、搜索建议、历史记录
- **特点**: 实时搜索、结果缓存、快捷键支持

### 5. 群组管理组件 (`components/group/`)

#### GroupApplicationHandler.vue
- **用途**: 群组申请处理组件
- **功能**: 申请列表展示、批准/拒绝操作
- **特点**: 批量处理、通知反馈

#### GroupMemberManager.vue
- **用途**: 群组成员管理组件
- **功能**: 成员列表、角色设置、移除成员
- **特点**: 权限检查、操作日志

### 6. 文章组件 (`components/article/`)

#### ArticleVersions.vue
- **用途**: 文章版本管理组件
- **功能**: 版本历史查看、版本对比、版本恢复
- **特点**: 差异高亮、时间线展示

---

## API模块说明

### 1. API模块 (`api/modules/`)
基于axios实例封装的API模块，按功能领域组织：

#### 核心API模块
- **auth.js**: 用户认证相关API
  - `login(data)` - 用户登录
  - `register(data)` - 用户注册
  - `logout()` - 用户登出
  - `getUserInfo()` - 获取当前用户信息
  - `updateUserInfo(data)` - 更新用户信息
  - `forgotPassword(email)` - 忘记密码
  - `resetPassword(data)` - 重置密码

- **user.js**: 用户管理API
  - `getUserById(userId)` - 获取指定用户信息
  - `getUsers(params)` - 获取用户列表（分页）
  - `searchUsers(q, limit)` - 搜索用户
  - `updateProfile(data)` - 更新个人资料
  - `uploadAvatar(file)` - 上传头像
  - `followUser(userId)` - 关注用户
  - `unfollowUser(userId)` - 取消关注
  - `getFollowers(userId, page, size)` - 获取粉丝列表

- **admin.js**: 管理员功能API
  - `getUsers(params)` - 获取用户列表
  - `banUser(userId)` - 封禁用户
  - `unbanUser(userId)` - 解封用户
  - `resetUserPassword(userId, password)` - 重置密码

- **rolePermission.js**: 角色权限API
  - `getRoles()` - 获取角色列表
  - `createRole(data)` - 创建角色
  - `updateRole(id, data)` - 更新角色
  - `deleteRole(id)` - 删除角色

- **chat.js**: 聊天功能API
  - `getChatList()` - 获取聊天列表
  - `createChat(targetId)` - 创建聊天会话
  - `getChatMessages(chatId, page, size)` - 获取聊天消息
  - `sendMessage(chatId, content)` - 发送消息
  - `markAsRead(chatId)` - 标记已读
  - `deleteChat(chatId)` - 删除聊天
  - `addReaction(messageId, reaction)` - 添加消息反应

#### 内容管理API模块
- **article.js**: 文章管理API
  - `getArticles(params)` - 获取文章列表
  - `getArticleById(id)` - 获取文章详情
  - `createArticle(data)` - 创建文章
  - `updateArticle(id, data)` - 更新文章
  - `deleteArticle(id)` - 删除文章
  - `searchArticles(query, page, size)` - 搜索文章
  - `likeArticle(id)` - 点赞文章
  - `collectArticle(id)` - 收藏文章
  - `getArticleComments(id, page, size)` - 获取文章评论

- **comment.js**: 评论管理API
  - `getComments(articleId, page, size)` - 获取文章评论
  - `addComment(articleId, content)` - 添加评论
  - `updateComment(commentId, content)` - 更新评论
  - `deleteComment(commentId)` - 删除评论
  - `likeComment(commentId)` - 点赞评论

#### 社交功能API模块
- **contact.js**: 联系人管理API
  - `getContacts(status)` - 获取联系人列表
  - `applyFriend(applyVo)` - 发送好友申请
  - `acceptFriend(contactId)` - 接受好友申请
  - `declineFriend(contactId)` - 拒绝好友申请
  - `blockContact(contactId)` - 拉黑联系人
  - `getContactGroups()` - 获取联系人分组
  - `createContactGroup(name, order)` - 创建分组

- **group.js**: 群组管理API
  - `getMyGroups()` - 获取我的群组
  - `getMyCreatedGroups()` - 获取我创建的群组
  - `createGroup(data)` - 创建群组
  - `updateGroup(groupId, data)` - 更新群组
  - `deleteGroup(groupId)` - 删除群组
  - `inviteUser(groupId, userId)` - 邀请用户加入群组
  - `leaveGroup(groupId)` - 退出群组

- **follow.js**: 关注管理API
  - `followUser(targetUserId)` - 关注用户
  - `unfollowUser(targetUserId)` - 取消关注
  - `getFollowStatus(targetUserId)` - 获取关注状态
  - `getFollowers(userId, page, size)` - 获取粉丝列表
  - `getFollowing(userId, page, size)` - 获取关注列表

#### 高级功能API模块
- **ai.js**: AI功能API
  - `chatWithAI(messages)` - AI聊天对话
  - `generateArticleSummary(content)` - 生成文章摘要
  - `refineText(content, tone)` - 润色文本
  - `translateText(content, targetLang)` - 翻译文本
  - `extractKeywords(content, count)` - 提取关键词
  - `generateTitleSuggestions(content, count)` - 生成标题建议

- **search.js**: 搜索功能API
  - `searchMessages(q, page, size, filters)` - 搜索消息
  - `searchUsers(q, page, size)` - 搜索用户
  - `searchGroups(q, page, size)` - 搜索群组
  - `searchArticles(q, page, size)` - 搜索文章
  - `searchAll(q, page, size)` - 综合搜索

- **notification.js**: 通知管理API
  - `getNotifications(page, size)` - 获取通知列表
  - `markAsRead(notificationId)` - 标记已读
  - `markAllAsRead()` - 全部标记已读
  - `deleteNotification(notificationId)` - 删除通知

- **message.js**: 消息管理API
  - `sendMessage(data)` - 发送消息
  - `getMessages(chatId, page, size)` - 获取消息列表
  - `deleteMessage(messageId)` - 删除消息
  - `markAsRead(messageId)` - 标记已读

- **messageThread.js**: 消息线程API
  - `createThread(data)` - 创建消息线程
  - `getThreadMessages(threadId, page, size)` - 获取线程消息
  - `replyToThread(threadId, content)` - 回复线程
  - `addThreadParticipant(threadId, userId)` - 添加参与者

- **comment.js**: 评论管理API
  - `getComments(articleId, page, size)` - 获取文章评论
  - `addComment(articleId, content)` - 添加评论
  - `updateComment(commentId, content)` - 更新评论
  - `deleteComment(commentId)` - 删除评论
  - `likeComment(commentId)` - 点赞评论

#### 等级管理API模块
- **userLevel.js**: 用户等级API
  - `getUserLevel(userId)` - 获取用户等级
  - `getLevelDefinitions()` - 获取等级定义
  - `addPoints(userId, points, reason)` - 增加积分

- **userLevelHistory.js**: 等级历史API
  - `getUserLevelHistory(userId, page, size)` - 获取等级历史
  - `getLevelStatistics()` - 获取等级统计

- **userLevelIntegration.js**: 等级积分API
  - `completeTask(userId, taskId)` - 完成任务
  - `getAvailableTasks(userId)` - 获取可用任务

## 工具库说明

### 2. 工具函数 (`utils/`)
- **appleMessage.js**: 消息处理工具
- **appleIcons.js**: 图标管理
- **appleConfirm.js**: 确认对话框
- **logger.js**: 日志记录
- **uuid.js**: UUID生成
- **message.js**: 消息格式化
- **elementMigrationHelper.js**: Element迁移助手
- **websocketDebug.js**: WebSocket调试工具

### 3. 状态管理 (`stores/`)
基于Pinia的状态管理，按功能模块组织：

- **authStore.js**: 用户认证状态管理
  - 用户登录/登出状态
  - JWT令牌管理
  - 用户信息缓存
  - 自动登录逻辑
  - 路由守卫集成

- **chatStore.js**: 聊天状态管理
  - 聊天列表状态
  - 当前活跃聊天
  - 消息历史缓存
  - WebSocket连接管理
  - 在线用户状态
  - 未读消息计数

- **newChatStore.js**: 新聊天状态管理
  - 扩展的聊天功能状态
  - 消息线程管理
  - 文件上传状态
  - 消息搜索状态

- **notificationStore.js**: 通知状态管理
  - 通知列表管理
  - 未读通知计数
  - 通知类型筛选
  - 实时通知推送

- **themeStore.js**: 主题状态管理
  - 亮色/暗色主题切换
  - 主题配置持久化
  - 自定义主题变量

---

## 设计原则

### 1. 用户体验原则
- **一致性**: 统一的Apple风格UI组件和交互模式
- **响应式**: 完美适配桌面端和移动端设备
- **直观性**: 清晰的信息层级和操作反馈
- **可访问性**: 支持键盘导航和屏幕阅读器
- **流畅性**: 60fps动画和流畅的页面切换

### 2. 性能优化原则
- **懒加载**: 路由组件和页面按需加载，首屏优化
- **虚拟滚动**: 使用@tanstack/vue-virtual处理大列表
- **缓存策略**: 多层缓存（浏览器缓存、内存缓存、IndexedDB）
- **代码分割**: 基于路由和功能的代码分割
- **资源优化**: 图片懒加载、字体子集、CDN加速

### 3. 可维护性原则
- **组件化**: 高内聚、低耦合的组件设计
- **模块化**: 清晰的目录结构和功能划分
- **标准化**: 统一的命名规范和代码风格
- **文档化**: 完整的组件文档和API说明
- **测试驱动**: 组件单元测试和集成测试

### 4. 安全性原则
- **身份验证**: JWT令牌管理和自动刷新
- **权限控制**: 基于用户角色的功能访问控制
- **数据验证**: 前后端双重数据校验和XSS防护
- **通信安全**: HTTPS强制使用和API请求签名
- **敏感信息**: 密码等敏感信息的安全处理

### 5. 架构设计原则
- **状态管理**: 基于Pinia的集中式状态管理
- **组合式API**: Vue 3 Composition API优先使用
- **类型安全**: 完整的TypeScript类型定义
- **错误处理**: 统一的错误捕获和用户友好的错误提示
- **国际化**: 支持多语言切换的架构设计

---

## 页面关系图

```
┌─────────────────────────────────────────────────────────────┐
│                        认证模块                                │
│  Login.vue → Register.vue → Forget.vue                      │
│  └─ auth/ (备用页面: HelpCenter.vue, UserInform.vue)        │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                        主布局 (Layout.vue)                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  AsideMenu.vue (侧边导航)                                 │ │
│  │  ├─ Chat (/chat) - 聊天页面                             │ │
│  │  │  └─ ChatPage.vue, ChatWindow.vue, NewChatWindow.vue  │ │
│  │  ├─ Contact (/contact) - 联系人页面                     │ │
│  │  │  └─ ContactPage.vue                                  │ │
│  │  ├─ Article (/article) - 文章中心                       │ │
│  │  │  └─ ArticleMain.vue, ArticleRead.vue, ArticleWrite.vue │ │
│  │  ├─ Groups (/groups) - 群组列表                         │ │
│  │  │  └─ Groups.vue, GroupDetail.vue                      │ │
│  │  ├─ Profile (/profile) - 个人资料                       │ │
│  │  │  └─ UserProfile.vue                                  │ │
│  │  ├─ Settings (/setting) - 设置页面                      │ │
│  │  │  └─ Settings.vue                                     │ │
│  │  ├─ Search (/search) - 全局搜索                         │ │
│  │  │  └─ SearchPage.vue                                   │ │
│  │  └─ Notifications (/notifications) - 通知中心           │ │
│      └─ NotificationListPage.vue                            │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  子页面路由:                                                   │
│  ├─ /chat/:type/:id - 指定聊天                               │
│  ├─ /article/read/:id - 文章阅读                             │
│  ├─ /article/write - 文章发布                               │
│  ├─ /article/manage - 文章管理                              │
│  ├─ /article/edit/:id - 文章编辑                            │
│  ├─ /groups/manage - 群组管理                               │
│  ├─ /group/:id - 群组详情                                   │
│  ├─ /user/:id - 用户详情                                    │
│  ├─ /level-history - 等级历史                               │
│  └─ /test-notifications - 通知测试                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        组件层                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  通用组件 (components/common/)                          │ │
│  │  ├─ AppleButton.vue, AppleInput.vue, ...              │ │
│  │  └─ AppleModal.vue, AppleTable.vue, ...               │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  业务组件                                               │ │
│  │  ├─ Chat/components/ - 聊天组件                        │ │
│  │  ├─ components/message/ - 消息组件                     │ │
│  │  ├─ components/article/ - 文章组件                     │ │
│  │  └─ components/group/ - 群组组件                       │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据层                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  API层 (api/modules/)                                   │ │
│  │  ├─ auth.js, user.js, chat.js, ...                     │ │
│  │  └─ ai.js, search.js, notification.js, ...            │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  状态管理 (stores/)                                     │ │
│  │  ├─ authStore.js - 用户认证状态                         │ │
│  │  ├─ chatStore.js - 聊天状态                             │ │
│  │  └─ notificationStore.js - 通知状态                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 技术特色

### 🎨 设计特色
- **Apple风格UI**: 统一的视觉设计语言和交互体验
- **响应式设计**: 支持桌面端、平板端、移动端多设备适配
- **暗色模式**: 支持亮色/暗色主题切换
- **无障碍设计**: 键盘导航和屏幕阅读器支持

### ⚡ 性能特色
- **Vite构建**: 快速的开发服务器和优化的生产构建
- **代码分割**: 基于路由的自动代码分割
- **虚拟滚动**: 大列表的高性能渲染
- **懒加载**: 图片和组件的按需加载

### 🔧 开发特色
- **组合式API**: Vue 3 Composition API的最佳实践
- **TypeScript**: 类型安全和更好的开发体验
- **组件化**: 高复用的组件设计模式
- **状态管理**: Pinia的现代化状态管理方案

这个前端架构提供了完整的现代化社交平台功能，涵盖了用户管理、实时通信、内容创作、社交互动、AI助手等核心特性，采用最新的前端技术和最佳实践构建。
