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
- **注解驱动**: 自定义注解简化开发
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
| POST | `/validate` | 验证邮箱 | `email` (query) |
| POST | `/change-password` | 修改密码 | `PasswordChangeVo` (body), `userId` (@Userid) |
| POST | `/forgot-password` | 忘记密码 | `ForgotPasswordVo` (body) |
| POST | `/reset-password` | 重置密码 | `PasswordResetVo` (body) |
| GET | `/verify-reset-token` | 验证重置令牌 | `token` (query), `email` (query) |

### 2. StandardUserController (用户管理) ✅ 规范化用户API
**基路径**: `/api/users`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/me` | 获取当前用户信息 | 无 (SecurityUtils获取userId) |
| GET | `/me/profile` | 获取当前用户完整信息 | 无 (SecurityUtils获取userId) |
| GET | `/me/info` | 获取当前用户基本信息 | 无 (SecurityUtils获取userId) |
| PUT | `/me` | 更新当前用户信息 | `UpdateUserVo` (body) |
| GET | `/{userId}` | 获取指定用户信息 | `userId` (path) |
| GET | `/by-username/{username}` | 通过用户名获取用户信息 ✨新增 | `username` (path) |
| GET | `/` | 获取用户列表（分页） | `page`, `pageSize`, `keyword` (query) |
| GET | `/search` | 搜索用户 | `q`, `limit` (query) |
| GET | `/me/groups` | 获取当前用户的群组列表 | 无 (SecurityUtils获取userId) |
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
| POST | `/` | 创建群组 | `GroupCreateVo` (body), `userId` (@Userid) |
| GET | `/{groupId}` | 获取群组详情 | `groupId` (path), `userId` (@Userid) |
| PUT | `/{groupId}` | 更新群组信息 | `groupId` (path), `GroupCreateVo` (body), `userId` (@Userid) |
| DELETE | `/{groupId}` | 删除群组 | `groupId` (path), `userId` (@Userid) |
| GET | `/{groupId}/members` | 获取群组成员列表 | `groupId` (path) |
| POST | `/{groupId}/members` | 邀请用户加入群组 | `groupId` (path), `GroupInviteVo` (body), `userId` (@Userid) |
| DELETE | `/{groupId}/members/me` | 退出群组 | `groupId` (path), `userId` (@Userid) |
| DELETE | `/{groupId}/members/{userId}` | 移除群组成员 | `groupId` (path), `userId` (path), `currentUserId` (@Userid) |
| POST | `/{groupId}/applications` | 申请加入群组 | `groupId` (path), `Map<String, String>` (body), `userId` (@Userid) |
| GET | `/{groupId}/applications` | 获取群组申请列表 | `groupId` (path) |
| PUT | `/{groupId}/applications/{applicationId}` | 处理群组申请 | `groupId` (path), `applicationId` (path), `Map<String, String>` (body), `userId` (@Userid) |
| PUT | `/{groupId}/members/{userId}/role` | 设置群组管理员 | `groupId` (path), `userId` (path), `Map<String, String>` (body), `currentUserId` (@Userid) |
| GET | `/search` | 搜索群组 | `q`, `limit` (query) |
| GET | `/my-groups` | 获取用户加入的群组列表 | `userId` (@Userid) |
| GET | `/my-created` | 获取用户创建的群组列表 | `userId` (@Userid) |

### 4. ContactController (联系人管理)
**基路径**: `/api/contacts`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/apply` | 发送好友申请（通过用户ID） | `ContactApplyVo` (body), `userId` (@Userid) |
| POST | `/request` | 发送好友申请（兼容前端 /request 路径） | `Map<String, Object>` (body), `userId` (@Userid) |
| POST | `/request/by-username` | 通过用户名发送好友申请 | `Map<String, String>` (body), `userId` (@Userid) |
| POST | `/accept/{contactId}` | 同意好友申请 | `contactId` (path), `userId` (@Userid) |
| POST | `/request/{requestId}/accept` | 同意好友申请（兼容路径） | `requestId` (path), `userId` (@Userid) |
| POST | `/decline/{contactId}` | 拒绝好友申请 | `contactId` (path), `userId` (@Userid) |
| POST | `/request/{requestId}/reject` | 拒绝好友申请（兼容路径） | `requestId` (path), `userId` (@Userid) |
| POST | `/block/{contactId}` | 拉黑联系人 | `contactId` (path), `userId` (@Userid) |
| GET | `/` | 获取联系人列表 | `userId` (@Userid), `status` (query, ContactStatus) |
| GET | `/requests` | 获取待处理的好友申请列表 | `userId` (@Userid) |
| POST | `/groups` | 创建联系人分组 | `Map<String, Object>` (body), `userId` (@Userid) |
| GET | `/groups` | 获取用户的所有联系人分组 | `userId` (@Userid) |
| PUT | `/groups/{groupId}/name` | 更新分组名称 | `groupId` (path), `Map<String, String>` (body), `userId` (@Userid) |
| PUT | `/groups/{groupId}/order` | 更新分组排序 | `groupId` (path), `Map<String, Integer>` (body), `userId` (@Userid) |
| DELETE | `/groups/{groupId}` | 删除分组 | `groupId` (path), `userId` (@Userid) |
| POST | `/groups/{groupId}/contacts/{contactId}` | 将联系人添加到分组 | `groupId` (path), `contactId` (path), `userId` (@Userid) |
| DELETE | `/groups/contacts/{contactId}` | 从分组中移除联系人 | `contactId` (path), `userId` (@Userid) |
| GET | `/groups/{groupId}/contacts` | 获取指定分组的联系人列表 | `groupId` (path), `userId` (@Userid) |

