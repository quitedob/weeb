# 前端页面功能与设计文档

## 项目概述

这是一个基于Vue 3.5.13的前端社交平台项目，采用现代化的前端架构设计，提供实时聊天、内容管理、用户社交等完整功能。项目使用Vite作为构建工具，Element Plus作为UI组件库，实现了响应式设计和Apple风格的用户界面。

### 技术栈
- **Vue 3.5.13**: 渐进式JavaScript框架，使用Composition API
- **Vue Router 4.5.0**: 路由管理，支持懒加载和路由守卫
- **Pinia 2.1.7**: 状态管理，集成持久化插件
- **Element Plus 2.11.5**: UI组件库
- **Axios 1.7.9**: HTTP客户端，统一API调用
- **Vite 5.4.11**: 构建工具，支持热重载
- **Quill 2.0.3**: 富文本编辑器，用于文章编辑
- **@tanstack/vue-virtual 3.13.12**: 虚拟滚动，优化大列表性能
- **@stomp/stompjs 7.2.1**: WebSocket通信，STOMP协议支持
- **SockJS-client 1.6.1**: WebSocket降级支持
- **Socket.io-client 4.8.1**: 备用WebSocket通信
- **Day.js 1.11.18**: 日期处理库
- **Lodash-es 4.17.21**: 工具函数库
- **DOMPurify 3.3.0**: HTML内容净化，XSS防护
- **Marked 16.4.1**: Markdown解析器
- **Pinia-plugin-persistedstate 2.4.0**: 状态持久化
- **Vitest 3.2.4**: 单元测试框架

### 项目特性
- **Apple风格UI**: 统一的Apple设计语言组件库
- **实时通信**: WebSocket + STOMP实现的聊天系统
- **响应式设计**: 完美适配桌面端和移动端
- **主题系统**: 支持亮色/暗色/系统主题切换
- **性能优化**: 虚拟滚动、懒加载、缓存策略
- **权限管理**: 基于JWT的身份认证和授权
- **API标准化**: 统一的ChatListDTO数据格式，解决前后端字段不一致问题
- **智能数据解析**: 自动解析用户信息，支持JSON格式和降级处理
- **数据完整性**: SharedChat ID确保聊天会话的统一性和完整性
- **现代化开发**: 基于Vite 5.4.11的极速开发体验
- **完整测试**: Vitest 3.2.4 单元测试框架
- **代码质量**: ESLint + Prettier 统一代码风格
- **包分析**: Rollup Visualizer 优化构建体积

---

## 实际项目结构

