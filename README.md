# WEEB - 现代化社交平台

## 项目概述

WEEB是一个基于Vue 3 + Spring Boot的前后端分离社交平台，采用微服务架构设计，集成了即时通信、内容管理、AI对话、SSH终端等核心功能。平台致力于提供高质量的用户体验，注重代码质量和系统架构的清晰性。

### 🎯 设计理念
- **分层清晰**: 严格遵循前后端分离、职责单一原则
- **高内聚低耦合**: 模块化设计，便于维护和扩展
- **接口标准化**: RESTful API设计，统一的响应格式
- **性能优化**: 支持高并发，优秀的用户体验
- **安全优先**: 完整的权限控制和安全审计机制

### 🚀 核心特性
- **实时聊天**: 支持一对一和群组聊天，WebSocket实时通信
- **内容管理**: 文章发布、评论、点赞、收藏、版本控制功能
- **社交互动**: 用户关注、好友系统、群组管理、联系人分组
- **AI集成**: AI聊天对话、文本分析、翻译、摘要生成、情感分析
- **搜索功能**: 基于Elasticsearch的全局搜索
- **SSH终端**: 内置SSH终端功能，支持命令执行
- **权限管理**: 基于用户类型的权限控制(ADMIN/USER/VIP)
- **响应式设计**: 完美适配PC和移动端

---

## 技术栈

### 前端技术栈
- **Vue 3.5.13** - 渐进式JavaScript框架
- **Vue Router 4.5.0** - 路由管理
- **Pinia 2.1.7** - 状态管理
- **Element Plus 2.11.5** - UI组件库
- **Axios 1.7.9** - HTTP客户端
- **Vite 5.4.11** - 构建工具
- **Quill 2.0.3** - 富文本编辑器
- **SockJS 1.6.1 & STOMP 7.2.1** - WebSocket通信
- **Day.js 1.11.18** - 日期处理库
- **Lodash-es 4.17.21** - 工具函数库
- **DOMPurify 3.3.0** - HTML内容净化

### 后端技术栈
- **Java 17** - 开发语言
- **Spring Boot 3.5.4** - 应用框架
- **Spring Security** - 安全框架
- **JWT 0.11.5** - JSON Web Token认证
- **MySQL 8.0+** - 关系型数据库
- **Redis 7.0+** - 缓存和会话存储
- **Elasticsearch 8.18.6** - 搜索引擎
- **MyBatis-Plus 3.5.8** - ORM框架
- **WebSocket** - 实时通信
- **Spring AI 1.0.0-M1** - AI集成框架
- **Apache SSHD 2.14.0** - SSH终端服务
- **Hutool 5.8.18** - Java工具库
- **IP2Region 2.7.0** - IP地理位置查询

### 开发工具
- **Maven 3.9+** - Java项目管理
- **npm/yarn** - Node.js包管理
- **Git** - 版本控制
- **Docker** - 容器化部署
- **Lombok** - Java代码简化

---

## 项目结构

```
weeb/
├── Vue/                           # 前端项目
│   ├── src/
│   │   ├── api/                   # API接口封装
│   │   │   ├── modules/           # API模块
│   │   │   └── axiosInstance.js   # Axios实例配置
│   │   ├── components/            # 公共组件
│   │   │   ├── common/            # Apple系列组件
│   │   │   ├── message/           # 消息组件
│   │   │   └── ...
│   │   ├── views/                 # 页面组件
│   │   │   ├── chat/              # 聊天相关页面
│   │   │   ├── auth/              # 认证页面
│   │   │   └── ...
│   │   ├── stores/                # Pinia状态管理
│   │   ├── router/                # 路由配置
│   │   ├── utils/                 # 工具函数
│   │   └── assets/                # 静态资源
│   ├── public/                    # 公共静态文件
│   └── package.json               # 前端依赖配置
├── src/                           # 后端源码
│   ├── main/java/com/web/
│   │   ├── Controller/            # REST控制器
│   │   ├── service/               # 业务服务接口
│   │   │   └── Impl/              # 服务实现
│   │   ├── mapper/                # 数据访问层
│   │   ├── model/                 # 数据模型
│   │   ├── dto/                   # 数据传输对象
│   │   ├── vo/                    # 视图对象
│   │   ├── annotation/            # 自定义注解
│   │   ├── exception/             # 异常处理
│   │   ├── security/              # 安全配置
│   │   └── util/                  # 工具类
│   └── main/resources/
│       ├── Mapper/                # MyBatis XML映射
│       ├── application.yml        # 应用配置
│       └── db/                    # 数据库脚本
├── target/                        # Maven构建输出
├── Vue/dist/                      # 前端构建输出
├── pom.xml                        # Maven配置
├── package.json                   # 根级依赖
├── vite.config.js                 # Vite配置
├── rule.txt                       # 开发规范
├── frontend.md                    # 前端文档
├── backend.md                     # 后端API文档
└── todo.txt                       # 待办事项
```

