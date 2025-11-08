# 后端API接口文档

## 项目概述

本项目采用Spring Boot 3.5.4 + Java 17架构，提供完整的社交平台后端服务。包含用户认证、聊天系统、内容管理、AI功能、搜索服务等核心模块。

### 技术栈
- **框架**: Spring Boot 3.5.4
- **语言**: Java 17
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.0+
- **搜索引擎**: Elasticsearch 8.18.6
- **ORM**: MyBatis-Plus 3.5.8
- **认证**: JWT + Spring Security
- **AI**: Spring AI 1.0.0-M1
- **WebSocket**: Spring WebSocket (STOMP)
- **SSH**: Apache SSHD 2.14.0

### 架构特点
- **分层架构**: Controller → Service → Mapper → Database
- **RESTful API**: 统一的接口设计规范
- **统一响应**: 标准化的ApiResponse格式
- **注解驱动**: 自定义注解简化开发（@Userid, @UrlLimit等）
- **条件装配**: 按需启用功能模块

---

## 控制器接口列表

### 1. StandardAuthController (认证管理)
**基路径**: `/api/auth`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/register` | 用户注册 | `RegistrationVo` (body) |
| POST | `/login` | 用户登录 | `LoginVo` (body) |
| POST | `/logout` | 用户登出 | `Authorization` (header) |
| POST | `/refresh` | 刷新令牌 | `Authorization` (header) |
| POST | `/validate` | 验证令牌 | `Authorization` (header) |
| POST | `/change-password` | 修改密码 | `PasswordChangeVo` (body), `Authorization` (header) |
| POST | `/forgot-password` | 忘记密码 | `ForgotPasswordVo` (body) |
| POST | `/reset-password` | 重置密码 | `PasswordResetVo` (body) |
| GET | `/verify-reset-token` | 验证重置令牌 | `token` (query) |

### 2. StandardUserController (用户管理)
**基路径**: `/api/users`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/me` | 获取当前用户信息 | 无 |
| GET | `/me/profile` | 获取当前用户完整信息 | 无 |
| GET | `/me/info` | 获取当前用户基本信息 | 无 |
| PUT | `/me` | 更新当前用户信息 | `UpdateUserVo` (body) |
| GET | `/{userId}` | 获取指定用户信息 | `userId` (path) |
| GET | `/by-username/{username}` | 通过用户名获取用户信息 | `username` (path) |
| GET | `/` | 获取用户列表（分页） | `page`, `pageSize`, `keyword` (query) |
| GET | `/search` | 搜索用户 | `q`, `limit` (query) |
| GET | `/me/groups` | 获取当前用户的群组列表 | 无 |
| POST | `/{userId}/ban` | 封禁用户 | `userId` (path) |
| POST | `/{userId}/unban` | 解封用户 | `userId` (path) |
| POST | `/{userId}/reset-password` | 重置用户密码 | `userId` (path), `AdminResetPasswordRequestVo` (body) |
| POST | `/{userId}/follow` | 关注用户 | `userId` (path) |
| DELETE | `/{userId}/follow` | 取消关注用户 | `userId` (path) |
| GET | `/{userId}/following` | 获取用户的关注列表 | `userId` (path), `page`, `pageSize` (query) |
| GET | `/{userId}/followers` | 获取用户的粉丝列表 | `userId` (path), `page`, `pageSize` (query) |
| GET | `/{userId}/follow/status` | 检查当前用户是否关注了指定用户 | `userId` (path) |
| GET | `/{userId}/stats` | 获取用户统计信息 | `userId` (path) |
| GET | `/{userId}/activities` | 获取用户最近活动 | `userId` (path), `limit` (query) |
| PUT | `/profile` | 更新个人资料 | `UpdateUserVo` (body) |
| POST | `/avatar` | 上传用户头像 | `avatar` (multipart file) |

