# Weeb 项目说明

本项目为基于 Spring Boot 3 + MyBatis(Plus) + Redis + Elasticsearch 的即时通信与内容系统，前端基于 Vue 3 + Vite。本文档汇总后端 API、请求参数、SQL 表结构（依据 src/main/resources/Mapper 与 src/main/java/com/web/model）、前端目录与启动方法，以及部署与环境变量说明。

## 目录
- 后端概览
- 环境变量与启动
- API 一览（分模块）
- 主要数据表结构（DDL 参考）
- 前端（Vue）说明
- 安全、认证与跨域
- 常见问题（FAQ）

---

## 后端概览
- 框架: Spring Boot 3.5.x
- 模块: 
  - 鉴权与用户: AuthController, UserController
  - 聊天与消息: MessageController, ChatListController
  - 群组: GroupController
  - 联系人(好友): ContactController
  - 文件/视频信令: FileController
  - 通知: NotificationController
  - 文章中心: ArticleCenterController
  - 搜索: SearchController（Elasticsearch）
- 数据访问: MyBatis / MyBatis-Plus，XML 位于 src/main/resources/Mapper，实体位于 src/main/java/com/web/model
- 实时: WebSocket + Redis Pub/Sub（通过 RedisSubscriber 转发）
- 搜索: Spring Data Elasticsearch（MessageDocument 与 MessageSearchRepository）

## 环境变量与启动
- Java: 17+
- 依赖服务: MySQL、Redis、Elasticsearch
- 应用配置（环境变量优先）:
  - 数据库（dev 示例见 application-dev.yml）
    - DEV_DB_PASSWORD
  - Redis
    - REDIS_PASSWORD（如有）
  - Elasticsearch
    - ES_USERNAME, ES_PASSWORD, spring.elasticsearch.uris
  - 业务配置 weeb.*
    - WEEB_PASSWORD（群聊密码等）
  - JWT
    - JWT_SECRET_KEY（dev）/ PROD_JWT_SECRET_KEY（prod）
  - CORS（prod）
    - ALLOWED_ORIGINS（逗号分隔域名）
- 启动
  - 后端: mvn -DskipTests spring-boot:run
  - 前端: 见下文“前端（Vue）说明”

---

## API 一览（后端）
以下仅列主要接口，详见对应 Controller 源码。

### 认证与用户（AuthController, UserController）
- POST /register
  - body: { username, password, gender, phone, email }
  - resp: { success, message }
- POST /login
  - body: { username, password }
  - resp: { success, token }
- POST /logout
  - header: Authorization: Bearer <token>
  - resp: "登出成功"
- GET /user（基础信息）
  - header: Authorization
  - resp: { id, username }
- GET /user/info（详细信息 by token）
  - header: Authorization
  - resp(data): { username, userEmail, phoneNumber, sex }
- PUT /user/info（更新 by token）
  - header: Authorization
  - body: { username?, userEmail?, phoneNumber?, sex? }
- GET /list, GET /list/map, GET /online/web（管理/查询）
- GET /findByUsername?username=, GET /findByUserID?userID=（开放查询）

- 用户中心（UserController，需登录，@Userid 注入）
  - GET /api/user/info
  - POST /api/user/update  body: UpdateUserVo

### 消息与会话（MessageController, ChatListController）
- 根路径: /api/v1/message
  - POST /send  body: Message
  - POST /record  body: { targetId, index, num }
  - POST /recall  body: { msgId }
  - POST /react  body: ReactionVo { messageId, emoji }
- 会话: /api/v1/chat-list
  - GET /list/private?userId=
  - GET /group?userId=
  - POST /create?userId=&targetId=
  - POST /read?userId=&targetId=
  - POST /delete?userId=&chatListId=

### 群组（GroupController，/api/group）
- POST /create  body: GroupCreateVo
- DELETE /{groupId}  解散
- DELETE /quit/{groupId}  退群
- POST /{groupId}/invite  body: GroupInviteVo
- POST /{groupId}/kick  body: GroupKickVo
- GET /my-list  当前用户加入的群
- GET /{groupId}  群详情
- GET /members/{groupId}  成员
- PUT /{groupId}  更新群信息  body: Group

### 联系人/好友（ContactController，/api/contact）
- POST /apply  body: ContactApplyVo { friendId, remarks }
- POST /accept/{contactId}
- POST /decline/{contactId}
- POST /block/{contactId}
- GET /list?status=ACCEPTED|PENDING  返回 List<UserDto>
- GET /list/pending

### 文件/视频信令（FileController，/api/v1/file）
- POST /offer  body: OfferVo
- POST /answer  body: AnswerVo
- POST /candidate  body: CandidateVo
- POST /cancel  body: CancelVo
- POST /invite  body: InviteVo
- POST /accept  body: AcceptVo

### 通知（NotificationController，/api/notifications）
- POST /test（开发测试）
- GET /  分页: page=1&size=10
- GET /unread-count
- POST /read-all
- POST /{id}/read
- DELETE /read

### 文章中心（ArticleCenterController，/articles）
- GET /{id}
- GET /userinform?userId=（聚合统计）
- GET /userinform-by-username?username=（含注册天数）
- POST /{id}/like
- POST /subscribe?targetUserId=
- PUT /{id}  body: Article
- POST /{id}/addcoin?amount=
- POST /{id}/read
- POST /new  body: Article
- POST /userinform?userId=（用户全部文章统计）
- POST /myarticles?userId=（用户文章列表）
- GET /getall?page=&pageSize=（分页文章列表）
- DELETE /{id}（RESTful 删除）