### 5. ChatController (聊天管理) ✅ 统一聊天API
**基路径**: `/api/chats`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/` | 获取用户的聊天列表 | `userId` (@Userid) |
| POST | `/` | 创建新的聊天会话 | `userId` (@Userid), `ChatCreateVo` (body) |
| GET | `/{chatId}/messages` | 获取聊天消息历史记录 | `chatId` (path), `ChatMessagesVo` (@ModelAttribute) |
| POST | `/{chatId}/messages` | 发送聊天消息 | `chatId` (path), `userId` (@Userid), `ChatMessageVo` (body) |
| POST | `/{chatId}/read` | 标记消息为已读 | `chatId` (path), `userId` (@Userid) |
| POST | `/read/batch` | 批量标记已读 | `chatIds` (body), `userId` (@Userid) |
| DELETE | `/{chatId}` | 删除聊天会话 | `chatId` (path), `userId` (@Userid) |
| POST | `/messages/{messageId}/react` | 对消息添加反应 | `messageId` (path), `userId` (@Userid), `reactionType` (query) |
| DELETE | `/messages/{messageId}` | 撤回消息 | `messageId` (path), `userId` (@Userid) |
| GET | `/unread/stats` | 获取未读消息统计 | `userId` (@Userid) |
| GET | `/{chatId}/unread` | 获取单个聊天未读数 | `chatId` (path), `userId` (@Userid) |
| GET | `/groups/{groupId}/unread` | 获取群组未读数 | `groupId` (path), `userId` (@Userid) |
| GET | `/online-users` | 获取在线用户列表 | 无 |
| GET | `/users/{targetUserId}/online` | 检查用户是否在线 | `targetUserId` (path) |

### 6. SearchController (全局搜索)
**基路径**: `/api/search`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/messages` | 搜索消息内容 | `q`, `page`, `size`, `startDate`, `endDate`, `messageTypes`, `userIds`, `groupIds`, `sortBy` (query) |
| GET | `/group` | 搜索公开群组 | `keyword`, `page`, `size`, `startDate`, `endDate`, `sortBy` (query) |
| GET | `/users` | 搜索用户 | `keyword`, `page`, `size`, `startDate`, `endDate`, `sortBy` (query) |
| GET | `/articles` | 搜索文章 | `query`, `page`, `pageSize`, `startDate`, `endDate`, `sortBy`, `sortOrder` (query) |
| GET | `/all` | 综合搜索 | `q`, `page`, `size` (query) |

