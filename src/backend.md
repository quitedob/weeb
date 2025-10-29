# 后端API接口文档

## 接口冲突分析

### 1. 路径冲突
- **ContactController** (`/api/contacts`) 和 **ContactGroupController** (`/api/contact-groups`)：功能区分清晰，前者处理好友关系，后者处理联系人分组
- **StandardGroupController** (`/api/groups`) 和 **ContactGroupController** (`/api/contact-groups`)：功能完全不同，无冲突
- **ChatController** (`/api/chats`) 和 **UnifiedMessageController** (`/api/messages`)：功能重叠，需要整合

### 2. 功能重叠
- **ChatController** 和 **UnifiedMessageController** 都处理消息相关功能
- **ContactController** 和 **ContactGroupController** 都涉及联系人管理
- **StandardGroupController** 和 **SocialRelationshipController** 部分功能重叠

### 3. 建议解决方案
- 整合 ChatController 和 UnifiedMessageController 为统一的消息控制器
- 明确 ContactController 和 ContactGroupController 的职责分工
- 标准化所有控制器的路径命名和参数格式

---

## 控制器接口列表

### 1. StandardGroupController (群组管理)
**基路径**: `/api/groups`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/` | 创建群组 | `GroupCreateVo` (body), `userId` (@Userid) | 2 |
| GET | `/{groupId}` | 获取群组详情 | `groupId` (path), `userId` (@Userid) | 2 |
| PUT | `/{groupId}` | 更新群组信息 | `groupId` (path), `GroupCreateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{groupId}` | 删除群组 | `groupId` (path), `userId` (@Userid) | 2 |
| GET | `/{groupId}/members` | 获取群组成员列表 | `groupId` (path) | 1 |
| POST | `/{groupId}/members` | 邀请用户加入群组 | `groupId` (path), `GroupInviteVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{groupId}/members/me` | 退出群组 | `groupId` (path), `userId` (@Userid) | 2 |
| DELETE | `/{groupId}/members/{userId}` | 移除群组成员 | `groupId` (path), `userId` (path), `currentUserId` (@Userid) | 3 |
| POST | `/{groupId}/applications` | 申请加入群组 | `groupId` (path), `Map<String, String>` (body), `userId` (@Userid) | 3 |
| GET | `/{groupId}/applications` | 获取群组申请列表 | `groupId` (path) | 1 |
| PUT | `/{groupId}/applications/{applicationId}` | 处理群组申请 | `groupId` (path), `applicationId` (path), `Map<String, String>` (body), `userId` (@Userid) | 4 |
| PUT | `/{groupId}/members/{userId}/role` | 设置群组管理员 | `groupId` (path), `userId` (path), `Map<String, String>` (body), `currentUserId` (@Userid) | 4 |
| GET | `/search` | 搜索群组 | `q` (query), `limit` (query, default=10) | 2 |
| GET | `/my-groups` | 获取用户加入的群组列表 | `userId` (@Userid) | 1 |
| GET | `/my-created` | 获取用户创建的群组列表 | `userId` (@Userid) | 1 |

### 2. ContactController (联系人管理)
**基路径**: `/api/contacts`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/apply` | 发送好友申请（通过用户ID） | `ContactApplyVo` (body), `userId` (@Userid) | 2 |
| POST | `/request` | 发送好友申请（兼容前端 /request 路径） | `Map<String, Object>` (body), `userId` (@Userid) | 2 |
| POST | `/request/by-username` | 通过用户名发送好友申请 | `Map<String, String>` (body), `userId` (@Userid) | 2 |
| POST | `/accept/{contactId}` | 同意好友申请 | `contactId` (path), `userId` (@Userid) | 2 |
| POST | `/request/{requestId}/accept` | 同意好友申请（兼容路径） | `requestId` (path), `userId` (@Userid) | 2 |
| POST | `/decline/{contactId}` | 拒绝好友申请 | `contactId` (path), `userId` (@Userid) | 2 |
| POST | `/request/{requestId}/reject` | 拒绝好友申请（兼容路径） | `requestId` (path), `userId` (@Userid) | 2 |
| POST | `/block/{contactId}` | 拉黑联系人 | `contactId` (path), `userId` (@Userid) | 2 |
| GET | `/` | 获取联系人列表 | `userId` (@Userid), `status` (query, ContactStatus) | 2 |
| GET | `/requests` | 获取待处理的好友申请列表 | `userId` (@Userid) | 1 |
| POST | `/groups` | 创建联系人分组 | `Map<String, Object>` (body), `userId` (@Userid) | 2 |
| GET | `/groups` | 获取用户的所有联系人分组 | `userId` (@Userid) | 1 |
| PUT | `/groups/{groupId}/name` | 更新分组名称 | `groupId` (path), `Map<String, String>` (body), `userId` (@Userid) | 3 |
| PUT | `/groups/{groupId}/order` | 更新分组排序 | `groupId` (path), `Map<String, Integer>` (body), `userId` (@Userid) | 3 |
| DELETE | `/groups/{groupId}` | 删除分组 | `groupId` (path), `userId` (@Userid) | 2 |
| POST | `/groups/{groupId}/contacts/{contactId}` | 将联系人添加到分组 | `groupId` (path), `contactId` (path), `userId` (@Userid) | 3 |
| DELETE | `/groups/contacts/{contactId}` | 从分组中移除联系人 | `contactId` (path), `userId` (@Userid) | 2 |
| GET | `/groups/{groupId}/contacts` | 获取指定分组的联系人列表 | `groupId` (path), `userId` (@Userid) | 2 |