### 3. StandardGroupController (群组管理)
**基路径**: `/api/groups`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建群组 | `GroupCreateVo` (body), `@Userid Long userId` |
| GET | `/{groupId}` | 获取群组详情 | `groupId` (path), `@Userid Long userId` |
| PUT | `/{groupId}` | 更新群组信息 | `groupId` (path), `GroupCreateVo` (body), `@Userid Long userId` |
| DELETE | `/{groupId}` | 删除群组 | `groupId` (path), `@Userid Long userId` |
| GET | `/{groupId}/members` | 获取群组成员列表 | `groupId` (path) |
| POST | `/{groupId}/members` | 邀请用户加入群组 | `groupId` (path), `GroupInviteVo` (body), `@Userid Long userId` |
| DELETE | `/{groupId}/members/me` | 退出群组 | `groupId` (path), `@Userid Long userId` |
| DELETE | `/{groupId}/members/{userId}` | 移除群组成员 | `groupId` (path), `userId` (path), `@Userid Long currentUserId` |
| POST | `/{groupId}/applications` | 申请加入群组 | `groupId` (path), `Map<String, String>` (body), `@Userid Long userId` |
| GET | `/{groupId}/applications` | 获取群组申请列表 | `groupId` (path), `status` (query), `@Userid Long userId` |
| PUT | `/{groupId}/applications/{applicationId}` | 处理群组申请 | `groupId` (path), `applicationId` (path), `Map<String, String>` (body), `@Userid Long userId` |
| PUT | `/{groupId}/members/{userId}/role` | 设置群组管理员 | `groupId` (path), `userId` (path), `Map<String, String>` (body), `@Userid Long currentUserId` |
| GET | `/search` | 搜索群组 | `q` (query), `limit` (query) |
| GET | `/my-groups` | 获取用户加入的群组列表 | `@Userid Long userId` |
| GET | `/my-created` | 获取用户创建的群组列表 | `@Userid Long userId` |

### 4. ContactController (联系人管理)
**基路径**: `/api/contacts`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/apply` | 发送好友申请（通过用户ID） | `ContactApplyVo` (body), `@Userid Long userId` |
| POST | `/request` | 发送好友申请（兼容前端 /request 路径） | `Map<String, Object>` (body), `@Userid Long userId` |
| POST | `/request/by-username` | 通过用户名发送好友申请 | `Map<String, String>` (body), `@Userid Long userId` |
| POST | `/accept/{contactId}` | 同意好友申请 | `contactId` (path), `@Userid Long userId` |
| POST | `/request/{requestId}/accept` | 同意好友申请（兼容路径） | `requestId` (path), `@Userid Long userId` |
| POST | `/decline/{contactId}` | 拒绝好友申请 | `contactId` (path), `@Userid Long userId` |
| POST | `/request/{requestId}/reject` | 拒绝好友申请（兼容路径） | `requestId` (path), `@Userid Long userId` |
| POST | `/block/{contactId}` | 拉黑联系人 | `contactId` (path), `@Userid Long userId` |
| GET | `/` | 获取联系人列表 | `@Userid Long userId`, `status` (query, ContactStatus) |
| GET | `/requests` | 获取待处理的好友申请列表 | `@Userid Long userId` |
| POST | `/groups` | 创建联系人分组 | `Map<String, Object>` (body), `@Userid Long userId` |
| GET | `/groups` | 获取用户的所有联系人分组 | `@Userid Long userId` |
| PUT | `/groups/{groupId}/name` | 更新分组名称 | `groupId` (path), `Map<String, String>` (body), `@Userid Long userId` |
| PUT | `/groups/{groupId}/order` | 更新分组排序 | `groupId` (path), `Map<String, Integer>` (body), `@Userid Long userId` |
| DELETE | `/groups/{groupId}` | 删除分组 | `groupId` (path), `@Userid Long userId` |
| POST | `/groups/{groupId}/contacts/{contactId}` | 将联系人添加到分组 | `groupId` (path), `contactId` (path), `@Userid Long userId` |
| DELETE | `/groups/contacts/{contactId}` | 从分组中移除联系人 | `contactId` (path), `@Userid Long userId` |
| GET | `/groups/{groupId}/contacts` | 获取指定分组的联系人列表 | `groupId` (path), `@Userid Long userId` |
| DELETE | `/{contactId}` | 删除联系人 | `contactId` (path), `@Userid Long userId` |

### 5. ChatController (聊天管理)
**基路径**: `/api/chats`
**特殊注解**: 所有接口使用 `@UrlLimit`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/` | 获取用户的聊天列表 | `@Userid Long userId` |
| POST | `/` | 创建新的聊天会话 | `@Userid Long userId`, `ChatCreateVo` (body) |
| GET | `/{chatId}/messages` | 获取聊天消息历史记录 | `chatId` (path), `ChatMessagesVo` (@ModelAttribute) |
| POST | `/{chatId}/messages` | 发送聊天消息 | `chatId` (path), `@Userid Long userId`, `ChatMessageVo` (body) |
| POST | `/{chatId}/read` | 标记消息为已读 | `chatId` (path), `@Userid Long userId` |
| POST | `/read/batch` | 批量标记已读 | `chatIds` (body), `@Userid Long userId` |
| DELETE | `/{chatId}` | 删除聊天会话 | `chatId` (path), `@Userid Long userId` |
| POST | `/messages/{messageId}/react` | 对消息添加反应 | `messageId` (path), `@Userid Long userId`, `reactionType` (query) |
| DELETE | `/messages/{messageId}` | 撤回消息 | `messageId` (path), `@Userid Long userId` |
| GET | `/unread/stats` | 获取未读消息统计 | `@Userid Long userId` |
| GET | `/{chatId}/unread` | 获取单个聊天未读数 | `chatId` (path), `@Userid Long userId` |
| GET | `/groups/{groupId}/unread` | 获取群组未读数 | `groupId` (path), `@Userid Long userId` |
| GET | `/online-users` | 获取在线用户列表 | 无 |
| GET | `/users/{targetUserId}/online` | 检查用户是否在线 | `targetUserId` (path) |