### 7. ArticleCenterController (文章中心)
**基路径**: `/api/articles`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/{id}` | 根据ID获取文章信息 | `id` (path) |
| ~~GET~~ | ~~/userinform~~ | ❌ 已删除 - 请使用 `/api/users/{userId}` | ~~userId (query)~~ |
| POST | `/{id}/like` | 文章点赞 | `id` (path) |
| GET | `/` | 获取文章列表 | `page`, `size` (query) |
| POST | `/` | 创建文章 | `ArticleCreateVo` (body), `userId` (@Userid) |
| PUT | `/{id}` | 更新文章 | `id` (path), `ArticleUpdateVo` (body), `userId` (@Userid) |
| DELETE | `/{id}` | 删除文章 | `id` (path), `userId` (@Userid) |
| GET | `/search` | 搜索文章 | `keyword`, `page`, `size` (query) |
| GET | `/advanced-search` | 高级搜索文章 | `ArticleSearchAdvancedVo` (query params) |
| GET | `/{id}/comments` | 获取文章评论 | `id` (path), `page`, `size` (query) |
| POST | `/{id}/comments` | 添加文章评论 | `id` (path), `CommentCreateVo` (body), `userId` (@Userid) |
| GET | `/categories` | 获取文章分类 | 无 |
| GET | `/tags` | 获取文章标签 | 无 |
| GET | `/hot` | 获取热门文章 | `limit` (query) |
| GET | `/recommend` | 获取推荐文章 | `userId` (@Userid), `limit` (query) |
| POST | `/{id}/collect` | 收藏文章 | `id` (path), `userId` (@Userid) |
| DELETE | `/{id}/collect` | 取消收藏文章 | `id` (path), `userId` (@Userid) |
| GET | `/user/{userId}/collected` | 获取用户收藏的文章 | `userId` (path), `page`, `size` (query) |
| GET | `/user/{userId}/published` | 获取用户发布的文章 | `userId` (path), `page`, `size` (query) |
| GET | `/statistics` | 获取文章统计信息 | 无 |
| DELETE | `/{id}/admin` | 管理员删除文章 | `id` (path) |
| GET | `/moderation/pending` | 获取待审核文章 | 无 |
| POST | `/{id}/approve` | 审核通过文章 | `id` (path), `userId` (@Userid) |
| POST | `/{id}/reject` | 审核拒绝文章 | `id` (path), `userId` (@Userid) |
| GET | `/moderation/statistics` | 获取审核统计 | 无 |

### 8. AIController (AI功能)
**基路径**: `/api/ai`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/article/summary` | 生成文章摘要 | `ArticleSummaryRequestVo` (body) |
| POST | `/text/refine` | 润色文本内容 | `TextRefineRequestVo` (body) |
| POST | `/article/titles` | 生成文章标题建议 | `TitleSuggestionRequestVo` (body) |
| POST | `/chat` | AI聊天对话 | `ChatRequestVo` (body), `userId` (@Userid) |
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

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| SUBSCRIBE | `/chat/connect` | 连接事件处理 | `principal` |
| MESSAGE | `/chat.sendMessage` | 发送聊天消息 | `message`, `roomId`, `principal` |
| MESSAGE | `/chat/join/{roomId}` | 用户加入聊天室 | `roomId`, `principal` |
| MESSAGE | `/chat/leave/{roomId}` | 用户离开聊天室 | `roomId`, `principal` |
| MESSAGE | `/chat/typing/{roomId}` | 用户正在输入 | `roomId`, `principal` |
| MESSAGE | `/chat/private` | 发送私聊消息 | `message`, `principal` |
| MESSAGE | `/chat/recall/{messageId}` | 撤回消息 | `messageId`, `roomId`, `principal` |
| MESSAGE | `/chat/heartbeat` | 处理心跳消息 | `principal`, `headerAccessor` |

#### WebSocket订阅队列

前端需要订阅以下队列以接收实时消息：

| 队列路径 | 描述 | 消息格式 |
|---------|------|---------|
| `/user/{username}/queue/private` | 私聊消息 | `MessageResponse` |
| `/user/{username}/queue/chat-list-update` | 聊天列表更新 | `ChatList` |
| `/user/{username}/queue/message-status` | 消息状态更新 | `{ messageId, status, timestamp }` |
| `/user/{username}/queue/read-receipt` | 已读回执 | `{ chatId, messageId, timestamp, status }` |
| `/user/{username}/queue/group-member-change` | 群组成员变更 ✨新增 | `GroupMemberChangeEvent` |
| `/user/{username}/queue/group-info-change` | 群组信息变更 ✨新增 | `GroupInfoChangeEvent` |
| `/user/{username}/queue/errors` | 错误消息 | `{ type, message, clientMessageId, timestamp }` |

