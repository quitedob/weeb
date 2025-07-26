好的，这是为您项目生成的带有图标的 `README.md` 文件。

# 💬 WEEB - 全栈即时通讯应用

[](https://www.java.com)
[](https://spring.io/projects/spring-boot)
[](https://vuejs.org/)
[](http://www.apache.org/licenses/LICENSE-2.0)

WEEB 是一个功能全面的全栈即时通讯平台，采用现代技术栈构建。项目后端基于 **Spring Boot**，前端采用 **Vue.js 3**，实现了包括实时单聊、群聊、用户管理、文章发布、实时通知等在内的多种功能。

## ✨ 主要功能

  - **👤 用户认证与管理**：基于 JWT 和 Spring Security 的安全认证，支持注册、登录、登出及用户信息管理。
  - **💬 实时聊天**：
      - 基于 WebSocket (Netty) 实现实时双向通信。
      - 支持私聊和群聊。
      - [cite\_start]消息频率限制，防止刷屏。 [cite: 51, 52]
      - [cite\_start]支持文本、表情、Markdown 等多种消息类型。 [cite: 104, 303, 706]
      - [cite\_start]消息撤回、Emoji 反应等互动功能。 [cite: 182, 184]
  - **📝 文章中心**：
      - 完整的文章 CRUD (创建、读取、更新、删除) 功能。
      - 文章点赞、阅读数统计、订阅作者等社交功能。
  - **🔔 实时通知系统**：
      - [cite\_start]用户上线/下线通知。 [cite: 105]
      - 文章点赞、新关注者等事件通知。
      - [cite\_start]通过 Redis Pub/Sub 实现跨实例消息广播。 [cite: 75, 401]
  - **👥 好友与群组系统**：
      - [cite\_start]好友申请、同意/拒绝、拉黑。 [cite: 160, 161, 162, 163]
      - 群组创建、邀请成员、踢出成员、退群等。
  - **🔍 全局搜索**：
      - [cite\_start]基于 Elasticsearch 实现消息内容的全文检索。 [cite: 202]
  - **🔐 安全与过滤**：
      - [cite\_start]接口访问频率限制 (基于 Caffeine)。 [cite: 59, 60, 61]
      - [cite\_start]敏感词过滤 (基于 `houbb/sensitive-word`)。 [cite: 89]
  - **💻 SSH 命令行**：
      - 内置一个基于 SSH 的命令行交互界面，用于执行自定义后台命令。
  - **🔧 其他特性**：
      - IP 地址归属地查询。
      - 支持文件上传至阿里云 OSS。
      - 生产与开发环境分离配置。
      - 统一的 API 响应格式和全局异常处理。

## 🛠️ 技术栈

| 分类     | 技术                                                                                                       |
| :------- | :--------------------------------------------------------------------------------------------------------- |
| **后端** | `Java 17`, `Spring Boot`, `Spring Security`, `MyBatis-Plus`, `MySQL`, `Redis`, `Elasticsearch`, `Netty`, `JWT` |
| **前端** | `Vue.js 3`, `Vite`, `Pinia`, `Vue Router`, `Axios`, `Element Plus` (UI组件)                                 |
| **构建** | `Maven` (后端), `npm` (前端)                                                                               |

## 🚀 快速开始

### 环境准备

在启动项目之前，请确保您已安装以下环境：

  - Java 17+
  - Maven 3.6+
  - Node.js 18+
  - MySQL 8.0+
  - Redis
  - Elasticsearch (需安装 `ik` 中文分词插件)

### 后端启动

1.  **克隆仓库**

    ```bash
    git clone <your-repository-url>
    cd <project-root>
    ```

2.  **配置数据库**

      - 创建一个名为 `weeb` 的 MySQL 数据库。
      - [cite\_start]根据您的数据库信息，修改 `weeb/src/main/resources/application-dev.yml` 文件中的 `spring.datasource` 配置。 [cite: 711]

3.  **配置 Redis & Elasticsearch**

      - 确保 Redis 和 Elasticsearch 服务正在运行。
      - [cite\_start]根据需要修改 `application-dev.yml` 和 `application.yml` 中的相关配置。 [cite: 711, 717]

4.  **运行项目**

      - 使用您的 IDE (如 IntelliJ IDEA) 打开项目，等待 Maven 依赖下载完成。
      - 运行 `WeebApplication.java` 即可启动后端服务。

### 前端启动

1.  **进入前端目录**

    ```bash
    cd Vue
    ```

2.  **安装依赖**

    ```bash
    npm install
    ```

3.  **配置代理**

      - [cite\_start]前端项目通过 Vite 代理将 API 请求转发到后端。请检查 `vite.config.js` 文件中的 `server.proxy` 配置，确保目标地址 (`target`) 指向您的后端服务地址（默认为 `http://localhost:8080`）。 [cite: 1224]

4.  **运行项目**

    ```bash
    npm run dev
    ```

    启动成功后，您可以在浏览器中访问 `http://localhost:5173`。

## 📁 项目结构

```
WEEB/
├── pom.xml                   # Maven 配置文件
├── src/main/java/com/web/    # Java 源码
│   ├── aop/                  # AOP 切面 (如：消息频率限制)
│   ├── annotation/           # 自定义注解
│   ├── common/               # 通用工具类 (如：API响应)
│   ├── Config/               # Spring 配置类 (Redis, Security, Mybatis等)
│   ├── controller/           # 控制器 (API 接口)
│   ├── dto/                  # 数据传输对象
│   ├── exception/            # 全局异常处理
│   ├── filter/               # 过滤器 (如：JWT认证)
│   ├── mapper/               # MyBatis Mapper 接口
│   ├── model/                # 数据模型 (实体类)
│   ├── service/              # 业务服务接口与实现
│   └── WeebApplication.java  # Spring Boot 启动类
├── src/main/resources/       # 资源文件
│   ├── mapper/               # MyBatis XML 映射文件
│   └── application.yml       # 配置文件
└── Vue/                      # 前端项目
    ├── public/
    ├── src/
    │   ├── api/              # API 请求封装
    │   ├── assets/           # 静态资源
    │   ├── components/       # 可复用组件
    │   ├── layout/           # 布局组件
    │   ├── router/           # 路由配置
    │   ├── stores/           # Pinia 状态管理
    │   ├── views/            # 页面视图
    │   ├── App.vue
    │   └── main.js
    ├── package.json          # npm 配置文件
    └── vite.config.js        # Vite 配置文件
```

## 📄 许可证

[cite\_start]该项目根据 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可证授权。 [cite: 1, 17]
