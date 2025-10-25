<template>
  <el-dropdown @command="handleCommand" trigger="click" class="user-menu">
    <span class="user-avatar-wrapper">
      <el-avatar :src="userAvatar" :size="40">
        <span>{{ userInitial }}</span>
      </el-avatar>
      <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item disabled class="user-info-item">
          <div class="user-info">
            <div class="user-name">{{ userName }}</div>
            <div class="user-email">{{ userEmail }}</div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item divided command="profile">
          <el-icon><User /></el-icon>
          <span>个人资料</span>
        </el-dropdown-item>
        <el-dropdown-item command="settings">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </el-dropdown-item>
        <el-dropdown-item command="security">
          <el-icon><Lock /></el-icon>
          <span>安全中心</span>
        </el-dropdown-item>
        <el-dropdown-item divided command="logout" class="logout-item">
          <el-icon><SwitchButton /></el-icon>
          <span>登出</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { User, Setting, Lock, SwitchButton, ArrowDown } from '@element-plus/icons-vue';

const authStore = useAuthStore();
const router = useRouter();

const userName = computed(() => 
  authStore.currentUser?.nickname || 
  authStore.currentUser?.username || 
  '用户'
);

const userEmail = computed(() => 
  authStore.currentUser?.userEmail || 
  authStore.currentUser?.email || 
  ''
);

const userAvatar = computed(() => 
  authStore.currentUser?.avatar || ''
);

const userInitial = computed(() => {
  const name = userName.value;
  return name ? name.charAt(0).toUpperCase() : 'U';
});

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'settings':
      router.push('/settings');
      break;
    case 'security':
      router.push('/security');
      break;
    case 'logout':
      await handleLogout();
      break;
  }
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await authStore.logout();
    ElMessage.success('已退出登录');
    router.push('/login');
  } catch (error) {
    // 用户取消或出错
    if (error !== 'cancel') {
      console.error('登出失败:', error);
    }
  }
};
</script>

<style scoped>
.user-menu {
  cursor: pointer;
}

.user-avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background-color 0.3s;
}

.user-avatar-wrapper:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.dropdown-icon {
  font-size: 12px;
  transition: transform 0.3s;
}

.user-menu:hover .dropdown-icon {
  transform: rotate(180deg);
}

.user-info-item {
  cursor: default !important;
}

.user-info {
  padding: 8px 0;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.user-email {
  font-size: 12px;
  color: #909399;
}

.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.logout-item {
  color: #f56c6c;
}

.logout-item:hover {
  background-color: #fef0f0;
  color: #f56c6c;
}
</style>
