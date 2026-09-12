<template>
  <div class="forget-container">
    <div class="forget-background">
      <div class="forget-card apple-card apple-fade-in">
        <div class="forget-header">
          <div class="logo">
            <div class="logo-icon">🔐</div>
            <h1 class="logo-title">找回密码</h1>
            <p class="logo-subtitle">重置您的账户密码</p>
          </div>
        </div>

        <!-- 步骤指示器 -->
        <div class="step-indicator">
          <div 
            class="step" 
            :class="{ active: currentStep >= 1, completed: currentStep > 1 }"
          >
            <div class="step-number">1</div>
            <span class="step-text">验证身份</span>
          </div>
          <div class="step-line" :class="{ completed: currentStep > 1 }"></div>
          <div 
            class="step" 
            :class="{ active: currentStep >= 2, completed: currentStep > 2 }"
          >
            <div class="step-number">2</div>
            <span class="step-text">重置密码</span>
          </div>
        </div>

        <!-- 步骤1：发送重置邮件 -->
        <div v-if="currentStep === 1" class="step-content">
          <form @submit.prevent="sendResetEmail" class="forget-form">
            <div class="form-group">
              <label class="apple-label" for="email">邮箱地址</label>
              <input
                id="email"
                v-model="formData.email"
                type="email"
                class="apple-input"
                placeholder="请输入注册时的邮箱地址"
                required
                :disabled="loading"
              />
            </div>

            <button
              type="submit"
              class="apple-button apple-button-primary verify-button"
              :disabled="loading"
            >
              <span v-if="loading" class="apple-loading"></span>
              <span v-else>发送重置邮件</span>
            </button>
          </form>
        </div>

        <!-- 步骤2：重置密码 -->
        <div v-if="currentStep === 2" class="step-content">
          <form @submit.prevent="resetPassword" class="forget-form">
            <div class="form-group">
              <label class="apple-label" for="resetToken">重置令牌</label>
              <input
                id="resetToken"
                v-model="formData.resetToken"
                type="text"
                class="apple-input"
                placeholder="请输入发送到邮箱的重置令牌"
                required
                :disabled="loading"
              />
            </div>

            <div class="form-group">
              <label class="apple-label" for="newPassword">新密码</label>
              <input
                id="newPassword"
                v-model="formData.newPassword"
                type="password"
                class="apple-input"
                placeholder="请输入新密码"
                required
                :disabled="loading"
                minlength="6"
              />
              <div class="password-strength" v-if="formData.newPassword">
                <div class="strength-bar">
                  <div
                    class="strength-fill"
                    :class="passwordStrength.class"
                    :style="{ width: passwordStrength.percentage + '%' }"
                  ></div>
                </div>
                <span class="strength-text">{{ passwordStrength.text }}</span>
              </div>
            </div>

            <div class="form-group">
              <label class="apple-label" for="confirmPassword">确认密码</label>
              <input
                id="confirmPassword"
                v-model="formData.confirmPassword"
                type="password"
                class="apple-input"
                placeholder="请再次输入新密码"
                required
                :disabled="loading"
                minlength="6"
              />
              <div class="password-match" v-if="formData.confirmPassword">
                <span :class="{ match: passwordsMatch, mismatch: !passwordsMatch }">
                  {{ passwordsMatch ? '✓ 密码匹配' : '✗ 密码不匹配' }}
                </span>
              </div>
            </div>

            <div class="button-group">
              <button
                type="button"
                class="apple-button apple-button-outline"
                @click="goBack"
                :disabled="loading"
              >
                返回上一步
              </button>

              <button
                type="submit"
                class="apple-button apple-button-primary"
                :disabled="loading || !canSubmit"
              >
                <span v-if="loading" class="apple-loading"></span>
                <span v-else>重置密码</span>
              </button>
            </div>
          </form>
        </div>

        <!-- 成功提示 -->
        <div v-if="currentStep === 3" class="success-content">
          <div class="success-icon">✅</div>
          <h2 class="success-title">密码重置成功！</h2>
          <p class="success-message">您的密码已经成功重置，请使用新密码登录。</p>
          <button
            class="apple-button apple-button-primary"
            @click="goToLogin"
          >
            立即登录
          </button>
        </div>

        <div class="apple-divider"></div>

        <div class="back-to-login">
          <router-link to="/login" class="back-link">
            ← 返回登录页面
          </router-link>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message apple-slide-in">
      <div class="error-icon">⚠️</div>
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { instance as axiosInstance } from '@/api/axiosInstance'

const router = useRouter()

// 响应式数据
const currentStep = ref(1)
const loading = ref(false)
const error = ref('')

const formData = ref({
  email: '',
  newPassword: '',
  confirmPassword: '',
  resetToken: ''
})

// 计算属性
const passwordsMatch = computed(() => {
  return formData.value.newPassword && 
         formData.value.confirmPassword && 
         formData.value.newPassword === formData.value.confirmPassword
})

const passwordStrength = computed(() => {
  const password = formData.value.newPassword
  if (!password) return { class: '', percentage: 0, text: '' }
  
  let score = 0
  let feedback = []
  
  if (password.length >= 8) score += 1
  if (/[a-z]/.test(password)) score += 1
  if (/[A-Z]/.test(password)) score += 1
  if (/[0-9]/.test(password)) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  
  if (score <= 1) {
    return { class: 'weak', percentage: 20, text: '密码强度：弱' }
  } else if (score <= 3) {
    return { class: 'medium', percentage: 60, text: '密码强度：中等' }
  } else {
    return { class: 'strong', percentage: 100, text: '密码强度：强' }
  }
})