### 6. SearchController (全局搜索)
**基路径**: `/api/search`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/messages` | 搜索消息内容 | `q`, `page`, `size`, `startDate`, `endDate`, `messageTypes`, `userIds`, `groupIds`, `sortBy` (query) |
| GET | `/group` | 搜索群组 | `keyword`, `page`, `size`, `startDate`, `endDate`, `sortBy` (query) |
| GET | `/users` | 搜索用户 | `keyword`, `page`, `size`, `startDate`, `endDate`, `sortBy` (query) |
| GET | `/articles` | 搜索文章 | `query`, `page`, `pageSize`, `startDate`, `endDate`, `sortBy`, `sortOrder` (query) |
| GET | `/` | 通用搜索接口 | `q`, `type`, `page`, `size` (query) |
| GET | `/all` | 综合搜索 | `q`, `page`, `size` (query) |

### 7. ArticleCenterController (文章中心)
**基路径**: `/api/articles`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/{id}` | 根据ID获取文章信息 | `id` (path) |
| POST | `/{id}/like` | 文章点赞 | `id` (path), `@Userid Long authenticatedUserId` |
| DELETE | `/{id}/like` | 取消点赞文章 | `id` (path), `@Userid Long authenticatedUserId` |
| GET | `/{id}/like/status` | 检查点赞状态 | `id` (path), `@Userid Long authenticatedUserId` |
| POST | `/subscribe` | 订阅用户 | `targetUserId` (query), `@Userid Long authenticatedUserId` |
| PUT | `/{id}` | 更新文章 | `id` (path), `ArticleUpdateVo` (body), `@Userid Long authenticatedUserId` |
| POST | `/{id}/addcoin` | 增加文章金币 | `id` (path), `amount` (query) |
| POST | `/{id}/read` | 增加阅读数量 | `id` (path) |
| POST | `/new` | 创建新文章 | `ArticleCreateVo` (body), `@Userid Long authenticatedUserId` |
| POST | `/userinform` | 获取用户文章统计 | `userId` (query) |
| GET | `/myarticles` | 获取用户文章列表 | `userId` (query) |
| POST | `/{id}/favorite` | 收藏文章 | `id` (path), `@Userid Long authenticatedUserId` |
| DELETE | `/{id}/favorite` | 取消收藏文章 | `id` (path), `@Userid Long authenticatedUserId` |
| GET | `/{id}/favorite/status` | 检查收藏状态 | `id` (path), `@Userid Long authenticatedUserId` |
| GET | `/favorites` | 获取用户收藏文章 | `@Userid Long authenticatedUserId`, `page`, `pageSize` (query) |
| GET | `/categories` | 获取文章分类 | 无 |
| GET | `/recommended` | 获取推荐文章 | `page`, `pageSize` (query) |
| GET | `/search` | 搜索文章 | `query`, `page`, `pageSize`, `sortBy`, `sortOrder` (query) |
| GET | `/search/advanced` | 高级搜索文章 | `ArticleSearchAdvancedVo` (query params) |
| GET | `/getall` | 获取所有文章（分页） | `page`, `pageSize`, `sortBy`, `sortOrder` (query) |
| DELETE | `/{id}` | 删除文章 | `id` (path), `@Userid Long authenticatedUserId` |
| DELETE | `/{id}/admin` | 管理员删除文章 | `id` (path), `@Userid Long authenticatedUserId`, `reason` (body) |
| GET | `/moderation/pending` | 获取待审核文章 | `page`, `pageSize`, `status`, `keyword` (query) |
| POST | `/{id}/approve` | 审核通过文章 | `id` (path) |
| POST | `/{id}/reject` | 审核拒绝文章 | `id` (path), `reason` (body) |
| GET | `/moderation/statistics` | 获取审核统计 | 无 |

