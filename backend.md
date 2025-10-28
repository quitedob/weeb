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

## 权限系统说明
**重要更新**: 项目已从复杂的RBAC权限管理系统迁移到简洁的用户类型授权系统。

### 当前权限架构
项目采用基于用户类型的简化权限管理：
- **ADMIN**: 管理员用户，拥有系统管理权限
- **USER**: 普通用户，拥有基础功能权限（默认类型）
- **BOT**: 机器人用户，用于系统自动化功能

### 权限验证机制
- 基于User实体的`type`字段进行权限验证
- 简化的权限检查逻辑，无需复杂的角色分配
- 所有新用户默认分配为"USER"类型
- 移除了复杂的角色-权限-用户关联表

### 迁移说明
- 原RBAC相关API已移除
- 数据库中的权限相关表已清理
- 用户注册流程简化，无需角色分配步骤

---

## API接口列表

### 1. 管理员管理 API (`/api/admin`)

**重要更新**: RBAC权限管理相关API已移除，现在使用简化的用户类型管理。

#### 用户管理
- **GET /api/admin/users**
  - 功能：获取用户管理页面数据
  - 权限：ADMIN用户类型
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `keyword` (String, 可选): 搜索关键词
    - `status` (String, 可选): 用户状态(active/banned/all)
    - `type` (String, 可选): 用户类型(ADMIN/USER/BOT)

- **POST /api/admin/users/{userId}/ban**
  - 功能：封禁用户
  - 权限：ADMIN用户类型
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/admin/users/{userId}/unban**
  - 功能：解封用户
  - 权限：ADMIN用户类型
  - 参数：`userId` (Long, 路径): 用户ID

- **POST /api/admin/users/{userId}/reset-password**
  - 功能：重置用户密码
  - 权限：ADMIN用户类型
  - 参数：
    - `userId` (Long, 路径): 用户ID
    - 请求体：`{"newPassword": "新密码"}`

- **POST /api/admin/users/{userId}/set-type**
  - 功能：设置用户类型
  - 权限：ADMIN用户类型
  - 参数：
    - `userId` (Long, 路径): 用户ID
    - 请求体：`{"userType": "ADMIN|USER|BOT"}`

- **GET /api/admin/users/{userId}/type**
  - 功能：获取用户类型
  - 权限：ADMIN用户类型
  - 参数：`userId` (Long, 路径): 用户ID

#### 系统统计
- **GET /api/admin/statistics**
  - 功能：获取系统统计信息
  - 权限：ADMIN用户类型
  - 参数：无

#### 系统日志
- **GET /api/admin/logs**
  - 功能：获取系统日志
  - 权限：ADMIN用户类型
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
  - 权限：ADMIN用户类型
  - 参数：无

- **GET /api/admin/logs/errors**
  - 功能：获取错误日志统计
  - 权限：ADMIN用户类型
  - 参数：`hours` (int, 默认24): 统计小时数

- **GET /api/admin/logs/recent-activity**
  - 功能：获取最近活动统计
  - 权限：ADMIN用户类型
  - 参数：`minutes` (int, 默认15): 统计分钟数

- **GET /api/admin/logs/actions**
  - 功能：获取可用操作类型列表
  - 权限：ADMIN用户类型
  - 参数：无

- **GET /api/admin/logs/operators**
  - 功能：获取可用操作员列表
  - 权限：ADMIN用户类型
  - 参数：`days` (int, 默认30): 最近天数

- **GET /api/admin/logs/export**
  - 功能：导出系统日志
  - 权限：ADMIN用户类型
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
  - 权限：ADMIN用户类型
  - 参数：`days` (int, 默认30): 保留天数

- **DELETE /api/admin/logs/batch**
  - 功能：批量删除日志
  - 权限：ADMIN用户类型
  - 参数：`logIds` (List<Long>): 日志ID列表

- **GET /api/admin/logs/{logId}**
  - 功能：获取日志详情
  - 权限：ADMIN用户类型
  - 参数：`logId` (Long, 路径): 日志ID

- **GET /api/admin/logs/distribution**
  - 功能：获取日志级别分布
  - 权限：ADMIN用户类型
  - 参数：
    - `startDate` (String, 可选): 开始日期 (yyyy-MM-dd)
    - `endDate` (String, 可选): 结束日期 (yyyy-MM-dd)

- **GET /api/admin/logs/hourly**
  - 功能：获取每小时日志统计
  - 权限：ADMIN用户类型
  - 参数：
    - `date` (String): 日期 (yyyy-MM-dd)

- **GET /api/admin/logs/search**
  - 功能：搜索日志
  - 权限：ADMIN用户类型
  - 参数：
    - `keyword` (String): 搜索关键词
    - `limit` (int, 默认100): 返回数量限制

