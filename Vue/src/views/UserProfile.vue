<template>
  <div class="user-profile-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="skeleton-loader">
        <div class="skeleton-item" v-for="i in 8" :key="i">
          <div class="skeleton-line"></div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <AppleCard class="error-card">
        <div class="error-content">
          <div class="error-icon">⚠️</div>
          <h3 class="error-title">加载失败</h3>
          <p class="error-message">{{ error }}</p>
          <AppleButton
            @click="loadUserData"
            type="primary"
            size="small"
          >
            重试
          </AppleButton>
        </div>
      </AppleCard>
    </div>

    <!-- 用户信息 -->
    <div v-else-if="currentUser" class="user-profile">
      <!-- 用户基本信息卡片 -->
      <AppleCard class="user-info-card">
        <div class="user-header">
          <div class="avatar-section">
            <div class="user-avatar">
              <img
                :src="currentUser.avatar || 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'"
                :alt="currentUser.username"
                class="avatar-img"
              />
            </div>
            <AppleButton
              @click="handleAvatarChange"
              type="secondary"
              size="small"
              class="avatar-change-btn"
            >
              更换头像
            </AppleButton>
          </div>

          <div class="user-basic-info">
            <h2 class="username">{{ currentUser.username || '未知用户' }}</h2>
            <p class="nickname" v-if="currentUser.nickname">
              {{ currentUser.nickname }}
            </p>
            <p class="bio" v-if="currentUser.bio">
              {{ currentUser.bio }}
            </p>
            <div class="user-meta">
              <span class="user-level">
                用户等级: {{ getUserLevelDisplay(currentUser.userLevel) }}
                <router-link to="/level-history" class="level-history-link">
                  <el-icon><View /></el-icon>
                  查看历史
                </router-link>
              </span>
              <span class="join-time">加入时间: {{ formatDate(currentUser.createdAt) }}</span>
            </div>
          </div>

          <div class="user-actions">
            <AppleButton
              @click="editMode = !editMode"
              :type="editMode ? 'primary' : 'secondary'"
              size="small"
            >
              {{ editMode ? '取消编辑' : '编辑资料' }}
            </AppleButton>
          </div>
        </div>
      </AppleCard>

      <!-- 编辑模式 -->
      <AppleCard v-if="editMode" class="edit-card">
        <template #header>
          <h3>编辑个人资料</h3>
        </template>

        <div class="edit-form">
          <div class="form-group">
            <label>用户名</label>
            <AppleInput
              v-model="editForm.username"
              placeholder="用户名"
              :disabled="true"
              size="small"
            />
          </div>

          <div class="form-group" :class="{ 'has-error': errors.nickname }">
            <label>昵称</label>
            <AppleInput
              v-model="editForm.nickname"
              placeholder="输入昵称"
              size="small"
              @blur="validateField('nickname')"
              :error="errors.nickname"
            />
            <div v-if="errors.nickname" class="field-error">{{ errors.nickname }}</div>
          </div>

          <div class="form-group" :class="{ 'has-error': errors.bio }">
            <label>个人简介</label>
            <AppleTextarea
              v-model="editForm.bio"
              placeholder="介绍一下自己吧"
              :rows="4"
              size="small"
              @blur="validateField('bio')"
              :error="errors.bio"
              maxlength="200"
              showWordCount
            />
            <div v-if="errors.bio" class="field-error">{{ errors.bio }}</div>
          </div>

          <div class="form-group" :class="{ 'has-error': errors.email }">
            <label>邮箱</label>
            <AppleInput
              v-model="editForm.email"
              placeholder="邮箱地址"
              type="email"
              size="small"
              @blur="validateField('email')"
              :error="errors.email"
            />
            <div v-if="errors.email" class="field-error">{{ errors.email }}</div>
          </div>

          <div class="form-actions">
            <AppleButton
              @click="saveProfile"
              type="primary"
              size="small"
              :loading="saving"
            >
              保存
            </AppleButton>
            <AppleButton
              @click="cancelEdit"
              type="secondary"
              size="small"
            >
              取消
            </AppleButton>
          </div>
        </div>
      </AppleCard>

      <!-- 统计信息 -->
      <div class="stats-section">
        <AppleCard class="stats-card">
          <h3>统计信息</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ userStats.articleCount || 0 }}</div>
              <div class="stat-label">文章</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userStats.friendCount || 0 }}</div>
              <div class="stat-label">好友</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userStats.groupCount || 0 }}</div>
              <div class="stat-label">群组</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userStats.loginCount || 0 }}</div>
              <div class="stat-label">登录次数</div>
            </div>
          </div>
        </AppleCard>
      </div>

      <!-- 最近活动 -->
      <AppleCard class="activity-card">
        <template #header>
          <h3>最近活动</h3>
        </template>

        <div class="activity-list">
          <div
            v-for="activity in recentActivities"
            :key="activity.id"
            class="activity-item"
          >
            <div class="activity-icon">{{ getActivityIcon(activity.type) }}</div>
            <div class="activity-content">
              <div class="activity-title">{{ activity.title }}</div>
              <div class="activity-time">{{ formatDate(activity.createdAt) }}</div>
            </div>
          </div>

          <div v-if="recentActivities.length === 0" class="no-activity">
            暂无活动记录
          </div>
        </div>
      </AppleCard>
    </div>
  </div>