### 8. AIController (AI功能)
**基路径**: `/api/ai`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/article/summary` | 生成文章摘要 | `ArticleSummaryRequestVo` (body) |
| POST | `/text/refine` | 润色文本内容 | `TextRefineRequestVo` (body) |
| POST | `/article/titles` | 生成文章标题建议 | `TitleSuggestionRequestVo` (body) |
| POST | `/chat` | AI聊天对话 | `ChatRequestVo` (body), `@Userid Long userId` |
| POST | `/sentiment/analyze` | 分析内容情感 | `SentimentAnalysisRequestVo` (body) |
| POST | `/keywords/extract` | 提取关键词 | `KeywordsExtractionRequestVo` (body) |
| POST | `/text/translate` | 翻译文本 | `TextTranslationRequestVo` (body) |
| POST | `/article/tags` | 生成文章标签 | `ArticleTagsGenerationRequestVo` (body) |
| POST | `/content/compliance` | 检查内容合规性 | `ContentComplianceCheckRequestVo` (body) |
| POST | `/reply/suggestions` | 生成回复建议 | `ReplySuggestionsRequestVo` (body) |
| POST | `/conversation/summary` | 总结对话历史 | `ConversationSummaryRequestVo` (body) |
| POST | `/content/suggestions` | 生成内容创作建议 | `ContentSuggestionsRequestVo` (body) |
| POST | `/text/proofread` | 校对和修正文本 | `TextProofreadRequestVo` (body) |
| POST | `/content/outline` | 生成内容大纲 | `ContentOutlineRequestVo` (body) |
| GET | `/config` | 获取AI配置信息 | 无 |

### 9. WebSocketMessageController (WebSocket消息)
**基路径**: `/app`

| WebSocket方法 | 路径 | 描述 | 参数 |
|---------------|------|------|------|
| @SubscribeMapping | `/chat/connect` | 连接事件处理 | `principal`, `headerAccessor` |
| @MessageMapping | `/chat.sendMessage` | 发送聊天消息 | `message` (body), `roomId`, `principal` |
| @MessageMapping | `/chat/join/{roomId}` | 用户加入聊天室 | `roomId` (path), `principal` |
| @MessageMapping | `/chat/leave/{roomId}` | 用户离开聊天室 | `roomId` (path), `principal` |
| @MessageMapping | `/chat/typing/{roomId}` | 用户正在输入 | `roomId` (path), `principal` |
| @MessageMapping | `/chat/private` | 发送私聊消息 | `message` (body), `principal` |
| @MessageMapping | `/chat/recall/{messageId}` | 撤回消息 | `messageId` (path), `roomId` (path), `principal` |
| @MessageMapping | `/chat/heartbeat` | 处理心跳消息 | `principal`, `headerAccessor` |
| @MessageMapping | `/chat/read-receipt` | 处理已读回执 | `receipt` (body), `principal` |

#### WebSocket订阅队列

前端需要订阅以下队列以接收实时消息：

| 队列路径 | 描述 | 消息格式 |
|---------|------|---------|
| `/user/{username}/queue/private` | 私聊消息 | `MessageResponse` |
| `/user/{username}/queue/chat-list-update` | 聊天列表更新 | `ChatList` |
| `/user/{username}/queue/message-status` | 消息状态更新 | `{ messageId, status, timestamp }` |
| `/user/{username}/queue/read-receipt` | 已读回执 | `{ chatId, messageId, timestamp, status }` |
| `/user/{username}/queue/group-member-change` | 群组成员变更 | `GroupMemberChangeEvent` |
| `/user/{username}/queue/group-info-change` | 群组信息变更 | `GroupInfoChangeEvent` |
| `/user/{username}/queue/errors` | 错误消息 | `{ type, message, clientMessageId, timestamp }` |

### 10. WebSocketMonitorController (WebSocket监控)
**基路径**: `/api/websocket/monitor`

| 方法 | 路径 | 描述 | 权限要求 |
|------|------|------|---------|
| GET | `/online-count` | 获取在线用户数 | 无 |
| GET | `/online-users` | 获取在线用户列表 | `ADMIN` |
| GET | `/user/{userId}/online` | 检查用户是否在线 | 无 |
| GET | `/user/{userId}/info` | 获取用户连接信息 | `ADMIN` 或 用户本人 |
| GET | `/statistics` | 获取连接统计信息 | `ADMIN` |
| POST | `/clean-expired` | 手动清理过期连接 | `ADMIN` |
| GET | `/user/{userId}/sessions` | 获取用户活跃会话列表 | `ADMIN` 或 用户本人 |

### 11. MessageThreadController (消息线程管理)
**基路径**: `/api/threads`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建消息线程 | `CreateThreadRequest` (body), `@Userid Long userId` |
| GET | `/{threadId}` | 获取消息线程详情 | `threadId` (path) |
| GET | `/{threadId}/messages` | 获取线程消息列表 | `threadId` (path), `page`, `pageSize` (query) |
| POST | `/{threadId}/replies` | 回复消息到线程 | `threadId` (path), `ReplyRequest` (body), `@Userid Long userId` |
| POST | `/{threadId}/join` | 加入线程 | `threadId` (path), `@Userid Long userId` |
| DELETE | `/{threadId}/leave` | 离开线程 | `threadId` (path), `@Userid Long userId` |
| POST | `/{threadId}/archive` | 归档线程 | `threadId` (path), `@Userid Long userId` |
| POST | `/{threadId}/close` | 关闭线程 | `threadId` (path), `@Userid Long userId` |
| POST | `/{threadId}/pin` | 置顶/取消置顶线程 | `threadId` (path), `isPinned` (query), `@Userid Long userId` |
| POST | `/{threadId}/lock` | 锁定/解锁线程 | `threadId` (path), `isLocked` (query), `@Userid Long userId` |
| GET | `/my-threads` | 获取用户参与的线程列表 | `page`, `pageSize` (query), `@Userid Long userId` |
| GET | `/active` | 获取活跃线程列表 | `page`, `pageSize` (query) |
| GET | `/created` | 获取用户创建的线程列表 | `page`, `pageSize` (query), `@Userid Long userId` |
| GET | `/search` | 搜索线程 | `keyword` (query), `page`, `pageSize` (query) |
| GET | `/{threadId}/statistics` | 获取线程统计信息 | `threadId` (path) |
| GET | `/context/{messageId}` | 获取消息的线程上下文 | `messageId` (path), `@Userid Long userId` |

### 12. ContentReportController (内容举报管理)
**基路径**: `/api/content-reports`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建内容举报 | `ContentReport` (body), `@Userid Long userId` |
| GET | `/pending` | 获取待处理举报列表 | `page`, `pageSize`, `contentType`, `reason`, `isUrgent` (query) |
| PUT | `/{reportId}/process` | 处理举报 | `reportId` (path), `processRequest` (body), `@Userid Long reviewerId` |
| PUT | `/batch-process` | 批量处理举报 | `batchRequest` (body), `@Userid Long reviewerId` |
| GET | `/my-reports` | 获取我的举报列表 | `@Userid Long userId`, `page`, `pageSize` (query) |
| GET | `/content/{contentType}/{contentId}` | 获取内容的举报列表 | `contentType` (path), `contentId` (path), `page`, `pageSize` (query) |
| GET | `/statistics` | 获取举报统计信息 | 无 |
| GET | `/top-reported` | 获取被举报最多的内容 | `limit` (query) |
| GET | `/reviewer-stats` | 获取审核员统计信息 | `reviewerId` (query), `days` (query) |
| PUT | `/{reportId}/withdraw` | 撤回举报 | `reportId` (path), `@Userid Long userId` |
| PUT | `/{reportId}/mark-urgent` | 标记举报为紧急 | `reportId` (path), `@Userid Long reviewerId` |
| GET | `/{reportId}` | 获取举报详情 | `reportId` (path) |
| GET | `/can-report` | 检查用户是否可以举报内容 | `contentType`, `contentId` (query), `@Userid Long userId` |

### 13. UserLevelIntegrationController (用户等级积分管理)
**基路径**: `/api/user-level-integration`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/handle-level-change` | 处理等级变更 | `userId`, `oldLevel`, `newLevel`, `changeReason`, `changeType`, `operatorId` (query) |
| POST | `/batch-handle-level-changes` | 批量处理等级变更 | `List<Map<String, Object>> levelChanges` (body) |
| GET | `/user/{userId}/complete-info` | 获取用户完整等级信息 | `userId` (path) |
| GET | `/validate-level-change` | 验证等级变更 | `userId`, `oldLevel`, `newLevel` (query) |