#### 系统监控
- **GET /api/admin/monitor/realtime**
  - 功能：获取实时系统监控数据
  - 权限：ADMIN用户类型
  - 参数：无

- **GET /api/admin/monitor/user-behavior**
  - 功能：获取用户行为分析数据
  - 权限：ADMIN用户类型
  - 参数：`days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/user-events**
  - 功能：获取用户行为事件列表
  - 权限：ADMIN用户类型
  - 参数：
    - `days` (int, 默认1): 统计天数
    - `eventType` (String, 可选): 事件类型过滤
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认50): 每页大小

- **GET /api/admin/monitor/user-segments**
  - 功能：获取用户分群统计数据
  - 权限：ADMIN用户类型
  - 参数：`days` (int, 默认30): 统计天数

- **GET /api/admin/monitor/popular-pages**
  - 功能：获取热门页面统计
  - 权限：ADMIN用户类型
  - 参数：`days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/anomalies**
  - 功能：获取异常行为检测结果
  - 权限：ADMIN用户类型
  - 参数：`hours` (int, 默认24): 检测时间范围

- **POST /api/admin/monitor/run-anomaly-detection**
  - 功能：运行异常行为检测
  - 权限：ADMIN用户类型
  - 参数：无

- **GET /api/admin/monitor/export-behavior-data**
  - 功能：导出用户行为数据
  - 权限：ADMIN用户类型
  - 参数：
    - `format` (String, 默认csv): 导出格式(csv, xlsx, json)
    - `days` (int, 默认30): 统计天数

- **GET /api/admin/monitor/activity-heatmap**
  - 功能：获取用户活动热力图数据
  - 权限：ADMIN用户类型
  - 参数：
    - `type` (String, 默认hourly): 热力图类型(hourly, weekly, monthly)
    - `days` (int, 默认7): 统计天数

- **GET /api/admin/monitor/user-retention**
  - 功能：获取用户留存分析
  - 权限：ADMIN用户类型
  - 参数：
    - `cohortType` (String, 默认weekly): 队列类型(daily, weekly, monthly)
    - `periods` (int, 默认12): 分析期数

- **GET /api/admin/monitor/performance**
  - 功能：获取系统性能指标
  - 权限：ADMIN用户类型
  - 参数：无

#### 内容审核
- **GET /api/admin/content/articles/pending**
  - 功能：获取待审核文章列表
  - 权限：ADMIN用户类型
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
    - `status` (Integer, 可选): 文章状态
    - `keyword` (String, 可选): 关键词搜索

- **POST /api/admin/content/articles/{articleId}/approve**
  - 功能：审核通过文章
  - 权限：ADMIN用户类型
  - 参数：`articleId` (Long, 路径): 文章ID

- **POST /api/admin/content/articles/{articleId}/reject**
  - 功能：审核拒绝文章
  - 权限：ADMIN用户类型
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - 请求体：`{"reason": "拒绝原因"}`

- **DELETE /api/admin/content/articles/{articleId}**
  - 功能：管理员删除文章
  - 权限：ADMIN用户类型
  - 参数：
    - `articleId` (Long, 路径): 文章ID
    - 请求体：`{"reason": "删除原因"}`

- **GET /api/admin/content/statistics**
  - 功能：获取内容审核统计
  - 权限：ADMIN用户类型
  - 参数：无

#### 系统管理
- **GET /api/admin/health**
  - 功能：获取系统健康状态
  - 权限：ADMIN用户类型
  - 参数：无
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
  - 权限：ARTICLE_READ_OWN
  - 参数：
    - `content` (String): 文章内容
    - `maxLength` (Integer, 可选): 摘要最大长度

- **POST /api/ai/article/titles**
  - 功能：生成文章标题建议
  - 权限：ARTICLE_CREATE_OWN
  - 参数：
    - `content` (String): 文章内容
    - `count` (Integer, 默认5): 生成数量

- **POST /api/ai/article/tags**
  - 功能：生成文章标签
  - 权限：ARTICLE_CREATE_OWN
  - 参数：
    - `content` (String): 文章内容
    - `count` (Integer, 默认5): 生成数量

#### 文本处理
- **POST /api/ai/text/refine**
  - 功能：润色文本内容
  - 权限：ARTICLE_UPDATE_OWN
  - 参数：
    - `content` (String): 文本内容
    - `tone` (String): 语气风格

- **POST /api/ai/text/translate**
  - 功能：翻译文本
  - 权限：AI_TRANSLATE_OWN
  - 参数：
    - `content` (String): 原文内容
    - `targetLanguage` (String): 目标语言

- **POST /api/ai/text/proofread**
  - 功能：校对和修正文本
  - 权限：ARTICLE_UPDATE_OWN
  - 参数：`content` (String): 待校对文本

