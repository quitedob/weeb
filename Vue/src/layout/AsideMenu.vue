<template>
  <el-menu
    :default-active="activeMenu"
    class="aside-menu"
    :collapse="isCollapse"
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409EFF"
    router
  >
    <div class="logo-container" @click="goHome">
      <img src="@/assets/logo.png" alt="Logo" class="logo-img" v-if="!isCollapse"/>
      <span v-if="!isCollapse" class="logo-text">Weeb IM</span>
      <img src="@/assets/logo-small.png" alt="Logo" class="logo-img-small" v-else/>
    </div>

    <el-menu-item index="/chat">
      <el-icon><ChatDotRound /></el-icon>
      <template #title>聊天</template>
    </el-menu-item>
    <el-menu-item index="/contact">
      <el-icon><User /></el-icon>
      <template #title>联系人</template>
    </el-menu-item>

    <el-menu-item index="/groups">
      <el-icon><Collection /></el-icon> <template #title>群组</template>
    </el-menu-item>

    <el-menu-item index="/search">
      <el-icon><Search /></el-icon>
      <template #title>搜索</template>
    </el-menu-item>

    <!-- User Info Display -->
    <el-menu-item v-if="authStore.currentUser && !isCollapse" class="user-info-menu-item" disabled>
      <el-icon><Avatar /></el-icon> <!-- Added Avatar icon -->
      <template #title>{{ authStore.currentUser.username }}</template>
    </el-menu-item>
    <!-- End User Info Display -->

    <div class="menu-spacer"></div>

    <el-menu-item index="/setting">
      <el-icon><Setting /></el-icon>
      <template #title>设置</template>
    </el-menu-item>
    <el-menu-item @click="handleLogout">
      <el-icon><SwitchButton /></el-icon>
      <template #title>退出登录</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ChatDotRound,
  User,
  Setting,
  Search,
  // Document, // Article icon, can be added later
  Collection,
  SwitchButton,
  Avatar // Added Avatar icon for user info display
} from '@element-plus/icons-vue';
import { useAppStore } from '@/stores/appStore'; // Assuming appStore.js will be created
import { useAuthStore } from '@/stores/authStore';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/api';

// Assuming appStore.js might be simple like:
// import { defineStore } from 'pinia';
// export const useAppStore = defineStore('app', {
//   state: () => ({ isSidebarCollapsed: false }),
//   actions: { toggleSidebar() { this.isSidebarCollapsed = !this.isSidebarCollapsed; } }
// });
// For now, if useAppStore is not available, provide a placeholder to avoid immediate error.
let appStoreInstance; // Renamed to avoid conflict with the import
let authStoreInstance;  // Renamed to avoid conflict with the import
try {
  appStoreInstance = useAppStore();
} catch (e) {
  console.warn("AsideMenu.vue: appStore not available, using placeholder. Functionality like sidebar collapse might be affected.");
  appStoreInstance = { isSidebarCollapsed: false, toggleSidebar: () => {} }; // Placeholder
}

try {
  authStoreInstance = useAuthStore();
} catch (e) {
  console.warn("AsideMenu.vue: authStore not available, using placeholder. User info and logout might be affected.");
  authStoreInstance = { currentUser: null, logout: () => console.log("Placeholder authStore.logout called") }; // Placeholder
}
// Use the potentially placeholder instances with the original names used in the template/script logic
const appStore = appStoreInstance;
const authStore = authStoreInstance;


const route = useRoute();
const router = useRouter();

const isCollapse = computed(() => appStore.isSidebarCollapsed);
const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const goHome = () => {
    router.push('/');
}

const handleLogout = async () => {
  ElMessageBox.confirm('您确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      // This should use the modular API structure: api.auth.logout()
      await api.auth.logout();
      // authStore.logout() in Pinia store should handle clearing localStorage and state
      if (authStore.logout) { // Check if placeholder or real store
        // This calls the action in Pinia store, which should also clear state and localStorage
        authStore.logout(); // This is the Pinia action, which itself calls api.auth.logout and then cleanup
      }
      ElMessage.success('已成功退出登录');
      router.push('/login');
    } catch (error) {
      console.error('登出失败:', error);
      ElMessage.error('登出失败，请稍后再试');
      if (authStore.logout) { // Ensure cleanup even if API call within store action failed
        authStore.logoutCleanup ? authStore.logoutCleanup() : authStore.logout();
      }
      router.push('/login'); // Ensure redirection
    }
  }).catch(() => {
    // User cancelled
  });
};
</script>

<style scoped>
.aside-menu:not(.el-menu--collapse) {
  width: 210px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.aside-menu {
   min-height: 100vh;
   display: flex;
   flex-direction: column;
   border-right: none;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b2f3a;
  cursor: pointer;
  padding: 0 10px;
  box-sizing: border-box;
}

.logo-img {
  height: 32px;
  width: 32px;
  margin-right: 12px;
}
.logo-img-small {
  height: 32px;
  width: 32px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
}

.el-menu-item {
  font-size: 14px;
}
.el-menu-item i, .el-menu-item .el-icon {
  margin-right: 5px;
  width: 24px;
  text-align: center;
  font-size: 18px;
  vertical-align: middle;
}

.user-info-menu-item.is-disabled { /* Style for disabled user info item */
  opacity: 1 !important;
  cursor: default !important;
  color: #bfcbd9 !important; /* Match text-color */
}
.user-info-menu-item .el-icon { /* Ensure icon color matches */
 color: #bfcbd9 !important;
}


.menu-spacer {
  flex-grow: 1;
}

.el-menu--collapse .logo-container {
  padding: 0;
}
.el-menu--collapse .el-menu-item .el-tooltip__trigger {
  padding: 0 20px !important;
}
</style>
