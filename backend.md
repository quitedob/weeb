# 后端API文档

## 项目概述
这是一个基于Spring Boot的后端项目，采用前后端分离架构，包含即时通信、内容管理、AI对话等核心功能模块。

## 技术栈
- **框架**: Spring Boot 3.5.4
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis-Plus 3.5.8
- **缓存**: Redis
- **搜索**: Elasticsearch
- **消息队列**: WebSocket

## 架构规范
项目遵循分层架构：Controller -> Service -> ServiceImpl -> Mapper -> Database

---

## API接口列表

### 1. 管理员管理 API (`/api/admin`)

#### 权限管理
- **GET /api/admin/permissions**
  - 功能：获取权限管理页面数据
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `keyword` (String, 可选): 搜索关键词
    - `resource` (String, 可选): 资源过滤
    - `status` (String, 可选): 状态过滤

- **POST /api/admin/permissions**
  - 功能：创建权限
  - 参数：Permission对象 (JSON)
    - `name`: 权限名称
    - `resource`: 资源标识
    - `action`: 操作类型
    - `description`: 描述

- **PUT /api/admin/permissions/{permissionId}**
  - 功能：更新权限
  - 参数：
    - `permissionId` (Long, 路径): 权限ID
    - `permission` (Permission对象): 更新的权限信息

- **DELETE /api/admin/permissions/{permissionId}**
  - 功能：删除权限
  - 参数：`permissionId` (Long, 路径): 权限ID

#### 角色管理
- **GET /api/admin/roles**
  - 功能：获取角色管理页面数据
  - 参数：同权限管理分页参数

- **POST /api/admin/roles**
  - 功能：创建角色
  - 参数：Role对象 (JSON)

- **PUT /api/admin/roles/{roleId}**
  - 功能：更新角色
  - 参数：
    - `roleId` (Long, 路径): 角色ID
    - `role` (Role对象): 更新信息

- **DELETE /api/admin/roles/{roleId}**
  - 功能：删除角色
  - 参数：`roleId` (Long, 路径): 角色ID

#### 角色权限管理
- **POST /api/admin/roles/{roleId}/permissions**
  - 功能：为角色分配权限
  - 参数：
    - `roleId` (Long, 路径): 角色ID
    - `permissionIds` (List<Long>): 权限ID列表

- **DELETE /api/admin/roles/{roleId}/permissions**
  - 功能：从角色移除权限
  - 参数：
    - `roleId` (Long, 路径): 角色ID
    - `permissionIds` (List<Long>): 权限ID列表

- **GET /api/admin/roles/{roleId}/permissions**
  - 功能：获取角色权限列表
  - 参数：`roleId` (Long, 路径): 角色ID

#### 用户管理
- **GET /api/admin/users**
  - 功能：获取用户管理页面数据
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `keyword` (String, 可选): 搜索关键词
    - `status` (String, 可选): 用户状态(active/banned/all)

- **POST /api/admin/users/{userId}/ban**
  - 功能：封禁用户
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/admin/users/{userId}/unban**
  - 功能：解封用户
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/admin/users/{userId}/reset-password**
  - 功能：重置用户密码
  - 参数：
    - `userId` (Long, 路径): 用户ID
    - 请求体：`{"newPassword": "新密码"}`

- **POST /api/admin/users/{userId}/roles/{roleId}**
  - 功能：为用户分配角色
  - 参数：
    - `userId` (Long, 路径): 用户ID
    - `roleId` (Long, 路径): 角色ID

- **DELETE /api/admin/users/{userId}/roles/{roleId}**
  - 功能：从用户移除角色
  - 参数：
    - `userId` (Long, 路径): 用户ID
    - `roleId` (Long, 路径): 角色ID

- **GET /api/admin/users/{userId}/roles**
  - 功能：获取用户角色列表
  - 参数：`userId` (Long, 路径): 用户ID

#### 系统统计
- **GET /api/admin/statistics**
  - 功能：获取系统统计信息
  - 参数：无

#### 系统日志
- **GET /api/admin/logs**
  - 功能：获取系统日志
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
    - `operatorId` (Long, 可选): 操作者ID
    - `action` (String, 可选): 操作类型
    - `ipAddress` (String, 可选): IP地址
    - `startDate` (String, 可选): 开始日期 (yyyy-MM-dd)
    - `endDate` (String, 可选): 结束日期 (yyyy-MM-dd)
    - `keyword` (String, 可选): 关键词搜索

- **GET /api/admin/logs/statistics**
  - 功能：获取系统日志统计信息
  - 参数：无

- **GET /api/admin/logs/errors**
  - 功能：获取错误日志统计
  - 参数：`hours` (int, 默认24): 统计小时数

- **GET /api/admin/logs/recent-activity**
  - 功能：获取最近活动统计
  - 参数：`minutes` (int, 默认15): 统计分钟数

- **GET /api/admin/logs/actions**
  - 功能：获取可用操作类型列表
  - 参数：无

- **GET /api/admin/logs/operators**
  - 功能：获取可用操作员列表
  - 参数：`days` (int, 默认30): 最近天数

- **GET /api/admin/logs/export**
  - 功能：导出系统日志
  - 参数：
    - `format` (String, 默认csv): 导出格式(csv, xlsx, json)
    - `startDate` (String): 开始日期 (yyyy-MM-dd)
    - `endDate` (String): 结束日期 (yyyy-MM-dd)
    - `operatorId` (Long, 可选): 操作者ID
    - `action` (String, 可选): 操作类型
    - `ipAddress` (String, 可选): IP地址
    - `keyword` (String, 可选): 关键词

- **POST /api/admin/logs/cleanup**
  - 功能：清理过期日志
  - 参数：`days` (int, 默认30): 保留天数

- **DELETE /api/admin/logs/batch**
  - 功能：批量删除日志
  - 参数：`logIds` (List<Long>): 日志ID列表