#### 群组成员变更事件 (GroupMemberChangeEvent)

```json
{
  "type": "GROUP_MEMBER_CHANGE",
  "groupId": 123,
  "changeType": "MEMBER_ADDED | MEMBER_REMOVED | MEMBER_LEFT | ROLE_CHANGED",
  "affectedUserId": 456,
  "affectedUsername": "user123",
  "affectedNickname": "张三",
  "affectedAvatar": "https://...",
  "operatorId": 789,
  "operatorUsername": "admin",
  "operatorNickname": "管理员",
  "timestamp": "2025-11-06T10:30:00Z",
  "additionalData": {
    "oldRole": 0,
    "newRole": 1,
    "reason": "..."
  }
}
```

#### 群组信息变更事件 (GroupInfoChangeEvent)

```json
{
  "type": "GROUP_INFO_CHANGE",
  "groupId": 123,
  "changeType": "INFO_UPDATED | OWNER_TRANSFERRED | GROUP_DISSOLVED",
  "operatorId": 789,
  "operatorUsername": "admin",
  "operatorNickname": "管理员",
  "timestamp": "2025-11-06T10:30:00Z",
  "oldGroupName": "旧群名",
  "newGroupName": "新群名",
  "oldGroupAvatarUrl": "https://...",
  "newGroupAvatarUrl": "https://...",
  "oldOwnerId": 456,
  "newOwnerId": 789
}
```

