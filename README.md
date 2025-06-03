# 📜 readme.md

```markdown
# 📦 小蓝盒 (WEEB) 内容分享平台

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-brightgreen?style=flat-square&logo=spring-boot)
![Vue.js](https://img.shields.io/badge/Vue.js-3.5.13-blue?style=flat-square&logo=vue.js)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-orange?style=flat-square&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-latest-red?style=flat-square&logo=redis)
![Maven](https://img.shields.io/badge/Maven-3.3%2B-blue?style=flat-square&logo=apache-maven)
![Vite](https://img.shields.io/badge/Vite-5.4.11-yellowgreen?style=flat-square&logo=vite)

这是一个全栈内容分享平台，后端基于 Spring Boot，前端基于 Vue.js 3。项目实现了完整的前后端分离架构，包含用户认证、文章管理、数据统计等核心功能。

---

## ✨ 项目特色

-   **前后端分离**: 后端提供 RESTful API，前端负责渲染和用户交互，职责清晰，易于维护。
-   **JWT 认证**: 使用 JSON Web Tokens (JWT) 实现无状态认证，并通过 Redis 管理令牌，安全高效。
-   **数据持久化**: 结合 MySQL 数据库和 MyBatis ORM 框架，实现对用户和文章数据的持久化操作。
-   **缓存优化**: 利用 Redis 缓存热点数据，提高响应速度；利用 Caffeine 作为本地缓存，提升性能。
-   **模块化开发**: 项目结构清晰，分为 Controller, Service, Mapper, Model 等层次，便于团队协作和功能扩展。
-   **现代化前端**: 使用 Vite 构建，提供极速的开发体验；采用 Vue 3 的组合式 API，代码更简洁。

---

## 🛠️ 技术栈

| 类型       | 技术                                                                                                                              | 简介                                                     |
| :--------- | :-------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------- |
| **后端** | ![Java](https://img.shields.io/badge/-Java_17-orange?style=flat-square&logo=openjdk)                                                | 主要编程语言                                             |
|            | ![Spring Boot](https://img.shields.io/badge/-Spring_Boot-brightgreen?style=flat-square&logo=spring-boot)                            | 核心开发框架，简化了 Spring 应用的搭建和开发过程         |
|            | ![Spring Security](https://img.shields.io/badge/-Spring_Security-green?style=flat-square&logo=spring-security)                      | 提供用户认证和授权功能，保障应用安全                     |
|            | ![MyBatis](https://img.shields.io/badge/-MyBatis-red?style=flat-square&logo=apache)                                                 | 持久层框架，通过 XML 或注解将 SQL 与 Java 对象映射         |
|            | ![JWT](https://img.shields.io/badge/-JWT-black?style=flat-square&logo=json-web-tokens)                                              | 用于生成和验证身份令牌的开放标准                         |
| **前端** | ![Vue.js](https://img.shields.io/badge/-Vue.js_3-blue?style=flat-square&logo=vue.js)                                                 | 渐进式 JavaScript 框架，用于构建用户界面                 |
|            | ![Vite](https://img.shields.io/badge/-Vite-yellowgreen?style=flat-square&logo=vite)                                                 | 新一代前端构建工具，提供极速的开发服务器和打包体验       |
|            | ![Vue Router](https://img.shields.io/badge/-Vue_Router-green?style=flat-square&logo=vue.js)                                          | Vue.js 官方的路由管理器                                  |
|            | ![Axios](https://img.shields.io/badge/-Axios-purple?style=flat-square&logo=axios)                                                   | 基于 Promise 的 HTTP 客户端，用于浏览器和 Node.js        |
| **数据库** | ![MySQL](https://img.shields.io/badge/-MySQL-orange?style=flat-square&logo=mysql)                                                   | 关系型数据库管理系统                                     |
| **缓存** | ![Redis](https://img.shields.io/badge/-Redis-red?style=flat-square&logo=redis)                                                       | 基于内存的键值存储，用于缓存 JWT 令牌和热点数据          |
|            | ![Caffeine](https://img.shields.io/badge/-Caffeine-blueviolet?style=flat-square)                                                   | 高性能的 Java 本地缓存库                                 |
| **构建工具** | ![Maven](https://img.shields.io/badge/-Maven-blue?style=flat-square&logo=apache-maven)                                              | 后端项目管理和构建工具                                   |
|            | ![NPM](https://img.shields.io/badge/-NPM-black?style=flat-square&logo=npm)                                                           | 前端包管理工具                                           |

---

## 🚀 快速开始

### 1. 环境准备

-   JDK 17
-   Maven 3.3+
-   MySQL 8.0+
-   Redis
-   Node.js 18+

### 2. 后端启动

1.  **克隆项目**
    ```bash
    git clone <your-repository-url>
    cd WEEB
    ```

2.  **配置数据库**
    -   创建一个名为 `weeb` 的数据库。
    -   修改 `src/main/resources/application.yml` 文件 ，配置您的 MySQL 和 Redis 连接信息。
        ```yaml
        spring:
          datasource:
            url: jdbc:mysql://localhost:3306/weeb?useUnicode=true&characterEncoding=utf8
            username: your-mysql-username
            password: your-mysql-password
          data:
            redis:
              host: localhost
              port: 6379
        ```

3.  **启动后端服务**
    -   使用 IDE (如 IntelliJ IDEA) 直接运行 `WeebApplication.java` 。
    -   或使用 Maven 命令行：
        ```bash
        mvn spring-boot:run
        ```
    后端服务将启动在 `http://localhost:8080`。