- **GET /api/admin/logs/{logId}**
  - 功能：获取日志详情
  - 参数：`logId` (Long, 路径): 日志ID

- **GET /api/admin/logs/distribution**
  - 功能：获取日志级别分布
  - 参数：
    - `startDate` (String, 可选): 开始日期 (yyyy-MM-dd)
    - `endDate` (String, 可选): 结束日期 (yyyy-MM-dd)

- **GET /api/admin/logs/hourly**
  - 功能：获取每小时日志统计
  - 参数：`date` (String): 日期 (yyyy-MM-dd)

- **GET /api/admin/logs/search**
  - 功能：搜索日志
  - 参数：
    - `keyword` (String): 搜索关键词
    - `limit` (int, 默认100): 返回数量限制

#### 系统监控
- **GET /api/admin/monitor/realtime**
  - 功能：获取实时系统监控数据
  - 参数：无

- **GET /api/admin/monitor/user-behavior**
  - 功能：获取用户行为分析数据
  - 参数：`days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/user-events**
  - 功能：获取用户行为事件列表
  - 参数：
    - `days` (int, 默认1): 统计天数
    - `eventType` (String, 可选): 事件类型过滤
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认50): 每页大小

- **GET /api/admin/monitor/user-segments**
  - 功能：获取用户分群统计数据
  - 参数：`days` (int, 默认30): 统计天数

- **GET /api/admin/monitor/popular-pages**
  - 功能：获取热门页面统计
  - 参数：`days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/anomalies**
  - 功能：获取异常行为检测结果
  - 参数：`hours` (int, 默认24): 检测时间范围

- **POST /api/admin/monitor/run-anomaly-detection**
  - 功能：运行异常行为检测
  - 参数：无

- **GET /api/admin/monitor/export-behavior-data**
  - 功能：导出用户行为数据
  - 参数：
    - `format` (String, 默认csv): 导出格式(csv, xlsx, json)
    - `days` (int, 默认30): 统计天数

