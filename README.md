# Weeb - 现代化社交平台

## 项目概述

Weeb是一个基于Vue 3 + Spring Boot的前后端分离社交平台，采用微服务架构设计，集成了即时通信、内容管理、AI对话等核心功能。平台致力于提供高质量的用户体验，注重代码质量和系统架构的清晰性。

### 🎯 设计理念
- **分层清晰**: 严格遵循前后端分离、职责单一原则
- **高内聚低耦合**: 模块化设计，便于维护和扩展
- **接口标准化**: RESTful API设计，统一的响应格式
- **性能优化**: 支持高并发，优秀的用户体验

### 🚀 核心特性
- **实时聊天**: 支持一对一和群组聊天，WebSocket实时通信
- **内容管理**: 文章发布、评论、点赞、收藏功能
- **社交互动**: 用户关注、好友系统、群组管理
- **AI集成**: AI聊天对话、文本分析、图像生成
- **搜索功能**: 基于Elasticsearch的全局搜索
- **权限管理**: 基于RBAC的角色权限系统
- **响应式设计**: 完美适配PC和移动端

---

## 技术栈

### 前端技术栈
- **Vue 3.5.13** - 渐进式JavaScript框架
- **Vue Router 4** - 路由管理
- **Pinia** - 状态管理
- **Element Plus 2.7.8** - UI组件库
- **Axios** - HTTP客户端
- **Vite** - 构建工具
- **Quill 2.0.3** - 富文本编辑器

### 后端技术栈
- **Java 17+** - 开发语言
- **Spring Boot 3.5.4** - 应用框架
- **MySQL 8.0+** - 关系型数据库
- **Redis** - 缓存和会话存储
- **Elasticsearch** - 搜索引擎
- **MyBatis-Plus 3.5.8** - ORM框架
- **WebSocket** - 实时通信

### 开发工具
- **Maven** - Java项目管理
- **npm/yarn** - Node.js包管理
- **Git** - 版本控制
- **Docker** - 容器化部署

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
- **Java**: JDK 17+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Elasticsearch**: 7.10+ (可选，用于搜索功能)

### 后端启动
1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd weeb
   ```

2. **配置数据库**
   ```sql
   CREATE DATABASE weeb DEFAULT CHARACTER SET utf8mb4;
   ```

3. **修改配置**
   编辑 `src/main/resources/application.yml`，配置数据库连接信息

4. **启动后端**
   ```bash
   mvn spring-boot:run
   ```

### 前端启动
1. **安装依赖**
   ```bash
   cd Vue
   npm install
   ```

2. **启动开发服务器**
   ```bash
   npm run dev
   ```

3. **访问应用**
   打开浏览器访问 `http://localhost:5173`

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
- **注册登录**: 支持邮箱注册、JWT认证
- **个人资料**: 头像上传、信息编辑、隐私设置
- **等级系统**: 用户成长激励、积分奖励
- **权限管理**: 基于RBAC的角色权限控制

### 2. 聊天系统
- **实时通信**: WebSocket实现即时消息
- **多会话支持**: 私聊、群聊、消息线程
- **消息管理**: 发送、撤回、反应表情、文件传输
- **在线状态**: 用户在线状态显示和通知

### 3. 内容管理
- **文章发布**: 富文本编辑器、标签分类
- **互动功能**: 点赞、收藏、评论、分享
- **内容审核**: 举报和审核机制
- **搜索功能**: 基于Elasticsearch的全文搜索

### 4. 社交功能
- **好友系统**: 添加好友、好友分组管理
- **关注系统**: 用户关注、粉丝列表、推荐关注
- **群组管理**: 创建群组、成员管理、群组权限
- **通知中心**: 系统通知、消息提醒、设置管理

### 5. AI功能
- **AI对话**: 智能聊天助手、多模型支持
- **内容生成**: 文本分析、图像生成
- **翻译服务**: 多语言翻译功能
- **个性化推荐**: 基于用户行为的智能推荐

### 6. 系统管理
- **数据统计**: 用户行为分析、系统监控
- **内容管理**: 文章审核、用户管理
- **系统设置**: 配置管理、维护模式
- **日志审计**: 操作日志、安全审计

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
# 后端开发启动
mvn spring-boot:run

# 前端开发启动
cd Vue && npm run dev
```

### 生产环境
1. **后端构建**
   ```bash
   mvn clean package -DskipTests
   java -jar target/weeb-*.jar
   ```

2. **前端构建**
   ```bash
   cd Vue
   npm run build
   # 静态文件部署到Nginx或其他Web服务器
   ```

3. **Docker部署**
   ```bash
   # 构建镜像
   docker build -t weeb .
   docker run -p 8080:8080 weeb
   ```

### 环境变量
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=weeb
DB_USER=root
DB_PASS=password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASS=password

# JWT配置
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400

# Elasticsearch配置
ES_HOST=localhost
ES_PORT=9200
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

### v1.0.0 (2025-01-01)
- ✨ 初始版本发布
- 🚀 支持基础聊天功能
- 📝 实现文章发布系统
- 👥 添加好友和群组功能
- 🔍 集成搜索功能
- 🤖 AI功能集成

### 开发计划
- [ ] 移动端App开发
- [ ] 视频通话功能
- [ ] 国际化支持
- [ ] 第三方登录集成
- [ ] 数据分析面板

---

**最后更新**: 2025年10月29日

感谢使用Weeb！🎉