#### AI对话
- **POST /api/ai/chat**
  - 功能：AI聊天对话
  - 权限：AI_CHAT_OWN
  - 参数：
    - `messages` (List<Message>): 消息列表
    - `sessionId` (String, 可选): 会话ID

- **POST /api/ai/reply/suggestions**
  - 功能：生成回复建议
  - 权限：AI_CHAT_OWN
  - 参数：
    - `originalMessage` (String): 原始消息
    - `context` (String, 可选): 上下文

- **POST /api/ai/conversation/summary**
  - 功能：总结对话历史
  - 权限：AI_CHAT_OWN
  - 参数：
    - `messages` (List<Map>): 消息历史
    - `maxLength` (Integer, 默认200): 摘要最大长度

#### 内容分析
- **POST /api/ai/sentiment/analyze**
  - 功能：分析内容情感
  - 权限：CONTENT_ANALYZE_OWN
  - 参数：`content` (String): 分析内容

- **POST /api/ai/keywords/extract**
  - 功能：提取关键词
  - 权限：CONTENT_ANALYZE_OWN
  - 参数：
    - `content` (String): 提取内容
    - `count` (Integer, 默认10): 关键词数量

- **POST /api/ai/content/compliance**
  - 功能：检查内容合规性
  - 权限：CONTENT_MODERATE_OWN
  - 参数：`content` (String): 检查内容

- **POST /api/ai/content/suggestions**
  - 功能：生成内容创作建议
  - 权限：ARTICLE_CREATE_OWN
  - 参数：
    - `topic` (String): 主题
    - `contentType` (String, 默认"article"): 内容类型

- **POST /api/ai/content/outline**
  - 功能：生成内容大纲
  - 权限：ARTICLE_CREATE_OWN
  - 参数：
    - `topic` (String): 主题
    - `structure` (String, 默认"introduction-body-conclusion"): 结构

#### 配置管理
- **GET /api/ai/config**
  - 功能：获取AI配置信息
  - 权限：无特殊权限要求
  - 参数：无

### 3. 文章管理 API (`/api/articles`)

#### 基础文章操作
- **GET /api/articles/{id}**
  - 功能：根据ID获取文章信息
  - 权限：无特殊权限要求
  - 参数：`id` (Long, 路径): 文章ID

- **PUT /api/articles/{id}**
  - 功能：修改文章内容
  - 权限：ARTICLE_UPDATE_OWN
  - 参数：
    - `id` (Long, 路径): 文章ID
    - `articleTitle` (String): 文章标题
    - `articleContent` (String): 文章内容
    - `articleLink` (String): 文章链接
    - `categoryId` (Long): 分类ID
    - `status` (Integer): 文章状态

- **DELETE /api/articles/{id}**
  - 功能：删除文章
  - 权限：ARTICLE_DELETE_OWN
  - 参数：`id` (Long, 路径): 文章ID

- **POST /api/articles/new**
  - 功能：创建新文章
  - 权限：ARTICLE_CREATE_OWN
  - 参数：
    - `articleTitle` (String): 文章标题
    - `articleContent` (String): 文章内容
    - `articleLink` (String): 文章链接
    - `categoryId` (Long): 分类ID
    - `status` (Integer): 文章状态

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

#### 📋 功能职责说明
**ChatController** 负责聊天会话的管理和历史消息查询，专注于REST API形式的聊天业务逻辑。与WebSocketMessageController的实时通信功能相辅相成。

#### 🚀 消息处理增强功能
- **统一消息验证**: 使用MessageValidator统一验证消息格式、内容长度、非法字符
- **Redis缓存**: 最近100条消息缓存1小时，消息列表缓存30分钟
- **自动重试**: 失败消息自动重试，最多3次，每2分钟执行一次
- **消息预加载**: 自动预加载下一页消息，提高用户体验
- **缓存统计**: 提供缓存命中率统计，监控缓存效果

- **GET /api/chats**
  - 功能：获取用户的聊天列表
  - 权限：已认证用户
  - 参数：无

- **POST /api/chats**
  - 功能：创建新的聊天会话
  - 权限：已认证用户
  - 参数：
    - `targetId` (Long): 目标用户ID

- **GET /api/chats/{chatId}/messages**
  - 功能：获取聊天消息历史记录（分页查询，支持缓存）
  - 权限：已认证用户
  - 参数：
    - `chatId` (Long, 路径): 聊天ID
    - `page` (int, 默认1): 页码
    - `size` (int, 默认20): 每页大小
  - 💡 **性能优化**: 优先从Redis缓存读取，自动预加载下一页