</template>

<script setup>
import appleMessage from '@/utils/appleMessage';
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { View } from '@element-plus/icons-vue';
import AppleCard from '@/components/common/AppleCard.vue';
import AppleButton from '@/components/common/AppleButton.vue';
import AppleInput from '@/components/common/AppleInput.vue';
import AppleTextarea from '@/components/common/AppleTextarea.vue';
import api from '@/api';

const authStore = useAuthStore();
const notificationStore = useNotificationStore();

// 响应式数据
const loading = ref(true);
const error = ref(null);
const editMode = ref(false);
const saving = ref(false);
const userStats = ref({});
const recentActivities = ref([]);

// 编辑表单
const editForm = ref({
  username: '',
  nickname: '',
  bio: '',
  email: ''
});

// 表单验证错误
const errors = ref({
  nickname: '',
  bio: '',
  email: ''
});

// 计算属性
const currentUser = computed(() => authStore.currentUser);

// 方法
const loadUserData = async () => {
  try {
    loading.value = true;
    error.value = null;

    // 当前用户信息已从 authStore 获取
    if (currentUser.value) {
      // 获取用户统计信息
      try {
        const statsResponse = await api.user.getUserStats(currentUser.value.id);
        if (statsResponse.code === 0) {
          userStats.value = statsResponse.data || {};
        }
      } catch (err) {
        console.warn('获取用户统计失败:', err);
        userStats.value = {
          articleCount: 0,
          friendCount: 0,
          groupCount: 0,
          loginCount: 0
        };
      }

      // 获取最近活动
      try {
        const activityResponse = await api.user.getUserActivities(currentUser.value.id);
        if (activityResponse.code === 0) {
          recentActivities.value = activityResponse.data || [];
        }
      } catch (err) {
        console.warn('获取最近活动失败:', err);
        recentActivities.value = [];
      }

      // 初始化编辑表单
      resetEditForm();
    }
  } catch (err) {
    console.error('加载用户数据失败:', err);
    error.value = '加载用户数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const resetEditForm = () => {
  if (currentUser.value) {
    editForm.value = {
      username: currentUser.value.username || '',
      nickname: currentUser.value.nickname || '',
      bio: currentUser.value.bio || '',
      email: currentUser.value.email || ''
    };
    // 清空错误信息
    errors.value = {
      nickname: '',
      bio: '',
      email: ''
    };
  }
};

// 验证单个字段
const validateField = (field) => {
  switch (field) {
    case 'nickname':
      validateNickname();
      break;
    case 'bio':
      validateBio();
      break;
    case 'email':
      validateEmail();
      break;
  }
};

// 验证昵称
const validateNickname = () => {
  const nickname = editForm.value.nickname?.trim();
  if (nickname && nickname.length > 20) {
    errors.value.nickname = '昵称不能超过20个字符';
  } else if (nickname && nickname.length < 2) {
    errors.value.nickname = '昵称至少需要2个字符';
  } else {
    errors.value.nickname = '';
  }
};

// 验证个人简介
const validateBio = () => {
  const bio = editForm.value.bio?.trim();
  if (bio && bio.length > 200) {
    errors.value.bio = '个人简介不能超过200个字符';
  } else {
    errors.value.bio = '';
  }
};

// 验证邮箱
const validateEmail = () => {
  const email = editForm.value.email?.trim();
  if (email && !isValidEmail(email)) {
    errors.value.email = '请输入有效的邮箱地址';
  } else {
    errors.value.email = '';
  }
};

// 验证整个表单
const validateForm = () => {
  validateNickname();
  validateBio();
  validateEmail();

  return !errors.value.nickname && !errors.value.bio && !errors.value.email;
};

// 邮箱格式验证
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

const handleAvatarChange = () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async (event) => {
    const file = event.target.files[0];
    if (file) {
      // 验证文件类型和大小
      if (!file.type.startsWith('image/')) {
        appleMessage.error('请选择图片文件');
        return;
      }

      if (file.size > 5 * 1024 * 1024) { // 5MB限制
        appleMessage.error('图片大小不能超过5MB');
        return;
      }

      try {
        await uploadAvatar(file);
      } catch (error) {
        console.error('头像上传失败:', error);
        appleMessage.error('头像上传失败: ' + error.message);
      }
    }
  };
  input.click();
};

