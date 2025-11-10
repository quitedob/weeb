<template>
  <div class="settings-container">
    <h2>个人设置</h2>
    <SimpleTabs v-model="activeTab">
      <!-- 基本信息设置 -->
      <div v-show="activeTab === 'profile'">
        <div v-if="user" class="settings-form">
          <div class="form-item">
            <label for="username">用户名:</label>
            <AppleInput
              id="username"
              v-model="form.username"
              placeholder="请输入新用户名"
              style="width: 300px;"
            />
          </div>
          <div class="form-item">
            <label for="nickname">昵称:</label>
            <AppleInput
              id="nickname"
              v-model="form.nickname"
              placeholder="请输入昵称"
              style="width: 300px;"
            />
          </div>
          <div class="form-item">
            <label for="bio">个人简介:</label>
            <AppleInput
              id="bio"
              v-model="form.bio"
              type="textarea"
              :rows="3"
              placeholder="介绍一下自己吧"
              style="width: 300px;"
            />
          </div>
            <AppleButton type="primary" @click="updateProfile" class="save-btn" :loading="loading">
            保存更改
          </AppleButton>
          <div v-if="message" :class="['message', messageType]">{{ message }}</div>
        </div>
        <div v-else class="loading-state">
          <div class="loading-text">正在加载用户信息...</div>
        </div>
      </div>

      <!-- 隐私设置 -->
      <div v-show="activeTab === 'privacy'">
        <div class="privacy-settings">
          <div class="setting-item">
            <div class="setting-info">
              <h4>在线状态可见性</h4>
              <p>控制其他用户是否能看到你的在线状态</p>
            </div>
            <AppleSwitch v-model="privacySettings.onlineVisible" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>接收私信</h4>
              <p>允许其他用户向你发送私信</p>
            </div>
            <AppleSwitch v-model="privacySettings.allowMessages" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>显示关注列表</h4>
              <p>让其他用户查看你关注的人</p>
            </div>
            <AppleSwitch v-model="privacySettings.showFollows" />
          </div>
          <AppleButton type="primary" @click="updatePrivacySettings" :loading="loading">
            保存隐私设置
          </AppleButton>
        </div>
      </div>

      <!-- 通知设置 -->
      <div v-show="activeTab === 'notifications'">
        <div class="notification-settings">
          <div class="setting-item">
            <div class="setting-info">
              <h4>新消息通知</h4>
              <p>收到新消息时通知我</p>
            </div>
            <AppleSwitch v-model="notificationSettings.newMessages" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>关注通知</h4>
              <p>有人关注我时通知我</p>
            </div>
            <AppleSwitch v-model="notificationSettings.follows" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>点赞通知</h4>
              <p>有人点赞我的内容时通知我</p>
            </div>
            <AppleSwitch v-model="notificationSettings.likes" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>评论通知</h4>
              <p>有人评论我的内容时通知我</p>
            </div>
            <AppleSwitch v-model="notificationSettings.comments" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h4>群组邀请</h4>
              <p>收到群组邀请时通知我</p>
            </div>
            <AppleSwitch v-model="notificationSettings.groupInvites" />
          </div>
          <AppleButton type="primary" @click="updateNotificationSettings" :loading="loading">
            保存通知设置
          </AppleButton>
        </div>
      </div>

      <!-- 账号安全 -->
      <div v-show="activeTab === 'security'">
        <div class="security-settings">
          <AppleCard class="section">
            <template #header>
              <h3>修改密码</h3>
            </template>
            <div class="form-item">
              <label>当前密码:</label>
              <AppleInput
                v-model="passwordForm.currentPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
                style="width: 300px;"
              />
            </div>
            <div class="form-item">
              <label>新密码:</label>
              <AppleInput
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
                style="width: 300px;"
              />
            </div>
            <div class="form-item">
              <label>确认新密码:</label>
              <AppleInput
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                style="width: 300px;"
              />
            </div>
            <AppleButton type="primary" @click="changePassword" :loading="loading">
              修改密码
            </AppleButton>
          </AppleCard>

          <AppleCard class="section">
            <template #header>
              <h3>邮箱设置</h3>
            </template>
            <div class="form-item">
              <label>当前邮箱:</label>
              <span class="current-email">{{ user?.user_email || '未设置' }}</span>
            </div>
            <div class="form-item">
              <label>新邮箱:</label>
              <AppleInput
                v-model="emailForm.newEmail"
                placeholder="请输入新邮箱地址"
                style="width: 300px;"
              />
            </div>
            <AppleButton type="primary" @click="changeEmail" :loading="loading">
              修改邮箱
            </AppleButton>
          </AppleCard>
        </div>
      </div>
    </SimpleTabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '@/api';