- **GET /api/admin/monitor/activity-heatmap**
  - 功能：获取用户活动热力图数据
  - 参数：
    - `type` (String, 默认hourly): 热力图类型(hourly, weekly, monthly)
    - `days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/user-retention**
  - 功能：获取用户留存分析
  - 参数：
    - `cohortType` (String, 默认weekly): 队列类型(daily, weekly, monthly)
    - `periods` (int, 默认12): 分析期数

- **GET /api/admin/monitor/performance**
  - 功能：获取系统性能指标
  - 参数：无

#### 内容审核
- **GET /api/admin/content/articles/pending**
  - 功能：获取待审核文章列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
    - `status` (Integer, 可选): 文章状态
    - `keyword` (String, 可选): 关键词搜索

- **POST /api/admin/content/articles/{articleId}/approve**
  - 功能：审核通过文章
  - 参数：`articleId` (Long, 路径): 文章ID

- **POST /api/admin/content/articles/{articleId}/reject**
  - 功能：审核拒绝文章
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - 请求体：`{"reason": "拒绝原因"}`

- **DELETE /api/admin/content/articles/{articleId}**
  - 功能：管理员删除文章
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - 请求体：`{"reason": "删除原因"}`

- **GET /api/admin/content/statistics**
  - 功能：获取内容审核统计
  - 参数：无

#### 系统管理
- **POST /api/admin/initialize-permissions**
  - 功能：初始化系统权限
  - 参数：无

- **POST /api/admin/initialize-roles**
  - 功能：初始化系统角色
  - 参数：无

- **POST /api/admin/refresh-cache**
  - 功能：刷新权限缓存
  - 参数：无

- **GET /api/admin/health**
  - 功能：获取系统健康状态
  - 参数：无

### 2. AI功能 API (`/api/ai`)

#### 文章处理
- **POST /api/ai/article/summary**
  - 功能：生成文章摘要
  - 参数：
    - `content` (String): 文章内容
    - `maxLength` (Integer, 可选): 摘要最大长度
  - 权限：ARTICLE_READ_OWN

- **POST /api/ai/article/titles**
  - 功能：生成文章标题建议
  - 参数：
    - `content` (String): 文章内容
    - `count` (Integer, 默认5): 生成数量
  - 权限：ARTICLE_CREATE_OWN

- **POST /api/ai/article/tags**
  - 功能：生成文章标签
  - 参数：
    - `content` (String): 文章内容
    - `count` (Integer, 默认5): 生成数量
  - 权限：ARTICLE_CREATE_OWN

#### 文本处理
- **POST /api/ai/text/refine**
  - 功能：润色文本内容
  - 参数：
    - `content` (String): 文本内容
    - `tone` (String): 语气风格
  - 权限：ARTICLE_UPDATE_OWN

- **POST /api/ai/text/translate**
  - 功能：翻译文本
  - 参数：
    - `content` (String): 原文内容
    - `targetLanguage` (String): 目标语言
  - 权限：AI_TRANSLATE_OWN

- **POST /api/ai/text/proofread**
  - 功能：校对和修正文本
  - 参数：`content` (String): 待校对文本
  - 权限：ARTICLE_UPDATE_OWN

#### AI对话
- **POST /api/ai/chat**
  - 功能：AI聊天对话
  - 参数：
    - `messages` (List<Message>): 消息列表
    - `sessionId` (String, 可选): 会话ID
  - 权限：AI_CHAT_OWN

- **POST /api/ai/reply/suggestions**
  - 功能：生成回复建议
  - 参数：
    - `originalMessage` (String): 原始消息
    - `context` (String, 可选): 上下文
  - 权限：AI_CHAT_OWN

- **POST /api/ai/conversation/summary**
  - 功能：总结对话历史
  - 参数：
    - `messages` (List<Map>): 消息历史
    - `maxLength` (Integer, 默认200): 摘要最大长度
  - 权限：AI_CHAT_OWN

#### 内容分析
- **POST /api/ai/sentiment/analyze**
  - 功能：分析内容情感
  - 参数：`content` (String): 分析内容
  - 权限：CONTENT_ANALYZE_OWN

- **POST /api/ai/keywords/extract**
  - 功能：提取关键词
  - 参数：
    - `content` (String): 提取内容
    - `count` (Integer, 默认10): 关键词数量
  - 权限：CONTENT_ANALYZE_OWN

- **POST /api/ai/content/compliance**
  - 功能：检查内容合规性
  - 参数：`content` (String): 检查内容
  - 权限：CONTENT_MODERATE_OWN

- **POST /api/ai/content/suggestions**
  - 功能：生成内容创作建议
  - 参数：
    - `topic` (String): 主题
    - `contentType` (String, 默认"article"): 内容类型
  - 权限：ARTICLE_CREATE_OWN

- **POST /api/ai/content/outline**
  - 功能：生成内容大纲
  - 参数：
    - `topic` (String): 主题
    - `structure` (String, 默认"introduction-body-conclusion"): 结构
  - 权限：ARTICLE_CREATE_OWN

#### 配置管理
- **GET /api/ai/config**
  - 功能：获取AI配置信息
  - 参数：无

### 3. 文章管理 API (`/api/articles`)

#### 基础文章操作
- **GET /api/articles/{id}**
  - 功能：根据ID获取文章信息
  - 参数：`id` (Long, 路径): 文章ID

- **PUT /api/articles/{id}**
  - 功能：修改文章内容
  - 参数：
    - `id` (Long, 路径): 文章ID
    - `articleTitle` (String): 文章标题
    - `articleContent` (String): 文章内容
    - `articleLink` (String): 文章链接
    - `categoryId` (Long): 分类ID
    - `status` (Integer): 文章状态
  - 权限：ARTICLE_UPDATE_OWN

- **DELETE /api/articles/{id}**
  - 功能：删除文章
  - 参数：`id` (Long, 路径): 文章ID
  - 权限：ARTICLE_DELETE_OWN

- **POST /api/articles/new**
  - 功能：创建新文章
  - 参数：
    - `articleTitle` (String): 文章标题
    - `articleContent` (String): 文章内容
    - `articleLink` (String): 文章链接
    - `categoryId` (Long): 分类ID
    - `status` (Integer): 文章状态
  - 权限：ARTICLE_CREATE_OWN

#### 文章交互
- **POST /api/articles/{id}/like**
  - 功能：文章点赞
  - 参数：`id` (Long, 路径): 文章ID

- **POST /api/articles/{id}/read**
  - 功能：增加阅读数量
  - 参数：`id` (Long, 路径): 文章ID

- **POST /api/articles/{id}/addcoin**
  - 功能：增加文章金币
  - 参数：
    - `id` (Long, 路径): 文章ID
    - `amount` (Double): 金币数量

- **POST /api/articles/{id}/favorite**
  - 功能：收藏文章
  - 参数：`id` (Long, 路径): 文章ID

- **DELETE /api/articles/{id}/favorite**
  - 功能：取消收藏文章
  - 参数：`id` (Long, 路径): 文章ID

- **GET /api/articles/{id}/favorite/status**
  - 功能：检查收藏状态
  - 参数：`id` (Long, 路径): 文章ID

#### 用户文章管理
- **GET /api/articles/userinform**
  - 功能：根据用户ID获取用户信息
  - 参数：`userId` (Long): 用户ID

- **POST /api/articles/userinform**
  - 功能：获取用户所有文章统计
  - 参数：`userId` (Long): 用户ID


- **GET /api/articles/myarticles**
  - 功能：获取当前用户的所有文章
  - 参数：`userId` (Long): 用户ID

- **GET /api/articles/favorites**
  - 功能：获取用户收藏的文章列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小

#### 订阅管理
- **POST /api/articles/subscribe**
  - 功能：增加粉丝（订阅用户）
  - 参数：`targetUserId` (Long): 目标用户ID

#### 文章列表
- **GET /api/articles/getall**
  - 功能：获取所有文章（支持分页和排序）
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `sortBy` (String, 默认"created_at"): 排序字段
    - `sortOrder` (String, 默认"desc"): 排序方向

- **GET /api/articles/recommended**
  - 功能：获取推荐文章列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小

#### 搜索功能
- **GET /api/articles/search**
  - 功能：搜索文章
  - 参数：
    - `query` (String): 搜索关键词
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `sortBy` (String, 默认"created_at"): 排序字段
    - `sortOrder` (String, 默认"desc"): 排序方向

- **GET /api/articles/search/advanced**
  - 功能：高级搜索文章
  - 参数：ArticleSearchAdvancedVo对象（包含多种搜索条件）

#### 分类管理
- **GET /api/articles/categories**
  - 功能：获取文章分类列表
  - 参数：无

### 4. 文章评论 API (`/api/articles/{articleId}/comments`)

- **GET /api/articles/{articleId}/comments**
  - 功能：获取文章评论列表
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小

- **POST /api/articles/{articleId}/comments**
  - 功能：发表文章评论
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - `content` (String): 评论内容
    - `parentId` (Long, 可选): 父评论ID

- **DELETE /api/articles/{articleId}/comments/{commentId}**
  - 功能：删除文章评论
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - `commentId` (Long, 路径): 评论ID

- **GET /api/articles/{articleId}/comments/count**
  - 功能：获取文章评论数量
  - 参数：`articleId` (Long, 路径): 文章ID

### 4.1 文章版本 API (`/api/articles/{articleId}/versions`)

- **GET /api/articles/{articleId}/versions**
  - 功能：获取文章所有版本

- **GET /api/articles/{articleId}/versions/{versionNumber}**
  - 功能：获取指定版本

- **GET /api/articles/{articleId}/versions/latest**
  - 功能：获取最新版本

- **GET /api/articles/{articleId}/versions/latest-published**
  - 功能：获取最新发布版本

- **GET /api/articles/{articleId}/versions/major**
  - 功能：获取主要版本列表

- **POST /api/articles/{articleId}/versions/{versionNumber}/rollback**
  - 功能：回滚到指定版本
  - 请求体：`{ rollbackNote }`（可选）

- **POST /api/articles/{articleId}/versions/auto-save**
  - 功能：自动保存版本
  - 请求体：`{ title, content }`

- **POST /api/articles/{articleId}/versions/major**
  - 功能：创建主要版本
  - 请求体：`{ title, content, changeNote }`

- **GET /api/articles/{articleId}/versions/compare?from=&to=**
  - 功能：比较两个版本

- **GET /api/articles/{articleId}/versions/statistics**
  - 功能：获取版本统计

- **DELETE /api/articles/{articleId}/versions/cleanup?keepCount=10**
  - 功能：清理旧版本

- **GET /api/articles/{articleId}/versions/search?keyword=**
  - 功能：搜索版本

- **GET /api/articles/{articleId}/versions/auto-saves?hours=24**
  - 功能：获取自动保存版本

- **GET /api/articles/{articleId}/versions/change-stats**
  - 功能：获取版本变更统计

- **GET /api/articles/{articleId}/versions/export?format=json**
  - 功能：导出版本历史

- **GET /api/articles/{articleId}/versions/timeline**
  - 功能：获取版本时间线

### 5. 聊天 API (`/api/chats`)

- **GET /api/chats**
  - 功能：获取用户的聊天列表
  - 参数：无

- **POST /api/chats**
  - 功能：创建新的聊天会话
  - 参数：
    - `targetId` (Long): 目标用户ID

- **GET /api/chats/{chatId}/messages**
  - 功能：获取聊天消息历史记录
  - 参数：
    - `chatId` (Long, 路径): 聊天ID
    - `page` (int, 默认1): 页码
    - `size` (int, 默认20): 每页大小

- **POST /api/chats/{chatId}/messages**
  - 功能：发送聊天消息
  - 参数：
    - `chatId` (Long, 路径): 聊天ID
    - `content` (String): 消息内容
    - `messageType` (Integer, 可选): 消息类型

- **POST /api/chats/{chatId}/read**
  - 功能：标记消息为已读
  - 参数：`chatId` (Long, 路径): 聊天ID

- **DELETE /api/chats/{chatId}**
  - 功能：删除聊天会话
  - 参数：`chatId` (Long, 路径): 聊天ID

- **POST /api/chats/messages/{messageId}/react**
  - 功能：对消息添加反应
  - 参数：
    - `messageId` (Long, 路径): 消息ID
    - `reactionType` (String): 反应类型（如👍、❤️等）



### 6.1 消息线索 API (`/api/threads`)

- **POST /api/threads**
  - 功能：创建消息线索
  - 请求体：`{ rootMessageId, title }`

- **GET /api/threads/{threadId}**
  - 功能：获取线索详情
  - 参数：`threadId` (Long, 路径)

- **GET /api/threads/{threadId}/messages**
  - 功能：获取线索内消息
  - 参数：`threadId` (Long, 路径)、`page` (int, 默认1)、`pageSize` (int, 默认20)

- **POST /api/threads/{threadId}/replies**
  - 功能：在线索中回复
  - 请求体：`{ content }`

- **POST /api/threads/{threadId}/join** / **DELETE /api/threads/{threadId}/leave**
  - 功能：加入/离开线索

- **POST /api/threads/{threadId}/archive** / **POST /api/threads/{threadId}/close**
  - 功能：归档/关闭线索

- **POST /api/threads/{threadId}/pin?isPinned=bool** / **POST /api/threads/{threadId}/lock?isLocked=bool**
  - 功能：置顶/锁定线索

- **GET /api/threads/my-threads**
  - 功能：获取我参与的线索
  - 参数：`page` (int, 默认1)、`pageSize` (int, 默认20)

- **GET /api/threads/active**
  - 功能：获取活跃线索
  - 参数：`page` (int, 默认1)、`pageSize` (int, 默认20)

- **GET /api/threads/created**
  - 功能：获取我创建的线索
  - 参数：`page` (int, 默认1)、`pageSize` (int, 默认20)

- **GET /api/threads/search**
  - 功能：搜索线索
  - 参数：`keyword` (String)、`page` (int, 默认1)、`pageSize` (int, 默认20)

- **GET /api/threads/{threadId}/statistics**
  - 功能：获取线索统计信息

- **GET /api/threads/context/{messageId}**
  - 功能：获取消息的线索上下文
  - 参数：`messageId` (Long, 路径)

### 7. 认证 API (`/api/auth`)

#### 标准认证
- **POST /api/auth/register**
  - 功能：用户注册
  - 参数：
    - `username` (String): 用户名 (必需，3-20字符)
    - `password` (String): 密码 (必需，6-50字符)
    - `confirmPassword` (String): 确认密码 (必需)
    - `email` (String): 邮箱 (必需，格式验证)
    - `phone` (String, 可选): 手机号 (最多20字符)
    - `nickname` (String, 可选): 昵称 (最多50字符)
    - `bio` (String, 可选): 个人简介 (最多200字符)

- **POST /api/auth/login**
  - 功能：用户登录
  - 参数：
    - `username` (String): 用户名 (必需，3-50字符)
    - `password` (String): 密码 (必需，6-100字符)
    - `rememberMe` (Boolean, 可选): 记住我选项 (默认false)

- **POST /api/auth/logout**
  - 功能：用户登出
  - 参数：Authorization header (Bearer token)


- **POST /api/auth/refresh**
  - 功能：刷新访问令牌
  - 参数：Authorization header (Bearer token)

- **POST /api/auth/validate**
  - 功能：验证令牌
  - 参数：Authorization header (Bearer token)

- **POST /api/auth/change-password**
  - 功能：修改密码
  - 参数：Authorization header (Bearer token)
  - 请求体：
    - `currentPassword` (String): 当前密码
    - `newPassword` (String): 新密码
    - `confirmPassword` (String): 确认新密码

#### 密码重置
- **POST /api/auth/forgot-password**
  - 功能：发送密码重置链接
  - 参数：`email` (String): 邮箱地址

- **POST /api/auth/reset-password**
  - 功能：重置密码
  - 参数：
    - `token` (String): 重置令牌
    - `newPassword` (String): 新密码

- **GET /api/auth/verify-reset-token**
  - 功能：验证重置令牌
  - 参数：`token` (String): 重置令牌

### 8. 用户管理 API (`/api/users`)

- **GET /api/users/me**
  - 功能：获取当前用户完整信息（包含统计数据）
  - 参数：无
  - 📝 **统一接口**：整合了原本分散的用户信息获取功能

- **GET /api/users/me/profile**
  - 功能：获取当前用户完整信息（包含统计数据）
  - 参数：无
  - 📝 **兼容接口**：与 GET /api/users/me 功能相同

- **GET /api/users/me/info**
  - 功能：获取当前用户基本信息
  - 参数：无
  - 📝 **兼容接口**：返回用户基础信息，不含统计数据

- **PUT /api/users/me**
  - 功能：更新当前用户信息
  - 参数：UpdateUserVo对象

- **GET /api/users/{userId}**
  - 功能：获取指定用户信息
  - 参数：`userId` (Long, 路径): 用户ID

- **GET /api/users**
  - 功能：获取用户列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `keyword` (String, 可选): 搜索关键词

- **GET /api/users/search**
  - 功能：搜索用户
  - 参数：
    - `q` (String): 搜索关键词
    - `limit` (int, 默认10): 返回数量

- **GET /api/users/me/groups**
  - 功能：获取当前用户的群组列表
  - 参数：无

#### 用户操作
- **POST /api/users/{userId}/ban**
  - 功能：封禁用户
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/users/{userId}/unban**
  - 功能：解封用户
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/users/{userId}/reset-password**
  - 功能：重置用户密码
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/users/{userId}/follow**
  - 功能：关注用户
  - 参数：`userId` (Long, 路径): 用户ID

- **DELETE /api/users/{userId}/follow**
  - 功能：取消关注用户
  - 参数：`userId` (Long, 路径): 用户ID

#### 关注管理
- **GET /api/users/{userId}/following**
  - 功能：获取用户关注列表
  - 参数：`userId` (Long, 路径): 用户ID

- **GET /api/users/{userId}/followers**
  - 功能：获取用户粉丝列表
  - 参数：`userId` (Long, 路径): 用户ID

- **GET /api/users/{userId}/follow/status**
  - 功能：检查关注状态
  - 参数：`userId` (Long, 路径): 用户ID

#### 用户统计
- **GET /api/users/{userId}/stats**
  - 功能：获取用户统计信息
  - 参数：`userId` (Long, 路径): 用户ID

- **GET /api/users/{userId}/activities**
  - 功能：获取用户活动记录
  - 参数：`userId` (Long, 路径): 用户ID

#### 个人资料
- **PUT /api/users/profile**
  - 功能：更新用户个人资料
  - 参数：UpdateUserVo对象

- **POST /api/users/avatar**
  - 功能：上传用户头像
  - 参数：头像文件 (MultipartFile)

### 9. 群组管理 API (`/api/groups`)

- **POST /api/groups**
  - 功能：创建群组
  - 参数：
    - `name` (String): 群组名称
    - `description` (String): 群组描述
    - `avatar` (String, 可选): 群组头像

- **GET /api/groups/{groupId}**
  - 功能：获取群组信息
  - 参数：`groupId` (Long, 路径): 群组ID

- **PUT /api/groups/{groupId}**
  - 功能：更新群组信息
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `name` (String): 群组名称
    - `description` (String): 群组描述

- **DELETE /api/groups/{groupId}**
  - 功能：删除群组
  - 参数：`groupId` (Long, 路径): 群组ID

- **GET /api/groups/{groupId}/members**
  - 功能：获取群组成员列表
  - 参数：`groupId` (Long, 路径): 群组ID

- **POST /api/groups/{groupId}/members**
  - 功能：添加群组成员
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `userId` (Long): 用户ID

- **DELETE /api/groups/{groupId}/members/me**
  - 功能：退出群组
  - 参数：`groupId` (Long, 路径): 群组ID

- **DELETE /api/groups/{groupId}/members/{userId}**
  - 功能：移除群组成员
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `userId` (Long, 路径): 用户ID

- **POST /api/groups/{groupId}/applications**
  - 功能：申请加入群组
  - 参数：`groupId` (Long, 路径): 群组ID

- **GET /api/groups/{groupId}/applications**
  - 功能：获取群组申请列表
  - 参数：`groupId` (Long, 路径): 群组ID

- **PUT /api/groups/{groupId}/applications/{applicationId}**
  - 功能：处理群组申请
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `applicationId` (Long, 路径): 申请ID
    - `action` (String): 处理动作 (approve/reject)

- **PUT /api/groups/{groupId}/members/{userId}/role**
  - 功能：更改成员角色
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `userId` (Long, 路径): 用户ID
    - `role` (String): 新角色

- **GET /api/groups/search**
  - 功能：搜索群组
  - 参数：
    - `q` (String): 搜索关键词
    - `limit` (int, 默认10): 返回数量

- **GET /api/groups/my-groups**
  - 功能：获取我的群组
  - 参数：无

- **GET /api/groups/my-created**
  - 功能：获取我创建的群组
  - 参数：无

### 10. 联系人管理 API (`/api/contacts`)

- **POST /api/contacts/apply**
  - 功能：申请添加好友
  - 参数：`targetUserId` (Long): 目标用户ID

- **POST /api/contacts/accept/{contactId}**
  - 功能：接受好友申请
  - 参数：`contactId` (Long, 路径): 联系人ID

- **POST /api/contacts/decline/{contactId}**
  - 功能：拒绝好友申请
  - 参数：`contactId` (Long, 路径): 联系人ID

- **POST /api/contacts/block/{contactId}**
  - 功能：拉黑联系人
  - 参数：`contactId` (Long, 路径): 联系人ID

- **GET /api/contacts**
  - 功能：获取联系人列表
  - 参数：`status` (String): 关系状态(如ACCEPTED、PENDING)

- **GET /api/contacts/requests**
  - 功能：获取好友申请列表
  - 参数：无

### 11. 通知 API (`/api/notifications`)

- **POST /api/notifications/test**
  - 功能：发送测试通知
  - 参数：`userId` (Long): 用户ID

- **GET /api/notifications**
  - 功能：获取通知列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `size` (int, 默认10): 每页大小

- **GET /api/notifications/unread-count**
  - 功能：获取未读通知数量
  - 参数：无

- **POST /api/notifications/read-all**
  - 功能：标记所有通知为已读
  - 参数：无

- **POST /api/notifications/{id}/read**
  - 功能：标记单个通知为已读
  - 参数：`id` (Long, 路径): 通知ID

- **DELETE /api/notifications/read**
  - 功能：删除已读通知
  - 参数：无

### 12. 安全中心 API (`/api/security`)

- **GET /api/security/center**
  - 功能：获取安全中心信息
  - 参数：无

- **GET /api/security/sessions**
  - 功能：获取登录会话列表
  - 参数：无

- **POST /api/security/sessions/{sessionId}/logout**
  - 功能：强制登出会话
  - 参数：`sessionId` (String, 路径): 会话ID

- **POST /api/security/sessions/logout-all**
  - 功能：登出所有会话
  - 参数：无

- **GET /api/security/login-history**
  - 功能：获取登录历史
  - 参数：无

- **GET /api/security/security-events**
  - 功能：获取安全事件
  - 参数：无

- **POST /api/security/change-password**
  - 功能：修改密码
  - 参数：Authorization header (Bearer token)
  - 请求体：
    - `currentPassword` (String): 当前密码
    - `newPassword` (String): 新密码
    - `confirmPassword` (String): 确认新密码

- **GET /api/security/permissions**
  - 功能：获取用户权限列表
  - 参数：无

- **GET /api/security/has-permission/{permission}**
  - 功能：检查用户是否有指定权限
  - 参数：`permission` (String, 路径): 权限标识

- **GET /api/security/roles**
  - 功能：获取用户角色列表
  - 参数：无

- **GET /api/security/is-admin**
  - 功能：检查用户是否为管理员
  - 参数：无

- **GET /api/security/security-score**
  - 功能：获取安全评分
  - 参数：无

### 13. 搜索 API (`/api/search`)

- **GET /api/search/messages**
  - 功能：搜索消息
  - 参数：
    - `q` (String): 搜索关键词
    - `page` (int, 默认0): 页码（从0开始）
    - `size` (int, 默认10): 每页数量
    - `startDate` (String, 可选): 开始日期
    - `endDate` (String, 可选): 结束日期
    - `messageTypes` (String, 可选): 消息类型，逗号分隔
    - `userIds` (String, 可选): 用户ID列表，逗号分隔
    - `groupIds` (String, 可选): 群组ID列表，逗号分隔
    - `sortBy` (String, 默认relevance): 排序（relevance、time_desc、time_asc、username_asc、username_desc）

- **GET /api/search/group**
  - 功能：搜索群组
  - 参数：
    - `keyword` (String): 搜索关键词
    - `page` (int, 默认0): 页码（从0开始）
    - `size` (int, 默认10): 每页数量
    - `startDate` (String, 可选): 开始日期
    - `endDate` (String, 可选): 结束日期
    - `sortBy` (String, 默认relevance): 排序（relevance、time_desc、time_asc、name_asc、name_desc）

- **GET /api/search/users**
  - 功能：搜索用户
  - 参数：
    - `keyword` (String): 搜索关键词
    - `page` (int, 默认0): 页码（从0开始）
    - `size` (int, 默认10): 每页数量
    - `startDate` (String, 可选): 开始日期
    - `endDate` (String, 可选): 结束日期
    - `sortBy` (String, 默认relevance): 排序（relevance、time_desc、time_asc、name_asc、name_desc）

- **GET /api/search/articles**
  - 功能：搜索文章
  - 参数：
    - `query` (String): 搜索关键词
    - `page` (int, 默认1): 页码（从1开始）
    - `pageSize` (int, 默认10): 每页数量
    - `startDate` (String, 可选): 开始日期
    - `endDate` (String, 可选): 结束日期
    - `sortBy` (String, 默认created_at): 排序字段（created_at、updated_at、title、relevance）
    - `sortOrder` (String, 默认desc): 排序方向（asc、desc）

- **GET /api/search/all**
  - 功能：综合搜索（用户、群组、文章、消息）
  - 参数：
    - `q` (String): 搜索关键词
    - `page` (int, 默认0): 页码（从0开始）
    - `size` (int, 默认5): 每页数量

### 14. 关注管理 API (`/api/follow`)

- **POST /api/follow/{followeeId}**
  - 功能：关注用户
  - 参数：`followeeId` (Long, 路径): 被关注者ID

- **DELETE /api/follow/{followeeId}**
  - 功能：取消关注用户
  - 参数：`followeeId` (Long, 路径): 被关注者ID

- **GET /api/follow/check/{followeeId}**
  - 功能：检查关注状态
  - 参数：`followeeId` (Long, 路径): 被关注者ID

- **GET /api/follow/following**
  - 功能：获取关注列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `size` (int, 默认10): 每页大小

- **GET /api/follow/followers**
  - 功能：获取粉丝列表
  - 参数：
    - `page` (int, 默认1): 页码
    - `size` (int, 默认10): 每页大小

- **GET /api/follow/stats**
  - 功能：获取关注统计
  - 参数：无

- **GET /api/follow/stats/{targetUserId}**
  - 功能：获取指定用户的关注统计
  - 参数：`targetUserId` (Long, 路径): 目标用户ID

### 15. 内容举报 API (`/api/content-reports`)

- **POST /api/content-reports**
  - 功能：提交内容举报
  - 参数：
    - `contentType` (String): 内容类型
    - `contentId` (Long): 内容ID
    - `reason` (String): 举报原因
    - `description` (String, 可选): 详细描述

- **GET /api/content-reports/pending**
  - 功能：获取待处理的举报
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小

- **PUT /api/content-reports/{reportId}/process**
  - 功能：处理举报
  - 参数：
    - `reportId` (Long, 路径): 举报ID
    - `action` (String): 处理动作
    - `reason` (String, 可选): 处理原因

- **PUT /api/content-reports/batch-process**
  - 功能：批量处理举报
  - 参数：
    - `reportIds` (List<Long>): 举报ID列表
    - `action` (String): 处理动作
    - `reason` (String, 可选): 处理原因

- **GET /api/content-reports/my-reports**
  - 功能：获取我的举报记录
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小

- **GET /api/content-reports/content/{contentType}/{contentId}**
  - 功能：获取内容的举报记录
  - 参数：
    - `contentType` (String, 路径): 内容类型
    - `contentId` (Long, 路径): 内容ID

- **GET /api/content-reports/statistics**
  - 功能：获取举报统计
  - 参数：无

- **GET /api/content-reports/top-reported**
  - 功能：获取最常被举报的内容
  - 参数：
    - `limit` (int, 默认10): 返回数量限制

- **GET /api/content-reports/reviewer-stats**
  - 功能：获取审核员统计
  - 参数：无

- **PUT /api/content-reports/{reportId}/withdraw**
  - 功能：撤回举报
  - 参数：`reportId` (Long, 路径): 举报ID

- **PUT /api/content-reports/{reportId}/mark-urgent**
  - 功能：标记举报为紧急
  - 参数：`reportId` (Long, 路径): 举报ID

- **GET /api/content-reports/{reportId}**
  - 功能：获取举报详情
  - 参数：`reportId` (Long, 路径): 举报ID

- **GET /api/content-reports/can-report**
  - 功能：检查是否可以举报
  - 参数：
    - `contentType` (String): 内容类型
    - `contentId` (Long): 内容ID

### 16. 调试 API (`/api/debug`)

- **GET /api/debug/user-permissions**
  - 功能：获取用户权限信息（调试用）
  - 参数：无

- **GET /api/debug/all-permissions**
  - 功能：获取所有权限信息（调试用）
  - 参数：无

- **GET /api/debug/all-roles**
  - 功能：获取所有角色信息（调试用）
  - 参数：无

- **GET /api/debug/system-health**
  - 功能：获取系统健康状态
  - 参数：无

- **GET /api/debug/database-status**
  - 功能：获取数据库状态
  - 参数：无

- **GET /api/debug/system-config**
  - 功能：获取系统配置
  - 参数：无

### 17. WebSocket/STOMP 端点

- 连接与订阅
  - 订阅：`/topic/chat/{roomId}`（聊天室广播）
  - 订阅：`/user/queue/private`（私聊消息）

- 发送端点（客户端发送）
  - `@MessageMapping("/chat.sendMessage")`：发送聊天室消息（服务端转发到 `/topic/chat/{roomId}`）
  - `@MessageMapping("/chat/join/{roomId}")`：加入聊天室（广播加入事件）
  - `@MessageMapping("/chat/leave/{roomId}")`：离开聊天室（广播离开事件）
  - `@MessageMapping("/chat/typing/{roomId}")`：正在输入（广播输入状态）
  - `@MessageMapping("/chat/private")`：发送私聊消息（发送到 `/user/{target}/queue/private`）

- 其他
  - 心跳：`/chat/heartbeat`
  - 连接握手：`/chat/connect`（`@SubscribeMapping`）

### 18. 迁移 API (`/api/migration`)

- **GET /api/migration/validate/pre**
  - 功能：迁移前验证
  - 参数：无

- **GET /api/migration/validate/post**
  - 功能：迁移后验证
  - 参数：无

- **POST /api/migration/create-missing-stats**
  - 功能：创建缺失的统计数据
  - 参数：无

- **GET /api/migration/status**
  - 功能：获取迁移状态
  - 参数：无

---

## 数据模型

### 用户 (User)
- `id`: 用户ID
- `username`: 用户名
- `password`: 密码（加密存储）
- `userEmail`: 邮箱
- `sex`: 性别 (0: 女, 1: 男)
- `phoneNumber`: 手机号
- `avatar`: 头像URL
- `nickname`: 昵称
- `bio`: 个人简介
- `status`: 用户状态
- `createdAt`: 创建时间
- `updatedAt`: 更新时间

### 文章 (Article)
- `id`: 文章ID
- `userId`: 作者ID
- `categoryId`: 分类ID
- `articleTitle`: 标题
- `articleContent`: 内容
- `articleLink`: 链接
- `status`: 状态
- `likesCount`: 点赞数
- `favoritesCount`: 收藏数
- `sponsorsCount`: 赞助数
- `exposureCount`: 阅读数
- `createdAt`: 创建时间
- `updatedAt`: 更新时间

### 消息 (Message)
- `id`: 消息ID
- `senderId`: 发送者ID
- `chatId`: 聊天ID
- `content`: 消息内容
- `messageType`: 消息类型
- `createdAt`: 发送时间
- `updatedAt`: 更新时间

### 权限 (Permission)
- `id`: 权限ID
- `name`: 权限名称
- `resource`: 资源标识
- `action`: 操作类型
- `description`: 描述

### 角色 (Role)
- `id`: 角色ID
- `name`: 角色名称
- `description`: 角色描述
- `status`: 状态

---

## 错误码说明

- `200`: 成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 权限不足
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 规范检查结果

### ✅ 符合规范的项目

1. **分层架构**: Controller → Service → ServiceImpl → Mapper → XML
2. **接口标准化**: 所有API返回ResponseEntity<ApiResponse<T>>格式
3. **参数验证**: 使用@Valid注解和ValidationUtils
4. **权限控制**: 使用@PreAuthorize注解进行权限验证
5. **事务管理**: ServiceImpl中使用@Transactional注解
6. **异常处理**: 使用GlobalExceptionHandler统一处理异常

### ⚠️ 需要检查的项目

1. **端口配置**: 确认application.yml中的端口配置是否正确 ✅
2. **数据库连接**: 确保MySQL、Redis、Elasticsearch连接正常 ✅
3. **缓存配置**: 验证Redis缓存配置是否正确 ✅
4. **WebSocket配置**: 检查WebSocket端口和连接配置 ✅

### 📝 当前实现状态

#### ✅ 已完全实现的核心功能
1. **实时消息推送**: WebSocket消息推送机制已实现
2. **RBAC权限系统**: 完整的角色权限管理
3. **AI功能框架**: 支持多种AI服务提供商
4. **搜索功能**: Elasticsearch全文搜索
5. **用户等级系统**: 完整的等级历史追踪

#### 🟡 部分实现的模块
1. **文件上传**: 用户头像上传功能部分实现
2. **邮件服务**: 密码重置邮件发送框架已搭建
3. **定时任务**: 基础的定时任务框架已建立

#### 📋 待优化项目
1. **代码质量**: 清理582个linter警告
2. **API文档**: Swagger/OpenAPI文档生成
3. **性能优化**: 数据库查询和缓存优化

---

## 🔍 项目问题分析与状态报告

### 最新分析时间
**2025年10月25日** - 基于当前项目状态的全面分析

### 🎯 当前项目状态

#### ✅ 核心功能可用性
- **编译状态**: ✅ 完全成功（修复UTF-8编码配置问题）
- **架构完整性**: ✅ 所有核心模块正常运行
- **数据库连接**: ✅ MySQL、Redis、Elasticsearch配置正常
- **安全框架**: ✅ RBAC权限系统完整实现
- **实时通信**: ✅ WebSocket消息推送正常

#### ⚠️ 发现的问题分类

##### 1. 代码质量问题 (582个linter警告)
- **未使用的导入**: 约200+个未使用的import语句
- **未使用的变量**: 约150+个未使用的局部变量和字段
- **未使用的方法**: 约50+个未使用的方法
- **类型安全警告**: 约30+个unchecked cast和类型安全问题

##### 2. 接口一致性问题
- **PasswordResetService**: 接口与实现方法签名不匹配
- **Service层方法**: 部分Service接口与实现类方法签名不一致

##### 3. 依赖管理问题
- **循环依赖**: 部分模块存在潜在的循环依赖风险
- **未使用的依赖**: pom.xml中存在未使用的依赖包

### 📊 问题严重程度评估

| 问题类型 | 数量 | 严重程度 | 影响 |
|---------|------|----------|------|
| 编译错误 | 0 | ✅ 已解决 | 无影响 |
| 类无法解析 | 0 | ✅ 已解决 | 无影响 |
| 接口不一致 | 1 | 🟡 中等 | 影响较小 |
| 类型安全警告 | 30 | 🟡 中等 | 潜在运行时错误 |
| 代码质量问题 | 582 | 🟡 中等 | 维护困难 |

### 🛠️ 建议的修复优先级

#### 🔴 高优先级 (影响功能)
1. **修复PasswordResetService接口一致性**
2. **解决Service层方法签名不匹配问题**
3. **修复关键的unchecked cast**

#### 🟡 中优先级 (影响维护)
1. **清理未使用的导入和变量**
2. **移除冗余的方法和字段**
3. **优化依赖管理**

#### 🟢 低优先级 (锦上添花)
1. **完善JavaDoc文档**
2. **统一代码风格**
3. **添加更多的单元测试**

### 🚀 项目优势总结

#### ✅ 架构优势
- **分层架构完善**: Controller → Service → ServiceImpl → Mapper → Database
- **统一响应格式**: 所有API使用ApiResponse<T>格式
- **参数验证统一**: 使用@Valid注解和ValidationUtils
- **权限控制完善**: 使用@PreAuthorize注解
- **事务管理规范**: ServiceImpl使用@Transactional

#### ✅ 功能完整性
- **RBAC权限系统**: 完整的角色权限管理
- **实时通信系统**: WebSocket消息推送
- **AI功能框架**: 支持多种AI服务提供商
- **搜索功能**: Elasticsearch全文搜索
- **用户等级系统**: 完整的等级历史追踪

### 📈 优化建议

1. **代码清理**: 定期清理未使用的代码，保持代码库整洁
2. **接口文档**: 完善API文档，添加更多使用示例
3. **性能监控**: 添加性能监控和日志分析
4. **测试覆盖**: 增加单元测试和集成测试覆盖率
5. **部署优化**: 完善生产环境部署配置

---

*本文档由系统自动生成，基于代码分析结果。如有变更，请及时更新。*
