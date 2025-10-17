<template>
  <div class="reset-password-container">
    <div class="reset-password-card">
      <div class="header">
        <h2>重置密码</h2>
        <p class="subtitle">请输入您的新密码</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="reset-form"
      >
        <el-form-item prop="password">
          <template #label>
            <span class="form-label">新密码</span>
          </template>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入新密码"
            show-password
            @input="validatePasswordStrength"
          />
          <!-- 密码强度指示器 -->
          <div class="password-strength">
            <div class="strength-bar">
              <div
                class="strength-fill"
                :class="passwordStrengthClass"
                :style="{ width: passwordStrength + '%' }"
              ></div>
            </div>
            <span class="strength-text">{{ passwordStrengthText }}</span>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <template #label>
            <span class="form-label">确认密码</span>
          </template>
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 错误信息显示 -->
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        class="error-alert"
      />

      <!-- 成功信息显示 -->
      <el-alert
        v-if="successMessage"
        :title="successMessage"
        type="success"
        :closable="false"
        class="success-alert"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axiosInstance from '@/api/axiosInstance'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 密码强度相关
const passwordStrength = ref(0)
const passwordStrengthClass = ref('')
const passwordStrengthText = ref('')

const form = reactive({
  password: '',
  confirmPassword: ''
})

const rules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    { max: 32, message: '密码长度不能超过32位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 密码强度验证
const validatePasswordStrength = () => {
  const password = form.password
  let strength = 0
  let text = ''
  let className = ''

  if (password.length >= 8) strength += 25
  if (/[a-z]/.test(password)) strength += 25
  if (/[A-Z]/.test(password)) strength += 25
  if (/[0-9]/.test(password)) strength += 15
  if (/[^A-Za-z0-9]/.test(password)) strength += 10

  passwordStrength.value = Math.min(strength, 100)

  if (strength < 30) {
    text = '弱'
    className = 'weak'
  } else if (strength < 60) {
    text = '中等'
    className = 'medium'
  } else if (strength < 90) {
    text = '强'
    className = 'strong'
  } else {
    text = '很强'
    className = 'very-strong'
  }

  passwordStrengthText.value = text
  passwordStrengthClass.value = className
}

// 验证重置令牌
const validateResetToken = async () => {
  const token = route.query.token
  if (!token) {
    errorMessage.value = '重置令牌无效或缺失'
    return false
  }

  try {
    // 调用后端API验证令牌有效性
    const response = await axiosInstance.get('/api/validate-reset-token', {
      params: { token }
    })
    if (!response.data.success) {
      errorMessage.value = '重置令牌已过期或无效'
      return false
    }
    return true
  } catch (error) {
    errorMessage.value = '令牌验证失败，请稍后重试'
    return false
  }
}

// 提交重置密码请求
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    if (passwordStrength.value < 30) {
      ElMessage.warning('密码强度太弱，请选择更安全的密码')
      return
    }

    loading.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const token = route.query.token

    // 调用后端API执行密码重置
    const response = await axiosInstance.post('/api/password/execute-reset', {
      token,
      newPassword: form.password
    })

    if (response.data.success) {
      successMessage.value = '密码重置成功，正在跳转到登录页面...'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      errorMessage.value = response.data.message || '密码重置失败'
    }
  } catch (error) {
    if (error.response?.data?.message) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = '密码重置失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 验证重置令牌
  const isValid = await validateResetToken()
  if (!isValid) {
    // 令牌无效时可以选择重定向到忘记密码页面
    setTimeout(() => {
      router.push('/forget')
    }, 3000)
  }
})
</script>

<style scoped>
.reset-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.reset-password-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px;
  box-sizing: border-box;
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h2 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.reset-form {
  margin-top: 24px;
}

.form-label {
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 8px;
  display: block;
}

/* 密码强度指示器样式 */
.password-strength {
  margin-top: 8px;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: #ecf0f1;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak {
  background: #e74c3c;
}

.strength-fill.medium {
  background: #f39c12;
}

.strength-fill.strong {
  background: #27ae60;
}

.strength-fill.very-strong {
  background: #2ecc71;
}

.strength-text {
  font-size: 12px;
  color: #7f8c8d;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  margin-top: 16px;
}

.error-alert {
  margin-top: 20px;
}

.success-alert {
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .reset-password-card {
    padding: 24px;
    margin: 0 16px;
  }

  .header h2 {
    font-size: 20px;
  }
}
</style>