const uploadAvatar = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.user.uploadAvatar(formData);

  if (response.code === 0 && response.data) {
    // 更新用户头像URL
    const updatedUser = {
      ...currentUser.value,
      avatar: response.data.avatar
    };
    authStore.setCurrentUser(updatedUser);

    appleMessage.success('头像更新成功');
  } else {
    throw new Error(response.message || '头像上传失败');
  }
};

const saveProfile = async () => {
  try {
    // 验证表单
    if (!validateForm()) {
      appleMessage.error('请修正表单错误后再提交');
      return;
    }

    saving.value = true;

    const response = await api.user.updateProfile({
      nickname: editForm.value.nickname?.trim() || null,
      bio: editForm.value.bio?.trim() || null,
      email: editForm.value.email?.trim() || null
    });

    if (response.code === 0) {
      // 更新本地用户信息
      const updatedUser = {
        ...currentUser.value,
        nickname: editForm.value.nickname?.trim(),
        bio: editForm.value.bio?.trim(),
        email: editForm.value.email?.trim()
      };
      authStore.setCurrentUser(updatedUser);

      editMode.value = false;
      appleMessage.success('个人资料更新成功');

      // 重新加载用户数据以获取最新信息
      await loadUserData();
    } else {
      throw new Error(response.message || '更新失败');
    }
  } catch (err) {
    console.error('保存个人资料失败:', err);
    appleMessage.error('保存失败: ' + err.message);
  } finally {
    saving.value = false;
  }
};

const cancelEdit = () => {
  resetEditForm();
  editMode.value = false;
};

const getUserLevelDisplay = (level) => {
  const levels = {
    1: '普通用户',
    2: '活跃用户',
    3: '资深用户',
    4: '版主',
    5: '管理员'
  };
  return levels[level] || '未知';
};

const formatDate = (dateString) => {
  if (!dateString) return '未知';
  try {
    return new Date(dateString).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  } catch {
    return '未知';
  }
};

const getActivityIcon = (type) => {
  const icons = {
    'article': '📝',
    'login': '👋',
    'comment': '💬',
    'group': '👥',
    'friend': '🤝'
  };
  return icons[type] || '📌';
};

// 生命周期
onMounted(() => {
  loadUserData();
});
</script>

<style scoped>
.user-profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.error-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.error-card {
  max-width: 400px;
  width: 100%;
}

.error-content {
  text-align: center;
  padding: 20px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-title {
  margin: 0 0 8px 0;
  color: var(--apple-text-primary);
}

.error-message {
  color: var(--apple-text-secondary);
  margin: 0 0 20px 0;
}

.user-info-card {
  margin-bottom: 24px;
}

.user-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 24px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--apple-bg-quaternary);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-change-btn {
  font-size: 12px;
}

.user-basic-info {
  flex: 1;
}

.username {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.nickname {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: var(--apple-text-secondary);
  font-weight: 500;
}

.bio {
  margin: 0 0 16px 0;
  color: var(--apple-text-secondary);
  line-height: 1.5;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-level,
.join-time {
  font-size: 14px;
  color: var(--apple-text-tertiary);
}

.level-history-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  font-size: 12px;
  color: var(--apple-blue);
  text-decoration: none;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.level-history-link:hover {
  background-color: var(--apple-hover);
  color: var(--apple-blue-hover);
}

.level-history-link .el-icon {
  font-size: 12px;
}

.user-actions {
  display: flex;
  align-items: flex-start;
}

.edit-card {
  margin-bottom: 24px;
}

.edit-form {
  padding: 20px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--apple-text-primary);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.stats-section {
  margin-bottom: 24px;
}

.stats-card h3 {
  margin: 0 0 20px 0;
  color: var(--apple-text-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--apple-blue);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.activity-card h3 {
  margin: 0 0 20px 0;
  color: var(--apple-text-primary);
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--apple-bg-quaternary);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
}

.activity-content {
  flex: 1;
}

.activity-title {
  color: var(--apple-text-primary);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

.no-activity {
  text-align: center;
  color: var(--apple-text-tertiary);
  padding: 40px 0;
}

/* 表单验证样式 */
.form-group.has-error .apple-textarea-input,
.form-group.has-error .apple-input {
  border-color: var(--apple-red);
}

.field-error {
  font-size: 12px;
  color: var(--apple-red);
  margin-top: 4px;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-profile-container {
    padding: 16px;
  }

  .user-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 20px;
  }

  .username {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