- **POST /api/chats/{chatId}/messages**
  - 功能：发送聊天消息（REST API方式，主要用于兼容性）
  - 权限：已认证用户
  - 参数：
    - `chatId` (Long, 路径): 聊天ID
    - `content` (String): 消息内容（1-5000字符）
    - `messageType` (Integer, 可选): 消息类型
  - 💡 **增强功能**: 自动验证、清理内容、失败重试、缓存更新

- **POST /api/chats/{chatId}/read**
  - 功能：标记消息为已读
  - 权限：已认证用户
  - 参数：`chatId` (Long, 路径): 聊天ID

- **DELETE /api/chats/{chatId}**
  - 功能：删除聊天会话
  - 权限：已认证用户
  - 参数：`chatId` (Long, 路径): 聊天ID

- **POST /api/chats/messages/{messageId}/react**
  - 功能：对消息添加反应
  - 权限：已认证用户
  - 参数：
    - `messageId` (Long, 路径): 消息ID
    - `reactionType` (String): 反应类型（如👍、❤️等）

- **DELETE /api/messages/{messageId}**
  - 功能：撤回消息
  - 权限：已认证用户（仅限消息发送者）
  - 参数：`messageId` (Long, 路径): 消息ID
  - 限制：消息发送时间不超过2分钟
  - 返回：撤回结果



### 6.1 消息线索 API (`/api/threads`)

- **POST /api/threads**
  - 功能：创建消息线索
  - 权限：已认证用户
  - 参数：
    - `rootMessageId` (Long): 根消息ID
    - `title` (String): 线索标题
  - 返回：创建的MessageThread对象

- **GET /api/threads/{threadId}**
  - 功能：获取线索详情
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：MessageThread对象

- **GET /api/threads/{threadId}/messages**
  - 功能：获取线索内消息
  - 权限：已认证用户
  - 参数：
    - `threadId` (Long, 路径): 线索ID
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
  - 返回：分页的消息列表

- **POST /api/threads/{threadId}/replies**
  - 功能：在线索中回复
  - 权限：已认证用户
  - 参数：
    - `threadId` (Long, 路径): 线索ID
    - `content` (String): 回复内容
  - 返回：回复结果

- **POST /api/threads/{threadId}/join**
  - 功能：加入线索
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：加入结果

- **DELETE /api/threads/{threadId}/leave**
  - 功能：离开线索
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：离开结果

- **POST /api/threads/{threadId}/archive**
  - 功能：归档线索
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：归档结果

- **POST /api/threads/{threadId}/close**
  - 功能：关闭线索
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：关闭结果

- **POST /api/threads/{threadId}/pin**
  - 功能：置顶/取消置顶线索
  - 权限：已认证用户
  - 参数：
    - `threadId` (Long, 路径): 线索ID
    - `isPinned` (Boolean): 是否置顶
  - 返回：操作结果

- **POST /api/threads/{threadId}/lock**
  - 功能：锁定/解锁线索
  - 权限：已认证用户
  - 参数：
    - `threadId` (Long, 路径): 线索ID
    - `isLocked` (Boolean): 是否锁定
  - 返回：操作结果

- **GET /api/threads/my-threads**
  - 功能：获取我参与的线索
  - 权限：已认证用户
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
  - 返回：用户参与的线索列表

- **GET /api/threads/active**
  - 功能：获取活跃线索
  - 权限：无特殊权限要求
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
  - 返回：活跃线索列表

- **GET /api/threads/created**
  - 功能：获取我创建的线索
  - 权限：已认证用户
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
  - 返回：用户创建的线索列表

- **GET /api/threads/search**
  - 功能：搜索线索
  - 权限：无特殊权限要求
  - 参数：
    - `keyword` (String): 搜索关键词
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认20): 每页大小
  - 返回：搜索结果

- **GET /api/threads/{threadId}/statistics**
  - 功能：获取线索统计信息
  - 权限：已认证用户
  - 参数：`threadId` (Long, 路径): 线索ID
  - 返回：线索统计数据

- **GET /api/threads/context/{messageId}**
  - 功能：获取消息的线索上下文
  - 权限：已认证用户
  - 参数：`messageId` (Long, 路径): 消息ID
  - 返回：线索上下文信息

### 7. 认证 API (`/api/auth`)

#### 标准认证
- **POST /api/auth/register**
  - 功能：用户注册
  - 权限：公开访问
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
  - 权限：公开访问
  - 参数：
    - `username` (String): 用户名 (必需，3-50字符)
    - `password` (String): 密码 (必需，6-100字符)
    - `rememberMe` (Boolean, 可选): 记住我选项 (默认false)

- **POST /api/auth/logout**
  - 功能：用户登出
  - 权限：已认证用户
  - 参数：Authorization header (Bearer token)