### 14. UserLevelHistoryController (用户等级历史管理)
**基路径**: `/api/user-level-history`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/{id}` | 根据ID获取等级历史记录 | `id` (path) |
| GET | `/user/{userId}` | 获取用户等级历史列表 | `userId` (path), `page`, `pageSize` (query) |
| POST | `/query` | 查询等级历史记录 | `UserLevelHistoryQueryVo` (body) |
| GET | `/user/{userId}/recent` | 获取用户最近等级变更 | `userId` (path), `limit` (query) |
| GET | `/user/{userId}/current-level` | 获取用户当前等级 | `userId` (path) |
| GET | `/user/{userId}/stats` | 获取用户等级统计信息 | `userId` (path), `days` (query) |
| GET | `/level-up` | 获取升级记录 | `userId`, `startTime`, `endTime`, `limit` (query) |
| GET | `/level-down` | 获取降级记录 | `userId`, `startTime`, `endTime`, `limit` (query) |
| GET | `/user/{userId}/count` | 获取用户等级变更次数 | `userId` (path) |

### 15. NotificationController (通知管理)
**基路径**: `/api/notifications`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/test` | 发送测试通知 | 无 |
| GET | `/` | 获取用户通知列表 | `page`, `size` (query) |
| GET | `/unread-count` | 获取未读通知数量 | 无 |
| POST | `/read-all` | 标记所有通知为已读 | 无 |
| POST | `/{id}/read` | 标记通知为已读 | `id` (path) |
| DELETE | `/read` | 删除所有已读通知 | 无 |