### 3. ChatController (聊天管理)
**基路径**: `/api/chats`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/` | 获取用户的聊天列表 | `userId` (@Userid) | 1 |
| POST | `/` | 创建新的聊天会话 | `userId` (@Userid), `ChatCreateVo` (body) | 2 |
| GET | `/{chatId}/messages` | 获取聊天消息历史记录 | `chatId` (path), `ChatMessagesVo` (@ModelAttribute) | 2 |
| POST | `/{chatId}/messages` | 发送聊天消息 | `chatId` (path), `userId` (@Userid), `ChatMessageVo` (body) | 3 |
| POST | `/{chatId}/read` | 标记消息为已读 | `chatId` (path), `userId` (@Userid) | 2 |
| DELETE | `/{chatId}` | 删除聊天会话 | `chatId` (path), `userId` (@Userid) | 2 |
| POST | `/messages/{messageId}/react` | 对消息添加反应 | `messageId` (path), `userId` (@Userid), `reactionType` (query) | 3 |
| DELETE | `/messages/{messageId}` | 撤回消息 | `messageId` (path), `userId` (@Userid) | 2 |

### 4. WebSocketMonitorController (WebSocket监控)
**基路径**: `/api/websocket/monitor`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/online-count` | 获取在线用户数 | 无 | 0 |
| GET | `/online-users` | 获取在线用户列表 | 无 | 0 |
| GET | `/user/{userId}/online` | 检查用户是否在线 | `userId` (path) | 1 |
| GET | `/user/{userId}/info` | 获取用户连接信息 | `userId` (path) | 1 |
| GET | `/statistics` | 获取连接统计信息 | 无 | 0 |
| POST | `/clean-expired` | 手动清理过期连接 | 无 | 0 |
| GET | `/user/{userId}/sessions` | 获取用户活跃会话列表 | `userId` (path) | 1 |