### 10. WebSocketMonitorController (WebSocket监控)
**基路径**: `/api/websocket/monitor`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/online-count` | 获取在线用户数 | 无 |
| GET | `/online-users` | 获取在线用户列表 | 无 |
| GET | `/user/{userId}/online` | 检查用户是否在线 | `userId` (path) |
| GET | `/user/{userId}/info` | 获取用户连接信息 | `userId` (path) |
| GET | `/statistics` | 获取连接统计信息 | 无 |
| POST | `/clean-expired` | 手动清理过期连接 | 无 |
| GET | `/user/{userId}/sessions` | 获取用户活跃会话列表 | `userId` (path) |

### 11. UnifiedMessageController (统一消息管理) ⚠️ 已废弃
**基路径**: `/api/messages`
**状态**: 已标记为 @Deprecated，请使用 ChatController (`/api/chats`)

> ⚠️ **重要提示**: 此Controller已废弃，所有功能已迁移到ChatController。
> 请参考 [API迁移指南](../API_MIGRATION_GUIDE.md) 了解如何迁移到新API。

| 方法 | 路径 | 描述 | 迁移到 |
|------|------|------|--------|
| POST | `/send` | 发送消息 | `POST /api/chats/{chatId}/messages` |
| GET | `/history` | 获取消息历史 | `GET /api/chats/{chatId}/messages` |
| GET | `/unread` | 获取未读消息 | `GET /api/chats/unread/stats` |
| PUT | `/{messageId}/read` | 标记消息为已读 | `POST /api/chats/{chatId}/read` |
| DELETE | `/{messageId}` | 删除消息 | `DELETE /api/chats/messages/{messageId}` |
| POST | `/{messageId}/reaction` | 添加消息反应 | `POST /api/chats/messages/{messageId}/react` |
| POST | `/{messageId}/recall` | 撤回消息 | `DELETE /api/chats/messages/{messageId}` |
| GET | `/search` | 搜索消息 | 使用 SearchController |

### 12. MessageThreadController (消息线程管理)
**基路径**: `/api/threads`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建消息线程 | `CreateThreadRequest` (body), `userId` (@Userid) |
| GET | `/{threadId}` | 获取消息线程详情 | `threadId` (path) |
| GET | `/{threadId}/messages` | 获取线程消息列表 | `threadId` (path), `page`, `pageSize` (query) |
| POST | `/{threadId}/replies` | 回复消息到线程 | `threadId` (path), `ReplyRequest` (body), `userId` (@Userid) |
| POST | `/{threadId}/join` | 加入线程 | `threadId` (path), `userId` (@Userid) |
| DELETE | `/{threadId}/leave` | 离开线程 | `threadId` (path), `userId` (@Userid) |
| POST | `/{threadId}/archive` | 归档线程 | `threadId` (path), `userId` (@Userid) |
| POST | `/{threadId}/close` | 关闭线程 | `threadId` (path), `userId` (@Userid) |
| POST | `/{threadId}/pin` | 置顶/取消置顶线程 | `threadId` (path), `isPinned` (query), `userId` (@Userid) |
| POST | `/{threadId}/lock` | 锁定/解锁线程 | `threadId` (path), `isLocked` (query), `userId` (@Userid) |
| GET | `/my-threads` | 获取用户参与的线程列表 | `page`, `pageSize` (query), `userId` (@Userid) |
| GET | `/active` | 获取活跃线程列表 | `page`, `pageSize` (query) |
| GET | `/created` | 获取用户创建的线程列表 | `page`, `pageSize` (query), `userId` (@Userid) |
| GET | `/search` | 搜索线程 | `keyword` (query), `page`, `pageSize` (query) |
| GET | `/{threadId}/statistics` | 获取线程统计信息 | `threadId` (path) |
| GET | `/context/{messageId}` | 获取消息的线程上下文 | `messageId` (path), `userId` (@Userid) |

### 13. ContentReportController (内容举报管理)
**基路径**: `/api/content-reports`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建内容举报 | `ContentReport` (body), `userId` (@Userid) |
| GET | `/pending` | 获取待处理举报列表 | `page`, `size` (query) |
| PUT | `/{reportId}/process` | 处理举报 | `reportId` (path), `ContentReportUpdateVo` (body), `userId` (@Userid) |
| PUT | `/batch-process` | 批量处理举报 | `BatchProcessRequest` (body), `userId` (@Userid) |
| GET | `/my-reports` | 获取我的举报列表 | `page`, `size` (query), `userId` (@Userid) |
| GET | `/content/{contentType}/{contentId}` | 获取内容的举报列表 | `contentType` (path), `contentId` (path), `page`, `size` (query) |
| GET | `/statistics` | 获取举报统计信息 | 无 |
| GET | `/top-reported` | 获取被举报最多的内容 | `limit` (query) |
| GET | `/reviewer-stats` | 获取审核员统计信息 | `reviewerId` (query) |
| PUT | `/{reportId}/withdraw` | 撤回举报 | `reportId` (path), `userId` (@Userid) |
| PUT | `/{reportId}/mark-urgent` | 标记举报为紧急 | `reportId` (path), `userId` (@Userid) |
| GET | `/{reportId}` | 获取举报详情 | `reportId` (path) |
| GET | `/can-report` | 检查用户是否可以举报内容 | `userId`, `contentType`, `contentId` (query) |

### 14. UserLevelIntegrationController (用户等级积分管理)
**基路径**: `/api/user-level-integration`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/handle-level-change` | 处理等级变更 | `LevelChangeRequest` (body) |
| POST | `/batch-handle-level-changes` | 批量处理等级变更 | `BatchLevelChangeRequest` (body) |
| GET | `/user/{userId}/complete-info` | 获取用户完整等级信息 | `userId` (path) |
| GET | `/validate-level-change` | 验证等级变更 | `userId`, `newLevel`, `reason` (query) |