### 搜索（SearchController，/api/search）
- GET /messages?q=keyword&page=0&size=10
  - resp: { success, data: { list: MessageDocument[], total } }

---

## 主要数据表结构（参考）
以下依据 Model 与 Mapper 汇总，生产以真实 DDL 为准。

### 表 auth（用户）
- id BIGINT PK
- username VARCHAR
- password VARCHAR
- sex INT
- phone_number VARCHAR
- user_email VARCHAR
- fans_count BIGINT
- unique_article_link VARCHAR
- unique_video_link VARCHAR
- total_likes BIGINT
- total_favorites BIGINT
- total_sponsorship BIGINT
- total_article_exposure BIGINT
- website_coins BIGINT
- registration_date DATETIME
- ip_ownership VARCHAR
- type VARCHAR
- avatar VARCHAR
- badge VARCHAR
- login_time DATETIME
- bio TEXT
- online_status INT

### 表 message（消息）
- id BIGINT PK
- sender_id BIGINT
- chat_id BIGINT
- content JSON/TEXT（JacksonTypeHandler 序列化）
- message_type INT
- read_status INT
- is_recalled INT
- user_ip VARCHAR
- source VARCHAR
- is_show_time INT
- created_at TIMESTAMP
- updated_at TIMESTAMP
- reply_to_message_id BIGINT

### 表 chat_list（会话列表）
- id VARCHAR(255) PK
- user_id BIGINT
- target_id BIGINT
- target_info TEXT NOT NULL
- unread_count INT DEFAULT 0
- last_message TEXT
- type VARCHAR(255)
- create_time TIMESTAMP(3) NOT NULL
- update_time TIMESTAMP(3) NOT NULL
- group_id BIGINT

### 表 group（群组）
- id BIGINT PK AUTO_INCREMENT
- group_name VARCHAR
- owner_id BIGINT
- group_avatar_url VARCHAR
- create_time DATETIME

### 表 group_member（群成员）
- id BIGINT PK AUTO_INCREMENT
- group_id BIGINT
- user_id BIGINT
- role INT（OWNER、ADMIN、MEMBER 对应码）
- join_time DATETIME
- update_time DATETIME

### 表 contact（联系人关系）
- id BIGINT PK AUTO_INCREMENT
- user_id BIGINT
- friend_id BIGINT
- status INT（PENDING/ACCEPTED/REJECTED/BLOCKED）
- remarks VARCHAR
- create_time DATETIME
- update_time DATETIME

### 表 notifications（通知）
- id BIGINT PK AUTO_INCREMENT
- recipient_id BIGINT
- actor_id BIGINT
- type VARCHAR
- entity_type VARCHAR
- entity_id BIGINT
- is_read TINYINT(1)
- created_at DATETIME

### 表 articles（文章）
- article_id BIGINT PK
- user_id BIGINT
- article_title VARCHAR
- article_content TEXT
- likes_count INT
- favorites_count INT
- sponsors_count DOUBLE
- exposure_count BIGINT
- created_at DATETIME
- updated_at DATETIME

### 表 message_reaction（消息反应）
- id BIGINT PK AUTO_INCREMENT
- message_id BIGINT
- user_id BIGINT
- reaction_type VARCHAR
- create_time DATETIME

### 表 file_transfer（文件/视频信令）
- id BIGINT PK AUTO_INCREMENT
- initiator_id BIGINT
- target_id BIGINT
- offer_sdp TEXT
- answer_sdp TEXT
- candidate TEXT
- status INT（0=INVITE,1=OFFERED,2=ANSWERED...）
- created_at DATETIME
- updated_at DATETIME

### Elasticsearch 索引：message
- id: Long（消息ID）
- fromId: Long（发送者）
- chatListId: Long（会话ID）
- content: Text（中文分词需 IK 插件）
- sendTime: Date

---

## 前端（Vue）说明
- 技术栈: Vue 3 + Vite + Pinia + Axios + Element Plus
- 目录结构（Vue/）
  - src/router/index.js：路由（聊天、联系人、文章、群组、设置、搜索、通知、登录注册等）
  - src/api/axiosInstance.js：Axios 实例，自动附加 Authorization: Bearer <token>，统一错误处理
  - src/api/modules/*.js：API 模块（auth、user、group、article、notification、search）
  - 其他视图与组件：views/、article/、layout/、stores/ 等
- 启动
  - 安装依赖：cd Vue && npm i
  - 启动：npm run dev
  - 开发代理：在 vite.config.js 中将 /user、/test 代理到后端 http://localhost:8080
  - Axios baseURL：http://localhost:8080（如需通过 Vite 代理，改为 /api 并调整代理规则）

## 安全、认证与跨域
- 认证：JWT（JwtAuthenticationFilter），前端通过 Pinia/LocalStorage 携带 token
- 鉴权：SecurityConfig 配置路径放行与认证；其余路径默认需要认证
- CORS：CorsConfig 按 profile 生效，dev 放开、prod 通过 ALLOWED_ORIGINS 控制
- 敏感配置：通过环境变量提供，避免明文硬编码

## 常见问题（FAQ）
- 文章删除改为 RESTful
  - 后端：DELETE /articles/{id}；前端已同步 request.delete('/articles/${id}')
- 搜索接口
  - 已接入 Spring Data Elasticsearch：GET /api/search/messages?q=...
- WebSocket 通知 TODO 已完成
  - Group/Contact 通过 Redis Pub/Sub 转发，避免循环依赖
- 运行失败排查
  - 确保 ES/Redis/MySQL 配置正确；首次运行缺表时请根据上述表结构初始化

---

> 本 README 基于代码库 src/ 与 Vue/ 当前实现汇总，如有接口新增/调整，请同步更新本文档。