```
vue/src/
├── api/                          # API接口封装
│   ├── index.js                  # API统一导出
│   └── modules/                  # API模块 (20个模块)
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
├── components/                   # 公共组件 (34个组件)
│   ├── common/                   # Apple系列组件 (24个组件)
│   │   ├── AppEmpty.vue          # 空状态组件
│   │   ├── AppSkeleton.vue       # 骨架屏组件
│   │   ├── AppleButton.vue       # 统一按钮组件
│   │   ├── AppleCard.vue         # 卡片容器
│   │   ├── AppleCol.vue          # 列组件
│   │   ├── AppleDropdown.vue     # 下拉菜单
│   │   ├── AppleDropdownItem.vue # 下拉菜单项
│   │   ├── AppleGrid.vue         # 网格布局
│   │   ├── AppleInput.vue        # 统一输入框
│   │   ├── AppleMessage.vue      # 消息组件
│   │   ├── AppleModal.vue        # 模态对话框
│   │   ├── AppleOption.vue       # 选项组件
│   │   ├── ApplePagination.vue   # 分页组件
│   │   ├── AppleSelect.vue       # 下拉选择
│   │   ├── AppleSwitch.vue       # 开关组件
│   │   ├── AppleTable.vue        # 数据表格
│   │   ├── AppleTabPane.vue      # 标签面板
│   │   ├── AppleTabs.vue         # 标签页
│   │   ├── AppleTag.vue          # 标签组件
│   │   ├── AppleTextarea.vue     # 多行文本
│   │   ├── ErrorBoundary.vue     # 错误边界组件
│   │   ├── SimpleTabs.vue        # 简单标签页
│   │   └── ThemeToggle.vue       # 主题切换
│   ├── message/                  # 消息相关组件
│   │   ├── EmojiSelector.vue     # 表情选择器
│   │   ├── MessageInput.vue      # 消息输入组件
│   │   ├── MessageList.vue       # 消息列表
│   │   ├── MessageThread.vue     # 消息线程
│   │   └── MessageThreadDisplay.vue # 消息线程显示
│   ├── article/                  # 文章相关组件
│   │   └── ArticleVersions.vue   # 文章版本管理
│   ├── group/                    # 群组相关组件
│   │   ├── GroupApplicationHandler.vue # 群组申请处理
│   │   └── GroupMemberManager.vue # 群组成员管理
│   ├── EnhancedSearch.vue        # 增强搜索组件
│   ├── UserMenu.vue              # 用户菜单
│   └── VirtualMessageList.vue    # 虚拟消息列表
├── views/                        # 页面组件 (23个页面)
│   ├── chat/                     # 聊天相关页面
│   ├── admin/                    # 管理员页面
│   ├── group/                    # 群组相关页面
│   ├── DiagnosticPage.vue        # 诊断页面
│   ├── Explore.vue               # 发现页面
│   ├── FollowList.vue            # 关注列表
│   ├── Forget.vue                # 忘记密码页面
│   ├── GroupManagement.vue       # 群组管理
│   ├── Groups.vue                # 群组列表
│   ├── Login.vue                 # 登录页面
│   ├── NotFound.vue              # 404页面
│   ├── NotificationListPage.vue  # 通知列表
│   ├── Register.vue              # 注册页面
│   ├── ResetPassword.vue         # 重置密码页面
│   ├── SecurityCenter.vue        # 安全中心
│   ├── Settings.vue              # 设置页面
│   ├── TestNotificationPage.vue  # 通知测试页面
│   ├── ThemeDemo.vue             # 主题演示
│   ├── ThemeTest.vue             # 主题测试
│   ├── UserDetail.vue            # 用户详情
│   ├── UserLevelHistory.vue      # 等级历史
│   └── UserProfile.vue           # 用户资料
├── stores/                       # Pinia状态管理
│   ├── index.js                  # Store统一导出
│   ├── setup.js                  # Store配置初始化
│   ├── README.md                 # Store说明文档
│   ├── authStore.js              # 用户认证状态
│   ├── chatStore.js              # 聊天状态管理
│   ├── newChatStore.js           # 新聊天状态管理
│   ├── notificationStore.js      # 通知状态管理
│   ├── themeStore.js             # 主题状态管理
│   └── plugins/                  # Store插件
│       ├── monitorPlugin.js      # 监控插件
│       └── persistPlugin.js      # 持久化插件
├── router/                       # 路由配置
│   ├── index.js                  # 主路由配置
│   └── admin-routes.js           # 管理员路由（已禁用）
├── utils/                        # 工具函数
│   ├── appleConfirm.js           # 确认对话框
│   ├── appleIcons.js             # 图标管理
│   ├── appleMessage.js           # 消息处理工具
│   ├── elementMigrationHelper.js # Element迁移助手
│   ├── logger.js                 # 日志记录
│   ├── message.js                # 消息格式化
│   ├── messageStatus.js          # 消息状态
│   ├── performance.js            # 性能监控
│   ├── uuid.js                   # UUID生成
│   └── websocketDebug.js         # WebSocket调试工具
├── layout/                       # 布局组件
│   ├── Layout.vue                # 主布局
│   ├── AsideMenu.vue             # 侧边菜单
│   └── components/               # 布局子组件
│       └── NotificationBell.vue  # 通知铃铛组件
├── assets/                       # 静态资源
│   └── main.css                  # 主样式文件
├── config/                       # 配置文件
│   └── performance.js            # 性能配置
├── constant/                     # 常量定义
│   └── emoji/                    # 表情相关常量
│       └── emoji.js              # 表情数据
├── article/                      # 文章页面组件
│   ├── ArticleEdit.vue           # 文章编辑页面
│   ├── ArticleMain.vue           # 文章主页
│   ├── ArticleManage.vue         # 文章管理
│   ├── ArticleModeration.vue     # 文章审核
│   ├── ArticleRead.vue           # 文章阅读
│   └── ArticleWrite.vue          # 文章写作
├── auth/                         # 认证相关页面
│   ├── HelpCenter.vue            # 帮助中心
│   ├── Register.vue              # 注册页面（备用）
│   ├── UserInform.vue            # 用户信息
│   └── usermain.vue              # 用户主页面
├── Chat/                         # 聊天组件
│   ├── ChatPage.vue              # 聊天页面
│   ├── ChatPageResponsive.vue    # 响应式聊天页面
│   └── components/               # 聊天子组件
│       ├── CustomStatus.vue      # 自定义状态
│       ├── LinkPreview.vue       # 链接预览
│       ├── MessageThread.vue     # 消息线程
│       └── UserMention.vue       # 用户提及
├── contact/                      # 联系人页面
│   └── ContactPage.vue           # 联系人页面
├── group/                        # 群组页面
│   ├── GroupDetailPage.vue       # 群组详情页面
│   └── GroupPage.vue             # 群组页面
├── search/                       # 搜索页面
│   └── SearchPage.vue            # 搜索页面
├── value/                        # 值相关组件
│   ├── usevalue.vue              # 值使用组件
│   └── value.vue                 # 值组件
├── video/                        # 视频相关功能
│   └── Video.vue                 # 视频组件
├── App.vue                       # 根组件
└── main.js                       # 应用入口
```

