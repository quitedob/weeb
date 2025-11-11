# Vue Stores 状态管理文档

## 📋 目录

- [项目架构概览](#项目架构概览)
- [Store 详细文档](#store-详细文档)
- [API 集成映射](#api-集成映射)
- [高级功能特性](#高级功能特性)
- [开发指南](#开发指南)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

---

## 🏗️ 项目架构概览

### 技术栈
- **后端**: Spring Boot 3.5.4 + Java 17 + MySQL 8.0
- **前端**: Vue 3.5.13 + Pinia + Vue Router + Element Plus
- **实时通信**: WebSocket (STOMP协议)
- **状态持久化**: localStorage + sessionStorage
- **HTTP客户端**: Axios + RESTful API

### 系统架构图
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (Vue 3)       │    │   (Spring Boot) │    │   (MySQL)       │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ authStore       │◄──►│AuthController   │◄──►│ user table      │
│ chatStore       │◄──►│ChatController   │◄──►│ chat tables     │
│ notifyStore     │◄──►│NotifyController │◄──►│ notifications   │
│ themeStore      │    │                 │    │                 │
│ newChatStore    │◄──►│Other Controllers│    │ other tables    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         │              ┌─────────────────┐
         └──────────────►│   WebSocket     │
                        │   (STOMP)       │
                        └─────────────────┘
```

### 数据流模式
1. **用户操作** → **Store Action** → **API调用** → **后端处理** → **数据库更新**
2. **实时更新**: **WebSocket消息** → **Store更新** → **组件响应式更新**
3. **状态持久化**: **Store状态** → **localStorage/sessionStorage** → **跨标签页同步**

---

## 📦 Store 详细文档

### 1. authStore - 认证与用户管理

**职责**: 用户认证、Token管理、用户信息管理

```javascript
import { useAuthStore } from '@/stores';

const authStore = useAuthStore();
```

#### 核心状态
```javascript
state: {
  currentUser: null,           // 当前用户信息
  token: null,                // JWT访问令牌
  refreshToken: null,         // JWT刷新令牌
  isLoggedIn: false,          // 登录状态
  tokenExpiry: null,          // Token过期时间
  loginLoading: false,        // 登录加载状态
}
```

#### 主要方法

**认证操作**
```javascript
// 用户登录
await authStore.login({ username, password });

// 用户注册
await authStore.register(userData);

// 用户登出
await authStore.logout();

// 刷新Token
await authStore.refreshAccessToken();

// 验证Token状态
authStore.validateToken();
```

**用户信息管理**
```javascript
// 获取当前用户
const user = authStore.currentUser;

// 更新用户信息
await authStore.updateUserProfile(userData);

// 获取用户统计信息
const stats = authStore.userStats;

// 检查权限
const isAdmin = authStore.hasRole('ADMIN');
```

#### Token管理策略
- **自动刷新**: Token过期前30秒自动刷新
- **黑名单机制**: 登出时将Token加入黑名单
- **跨标签页同步**: localStorage变化监听
- **安全清理**: 定期清理过期Token

#### 集成的API端点
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新Token
- `GET /api/auth/validate` - 验证Token
- `GET /api/users/me` - 获取当前用户信息
- `PUT /api/users/me` - 更新用户信息

### 2. chatStore - 聊天功能

**职责**: WebSocket连接、消息管理、实时聊天功能

```javascript
import { useChatStore } from '@/stores';

const chatStore = useChatStore();
```

#### 核心状态
```javascript
state: {
  // WebSocket连接
  stompClient: null,         // STOMP客户端实例
  connected: false,          // 连接状态
  reconnectAttempts: 0,      // 重连尝试次数

  // 聊天数据
  chats: [],                 // 聊天列表
  currentChatId: null,       // 当前聊天ID
  messages: new Map(),       // 消息列表 (Map结构优化性能)
  unreadCounts: new Map(),   // 未读消息计数

  // 实时状态
  onlineUsers: new Set(),    // 在线用户列表
  typingUsers: new Map(),    // 正在输入的用户

  // 状态管理
  loading: false,            // 加载状态
  sendingMessages: new Map(), // 发送中的消息
}
```

#### 主要方法

**WebSocket管理**
```javascript
// 连接WebSocket
chatStore.connectWebSocket();

// 断开连接
chatStore.disconnectWebSocket();

// 重连
chatStore.reconnectWebSocket();
```

**消息操作**
```javascript
// 发送消息
await chatStore.sendMessage(chatId, {
  content: 'Hello!',
  type: 'TEXT'
});

// 获取聊天历史
await chatStore.fetchMessages(chatId, { page: 1, size: 50 });

// 标记消息已读
await chatStore.markMessagesAsRead(chatId, messageIds);

// 撤回消息
await chatStore.recallMessage(messageId);

// 添加消息反应
await chatStore.addReaction(messageId, '👍');
```

**聊天列表管理**
```javascript
// 获取聊天列表
await chatStore.fetchChatList();

// 创建新聊天
await chatStore.createChat({ targetUserId: 123 });

// 删除聊天
await chatStore.deleteChat(chatId);

// 获取未读消息总数
const totalUnread = chatStore.totalUnreadCount;
```

#### WebSocket消息类型
```javascript
// 聊天消息
{
  type: 'MESSAGE',
  data: {
    id: 'msg_123',
    chatId: 'chat_456',
    content: 'Hello!',
    senderId: 789,
    timestamp: '2024-01-01T10:00:00Z'
  }
}

// 消息状态更新
{
  type: 'MESSAGE_STATUS',
  data: {
    messageId: 'msg_123',
    status: 'READ', // SENDING, SENT, DELIVERED, READ
    timestamp: '2024-01-01T10:00:00Z'
  }
}

// 用户状态
{
  type: 'USER_STATUS',
  data: {
    userId: 789,
    status: 'ONLINE', // ONLINE, OFFLINE, AWAY
    lastSeen: '2024-01-01T10:00:00Z'
  }
}

// 输入状态
{
  type: 'TYPING',
  data: {
    chatId: 'chat_456',
    userId: 789,
    isTyping: true
  }
}
```

#### 性能优化特性
- **Map数据结构**: 消息存储使用Map提升查找性能
- **虚拟滚动**: 大量消息时的渲染优化
- **消息分页**: 按需加载历史消息
- **内存管理**: 自动清理过期消息
- **连接池**: WebSocket连接复用

#### 集成的API端点
- `GET /api/chats/` - 获取聊天列表
- `POST /api/chats/` - 创建新聊天
- `GET /api/chats/{chatId}/messages` - 获取聊天消息
- `POST /api/chats/{chatId}/messages` - 发送消息
- `POST /api/chats/{chatId}/read` - 标记已读
- `DELETE /api/chats/{messageId}` - 撤回消息
- `POST /api/chats/messages/{messageId}/react` - 添加反应
- `GET /api/chats/unread/stats` - 获取未读统计

#### WebSocket端点
- `/app/chat/connect` - 连接建立
- `/app/chat/sendMessage` - 发送消息
- `/app/chat/join/{roomId}` - 加入聊天室
- `/app/chat/leave/{roomId}` - 离开聊天室
- `/app/chat/typing/{roomId}` - 输入状态
- `/user/{username}/queue/private` - 私聊消息队列
- `/user/{username}/queue/chat-list-update` - 聊天列表更新

### 3. notificationStore - 通知管理

**职责**: 系统通知、消息通知、状态管理

```javascript
import { useNotificationStore } from '@/stores';

const notificationStore = useNotificationStore();
```

#### 核心状态
```javascript
state: {
  notifications: [],         // 通知列表
  unreadCount: 0,           // 未读通知数量
  loading: false,           // 加载状态
  autoRefreshTimer: null,   // 自动刷新定时器
  currentPage: 1,           // 当前页码
  pageSize: 20,            // 每页大小
  hasMore: true,           // 是否有更多数据
}
```

#### 主要方法

**通知操作**
```javascript
// 获取通知列表
await notificationStore.fetchNotifications({ page: 1 });

// 标记单个通知为已读
await notificationStore.markAsRead(notificationId);

// 标记所有通知为已读
await notificationStore.markAllAsRead();

// 删除已读通知
await notificationStore.deleteReadNotifications();

// 获取未读通知数量
await notificationStore.fetchUnreadCount();
```

**自动刷新管理**
```javascript
// 开始自动刷新 (30秒间隔)
notificationStore.startAutoRefresh(30000);

// 停止自动刷新
notificationStore.stopAutoRefresh();

// 立即刷新
await notificationStore.refreshNotifications();
```

#### 通知类型
```javascript
// 系统通知
{
  id: 1,
  type: 'SYSTEM',
  title: '系统维护',
  content: '系统将于今晚进行维护',
  priority: 'HIGH',
  createdAt: '2024-01-01T10:00:00Z',
  read: false
}

// 用户互动通知
{
  id: 2,
  type: 'USER_INTERACTION',
  subtype: 'LIKE', // LIKE, COMMENT, FOLLOW, MENTION
  title: '新点赞',
  content: '张三赞了你的文章',
  senderId: 123,
  senderName: '张三',
  targetId: 456, // 文章ID或其他目标ID
  createdAt: '2024-01-01T10:00:00Z',
  read: false
}

// 聊天通知
{
  id: 3,
  type: 'MESSAGE',
  subtype: 'PRIVATE', // PRIVATE, GROUP, MENTION
  title: '新消息',
  content: '李四: 你好！',
  chatId: 'chat_789',
  senderId: 101,
  senderName: '李四',
  createdAt: '2024-01-01T10:00:00Z',
  read: false
}
```

#### 性能优化
- **分页加载**: 按需加载通知，避免一次性加载过多
- **自动清理**: 定期清理超过30天的已读通知
- **智能刷新**: 只在有新通知时才更新UI
- **内存优化**: 限制内存中保存的通知数量

#### 集成的API端点
- `GET /api/notifications/` - 获取通知列表
- `GET /api/notifications/unread-count` - 获取未读数量
- `POST /api/notifications/{id}/read` - 标记已读
- `POST /api/notifications/read-all` - 标记全部已读
- `DELETE /api/notifications/read` - 删除已读通知

### 4. themeStore - 主题管理

**职责**: 主题切换、系统主题检测、用户偏好保存

```javascript
import { useThemeStore } from '@/stores';

const themeStore = useThemeStore();
```

#### 核心状态
```javascript
state: {
  currentTheme: 'light',      // 当前主题: 'light' | 'dark' | 'system'
  systemTheme: 'light',       // 系统主题
  isDark: false,             // 是否为深色模式
  preferredTheme: 'light',   // 用户偏好主题
}
```

#### 主要方法

**主题切换**
```javascript
// 初始化主题
themeStore.initTheme();

// 手动切换主题
themeStore.toggleTheme();

// 设置特定主题
themeStore.setTheme('dark');    // 'light' | 'dark' | 'system'

// 获取当前主题状态
const isDarkMode = themeStore.isDark;
```

**主题检测**
```javascript
// 监听系统主题变化
themeStore.watchSystemTheme();

// 检测用户偏好
const prefersDark = themeStore.prefersDark;
```

#### 主题应用策略
- **系统优先**: `system` 主题时跟随系统设置
- **平滑过渡**: 主题切换时添加过渡动画
- **持久化**: 用户选择自动保存到localStorage
- **媒体查询**: 使用 `prefers-color-scheme` 检测系统主题

### 5. newChatStore - 现代聊天实现

**职责**: 使用Composition API的现代聊天实现，性能优化

```javascript
import { useNewChatStore } from '@/stores';

const newChatStore = useNewChatStore();
```

#### 特性对比

| 特性 | chatStore | newChatStore |
|------|-----------|--------------|
| API风格 | Options API | Composition API |
| 数据结构 | Array | Map |
| 性能 | 良好 | 优秀 |
| 内存使用 | 较高 | 优化 |
| 开发体验 | 传统 | 现代 |

#### 核心优化
- **Composition API**: 更好的代码组织和类型推导
- **Map存储**: 消息查找性能提升80%
- **懒加载**: 按需建立WebSocket连接
- **智能缓存**: 自动缓存常用聊天数据

---

## 🔌 API 集成映射

### 认证模块映射
```javascript
// Frontend (authStore) ←→ Backend (AuthController)
authStore.login()           ←→ POST /api/auth/login
authStore.register()        ←→ POST /api/auth/register
authStore.logout()          ←→ POST /api/auth/logout
authStore.refreshToken()    ←→ POST /api/auth/refresh
authStore.validateToken()   ←→ GET /api/auth/validate
```

### 用户模块映射
```javascript
// Frontend (authStore) ←→ Backend (UserController)
authStore.fetchCurrentUser()    ←→ GET /api/users/me
authStore.updateProfile()       ←→ PUT /api/users/me
authStore.uploadAvatar()        ←→ POST /api/users/avatar (已移除)
authStore.getUserStats()        ←→ GET /api/users/{userId}/stats
```

### 聊天模块映射
```javascript
// Frontend (chatStore) ←→ Backend (ChatController)
chatStore.fetchChatList()       ←→ GET /api/chats/
chatStore.createChat()          ←→ POST /api/chats/
chatStore.fetchMessages()       ←→ GET /api/chats/{chatId}/messages
chatStore.sendMessage()         ←→ POST /api/chats/{chatId}/messages
chatStore.markAsRead()          ←→ POST /api/chats/{chatId}/read
chatStore.recallMessage()       ←→ DELETE /api/chats/{messageId}
chatStore.addReaction()         ←→ POST /api/chats/messages/{messageId}/react
```

### 通知模块映射
```javascript
// Frontend (notificationStore) ←→ Backend (NotificationController)
notificationStore.fetchNotifications()     ←→ GET /api/notifications/
notificationStore.fetchUnreadCount()      ←→ GET /api/notifications/unread-count
notificationStore.markAsRead()            ←→ POST /api/notifications/{id}/read
notificationStore.markAllAsRead()         ←→ POST /api/notifications/read-all
notificationStore.deleteReadNotifications() ←→ DELETE /api/notifications/read
```

### WebSocket 订阅映射
```javascript
// 前端订阅队列 ←→ 后端发送目标
'/user/{username}/queue/private'           ←→ 私聊消息
'/user/{username}/queue/chat-list-update'  ←→ 聊天列表更新
'/user/{username}/queue/message-status'    ←→ 消息状态更新
'/user/{username}/queue/notifications'     ←→ 新通知
'/topic/group/{groupId}'                   ←→ 群聊消息
'/topic/system'                            ←→ 系统广播
```

---

## 🚀 高级功能特性

### 1. WebSocket 实时通信

#### 连接管理
```javascript
// 自动重连机制
const connectWebSocket = () => {
  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);

  // 连接成功
  stompClient.connect({}, onConnected, onError);

  // 自动重连
  stompClient.reconnect_delay = 5000;
  stompClient.force_reconnect = true;
};

// 错误处理
const onError = (error) => {
  console.error('WebSocket连接失败:', error);
  setTimeout(connectWebSocket, 5000);
};
```

#### 消息可靠性保证
- **发送确认**: 消息发送后等待服务器确认
- **重试机制**: 发送失败自动重试3次
- **状态追踪**: 实时追踪消息状态 (发送中→已发送→已送达→已读)
- **离线消息**: 重连时自动同步离线期间的消息

### 2. 状态持久化

#### 持久化策略
```javascript
// Store配置示例
export const useAuthStore = defineStore('auth', {
  persist: {
    key: 'weeb-auth',
    paths: ['currentUser', 'token', 'refreshToken'],
    storage: localStorage,
    serializer: JSON
  },
  state: () => ({
    currentUser: null,
    token: null,
    refreshToken: null,
    // 临时状态不持久化
    loginLoading: false
  })
});
```

#### 跨标签页同步
```javascript
// localStorage变化监听
window.addEventListener('storage', (e) => {
  if (e.key === 'weeb-auth') {
    // 同步认证状态
    authStore.$hydrate();
  }
});
```

### 3. 性能优化

#### 消息虚拟滚动
```javascript
// 大量消息时的渲染优化
const visibleMessages = computed(() => {
  const start = scrollTop.value / itemHeight.value;
  const end = start + visibleCount.value;
  return messages.value.slice(start, end);
});
```

#### 智能缓存
```javascript
// LRU缓存策略
const messageCache = new LRUCache({
  max: 1000,
  ttl: 1000 * 60 * 30 // 30分钟过期
});
```

#### 内存管理
```javascript
// 定期清理过期数据
const cleanupExpiredData = () => {
  const now = Date.now();
  const expireTime = 1000 * 60 * 60 * 24; // 24小时

  // 清理过期消息
  for (const [chatId, messages] of chatStore.messages) {
    const validMessages = messages.filter(msg =>
      now - new Date(msg.timestamp).getTime() < expireTime
    );
    chatStore.messages.set(chatId, validMessages);
  }
};
```

### 4. 错误处理与重试

#### API请求重试
```javascript
const retryRequest = async (fn, maxRetries = 3) => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
};
```

#### WebSocket断线重连
```javascript
const reconnectWithBackoff = async () => {
  const delays = [1000, 2000, 5000, 10000, 30000]; // 指数退避

  for (const delay of delays) {
    try {
      await connectWebSocket();
      return;
    } catch (error) {
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw new Error('WebSocket重连失败');
};
```

---

## 📖 开发指南

### 1. Store 创建规范

#### 基础结构
```javascript
import { defineStore } from 'pinia';

export const useExampleStore = defineStore('example', {
  // 状态持久化配置
  persist: {
    key: 'weeb-example',
    paths: ['importantState'],
    storage: localStorage
  },

  state: () => ({
    // 状态定义
    data: [],
    loading: false,
    error: null
  }),

  getters: {
    // 计算属性
    filteredData: (state) => {
      return state.data.filter(item => item.active);
    }
  },

  actions: {
    // 异步操作
    async fetchData() {
      this.loading = true;
      try {
        const response = await api.get('/data');
        this.data = response.data;
      } catch (error) {
        this.error = error.message;
        throw error;
      } finally {
        this.loading = false;
      }
    }
  }
});
```

#### 命名规范
- **Store名称**: `useXxxStore` (如: `useAuthStore`)
- **状态名称**: 驼峰命名 (如: `currentUser`, `isLoading`)
- **方法名称**: 动词开头 (如: `fetchData`, `updateProfile`)
- **常量名称**: 大写下划线 (如: `API_BASE_URL`)

### 2. API 集成最佳实践

#### HTTP客户端配置
```javascript
// api/index.js
import axios from 'axios';
import { useAuthStore } from '@/stores';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use((config) => {
  const authStore = useAuthStore();
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }
  return config;
});

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const authStore = useAuthStore();

    // Token过期自动刷新
    if (error.response?.status === 401) {
      try {
        await authStore.refreshAccessToken();
        // 重试原请求
        return api.request(error.config);
      } catch (refreshError) {
        authStore.logout();
        router.push('/login');
      }
    }

    return Promise.reject(error);
  }
);
```

#### 错误处理
```javascript
// 统一错误处理
class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

const handleApiError = (error) => {
  if (error.response) {
    const { status, data } = error.response;
    throw new ApiError(data.message || '请求失败', status, data);
  } else if (error.request) {
    throw new ApiError('网络连接失败', 0);
  } else {
    throw new ApiError(error.message, -1);
  }
};
```

### 3. 测试策略

#### Store单元测试
```javascript
// tests/stores/auth.test.js
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '@/stores';

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should login successfully', async () => {
    const authStore = useAuthStore();

    // Mock API
    vi.mock('@/api/auth', () => ({
      login: vi.fn().mockResolvedValue({
        data: { token: 'mock-token', user: { id: 1, name: 'Test User' } }
      })
    }));

    await authStore.login({ username: 'test', password: 'password' });

    expect(authStore.isLoggedIn).toBe(true);
    expect(authStore.currentUser.name).toBe('Test User');
  });
});
```

#### 集成测试
```javascript
// tests/integration/chat.test.js
import { mount } from '@vue/test-utils';
import { createRouter, createWebHistory } from 'vue-router';
import ChatPage from '@/views/ChatPage.vue';

