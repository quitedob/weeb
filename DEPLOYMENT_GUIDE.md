# 项目部署指南

## 环境要求

### 后端环境
- Java 17 或更高版本
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+ (可选，用于缓存)

### 前端环境
- Node.js 16+ 
- npm 8+ 或 yarn 1.22+

## 数据库准备

### 1. 创建数据库
```sql
CREATE DATABASE weeb_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置数据库连接
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weeb_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 3. 数据库初始化
项目使用 `DatabaseInitializer` 自动创建表结构，首次启动时会自动执行。

关键表结构：
- `user` - 用户表
- `user_stats` - 用户统计表  
- `articles` - 文章表（注意：表名是复数形式）
- `groups` - 群组表
- `messages` - 消息表
- 其他相关表...

## 后端部署

### 1. 编译打包
```bash
cd /path/to/project
mvn clean package -DskipTests
```

### 2. 运行应用
```bash
java -jar target/weeb-0.0.1-SNAPSHOT.jar
```

### 3. 验证后端服务
访问：`http://localhost:8080/api/user/info`
应该返回401未授权错误（说明服务正常启动）

### 4. 生产环境配置
创建 `application-prod.yml`:
```yaml
server:
  port: 8080
  
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://your-db-host:3306/weeb_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
logging:
  level:
    com.web: INFO
    root: WARN
  file:
    name: logs/weeb-app.log
```

使用环境变量启动：
```bash
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
java -jar -Dspring.profiles.active=prod target/weeb-0.0.1-SNAPSHOT.jar
```

## 前端部署

### 1. 安装依赖
```bash
cd Vue
npm install
```

### 2. 配置环境变量
创建 `.env.production`:
```
VITE_API_BASE_URL=http://your-backend-host:8080
```

### 3. 构建生产版本
```bash
npm run build
```

### 4. 部署静态文件
将 `dist` 目录下的文件部署到 Web 服务器（如 Nginx）

### 5. Nginx 配置示例
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理到后端
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Docker 部署（推荐）

### 1. 后端 Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/weeb-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

### 2. 前端 Dockerfile
```dockerfile
FROM node:16-alpine as build

WORKDIR /app
COPY Vue/package*.json ./
RUN npm install

COPY Vue/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
```

### 3. docker-compose.yml
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: weeb_db
      MYSQL_USER: weeb_user
      MYSQL_PASSWORD: weeb_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/weeb_db
      SPRING_DATASOURCE_USERNAME: weeb_user
      SPRING_DATASOURCE_PASSWORD: weeb_password

  frontend:
    build: 
      context: .
      dockerfile: Dockerfile.frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### 4. 启动服务
```bash
docker-compose up -d
```

## 部署后验证

### 1. 健康检查
```bash
# 检查后端服务
curl http://your-domain/api/user/info

# 检查前端页面
curl http://your-domain/
```

### 2. 功能测试
1. 访问首页，确认页面正常加载
2. 注册新用户
3. 登录测试
4. 发布文章测试
5. 文章列表和阅读测试

### 3. 日志检查
```bash
# 查看应用日志
tail -f logs/weeb-app.log

# 查看 Docker 日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

## 常见问题解决

### 1. 数据库连接失败
- 检查数据库服务是否启动
- 验证连接字符串、用户名、密码
- 确认防火墙设置

### 2. 前端API请求失败
- 检查 CORS 配置
- 验证 API 基础URL 配置
- 确认后端服务可访问

### 3. 文章内容不显示
- 确认数据库表结构正确（articles 表包含 article_content 字段）
- 检查文章状态筛选（只显示 status=1 的文章）

### 4. 登录失败
- 检查 JWT 配置
- 验证用户表数据
- 确认密码加密方式

### 5. 文件上传问题
- 检查文件存储路径权限
- 验证文件大小限制配置
- 确认临时目录可写

## 性能优化建议

### 1. 数据库优化
- 为常用查询字段添加索引
- 定期清理过期数据
- 配置数据库连接池

### 2. 缓存配置
- 启用 Redis 缓存
- 配置适当的缓存策略
- 缓存热点数据

### 3. 前端优化
- 启用 Gzip 压缩
- 配置 CDN
- 优化图片资源

### 4. 监控配置
- 配置应用监控（如 Spring Boot Actuator）
- 设置日志轮转
- 监控系统资源使用

## 安全配置

### 1. 数据库安全
- 使用专用数据库用户
- 限制数据库访问权限
- 定期备份数据

### 2. 应用安全
- 配置 HTTPS
- 设置安全的 JWT 密钥
- 启用请求频率限制

### 3. 服务器安全
- 配置防火墙
- 定期更新系统
- 监控异常访问

## 备份策略

### 1. 数据库备份
```bash
# 每日备份脚本
mysqldump -u username -p weeb_db > backup_$(date +%Y%m%d).sql
```

### 2. 应用备份
- 备份应用配置文件
- 备份上传的文件
- 备份日志文件

### 3. 恢复测试
- 定期测试备份恢复
- 验证数据完整性
- 测试应用功能