### 5. RateLimitController (限流管理)
**基路径**: `/api/rate-limit`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/config` | 配置限流规则 | `RateLimitConfigVo` (body) | 1 |
| GET | `/config` | 获取限流配置 | `path` (query) | 1 |
| DELETE | `/config` | 删除限流配置 | `path` (query) | 1 |
| GET | `/config/all` | 获取所有限流配置 | 无 | 0 |
| GET | `/statistics` | 获取限流统计 | 无 | 0 |
| GET | `/events` | 获取限流事件 | `page` (query, default=0), `size` (query, default=20) | 2 |
| GET | `/alerts` | 获取限流告警 | `page` (query, default=0), `size` (query, default=20) | 2 |
| DELETE | `/statistics` | 清空限流统计 | 无 | 0 |
| POST | `/unlock` | 解锁限流 | `path` (query) | 1 |

### 6. SocialRelationshipController (社交关系管理)
**基路径**: `/api/social`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/relationship/{targetUserId}` | 获取两个用户间的关系 | `targetUserId` (path), `userId` (@Userid) | 2 |
| POST | `/follow/{targetUserId}` | 关注用户 | `targetUserId` (path), `userId` (@Userid) | 2 |
| DELETE | `/follow/{targetUserId}` | 取消关注用户 | `targetUserId` (path), `userId` (@Userid) | 2 |
| DELETE | `/friend/{friendId}` | 删除好友关系 | `friendId` (path), `userId` (@Userid) | 2 |
| GET | `/friends` | 获取好友列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/following` | 获取关注列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/followers` | 获取粉丝列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/mutual-follow` | 获取互相关注列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/recommendations/friends` | 获取好友推荐 | `userId` (@Userid), `limit` (query, default=10) | 2 |
| GET | `/recommendations/follow` | 获取关注推荐 | `userId` (@Userid), `limit` (query, default=10) | 2 |
| GET | `/statistics` | 获取社交统计信息 | `userId` (@Userid) | 1 |