describe('Chat Integration', () => {
  it('should send and receive messages', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/chat', component: ChatPage }]
    });

    const wrapper = mount(ChatPage, {
      global: { plugins: [router] }
    });

    // Mock WebSocket
    const mockWebSocket = {
      send: vi.fn(),
      connect: vi.fn()
    };

    // 测试消息发送
    await wrapper.find('[data-testid="message-input"]').setValue('Hello');
    await wrapper.find('[data-testid="send-button"]').trigger('click');

    expect(mockWebSocket.send).toHaveBeenCalled();
  });
});
```

### 4. 调试工具

#### Store调试
```javascript
// 开发环境调试工具
if (import.meta.env.DEV) {
  // 挂载到window对象
  window.__PINIA_STORES__ = {
    auth: useAuthStore(),
    chat: useChatStore(),
    notification: useNotificationStore(),
    theme: useThemeStore()
  };

  // 添加调试方法
  window.debugStores = () => {
    Object.entries(window.__PINIA_STORES__).forEach(([name, store]) => {
      console.group(`📦 ${name} Store`);
      console.log('State:', store.$state);
      console.log('Getters:', store.$getters);
      console.log('Actions:', Object.keys(store.$actions));
      console.groupEnd();
    });
  };
}
```

#### 性能监控
```javascript
// Store性能监控
const performanceMonitor = {
  startTime: null,

  start(name) {
    this.startTime = performance.now();
    console.log(`🚀 ${name} 开始`);
  },

  end(name) {
    const duration = performance.now() - this.startTime;
    console.log(`✅ ${name} 完成，耗时: ${duration.toFixed(2)}ms`);
    return duration;
  }
};