const canSubmit = computed(() => {
  return formData.value.newPassword &&
         formData.value.confirmPassword &&
         formData.value.resetToken &&
         passwordsMatch.value &&
         passwordStrength.value.percentage >= 60
})

// 方法

const sendResetEmail = async () => {
  if (!formData.value.email) {
    error.value = '请输入邮箱地址'
    return
  }

  try {
    loading.value = true
    error.value = ''

    // 调用后端发送重置邮件接口
    const response = await api.auth.forgotPassword({
      email: formData.value.email
    })

    if (response.code === 0) {
      ElMessage.success('重置邮件已发送到您的邮箱，请查收')
      currentStep.value = 2
    } else {
      throw new Error(response.message || '发送重置邮件失败')
    }

  } catch (err) {
    console.error('发送重置邮件失败:', err)
    error.value = err.message || '发送重置邮件失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const resetPassword = async () => {
  if (!canSubmit.value) {
    error.value = '请检查密码输入'
    return
  }

  try {
    loading.value = true
    error.value = ''

    const response = await api.auth.resetPassword({
      email: formData.value.email,
      token: formData.value.resetToken,
      newPassword: formData.value.newPassword
    })

    if (response.code === 0) {
      ElMessage.success('密码重置成功')
      currentStep.value = 3
    } else {
      throw new Error(response.message || '密码重置失败')
    }

  } catch (err) {
    console.error('密码重置失败:', err)
    error.value = err.message || '密码重置失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  currentStep.value = 1
}

const goToLogin = () => {
  router.push('/login')
}

// 生命周期
onMounted(() => {
  // 页面加载时的初始化逻辑
})
</script>

<style scoped>
.forget-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--apple-accent-gradient);
  padding: var(--apple-spacing-md);
}

.forget-background {
  width: 100%;
  max-width: 500px;
}

.forget-card {
  padding: var(--apple-spacing-xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.forget-header {
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

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--apple-spacing-xl);
  gap: var(--apple-spacing-md);
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--apple-spacing-xs);
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--apple-gray);
  color: var(--apple-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.3s ease;
}

.step.active .step-number {
  background: var(--apple-blue);
}

.step.completed .step-number {
  background: var(--apple-green);
}

.step-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  font-weight: 500;
}

.step.active .step-text {
  color: var(--apple-blue);
}

.step.completed .step-text {
  color: var(--apple-green);
}

.step-line {
  width: 60px;
  height: 2px;
  background: var(--apple-gray);
  transition: all 0.3s ease;
}

.step-line.completed {
  background: var(--apple-green);
}

.step-content {
  margin-bottom: var(--apple-spacing-xl);
}

.forget-form {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.apple-label {
  font-weight: 500;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-sm);
}

.apple-input {
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-md);
  transition: all 0.3s ease;
  background: var(--apple-white);
}

.apple-input:focus {
  outline: none;
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}


.password-strength {
  margin-top: var(--apple-spacing-xs);
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: var(--apple-gray);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: var(--apple-spacing-xs);
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
}

.strength-fill.weak {
  background: var(--apple-red);
}

.strength-fill.medium {
  background: var(--apple-yellow);
}

.strength-fill.strong {
  background: var(--apple-green);
}

.strength-text {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.password-match {
  margin-top: var(--apple-spacing-xs);
  font-size: var(--apple-font-sm);
}

.password-match .match {
  color: var(--apple-green);
}

.password-match .mismatch {
  color: var(--apple-red);
}

.verify-button {
  margin-top: var(--apple-spacing-md);
}

.button-group {
  display: flex;
  gap: var(--apple-spacing-md);
  margin-top: var(--apple-spacing-md);
}

.button-group .apple-button {
  flex: 1;
}

.success-content {
  text-align: center;
  padding: var(--apple-spacing-xl) 0;
}

.success-icon {
  font-size: 64px;
  margin-bottom: var(--apple-spacing-lg);
}

.success-title {
  font-size: var(--apple-font-title);
  font-weight: 700;
  color: var(--apple-text-primary);
  margin: 0 0 var(--apple-spacing-md) 0;
}

.success-message {
  font-size: var(--apple-font-md);
  color: var(--apple-text-secondary);
  margin: 0 0 var(--apple-spacing-xl) 0;
  line-height: 1.5;
}

.back-to-login {
  text-align: center;
  margin-top: var(--apple-spacing-lg);
}

.back-link {
  color: var(--apple-blue);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.back-link:hover {
  color: var(--apple-blue-dark);
}

.error-message {
  position: fixed;
  top: var(--apple-spacing-lg);
  right: var(--apple-spacing-lg);
  background: var(--apple-red);
  color: var(--apple-white);
  padding: var(--apple-spacing-md);
  border-radius: var(--apple-border-radius-md);
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  z-index: 1000;
}

.error-icon {
  font-size: var(--apple-font-lg);
}

@media (max-width: 768px) {
  .forget-container {
    padding: var(--apple-spacing-sm);
  }
  
  .forget-card {
    padding: var(--apple-spacing-lg);
  }
  
  .button-group {
    flex-direction: column;
  }
  
  .step-indicator {
    gap: var(--apple-spacing-sm);
  }
  
  .step-line {
    width: 40px;
  }
}
</style>