---

## 页面功能详解

### 1. 认证相关页面

#### 1.1 登录页面 (`/login`)
**文件路径**: `views/Login.vue`
**设计目的**:
- 提供用户身份验证入口
- 支持JWT令牌认证和自动刷新
- 实现智能路由跳转和登录状态持久化
- 友好的错误提示和验证反馈

**核心功能**:
- 用户名/密码登录
- 表单验证（必填、格式校验）
- JWT令牌管理（访问令牌+刷新令牌）
- 登录状态持久化（localStorage）
- 自动跳转到目标页面
- 登录失败错误处理

**对应后端API**:
- `POST /api/auth/login` - 用户登录
- `GET /api/users/me` - 获取用户信息（验证token）

**技术特点**:
- **智能令牌管理**: 自动检测令牌过期并刷新
- **路由守卫集成**: 自动重定向到登录前页面
- **Pinia状态管理**: 与authStore深度集成
- **响应式布局**: 适配桌面端和移动端

#### 1.2 注册页面 (`/register`)
**文件路径**: `views/Register.vue`
**设计目的**:
- 新用户注册账号
- 收集必要用户信息
- 密码强度验证和实时反馈
- 防止恶意注册和重复提交

**核心功能**:
- 用户名、邮箱、密码注册
- 密码确认验证
- 邮箱格式验证
- 用户名可用性检查
- 注册成功自动登录

**对应后端API**:
- `POST /api/auth/register` - 用户注册

**技术特点**:
- **实时验证**: 用户名和邮箱实时可用性检查
- **密码强度指示**: 动态密码强度提示
- **防重复提交**: 注册按钮状态管理

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

#### 1.4 重置密码页面 (`/reset-password`)
**文件路径**: `views/ResetPassword.vue`
**设计目的**:
- 通过重置链接设置新密码
- 验证重置令牌有效性
- 安全的密码更新流程

**核心功能**:
- 重置令牌验证
- 新密码设置和确认
- 密码强度验证
- 重置成功后自动登录

### 2. 主功能页面

#### 2.1 聊天页面 (`/chat`)
**文件路径**: `views/chat/ChatPage.vue`
**设计目的**:
- 提供完整的实时聊天体验
- 支持私聊和群聊消息收发
- 集成消息搜索和历史记录功能
- 解决群组创建后的聊天会话中断问题
- 修复私聊标题显示异常问题

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
- **智能名称显示**: 自动解析用户信息，显示真实姓名而非"Private Chat"