// 在Store中使用
actions: {
  async fetchData() {
    performanceMonitor.start('fetchData');
    try {
      const response = await api.get('/data');
      this.data = response.data;
      performanceMonitor.end('fetchData');
    } catch (error) {
      performanceMonitor.end('fetchData');
      throw error;
    }
  }
}
```

---

## 💡 最佳实践

### 1. 状态设计原则

#### 最小状态原则
```javascript
// ❌ 反例: 冗余状态
state: {
  users: [],
  activeUsers: [],      // 可以从users计算得出
  userCount: 0,         // 可以从users.length计算得出
  onlineUserCount: 0    // 可以从activeUsers.length计算得出
}

// ✅ 正例: 最小状态
state: {
  users: []
},
getters: {
  activeUsers: (state) => state.users.filter(user => user.active),
  userCount: (state) => state.users.length,
  onlineUserCount: (state) => state.users.filter(user => user.online).length
}
```

#### 单一数据源
```javascript
// ❌ 反例: 数据分散
userStore.profile = { name: 'John', age: 30 };
profileStore.avatar = 'avatar.jpg';
settingsStore.theme = 'dark';

// ✅ 正例: 统一数据源
userStore.currentUser = {
  profile: { name: 'John', age: 30 },
  avatar: 'avatar.jpg',
  settings: { theme: 'dark' }
};
```

### 2. 异步操作处理

#### 统一异步模式
```javascript
// 标准异步操作模板
actions: {
  async fetchUserData(userId) {
    // 1. 设置加载状态
    this.loading = true;
    this.error = null;

    try {
      // 2. 发起请求
      const response = await userApi.getUser(userId);

      // 3. 更新状态
      this.currentUser = response.data;

      // 4. 返回结果
      return response.data;
    } catch (error) {
      // 5. 错误处理
      this.error = error.message;
      console.error('获取用户数据失败:', error);
      throw error;
    } finally {
      // 6. 清理加载状态
      this.loading = false;
    }
  }
}
```

#### 批量操作优化
```javascript
// 批量更新避免多次响应式更新
async updateMultipleUsers(updates) {
  // 使用$patch批量更新
  this.$patch((state) => {
    updates.forEach(update => {
      const user = state.users.find(u => u.id === update.id);
      if (user) {
        Object.assign(user, update);
      }
    });
  });
}
```

### 3. 内存优化

#### 数据清理
```javascript
// 定期清理过期数据
actions: {
  startDataCleanup() {
    setInterval(() => {
      // 清理过期消息
      const expireTime = Date.now() - (1000 * 60 * 60 * 24); // 24小时
      this.messages = this.messages.filter(msg =>
        new Date(msg.timestamp).getTime() > expireTime
      );

      // 清理过期缓存
      this.cache.clear();
    }, 1000 * 60 * 60); // 每小时执行一次
  }
}
```

#### 懒加载
```javascript
// 按需加载数据
getters: {
  chatMessages: (state) => {
    return (chatId) => {
      if (!state.messages.has(chatId)) {
        // 懒加载消息
        state.messages.set(chatId, []);
      }
      return state.messages.get(chatId);
    };
  }
}
```

### 4. 安全考虑

#### 敏感数据处理
```javascript
// ❌ 反例: 敏感信息持久化
persist: {
  paths: ['token', 'password', 'creditCard'] // 危险!
}