### 3. 前端启动

1.  **进入前端目录**
    ```bash
    cd ../Vue # 假设 Vue 文件夹在 WEEB 文件夹的同级目录
    ```

2.  **安装依赖**
    ```bash
    npm install
    ```

3.  **启动开发服务器**
    ```bash
    npm run dev
    ```
    前端服务将启动在 `http://localhost:5173` 。

4.  **访问项目**
    在浏览器中打开 `http://localhost:5173`，开始使用。

---

## 📁 项目结构

WEEB/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/web/
│   │   │       ├── Config/         # Spring Boot 配置 (Security, Redis, CORS)
│   │   │       ├── Controller/     # 控制器 (API 端点)
│   │   │       ├── filter/         # JWT 认证过滤器
│   │   │       ├── mapper/         # MyBatis Mapper 接口
│   │   │       ├── model/          # 数据模型 (User, Article)
│   │   │       ├── service/        # 业务逻辑服务接口
│   │   │       │   └── Impl/       # 服务实现类
│   │   │       ├── util/           # 工具类 (JWT, 缓存等)
│   │   │       └── WeebApplication.java # Spring Boot 启动类
│   │   └── resources/
│   │       ├── mapper/             # MyBatis XML 映射文件
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.yml     # Spring Boot 配置文件
│   └── test/
├── pom.xml                         # Maven 项目配置文件
└── mvnw                            # Maven Wrapper
└── mvnw.cmd

Vue/
├── public/
├── src/
│   ├── assets/                     # 静态资源 (CSS, 图片)
│   ├── article/                    # 文章相关组件
│   ├── auth/                       # 认证相关组件 (登录、注册)
│   ├── router/                     # Vue Router 路由配置
│   ├── value/                      # 其他视图组件
│   ├── video/                      # 视频演示组件
│   ├── App.vue                     # 根组件
│   └── main.js                     # 入口文件
├── index.html                      # 主 HTML 文件
├── package.json                    # NPM 依赖配置
└── vite.config.js                  # Vite 配置文件

---

## 🧩 功能列表

-   **用户模块**
    -   [✔] 用户注册与登录 。
    -   [✔] JWT 令牌的生成、验证与注销 。
    -   [✔] 基于 Spring Security 的请求拦截与权限控制 。
    -   [✔] 获取用户信息、用户ID查询 。

-   **文章模块**
    -   [✔] 创建、编辑、删除文章 。
    -   [✔] 获取单篇文章详情与所有文章列表 。
    -   [✔] 文章点赞、增加阅读量 。
    -   [✔] 获取指定用户的所有文章 。

-   **数据统计**
    -   [✔] 统计用户文章的总点赞、收藏、曝光量等数据 。
    -   [✔] 获取用户粉丝数、金币等综合信息 。
    -   [✔] 动态计算用户注册天数 。

---

## 📡 API 端点

### 认证接口 (`AuthController`)

| 方法   | URL             | 描述                 |
| :----- | :-------------- | :------------------- |
| `POST` | `/register`     | 用户注册  |
| `POST` | `/login`        | 用户登录  |
| `POST` | `/logout`       | 用户登出  |
| `GET`  | `/user`         | 获取当前用户信息  |
| `GET`  | `/findByUsername` | 根据用户名查找用户  |
| `GET`  | `/findByUserID` | 根据用户ID查找用户  |

### 文章接口 (`ArticleCenterController`)

| 方法   | URL                           | 描述                                     |
| :----- | :---------------------------- | :--------------------------------------- |
| `POST` | `/articles/new`               | 创建新文章                             |
| `GET`  | `/articles/{id}`              | 根据ID获取文章                      |
| `PUT`  | `/articles/{id}`              | 编辑文章                              |
| `POST` | `/articles/deletearticle`     | 根据ID删除文章                         |
| `POST` | `/articles/{id}/like`         | 点赞文章                              |
| `POST` | `/articles/{id}/read`         | 增加阅读量                              |
| `POST` | `/articles/getall`            | 获取所有文章                          |
| `POST` | `/articles/myarticles`        | 获取当前用户的所有文章                  |
| `GET`  | `/articles/userinform`        | 根据用户ID获取统计信息                  |
| `GET`  | `/articles/userinform-by-username` | 根据用户名获取详细信息（含注册天数）|

---

## Production-Grade Setup Notes
Detailed instructions will be added here.
```
