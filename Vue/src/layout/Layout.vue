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
          <el-button
            type="text"
            @click="toggleSidebar"
            class="sidebar-toggle"
          >
            <el-icon size="20">
              <Fold v-if="!isSidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <h1 class="page-title">{{ currentPageTitle }}</h1>
        </div>

        <div class="header-right">
          <!-- 通知铃铛 -->
          <NotificationBell />
          
          <!-- 用户信息 -->
          <div class="user-info">
            <el-dropdown @command="handleUserCommand">
              <div class="user-avatar">
                <el-avatar :size="32" :src="userAvatar" />
                <span class="username">{{ currentUser?.username || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                  <el-dropdown-item command="settings">设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Fold, Expand, ArrowDown } from '@element-plus/icons-vue';
import AsideMenu from './AsideMenu.vue';
import NotificationBell from './components/NotificationBell.vue';
import api from '@/api';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();

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
    '/settings': '设置'
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
      router.push('/settings');
      break;
    case 'logout':
      await handleLogout();
      break;
  }
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('您确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    await api.auth.logout();
    authStore.logout();
    notificationStore.reset(); // 重置通知状态
    ElMessage.success('已成功退出登录');
    router.push('/login');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('登出失败:', error);
      ElMessage.error('登出失败，请稍后再试');
      authStore.logout(); // 确保清理状态
      router.push('/login');
    }
  }
};

// 生命周期
onMounted(() => {
  // 初始化WebSocket连接（如果需要）
  // 这里可以添加WebSocket连接逻辑
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
  border-right: 1px solid var(--apple-border-secondary);
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
  border-bottom: 1px solid var(--apple-border-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--apple-spacing-lg);
  box-shadow: var(--apple-shadow-light);
  backdrop-filter: blur(20px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
}

.sidebar-toggle {
  padding: var(--apple-spacing-sm);
  color: var(--apple-text-secondary);
  border-radius: var(--apple-radius-medium);
  transition: all 0.2s ease;
}

.sidebar-toggle:hover {
  color: var(--apple-blue);
  background-color: var(--apple-bg-secondary);
}

.page-title {
  margin: 0;
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  cursor: pointer;
  padding: var(--apple-spacing-xs) var(--apple-spacing-sm);
  border-radius: var(--apple-radius-medium);
  transition: all 0.2s ease;
}

.user-avatar:hover {
  background-color: var(--apple-bg-secondary);
}

.username {
  font-size: var(--apple-font-md);
  color: var(--apple-text-secondary);
  margin: 0 var(--apple-spacing-xs);
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--apple-spacing-lg);
  background-color: var(--apple-bg-secondary);
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

  .page-title {
    font-size: var(--apple-font-lg);
  }

  .username {
    display: none;
  }

  .page-content {
    padding: var(--apple-spacing-md);
  }
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .top-header {
    background-color: var(--apple-bg-primary);
    border-bottom-color: var(--apple-border-primary);
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
</style> 