### 15. UserLevelHistoryController (用户等级历史管理)
**基路径**: `/api/user-level-history`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/{id}` | 根据ID获取等级历史记录 | `id` (path) |
| GET | `/user/{userId}` | 获取用户等级历史列表 | `userId` (path), `page`, `size` (query) |
| POST | `/query` | 查询等级历史记录 | `UserLevelHistoryQuery` (body) |
| GET | `/user/{userId}/recent` | 获取用户最近等级变更 | `userId` (path), `limit` (query) |
| GET | `/user/{userId}/current-level` | 获取用户当前等级 | `userId` (path) |
| GET | `/user/{userId}/stats` | 获取用户等级统计信息 | `userId` (path) |
| GET | `/level-up` | 获取升级记录 | `page`, `size` (query) |
| GET | `/level-down` | 获取降级记录 | `page`, `size` (query) |
| GET | `/user/{userId}/count` | 获取用户等级变更次数 | `userId` (path) |

### 16. NotificationController (通知管理)
**基路径**: `/api/notifications`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/` | 获取用户通知列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/unread` | 获取未读通知 | `userId` (@Userid) |
| PUT | `/{notificationId}/read` | 标记通知为已读 | `notificationId` (path), `userId` (@Userid) |
| PUT | `/read-all` | 标记所有通知为已读 | `userId` (@Userid) |
| DELETE | `/{notificationId}` | 删除通知 | `notificationId` (path), `userId` (@Userid) |
| DELETE | `/read` | 删除所有已读通知 | `userId` (@Userid) |
| GET | `/count` | 获取通知数量统计 | `userId` (@Userid) |
| POST | `/test` | 发送测试通知 | `TestNotificationVo` (body) |
| GET | `/types` | 获取通知类型列表 | 无 |
| PUT | `/settings` | 更新通知设置 | `NotificationSettingsVo` (body), `userId` (@Userid) |
| GET | `/settings` | 获取通知设置 | `userId` (@Userid) |

### 17. MigrationController (数据迁移管理)
**基路径**: `/api/migration`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/start` | 开始数据迁移 | `MigrationRequest` (body), `operatorId` (@Userid) |
| GET | `/status/{migrationId}` | 获取迁移状态 | `migrationId` (path) |
| GET | `/history` | 获取迁移历史 | `page`, `size` (query) |
| POST | `/rollback/{migrationId}` | 回滚迁移 | `migrationId` (path), `operatorId` (@Userid) |
| GET | `/available` | 获取可用的迁移脚本 | 无 |
| POST | `/validate/{migrationId}` | 验证迁移结果 | `migrationId` (path) |
| DELETE | `/cleanup/{migrationId}` | 清理迁移临时数据 | `migrationId` (path), `operatorId` (@Userid) |
| GET | `/statistics` | 获取迁移统计信息 | 无 |

### 18. ArticleCommentController (文章评论管理)
**基路径**: `/api/article-comments`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/article/{articleId}` | 获取文章评论列表 | `articleId` (path), `page`, `size` (query) |
| POST | `/article/{articleId}` | 添加文章评论 | `articleId` (path), `ArticleCommentCreateVo` (body), `userId` (@Userid) |
| PUT | `/{commentId}` | 更新评论 | `commentId` (path), `ArticleCommentUpdateVo` (body), `userId` (@Userid) |
| DELETE | `/{commentId}` | 删除评论 | `commentId` (path), `userId` (@Userid) |
| POST | `/{commentId}/like` | 点赞评论 | `commentId` (path), `userId` (@Userid) |
| DELETE | `/{commentId}/like` | 取消点赞评论 | `commentId` (path), `userId` (@Userid) |
| GET | `/{commentId}/replies` | 获取评论回复 | `commentId` (path), `page`, `size` (query) |
| POST | `/{commentId}/reply` | 回复评论 | `commentId` (path), `ArticleCommentCreateVo` (body), `userId` (@Userid) |

### 19. UserFollowController (用户关注管理)
**基路径**: `/api/user-follows`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/{targetUserId}` | 关注用户 | `targetUserId` (path), `userId` (@Userid) |
| DELETE | `/{targetUserId}` | 取消关注用户 | `targetUserId` (path), `userId` (@Userid) |
| GET | `/status/{targetUserId}` | 获取关注状态 | `targetUserId` (path), `userId` (@Userid) |
| GET | `/followers` | 获取粉丝列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/following` | 获取关注列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/mutual` | 获取互相关注列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/count/{userId}` | 获取用户关注统计 | `userId` (path) |
| GET | `/recommendations` | 获取关注推荐 | `userId` (@Userid), `limit` (query) |

### 20. ArticleVersionController (文章版本管理)
**基路径**: `/api/article-versions`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/article/{articleId}` | 获取文章版本历史 | `articleId` (path), `page`, `size` (query) |
| GET | `/{versionId}` | 获取版本详情 | `versionId` (path) |
| POST | `/article/{articleId}/restore` | 恢复到指定版本 | `articleId` (path), `versionId` (query), `userId` (@Userid) |
| GET | `/article/{articleId}/compare` | 比较两个版本 | `articleId` (path), `versionId1`, `versionId2` (query) |
| DELETE | `/{versionId}` | 删除版本记录 | `versionId` (path), `userId` (@Userid) |