---

## 快速开始

### 环境要求
- **Java**: JDK 17+ (推荐17 LTS)
- **Node.js**: 18+ (推荐20 LTS)
- **MySQL**: 8.0+ (推荐8.0.33+)
- **Redis**: 7.0+ (推荐7.2+)
- **Elasticsearch**: 8.18.6 (用于搜索功能)
- **Ollama**: 0.3+ (可选，用于本地AI功能，默认端口11434)
- **DeepSeek API**: API密钥 (可选，用于云端AI功能)

### 后端启动
1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd weeb
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE weeb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **安装和启动依赖服务**
   ```bash
   # 启动MySQL服务 (端口3306)
   # 启动Redis服务 (端口6379)
   # 启动Elasticsearch服务 (端口9200)
   # 可选: 启动Ollama服务 (端口11434)
   ```

4. **修改配置**
   编辑 `src/main/resources/application.yml`，根据需要修改：
   - 数据库连接信息 (默认: localhost:3306, root/1234)
   - Redis连接信息 (默认: localhost:6379)
   - Elasticsearch连接信息 (默认: localhost:9200)
   - AI服务配置 (Ollama或DeepSeek)
   - JWT密钥配置

5. **启动后端**
   ```bash
   # 开发环境启动
   mvn spring-boot:run

   # 或生产环境启动
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

### 前端启动
1. **安装依赖**
   ```bash
   cd Vue
   npm install
   ```

2. **启动开发服务器**
   ```bash
   # 开发环境启动
   npm run dev

   # 或构建生产版本
   npm run build
   npm run preview
   ```

3. **访问应用**
   打开浏览器访问 `http://localhost:5173`

4. **初始用户账号**
   系统会自动创建以下测试用户账号：

   | 用户名 | 密码 | 角色 | 邮箱 | 昵称 |
   |--------|------|------|------|------|
   | admin | admin123 | 管理员 | admin@weeb.com | 系统管理员 |
   | testuser | test123 | 普通用户 | test@weeb.com | 测试用户 |
   | alice | password100 | 普通用户 | alice@weeb.com | 爱丽丝 |
   | bob | password200 | 普通用户 | bob@weeb.com | 鲍勃 |
   | charlie | password300 | 普通用户 | charlie@weeb.com | 查理 |
   | diana | password400 | 普通用户 | diana@weeb.com | 戴安娜 |
   | eve | password500 | 普通用户 | eve@weeb.com | 伊芙 |

   **用户类型说明：**
   - **ADMIN**: 系统管理员，拥有所有权限
   - **USER**: 普通用户，基础功能权限
   - **VIP**: VIP用户，扩展功能权限（预留）

### Docker部署（可选）
```bash
# 构建后端镜像
docker build -t weeb-backend .

# 构建前端镜像
cd Vue && docker build -t weeb-frontend .

# 使用docker-compose启动
docker-compose up -d
```

---

## 核心功能

### 1. 用户系统
- **注册登录**: 支持邮箱注册、JWT认证、密码重置
- **个人资料**: 头像上传、信息编辑、隐私设置、等级系统
- **用户统计**: 用户行为数据统计、成长激励
- **权限管理**: 基于用户类型的权限控制（ADMIN/USER/VIP）

### 2. 聊天系统
- **实时通信**: WebSocket + STOMP实现即时消息
- **多会话支持**: 私聊、群聊、消息线程、联系人分组
- **消息管理**: 发送、撤回、编辑、反应表情、文件传输
- **在线状态**: 用户在线状态显示、心跳检测、连接监控
- **消息搜索**: 基于Elasticsearch的消息全文搜索