- **POST /api/auth/refresh**
  - 功能：刷新访问令牌
  - 权限：已认证用户
  - 参数：Authorization header (Bearer token)

- **POST /api/auth/validate**
  - 功能：验证令牌
  - 权限：公开访问
  - 参数：Authorization header (Bearer token)

- **POST /api/auth/change-password**
  - 功能：修改密码
  - 权限：已认证用户
  - 参数：Authorization header (Bearer token)
  - 请求体：
    - `currentPassword` (String): 当前密码
    - `newPassword` (String): 新密码
    - `confirmPassword` (String): 确认新密码

#### 密码重置
- **POST /api/auth/forgot-password**
  - 功能：发送密码重置链接
  - 权限：公开访问
  - 参数：`email` (String): 邮箱地址

- **POST /api/auth/reset-password**
  - 功能：重置密码
  - 权限：公开访问
  - 参数：
    - `token` (String): 重置令牌
    - `newPassword` (String): 新密码

- **GET /api/auth/verify-reset-token**
  - 功能：验证重置令牌
  - 权限：公开访问
  - 参数：`token` (String): 重置令牌

### 8. 用户管理 API (`/api/users`)

- **GET /api/users/me**
  - 功能：获取当前用户完整信息（包含统计数据）
  - 权限：isAuthenticated()
  - 参数：无
  - 📝 **统一接口**：整合了原本分散的用户信息获取功能

- **GET /api/users/me/profile**
  - 功能：获取当前用户完整信息（包含统计数据）
  - 权限：isAuthenticated()
  - 参数：无
  - 📝 **兼容接口**：与 GET /api/users/me 功能相同

- **GET /api/users/me/info**
  - 功能：获取当前用户基本信息
  - 权限：isAuthenticated()
  - 参数：无
  - 📝 **兼容接口**：返回用户基础信息，不含统计数据

- **PUT /api/users/me**
  - 功能：更新当前用户信息
  - 权限：USER_UPDATE_OWN
  - 参数：UpdateUserVo对象

- **GET /api/users/{userId}**
  - 功能：获取指定用户信息
  - 权限：USER_READ_ANY
  - 参数：`userId` (Long, 路径): 用户ID

- **GET /api/users**
  - 功能：获取用户列表
  - 权限：USER_READ_ANY
  - 参数：
    - `page` (int, 默认1): 页码
    - `pageSize` (int, 默认10): 每页大小
    - `keyword` (String, 可选): 搜索关键词

- **GET /api/users/search**
  - 功能：搜索用户
  - 权限：USER_READ_ANY
  - 参数：
    - `q` (String): 搜索关键词
    - `limit` (int, 默认10): 返回数量

- **GET /api/users/me/groups**
  - 功能：获取当前用户的群组列表
  - 权限：GROUP_READ_OWN
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
  - 权限：GROUP_CREATE_OWN
  - 参数：GroupCreateVo对象
    - `groupName` (String): 群组名称
    - `groupDescription` (String): 群组描述

- **GET /api/groups/{groupId}**
  - 功能：获取群组信息
  - 权限：GROUP_READ_OWN
  - 参数：`groupId` (Long, 路径): 群组ID

- **PUT /api/groups/{groupId}**
  - 功能：更新群组信息
  - 权限：GROUP_UPDATE_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - GroupCreateVo对象：更新的群组信息

- **DELETE /api/groups/{groupId}**
  - 功能：删除群组
  - 权限：GROUP_DELETE_OWN
  - 参数：`groupId` (Long, 路径): 群组ID

- **GET /api/groups/{groupId}/members**
  - 功能：获取群组成员列表
  - 权限：GROUP_READ_OWN
  - 参数：`groupId` (Long, 路径): 群组ID

- **POST /api/groups/{groupId}/members**
  - 功能：邀请用户加入群组
  - 权限：GROUP_MANAGE_MEMBERS_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - GroupInviteVo对象：邀请信息

- **DELETE /api/groups/{groupId}/members/me**
  - 功能：退出群组
  - 权限：GROUP_LEAVE_OWN
  - 参数：`groupId` (Long, 路径): 群组ID

- **DELETE /api/groups/{groupId}/members/{userId}**
  - 功能：移除群组成员
  - 权限：GROUP_MANAGE_MEMBERS_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `userId` (Long, 路径): 用户ID

- **POST /api/groups/{groupId}/applications**
  - 功能：申请加入群组
  - 权限：GROUP_JOIN_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `message` (String, 可选): 申请消息

- **GET /api/groups/{groupId}/applications**
  - 功能：获取群组申请列表
  - 权限：GROUP_MANAGE_MEMBERS_OWN
  - 参数：`groupId` (Long, 路径): 群组ID