### 16. MigrationController (数据迁移管理)
**基路径**: `/api/migration`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/validate/pre` | 验证迁移前状态 | 无 |
| GET | `/validate/post` | 验证迁移后状态 | 无 |
| POST | `/create-missing-stats` | 创建缺失的统计数据 | 无 |
| GET | `/status` | 获取迁移状态 | 无 |

### 17. ArticleCommentController (文章评论管理)
**基路径**: `/api/articles/{articleId}/comments`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/` | 获取文章评论列表 | `articleId` (path) |
| POST | `/` | 添加文章评论 | `articleId` (path), `ArticleCommentVo` (body), `@Userid Long userId` |
| DELETE | `/{commentId}` | 删除评论 | `articleId` (path), `commentId` (path), `@Userid Long userId` |
| GET | `/count` | 获取文章评论数量 | `articleId` (path) |

### 18. ArticleVersionController (文章版本管理)
**基路径**: `/api/articles/{articleId}/versions`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/` | 获取文章版本历史 | `articleId` (path) |
| GET | `/latest` | 获取文章最新版本 | `articleId` (path) |
| POST | `/auto-save` | 自动保存草稿 | `articleId` (path), `Map<String, String>` (body), `@Userid Long userId` |
| GET | `/statistics` | 获取版本统计信息 | `articleId` (path) |

### 19. UserFollowController (用户关注管理)
**基路径**: `/api/follow`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/{followeeId}` | 关注用户 | `followeeId` (path), `@Userid Long userId` |
| DELETE | `/{followeeId}` | 取消关注用户 | `followeeId` (path), `@Userid Long userId` |
| GET | `/check/{followeeId}` | 检查关注状态 | `followeeId` (path), `@Userid Long userId` |
| GET | `/following` | 获取关注列表 | `page`, `size` (query), `@Userid Long userId` |
| GET | `/followers` | 获取粉丝列表 | `page`, `size` (query), `@Userid Long userId` |
| GET | `/stats` | 获取关注统计 | `@Userid Long userId` |
| GET | `/stats/{targetUserId}` | 获取指定用户关注统计 | `targetUserId` (path) |

### 20. SocialRelationshipController (社交关系管理)
**基路径**: `/api/social`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/relationship/{targetUserId}` | 获取两个用户间的关系 | `targetUserId` (path), `@Userid Long userId` |
| POST | `/follow/{targetUserId}` | 关注用户 | `targetUserId` (path), `@Userid Long userId` |
| DELETE | `/follow/{targetUserId}` | 取消关注用户 | `targetUserId` (path), `@Userid Long userId` |
| DELETE | `/friend/{friendId}` | 删除好友关系 | `friendId` (path), `keepFollow` (query), `@Userid Long userId` |
| GET | `/friends` | 获取好友列表 | `@Userid Long userId` |
| GET | `/following` | 获取关注列表 | `@Userid Long userId` |
| GET | `/followers` | 获取粉丝列表 | `@Userid Long userId` |
| GET | `/mutual-follow` | 获取互相关注列表 | `@Userid Long userId` |
| GET | `/recommendations/friends` | 获取好友推荐 | `@Userid Long userId`, `limit` (query) |
| GET | `/recommendations/follow` | 获取关注推荐 | `@Userid Long userId`, `limit` (query) |
| GET | `/statistics` | 获取社交统计信息 | `@Userid Long userId` |

### 21. ContactGroupController (联系人分组管理)
**基路径**: `/api/contact-groups`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建联系人分组 | `Map<String, Object>` (body), `@Userid Long userId` |
| GET | `/` | 获取用户联系人分组列表 | `@Userid Long userId` |
| PUT | `/{groupId}/name` | 更新分组名称 | `groupId` (path), `Map<String, String>` (body), `@Userid Long userId` |
| PUT | `/{groupId}/order` | 更新分组排序 | `groupId` (path), `Map<String, Integer>` (body), `@Userid Long userId` |
| DELETE | `/{groupId}` | 删除分组 | `groupId` (path), `@Userid Long userId` |
| POST | `/{groupId}/contacts/{contactId}` | 添加联系人到分组 | `groupId` (path), `contactId` (path), `@Userid Long userId` |
| DELETE | `/contacts/{contactId}` | 从分组移除联系人 | `contactId` (path), `@Userid Long userId` |
| GET | `/{groupId}/contacts` | 获取分组联系人列表 | `groupId` (path), `@Userid Long userId` |
| GET | `/default` | 获取默认分组 | `@Userid Long userId` |

### 22. RateLimitController (限流管理)
**基路径**: `/api/rate-limit`
**权限要求**: 所有接口需要 `ADMIN` 角色

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/config` | 配置限流规则 | `path`, `maxRequests` (query) |
| GET | `/config` | 获取限流配置 | `path` (query) |
| DELETE | `/config` | 删除限流配置 | `path` (query) |
| GET | `/config/all` | 获取所有限流配置 | 无 |
| GET | `/statistics` | 获取限流统计 | 无 |
| GET | `/events` | 获取限流事件 | `limit` (query) |
| GET | `/alerts` | 获取限流告警 | 无 |
| DELETE | `/statistics` | 清空限流统计 | 无 |
| POST | `/unlock` | 解锁限流 | `identifier`, `path`, `type` (query) |