### 7. ContactGroupController (联系人分组管理)
**基路径**: `/api/contact-groups`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/` | 创建联系人分组 | `ContactGroupCreateVo` (body), `userId` (@Userid) | 2 |
| GET | `/` | 获取用户联系人分组列表 | `userId` (@Userid) | 1 |
| PUT | `/{groupId}/name` | 更新分组名称 | `groupId` (path), `ContactGroupUpdateVo` (body), `userId` (@Userid) | 3 |
| PUT | `/{groupId}/order` | 更新分组排序 | `groupId` (path), `ContactGroupUpdateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{groupId}` | 删除分组 | `groupId` (path), `userId` (@Userid) | 2 |
| POST | `/{groupId}/contacts/{contactId}` | 添加联系人到分组 | `groupId` (path), `contactId` (path), `userId` (@Userid) | 3 |
| DELETE | `/contacts/{contactId}` | 从分组移除联系人 | `contactId` (path), `userId` (@Userid) | 2 |
| GET | `/{groupId}/contacts` | 获取分组联系人列表 | `groupId` (path), `userId` (@Userid) | 2 |
| GET | `/default` | 获取默认分组 | `userId` (@Userid) | 1 |

### 8. UnifiedMessageController (统一消息管理)
**基路径**: `/api/messages`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/send` | 发送消息 | `MessageSendVo` (body), `userId` (@Userid) | 2 |
| GET | `/history` | 获取消息历史 | `chatId` (query), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/unread` | 获取未读消息 | `userId` (@Userid) | 1 |
| PUT | `/{messageId}/read` | 标记消息为已读 | `messageId` (path), `userId` (@Userid) | 2 |
| DELETE | `/{messageId}` | 删除消息 | `messageId` (path), `userId` (@Userid) | 2 |
| POST | `/{messageId}/reaction` | 添加消息反应 | `messageId` (path), `reaction` (query), `userId` (@Userid) | 3 |
| DELETE | `/{messageId}/reaction` | 移除消息反应 | `messageId` (path), `reaction` (query), `userId` (@Userid) | 3 |
| PUT | `/{messageId}` | 编辑消息 | `messageId` (path), `MessageEditVo` (body), `userId` (@Userid) | 3 |
| GET | `/search` | 搜索消息 | `keyword` (query), `chatId` (query), `userId` (@Userid) | 3 |
| POST | `/forward` | 转发消息 | `MessageForwardVo` (body), `userId` (@Userid) | 2 |
| GET | `/thread/{messageId}` | 获取消息线程 | `messageId` (path) | 1 |
| POST | `/thread/{messageId}/reply` | 回复消息线程 | `messageId` (path), `MessageSendVo` (body), `userId` (@Userid) | 3 |

### 9. ArticleCenterController (文章中心管理)
**基路径**: `/api/articles`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/{id}` | 根据ID获取文章信息 | `id` (path) | 1 |
| GET | `/userinform` | 获取用户各类统计信息 | `userId` (query) | 1 |
| POST | `/{id}/like` | 文章点赞 | `id` (path) | 1 |
| GET | `/` | 获取文章列表 | `page` (query, default=0), `size` (query, default=10) | 2 |
| POST | `/` | 创建文章 | `ArticleCreateVo` (body), `userId` (@Userid) | 2 |
| PUT | `/{id}` | 更新文章 | `id` (path), `ArticleUpdateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{id}` | 删除文章 | `id` (path), `userId` (@Userid) | 2 |
| GET | `/search` | 搜索文章 | `keyword` (query), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/advanced-search` | 高级搜索文章 | `ArticleSearchAdvancedVo` (query params) | 多个 |
| GET | `/{id}/comments` | 获取文章评论 | `id` (path), `page` (query, default=0), `size` (query, default=10) | 3 |
| POST | `/{id}/comments` | 添加文章评论 | `id` (path), `CommentCreateVo` (body), `userId` (@Userid) | 3 |
| GET | `/categories` | 获取文章分类 | 无 | 0 |
| GET | `/tags` | 获取文章标签 | 无 | 0 |
| GET | `/hot` | 获取热门文章 | `limit` (query, default=10) | 1 |
| GET | `/recommend` | 获取推荐文章 | `userId` (@Userid), `limit` (query, default=10) | 2 |
| POST | `/{id}/collect` | 收藏文章 | `id` (path), `userId` (@Userid) | 2 |
| DELETE | `/{id}/collect` | 取消收藏文章 | `id` (path), `userId` (@Userid) | 2 |
| GET | `/user/{userId}/collected` | 获取用户收藏的文章 | `userId` (path), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/user/{userId}/published` | 获取用户发布的文章 | `userId` (path), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/statistics` | 获取文章统计信息 | 无 | 0 |

### 10. StandardUserController (标准化用户管理)
**基路径**: `/api/users`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/me` | 获取当前用户信息 | 无 (SecurityUtils获取userId) | 0 |
| GET | `/me/profile` | 获取当前用户完整信息 | 无 (SecurityUtils获取userId) | 0 |
| GET | `/me/info` | 获取当前用户基本信息 | 无 (SecurityUtils获取userId) | 0 |
| PUT | `/me` | 更新当前用户信息 | `UpdateUserVo` (body) | 1 |
| GET | `/{userId}` | 获取指定用户信息 | `userId` (path) | 1 |
| GET | `/` | 获取用户列表（分页） | `page` (query, default=1), `pageSize` (query, default=20), `keyword` (query) | 3 |
| GET | `/search` | 搜索用户 | `q` (query), `limit` (query, default=10) | 2 |
| GET | `/me/groups` | 获取当前用户的群组列表 | 无 (SecurityUtils获取userId) | 0 |
| POST | `/{userId}/ban` | 封禁用户 | `userId` (path) | 1 |
| POST | `/{userId}/unban` | 解封用户 | `userId` (path) | 1 |
| POST | `/{userId}/reset-password` | 重置用户密码 | `userId` (path), `AdminResetPasswordRequestVo` (body) | 2 |
| POST | `/{userId}/follow` | 关注用户 | `userId` (path) | 1 |
| DELETE | `/{userId}/follow` | 取消关注用户 | `userId` (path) | 1 |
| GET | `/{userId}/following` | 获取用户的关注列表 | `userId` (path), `page` (query, default=1), `pageSize` (query, default=20) | 3 |
| GET | `/{userId}/followers` | 获取用户的粉丝列表 | `userId` (path), `page` (query, default=1), `pageSize` (query, default=20) | 3 |
| GET | `/{userId}/follow/status` | 检查当前用户是否关注了指定用户 | `userId` (path) | 1 |
| GET | `/{userId}/stats` | 获取用户统计信息 | `userId` (path) | 1 |
| GET | `/{userId}/activities` | 获取用户最近活动 | `userId` (path), `limit` (query, default=10) | 2 |
| PUT | `/profile` | 更新个人资料 | `UpdateUserVo` (body) | 1 |
| POST | `/avatar` | 上传用户头像 | `avatar` (multipart file) | 1 |

