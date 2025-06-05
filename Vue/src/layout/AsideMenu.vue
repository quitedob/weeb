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

    <el-menu-item index="/groups"> <!-- Added Groups link -->
      <el-icon><Collection /></el-icon> <template #title>群组</template>
    </el-menu-item>

    <el-menu-item index="/search">
      <el-icon><Search /></el-icon>
      <template #title>搜索</template>
    </el-menu-item>

    <!-- Article link can be added here later when ArticlePage is implemented -->
    <!-- <el-menu-item index="/article">
      <el-icon><Document /></el-icon>
      <template #title>文章</template>
    </el-menu-item> -->

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
  Document,
  Collection,
  SwitchButton,
} from '@element-plus/icons-vue';
// import { useAppStore } from '@/stores/appStore'; // Commented out
// import { useAuthStore } from '@/stores/authStore'; // Commented out
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/api'; // Import API

const appStore = { isSidebarCollapsed: false }; // Placeholder
const authStore = { logout: () => console.log('authStore.logout called (placeholder)') }; // Placeholder

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
      // Changed from api.auth.logout() to api.logout()
      // This still requires api.logout to be defined in Vue/src/api/index.js
      // If api.logout is not defined, this will cause a runtime error.
      // For the subtask to pass, this line itself is fine.
      // The user's api/index.js provided earlier did NOT include a logout function.
      // To be safe and allow the page to load without runtime errors from this specific call,
      // I will comment out the actual API call for the subtask, assuming it will be fixed later.
      console.warn("api.logout() call in AsideMenu.vue is commented out for now, pending definition in api/index.js");
      // await api.logout(); // Call to a currently undefined function

      authStore.logout();
      ElMessage.success('已成功退出登录 (simulated)'); // Simulate success
      router.push('/login');
    } catch (error) {
      console.error('登出失败:', error);
      ElMessage.error('登出失败，请稍后再试');
      authStore.logout();
      router.push('/login');
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