### 23. DiagnosticController (系统诊断)
**基路径**: `/api/diagnostic`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/cache/clear-user-stats` | 清空用户统计缓存 | 无 |
| GET | `/health` | 系统健康检查 | 无 |

### 24. UnifiedMessageController (统一消息管理) ⚠️ 已废弃
**基路径**: `/api/messages`
**特殊注解**: 部分接口使用 `@UrlLimit`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/send` | 发送消息 | `SendMessageVo` (body), `@Userid Long userId` |
| POST | `/private` | 发送私聊消息 | `Map<String, Object>` (body), `@Userid Long userId` |
| POST | `/group` | 发送群聊消息 | `Map<String, Object>` (body), `@Userid Long userId` |
| GET | `/` | 获取消息列表 | `@Userid Long userId`, `page`, `size` (query) |
| GET | `/private/{targetUserId}` | 获取私聊消息 | `targetUserId` (path), `@Userid Long userId`, `page`, `size` (query) |
| GET | `/group/{groupId}` | 获取群聊消息 | `groupId` (path), `@Userid Long userId`, `page`, `size` (query) |
| GET | `/chats` | 获取聊天会话列表 | `@Userid Long userId` |
| GET | `/unread/stats` | 获取未读消息统计 | `@Userid Long userId` |
| POST | `/{messageId}/read` | 标记消息为已读 | `messageId` (path), `@Userid Long userId` |
| POST | `/private/{targetUserId}/read` | 标记私聊消息为已读 | `targetUserId` (path), `@Userid Long userId` |
| POST | `/group/{groupId}/read` | 标记群聊消息为已读 | `groupId` (path), `@Userid Long userId` |
| DELETE | `/{messageId}` | 删除消息 | `messageId` (path), `@Userid Long userId` |
| POST | `/{messageId}/recall` | 撤回消息 | `messageId` (path), `@Userid Long userId` |
| GET | `/search` | 搜索消息 | `keyword` (query), `@Userid Long userId`, `page`, `size` (query) |
| GET | `/{messageId}` | 获取消息详情 | `messageId` (path), `@Userid Long userId` |

---

## 其他重要信息

### 实际控制器统计
本项目实际包含 **24个控制器**，涵盖了完整的社交平台功能：

- **核心功能**: 认证、用户管理、群组管理、联系人管理
- **聊天系统**: 实时聊天、消息管理、消息线程、WebSocket通信
- **内容管理**: 文章系统、评论、版本控制、内容审核
- **AI功能**: 智能处理、内容生成、情感分析、多语言支持
- **搜索服务**: 全文搜索、多类型搜索、高级搜索
- **管理功能**: 用户等级、通知、举报、限流、系统诊断
- **数据迁移**: 数据迁移工具和统计管理

### 特殊注解说明

#### 1. 认证相关注解
- `@Userid`: 自定义注解，从SecurityContext中自动提取当前用户ID，无需手动传递
- `@PreAuthorize`: Spring Security注解，用于权限控制（如：`hasRole('ADMIN')`）

#### 2. 功能性注解
- `@UrlLimit`: 自定义注解，应用接口限流保护，防止接口滥用
- `@Deprecated`: 标记已废弃的功能（如UnifiedMessageController）

#### 3. 数据验证注解
- `@RequestBody @Valid`: 请求体验证，确保数据格式正确
- `@RequestParam`: 查询参数验证
- `@PathVariable`: 路径参数验证
- `@ModelAttribute`: 模型属性绑定

### 权限控制说明

#### 1. 管理员权限接口
以下接口需要 `ADMIN` 角色权限：
- RateLimitController 的所有接口
- WebSocketMonitorController 的大部分监控接口
- 用户管理中的封禁、重置密码等管理功能

#### 2. 用户权限接口
大部分接口需要用户登录认证，通过 `@Userid` 自动获取用户ID

#### 3. 公开接口
部分接口无需认证即可访问，如：
- 用户信息查询
- 文章公开内容
- 搜索功能

