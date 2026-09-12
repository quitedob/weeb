/**
 * Store统一管理和初始化
 * 负责Store的职责划分、状态持久化和watch监听
 */

import { watch } from 'vue';
import { useAuthStore } from './authStore';
import { useChatStore } from './chatStore';
import { useNotificationStore } from './notificationStore';
import { useThemeStore } from './themeStore';

/**
 * Store职责划分：
 * 
 * 1. authStore - 认证和用户管理
 *    - 用户登录/登出
 *    - Token管理和自动刷新
 *    - 用户信息管理
 * 
 * 2. chatStore - 聊天功能
 *    - WebSocket连接管理
 *    - 消息发送和接收
 *    - 聊天列表管理
 *    - 未读消息计数
 * 
 * 3. notificationStore - 通知管理
 *    - 系统通知
 *    - 未读通知计数
 *    - 通知自动刷新
 * 
 * 4. themeStore - 主题管理
 *    - 主题切换
 *    - 系统主题检测
 */

// Store初始化标志
let initialized = false;

/**
 * 初始化所有Store
 */
export function initializeStores() {
  if (initialized) {
    console.log('Stores已初始化，跳过');
    return;
  }

  console.log('🚀 初始化Stores...');

  const authStore = useAuthStore();
  const chatStore = useChatStore();
  const notificationStore = useNotificationStore();
  const themeStore = useThemeStore();

  // 1. 初始化认证状态
  authStore.syncAuthStatus();
  authStore.startTokenRefreshTimer();

  // 2. 初始化主题
  themeStore.initTheme();

  // 3. 设置Store间的watch监听
  setupStoreWatchers();

  // 4. 设置跨标签页同步
  setupCrossTabSync();

  initialized = true;
  console.log('✅ Stores初始化完成');
}

/**
 * 设置Store间的watch监听
 */
function setupStoreWatchers() {
  const authStore = useAuthStore();
  const chatStore = useChatStore();
  const notificationStore = useNotificationStore();

  // 监听登录状态变化
  watch(
    () => authStore.isLoggedIn,
    (isLoggedIn, wasLoggedIn) => {
      console.log('🔐 登录状态变化:', { isLoggedIn, wasLoggedIn });

      if (isLoggedIn && !wasLoggedIn) {
        // 用户登录
        console.log('✅ 用户已登录，初始化服务');
        
        // 连接WebSocket
        chatStore.connectWebSocket();
        authStore.loadPreferences().catch(error => console.error('读取设置失败:', error));
        
        // 开始自动刷新通知
        notificationStore.startAutoRefresh();
        
        // 获取初始数据
        notificationStore.fetchUnreadCount();
        
      } else if (!isLoggedIn && wasLoggedIn) {
        // 用户登出
        console.log('👋 用户已登出，清理服务');
        
        // 断开WebSocket
        if (chatStore.stompClient) {
          chatStore.disconnectWebSocket();
        }
        
        // 停止自动刷新
        notificationStore.stopAutoRefresh();
        
        // 清理数据
        chatStore.$reset();
        notificationStore.resetState();
      }
    },
    { immediate: true, flush: 'sync' }
  );

  // 监听Token即将过期
  watch(
    () => authStore.needsRefresh,
    (needsRefresh) => {
      if (needsRefresh && !authStore.isRefreshing) {
        console.log('⏰ Token即将过期，触发自动刷新');
        authStore.refreshAccessToken().catch(error => {
          console.error('自动刷新Token失败:', error);
        });
      }
    }
  );

  // 监听WebSocket连接状态
  watch(
    () => chatStore.connectionStatus,
    (status, oldStatus) => {
      console.log('🔌 WebSocket状态变化:', { oldStatus, status });

      if (status === 'connected' && oldStatus !== 'connected') {
        console.log('✅ WebSocket已连接');
      } else if (status === 'error') {
        console.error('❌ WebSocket连接错误');
      }
    }
  );

  // 监听未读消息总数变化
  watch(
    () => chatStore.totalUnreadCount,
    (newCount, oldCount) => {
      if (newCount > oldCount) {
        console.log(`📬 新增 ${newCount - oldCount} 条未读消息`);
        
        // 更新页面标题
        updatePageTitle(newCount);
        
        // 发送浏览器通知
        if (authStore.preferences?.notifications?.newMessages === true) {
          sendBrowserNotification('新消息', `您有 ${newCount} 条未读消息`);
        }
      }
    }
  );

  // 监听未读通知数量变化
  watch(
    () => notificationStore.unreadCount,
    (newCount, oldCount) => {
      if (newCount > oldCount) {
        console.log(`🔔 新增 ${newCount - oldCount} 条未读通知`);
        
        // 发送浏览器通知
        sendBrowserNotification('新通知', `您有 ${newCount} 条未读通知`);
      }
    }
  );

  // 监听当前聊天变化
  watch(
    () => chatStore.currentChatId,
    (newChatId, oldChatId) => {
      if (newChatId && newChatId !== oldChatId) {
        console.log('💬 切换聊天:', { oldChatId, newChatId });
        
        // 标记当前聊天为已读
        chatStore.markChatAsRead(newChatId);
      }
    }
  );
}