### 3. 内容管理
- **文章发布**: Quill富文本编辑器、草稿保存、版本控制
- **互动功能**: 点赞、收藏、评论、分享、举报
- **内容审核**: 敏感词过滤、内容合规性检查
- **文章标签**: AI生成标签、关键词提取、内容分类

### 4. 社交功能
- **好友系统**: 添加好友、好友申请、好友分组管理、拉黑功能
- **关注系统**: 用户关注、粉丝列表、互关关系
- **群组管理**: 创建群组、成员邀请、权限设置、群组搜索
- **联系人管理**: 联系人分组、在线状态、最近聊天

### 5. AI功能
- **AI对话**: 支持Ollama本地模型和DeepSeek云端API
- **文本处理**: 摘要生成、润色、翻译、情感分析
- **内容生成**: 标题建议、标签生成、内容大纲
- **智能回复**: 基于上下文的回复建议生成
- **合规检查**: 内容合规性检测、敏感词过滤

### 6. 系统功能
- **搜索功能**: Elasticsearch全局搜索（用户、群组、文章、消息）
- **SSH终端**: 内置SSH终端功能、命令执行
- **通知中心**: 系统通知、消息提醒、@提及
- **数据统计**: 用户行为分析、系统监控面板
- **安全审计**: 操作日志记录、安全事件监控

---

## 开发规范

项目严格遵循统一的开发规范，所有开发者必须遵守以下规范：

### 📋 开发规范概览
- **[rule.txt](rule.txt)** - 完整的开发规范文档
- **[frontend.md](frontend.md)** - 前端页面和API文档
- **[backend.md](backend.md)** - 后端API接口文档

### 🔧 核心规范
1. **代码质量**: 遵循分层架构，单一职责原则
2. **接口设计**: RESTful API，统一响应格式
3. **安全要求**: 输入验证，权限控制，安全审计
4. **性能优化**: 数据库优化，前端性能，缓存策略
5. **文档同步**: 代码变更必须同步更新相关文档

### 🚫 禁止事项
- 随意修改端口配置
- 绕过安全验证
- 在Controller层编写复杂业务逻辑
- 不使用事务进行多表操作
- 提交包含安全隐患的代码
- 随意修改用户类型定义（ADMIN/USER/VIP）
- 在非管理员初始化代码中创建管理员用户

---

## API文档

### 后端API
详细的API接口文档请参考 **[backend.md](backend.md)**

主要API模块：
- **认证接口**: `/api/auth/*` - 用户注册、登录、权限管理
- **用户接口**: `/api/users/*` - 用户信息、个人资料管理
- **聊天接口**: `/api/chats/*` - 实时聊天、消息管理
- **内容接口**: `/api/articles/*` - 文章发布、评论管理
- **社交接口**: `/api/contacts/*` `/api/groups/*` - 好友、群组管理
- **搜索接口**: `/api/search/*` - 全局搜索功能

### 前端页面
详细的前端页面文档请参考 **[frontend.md](frontend.md)**

主要页面模块：
- **认证页面**: 登录、注册、忘记密码
- **聊天页面**: 实时聊天、消息管理
- **内容页面**: 文章阅读、发布、管理
- **社交页面**: 联系人、群组、个人资料
- **系统页面**: 搜索、设置、通知中心

---

## 部署说明

### 开发环境
```bash
# 确保依赖服务已启动
# MySQL (3306), Redis (6379), Elasticsearch (9200)

# 后端开发启动
mvn spring-boot:run

# 前端开发启动 (新终端)
cd Vue && npm run dev

# 访问地址
# 前端: http://localhost:5173
# 后端API: http://localhost:8080
# WebSocket: ws://localhost:8081/ws
```

