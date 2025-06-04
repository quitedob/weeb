<template>
  <div class="settings-container">
    <h2>个人设置</h2>
    <div v-if="user" class="settings-form">
      <div class="form-item">
        <label for="username">用户名:</label>
        <input type="text" id="username" v-model="form.username" placeholder="请输入新用户名" />
      </div>
      <div class="form-item">
        <label for="avatar">头像 URL:</label>
        <input type="text" id="avatar" v-model="form.avatar" placeholder="请输入头像图片URL" />
      </div>
      <div class="avatar-preview-container">
        <label>头像预览:</label>
        <img :src="form.avatar || defaultAvatar" alt="avatar" class="avatar-preview" />
      </div>
      <button @click="updateProfile" class="save-btn">保存更改</button>
      <p v-if="message" :class="messageType">{{ message }}</p>
    </div>
    <div v-else>
      正在加载用户信息...
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '@/api'; // 引入api管理文件

const user = ref(null);
const form = ref({
  username: '',
  avatar: '',
});
const message = ref('');
const messageType = ref('success'); // 'success' 或 'error'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'; // 默认头像

// 组件挂载后获取用户信息
onMounted(async () => {
  try {
    const response = await api.getUserInfo();
    if (response.code === 200) {
      user.value = response.data;
      form.value.username = response.data.username;
      form.value.avatar = response.data.avatar;
    } else {
      showMessage('获取用户信息失败: ' + response.message, 'error');
    }
  } catch (error) {
    showMessage('网络请求失败', 'error');
  }
});

// 显示提示信息
const showMessage = (msg, type = 'success') => {
  message.value = msg;
  messageType.value = type;
  setTimeout(() => (message.value = ''), 3000);
};

// 更新用户信息
const updateProfile = async () => {
  if (!user.value) return;
  try {
    const response = await api.updateUserInfo(form.value);
    if (response.code === 200) {
      showMessage('更新成功！', 'success');
      // 可选：更新本地存储或状态管理中的用户信息
    } else {
      showMessage('更新失败: ' + response.message, 'error');
    }
  } catch (error) {
    showMessage('网络请求失败', 'error');
  }
};
</script>

<style scoped>
.settings-container {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}
h2 {
  margin-bottom: 20px;
}
.form-item {
  margin-bottom: 16px;
}
.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}
.form-item input {
  width: 100%;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  box-sizing: border-box;
}
.avatar-preview-container {
  margin-top: 10px;
}
.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 2px solid #eee;
  object-fit: cover;
}
.save-btn {
  padding: 10px 20px;
  border: none;
  background-color: #409eff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 20px;
}
.save-btn:hover {
  background-color: #66b1ff;
}
.message {
  margin-top: 15px;
  padding: 10px;
  border-radius: 4px;
}
.success {
  color: #67c23a;
  background-color: #f0f9eb;
}
.error {
  color: #f56c6c;
  background-color: #fef0f0;
}
</style>