**核心增强功能**:
- **智能聊天名称解析**:
  - 自动解析后端JSON格式的用户信息
  - 支持新旧数据格式的平滑过渡
  - 降级处理机制确保显示可用
- **群组会话自动创建**:
  - 群组创建时自动生成SharedChat记录
  - 为群主和新成员自动创建ChatList记录
  - 解决"没下文了"的工作流中断问题
- **用户信息动态获取**:
  - 私聊显示对方用户的真实姓名和头像
  - 替换硬编码的"Private Chat"字符串
  - 支持用户昵称和用户名的智能选择

**对应后端API**:
- `GET /api/chats` - 获取聊天列表（返回ChatListDTO格式）
- `POST /api/chats` - 创建聊天会话
- `GET /api/chats/{chatId}/messages` - 获取聊天消息历史
- `POST /api/chats/{chatId}/messages` - 发送消息
- `POST /api/chats/{chatId}/read` - 标记消息已读
- `DELETE /api/chats/{chatId}` - 删除聊天会话
- `POST /api/chats/messages/{messageId}/react` - 添加消息反应
- `DELETE /api/chats/messages/{messageId}` - 撤回消息

**设计特点**:
- **响应式布局**: 桌面端左右分栏，移动端自适应
- **虚拟滚动**: 使用@tanstack/vue-virtual优化长列表性能
- **实时同步**: WebSocket实时消息推送和状态同步
- **消息分组**: 按日期分组显示消息
- **打字指示器**: 显示对方正在输入状态
- **消息预览**: 聊天列表显示最新消息预览
- **未读计数**: 实时显示未读消息数量
- **API标准化**: 使用ChatListDTO确保数据一致性
- **容错机制**: 多层降级处理确保功能可用性

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

#### 6.4 发现页面 (`/explore`)
**文件路径**: `views/Explore.vue`
**设计目的**:
- 内容发现和推荐
- 用户社交探索

**核心功能**:
- 推荐内容展示
- 热门用户推荐
- 趋势话题展示
- 兴趣标签推荐

**设计特点**:
- 瀑布流布局
- 个性化推荐算法

#### 6.5 关注列表 (`/follow-list`)
**文件路径**: `views/FollowList.vue`
**设计目的**:
- 管理关注关系
- 社交网络维护

**核心功能**:
- 关注/粉丝列表
- 批量关注管理
- 关注推荐
- 互相关注显示

**对应后端API**:
- `GET /api/user-follows/following` - 获取关注列表
- `GET /api/user-follows/followers` - 获取粉丝列表
- `GET /api/user-follows/mutual` - 获取互相关注

#### 6.6 安全中心 (`/security-center`)
**文件路径**: `views/SecurityCenter.vue`
**设计目的**:
- 账号安全管理
- 隐私保护设置

**核心功能**:
- 密码修改
- 两步验证设置
- 登录设备管理
- 隐私设置

**对应后端API**:
- `POST /api/auth/change-password` - 修改密码
- `PUT /api/users/me` - 更新安全设置

#### 6.7 系统诊断 (`/diagnostic`)
**文件路径**: `views/DiagnosticPage.vue`
**设计目的**:
- 系统状态检测
- 性能问题诊断

**核心功能**:
- 系统健康检查
- 性能指标监控
- 错误日志查看
- 网络连接测试

**对应后端API**:
- `GET /api/diagnostic/health` - 健康检查
- `GET /api/diagnostic/info` - 系统信息
- `GET /api/diagnostic/metrics` - 系统指标

#### 6.8 主题相关页面
**文件路径**: `views/ThemeDemo.vue`, `views/ThemeTest.vue`
**设计目的**:
- 主题系统测试和演示

**核心功能**:
- 主题切换演示
- 组件样式展示
- 响应式布局测试
- 暗色模式测试

**设计特点**:
- 开发者工具页面
- 主题开发参考

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

### 2.1 聊天页面组件 (`views/chat/`)

