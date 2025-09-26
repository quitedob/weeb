# 聊天API迁移指南

## 概述

为了统一聊天功能的API接口，我们已经将原有的 `MessageController` 和 `ChatListController` 的功能整合到了新的 `ChatController` 中。

## API变更对比

### 旧API (已废弃)

#### ChatListController
- `GET /api/v1/chat-list/list/private` - 获取私聊列表
- `GET /api/v1/chat-list/group` - 获取群聊记录
- `POST /api/v1/chat-list/create` - 创建私聊记录
- `POST /api/v1/chat-list/read` - 标记为已读
- `DELETE /api/v1/chat-list/delete` - 删除聊天记录

#### MessageController (部分功能)
- `POST /api/message/send` - 发送消息
- `POST /api/message/record` - 获取聊天记录
- `POST /api/message/recall` - 撤回消息
- `POST /api/message/react` - 消息反应

### 新API (推荐使用)

#### ChatController - 统一聊天API
- `GET /api/v1/chats` - 获取聊天列表
- `POST /api/v1/chats` - 创建新聊天会话
- `GET /api/v1/chats/{chatId}/messages` - 获取聊天消息
- `POST /api/v1/chats/{chatId}/messages` - 发送消息
- `POST /api/v1/chats/{chatId}/read` - 标记为已读
- `DELETE /api/v1/chats/{chatId}` - 删除聊天会话
- `POST /api/v1/chats/messages/{messageId}/react` - 消息反应

## 迁移步骤

### 1. 前端代码迁移

#### 旧代码示例：
```javascript
// 旧方式 - 使用多个不同的API
import { getPrivateChatList, createPrivateChat } from '@/api/modules/chat';
import { sendMessage } from '@/api/modules/message';

// 获取聊天列表
const chatList = await getPrivateChatList(userId);

// 创建聊天
const chat = await createPrivateChat(userId, targetId);

// 发送消息
const message = await sendMessage(messageData);
```

#### 新代码示例：
```javascript
// 新方式 - 使用统一的ChatController API
import { getChatList, createChat, sendMessage } from '@/api/modules/chat';

// 获取聊天列表
const chatList = await getChatList(); // 自动从token获取userId

// 创建聊天
const chat = await createChat(targetId);

// 发送消息
const message = await sendMessage(chatId, messageData);
```

### 2. 请求参数变更

#### 用户认证方式
- **旧方式**: 通过查询参数传递 `userId`
- **新方式**: 通过 `@Userid` 注解自动从JWT token获取用户ID

#### 消息发送
- **旧方式**: 直接传递 `Message` 实体
- **新方式**: 使用 `ChatMessageVo` 或 `SendMessageVo`

### 3. 响应格式统一

所有新API都使用统一的 `ApiResponse<T>` 格式：
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {...},
  "timestamp": "2024-01-01T12:00:00"
}
```

## 兼容性说明

### 过渡期支持
- `ChatListController` 已标记为 `@Deprecated`，但仍然可用
- 前端 `chat.js` 模块同时提供新旧API方法
- 建议在新功能中使用新API，现有功能逐步迁移

### 完全迁移时间表
1. **第一阶段** (当前): 新旧API并存，新功能使用新API
2. **第二阶段** (1个月后): 发出废弃警告，鼓励迁移到新API
3. **第三阶段** (3个月后): 移除旧API，完全使用新API

## 新API优势

### 1. 统一的接口设计
- 所有聊天相关功能集中在一个Controller
- RESTful风格的URL设计
- 一致的请求/响应格式

### 2. 更好的安全性
- 自动从JWT token获取用户ID，防止伪造
- 统一的权限验证机制
- 更严格的参数校验

### 3. 更清晰的业务逻辑
- 聊天会话和消息的关系更明确
- 支持群聊和私聊的统一处理
- 更好的扩展性

### 4. 更好的性能
- 减少不必要的数据库查询
- 优化的分页机制
- 更高效的缓存策略

## 迁移检查清单

### 后端迁移
- [ ] 确认 `ChatController` 包含所有必要功能
- [ ] 标记旧Controller为 `@Deprecated`
- [ ] 更新API文档
- [ ] 添加迁移日志和监控

### 前端迁移
- [ ] 更新API调用使用新接口
- [ ] 移除对旧API的依赖
- [ ] 更新错误处理逻辑
- [ ] 测试所有聊天功能

### 测试验证
- [ ] 聊天列表获取功能
- [ ] 创建新聊天会话
- [ ] 发送和接收消息
- [ ] 消息已读状态
- [ ] 聊天会话删除
- [ ] 消息反应功能

## 常见问题

### Q: 旧API什么时候会被完全移除？
A: 计划在3个月后移除，具体时间会根据迁移进度调整。

### Q: 新API是否向后兼容？
A: 新API在功能上完全兼容，但请求格式有所变化，需要适配。

### Q: 如何处理现有的聊天数据？
A: 现有数据无需迁移，新API可以直接读取现有的聊天记录。

### Q: 性能是否有提升？
A: 是的，新API优化了数据库查询和缓存机制，性能有显著提升。

## 技术支持

如果在迁移过程中遇到问题，请：
1. 查看本文档的常见问题部分
2. 检查API文档和示例代码
3. 联系开发团队获取支持