- **PUT /api/groups/{groupId}/applications/{applicationId}**
  - 功能：处理群组申请
  - 权限：GROUP_MANAGE_MEMBERS_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `applicationId` (Long, 路径): 申请ID
    - `action` (String): 处理动作 (approve/reject)
    - `reason` (String, 可选): 处理原因

- **PUT /api/groups/{groupId}/members/{userId}/role**
  - 功能：设置成员角色
  - 权限：GROUP_MANAGE_MEMBERS_OWN
  - 参数：
    - `groupId` (Long, 路径): 群组ID
    - `userId` (Long, 路径): 用户ID
    - `role` (String): 新角色

- **GET /api/groups/search**
  - 功能：搜索群组
  - 权限：GROUP_READ_OWN
  - 参数：
    - `q` (String): 搜索关键词
    - `limit` (int, 默认10): 返回数量

- **GET /api/groups/my-groups**
  - 功能：获取我的群组
  - 权限：GROUP_READ_OWN
  - 参数：无

- **GET /api/groups/my-created**
  - 功能：获取我创建的群组
  - 权限：GROUP_READ_OWN
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

#### 📋 功能职责说明
**WebSocketMessageController** 负责实时消息通信协议的处理，与ChatController的REST API相辅相成。主要处理WebSocket连接、实时消息推送和用户状态同步。

#### 连接与订阅
- **订阅端点**
  - `/topic/chat/{roomId}`：聊天室广播消息
  - `/user/queue/private`：私聊消息
  - `/queue/errors`：错误消息

#### 发送端点（客户端发送）
- **@MessageMapping("/chat.sendMessage")**
  - 功能：发送聊天室消息（实时通信）
  - 参数：`roomId` (路径), `content`, `type`, `replyToMessageId` (可选)
  - 广播到：`/topic/chat/{roomId}`
  - 💡 **与REST API互补**：此为实时发送，ChatController的POST为兼容性接口

- **@MessageMapping("/chat/join/{roomId}")**
  - 功能：加入聊天室
  - 参数：`roomId` (路径)
  - 广播到：`/topic/chat/{roomId}`

- **@MessageMapping("/chat/leave/{roomId}")**
  - 功能：离开聊天室
  - 参数：`roomId` (路径)
  - 广播到：`/topic/chat/{roomId}`

- **@MessageMapping("/chat/typing/{roomId}")**
  - 功能：正在输入状态
  - 参数：`roomId` (路径)
  - 广播到：`/topic/chat/{roomId}`

- **@MessageMapping("/chat/private")**
  - 功能：发送私聊消息（实时通信）
  - 参数：`targetUser`, `content`, `type`, `replyToMessageId` (可选)
  - 发送到：`/user/{target}/queue/private`

- **@MessageMapping("/chat/recall/{messageId}")**
  - 功能：撤回消息
  - 参数：`messageId` (路径), `roomId` (路径)
  - 广播到：`/topic/chat/{roomId}`

- **@MessageMapping("/chat/heartbeat")**
  - 功能：心跳检测
  - 参数：无

#### 订阅端点（客户端订阅）
- **@SubscribeMapping("/chat/connect")**
  - 功能：连接握手
  - 返回：连接状态信息

#### 消息格式
```json
{
  "id": "消息ID",
  "fromId": "发送者ID",
  "fromName": "发送者用户名",
  "content": "消息内容",
  "roomId": "房间ID",
  "timestamp": "时间戳",
  "type": "消息类型(text/image/file/etc)",
  "replyToMessageId": "回复消息ID(可选)"
}
```

### 18. WebSocket连接监控 API (`/api/websocket/monitor`)

#### 📋 功能说明
提供WebSocket连接状态监控、在线用户统计、连接管理等功能。

- **GET /api/websocket/monitor/online-count**
  - 功能：获取在线用户数
  - 权限：无特殊权限要求
  - 参数：无
  - 返回：在线用户数量

- **GET /api/websocket/monitor/online-users**
  - 功能：获取在线用户列表
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：在线用户ID集合

- **GET /api/websocket/monitor/user/{userId}/online**
  - 功能：检查用户是否在线
  - 权限：无特殊权限要求
  - 参数：`userId` (Long, 路径): 用户ID
  - 返回：在线状态（true/false）

- **GET /api/websocket/monitor/user/{userId}/info**
  - 功能：获取用户连接信息
  - 权限：ADMIN或本人
  - 参数：`userId` (Long, 路径): 用户ID
  - 返回：用户连接详细信息（会话列表、连接时间等）

- **GET /api/websocket/monitor/statistics**
  - 功能：获取连接统计信息
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：总连接数、总断开数、当前在线数、心跳统计等

- **POST /api/websocket/monitor/clean-expired**
  - 功能：手动清理过期连接
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：清理的连接数量

