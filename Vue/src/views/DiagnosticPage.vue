<template>
  <div class="diagnostic-page">
    <h1>系统诊断页面</h1>
    
    <div class="section">
      <h2>✅ Vue应用状态</h2>
      <p>如果你能看到这个页面，说明Vue应用已成功加载</p>
    </div>

    <div class="section">
      <h2>🔐 认证状态</h2>
      <p>登录状态: {{ authStore.isLoggedIn ? '已登录' : '未登录' }}</p>
      <p>Token: {{ authStore.accessToken ? '存在' : '不存在' }}</p>
      <p>当前用户: {{ authStore.currentUser?.username || '无' }}</p>
    </div>

    <div class="section">
      <h2>🌐 API配置</h2>
      <p>环境: {{ isDev ? '开发环境' : '生产环境' }}</p>
      <p>API Base URL: {{ apiBaseUrl }}</p>
    </div>

    <div class="section">
      <h2>💾 LocalStorage</h2>
      <p>jwt_token: {{ localStorage.getItem('jwt_token') ? '存在' : '不存在' }}</p>
      <p>currentUser: {{ localStorage.getItem('currentUser') ? '存在' : '不存在' }}</p>
    </div>

    <div class="section">
      <h2>🔧 操作</h2>
      <button @click="testApi">测试API连接</button>
      <button @click="clearStorage">清除存储</button>
      <button @click="goToLogin">前往登录</button>
    </div>

    <div v-if="apiTestResult" class="section">
      <h2>📡 API测试结果</h2>
      <pre>{{ apiTestResult }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import appleMessage from '@/utils/appleMessage'
import axios from 'axios'

const router = useRouter()
const authStore = useAuthStore()
const apiTestResult = ref(null)

const isDev = import.meta.env.DEV
const apiBaseUrl = import.meta.env.DEV ? '/' : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')

const testApi = async () => {
  try {
    apiTestResult.value = '测试中...'
    const response = await axios.get('/api/users/me')
    apiTestResult.value = JSON.stringify(response.data, null, 2)
  } catch (error) {
    apiTestResult.value = `错误: ${error.message}\n${JSON.stringify(error.response?.data, null, 2)}`
  }
}

const clearStorage = () => {
  localStorage.clear()
  sessionStorage.clear()
  appleMessage.success('存储已清除，请刷新页面')
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.diagnostic-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.section {
  background: white;
  padding: 20px;
  margin: 20px 0;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

h1 {
  color: #333;
}

h2 {
  color: #666;
  font-size: 18px;
  margin-bottom: 10px;
}

button {
  margin: 5px;
  padding: 10px 20px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

button:hover {
  background: #0051d5;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}
</style>