### 21. SocialRelationshipController (社交关系管理)
**基路径**: `/api/social`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| GET | `/relationship/{targetUserId}` | 获取两个用户间的关系 | `targetUserId` (path), `userId` (@Userid) |
| POST | `/follow/{targetUserId}` | 关注用户 | `targetUserId` (path), `userId` (@Userid) |
| DELETE | `/follow/{targetUserId}` | 取消关注用户 | `targetUserId` (path), `userId` (@Userid) |
| DELETE | `/friend/{friendId}` | 删除好友关系 | `friendId` (path), `userId` (@Userid) |
| GET | `/friends` | 获取好友列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/following` | 获取关注列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/followers` | 获取粉丝列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/mutual-follow` | 获取互相关注列表 | `userId` (@Userid), `page`, `size` (query) |
| GET | `/recommendations/friends` | 获取好友推荐 | `userId` (@Userid), `limit` (query) |
| GET | `/recommendations/follow` | 获取关注推荐 | `userId` (@Userid), `limit` (query) |
| GET | `/statistics` | 获取社交统计信息 | `userId` (@Userid) |

### 22. ContactGroupController (联系人分组管理)
**基路径**: `/api/contact-groups`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/` | 创建联系人分组 | `ContactGroupCreateVo` (body), `userId` (@Userid) |
| GET | `/` | 获取用户联系人分组列表 | `userId` (@Userid) |
| PUT | `/{groupId}/name` | 更新分组名称 | `groupId` (path), `ContactGroupUpdateVo` (body), `userId` (@Userid) |
| PUT | `/{groupId}/order` | 更新分组排序 | `groupId` (path), `ContactGroupUpdateVo` (body), `userId` (@Userid) |
| DELETE | `/{groupId}` | 删除分组 | `groupId` (path), `userId` (@Userid) |
| POST | `/{groupId}/contacts/{contactId}` | 添加联系人到分组 | `groupId` (path), `contactId` (path), `userId` (@Userid) |
| DELETE | `/contacts/{contactId}` | 从分组移除联系人 | `contactId` (path), `userId` (@Userid) |
| GET | `/{groupId}/contacts` | 获取分组联系人列表 | `groupId` (path), `userId` (@Userid) |
| GET | `/default` | 获取默认分组 | `userId` (@Userid) |

### 23. RateLimitController (限流管理)
**基路径**: `/api/rate-limit`

| 方法 | 路径 | 描述 | 参数 |
|------|------|------|------|
| POST | `/config` | 配置限流规则 | `RateLimitConfigVo` (body) |
| GET | `/config` | 获取限流配置 | `path` (query) |
| DELETE | `/config` | 删除限流配置 | `path` (query) |
| GET | `/config/all` | 获取所有限流配置 | 无 |
| GET | `/statistics` | 获取限流统计 | 无 |
| GET | `/events` | 获取限流事件 | `page`, `size` (query) |
| GET | `/alerts` | 获取限流告警 | `page`, `size` (query) |
| DELETE | `/statistics` | 清空限流统计 | 无 |
| POST | `/unlock` | 解锁限流 | `path` (query) |

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
- `GroupApplyVo`: 群组申请信息
- `GroupApplication`: 群组申请实体类
- `GroupMember`: 群组成员信息

### 3. 联系人相关
- `ContactDto`: 联系人详细信息
- `ContactApplyVo`: 联系人申请信息
- `ContactRequestDto`: 联系人请求信息

### 4. 聊天相关
- `ChatCreateVo`: 聊天创建信息
- `ChatMessageVo`: 聊天消息内容
- `ChatMessagesVo`: 聊天消息分页参数

