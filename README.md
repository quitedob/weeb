# WEEB 现代化即时通信与内容管理系统

🚀 **一键启动，自动初始化数据库** | 💬 **实时聊天，群组协作** | 📝 **文章发布，智能搜索**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.0-brightgreen.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## ✨ 核心特性

- 🚀 **高性能架构**: Spring Boot 3 + MySQL + Redis缓存 + 连接池优化
- 💬 **实时通信**: WebSocket实现实时消息推送 + 异步任务处理
- 🔍 **智能搜索**: Elasticsearch全文搜索（已启用）
- 📁 **文件管理**: 多文件上传与云存储集成
- 🔐 **企业级安全**: JWT令牌认证 + RSA/AES加密 + 密钥持久化
- 🎨 **现代化UI**: Vue 3 + Element Plus + 响应式设计
- 🏗️ **智能初始化**: 环境感知自动创建数据库（开发环境）+ 生产环境安全验证
- 🐳 **容器化部署**: Docker支持 + 环境变量配置
- 🔒 **安全防护**: CORS严格控制 + SSL强制连接 + 敏感信息环境变量管理

## 📋 目录

- [核心特性](#核心特性)
- [安全配置](#安全配置)
- [项目概述](#项目概述)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [API文档](#api-文档)
- [数据库结构](#数据库结构)
- [前端技术栈](#前端技术栈)
- [安全与认证](#安全与认证)
- [常见问题](#常见问题)
- [部署指南](#部署指南)
- [贡献指南](#贡献指南)

---

## 🔒 安全配置

⚠️ **重要**: 生产环境部署前必须完成以下安全配置，否则应用将无法启动！

### 开发环境配置

开发环境使用默认配置，支持快速启动：

```bash
# 数据库默认配置（仅开发环境）
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
```

### 生产环境配置

生产环境必须设置以下环境变量：

```bash
# 数据库配置（必须设置）
MYSQL_HOST=your_mysql_host
MYSQL_PORT=3306
MYSQL_DATABASE=weeb_prod
MYSQL_USERNAME=your_db_username
MYSQL_PASSWORD=your_secure_db_password

# JWT安全配置（必须设置强密钥）
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_at_least_32_characters_long

# AES加密密钥（必须设置）
AES_SECRET_KEY=your_secure_aes_key_at_least_16_characters

# 应用安全配置
WEEB_PASSWORD=your_secure_group_password
# AI功能已移除，不再需要DOUBAO_API_KEY配置

# CORS安全配置（生产环境必须设置具体域名）
ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
```

#### 生产环境启动命令

```bash
# 设置环境变量后启动
export MYSQL_HOST=your_host
export MYSQL_USERNAME=your_user
export MYSQL_PASSWORD=your_password
export JWT_SECRET=your_secure_jwt_secret
export AES_SECRET_KEY=your_aes_key
export ALLOWED_ORIGINS=https://yourdomain.com

java -jar weeb.jar --spring.profiles.active=prod
```

### 安全特性

- ✅ **JWT令牌安全**: 自动验证复杂度，生产环境强制强密钥，防止默认密钥风险
- ✅ **多层加密保护**: AES环境变量配置 + RSA密钥持久化存储 + BCrypt密码加密
- ✅ **连接安全**: 生产环境强制SSL/HTTPS + 数据库连接加密 + 安全连接池配置
- ✅ **访问控制**: CORS严格域名限制 + 环境感知配置 + 请求过滤器
- ✅ **密钥管理**: 敏感信息环境变量管理 + 密钥持久化 + 启动时安全验证
- ✅ **异步安全**: 线程池优化 + Redis原子操作 + 限流保护
- ✅ **SQL注入防护**: 内置SqlInjectionUtils工具类，动态检测和防止SQL注入攻击
- ✅ **输入验证**: ValidationUtils统一验证用户名、密码、邮箱、手机号等敏感字段
- ✅ **安全审计**: SecurityAuditUtils记录所有安全相关事件，包括登录失败、账户锁定、敏感操作
- ✅ **账户锁定**: 自动检测失败登录尝试，临时锁定账户防止暴力破解
- ✅ **XSS防护**: 输入内容自动检测和过滤XSS攻击模式
- ✅ **敏感词过滤**: 内置敏感词检测，防止不当内容发布
- ✅ **文件上传安全**: 严格的文件类型、大小和内容验证

---

## 项目概述

WEEB 是一个现代化的即时通信与内容管理系统，专为团队协作和内容创作而设计。系统集成了实时聊天、群组管理、文章发布、文件共享等核心功能，提供完整的企业级解决方案。

### 核心功能
- 👥 **用户管理**: 注册登录、个人资料管理、安全认证、账户锁定保护、用户关注系统
- 💬 **实时聊天**: 私聊、群聊、消息推送、消息搜索、消息撤回、消息状态
- 👥 **群组协作**: 创建群组、成员管理、权限控制、群组公告
- 📝 **内容创作**: 文章发布、编辑、富文本编辑器、评论系统、分类管理
- 👍 **社交互动**: 文章点赞、收藏、评论系统、阅读统计
- 🔍 **智能搜索**: 全文搜索、消息检索、文章搜索、Elasticsearch集成
- 📁 **文件管理**: 多文件上传、云存储、文件类型验证、安全检查、文件分享
- 🔔 **通知系统**: 实时通知、消息提醒、系统公告、邮件通知
- 📊 **数据统计**: 用户数据、文章统计、访问分析、性能监控
- 🔒 **安全防护**: SQL注入防护、XSS防护、输入验证、安全审计日志
- 🤖 **AI聊天**: AI对话服务 (AiChatService)
- 🖥️ **SSH终端**: 远程命令执行和终端交互功能
- 💬 **高级聊天功能**: 消息线索、用户提及、链接预览等现代化聊天体验

### 技术特色
- 🏗️ **自动初始化**: 一键启动，自动创建数据库和表结构
- 🔧 **环境隔离**: 开发/生产环境配置分离
- 🚀 **高性能**: Spring Boot 3 + MySQL优化
- 🔒 **安全可靠**: JWT认证、权限控制
- 📱 **响应式**: 现代化前端界面

---

## 快速开始

### 🚀 启动项目

项目支持**智能数据库初始化**，启动时会自动检查数据库结构，无需手动配置：

#### 🤖 智能初始化（推荐）

项目内置 `DatabaseInitializer` 组件，启动时自动执行：
- ✅ 检查并创建 `weeb` 数据库
- ✅ **智能验证表结构**：检查每个表是否包含所有必需的列
- ✅ **自动重建机制**：如果任何表结构不符合预期，自动删除并重建整个数据库
- ✅ 设置外键约束和索引优化
- ✅ 插入初始管理员账号和测试数据

只需启动应用即可：

```bash
mvn spring-boot:run
```

#### 🔧 表结构验证逻辑

系统会验证以下核心表的结构完整性：
- `user` - 用户表
- `user_stats` - 用户统计表
- `group` - 群组表
- `message` - 消息表
- `articles` - 文章表
- 以及其他所有业务表

**如果任何表缺少必需的列，系统将：**
1. 🔄 自动删除现有数据库
2. 🆕 创建全新的数据库结构
3. 👥 重新插入所有默认用户和数据
4. ✅ 确保系统以最新配置启动

> ⚠️ **注意**: 这意味着开发过程中数据库会被完全重置，请勿在开发环境存放重要数据

> 📖 **详细说明**: 查看 [DatabaseInitializer 使用指南](DATABASE_INITIALIZER_GUIDE.md)

#### 🔍 搜索功能配置

项目支持Elasticsearch全文搜索，但为可选功能：

**Elasticsearch已禁用（默认）**：
- 默认情况下，Elasticsearch功能已禁用，应用可以正常启动
- 基础搜索功能（用户、群组、文章）使用数据库实现
- 如需启用Elasticsearch，请修改 `application.yml`：
  ```yaml
  elasticsearch:
    enabled: true
  ```

**启用Elasticsearch**：
1. 确保Elasticsearch 8.x服务运行在 `http://localhost:9200`
2. 禁用安全认证（开发环境）：
   ```yaml
   # elasticsearch.yml
   xpack.security.enabled: false
   ```
3. 修改应用配置启用ES功能
4. 重启应用

#### 🛠️ 手动配置（可选）

如果需要手动配置，请按照以下步骤操作：

### 📋 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | Java 运行环境 |
| MySQL | 8.0+ | 数据库（自动初始化） |
| Maven | 3.6+ | 项目构建工具 |
| Node.js | 16+ | 前端开发环境 |
| Redis | 7.0+ | 缓存服务（可选） |
| Elasticsearch | 8.x | 搜索服务（可选） |

### 🛠️ 手动配置

如果需要手动配置，请按照以下步骤：

**注意**：系统会自动创建测试账号，无需手动创建。

1. **启动 MySQL 服务**
   ```bash
   # 确保 MySQL 服务正在运行
   sudo systemctl start mysql  # Linux
   # 或 Windows 服务管理器启动 MySQL
   ```

2. **可选：设置环境变量**
   ```bash
   # 自定义数据库配置
   export MYSQL_HOST=localhost
   export MYSQL_PORT=3306
   export MYSQL_DATABASE=weeb
   export MYSQL_USERNAME=root
   export MYSQL_PASSWORD=your_password

   # Elasticsearch配置（可选，已默认配置为HTTP模式）
   export ES_URIS=http://localhost:9200
   ```

3. **启动后端服务**
   ```bash
   # 开发环境
   mvn spring-boot:run

   # 生产环境
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

4. **启动前端服务**
   ```bash
   cd Vue
   npm install
   npm run dev
   ```

### 🌐 访问应用

- **后端 API**: http://localhost:8080
- **前端界面**: http://localhost:5173 (默认端口，如被占用会自动使用5174等)
- **WebSocket**: ws://localhost:8081/ws
- **Elasticsearch**: http://localhost:9200 (可选)
- **Redis**: localhost:6379 (可选)

### 📝 默认账号

系统启动时会自动初始化数据库并创建测试账号：

#### 🔐 管理员账号
- **用户名**: `admin`
- **密码**: `admin123`
- **邮箱**: `admin@weeb.com`
- **类型**: 系统管理员
- **权限**: 拥有所有系统管理权限

#### 👥 测试用户账号
- **用户名**: `testuser`
- **密码**: `test123`
- **邮箱**: `test@weeb.com`
- **类型**: 普通用户
- **权限**: 基础用户功能权限

#### 🎭 额外测试用户
系统还会自动创建以下测试用户（密码均为 `test123`）：
- **alice** - 爱丽丝 (alice@weeb.com)
- **bob** - 鲍勃 (bob@weeb.com)
- **charlie** - 查理 (charlie@weeb.com)
- **diana** - 戴安娜 (diana@weeb.com)
- **eve** - 伊芙 (eve@weeb.com)

> 💡 **提示**:
> - 数据库自动初始化由 `DatabaseInitializer.java` 处理
> - 系统会自动检查表结构，如不符合预期会重建整个数据库
> - 如果遇到数据库连接问题，请确保 MySQL 8.0+ 服务正在运行
> - 默认数据库配置：localhost:3306，数据库名：weeb，用户名：root，密码：1234
> - 生产环境会跳过自动初始化，请手动配置数据库

---

## 核心功能

### 👥 用户系统
- 用户注册登录（JWT认证）
- 个人资料管理
- 在线状态管理

### 💬 聊天系统
- 实时私聊和群聊 (WebSocket)
- 消息推送通知
- 文件和图片分享
- 消息搜索和历史记录
- 打字指示器和消息状态 (发送中/已发送/已送达/已读)
- 表情包支持和消息撤回功能
- 实时消息同步和断线重连

### 👥 群组协作
- 创建和管理群组
- 成员邀请和权限管理
- 群组公告和设置

### 📝 内容管理
- 文章发布和编辑
- 点赞收藏功能
- 内容搜索和分类

### 🔍 搜索功能
- 消息全文搜索 (Elasticsearch)
- 用户和群组搜索
- 文章内容搜索
- 综合搜索 (同时搜索所有类型)
- 高级搜索过滤器 (待实现)

### 🔔 通知系统
- 实时消息通知
- 系统公告提醒
- 个人消息推送
- 未读消息计数和批量操作
- 通知历史管理

---

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.5.4
- **数据库**: MySQL 8.0+ (环境感知自动初始化)
- **ORM**: MyBatis-Plus 3.5.8
- **认证**: JWT (JSON Web Token) + BCrypt密码加密
- **缓存**: Redis + Caffeine (限流 + 原子操作)
- **搜索**: Elasticsearch 8.18.6 (可选)
- **实时通信**: WebSocket + STOMP
- **安全**: RSA/AES加密 + SSL/TLS + CORS控制 + SQL注入防护 + XSS防护
- **安全工具**: ValidationUtils、SqlInjectionUtils、SecurityAuditUtils
- **日志**: 结构化日志 + 安全事件审计 + 性能监控
- **其他**: Netty 4.1.108、Apache SSHD 2.14.0、Hutool 5.8.18、敏感词过滤、IP2Region

### 前端技术
- **框架**: Vue 3.5.13 + Composition API
- **构建工具**: Vite 5.4.11 + 环境变量支持
- **状态管理**: Pinia 2.1.7 + Token持久化
- **UI组件**: Element Plus 2.7.8 + @element-plus/icons-vue 2.3.2
- **HTTP客户端**: Axios 1.7.9 + 拦截器优化 + Token管理
- **路由**: Vue Router 4.5.0 + 认证守卫
- **富文本编辑**: Quill 2.0.3
- **测试**: Vitest 3.2.4 + @vue/test-utils 2.4.6

### 开发工具
- **构建**: Maven 3.6+
- **JDK**: Java 17+
- **Node.js**: 16+
- **包管理**: npm/yarn

## 项目结构
weeb/
├── src/main/java/com/web/          # 后端主包 (197个Java文件)
│   ├── annotation/                 # 自定义注解
│   │   ├── CommandInfo.java       # 命令信息注解
│   │   ├── UrlFree.java           # URL免登录注解
│   │   ├── UrlLimit.java          # URL限流注解
│   │   ├── UrlResource.java       # URL资源注解
│   │   ├── Userid.java            # 用户ID注解
│   │   └── UserIp.java            # 用户IP注解
│   ├── aop/                        # 面向切面编程
│   │   ├── MessageRateLimitAspect.java # 消息限流切面
│   │   └── UrlLimitAspect.java     # URL限流切面
│   ├── common/                     # 公共类
│   │   └── ApiResponse.java       # 统一API响应格式
│   ├── Config/                     # 配置类
│   │   ├── AsyncConfig.java       # 异步配置
│   │   ├── CacheConfig.java       # 缓存配置
│   │   ├── CorsConfig.java        # CORS跨域配置
│   │   ├── DatabaseInitializer.java # 数据库自动初始化
│   │   ├── ElasticsearchConfig.java # Elasticsearch配置
│   │   ├── MybatisHandler.java    # MyBatis处理器
│   │   ├── RedisConfig.java       # Redis配置
│   │   ├── RedisHealthCheckConfig.java # Redis健康检查配置
│   │   ├── SecurityBeansConfig.java # 安全Bean配置
│   │   ├── SecurityConfig.java    # 安全配置
│   │   ├── SensitiveWordConfig.java # 敏感词配置
│   │   ├── UserInfoArgumentResolver.java # 用户信息参数解析器
│   │   ├── WebMvcConfig.java      # Web MVC配置
│   │   └── WeebConfig.java        # 应用主配置
│   ├── constant/                   # 常量类
│   │   ├── BadgeType.java         # 徽章类型
│   │   ├── ChatListType.java      # 聊天列表类型
│   │   ├── ContactStatus.java     # 联系人状态
│   │   ├── GroupRole.java         # 群组角色
│   │   ├── LimitKeyType.java      # 限流键类型
│   │   ├── MessageSource.java     # 消息来源
│   │   ├── MessageType.java       # 消息类型
│   │   ├── NotifyType.java        # 通知类型
│   │   ├── TextContentType.java   # 文本内容类型
│   │   ├── UserOnlineStatus.java  # 用户在线状态
│   │   ├── UserType.java          # 用户类型
│   │   └── WsContentType.java     # WebSocket内容类型
│   ├── Controller/                 # REST控制器 (17个)
│   │   ├── ArticleCenterController.java # 文章中心控制器
│   │   ├── ArticleCommentController.java # 文章评论控制器
│   │   ├── AuthController.java    # 认证控制器
│   │   ├── ChatController.java    # 聊天控制器
│   │   ├── ChatListController.java # 聊天列表控制器
│   │   ├── ContactController.java # 联系人控制器
│   │   ├── FileController.java    # 文件控制器
│   │   ├── FileManagementController.java # 文件管理控制器
│   │   ├── GroupController.java   # 群组控制器
│   │   ├── MessageController.java # 消息控制器
│   │   ├── MigrationController.java # 数据迁移控制器
│   │   ├── NotificationController.java # 通知控制器
│   │   ├── SearchController.java  # 搜索控制器
│   │   ├── UploadController.java  # 上传控制器
│   │   ├── UserController.java    # 用户控制器
│   │   └── UserFollowController.java # 用户关注控制器
│   ├── WeebApplication.java       # Spring Boot主启动类
│   ├── dto/                        # 数据传输对象
│   │   ├── NotifyDto.java         # 通知DTO
│   │   ├── RedisBroadcastMsg.java # Redis广播消息
│   │   ├── UrlLimitStats.java     # URL限流统计
│   │   └── UserDto.java           # 用户DTO
│   ├── exception/                  # 异常处理
│   │   ├── GlobalExceptionHandler.java # 全局异常处理器
│   │   └── WeebException.java     # 自定义异常
│   ├── filter/                     # 过滤器
│   │   └── JwtAuthenticationFilter.java # JWT认证过滤器
│   ├── mapper/                     # MyBatis Mapper接口
│   │   ├── ArticleCommentMapper.java # 文章评论Mapper
│   │   ├── ArticleMapper.java     # 文章Mapper
│   │   ├── AuthMapper.java        # 认证Mapper
│   │   ├── ChatListMapper.java    # 聊天列表Mapper
│   │   ├── ContactMapper.java     # 联系人Mapper
│   │   ├── FileMapper.java        # 文件Mapper
│   │   ├── FileRecordMapper.java  # 文件记录Mapper
│   │   ├── FileShareMapper.java   # 文件分享Mapper
│   │   ├── GroupMapper.java       # 群组Mapper
│   │   ├── GroupMemberMapper.java # 群组成员Mapper
│   │   ├── MessageMapper.java     # 消息Mapper
│   │   ├── MessageReactionMapper.java # 消息反应Mapper
│   │   ├── NotificationMapper.java # 通知Mapper
│   │   ├── UserFollowMapper.java  # 用户关注Mapper
│   │   ├── UserMapper.java        # 用户Mapper
│   │   └── UserStatsMapper.java   # 用户统计Mapper
│   ├── migration/                   # 数据库迁移
│   │   ├── DatabaseMigrationExecutor.java # 数据库迁移执行器
│   │   ├── MigrationRunner.java      # 迁移运行器
│   │   └── MigrationValidator.java   # 迁移验证器
│   ├── model/                      # 数据模型
│   │   ├── Article.java           # 文章模型
│   │   ├── ArticleComment.java    # 文章评论模型
│   │   ├── ChatList.java          # 聊天列表模型
│   │   ├── Contact.java           # 联系人模型
│   │   ├── elasticsearch/         # Elasticsearch文档
│   │   │   └── MessageDocument.java # 消息文档
│   │   ├── FileRecord.java        # 文件记录模型
│   │   ├── FileShare.java         # 文件分享模型
│   │   ├── FileTransfer.java      # 文件传输模型
│   │   ├── Group.java             # 群组模型
│   │   ├── GroupMember.java       # 群组成员模型
│   │   ├── Message.java           # 消息模型
│   │   ├── MessageReaction.java   # 消息反应模型
│   │   ├── Notification.java      # 通知模型
│   │   ├── User.java              # 用户模型
│   │   ├── UserFollow.java        # 用户关注模型
│   │   ├── UserStats.java         # 用户统计模型
│   │   └── UserWithStats.java     # 带统计的用户模型
│   ├── repository/                 # 数据仓库层
│   │   └── MessageSearchRepository.java # 消息搜索仓库
│   ├── runner/                     # 应用启动器
│   │   └── UrlPassRunner.java     # URL通行启动器
│   ├── schedule/                   # 定时任务
│   │   └── ExpiredClearTask.java  # 过期清理任务
│   ├── service/                    # 业务逻辑层 (37个)
│   │   ├── AiChatService.java     # AI聊天服务
│   │   ├── ArticleCommentService.java # 文章评论服务
│   │   ├── ArticleService.java    # 文章服务
│   │   ├── AuthService.java       # 认证服务
│   │   ├── ChatListService.java   # 聊天列表服务
│   │   ├── ChatService.java       # 聊天服务
│   │   ├── ContactService.java    # 联系人服务
│   │   ├── FileManagementService.java # 文件管理服务
│   │   ├── FileService.java       # 文件服务
│   │   ├── GroupService.java      # 群组服务
│   │   ├── Impl/                  # 服务实现类 (18个)
│   │   │   ├── ArticleCommentServiceImpl.java
│   │   │   ├── ArticleServiceImpl.java
│   │   │   ├── AuthServiceImpl.java
│   │   │   ├── ChatListServiceImpl.java
│   │   │   ├── ChatServiceImpl.java
│   │   │   ├── ContactServiceImpl.java
│   │   │   ├── FileManagementServiceImpl.java
│   │   │   ├── FileServiceImpl.java
│   │   │   ├── GroupServiceImpl.java
│   │   │   ├── MessageServiceImpl.java
│   │   │   ├── NotificationServiceImpl.java
│   │   │   ├── SearchServiceImpl.java
│   │   │   ├── StorageServiceImpl.java
│   │   │   ├── UserFollowServiceImpl.java
│   │   │   ├── UserServiceImpl.java
│   │   │   ├── UserStatsServiceImpl.java
│   │   │   └── UserTransactionServiceImpl.java
│   │   ├── MessageService.java    # 消息服务
│   │   ├── NotificationService.java # 通知服务
│   │   ├── RedisSubscriber.java   # Redis订阅者
│   │   ├── SearchService.java     # 搜索服务
│   │   ├── StorageService.java    # 存储服务
│   │   ├── UserFollowService.java # 用户关注服务
│   │   ├── UserService.java       # 用户服务
│   │   ├── UserStatsService.java  # 用户统计服务
│   │   ├── UserTransactionService.java # 用户事务服务
│   │   └── WebSocketService.java  # WebSocket服务
│   ├── ssh/                        # SSH终端交互
│   │   ├── CommandManager.java    # 命令管理器
│   │   ├── commands/              # 命令实现
│   │   ├── CustomCommand.java     # 自定义命令
│   │   └── InteractionConnect.java # 交互连接
│   ├── util/                       # 工具类
│   │   ├── CacheUtil.java         # 缓存工具
│   │   ├── DatabaseMigrationUtil.java # 数据库迁移工具
│   │   ├── IpUtil.java            # IP工具
│   │   ├── JwtUtil.java           # JWT工具
│   │   ├── ResultUtil.java        # 结果工具
│   │   ├── SecurityUtil.java      # 安全工具
│   │   ├── SqlInjectionUtils.java # SQL注入防护工具
│   │   ├── ValidationUtils.java   # 输入验证工具
│   │   ├── SecurityAuditUtils.java # 安全审计工具
│   │   ├── UrlPermitUtil.java     # URL权限工具
│   │   └── UserStatsUtil.java     # 用户统计工具
│   └── vo/                         # 视图对象
│       ├── article/                # 文章VO
│       ├── auth/                   # 认证VO
│       ├── chat/                   # 聊天VO
│       ├── chatList/               # 聊天列表VO
│       ├── comment/                # 评论VO
│       ├── contact/                # 联系人VO
│       ├── file/                   # 文件VO
│       ├── group/                  # 群组VO
│       ├── login/                  # 登录VO
│       ├── message/                # 消息VO
│       ├── user/                   # 用户VO
│       └── video/                  # 视频VO
├── src/main/resources/             # 资源文件
│   ├── application.yml             # 主配置文件
│   ├── es/                         # Elasticsearch配置
│   │   └── README.md               # ES配置说明
│   ├── Mapper/                     # MyBatis XML映射文件 (14个)
│   │   ├── ArticleCommentMapper.xml # 文章评论Mapper映射
│   │   ├── ArticleMapper.xml       # 文章Mapper映射
│   │   ├── AuthMapper.xml          # 认证Mapper映射
│   │   ├── ChatListMapper.xml      # 聊天列表Mapper映射
│   │   ├── ContactMapper.xml       # 联系人Mapper映射
│   │   ├── FileMapper.xml          # 文件Mapper映射
│   │   ├── FileRecordMapper.xml    # 文件记录Mapper映射
│   │   ├── GroupMapper.xml         # 群组Mapper映射
│   │   ├── MessageMapper.xml       # 消息Mapper映射
│   │   ├── MessageReactionMapper.xml # 消息反应Mapper映射
│   │   ├── NotificationMapper.xml  # 通知Mapper映射
│   │   ├── UserFollowMapper.xml    # 用户关注Mapper映射
│   │   ├── UserMapper.xml          # 用户Mapper映射
│   │   └── UserStatsMapper.xml     # 用户统计Mapper映射
│   └── sql/                        # 数据库初始化脚本
├── src/test/java/com/web/         # 测试代码
│   ├── Config/                    # 测试配置 (1个)
│   │   └── [测试配置文件]
│   ├── integration/               # 集成测试 (8个)
│   │   ├── ApiResponseConsistencyTest.java # API响应一致性测试
│   │   ├── BasicFunctionalityTest.java # 基础功能测试
│   │   ├── ComprehensiveSystemValidationTest.java # 系统综合验证测试
│   │   ├── CoreFeaturesIntegrationTest.java # 核心功能集成测试
│   │   ├── DatabaseSchemaIntegrationTest.java # 数据库架构集成测试
│   │   ├── NewFeaturesIntegrationTest.java # 新功能集成测试
│   │   ├── NotificationSystemValidationTest.java # 通知系统验证测试
│   │   └── SystemValidationTest.java # 系统验证测试
│   ├── mapper/                    # Mapper测试 (3个)
│   │   └── [Mapper测试类]
│   └── performance/               # 性能测试 (3个)
│       └── [性能测试类]
├── Vue/                           # 前端Vue项目
├── mvnw                           # Maven Wrapper (Linux/Mac)
├── mvnw.cmd                       # Maven Wrapper (Windows)
├── pom.xml                        # Maven项目配置
├── package.json                   # Node.js项目配置
├── package-lock.json              # Node.js依赖锁定
└── README.md                      # 项目文档
---

## API 文档

### 🔐 认证接口
- `POST /register` - 用户注册
- `POST /login` - 用户登录
- `POST /logout` - 用户登出
- `GET /user` - 获取用户信息
- `GET /user/info` - 获取用户详细信息
- `PUT /user/info` - 更新用户信息
- `GET /list` - 获取用户列表

### 💬 聊天接口
- `POST /send` - 发送消息
- `POST /record` - 获取消息记录
- `POST /recall` - 撤回消息

### 👥 群组管理
- `POST /create` - 创建群组
- `GET /my-list` - 获取我的群组
- `POST /{id}/invite` - 邀请成员
- `DELETE /{id}` - 解散群组

### 📝 文章管理
- `POST /articles/new` - 发布文章
- `GET /articles/getall` - 获取文章列表（支持排序、分页）
- `GET /articles/search` - 搜索文章
- `GET /articles/recommended` - 获取推荐文章
- `GET /articles/categories` - 获取文章分类
- `POST /articles/{id}/favorite` - 收藏文章
- `DELETE /articles/{id}/favorite` - 取消收藏文章
- `GET /articles/{id}/favorite/status` - 检查收藏状态
- `GET /articles/favorites` - 获取用户收藏列表
- `PUT /articles/{id}` - 更新文章
- `DELETE /articles/{id}` - 删除文章

### 💬 文章评论
- `GET /articles/{articleId}/comments` - 获取文章评论列表
- `POST /articles/{articleId}/comments` - 添加文章评论
- `DELETE /articles/{articleId}/comments/{commentId}` - 删除文章评论
- `GET /articles/{articleId}/comments/count` - 获取文章评论数量

### 🔍 搜索接口
- `GET /api/search/messages` - 搜索消息 (Elasticsearch)
- `GET /api/search/users` - 搜索用户
- `GET /api/search/groups` - 搜索群组
- `GET /api/search/articles` - 搜索文章
- `GET /api/search/all` - 综合搜索 (所有类型)

### 🔔 通知接口
- `GET /notifications` - 获取通知列表
- `POST /notifications/read-all` - 标记所有通知为已读

### 📁 文件管理
- 文件上传、下载和管理相关接口
- 支持多文件上传和云存储集成

> 📖 **详细API文档**: 请查看各个Controller类的注释，或使用Swagger UI (如果配置了) 查看完整的API文档。

---

## 数据库结构

项目使用 **MySQL 8.0+** 数据库，包含以下主要数据表：

### 核心数据表
- **user** - 用户信息表
- **article** - 文章内容表
- **message** - 聊天消息表
- **group_info** - 群组信息表
- **group_member** - 群组成员表
- **notification** - 系统通知表
- **chat_list** - 聊天会话列表

### 数据库特性
- 🔄 **自动初始化**: 应用启动时自动创建数据库和表结构
- 📜 **完整SQL脚本**: 提供完整的数据库初始化脚本
- 🔧 **环境隔离**: 支持开发和生产环境配置
- 📊 **索引优化**: 为常用查询字段添加适当索引

> 📄 **详细表结构**: 请查看 `src/main/resources/sql/init_database.sql` 文件中的完整DDL定义。

---

## 前端技术栈

### 技术架构
- **框架**: Vue 3.0 + Composition API
- **构建工具**: Vite 5.x
- **状态管理**: Pinia 2.x
- **UI组件库**: Element Plus
- **HTTP客户端**: Axios
- **路由管理**: Vue Router 4.x

### 前端项目结构 (Vue/)
Vue/ (51个Vue和JS文件)
├── src/                         # 源代码目录
│   ├── api/                     # API接口封装
│   │   ├── axiosInstance.js     # Axios实例配置
│   │   ├── index.js             # API统一导出
│   │   └── modules/             # API模块 (11个)
│   │       ├── article.js       # 文章API
│   │       ├── auth.js          # 认证API
│   │       ├── chat.js          # 聊天API
│   │       ├── comment.js       # 评论API
│   │       ├── contact.js       # 联系人API
│   │       ├── fileManagement.js # 文件管理API
│   │       ├── follow.js         # 关注API
│   │       ├── group.js         # 群组API
│   │       ├── notification.js  # 通知API
│   │       ├── search.js        # 搜索API
│   │       └── user.js          # 用户API
│   ├── article/                 # 文章相关页面 (5个)
│   │   ├── ArticleEdit.vue      # 文章编辑
│   │   ├── ArticleMain.vue      # 文章主页
│   │   ├── ArticleManage.vue    # 文章管理
│   │   ├── ArticleRead.vue      # 文章阅读
│   │   └── ArticleWrite.vue     # 文章写作
│   ├── assets/                  # 静态资源
│   │   ├── apple-style.css      # Apple风格样式
│   │   └── main.css             # 主样式文件
│   ├── auth/                    # 认证相关页面 (4个)
│   │   ├── HelpCenter.vue       # 帮助中心
│   │   ├── Register.vue         # 注册页面
│   │   ├── UserInform.vue       # 用户信息
│   │   └── usermain.vue         # 用户主页
│   ├── Chat/                    # 聊天相关页面
│   │   └── ChatPage.vue         # 聊天页面
│   ├── contact/                 # 联系人页面
│   │   └── ContactPage.vue      # 联系人页面
│   ├── group/                   # 群组页面
│   │   └── GroupPage.vue        # 群组页面
│   ├── layout/                  # 布局组件
│   │   ├── AsideMenu.vue        # 侧边菜单
│   │   ├── components/          # 布局子组件
│   │   │   └── NotificationBell.vue # 通知铃铛
│   │   └── Layout.vue           # 主布局
│   ├── main.js                  # Vue应用入口
│   ├── router/                  # 路由配置
│   │   └── index.js             # 路由定义
│   ├── search/                  # 搜索页面
│   │   └── SearchPage.vue       # 搜索页面
│   ├── stores/                  # Pinia状态管理
│   │   ├── authStore.js         # 认证状态
│   │   ├── chatStore.js         # 聊天状态
│   │   └── notificationStore.js # 通知状态
│   ├── utils/                   # 工具类
│   ├── value/                   # 值组件 (2个)
│   │   ├── usevalue.vue         # 使用值组件
│   │   └── value.vue            # 值组件
│   ├── video/                   # 视频组件
│   │   └── Video.vue            # 视频播放器
│   └── views/                   # 视图页面 (13个)
│       ├── chat/                # 聊天视图
│       │   └── ChatWindow.vue   # 聊天窗口
│       ├── FileTransfer.vue     # 文件传输
│       ├── Forget.vue           # 忘记密码
│       ├── group/               # 群组视图
│       │   └── GroupDetail.vue  # 群组详情
│       ├── Groups.vue           # 群组列表
│       ├── Login.vue            # 登录页面
│       ├── NotFound.vue         # 404页面
│       ├── NotificationListPage.vue # 通知列表
│       ├── Register.vue         # 注册页面
│       ├── Settings.vue         # 设置页面
│       ├── TestNotificationPage.vue # 通知测试页面
│       ├── UserDetail.vue       # 用户详情
│       └── Video.vue            # 视频页面
├── public/                      # 静态资源
│   └── favicon.ico              # 网站图标
├── package.json                 # 项目依赖配置
├── package-lock.json            # 依赖锁定文件
├── vite.config.js               # Vite构建配置
└── jsconfig.json                # JavaScript配置

### 开发启动

---

cd Vue
npm install
npm run dev

---

### 主要功能模块

- 🔐 用户认证 (登录/注册/登出)
- 💬 实时聊天 (私聊/群聊)
- 👥 群组管理 (创建/加入/管理)
- 📝 文章系统 (发布/编辑/浏览)
- 🔍 智能搜索 (消息/用户/内容)
- 🔔 通知中心 (消息提醒/系统通知)

## 安全与认证

### 认证机制

- **JWT认证**: 使用JSON Web Token进行身份验证，支持复杂度验证和过期检查
- **Token存储**: 前端通过Pinia状态管理和LocalStorage持久化，支持环境变量配置
- **多重加密**: RSA/AES双重加密保护 + 密钥持久化存储
- **自动刷新**: 支持Token自动刷新机制，异常情况降级处理

### 安全配置

- **密码加密**: BCrypt算法对用户密码进行加密存储
- **请求过滤**: JwtAuthenticationFilter拦截和验证请求，支持401/403处理
- **权限控制**: 基于角色的访问控制(RBAC) + 环境感知权限配置
- **CORS配置**: 开发环境完全开放，生产环境严格域名限制 + SSL强制
- **连接安全**: 数据库SSL连接 + HTTPS强制 + 安全连接池配置
- **账户保护**: 自动检测失败登录尝试，临时锁定账户防止暴力破解
- **输入验证**: 统一验证用户名、密码、邮箱、手机号等所有输入字段
- **SQL注入防护**: 动态检测和防止SQL注入攻击
- **XSS防护**: 输入内容自动检测和过滤XSS攻击模式
- **敏感词过滤**: 内置敏感词检测，防止不当内容发布
- **安全审计**: 记录所有安全相关事件，包括登录、注册、权限变更等

### 敏感信息保护

- **环境变量**: 所有敏感配置通过环境变量提供，启动时验证完整性
- **配置隔离**: 开发/生产环境配置完全分离，避免配置泄露
- **密钥管理**: JWT/AES密钥环境变量配置 + RSA密钥文件持久化
- **启动验证**: 应用启动时自动验证安全配置，生产环境强制要求

---

## 常见问题

### 🚀 启动相关

**Q: 启动时提示数据库连接失败？**
A: 确保MySQL服务正在运行，检查数据库连接配置。DatabaseInitializer会自动创建数据库，但需要MySQL服务可用。

**Q: 数据库初始化失败怎么办？**
A: 检查MySQL用户是否有CREATE DATABASE权限，确认字符集支持utf8mb4。查看启动日志获取详细错误信息。

**Q: 如何跳过自动数据库初始化？**
A: 设置环境变量 `spring.profiles.active=prod` 或 `spring.profiles.active=production`，生产环境会跳过自动初始化。

**Q: 启动后访问不到应用？**
A: 检查端口8080是否被占用，确认防火墙设置。数据库初始化成功后应用才会完全启动。

### 🗄️ 数据库相关

**Q: DatabaseInitializer 如何工作？**
A: 应用启动时自动检查数据库和表是否存在，不存在则创建。只在非生产环境执行，确保安全。

**Q: 如何查看数据库初始化日志？**
A: 启动应用时查看控制台输出，包含详细的创建过程和结果报告。

**Q: 数据库表结构在哪里查看？**
A: 查看 `DatabaseInitializer.java` 中的建表SQL，或使用 `src/main/resources/sql/` 目录下的脚本。

**Q: 如何修改数据库配置？**
A: 通过环境变量或修改application.yml文件。DatabaseInitializer会使用相同的配置。

### 🔐 认证相关

**Q: 登录后提示Token无效？**
A: 检查JWT_SECRET配置，确保生产环境使用强密钥（至少32字符），确认前端正确发送Authorization头。

**Q: 如何重置管理员密码？**
A: 直接在数据库中修改user表的password字段（使用BCrypt加密）。

**Q: 生产环境启动时提示JWT密钥不安全？**
A: 生产环境必须设置强JWT_SECRET（至少32字符，包含大小写字母、数字、特殊字符）。

**Q: RSA密钥丢失怎么办？**
A: 删除keystore目录，应用会自动生成新的RSA密钥对并保存，下次重启将加载已保存的密钥。

### 📱 前端相关

**Q: 前端启动失败？**
A: 确保Node.js版本为16+，运行 `npm install` 安装依赖。

**Q: 前后端联调问题？**
A: 检查CORS配置，确保后端允许前端域名访问。生产环境需要设置ALLOWED_ORIGINS环境变量。

**Q: 前端环境变量不生效？**
A: 确保使用VITE_前缀的环境变量，重启开发服务器后生效。生产环境通过.env文件配置。

### 🔒 安全配置相关

**Q: 生产环境启动失败，提示缺少安全配置？**
A: 生产环境必须设置所有必需的环境变量：MYSQL_HOST、MYSQL_USERNAME、MYSQL_PASSWORD、JWT_SECRET、AES_SECRET_KEY等。

**Q: 如何生成安全的JWT密钥？**
A: 使用强密码生成器生成至少32字符的密钥，包含大小写字母、数字和特殊字符。例如：`openssl rand -base64 32`

**Q: AES密钥应该多长？**
A: AES密钥至少16字符，建议使用32字符以获得更高安全性。

**Q: 生产环境忘记设置ALLOWED_ORIGINS怎么办？**
A: 应用启动会失败并提示设置此环境变量。设置格式：`ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com`

### 🐛 其他问题

**Q: 日志在哪里查看？**
A: 开发环境输出到控制台，生产环境保存在logs/weeb-prod.log。

**Q: 如何开启调试模式？**
A: 设置环境变量 `DEBUG=true` 或在IDE中启用调试模式。

**Q: Elasticsearch搜索功能状态？**
A: 已配置并启用。Elasticsearch 8.x 正在 localhost:9200，Redis 3.2.100 正在 6379 端口。

**Q: 如何配置Elasticsearch连接？**
A: 应用已配置为使用 HTTP 连接模式，无需额外的安全配置。

   1. **禁用 ES 安全认证**（重要）：
      - 找到 Elasticsearch 配置文件 `elasticsearch.yml`
      - 添加或修改以下配置：
        ```yaml
        xpack.security.enabled: false
        ```
      - 重启 Elasticsearch 服务

   2. **安装 IK 中文分词器**（可选，用于中文搜索优化）：
      - 下载 IK 分词器插件：https://github.com/medcl/elasticsearch-analysis-ik/releases
      - 安装命令（Windows）：
        ```bash
        .\bin\elasticsearch-plugin.bat install file:///D:/path/to/elasticsearch-analysis-ik-8.18.6.zip
        ```
      - 重启 Elasticsearch 服务

   详见 `src/main/resources/es/README.md`

**Q: 如何使用搜索功能？**
A: 使用API `/api/search/messages?q=关键词` 进行消息搜索。支持中文分词搜索。

**Q: 如果搜索服务停止会怎样？**
A: 不会影响核心聊天功能，应用会继续正常运行，只是搜索功能暂时不可用。

### 🔒 安全相关

**Q: 如何配置账户锁定机制？**
A: 账户锁定功能自动启用。连续失败登录尝试超过阈值后，账户会被临时锁定。锁定时间和失败尝试次数在 `SecurityConfig` 中配置。

**Q: 输入验证失败怎么办？**
A: 输入验证使用 `ValidationUtils` 工具类。验证失败会返回具体的错误信息，包括字段要求和格式规范。查看API响应中的错误消息获取详细信息。

**Q: 如何查看安全审计日志？**
A: 安全事件通过 `SecurityAuditUtils` 记录，包括登录失败、账户锁定、SQL注入尝试等。日志输出到控制台和日志文件，包含IP地址、用户代理等信息。

**Q: 如何自定义敏感词过滤？**
A: 敏感词列表在应用配置文件中维护。可以通过修改 `SensitiveWordConfig` 来添加或移除敏感词。系统会自动检测用户输入中的敏感内容。

**Q: SQL注入防护是如何工作的？**
A: 系统使用 `SqlInjectionUtils` 工具类进行动态检测，包括：
- 检测SQL关键字和特殊字符
- 验证参数化查询
- 检查动态SQL拼接
- 转义危险字符

**Q: 如何增强密码安全策略？**
A: 密码策略在 `SecurityConfig.PasswordPolicy` 中定义，包括：
- 最小长度要求
- 大小写字母要求
- 数字和特殊字符要求
- 常见密码黑名单检查

---

## 部署指南

### 🚀 快速部署（推荐）

1. **克隆项目**

```bash
git clone <repository-url>
cd weeb
```

2. **配置环境变量**

```bash
# 复制环境变量配置模板
cp env-example.txt .env

# 编辑 .env 文件，设置你的实际值
# 注意：Elasticsearch 已配置为 HTTP 模式，无需设置密码
```

4. **一键启动**

```bash
# Linux/Mac
./start.sh prod

# Windows
start.bat prod
```

### 🐳 Docker 部署

```bash
# 使用Docker Compose
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f weeb-app
```

### 📦 JAR包部署

```bash
# 构建项目
mvn clean package -DskipTests

# 运行JAR包
java -jar target/weeb-*.jar --spring.profiles.active=prod
```

### 🌐 Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/vue/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket代理
    location /ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## 贡献指南

### 开发流程
1. **Fork 项目** 到你的GitHub账户
2. **创建功能分支**：`git checkout -b feature/AmazingFeature`
3. **提交更改**：`git commit -m 'Add some AmazingFeature'`
4. **推送分支**：`git push origin feature/AmazingFeature`
5. **创建 Pull Request**

### 代码规范
- **后端**: 遵循阿里巴巴Java开发规范
- **前端**: 使用ESLint + Prettier格式化代码
- **提交信息**: 使用清晰的英文描述，格式为 `type(scope): description`

### 测试规范
```bash
# 后端单元测试
mvn test

# 前端测试
cd Vue && npm run test

# 端到端测试
cd Vue && npm run test:e2e
```

### 文档维护
- 🔄 API变更时同步更新本文档
- ✨ 新功能添加详细使用说明
- ⚙️ 配置变更更新部署指南
- 🐛 问题修复更新FAQ部分

---

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

## 联系我们

- 📧 **邮箱**: [your-email@example.com](mailto:your-email@example.com)
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/your-repo/weeb/issues)
- 📖 **项目文档**: [Wiki](https://github.com/your-repo/weeb/wiki)

---

> **WEEB Team** ❤️ 为现代化即时通信与内容管理系统而努力
>
> 最后更新时间：2025年10月 - 安全增强与功能完善版本

---

## 🔒 安全增强更新

### 最新安全特性 (2025年10月)

- ✅ **SQL注入防护**: 新增 `SqlInjectionUtils` 工具类，动态检测和防止SQL注入攻击
- ✅ **输入验证系统**: 实现 `ValidationUtils` 统一验证框架，覆盖所有敏感输入字段
- ✅ **安全审计日志**: 引入 `SecurityAuditUtils`，记录所有安全相关事件
- ✅ **账户锁定机制**: 自动检测暴力破解尝试，临时锁定可疑账户
- ✅ **XSS防护**: 增强输入内容过滤，防止跨站脚本攻击
- ✅ **敏感词过滤**: 内置内容审核机制，维护社区健康环境
- ✅ **文件上传安全**: 严格验证文件类型、大小和内容安全性
- ✅ **增强认证**: 改进用户注册和登录流程，增加多重验证
- ✅ **文章系统完善**: 新增评论、收藏、分类、搜索等完整社交功能
- ✅ **性能优化**: 只读操作添加事务优化，提升系统响应速度
- ✅ **AI功能集成**: 新增AiChatService，支持智能对话功能
- ✅ **SSH终端功能**: 集成Apache SSHD，支持远程命令执行和终端交互
- ✅ **文件分享系统**: 完善文件上传、管理和分享功能
- ✅ **用户关注系统**: 实现用户互相关注和社交网络功能

### 架构改进

- 🏗️ **分层架构优化**: 严格遵循Controller-Service-Mapper模式
- 📊 **数据库优化**: 添加索引和查询优化，提升数据访问性能
- 🔄 **缓存策略**: 合理使用Redis缓存，减少数据库压力
- 📝 **日志系统**: 结构化日志输出，支持安全事件追踪和性能监控
