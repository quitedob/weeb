# WEEB 项目配置说明

## 项目概述

WEEB 是一个基于 Spring Boot 的现代化 Web 应用，提供用户管理、文章发布、群组聊天等功能。

## 技术栈

- **后端**: Spring Boot 3.5.4, MySQL, MyBatis-Plus, JWT
- **前端**: Vue 3, Element Plus, Vite
- **数据库**: MySQL 8.0+
- **构建工具**: Maven

## 快速开始

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Maven 3.6+
- Node.js 16+ (前端开发)

### 2. 数据库设置

#### 🚀 自动初始化（推荐）

项目启动时会**自动检查并创建**数据库和表结构，无需手动操作：

1. 确保 MySQL 服务正在运行
2. 启动应用：`mvn spring-boot:run`
3. 系统会自动：
   - 连接 MySQL 服务器
   - 创建 `weeb` 数据库（如果不存在）
   - 创建所有必要的表结构
   - 插入测试数据

#### 🛠️ 手动初始化（可选）

如果需要手动创建数据库，可以使用提供的完整初始化脚本：

```bash
# 方法1：使用完整初始化脚本（推荐）
mysql -u root -p < src/main/resources/sql/init_database.sql

# 方法2：分步执行
mysql -u root -p -e "CREATE DATABASE weeb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p weeb < src/main/resources/sql/init_user_table.sql
mysql -u root -p weeb < src/main/resources/sql/init_article_table.sql
mysql -u root -p weeb < src/main/resources/sql/init_group_table.sql
mysql -u root -p weeb < src/main/resources/sql/init_message_table.sql
mysql -u root -p weeb < src/main/resources/sql/init_notification_table.sql
```

#### ⚙️ 环境变量配置

支持通过环境变量自定义数据库配置：

```bash
# 开发环境（默认）
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=weeb
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=root
export JWT_SECRET=mySecretKey123456789012345678901234567890

# 生产环境
export MYSQL_HOST=your_mysql_host
export MYSQL_PORT=3306
export MYSQL_DATABASE=weeb_prod
export MYSQL_USERNAME=weeb_user
export MYSQL_PASSWORD=your_secure_password
export JWT_SECRET=your_strong_jwt_secret
```

### 3. 后端配置

#### 开发环境配置 (application.yml)

项目默认使用开发环境配置，数据库连接信息：
- 主机: localhost
- 端口: 3306
- 数据库: weeb
- 用户名: root
- 密码: root

#### 生产环境配置

使用 `--spring.profiles.active=prod` 参数启动：
```bash
java -jar weeb.jar --spring.profiles.active=prod
```

### 4. 启动应用

#### 🚀 一键启动（推荐）

使用提供的启动脚本，系统会自动检查环境并启动应用：

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
- 🚀 启动应用

#### 手动启动

##### 后端启动

```bash
# 开发环境
mvn spring-boot:run

# 生产环境
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# 或使用IDE直接运行 WeebApplication.java
```

##### 前端启动

```bash
cd Vue
npm install
npm run dev
```

### 5. 访问应用

- 后端 API: http://localhost:8080
- 前端界面: http://localhost:5173 (Vite 默认端口)

## 数据库表结构

应用启动时会自动检查并创建以下表：

- `user` - 用户表
- `article` - 文章表
- `group_info` - 群组信息表
- `group_member` - 群组成员表
- `message` - 消息表
- `chat_list` - 聊天列表表
- `notification` - 通知表

## 配置说明

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weeb?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
```

### JWT 配置

```yaml
jwt:
  secret: your_jwt_secret_key
  expiration: 86400000  # 24小时，单位毫秒
```

### 环境变量

支持的环境变量：
- `MYSQL_HOST` - 数据库主机
- `MYSQL_PORT` - 数据库端口
- `MYSQL_DATABASE` - 数据库名
- `MYSQL_USERNAME` - 数据库用户名
- `MYSQL_PASSWORD` - 数据库密码
- `JWT_SECRET` - JWT 密钥
- `JWT_EXPIRATION` - JWT 过期时间
- `SERVER_PORT` - 服务器端口

## 功能特性

### 已实现功能

- ✅ 用户注册和登录 (JWT认证)
- ✅ 文章发布和管理
- ✅ 群组创建和管理
- ✅ 实时聊天功能
- ✅ 通知系统
- ✅ 文件上传
- ✅ 用户个人资料管理

### 已移除功能

- ❌ OAuth2 第三方登录 (Google, Microsoft)
- ❌ H2 嵌入式数据库 (仅使用 MySQL)

## 项目结构

```
weeb/
├── src/main/java/com/web/
│   ├── config/           # 配置类
│   │   └── DatabaseInitializer.java  # 数据库初始化器
│   ├── controller/       # 控制器
│   ├── model/           # 数据模型
│   ├── mapper/          # MyBatis Mapper
│   ├── service/         # 业务逻辑
│   └── util/            # 工具类
├── src/main/resources/
│   ├── sql/             # SQL 初始化脚本
│   │   ├── init_user_table.sql
│   │   ├── init_article_table.sql
│   │   ├── init_group_table.sql
│   │   ├── init_message_table.sql
│   │   └── init_notification_table.sql
│   ├── application.yml      # 主配置文件
│   ├── application-prod.yml # 生产环境配置
│   └── Mapper/              # MyBatis XML 映射文件
└── Vue/                 # 前端项目
```

## 开发指南

### 代码规范

- 使用 Lombok 简化代码
- 遵循 RESTful API 设计规范
- 统一异常处理
- 使用 MyBatis-Plus 简化数据库操作

### 数据库迁移

如需修改表结构：
1. 更新相应的 SQL 初始化文件
2. 重启应用，系统会自动检查并创建新表

### 日志配置

- 开发环境: 控制台输出
- 生产环境: 文件输出 (logs/weeb-prod.log)

## 部署说明

### 生产环境部署

1. 构建 JAR 包：
```bash
mvn clean package -DskipTests
```

2. 设置环境变量

3. 启动应用：
```bash
java -jar target/weeb-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 部署 (可选)

```dockerfile
FROM openjdk:17-jdk-alpine
COPY target/weeb-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 MySQL 服务是否启动
   - 验证数据库连接参数
   - 确认数据库和用户权限

2. **端口占用**
   - 修改 `server.port` 配置
   - 或使用随机端口：`server.port=0`

3. **JWT 认证失败**
   - 检查 `jwt.secret` 配置
   - 验证 JWT token 格式

### 日志调试

启用 DEBUG 日志：
```bash
java -jar app.jar --logging.level.com.web=DEBUG
```

## 联系方式

如有问题，请查看项目文档或提交 Issue。