- **GET /api/websocket/monitor/user/{userId}/sessions**
  - 功能：获取用户活跃会话列表
  - 权限：ADMIN或本人
  - 参数：`userId` (Long, 路径): 用户ID
  - 返回：活跃会话ID集合

### 19. 限流管理 API (`/api/rate-limit`)

#### 📋 功能说明
提供API限流配置管理、限流统计、限流事件查询等功能。支持动态调整限流参数。

#### 配置管理
- **POST /api/rate-limit/config**
  - 功能：设置动态限流配置
  - 权限：ADMIN用户类型
  - 参数：
    - `path` (String): API路径
    - `maxRequests` (int): 最大请求数
  - 返回：配置结果

- **GET /api/rate-limit/config**
  - 功能：获取动态限流配置
  - 权限：ADMIN用户类型
  - 参数：`path` (String): API路径
  - 返回：最大请求数配置

- **DELETE /api/rate-limit/config**
  - 功能：删除动态限流配置
  - 权限：ADMIN用户类型
  - 参数：`path` (String): API路径
  - 返回：删除结果

- **GET /api/rate-limit/config/all**
  - 功能：获取所有动态限流配置
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：所有路径的限流配置映射

#### 统计与监控
- **GET /api/rate-limit/statistics**
  - 功能：获取限流统计信息
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：总请求数、被阻止数、告警数等统计信息

- **GET /api/rate-limit/events**
  - 功能：获取限流事件列表
  - 权限：ADMIN用户类型
  - 参数：`limit` (int, 默认100): 返回数量限制
  - 返回：限流事件列表（包含时间、标识符、路径等）

- **GET /api/rate-limit/alerts**
  - 功能：获取限流告警列表
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：限流告警列表（达到80%阈值的告警）

- **DELETE /api/rate-limit/statistics**
  - 功能：清除限流统计
  - 权限：ADMIN用户类型
  - 参数：无
  - 返回：清除结果

#### 限流解除
- **POST /api/rate-limit/unlock**
  - 功能：手动解除限流
  - 权限：ADMIN用户类型
  - 参数：
    - `identifier` (String): 标识符（用户ID或IP）
    - `path` (String): API路径
    - `type` (String): 类型（USER或IP）
  - 返回：解除结果

### 20. 迁移 API (`/api/migration`)

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
1. **实时消息推送**: WebSocket消息推送机制已实现，包含心跳监控和连接管理
2. **RBAC权限系统**: 完整的角色权限管理
3. **AI功能框架**: 支持多种AI服务提供商
4. **搜索功能**: Elasticsearch全文搜索
5. **用户等级系统**: 完整的等级历史追踪
6. **消息缓存系统**: Redis缓存消息历史，提高查询性能
7. **消息重试机制**: 自动重试失败消息，最多3次
8. **API限流系统**: 支持用户和IP双重限流，动态配置
9. **WebSocket连接监控**: 实时监控在线用户和连接状态

#### 🟡 部分实现的模块
1. **文件上传**: 用户头像上传功能部分实现
2. **邮件服务**: 密码重置邮件发送框架已搭建
3. **定时任务**: 基础的定时任务框架已建立

#### 📋 待优化项目
1. **代码质量**: 清理剩余linter警告
2. **API文档**: Swagger/OpenAPI文档生成
3. **性能优化**: 数据库查询和缓存优化

---

## 🔍 项目问题分析与状态报告

### 最新分析时间
**2024年10月28日** - WebSocket连接管理、消息缓存重试、API限流系统完善

### 🎯 当前项目状态

#### ✅ 核心功能可用性
- **编译状态**: ✅ 完全成功（所有编译错误已修复）
- **架构完整性**: ✅ 所有核心模块正常运行
- **数据库连接**: ✅ MySQL、Redis、Elasticsearch配置正常
- **安全框架**: ✅ RBAC权限系统完整实现
- **实时通信**: ✅ WebSocket消息推送正常
- **用户等级系统**: ✅ 等级历史追踪和角色同步功能完整
- **前端集成**: ✅ 管理页面与后端API对齐

#### ⚠️ 发现的问题分类

##### 1. 代码质量问题 (45个linter警告)
- **未使用的导入**: 约15个未使用的import语句
- **未使用的变量**: 约20个未使用的局部变量和字段
- **类型安全警告**: 约10个unchecked cast和类型安全问题

##### 2. 功能完善性问题
- **单元测试**: 缺少核心业务逻辑的单元测试
- **API文档**: 缺少Swagger/OpenAPI自动生成文档
- **缓存机制**: 权限缓存策略待完善

##### 3. 性能优化空间
- **数据库查询**: 部分查询可添加索引优化
- **缓存策略**: 可实现更完善的缓存机制
- **批量操作**: 支持更多批量处理功能

