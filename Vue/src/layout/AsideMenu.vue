<template>
  <div class="aside-menu">
    <!-- 用户信息区域 -->
    <div class="user-section">
      <div class="user-avatar">
        <div class="avatar-placeholder">
          {{ currentUser?.username?.charAt(0)?.toUpperCase() || 'U' }}
        </div>
      </div>
      <div class="user-info">
        <div class="username">{{ currentUser?.username || '用户' }}</div>
        <div class="user-status">在线</div>
      </div>
    </div>

    <!-- 导航菜单 -->
    <nav class="nav-menu">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: $route.path === item.path }"
      >
        <div class="nav-icon">{{ item.icon }}</div>
        <span class="nav-text">{{ item.title }}</span>
        <div v-if="item.badge" class="nav-badge apple-badge">{{ item.badge }}</div>
      </router-link>
    </nav>

    <!-- 底部操作 -->
    <div class="menu-footer">
      <button class="apple-button apple-button-secondary settings-button" @click="goToSettings">
        <span class="button-icon">⚙️</span>
        <span class="button-text">设置</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// 当前用户信息
const currentUser = computed(() => authStore.currentUser)

// 未读通知数量
const unreadCount = computed(() => notificationStore.unreadCount)

// 菜单项配置
const menuItems = computed(() => [
  {
    path: '/chat',
    title: '聊天',
    icon: '💬',
    badge: null
  },
  {
    path: '/contact',
    title: '联系人',
    icon: '👥',
    badge: null
  },
  {
    path: '/groups',
    title: '群组',
    icon: '👨‍👩‍👧‍👦',
    badge: null
  },
  {
    path: '/article',
    title: '文章',
    icon: '📝',
    badge: null
  },
  {
    path: '/notifications',
    title: '通知',
    icon: '🔔',
    badge: unreadCount.value > 0 ? unreadCount.value : null
  },
  {
    path: '/search',
    title: '搜索',
    icon: '🔍',
    badge: null
  }
])

// 跳转到设置页面
const goToSettings = () => {
  router.push('/setting')
}
</script>

<style scoped>
.aside-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--apple-bg-secondary);
  border-right: 1px solid var(--apple-border-secondary);
}

.user-section {
  padding: var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-secondary);
   display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
}

.user-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--apple-blue), var(--apple-purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--apple-font-xl);
  font-weight: 600;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.username {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  font-size: var(--apple-font-sm);
  color: var(--apple-green);
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
}

.user-status::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--apple-green);
}

.nav-menu {
  flex: 1;
  padding: var(--apple-spacing-md) 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: var(--apple-spacing-md) var(--apple-spacing-lg);
  color: var(--apple-text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
  margin: 0 var(--apple-spacing-sm);
  border-radius: var(--apple-radius-medium);
}

.nav-item:hover {
  background-color: var(--apple-bg-tertiary);
  color: var(--apple-text-primary);
}

.nav-item.active {
  background-color: var(--apple-blue);
  color: white;
}

.nav-item.active .nav-badge {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}

.nav-icon {
  font-size: var(--apple-font-lg);
  margin-right: var(--apple-spacing-md);
  width: 24px;
  text-align: center;
}

.nav-text {
  flex: 1;
  font-size: var(--apple-font-md);
  font-weight: 500;
}

.nav-badge {
  margin-left: var(--apple-spacing-sm);
  min-width: 20px;
  height: 20px;
  font-size: var(--apple-font-xs);
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-footer {
  padding: var(--apple-spacing-lg);
  border-top: 1px solid var(--apple-border-secondary);
}

.settings-button {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--apple-spacing-sm);
  font-size: var(--apple-font-md);
  font-weight: 500;
}

.button-icon {
  font-size: var(--apple-font-md);
}

.button-text {
  font-size: var(--apple-font-md);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .aside-menu {
    width: 100%;
    height: 100vh;
  }
  
  .user-section {
    padding: var(--apple-spacing-md);
}

  .nav-menu {
    padding: var(--apple-spacing-sm) 0;
  }
  
  .nav-item {
    padding: var(--apple-spacing-md);
    margin: 0 var(--apple-spacing-xs);
}

  .menu-footer {
    padding: var(--apple-spacing-md);
}
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .aside-menu {
    background-color: var(--apple-bg-secondary);
  }
  
  .user-section {
    border-bottom-color: var(--apple-border-primary);
  }
  
  .menu-footer {
    border-top-color: var(--apple-border-primary);
  }
}
</style>
