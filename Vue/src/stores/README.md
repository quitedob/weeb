# Store使用指南

## 📦 Store职责划分

### 1. authStore - 认证和用户管理
负责用户登录、登出、Token管理和用户信息管理。

```javascript
import { useAuthStore } from '@/stores';

const authStore = useAuthStore();

// 登录
await authStore.login({ username, password });

// 登出
await authStore.logout();

// 获取当前用户
const user = authStore.currentUser;

// 检查登录状态
if (authStore.isLoggedIn) {
  // 已登录
}

// 刷新Token
await authStore.refreshAccessToken();
```

### 2. chatStore - 聊天功能
负责WebSocket连接、消息发送接收、聊天列表管理。

```javascript
import { useChatStore } from '@/stores';

const chatStore = useChatStore();

// 连接WebSocket
chatStore.connectWebSocket();

// 发送消息
await chatStore.sendMessage(chatId, message);

// 获取当前聊天消息
const messages = chatStore.messagesForCurrentChat;

// 获取未读消息总数
const unreadCount = chatStore.totalUnreadCount;
```

### 3. notificationStore - 通知管理
负责系统通知的获取、标记已读、自动刷新。

```javascript
import { useNotificationStore } from '@/stores';

const notificationStore = useNotificationStore();

// 获取通知列表
await notificationStore.fetchNotifications();

// 标记为已读
await notificationStore.markAsRead(notificationId);

// 标记所有为已读
await notificationStore.markAllAsRead();

// 开始自动刷新（30秒）
notificationStore.startAutoRefresh(30000);
```

### 4. themeStore - 主题管理
负责主题切换和系统主题检测。

```javascript
import { useThemeStore } from '@/stores';

const themeStore = useThemeStore();

// 初始化主题
themeStore.initTheme();

// 切换主题
themeStore.toggleTheme();

// 设置主题
themeStore.setTheme('dark'); // 'light' | 'dark' | 'system'

// 获取当前主题
const isDark = themeStore.isDark;
```

## 🔧 初始化

### 在main.js中配置Pinia

```javascript
import { createApp } from 'vue';
import { setupPinia } from '@/stores/setup';
import App from './App.vue';

const app = createApp(App);

// 配置Pinia
const pinia = setupPinia({
  enablePersist: true,
  enableMonitor: import.meta.env.DEV,
  enableDevTools: import.meta.env.DEV,
  enableHistory: import.meta.env.DEV,
});

app.use(pinia);
app.mount('#app');
```

### 在App.vue中初始化Stores

```javascript
import { onMounted } from 'vue';
import { initializeStores } from '@/stores';

export default {
  setup() {
    onMounted(() => {
      // 初始化所有Store
      initializeStores();
    });
  }
};
```

## 📊 状态持久化

### 配置持久化

在Store定义中添加persist配置：

```javascript
export const useMyStore = defineStore('myStore', {
  persist: {
    key: 'my-store',
    paths: ['field1', 'field2'], // 只持久化指定字段
    storage: localStorage, // 或 sessionStorage
  },
  
  state: () => ({
    field1: 'value1',
    field2: 'value2',
    field3: 'value3', // 不会被持久化
  }),
});
```

### 清除持久化数据

```javascript
import { clearAllPersist } from '@/stores/plugins/persistPlugin';

// 清除所有持久化数据
clearAllPersist();

// 清除特定Store的持久化数据
const store = useMyStore();
store.$clearPersist();
```

## 👁️ Watch监听

### 监听登录状态

```javascript
import { watch } from 'vue';
import { useAuthStore } from '@/stores';

const authStore = useAuthStore();

watch(
  () => authStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn) {
      console.log('用户已登录');
    } else {
      console.log('用户已登出');
    }
  }
);
```

### 监听未读消息

```javascript
import { watch } from 'vue';
import { useChatStore } from '@/stores';

const chatStore = useChatStore();

watch(
  () => chatStore.totalUnreadCount,
  (newCount, oldCount) => {
    console.log(`未读消息从 ${oldCount} 变为 ${newCount}`);
  }
);
```

## 🔌 跨标签页同步

Store会自动监听localStorage变化，实现跨标签页同步：

- 登录/登出状态自动同步
- 主题设置自动同步
- Token更新自动同步

## 🚀 性能监控

### 查看性能统计

```javascript
const store = useMyStore();

// 获取性能统计
const stats = store.$getPerformanceStats();
console.log(stats);

// 输出示例：
// {
//   fetchData: {
//     count: 10,
//     totalTime: 1234.56,
//     avgTime: 123.45,
//     minTime: 100.12,
//     maxTime: 200.34
//   }
// }
```

### 调试Store

```javascript
const store = useMyStore();

// 打印调试信息
store.$debug();

// 获取状态快照
const snapshot = store.$snapshot();

// 对比状态变化
const diff = store.$diff(snapshot);
```

## 📜 历史记录

### 查看历史记录

```javascript
const store = useMyStore();

// 获取历史记录
const history = store.$getHistory();

// 撤销到上一个状态
store.$undo();

// 清除历史记录
store.$clearHistory();
```

## 🔔 浏览器通知

Store会自动请求浏览器通知权限，并在以下情况发送通知：

- 收到新消息
- 收到新通知

可以在浏览器设置中管理通知权限。

## 🌐 网络状态监听

Store会自动监听网络状态：

- 网络恢复时自动重连WebSocket
- 页面重新可见时自动刷新数据

## 🛠️ 开发工具

### 在控制台访问Store

```javascript
// 所有Store都挂载到window.__PINIA_STORES__
window.__PINIA_STORES__.auth
window.__PINIA_STORES__.chat
window.__PINIA_STORES__.notification
window.__PINIA_STORES__.theme
```

### 性能分析

```javascript
// 查看所有Store的性能统计
Object.keys(window.__PINIA_STORES__).forEach(key => {
  const store = window.__PINIA_STORES__[key];
  console.log(`${key}:`, store.$getPerformanceStats());
});
```

## ⚠️ 注意事项

1. **不要在Store外部直接修改state**
   ```javascript
   // ❌ 错误
   authStore.currentUser.name = 'New Name';
   
   // ✅ 正确
   authStore.setCurrentUser({ ...authStore.currentUser, name: 'New Name' });
   ```

2. **异步操作使用async/await**
   ```javascript
   // ✅ 正确
   try {
     await authStore.login(credentials);
   } catch (error) {
     console.error('登录失败', error);
   }
   ```

3. **清理定时器和监听器**
   ```javascript
   // 在组件卸载时清理
   onUnmounted(() => {
     notificationStore.stopAutoRefresh();
   });
   ```

4. **避免循环依赖**
   ```javascript
   // ❌ 避免Store之间的循环引用
   // Store A 引用 Store B，Store B 又引用 Store A
   
   // ✅ 使用事件或回调解耦
   ```

## 📚 更多资源

- [Pinia官方文档](https://pinia.vuejs.org/)
- [Vue 3文档](https://vuejs.org/)
- [项目开发规范](../../../rule.txt)