### 11. MessageThreadController (消息线程管理)
**基路径**: `/api/message-threads`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/` | 创建消息线程 | `MessageThreadCreateVo` (body), `userId` (@Userid) | 2 |
| GET | `/{threadId}` | 获取消息线程详情 | `threadId` (path) | 1 |
| GET | `/{threadId}/messages` | 获取线程消息列表 | `threadId` (path), `page` (query, default=0), `size` (query, default=20) | 3 |
| POST | `/{threadId}/messages` | 在线程中发送消息 | `threadId` (path), `MessageSendVo` (body), `userId` (@Userid) | 3 |
| PUT | `/{threadId}` | 更新线程信息 | `threadId` (path), `MessageThreadUpdateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{threadId}` | 删除消息线程 | `threadId` (path), `userId` (@Userid) | 2 |
| POST | `/{threadId}/participants` | 添加线程参与者 | `threadId` (path), `participants` (body), `userId` (@Userid) | 3 |
| DELETE | `/{threadId}/participants/{userId}` | 移除线程参与者 | `threadId` (path), `userId` (path), `currentUserId` (@Userid) | 3 |
| GET | `/{threadId}/participants` | 获取线程参与者列表 | `threadId` (path) | 1 |
| PUT | `/{threadId}/read` | 标记线程为已读 | `threadId` (path), `userId` (@Userid) | 2 |
| GET | `/user/threads` | 获取用户参与的线程列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |

### 12. AIController (AI功能管理)
**基路径**: `/api/ai`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/chat` | AI聊天对话 | `AIChatRequest` (body), `userId` (@Userid) | 2 |
| POST | `/generate/image` | AI图像生成 | `ImageGenerationRequest` (body), `userId` (@Userid) | 2 |
| POST | `/analyze/text` | 文本分析 | `TextAnalysisRequest` (body), `userId` (@Userid) | 2 |
| POST | `/translate` | 文本翻译 | `TranslationRequest` (body), `userId` (@Userid) | 2 |
| GET | `/history` | 获取AI交互历史 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/models` | 获取可用AI模型列表 | 无 | 0 |
| POST | `/custom/prompt` | 自定义AI提示词 | `CustomPromptRequest` (body), `userId` (@Userid) | 2 |
| GET | `/usage/stats` | 获取AI使用统计 | `userId` (@Userid), `period` (query, default="month") | 2 |
| POST | `/feedback` | 提交AI反馈 | `AIFeedbackRequest` (body), `userId` (@Userid) | 2 |
| DELETE | `/history/{sessionId}` | 删除AI会话历史 | `sessionId` (path), `userId` (@Userid) | 2 |

### 13. ContentReportController (内容举报管理)
**基路径**: `/api/reports`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/` | 提交内容举报 | `ContentReportCreateVo` (body), `userId` (@Userid) | 2 |
| GET | `/{reportId}` | 获取举报详情 | `reportId` (path) | 1 |
| GET | `/` | 获取举报列表 | `status` (query), `type` (query), `page` (query, default=0), `size` (query, default=20) | 4 |
| PUT | `/{reportId}/status` | 更新举报状态 | `reportId` (path), `ContentReportUpdateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{reportId}` | 删除举报 | `reportId` (path), `userId` (@Userid) | 2 |
| GET | `/statistics` | 获取举报统计信息 | `startDate` (query), `endDate` (query) | 2 |
| POST | `/{reportId}/evidence` | 上传举报证据 | `reportId` (path), `files` (multipart), `userId` (@Userid) | 3 |
| GET | `/{reportId}/evidence` | 获取举报证据列表 | `reportId` (path) | 1 |
| POST | `/batch/process` | 批量处理举报 | `BatchProcessRequest` (body), `userId` (@Userid) | 2 |
| GET | `/types` | 获取举报类型列表 | 无 | 0 |