#### ChatPage.vue
- **用途**: 主聊天页面
- **功能**: 聊天列表、消息窗口、实时通信
- **核心增强**: 智能名称解析、API标准化、容错机制
- **关键方法**:
  - `getChatName(chat)`: 智能解析聊天名称，支持JSON格式和降级处理
  - `handleChatItemClick(chat)`: 处理聊天点击事件，使用sharedChatId
  - `normalizeChat(chat)`: 标准化聊天对象，确保数据一致性
- **特点**: WebSocket集成、响应式布局、消息状态管理、API标准化

#### ChatWindow.vue
- **用途**: 聊天窗口组件
- **功能**: 单个聊天会话界面
- **特点**: 消息历史、实时更新、输入状态

#### NewChatWindow.vue
- **用途**: 新聊天窗口
- **功能**: 创建新聊天会话
- **特点**: 用户选择、会话初始化

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

### 6.1 文章页面组件 (`article/`)

#### ArticleMain.vue
- **用途**: 文章中心主页
- **功能**: 文章列表展示、搜索筛选、分类浏览
- **特点**: 卡片布局、分页加载、搜索高亮

#### ArticleRead.vue
- **用途**: 文章阅读页面
- **功能**: 文章内容展示、互动功能、评论系统
- **特点**: 沉浸式阅读、响应式布局

#### ArticleWrite.vue
- **用途**: 文章编辑页面
- **功能**: 富文本编辑、媒体插入、草稿保存
- **特点**: 实时预览、自动保存

#### ArticleEdit.vue
- **用途**: 文章编辑页面
- **功能**: 编辑现有文章、版本管理
- **特点**: 继承ArticleWrite功能

#### ArticleManage.vue
- **用途**: 文章管理页面
- **功能**: 个人文章管理、批量操作、统计分析
- **特点**: 表格管理、状态筛选

#### ArticleModeration.vue
- **用途**: 文章审核页面
- **功能**: 内容审核、批量处理、审核统计
- **特点**: 管理员功能、审核流程

---

## 实际API模块说明

### API模块 (`api/modules/`)
基于axios实例封装的20+个API模块，统一错误处理和响应拦截：

#### 认证与用户管理
- **auth.js**: JWT认证系统，自动令牌刷新
- **user.js**: 用户资料、关注、统计信息
- **admin.js**: 管理员功能（封禁、重置密码）
- **rolePermission.js**: 角色权限管理

#### 实时通信模块
- **chat.js**: 聊天会话管理、在线状态、WebSocket集成
- **message.js**: 消息发送、状态管理、反应系统
- **messageThread.js**: 消息线程和回复管理

#### 内容管理系统
- **article.js**: 文章CRUD、点赞收藏、审核功能
- **comment.js**: 评论系统，支持嵌套回复
- **group.js**: 群组创建、成员管理、权限控制

#### 社交功能
- **contact.js**: 好友管理、申请处理、分组系统
- **follow.js**: 关注/取消关注、关系状态
- **notification.js**: 实时通知推送和管理

#### 高级功能
- **ai.js**: 15+种AI功能（文本处理、翻译、摘要等）
- **search.js**: 基础搜索功能
- **searchEnhanced.js**: 增强搜索（Elasticsearch + Redis缓存）

#### 用户成长系统
- **userLevel.js**: 用户等级和积分管理
- **userLevelHistory.js**: 等级变更历史追踪
- **userLevelIntegration.js**: 等级系统集成

### 2. 状态管理 (`stores/`)
基于Pinia的集中式状态管理，支持持久化和跨标签页同步：

#### **authStore.js** - 认证状态管理
```javascript
// 核心功能
- JWT令牌自动管理（访问令牌 + 刷新令牌）
- 智能令牌刷新（过期前5分钟自动刷新）
- 用户信息缓存和同步
- 登录状态持久化
- 自动清理机制
```

#### **chatStore.js** - 聊天状态管理
```javascript
// 核心功能
- WebSocket连接管理（STOMP协议）
- 聊天列表和消息缓存
- 实时消息推送
- 在线用户状态同步
- 未读消息计数
- 消息状态管理（发送中/已发送/已读）
```