import { useAuthStore } from '@/stores/authStore';
import AppleButton from '@/components/common/AppleButton.vue';
import AppleInput from '@/components/common/AppleInput.vue';
import AppleSwitch from '@/components/common/AppleSwitch.vue';
import SimpleTabs from '@/components/common/SimpleTabs.vue';
import AppleCard from '@/components/common/AppleCard.vue';

const authStore = useAuthStore();

const user = ref(null);
const loading = ref(false);
const activeTab = ref('profile');
const message = ref('');
const messageType = ref('success');
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

// 基本信息表单
const form = ref({
  username: '',
  nickname: '',
  bio: '',
  avatar: '',
});

// 隐私设置
const privacySettings = ref({
  onlineVisible: true,
  allowMessages: true,
  showFollows: true,
});

// 通知设置
const notificationSettings = ref({
  newMessages: true,
  follows: true,
  likes: true,
  comments: true,
  groupInvites: true,
});

// 密码修改表单
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

// 邮箱修改表单
const emailForm = ref({
  newEmail: '',
});

onMounted(async () => {
  await loadUserInfo();
});

const loadUserInfo = async () => {
  try {
    let userInfo;
    if (authStore.currentUser) {
      userInfo = authStore.currentUser;
    } else {
      const response = await api.user.getUserInfo();
      if (response.code === 200 && response.data) {
        userInfo = response.data;
        authStore.setCurrentUser(userInfo);
      } else {
        showMessage('获取用户信息失败: ' + (response.message || 'Unknown error'), 'error');
        return;
      }
    }

    user.value = userInfo;
    form.value = {
      username: userInfo.username || '',
      nickname: userInfo.nickname || '',
      bio: userInfo.bio || '',
      avatar: userInfo.avatar || '',
    };
  } catch (error) {
    showMessage('网络请求失败: ' + (error.message || 'Unknown error'), 'error');
  }
};

const showMessage = (msg, type = 'success') => {
  message.value = msg;
  messageType.value = type;
  // 使用原生 alert 替代 ElMessage
  alert(msg);
  setTimeout(() => (message.value = ''), 3000);
};

const updateProfile = async () => {
  if (!form.value) return;
  loading.value = true;
  try {
    const payload = {
      username: form.value.username,
      nickname: form.value.nickname,
      bio: form.value.bio,
      avatar: form.value.avatar,
    };
    const response = await api.user.updateUserInfo(payload);
    if (response.code === 200 && response.data) {
      authStore.setCurrentUser(response.data);
      user.value = { ...response.data };
      showMessage('更新成功！', 'success');
    } else {
      showMessage('更新失败: ' + (response.message || 'Unknown error'), 'error');
    }
  } catch (error) {
    showMessage('网络请求失败: ' + (error.message || 'Unknown error'), 'error');
  } finally {
    loading.value = false;
  }
};

const updatePrivacySettings = async () => {
  loading.value = true;
  try {
    // 这里需要调用相应的API，暂时模拟
    await new Promise(resolve => setTimeout(resolve, 1000));
    showMessage('隐私设置已保存', 'success');
  } catch (error) {
    showMessage('保存失败: ' + error.message, 'error');
  } finally {
    loading.value = false;
  }
};

const updateNotificationSettings = async () => {
  loading.value = true;
  try {
    // 这里需要调用相应的API，暂时模拟
    await new Promise(resolve => setTimeout(resolve, 1000));
    showMessage('通知设置已保存', 'success');
  } catch (error) {
    showMessage('保存失败: ' + error.message, 'error');
  } finally {
    loading.value = false;
  }
};