### 14. UserLevelIntegrationController (用户等级积分管理)
**基路径**: `/api/user-levels`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/user/{userId}` | 获取用户等级信息 | `userId` (path) | 1 |
| GET | `/levels` | 获取所有等级定义 | 无 | 0 |
| POST | `/user/{userId}/points/add` | 增加用户积分 | `userId` (path), `PointsOperationVo` (body), `operatorId` (@Userid) | 3 |
| POST | `/user/{userId}/points/deduct` | 扣除用户积分 | `userId` (path), `PointsOperationVo` (body), `operatorId` (@Userid) | 3 |
| GET | `/user/{userId}/points/history` | 获取用户积分历史 | `userId` (path), `page` (query, default=0), `size` (query, default=20) | 3 |
| POST | `/level/up` | 用户升级 | `userId` (path), `operatorId` (@Userid) | 2 |
| GET | `/statistics` | 获取等级统计信息 | 无 | 0 |
| GET | `/leaderboard` | 获取积分排行榜 | `period` (query, default="all"), `limit` (query, default=100) | 2 |
| POST | `/task/complete` | 完成任务获得积分 | `TaskCompletionVo` (body), `userId` (@Userid) | 2 |
| GET | `/tasks` | 获取可用任务列表 | `userId` (@Userid) | 1 |

### 15. UserLevelHistoryController (用户等级历史管理)
**基路径**: `/api/user-level-history`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/user/{userId}` | 获取用户等级历史 | `userId` (path), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/user/{userId}/current` | 获取用户当前等级状态 | `userId` (path) | 1 |
| GET | `/user/{userId}/upcoming` | 获取用户即将达到的等级 | `userId` (path) | 1 |
| GET | `/statistics` | 获取等级历史统计 | `startDate` (query), `endDate` (query) | 2 |
| GET | `/level/{levelId}/users` | 获取指定等级的用户列表 | `levelId` (path), `page` (query, default=0), `size` (query, default=20) | 3 |
| POST | `/migrate` | 迁移等级历史数据 | `MigrationRequest` (body), `operatorId` (@Userid) | 2 |
| GET | `/export` | 导出等级历史数据 | `startDate` (query), `endDate` (query), `format` (query, default="json") | 3 |
| POST | `/bulk/update` | 批量更新等级历史 | `BulkUpdateRequest` (body), `operatorId` (@Userid) | 2 |

### 16. StandardAuthController (标准化认证管理)
**基路径**: `/api/auth`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/register` | 用户注册 | `RegistrationVo` (body) | 1 |
| POST | `/login` | 用户登录 | `LoginVo` (body) | 1 |
| POST | `/logout` | 用户登出 | `Authorization` (header) | 1 |
| POST | `/refresh` | 刷新令牌 | `Authorization` (header) | 1 |
| POST | `/forgot-password` | 忘记密码 | `ForgotPasswordVo` (body) | 1 |
| POST | `/reset-password` | 重置密码 | `PasswordResetVo` (body) | 1 |
| POST | `/change-password` | 修改密码 | `PasswordChangeVo` (body), `userId` (@Userid) | 2 |
| GET | `/verify-token` | 验证令牌有效性 | `Authorization` (header) | 1 |
| POST | `/validate-email` | 验证邮箱 | `email` (query) | 1 |
| POST | `/resend-verification` | 重新发送验证邮件 | `email` (query) | 1 |
| GET | `/profile` | 获取用户资料 | `userId` (@Userid) | 1 |
| PUT | `/profile` | 更新用户资料 | `UserProfileUpdateVo` (body), `userId` (@Userid) | 2 |
| GET | `/permissions` | 获取用户权限 | `userId` (@Userid) | 1 |
| POST | `/deactivate` | 停用账户 | `userId` (@Userid) | 1 |
| POST | `/reactivate` | 重新激活账户 | `userId` (@Userid) | 1 |

