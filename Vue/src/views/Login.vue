<template>
  <div class="login-container">
    <div class="login-background">
      <div class="login-card apple-card apple-fade-in">
        <div class="login-header">
          <div class="logo">
            <div class="logo-icon">💬</div>
            <h1 class="logo-title">Weeb IM</h1>
            <p class="logo-subtitle">连接世界，分享生活</p>
          </div>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label class="apple-label" for="username">用户名</label>
            <input
              id="username"
              v-model="loginForm.username"
              type="text"
              class="apple-input"
              placeholder="请输入用户名"
              required
              :disabled="loading"
            />
          </div>

          <div class="form-group">
            <label class="apple-label" for="password">密码</label>
            <input
              id="password"
              v-model="loginForm.password"
              type="password"
              class="apple-input"
              placeholder="请输入密码"
              required
              :disabled="loading"
            />
          </div>

          <div class="form-options">
            <label class="remember-me">
              <input
                v-model="loginForm.rememberMe"
                type="checkbox"
                class="apple-checkbox"
              />
              <span class="checkbox-text">记住我</span>
            </label>
            <router-link to="/forget" class="forget-link">忘记密码？</router-link>
          </div>

          <button
            type="submit"
            class="apple-button apple-button-primary login-button"
            :disabled="loading"
          >
            <span v-if="loading" class="apple-loading"></span>
            <span v-else>{{ loading ? '登录中...' : '登录' }}</span>
          </button>

          <div class="apple-divider"></div>

          <div class="register-link">
            还没有账号？
            <router-link to="/register" class="register-text">立即注册</router-link>
          </div>
        </form>

        <!-- 错误提示 -->
        <div v-if="error" class="error-message apple-slide-in">
          <div class="error-icon">⚠️</div>
          <span>{{ error }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const error = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 登录方法
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    error.value = '请填写完整的登录信息'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await api.auth.login({
      username: loginForm.username,
      password: loginForm.password
    })

    if (response.success) {
      // 保存token
      const token = response.token
      authStore.setToken(token)
      
      // 获取用户信息
      await authStore.fetchUserInfo()
      
      ElMessage.success('登录成功')
      
      // 跳转到目标页面或首页
      const redirectPath = route.query.redirect || '/'
      router.push(redirectPath)
    } else {
      error.value = response.message || '登录失败'
    }
  } catch (err) {
    console.error('登录错误:', err)
    error.value = err.response?.data?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
  padding: var(--apple-spacing-md);
}

.login-background {
  width: 100%;
  max-width: 400px;
}

.login-card {
  padding: var(--apple-spacing-xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: var(--apple-spacing-xl);
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--apple-spacing-sm);
}

.logo-icon {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-sm);
}

.logo-title {
  font-size: var(--apple-font-title);
  font-weight: 700;
  color: var(--apple-text-primary);
  margin: 0;
}

.logo-subtitle {
  font-size: var(--apple-font-md);
  color: var(--apple-text-tertiary);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--apple-spacing-sm);
}

.remember-me {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
  cursor: pointer;
}

.apple-checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--apple-blue);
}

.checkbox-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.forget-link {
  font-size: var(--apple-font-sm);
  color: var(--apple-blue);
  text-decoration: none;
}

.forget-link:hover {
  text-decoration: underline;
}

.login-button {
  margin-top: var(--apple-spacing-md);
  height: 48px;
  font-size: var(--apple-font-lg);
  font-weight: 600;
}

.register-link {
  text-align: center;
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.register-text {
  color: var(--apple-blue);
  font-weight: 500;
  text-decoration: none;
}

.register-text:hover {
  text-decoration: underline;
}

.error-message {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  margin-top: var(--apple-spacing-md);
  padding: var(--apple-spacing-md);
  background-color: rgba(255, 59, 48, 0.1);
  border: 1px solid rgba(255, 59, 48, 0.2);
  border-radius: var(--apple-radius-medium);
  color: var(--apple-red);
  font-size: var(--apple-font-sm);
}

.error-icon {
  font-size: var(--apple-font-md);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: var(--apple-spacing-sm);
  }
  
  .login-card {
    padding: var(--apple-spacing-lg);
  }
  
  .logo-title {
    font-size: var(--apple-font-xxl);
  }
  
  .logo-subtitle {
    font-size: var(--apple-font-sm);
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .login-card {
    background: rgba(28, 28, 30, 0.95);
  }
}
</style> 