#### **notificationStore.js** - 通知状态管理
```javascript
// 核心功能
- 通知列表管理
- 实时通知推送
- 未读通知计数
- 通知类型筛选
- 自动刷新机制
```

#### **themeStore.js** - 主题状态管理
```javascript
// 核心功能
- 亮色/暗色/系统主题切换
- 主题配置持久化
- 主题变量动态更新
```

#### **newChatStore.js** - 扩展聊天功能
```javascript
// 核心功能
- 高级聊天功能状态
- 消息线程管理
- 文件上传状态
- 消息搜索和过滤
```

### Store特性
- **持久化**: localStorage/sessionStorage自动持久化
- **跨标签页同步**: 多标签页状态实时同步
- **自动刷新**: 数据自动同步和令牌刷新
- **错误恢复**: 网络错误和认证失败自动处理
- **开发监控**: 开发环境状态变化监控

## 核心技术架构

### 1. 工具函数库 (`utils/`)
完整的前端工具函数库，支持日常开发需求：

- **appleMessage.js**: Apple风格消息提示系统
- **appleConfirm.js**: 统一确认对话框组件
- **appleIcons.js**: 图标管理和缓存系统
- **message.js**: 消息格式化和处理
- **messageStatus.js**: 消息状态管理（发送中/已发送/已读/失败）
- **logger.js**: 开发日志记录系统
- **performance.js**: 性能监控和分析
- **uuid.js**: UUID生成工具
- **elementMigrationHelper.js**: Element Plus迁移助手
- **websocketDebug.js**: WebSocket连接调试工具

### 2. 路由系统 (`router/`)
基于Vue Router 4.x的现代化路由系统：

#### **主路由配置 (index.js)**
```javascript
// 核心特性
- 懒加载路由组件（代码分割）
- 路由守卫和权限控制
- JWT令牌自动验证
- 智能重定向机制
- 元数据管理（页面标题、权限要求）

// 实际路由结构
├── / → /chat (默认重定向)
├── 认证路由 (/login, /register, /forget)
├── 主应用路由 (Layout包装)
│   ├── /chat - 聊天系统
│   ├── /chat/:type/:id - 指定聊天
│   ├── /article - 文章中心
│   ├── /article/read/:id - 文章阅读
│   ├── /article/write - 文章发布
│   ├── /article/manage - 文章管理
│   ├── /article/edit/:id - 文章编辑
│   ├── /contact - 联系人管理
│   ├── /groups - 群组列表
│   ├── /groups/manage - 群组管理
│   ├── /group/:id - 群组详情
│   ├── /profile - 个人资料
│   ├── /level-history - 等级历史
│   ├── /setting - 系统设置
│   ├── /search - 全局搜索
│   ├── /notifications - 通知中心
│   ├── /test-notifications - 通知测试
│   └── /user/:id - 用户详情
└── /* → 404页面
```

#### **路由守卫功能**
- **认证检查**: 自动验证JWT令牌有效性
- **智能刷新**: 令牌即将过期时自动刷新
- **权限控制**: 基于用户角色的页面访问控制
- **重定向管理**: 登录后自动跳转到目标页面
- **错误处理**: 认证失败时自动清理并重定向

### 3. 布局系统 (`layout/`)
响应式布局组件，支持多种设备：

#### **Layout.vue - 主布局容器**
- **侧边栏导航**: 折叠/展开，响应式适配
- **顶部导航栏**: 页面标题、用户菜单、通知铃铛
- **主内容区**: 路由视图容器，自适应高度
- **主题集成**: 完整的主题切换支持
- **移动端适配**: 小屏幕自动折叠侧边栏

#### **AsideMenu.vue - 侧边导航菜单**
- **导航分组**: 功能模块分类显示
- **状态指示**: 当前页面高亮显示
- **用户信息**: 头像、昵称、在线状态
- **快捷操作**: 主题切换、设置入口

#### **NotificationBell.vue - 通知铃铛组件**
- **实时更新**: WebSocket推送通知
- **未读计数**: 动态显示未读数量
- **快速预览**: 下拉显示最新通知
- **标记已读**: 批量标记已读功能

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

## 项目架构特性