### WebSocket 实时通信

#### 1. 连接管理
- 支持多设备同时在线
- 自动心跳保活机制
- 连接状态监控和管理

#### 2. 消息类型
- 私聊消息：点对点实时通信
- 群聊消息：多用户群组通信
- 系统消息：通知和状态更新
- 状态消息：在线状态、输入状态等

#### 3. 订阅机制
- 用户个人队列：`/user/{username}/queue/*`
- 群组主题：`/topic/group/{groupId}`
- 系统广播：`/topic/system`

### API 设计规范

#### 1. RESTful 设计
- 遵循REST架构风格
- 统一的响应格式（ApiResponse）
- 合理的HTTP状态码使用

#### 2. 分页规范
- 统一的分页参数：`page`, `pageSize/size`
- 默认值：page=1, pageSize=10/20
- 支持排序和过滤

#### 3. 错误处理
- 统一的异常处理机制
- 详细的错误信息返回
- 多语言错误消息支持

---

## 数据传输对象 (DTO/VO) 说明

### 1. 用户相关
- `UserDto`: 用户基本信息
- `UserWithStats`: 用户信息含统计数据
- `UpdateUserVo`: 用户更新信息
- `AdminResetPasswordRequestVo`: 管理员重置密码

### 2. 群组相关
- `GroupDto`: 群组详细信息
- `GroupCreateVo`: 群组创建信息
- `GroupInviteVo`: 群组邀请信息
- `GroupApplication`: 群组申请实体类
- `GroupMember`: 群组成员信息

### 3. 联系人相关
- `ContactApplyVo`: 联系人申请信息
- `ContactDto`: 联系人详细信息

### 4. 聊天相关
- `ChatCreateVo`: 聊天创建信息
- `ChatMessageVo`: 聊天消息内容
- `ChatMessagesVo`: 聊天消息分页参数
- `SendMessageVo`: 发送消息封装

### 5. 文章相关
- `ArticleCreateVo`: 文章创建信息
- `ArticleUpdateVo`: 文章更新信息
- `ArticleSearchAdvancedVo`: 高级搜索参数
- `ArticleCommentVo`: 文章评论信息

### 6. AI功能相关
- `ChatRequestVo`: AI聊天请求
- `ArticleSummaryRequestVo`: 文章摘要请求
- `TextRefineRequestVo`: 文本润色请求
- `TitleSuggestionRequestVo`: 标题建议请求
- `SentimentAnalysisRequestVo`: 情感分析请求
- `KeywordsExtractionRequestVo`: 关键词提取请求
- `TextTranslationRequestVo`: 文本翻译请求
- `ArticleTagsGenerationRequestVo`: 文章标签生成请求
- `ContentComplianceCheckRequestVo`: 内容合规检查请求
- `ReplySuggestionsRequestVo`: 回复建议请求
- `ConversationSummaryRequestVo`: 对话总结请求
- `ContentSuggestionsRequestVo`: 内容创作建议请求
- `TextProofreadRequestVo`: 文本校对请求
- `ContentOutlineRequestVo`: 内容大纲请求

### 7. 认证相关
- `RegistrationVo`: 用户注册信息
- `LoginVo`: 用户登录信息
- `PasswordChangeVo`: 密码修改信息
- `PasswordResetVo`: 密码重置信息
- `ForgotPasswordVo`: 忘记密码信息

### 8. 系统管理相关
- `RateLimitConfigVo`: 限流配置信息
- `MigrationRequest`: 数据迁移请求
- `ContentReport`: 内容举报信息
- `CreateThreadRequest`: 消息线程创建请求
- `ReplyRequest`: 回复请求

### 9. 用户等级相关
- `LevelChangeRequest`: 等级变更请求
- `BatchLevelChangeRequest`: 批量等级变更请求
- `UserLevelHistoryQueryVo`: 用户等级历史查询

---

## 更新日志

### 当前版本特性
- ✅ **24个控制器**: 完整的社交平台API
- ✅ **实时通信**: WebSocket支持完整的聊天功能
- ✅ **AI集成**: 15+种AI功能接口
- ✅ **内容管理**: 文章、评论、版本控制系统
- ✅ **搜索服务**: 基于Elasticsearch的全文搜索
- ✅ **权限控制**: 细粒度的权限管理系统
- ✅ **限流保护**: 接口级别的限流控制
- ✅ **数据迁移**: 完整的数据迁移工具

### 标记说明
- ✅ **已完成**: 功能已实现并可正常使用
- ⚠️ **已废弃**: 功能将在新版本中移除，建议使用替代方案
- 🔒 **需权限**: 接口需要特定权限才能访问
- @Userid **自动获取**: 当前用户ID由系统自动注入

---