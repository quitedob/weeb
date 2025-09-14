<template>
  <div class="register-container">
    <div class="register-background">
      <div class="register-card apple-card apple-fade-in">
        <div class="register-header">
          <div class="logo">
            <div class="logo-icon">💬</div>
            <h1 class="logo-title">加入 Weeb IM</h1>
            <p class="logo-subtitle">创建您的账号，开始聊天之旅</p>
          </div>
        </div>

        <form @submit.prevent="handleRegister" class="register-form">
          <div class="form-row">
            <div class="form-group">
              <label class="apple-label" for="username">用户名</label>
              <input
                id="username"
                v-model="registerForm.username"
                type="text"
                class="apple-input"
                placeholder="请输入用户名"
                required
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="apple-label" for="email">邮箱</label>
              <input
                id="email"
                v-model="registerForm.email"
                type="email"
                class="apple-input"
                placeholder="请输入邮箱地址"
                required
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="apple-label" for="phone">手机号</label>
              <input
                id="phone"
                v-model="registerForm.phone"
                type="tel"
                class="apple-input"
                placeholder="请输入手机号"
                required
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="apple-label" for="password">密码</label>
              <input
                id="password"
                v-model="registerForm.password"
                type="password"
                class="apple-input"
                placeholder="请输入密码"
                required
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="apple-label" for="confirmPassword">确认密码</label>
              <input
                id="confirmPassword"
                v-model="registerForm.confirmPassword"
                type="password"
                class="apple-input"
                placeholder="请再次输入密码"
                required
                :disabled="loading"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="apple-label">性别</label>
              <div class="gender-options">
                <label class="gender-option">
                  <input
                    v-model="registerForm.gender"
                    type="radio"
                    value="male"
                    name="gender"
                    class="apple-radio"
                  />
                  <span class="radio-text">男</span>
                </label>
                <label class="gender-option">
                  <input
                    v-model="registerForm.gender"
                    type="radio"
                    value="female"
                    name="gender"
                    class="apple-radio"
                  />
                  <span class="radio-text">女</span>
                </label>
              </div>
            </div>
          </div>

          <div class="form-agreement">
            <label class="agreement-checkbox">
              <input
                v-model="registerForm.agreeTerms"
                type="checkbox"
                class="apple-checkbox"
                required
              />
              <span class="agreement-text">
                我已阅读并同意
                <a href="#" class="terms-link">服务条款</a>
                和
                <a href="#" class="terms-link">隐私政策</a>
              </span>
            </label>
          </div>

          <button
            type="submit"
            class="apple-button apple-button-primary register-button"
            :disabled="loading || !registerForm.agreeTerms"
          >
            <span v-if="loading" class="apple-loading"></span>
            <span v-else>{{ loading ? '注册中...' : '创建账号' }}</span>
          </button>

          <div class="apple-divider"></div>

          <div class="login-link">
            已有账号？
            <router-link to="/login" class="login-text">立即登录</router-link>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const error = ref('')

const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  gender: 'male',
  agreeTerms: false
})

// 注册方法
const handleRegister = async () => {
  // 表单验证
  if (!registerForm.username || !registerForm.email || !registerForm.phone || 
      !registerForm.password || !registerForm.confirmPassword) {
    error.value = '请填写完整的注册信息'
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }

  if (registerForm.password.length < 6) {
    error.value = '密码长度不能少于6位'
    return
  }

  if (!registerForm.agreeTerms) {
    error.value = '请同意服务条款和隐私政策'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await api.auth.register({
      username: registerForm.username,
      email: registerForm.email,
      phone: registerForm.phone,
      password: registerForm.password,
      gender: registerForm.gender
    })

    if (response.success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      error.value = response.message || '注册失败'
    }
  } catch (err) {
    console.error('注册错误:', err)
    error.value = err.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--apple-purple) 0%, var(--apple-pink) 100%);
  padding: var(--apple-spacing-md);
}

.register-background {
  width: 100%;
  max-width: 450px;
}

.register-card {
  padding: var(--apple-spacing-xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.register-header {
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

.register-form {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-md);
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.gender-options {
  display: flex;
  gap: var(--apple-spacing-lg);
  margin-top: var(--apple-spacing-xs);
}

.gender-option {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
  cursor: pointer;
}

.apple-radio {
  width: 16px;
  height: 16px;
  accent-color: var(--apple-blue);
}

.radio-text {
  font-size: var(--apple-font-md);
  color: var(--apple-text-secondary);
}

.form-agreement {
  margin-top: var(--apple-spacing-sm);
}

.agreement-checkbox {
  display: flex;
  align-items: flex-start;
  gap: var(--apple-spacing-xs);
  cursor: pointer;
}

.agreement-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  line-height: 1.4;
}

.terms-link {
  color: var(--apple-blue);
  text-decoration: none;
}

.terms-link:hover {
  text-decoration: underline;
}

.register-button {
  margin-top: var(--apple-spacing-md);
  height: 48px;
  font-size: var(--apple-font-lg);
  font-weight: 600;
}

.register-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.login-text {
  color: var(--apple-blue);
  font-weight: 500;
  text-decoration: none;
}

.login-text:hover {
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
  .register-container {
    padding: var(--apple-spacing-sm);
  }
  
  .register-card {
    padding: var(--apple-spacing-lg);
  }
  
  .logo-title {
    font-size: var(--apple-font-xxl);
  }
  
  .logo-subtitle {
    font-size: var(--apple-font-sm);
  }
  
  .gender-options {
    flex-direction: column;
    gap: var(--apple-spacing-sm);
  }
}

/* 暗色主题适配 */
@media (prefers-color-scheme: dark) {
  .register-card {
    background: rgba(28, 28, 30, 0.95);
  }
}
</style> 