/**
 * 设置跨标签页同步
 */
function setupCrossTabSync() {
  // 监听localStorage变化（跨标签页同步）
  window.addEventListener('storage', (event) => {
    const authStore = useAuthStore();
    const themeStore = useThemeStore();

    // 同步登录状态
    if (event.key === 'jwt_token') {
      if (event.newValue) {
        console.log('🔄 其他标签页登录，同步状态');
        authStore.syncAuthStatus();
      } else {
        console.log('🔄 其他标签页登出，同步状态');
        authStore.logoutCleanup();
      }
    }

    // 同步主题设置
    if (event.key === 'theme-preference') {
      console.log('🔄 其他标签页更改主题，同步设置');
      themeStore.initTheme();
    }
  });

  // 监听页面可见性变化
  document.addEventListener('visibilitychange', () => {
    const authStore = useAuthStore();
    const chatStore = useChatStore();
    const notificationStore = useNotificationStore();

    if (!document.hidden) {
      console.log('👁️ 页面可见，刷新数据');

      // 页面重新可见时，刷新数据
      if (authStore.isLoggedIn) {
        // 检查Token是否需要刷新
        if (authStore.needsRefresh) {
          authStore.refreshAccessToken();
        }

        // 重新连接WebSocket（如果断开）
        if (chatStore.connectionStatus !== 'connected') {
          chatStore.connectWebSocket();
        }

        // 刷新未读计数
        notificationStore.fetchUnreadCount();
      }
    } else {
      console.log('👁️ 页面隐藏');
    }
  });

  // 监听在线/离线状态
  window.addEventListener('online', () => {
    console.log('🌐 网络已连接');
    
    const authStore = useAuthStore();
    const chatStore = useChatStore();

    if (authStore.isLoggedIn) {
      // 重新连接WebSocket
      chatStore.connectWebSocket();
    }
  });

  window.addEventListener('offline', () => {
    console.log('🌐 网络已断开');
  });
}

/**
 * 更新页面标题
 */
function updatePageTitle(unreadCount) {
  const baseTitle = 'Weeb';
  
  if (unreadCount > 0) {
    document.title = `(${unreadCount}) ${baseTitle}`;
  } else {
    document.title = baseTitle;
  }
}

/**
 * 发送浏览器通知
 */
function sendBrowserNotification(title, body) {
  // 检查浏览器是否支持通知
  if (!('Notification' in window)) {
    return;
  }

  // 检查通知权限
  if (Notification.permission === 'granted') {
    new Notification(title, {
      body,
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      tag: 'weeb-notification',
      renotify: true
    });
  } else if (Notification.permission !== 'denied') {
    // 请求通知权限
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        new Notification(title, {
          body,
          icon: '/favicon.ico'
        });
      }
    });
  }
}

/**
 * 重置所有Store
 */
export function resetAllStores() {
  console.log('🔄 重置所有Stores');

  const authStore = useAuthStore();
  const chatStore = useChatStore();
  const notificationStore = useNotificationStore();

  authStore.logoutCleanup();
  chatStore.$reset();
  notificationStore.resetState();

  initialized = false;
}

/**
 * 导出所有Store
 */
export {
  useAuthStore,
  useChatStore,
  useNotificationStore,
  useThemeStore
};