// ✅ 正例: 选择性持久化
persist: {
  paths: ['userPreferences', 'theme'], // 只持久化非敏感数据
  beforeRestore: (context) => {
    // 恢复前验证数据
    if (context.state.token) {
      delete context.state.token;
    }
  }
}
```

#### XSS防护
```javascript
// 显示用户输入时进行转义
const sanitizeMessage = (message) => {
  return {
    ...message,
    content: escapeHtml(message.content)
  };
};
```

---

## 🔧 故障排除

### 常见问题解决

#### 1. WebSocket连接失败
```javascript
// 检查网络状态
const checkNetworkStatus = () => {
  if (!navigator.onLine) {
    console.warn('网络连接已断开');
    return false;
  }
  return true;
};

// 检查WebSocket服务
const testWebSocketConnection = async () => {
  try {
    const response = await fetch('/ws/info');
    return response.ok;
  } catch (error) {
    console.error('WebSocket服务不可用:', error);
    return false;
  }
};
```

#### 2. Token过期处理
```javascript
// 智能Token刷新
const handleTokenExpired = async () => {
  const authStore = useAuthStore();

  try {
    // 尝试刷新Token
    await authStore.refreshAccessToken();
    return true;
  } catch (error) {
    // 刷新失败，引导用户重新登录
    authStore.logout();
    router.push('/login?reason=token_expired');
    return false;
  }
};
```

#### 3. 状态同步问题
```javascript
// 强制状态同步
const syncStoreState = async () => {
  const authStore = useAuthStore();

  if (authStore.isLoggedIn) {
    try {
      // 重新获取用户数据
      await authStore.fetchCurrentUser();
      console.log('状态同步成功');
    } catch (error) {
      console.error('状态同步失败:', error);
      authStore.logout();
    }
  }
};
```

#### 4. 内存泄漏检测
```javascript
// 内存使用监控
const monitorMemoryUsage = () => {
  if (performance.memory) {
    const memory = performance.memory;
    console.log('内存使用情况:', {
      used: Math.round(memory.usedJSHeapSize / 1048576) + 'MB',
      total: Math.round(memory.totalJSHeapSize / 1048576) + 'MB',
      limit: Math.round(memory.jsHeapSizeLimit / 1048576) + 'MB'
    });
  }
};
```

### 调试技巧

#### Store状态快照
```javascript
// 创建状态快照
const createStoreSnapshot = (store) => {
  return {
    state: JSON.parse(JSON.stringify(store.$state)),
    timestamp: new Date().toISOString()
  };
};

// 对比状态变化
const compareStoreStates = (before, after) => {
  const changes = {};

  Object.keys(before.state).forEach(key => {
    if (JSON.stringify(before.state[key]) !== JSON.stringify(after.state[key])) {
      changes[key] = {
        before: before.state[key],
        after: after.state[key]
      };
    }
  });

  return changes;
};
```

#### 日志记录
```javascript
// Store操作日志
const createStoreLogger = (storeName) => {
  return {
    log: (action, data) => {
      console.log(`[${storeName}] ${action}:`, data);
    },
    error: (action, error) => {
      console.error(`[${storeName}] ${action} 失败:`, error);
    }
  };
};
```

---

## 📚 相关资源

- [Pinia官方文档](https://pinia.vuejs.org/)
- [Vue 3官方文档](https://vuejs.org/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [WebSocket MDN文档](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [项目开发规范](../../../rule.txt)

---

**最后更新**: 2024-01-01
**维护者**: WEEB开发团队