### 5. 文章相关
- `ArticleCreateVo`: 文章创建信息
- `ArticleUpdateVo`: 文章更新信息
- `ArticleSearchAdvancedVo`: 高级搜索参数

### 6. 认证相关
- `RegistrationVo`: 用户注册信息
- `LoginVo`: 用户登录信息
- `PasswordChangeVo`: 密码修改信息
- `PasswordResetVo`: 密码重置信息
- `ForgotPasswordVo`: 忘记密码信息

---

## 优化建议

### 1. 接口整合
- 将 `ChatController` 和 `UnifiedMessageController` 合并为一个统一的聊天控制器
- 统一 `ContactController` 和 `ContactGroupController` 的分组管理功能
- 整合 `UserFollowController` 和 `SocialRelationshipController` 的关注功能

### 2. 路径标准化
- 所有路径使用复数形式：`/api/users`, `/api/groups`, `/api/articles`
- 统一分页参数：`page`, `size` 替代 `page`, `pageSize`
- 统一搜索参数：`q` 用于关键词搜索

### 3. 参数优化
- 减少重复的参数定义
- 统一使用 `@Userid` 注解获取用户ID
- 标准化错误响应格式

### 4. 安全增强
- 添加接口权限验证
- 统一参数校验
- 加强敏感操作的审计日志

### 5. 性能优化
- 添加缓存机制
- 优化数据库查询
- 实现接口限流保护

--
-

## API迁移说明 (2025-11-06更新)

### 重要变更

#### 1. 聊天API统一 ✅
**UnifiedMessageController** (`/api/messages/*`) 已标记为废弃，所有功能已迁移到 **ChatController** (`/api/chats/*`)

**迁移对照表**:
| 旧端点 | 新端点 | 说明 |
|--------|--------|------|
| POST /api/messages/send | POST /api/chats/{chatId}/messages | 发送消息 |
| GET /api/messages/chats | GET /api/chats | 获取聊天列表 |
| GET /api/messages/unread/stats | GET /api/chats/unread/stats | 未读统计 |
| POST /api/messages/{id}/read | POST /api/chats/{chatId}/read | 标记已读 |
| POST /api/messages/{id}/recall | DELETE /api/chats/messages/{id} | 撤回消息 |

**新增功能**:
- `POST /api/chats/read/batch` - 批量标记已读
- `GET /api/chats/{chatId}/unread` - 获取单个聊天未读数
- `GET /api/chats/groups/{groupId}/unread` - 获取群组未读数
- `GET /api/chats/online-users` - 获取在线用户列表

#### 2. 用户信息API规范化 ✅
**ArticleCenterController** 中的用户信息端点已删除，统一使用 **StandardUserController**

**迁移对照表**:
| 旧端点 | 新端点 | 说明 |
|--------|--------|------|
| GET /api/articles/userinform | GET /api/users/{userId} | 获取用户信息 |
| GET /api/articles/userinform-by-username | GET /api/users/by-username/{username} | 通过用户名获取 |
| - | GET /api/users/{userId}/stats | 获取用户统计 |

**新增功能**:
- `GET /api/users/by-username/{username}` - 通过用户名获取完整用户信息（含统计数据）

### 迁移时间表

- **2025-11-06**: API重构完成，旧端点标记为废弃
- **2025-11-20**: 前端完成迁移（预计）
- **2025-12-01**: 移除废弃端点（计划）

### 详细迁移指南

请参考以下文档了解详细的迁移步骤：
- [API迁移指南](../API_MIGRATION_GUIDE.md) - 完整的API迁移说明
- [前端迁移任务](../FRONTEND_MIGRATION_TASKS.md) - 前端组件迁移清单
- [API重构总结](../API_REFACTORING_SUMMARY.md) - 重构详细说明

### 兼容性说明

- 废弃的端点仍然可用，但会在响应头中包含 `X-Deprecated-API: true`
- 建议尽快迁移到新端点，旧端点将在下一个主版本中移除
- 新端点提供更好的性能和更完整的功能

### 技术支持

如有迁移问题，请：
1. 查看迁移文档
2. 检查API响应日志
3. 联系开发团队

---

**最后更新**: 2025-11-06  
**文档版本**: 2.0