### 17. NotificationController (通知管理)
**基路径**: `/api/notifications`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/` | 获取用户通知列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/unread` | 获取未读通知 | `userId` (@Userid) | 1 |
| PUT | `/{notificationId}/read` | 标记通知为已读 | `notificationId` (path), `userId` (@Userid) | 2 |
| PUT | `/read-all` | 标记所有通知为已读 | `userId` (@Userid) | 1 |
| DELETE | `/{notificationId}` | 删除通知 | `notificationId` (path), `userId` (@Userid) | 2 |
| DELETE | `/read` | 删除所有已读通知 | `userId` (@Userid) | 1 |
| GET | `/count` | 获取通知数量统计 | `userId` (@Userid) | 1 |
| POST | `/test` | 发送测试通知 | `TestNotificationVo` (body) | 1 |
| GET | `/types` | 获取通知类型列表 | 无 | 0 |
| PUT | `/settings` | 更新通知设置 | `NotificationSettingsVo` (body), `userId` (@Userid) | 2 |
| GET | `/settings` | 获取通知设置 | `userId` (@Userid) | 1 |

### 18. MigrationController (数据迁移管理)
**基路径**: `/api/migration`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/start` | 开始数据迁移 | `MigrationRequest` (body), `operatorId` (@Userid) | 2 |
| GET | `/status/{migrationId}` | 获取迁移状态 | `migrationId` (path) | 1 |
| GET | `/history` | 获取迁移历史 | `page` (query, default=0), `size` (query, default=20) | 2 |
| POST | `/rollback/{migrationId}` | 回滚迁移 | `migrationId` (path), `operatorId` (@Userid) | 2 |
| GET | `/available` | 获取可用的迁移脚本 | 无 | 0 |
| POST | `/validate/{migrationId}` | 验证迁移结果 | `migrationId` (path) | 1 |
| DELETE | `/cleanup/{migrationId}` | 清理迁移临时数据 | `migrationId` (path), `operatorId` (@Userid) | 2 |
| GET | `/statistics` | 获取迁移统计信息 | 无 | 0 |

### 19. SearchController (全局搜索管理)
**基路径**: `/api/search`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/messages` | 搜索消息内容 | `q` (query), `page` (query, default=0), `size` (query, default=10), `startDate` (query), `endDate` (query), `messageTypes` (query), `userIds` (query), `groupIds` (query), `sortBy` (query, default="relevance") | 9 |
| GET | `/users` | 搜索用户 | `q` (query), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/groups` | 搜索群组 | `q` (query), `page` (query, default=0), `size` (query, default=10) | 3 |
| GET | `/articles` | 搜索文章 | `q` (query), `page` (query, default=0), `size` (query, default=10), `category` (query), `tags` (query), `author` (query) | 6 |
| GET | `/all` | 全局搜索 | `q` (query), `type` (query), `page` (query, default=0), `size` (query, default=10) | 4 |
| GET | `/suggestions` | 获取搜索建议 | `q` (query), `limit` (query, default=10) | 2 |
| GET | `/hot-keywords` | 获取热门搜索关键词 | `limit` (query, default=20) | 1 |
| POST | `/index/rebuild` | 重建搜索索引 | `type` (query), `operatorId` (@Userid) | 2 |
| GET | `/index/status` | 获取索引状态 | 无 | 0 |
| DELETE | `/index/clear` | 清空搜索索引 | `type` (query), `operatorId` (@Userid) | 2 |

### 20. ArticleCommentController (文章评论管理)
**基路径**: `/api/article-comments`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| GET | `/article/{articleId}` | 获取文章评论列表 | `articleId` (path), `page` (query, default=0), `size` (query, default=10) | 3 |
| POST | `/article/{articleId}` | 添加文章评论 | `articleId` (path), `ArticleCommentCreateVo` (body), `userId` (@Userid) | 3 |
| PUT | `/{commentId}` | 更新评论 | `commentId` (path), `ArticleCommentUpdateVo` (body), `userId` (@Userid) | 3 |
| DELETE | `/{commentId}` | 删除评论 | `commentId` (path), `userId` (@Userid) | 2 |
| POST | `/{commentId}/like` | 点赞评论 | `commentId` (path), `userId` (@Userid) | 2 |
| DELETE | `/{commentId}/like` | 取消点赞评论 | `commentId` (path), `userId` (@Userid) | 2 |
| GET | `/{commentId}/replies` | 获取评论回复 | `commentId` (path), `page` (query, default=0), `size` (query, default=10) | 3 |
| POST | `/{commentId}/reply` | 回复评论 | `commentId` (path), `ArticleCommentCreateVo` (body), `userId` (@Userid) | 3 |

### 21. UserFollowController (用户关注管理)
**基路径**: `/api/user-follows`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/{targetUserId}` | 关注用户 | `targetUserId` (path), `userId` (@Userid) | 2 |
| DELETE | `/{targetUserId}` | 取消关注用户 | `targetUserId` (path), `userId` (@Userid) | 2 |
| GET | `/status/{targetUserId}` | 获取关注状态 | `targetUserId` (path), `userId` (@Userid) | 2 |
| GET | `/followers` | 获取粉丝列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/following` | 获取关注列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/mutual` | 获取互相关注列表 | `userId` (@Userid), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/count/{userId}` | 获取用户关注统计 | `userId` (path) | 1 |
| GET | `/recommendations` | 获取关注推荐 | `userId` (@Userid), `limit` (query, default=10) | 2 |

### 22. WebSocketMessageController (WebSocket消息管理)
**基路径**: `/api/websocket/messages`

| 方法 | 路径 | 描述 | 参数 | 参数数量 |
|------|------|------|------|----------|
| POST | `/send` | 发送WebSocket消息 | `WebSocketMessageSendVo` (body), `userId` (@Userid) | 2 |
| POST | `/broadcast` | 广播消息 | `WebSocketMessageSendVo` (body), `userId` (@Userid) | 2 |
| POST | `/group/{groupId}` | 发送群组消息 | `groupId` (path), `WebSocketMessageSendVo` (body), `userId` (@Userid) | 3 |
| GET | `/history/{sessionId}` | 获取会话消息历史 | `sessionId` (path), `page` (query, default=0), `size` (query, default=20) | 3 |
| GET | `/online-users` | 获取在线用户列表 | 无 | 0 |
| POST | `/kick/{userId}` | 踢出用户 | `userId` (path), `operatorId` (@Userid) | 2 |
| GET | `/statistics` | 获取WebSocket统计信息 | 无 | 0 |
| POST | `/maintenance` | 维护模式切换 | `maintenance` (query, boolean), `operatorId` (@Userid) | 2 |
| GET | `/connections` | 获取连接列表 | `page` (query, default=0), `size` (query, default=20) | 2 |
| DELETE | `/connection/{connectionId}` | 断开连接 | `connectionId` (path), `operatorId` (@Userid) | 2 |

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