##### 4. 最近完成的修复 ✅
- **数据库表迁移**: role_permission和user_level_history表已迁移到DatabaseInitializer
- **依赖冲突修复**: javax.servlet和javax.validation已替换为jakarta包
- **编译问题解决**: 所有编译错误已修复，AuthService和UserStats方法已完善
- **API文档更新**: 新增了17个角色权限API和10个等级历史API的详细文档
- **WebSocket连接管理**: 实现完整的连接监控、心跳机制、自动清理
- **消息缓存优化**: 实现Redis缓存、消息预加载、缓存统计
- **消息重试机制**: 实现自动重试、失败记录、定时任务
- **API限流增强**: 实现双重限流、动态配置、告警机制

### 📊 问题严重程度评估

| 问题类型 | 数量 | 严重程度 | 影响 |
|---------|------|----------|------|
| 编译错误 | 0 | ✅ 已解决 | 无影响 |
| 类无法解析 | 0 | ✅ 已解决 | 无影响 |
| 接口不一致 | 0 | ✅ 已解决 | 无影响 |
| 类型安全警告 | 10 | 🟡 中等 | 潜在运行时错误 |
| 代码质量问题 | 45 | 🟡 中等 | 维护困难 |

### 🛠️ 建议的修复优先级

#### 🔴 高优先级 (影响功能)
1. **完善单元测试覆盖** - 为核心业务逻辑添加测试
2. **API文档完善** - 添加Swagger/OpenAPI文档生成
3. **性能监控** - 添加应用性能监控

#### 🟡 中优先级 (影响维护)
1. **代码质量优化** - 清理剩余的linter警告
2. **依赖管理优化** - 移除未使用的依赖
3. **缓存策略完善** - 实现角色权限缓存机制

#### 🟢 低优先级 (锦上添花)
1. **前端组件完善** - 优化UI交互体验
2. **部署文档** - 添加生产环境部署指南
3. **监控告警** - 完善系统监控和告警机制

### 🚀 项目优势总结

#### ✅ 架构优势
- **分层架构完善**: Controller → Service → ServiceImpl → Mapper → Database
- **统一响应格式**: 所有API使用ApiResponse<T>格式
- **参数验证统一**: 使用@Valid注解和ValidationUtils
- **权限控制完善**: 使用@PreAuthorize注解
- **事务管理规范**: ServiceImpl使用@Transactional
- **消息验证统一**: MessageValidator统一验证消息格式和内容
- **缓存策略完善**: Redis缓存消息和连接状态，提高性能

#### ✅ 功能完整性
- **RBAC权限系统**: 完整的角色权限管理（16个API接口）
- **实时通信系统**: WebSocket消息推送，心跳监控（25秒间隔）
- **WebSocket连接管理**: 连接状态跟踪、自动清理、统计监控（7个API接口）
- **消息缓存系统**: Redis缓存最近100条消息，30分钟TTL
- **消息重试机制**: 自动重试失败消息，最多3次，每2分钟执行
- **API限流系统**: 用户+IP双重限流，动态配置，告警机制（10个API接口）
- **AI功能框架**: 支持多种AI服务提供商
- **搜索功能**: Elasticsearch全文搜索
- **用户等级系统**: 完整的等级历史追踪和集成（13个API接口）
- **等级角色同步**: 自动根据等级分配角色权限
- **消息线索系统**: 完整的线索管理和回复功能（16个API接口）
- **前端管理界面**: 用户等级历史和角色权限管理页面

### 📈 优化建议

1. **代码清理**: 定期清理未使用的代码，保持代码库整洁
2. **接口文档**: 完善API文档，添加更多使用示例
3. **性能监控**: 添加性能监控和日志分析
4. **测试覆盖**: 增加单元测试和集成测试覆盖率
5. **部署优化**: 完善生产环境部署配置

---

## 📋 API接口统计

### 当前API接口总数
- **总接口数量**: 72个核心业务API
- **角色权限模块**: 16个API接口
- **等级历史模块**: 9个API接口
- **等级集成模块**: 4个API接口
- **消息线索模块**: 16个API接口
- **WebSocket监控模块**: 7个API接口
- **限流管理模块**: 10个API接口

### 接口分布
- **管理API**: 管理员权限管理、用户管理、系统监控等
- **业务API**: 文章、聊天、AI功能、搜索等
- **用户API**: 认证、个人资料、关注等
- **系统API**: 通知、安全中心、调试等
- **监控API**: WebSocket连接监控、限流统计等
- **配置API**: 限流配置管理、动态调整等

---

*本文档基于实际代码分析更新，确保所有接口参数数量准确。如有新功能开发，请及时更新文档。*
