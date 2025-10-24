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

          <!-- 用户信息 -->
          <div class="user-info">
            <AppleDropdown @command="handleUserCommand" placement="bottom-end">
              <template #trigger>
                <div class="user-avatar">
                  <div class="avatar-img">
                    <img
                      :src="userAvatar || defaultAvatar"
                      :alt="currentUser?.username || '用户'"
                      @error="onAvatarError"
                    />
                  </div>
                  <span class="username">{{ currentUser?.username || '用户' }}</span>
                  <span class="dropdown-arrow">▼</span>
                </div>
              </template>

              <AppleDropdownItem command="profile">
                <span class="item-icon">👤</span>
                个人资料
              </AppleDropdownItem>

              <AppleDropdownItem command="settings">
                <span class="item-icon">⚙️</span>
                设置
              </AppleDropdownItem>

              <div class="apple-dropdown-divider"></div>

              <AppleDropdownItem command="logout" danger>
                <span class="item-icon">🚪</span>
                退出登录
              </AppleDropdownItem>
            </AppleDropdown>
          </div>
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
import AppleDropdown from '@/components/common/AppleDropdown.vue';
import AppleDropdownItem from '@/components/common/AppleDropdownItem.vue';
import AsideMenu from './AsideMenu.vue';
import NotificationBell from './components/NotificationBell.vue';
import api from '@/api';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

// 响应式数据
const isSidebarCollapsed = ref(false);

// 计算属性
const currentUser = computed(() => authStore.currentUser);
const userAvatar = computed(() => currentUser.value?.avatar || '');
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

const handleUserCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'settings':
      router.push('/setting');
      break;
    case 'logout':
      await handleLogout();
      break;
  }
};

const handleLogout = async () => {
  try {
    // 使用原生 confirm 替代 ElMessageBox
    const confirmed = confirm('您确定要退出登录吗？');
    if (!confirmed) {
      return;
    }

    await api.auth.logout();
    authStore.logout();
    notificationStore.reset(); // 重置通知状态
    alert('已成功退出登录'); // 使用原生 alert 替代 ElMessage
    router.push('/login');
  } catch (error) {
    console.error('登出失败:', error);
    alert('登出失败，请稍后再试'); // 使用原生 alert 替代 ElMessage
    authStore.logout(); // 确保清理状态
    router.push('/login');
  }
};

const onAvatarError = (e) => {
  e.target.src = defaultAvatar;
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
  z-index: 1000;
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

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 12px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.user-avatar:hover {
  background-color: var(--apple-bg-tertiary);
  border-color: var(--apple-bg-quaternary);
}

.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--apple-bg-quaternary);
  flex-shrink: 0;
}

.avatar-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  font-weight: 500;
}

.dropdown-arrow {
  font-size: 12px;
  color: var(--apple-text-tertiary);
  transition: transform 0.2s ease;
}

.user-avatar:hover .dropdown-arrow {
  transform: rotate(180deg);
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
    z-index: 1001;
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

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .top-header {
    background-color: var(--apple-bg-primary);
    border-bottom-color: var(--apple-bg-quaternary);
  }

  .page-title {
    color: var(--apple-text-primary);
  }

  .username {
    color: var(--apple-text-secondary);
  }

  .user-avatar:hover {
    background-color: var(--apple-bg-tertiary);
  }
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