### 生产环境
1. **后端构建和部署**
   ```bash
   # 构建JAR包
   mvn clean package -DskipTests

   # 生产环境启动
   java -jar target/WEEB-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

2. **前端构建和部署**
   ```bash
   cd Vue

   # 构建生产版本
   npm run build

   # 静态文件部署到Web服务器 (如Nginx)
   # 将dist目录内容复制到Web服务器静态文件目录
   ```

3. **Docker部署**
   ```bash
   # 构建镜像
   docker build -t weeb-backend .
   cd Vue && docker build -t weeb-frontend .

   # 运行容器
   docker run -d -p 8080:8080 --name weeb-backend weeb-backend
   docker run -d -p 80:80 --name weeb-frontend weeb-frontend
   ```

4. **服务架构说明**
   - **后端服务**: 端口8080 (HTTP API + WebSocket 8081)
   - **前端服务**: 端口80/443 (静态文件)
   - **数据库**: MySQL 3306
   - **缓存**: Redis 6379
   - **搜索引擎**: Elasticsearch 9200

### 环境变量
```bash
# 数据库配置 (生产环境必需)
MYSQL_URL=jdbc:mysql://localhost:3306/weeb?useSSL=false&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_mysql_password

# Redis配置 (生产环境必需)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT配置 (生产环境必需)
JWT_SECRET=your_strong_jwt_secret_key_here
JWT_EXPIRATION=86400000

# Elasticsearch配置 (可选)
ES_HOST=localhost
ES_PORT=9200

# AI服务配置 (可选)
# Ollama配置
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=gemma3:4b

# 或DeepSeek配置
AI_PROVIDER=deepseek
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=your_deepseek_api_key
DEEPSEEK_CHAT_MODEL=deepseek-chat

# SSH终端配置 (可选)
SSH_SERVER_ENABLED=true
SSH_SERVER_PORT=2222

# 应用配置
SPRING_PROFILES_ACTIVE=prod
```

---

## 开发指南

### 代码提交
1. **分支管理**: 遵循Git Flow工作流
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **提交规范**
   ```bash
   git commit -m "feat: 添加用户注册功能"
   ```

3. **代码审查**: 所有变更必须经过代码审查

### 测试要求
- **单元测试**: 核心业务逻辑必须包含单元测试
- **集成测试**: API接口必须通过集成测试
- **端到端测试**: 重要功能流程需要E2E测试

### 文档维护
- **API变更**: 必须同步更新backend.md
- **页面变更**: 必须同步更新frontend.md
- **规范更新**: 新规范必须添加到rule.txt

---

## 贡献指南

### 如何贡献
1. **Fork项目** 到你的GitHub账户
2. **创建特性分支** `git checkout -b feature/AmazingFeature`
3. **提交变更** `git commit -m 'Add some AmazingFeature'`
4. **推送到分支** `git push origin feature/AmazingFeature`
5. **创建Pull Request**

### 贡献规范
- 遵循现有的代码风格和规范
- 添加必要的测试用例
- 更新相关文档
- 确保通过所有测试

### 问题反馈
- 使用GitHub Issues报告bug
- 提供详细的复现步骤
- 包含环境信息和错误日志

---

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

## 联系我们

- **项目主页**: [GitHub Repository]
- **问题反馈**: [GitHub Issues]
- **邮箱联系**: project@weeb.com

---

## 更新日志

### v0.0.1-SNAPSHOT (2025-01-01)
- ✨ 初始版本发布
- 🚀 实时聊天系统 (WebSocket + STOMP)
- 📝 文章发布系统 (Quill富文本编辑器)
- 👥 好友和群组管理系统
- 🔍 Elasticsearch全局搜索功能
- 🤖 AI功能集成 (Ollama + DeepSeek)
- 🖥️ SSH终端功能
- 📊 用户等级和统计系统
- 🔒 JWT安全认证
- 🎨 Element Plus响应式UI

### 开发计划
- [ ] 移动端App开发 (React Native/Flutter)
- [ ] 视频通话功能 (WebRTC)
- [ ] 国际化支持 (i18n)
- [ ] 第三方登录集成 (微信/QQ/微博)
- [ ] 数据分析面板和可视化
- [ ] 文件存储服务 (OSS/MinIO)
- [ ] 消息推送服务 (极光/个推)
- [ ] 缓存优化和性能监控
- [ ] API文档自动生成 (Swagger)
- [ ] 单元测试和集成测试完善

### 技术债务
- [ ] 数据库表结构优化
- [ ] 缓存策略完善
- [ ] 异常处理统一
- [ ] 日志系统完善
- [ ] 代码注释补全

---

**最后更新**: 2025年10月30日

感谢使用WEEB！🎉