### 1. 实时通信架构
- **WebSocket + STOMP**: 实现稳定的实时消息传输
- **连接管理**: 自动重连、心跳保活、连接状态监控
- **消息队列**: 确保消息可靠传递和顺序
- **多协议支持**: SockJS降级、Socket.io备用方案

### 2. Apple风格设计系统
- **统一组件库**: 19个Apple风格UI组件
- **设计语言**: 遵循Apple Human Interface Guidelines
- **主题系统**: 亮色/暗色/系统主题无缝切换
- **响应式设计**: 完美适配各种设备尺寸

### 3. 高性能架构
- **虚拟滚动**: @tanstack/vue-virtual优化大列表性能
- **懒加载**: 路由和组件按需加载，减少首屏时间
- **缓存策略**: 多层缓存提升响应速度
- **代码分割**: 基于功能的智能代码分割

### 4. 状态管理架构
- **Pinia生态**: 现代化状态管理，支持DevTools
- **持久化**: 自动状态持久化和跨标签页同步
- **模块化**: 按功能域拆分，高内聚低耦合
- **响应式**: 完整的Vue 3 Composition API集成

### 5. 开发体验
- **热重载**: Vite提供极速的开发体验
- **组件文档**: 完整的组件使用说明和示例
- **调试工具**: WebSocket调试、性能监控、状态追踪
- **代码质量**: ESLint + Prettier统一代码风格

### 6. 安全特性
- **JWT管理**: 智能令牌刷新和安全存储
- **XSS防护**: DOMPurify内容净化，防止注入攻击
- **CSRF防护**: 请求签名和验证机制
- **权限控制**: 细粒度的页面和功能权限控制

---

## 实际开发脚本

### NPM Scripts
```json
{
  "dev": "vite",                           // 开发服务器
  "build": "vite build",                   // 生产构建
  "preview": "vite preview",               // 构建预览
  "test": "vitest",                        // 单元测试
  "test:run": "vitest run",                // 运行测试
  "clean:console": "node scripts/clean-console.js",  // 清理调试代码
  "build:prod": "npm run clean:console && vite build", // 生产环境构建
  "dev:check": "node scripts/dev-check.js",            // 开发环境验证
  "precommit": "npm run dev:check && npm run clean:console" // Git提交前验证
}
```

### 开发工具链
- **Vite 5.4.11**: 极速构建工具，支持热重载
- **Vitest 3.2.4**: 单元测试框架，集成jsdom 27.0.0
- **Vue DevTools**: Vue 3开发者工具，vite-plugin-vue-devtools 7.6.5
- **Rollup Visualizer**: 包大小分析工具，v6.0.5
- **Terser**: JavaScript压缩工具，v5.44.1
- **@vitejs/plugin-vue**: Vue单文件组件支持，v5.2.1
- **@vue/test-utils**: Vue测试工具，v2.4.6

---

## 项目统计

### 代码规模
- **Vue组件**: 84个组件文件 (34个components + 23个views + 27个其他页面组件)
- **API模块**: 19个功能模块
- **工具函数**: 10个实用工具
- **路由页面**: 23个功能页面
- **状态管理**: 9个Store文件 (5个核心Store + 4个插件)

### 功能覆盖
- ✅ **实时聊天**: WebSocket + 消息状态管理
- ✅ **文章系统**: 创建、编辑、版本管理、审核
- ✅ **用户管理**: 认证、权限、等级系统
- ✅ **社交功能**: 好友、群组、关注、通知
- ✅ **搜索功能**: 全文搜索、高级筛选
- ✅ **AI集成**: 15+种AI功能接口
- ✅ **管理系统**: 角色、权限、内容审核
- ✅ **主题系统**: 完整的主题切换支持
- ✅ **API标准化**: 统一的ChatListDTO数据格式
- ✅ **智能解析**: 自动解析用户信息，支持JSON格式
- ✅ **数据完整性**: SharedChat确保聊天会话完整性
- ✅ **容错机制**: 多层降级处理确保功能可用性

这是一个功能完整、架构现代化、性能优化的前端社交平台项目。

---

