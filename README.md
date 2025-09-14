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

- [项目概述](#项目概述)
- [快速开始](#快速开始)
- [安全配置](#安全配置)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [项目结构](#项目结构)
- [API文档](#api文档)
- [部署指南](#部署指南)
- [常见问题](#常见问题)
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
DOUBAO_API_KEY=your_doubao_api_key

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

---

## 项目概述

WEEB 是一个现代化的即时通信与内容管理系统，专为团队协作和内容创作而设计。系统集成了实时聊天、群组管理、文章发布、文件共享等核心功能，提供完整的企业级解决方案。

### 核心功能
- 👥 **用户管理**: 注册登录、个人资料管理
- 💬 **实时聊天**: 私聊、群聊、消息推送
- 👥 **群组协作**: 创建群组、成员管理、权限控制
- 📝 **内容创作**: 文章发布、编辑、点赞收藏
- 🔍 **智能搜索**: 全文搜索、消息检索
- 📁 **文件管理**: 多文件上传、云存储
- 🔔 **通知系统**: 实时通知、消息提醒
- 📊 **数据统计**: 用户数据、文章统计

### 技术特色
- 🏗️ **自动初始化**: 一键启动，自动创建数据库和表结构
- 🔧 **环境隔离**: 开发/生产环境配置分离
- 🚀 **高性能**: Spring Boot 3 + MySQL优化
- 🔒 **安全可靠**: JWT认证、权限控制
- 📱 **响应式**: 现代化前端界面

---

## 快速开始

### 🚀 一键启动（推荐）

项目支持**一键启动**，自动检查环境并初始化数据库：

```bash
# Linux/Mac
./start.sh

# Windows
start.bat

# 生产环境启动
./start.sh prod    # Linux/Mac
start.bat prod     # Windows
```

启动脚本会自动：
- ✅ 检查 Java 和 Maven 环境
- 🔍 检查 MySQL 连接
- 🔨 编译项目
- 🏗️ 自动创建数据库（如果不存在）
- 📊 创建所有数据表
- 🚀 启动 Spring Boot 应用

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
- **前端界面**: http://localhost:5173
- **H2 控制台** (开发环境): http://localhost:8080/h2-console

### 📝 默认账号

系统启动后会自动创建测试账号：
- **管理员**: admin / admin123
- **普通用户**: user1 / user123

> 💡 **提示**: 如果启动过程中遇到数据库连接问题，请确保 MySQL 服务正在运行，并且数据库连接参数正确。

---

## 核心功能

### 👥 用户系统
- 用户注册登录（JWT认证）
- 个人资料管理
- 在线状态管理

### 💬 聊天系统
- 实时私聊和群聊
- 消息推送通知
- 文件和图片分享
- 消息搜索和历史记录

### 👥 群组协作
- 创建和管理群组
- 成员邀请和权限管理
- 群组公告和设置

### 📝 内容管理
- 文章发布和编辑
- 点赞收藏功能
- 内容搜索和分类

### 🔍 搜索功能
- 消息全文搜索
- 用户和群组搜索
- 内容标签搜索

### 🔔 通知系统
- 实时消息通知
- 系统公告提醒
- 个人消息推送

---

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.5.4
- **数据库**: MySQL 8.0+ (环境感知自动初始化)
- **ORM**: MyBatis-Plus 3.5.3
- **认证**: JWT (JSON Web Token) + BCrypt密码加密
- **缓存**: Redis 7.0+ (限流 + 原子操作)
- **搜索**: Elasticsearch 8.x (可选)
- **实时通信**: WebSocket + STOMP
- **安全**: RSA/AES加密 + SSL/TLS + CORS控制

### 前端技术
- **框架**: Vue 3.0 + Composition API
- **构建工具**: Vite 5.x + 环境变量支持
- **状态管理**: Pinia 2.x + Token持久化
- **UI组件**: Element Plus + 响应式设计
- **HTTP客户端**: Axios + 拦截器优化 + Token管理
- **路由**: Vue Router 4.x + 认证守卫

### 开发工具
- **构建**: Maven 3.6+
- **JDK**: Java 17+
- **Node.js**: 16+
- **包管理**: npm/yarn

## 项目结构
weeb/
├── src/main/java/com/web/          # 后端主包
│   ├── config/                     # 配置类
│   │   └── DatabaseInitializer.java # 数据库自动初始化
│   ├── controller/                 # REST控制器
│   ├── model/                      # 数据模型
│   ├── mapper/                     # MyBatis Mapper接口
│   ├── service/                    # 业务逻辑层
│   └── util/                       # 工具类
├── src/main/resources/             # 资源文件
│   ├── sql/                        # 数据库初始化脚本
│   │   ├── init_database.sql       # 完整初始化脚本
│   │   └── init_*.sql              # 单独表初始化脚本
│   ├── application.yml             # 主配置文件
│   ├── application-prod.yml        # 生产环境配置
│   └── Mapper/                     # MyBatis XML映射文件
├── Vue/                           # 前端Vue项目
├── start.sh                       # Linux/Mac启动脚本
├── start.bat                      # Windows启动脚本
└── README.md                      # 项目文档
---

## API 文档

### 🔐 认证接口
- `POST /auth/register` - 用户注册
- `POST /auth/login` - 用户登录
- `POST /auth/logout` - 用户登出
- `GET /auth/user` - 获取用户信息

### 👥 用户管理
- `GET /api/user/info` - 获取用户详细信息
- `PUT /api/user/info` - 更新用户信息
- `GET /api/user/list` - 获取用户列表

### 💬 聊天接口
- `POST /api/message/send` - 发送消息
- `GET /api/message/record` - 获取消息记录
- `POST /api/message/recall` - 撤回消息

### 👥 群组管理
- `POST /api/group/create` - 创建群组
- `GET /api/group/my-list` - 获取我的群组
- `POST /api/group/{id}/invite` - 邀请成员
- `DELETE /api/group/{id}` - 解散群组

### 📝 文章管理
- `POST /api/article/new` - 发布文章
- `GET /api/article/getall` - 获取文章列表
- `PUT /api/article/{id}` - 更新文章
- `DELETE /api/article/{id}` - 删除文章

### 🔍 搜索接口
- `GET /api/search/messages` - 搜索消息
- `GET /api/search/users` - 搜索用户

### 🔔 通知接口
- `GET /api/notifications` - 获取通知列表
- `POST /api/notifications/read-all` - 标记所有通知为已读

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

### 项目结构 (Vue/)
Vue/
├── src/                         # 源代码目录
│   ├── api/                     # API接口封装
│   │   ├── axiosInstance.js     # Axios实例配置
│   │   └── modules/             # API模块
│   ├── router/                  # 路由配置
│   ├── stores/                  # Pinia状态管理
│   ├── views/                   # 页面组件
│   └── components/              # 公共组件
├── public/                      # 静态资源
└── package.json                 # 项目依赖

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

### 敏感信息保护

- **环境变量**: 所有敏感配置通过环境变量提供，启动时验证完整性
- **配置隔离**: 开发/生产环境配置完全分离，避免配置泄露
- **密钥管理**: JWT/AES密钥环境变量配置 + RSA密钥文件持久化
- **启动验证**: 应用启动时自动验证安全配置，生产环境强制要求

---

## 常见问题

### 🚀 启动相关

**Q: 启动时提示数据库连接失败？**
A: 确保MySQL服务正在运行，检查application.yml中的数据库配置是否正确。

**Q: 如何使用一键启动脚本？**
A: 运行 `./start.sh` (Linux/Mac) 或 `start.bat` (Windows)，脚本会自动检查环境并启动应用。

**Q: 启动后访问不到应用？**
A: 检查端口8080是否被占用，确认防火墙设置。

### 🗄️ 数据库相关

**Q: 如何手动创建数据库？**
A: 运行 `mysql -u root -p < src/main/resources/sql/init_database.sql`

**Q: 数据库表结构在哪里查看？**
A: 查看 `src/main/resources/sql/init_database.sql` 文件。

**Q: 如何修改数据库配置？**
A: 通过环境变量或修改application.yml文件。

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
A: 已配置并启用。Elasticsearch 9.1.3正在172.18.48.1:9200，Redis 3.2.100正在6379端口。

**Q: 如何配置Elasticsearch连接？**
A: 1. 先运行连接测试：`test-es-connection.bat` (Windows) 或 `./test-es-connection.sh` (Linux/Mac)
2. 选择以下方案之一：

   - 方案A（推荐）：获取ES的http_ca.crt证书，配置HTTPS连接

   - 方案B（临时）：修改ES配置禁用TLS，使用HTTP连接

   详见 `src/main/resources/es/README.md`

**Q: 如何使用搜索功能？**
A: 使用API `/api/search/messages?q=关键词` 进行消息搜索，支持中文分词。

**Q: 如果搜索服务停止会怎样？**
A: 不会影响核心聊天功能，应用会继续正常运行，只是搜索功能暂时不可用。

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
# 特别注意：设置 ES_PASSWORD 为你的Elasticsearch密码
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

- 📧 **邮箱**: your-email@example.com
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/your-repo/weeb/issues)
- 📖 **项目文档**: [Wiki](https://github.com/your-repo/weeb/wiki)

---

> **WEEB Team** ❤️ 为现代化即时通信与内容管理系统而努力
>
> 最后更新时间：2025年1月 - 安全加固版本