const changePassword = async () => {
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword) {
    showMessage('请填写完整信息', 'error');
    return;
  }

  if (passwordForm.value.newPassword.length < 6) {
    showMessage('新密码长度至少6位', 'error');
    return;
  }

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showMessage('两次输入的密码不一致', 'error');
    return;
  }

  loading.value = true;
  try {
    // 这里需要调用相应的API，暂时模拟
    await new Promise(resolve => setTimeout(resolve, 1000));
    showMessage('密码修改成功', 'success');
    // 清空表单
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    };
  } catch (error) {
    showMessage('修改失败: ' + error.message, 'error');
  } finally {
    loading.value = false;
  }
};

const changeEmail = async () => {
  if (!emailForm.value.newEmail) {
    showMessage('请输入新邮箱地址', 'error');
    return;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(emailForm.value.newEmail)) {
    showMessage('请输入有效的邮箱地址', 'error');
    return;
  }

  loading.value = true;
  try {
    // 这里需要调用相应的API，暂时模拟
    await new Promise(resolve => setTimeout(resolve, 1000));
    showMessage('邮箱修改成功', 'success');
    emailForm.value.newEmail = '';
    await loadUserInfo(); // 重新加载用户信息
  } catch (error) {
    showMessage('修改失败: ' + error.message, 'error');
  } finally {
    loading.value = false;
  }
};

const onAvatarError = (e) => {
  e.target.src = defaultAvatar;
};
</script>

<style scoped>
.settings-container {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
  background: var(--apple-bg-secondary);
  min-height: 100vh;
}

h2 {
  margin-bottom: 24px;
  color: var(--apple-text-primary);
  font-size: 24px;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

h3 {
  margin: 0;
  color: var(--apple-text-primary);
  font-size: 16px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

h4 {
  margin: 0 0 4px 0;
  color: var(--apple-text-primary);
  font-size: 14px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.avatar-preview-container {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview-container label {
  margin-bottom: 0;
  font-weight: 500;
  color: var(--apple-text-secondary);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid var(--apple-bg-quaternary);
  object-fit: cover;
  background-color: var(--apple-bg-tertiary);
  transition: all 0.2s ease;
}

.avatar-preview:hover {
  border-color: var(--apple-blue);
  transform: scale(1.05);
}

.save-btn {
  margin-top: 24px;
}

.message {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  transition: all 0.2s ease;
}

.success {
  color: var(--apple-green);
  background-color: rgba(52, 199, 89, 0.1);
  border: 1px solid rgba(52, 199, 89, 0.2);
}

.error {
  color: var(--apple-red);
  background-color: rgba(255, 59, 48, 0.1);
  border: 1px solid rgba(255, 59, 48, 0.2);
}

/* 加载状态 */
.loading-state {
  padding: 40px;
  text-align: center;
}

.loading-text {
  color: var(--apple-text-tertiary);
  font-size: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 隐私设置和通知设置样式 */
.privacy-settings,
.notification-settings,
.security-settings {
  padding: 20px 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid var(--apple-bg-quaternary);
  transition: all 0.2s ease;
}

.setting-item:hover {
  background: var(--apple-bg-tertiary);
  margin: 0 -16px;
  padding: 20px 16px;
  border-radius: 8px;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info h4 {
  margin-bottom: 4px;
}

.setting-info p {
  margin: 0;
  color: var(--apple-text-tertiary);
  font-size: 13px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  line-height: 1.4;
}

/* 安全设置样式 */
.section {
  margin-bottom: 24px;
  padding: 24px;
  background-color: var(--apple-bg-primary);
  border-radius: 12px;
  border: 1px solid var(--apple-bg-quaternary);
  transition: all 0.2s ease;
}

.section:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.section:last-child {
  margin-bottom: 0;
}

.section h3 {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--apple-bg-quaternary);
}

.security-settings .form-item {
  margin-bottom: 20px;
}

.security-settings .form-item label {
  min-width: 100px;
  display: inline-block;
  margin-right: 16px;
  margin-bottom: 0;
  vertical-align: middle;
}

.current-email {
  color: var(--apple-text-primary);
  font-weight: 500;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .settings-container {
    padding: 16px;
  }

  h2 {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .setting-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px 0;
  }

  .setting-item:hover {
    margin: 0;
    padding: 16px 0;
    background: none;
  }

  .security-settings .form-item label {
    display: block;
    margin-bottom: 8px;
    margin-right: 0;
  }

  .section {
    padding: 20px;
    margin-bottom: 20px;
  }

  .avatar-preview {
    width: 60px;
    height: 60px;
  }
}
</style>