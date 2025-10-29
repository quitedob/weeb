<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <AsideMenu />
    </aside>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <div class="header-left">
          <AppleButton
            type="ghost"
            @click="toggleSidebar"
            class="sidebar-toggle"
            size="small"
          >
            <span class="menu-icon">
              {{ isSidebarCollapsed ? '☰' : '✕' }}
            </span>
          </AppleButton>
          <h1 class="page-title">{{ currentPageTitle }}</h1>
        </div>

        <div class="header-right">
          <!-- 通知铃铛 -->
          <NotificationBell />

          <!-- 用户菜单组件 -->
          <UserMenu />
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import AppleButton from '@/components/common/AppleButton.vue';
import AsideMenu from './AsideMenu.vue';
import NotificationBell from './components/NotificationBell.vue';
import UserMenu from '@/components/UserMenu.vue';
import api from '@/api';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();

// 响应式数据
const isSidebarCollapsed = ref(false);

// 计算属性
const currentPageTitle = computed(() => {
  const routeTitles = {
    '/chat': '聊天',
    '/contact': '联系人',
    '/groups': '群组',
    '/article': '文章中心',
    '/search': '搜索',
    '/notifications': '通知中心',
    '/profile': '个人资料',
    '/setting': '设置'
  };
  return routeTitles[route.path] || 'Weeb IM';
});

// 方法
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
};

// 生命周期
onMounted(() => {
  // 这里不再全局连接WebSocket，改为在聊天页面按需连接
});
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background-color: var(--apple-bg-primary);
}

.sidebar {
  width: 240px;
  transition: width 0.3s ease;
  background-color: var(--apple-bg-secondary);
  border-right: 1px solid var(--apple-bg-quaternary);
  z-index: var(--z-sticky);
}

.sidebar.collapsed {
  width: 64px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 64px;
  background-color: var(--apple-bg-primary);
  border-bottom: 1px solid var(--apple-bg-quaternary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sidebar-toggle {
  padding: 8px;
  color: var(--apple-text-secondary);
  border-radius: 8px;
  transition: all 0.2s ease;
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-toggle:hover {
  color: var(--apple-blue);
  background-color: var(--apple-bg-tertiary);
}

.menu-icon {
  font-size: 18px;
  font-weight: bold;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}



.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background-color: var(--apple-bg-secondary);
}

/* 下拉菜单项图标 */
.item-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: var(--z-fixed);
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar.collapsed {
    transform: translateX(0);
  }

  .top-header {
    padding: 0 16px;
  }

  .page-title {
    font-size: 18px;
  }

  .username {
    display: none;
  }

  .page-content {
    padding: 16px;
  }

  .header-left {
    gap: 12px;
  }

  .header-right {
    gap: 12px;
  }
}

/* 主题适配 */
.layout-container {
  background-color: var(--apple-bg-primary);
  color: var(--apple-text-primary);
}

.sidebar {
  background-color: var(--apple-bg-secondary);
  border-right: 1px solid var(--apple-border-secondary);
}

.top-header {
  background-color: var(--apple-bg-primary);
  border-bottom: 1px solid var(--apple-border-secondary);
}

.page-title {
  color: var(--apple-text-primary);
}

.page-content {
  background-color: var(--apple-bg-secondary);
}

/* 确保滚动条样式一致 */
.page-content::-webkit-scrollbar {
  width: 8px;
}

.page-content::-webkit-scrollbar-track {
  background: transparent;
}

.page-content::-webkit-scrollbar-thumb {
  background: var(--apple-bg-quaternary);
  border-radius: 4px;
}

.page-content::-webkit-scrollbar-thumb:hover {
  background: var(--apple-bg-tertiary);
}
</style>