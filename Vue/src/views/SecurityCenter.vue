<template>
  <div class="security-center">
    <div class="page-header">
      <h1>安全中心</h1>
      <p class="subtitle">管理您的账户安全和隐私设置</p>
    </div>

    <div class="security-content">
      <!-- 安全评分卡片 -->
      <div class="security-score-section">
        <el-card class="score-card">
          <div class="score-header">
            <h3>账户安全评分</h3>
            <el-button type="text" @click="refreshSecurityScore">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
          <div class="score-display">
            <div class="score-circle" :class="scoreLevelClass">
              <div class="score-number">{{ securityScore.score || 0 }}</div>
              <div class="score-label">{{ securityScore.level || '未知' }}</div>
            </div>
            <div class="score-suggestions">
              <h4>安全建议</h4>
              <ul>
                <li v-for="suggestion in securityScore.suggestions" :key="suggestion">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 安全设置选项卡 -->
      <el-tabs v-model="activeTab" class="security-tabs">
        <!-- 会话管理 -->
        <el-tab-pane label="会话管理" name="sessions">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>活跃会话</span>
                <el-button type="danger" @click="logoutAllSessions">
                  登出所有设备
                </el-button>
              </div>
            </template>

            <el-table :data="sessions" stripe style="width: 100%">
              <el-table-column prop="device" label="设备" width="200" />
              <el-table-column prop="ip" label="IP地址" width="150" />
              <el-table-column prop="location" label="位置" width="150" />
              <el-table-column prop="loginTime" label="登录时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.loginTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="lastActivity" label="最后活动" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.lastActivity) }}
                </template>
              </el-table-column>
              <el-table-column label="当前设备" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.isCurrent" type="success">当前</el-tag>
                  <el-tag v-else type="info">其他</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button
                    v-if="!row.isCurrent"
                    type="danger"
                    size="small"
                    @click="logoutSession(row.sessionId)"
                  >
                    登出
                  </el-button>
                  <span v-else class="current-device">当前设备</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 登录历史 -->
        <el-tab-pane label="登录历史" name="history">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>最近登录记录</span>
                <el-button type="text" @click="loadLoginHistory">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </template>

            <el-table :data="loginHistory" stripe style="width: 100%">
              <el-table-column prop="loginTime" label="登录时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.loginTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="ip" label="IP地址" width="150" />
              <el-table-column prop="device" label="设备" width="200" />
              <el-table-column prop="location" label="位置" width="150" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === '成功' ? 'success' : 'danger'">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-container">
              <el-pagination
                v-model:current-page="historyPagination.current"
                v-model:page-size="historyPagination.size"
                :total="historyPagination.total"
                layout="prev, pager, next"
                @current-change="loadLoginHistory"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <!-- 安全事件 -->
        <el-tab-pane label="安全事件" name="events">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>安全事件记录</span>
                <el-button type="text" @click="loadSecurityEvents">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </template>

            <el-table :data="securityEvents" stripe style="width: 100%">
              <el-table-column prop="eventTime" label="事件时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.eventTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="eventType" label="事件类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getEventType(row.eventType)">
                    {{ row.eventType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="ip" label="IP地址" width="150" />
              <el-table-column prop="device" label="设备" width="200" />
              <el-table-column prop="location" label="位置" width="150" />
              <el-table-column prop="description" label="描述" />
            </el-table>

            <div class="pagination-container">
              <el-pagination
                v-model:current-page="eventsPagination.current"
                v-model:page-size="eventsPagination.size"
                :total="eventsPagination.total"
                layout="prev, pager, next"
                @current-change="loadSecurityEvents"
              />
            </div>
          </el-card>
        </el-tab-pane>

        <!-- 两因素认证 -->
        <el-tab-pane label="两因素认证" name="2fa">
          <el-card>
            <div class="two-factor-section">
              <div class="two-factor-status">
                <h3>两因素认证 (2FA)</h3>
                <el-switch
                  v-model="twoFactorEnabled"
                  :active-value="true"
                  :inactive-value="false"
                  @change="toggleTwoFactor"
                />
                <span class="status-text">
                  {{ twoFactorEnabled ? '已启用' : '未启用' }}
                </span>
              </div>

              <div v-if="!twoFactorEnabled" class="enable-2fa">
                <p class="enable-tip">
                  启用两因素认证可以为您的账户提供额外的安全保护。
                  启用后，每次登录都需要输入验证码。
                </p>
                <el-button type="primary" @click="enableTwoFactor">
                  启用两因素认证
                </el-button>
              </div>

              <div v-else class="disable-2fa">
                <p class="disable-tip">
                  您已经启用了两因素认证。如果您丢失了设备，
                  请使用备用代码进行登录。
                </p>
                <el-button type="warning" @click="showDisableDialog = true">
                  禁用两因素认证
                </el-button>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <!-- 密码修改 -->
        <el-tab-pane label="密码修改" name="password">
          <el-card>
            <div class="password-section">
              <h3>修改密码</h3>

              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="120px"
                class="password-form"
              >
                <el-form-item label="当前密码" prop="currentPassword">
                  <el-input
                    v-model="passwordForm.currentPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
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

                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    show-password
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="changePassword" :loading="changingPassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-card>
        </el-tab-pane>

        <!-- 权限和角色 -->
        <el-tab-pane label="权限和角色" name="permissions">
          <el-card>
            <div class="permissions-section">
              <div class="section-header">
                <h3>我的权限</h3>
                <el-button type="text" @click="loadUserPermissions">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>

              <div class="permissions-list">
                <el-tag
                  v-for="permission in userPermissions"
                  :key="permission"
                  class="permission-tag"
                  type="success"
                >
                  {{ permission }}
                </el-tag>
              </div>

              <div class="section-header" style="margin-top: 30px;">
                <h3>我的角色</h3>
              </div>

              <div class="roles-list">
                <el-tag
                  v-for="role in userRoles"
                  :key="role"
                  class="role-tag"
                  type="primary"
                >
                  {{ role }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 禁用2FA确认对话框 -->
    <el-dialog
      v-model="showDisableDialog"
      title="禁用两因素认证"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="disable-2fa-dialog">
        <p>禁用两因素认证将降低账户安全性。您确定要继续吗？</p>
        <el-form ref="disableFormRef" :model="disableForm" label-width="80px">
          <el-form-item label="验证码" prop="verificationCode">
            <el-input
              v-model="disableForm.verificationCode"
              placeholder="请输入验证码"
              maxlength="6"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDisableDialog = false">取消</el-button>
          <el-button type="danger" @click="confirmDisable2FA">确定禁用</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 启用2FA对话框 -->
    <el-dialog
      v-model="showEnableDialog"
      title="启用两因素认证"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="enable-2fa-dialog">
        <div class="qr-code-section">
          <p class="step-title">第一步：扫描二维码</p>
          <div class="qr-code-placeholder">
            <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="QR Code" />
            <div v-else class="qr-loading">生成二维码中...</div>
          </div>
          <p class="qr-tip">使用Google Authenticator或其他认证器应用扫描此二维码</p>
        </div>

        <div class="backup-codes-section">
          <p class="step-title">第二步：保存备用代码</p>
          <div class="backup-codes">
            <div
              v-for="(code, index) in backupCodes"
              :key="index"
              class="backup-code"
            >
              {{ code }}
            </div>
          </div>
          <p class="backup-tip">请将这些备用代码保存在安全的地方。如果您丢失了设备，可以使用这些代码登录。</p>
        </div>

        <div class="verification-section">
          <p class="step-title">第三步：验证设置</p>
          <el-form ref="verifyFormRef" :model="verifyForm" label-width="80px">
            <el-form-item label="验证码" prop="verificationCode">
              <el-input
                v-model="verifyForm.verificationCode"
                placeholder="请输入验证码"
                maxlength="6"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEnableDialog = false">取消</el-button>
          <el-button type="primary" @click="verifyAndEnable2FA">完成设置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import axiosInstance from '@/api/axiosInstance'

// 响应式数据
const activeTab = ref('sessions')
const securityScore = ref({})
const sessions = ref([])
const loginHistory = ref([])
const securityEvents = ref([])
const userPermissions = ref([])
const userRoles = ref([])
const twoFactorEnabled = ref(false)
const showDisableDialog = ref(false)
const showEnableDialog = ref(false)
const qrCodeUrl = ref('')
const backupCodes = ref([])

const historyPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const eventsPagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const disableForm = reactive({
  verificationCode: ''
})

const verifyForm = reactive({
  verificationCode: ''
})

// 密码强度相关
const passwordStrength = ref(0)
const passwordStrengthClass = ref('')
const passwordStrengthText = ref('')

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    { max: 32, message: '密码长度不能超过32位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 获取安全评分
const loadSecurityScore = async () => {
  try {
    const response = await axiosInstance.get('/api/security/security-score')
    if (response.data.success) {
      securityScore.value = response.data.data
    }
  } catch (error) {
    console.error('获取安全评分失败:', error)
  }
}

// 获取会话列表
const loadSessions = async () => {
  try {
    const response = await axiosInstance.get('/api/security/sessions')
    if (response.data.success) {
      sessions.value = response.data.data
    }
  } catch (error) {
    console.error('获取会话列表失败:', error)
  }
}

// 获取登录历史
const loadLoginHistory = async () => {
  try {
    const params = {
      page: historyPagination.current,
      pageSize: historyPagination.size
    }
    const response = await axiosInstance.get('/api/security/login-history', { params })
    if (response.data.success) {
      loginHistory.value = response.data.data.list || []
      historyPagination.total = response.data.data.total || 0
    }
  } catch (error) {
    console.error('获取登录历史失败:', error)
  }
}

// 获取安全事件
const loadSecurityEvents = async () => {
  try {
    const params = {
      page: eventsPagination.current,
      pageSize: eventsPagination.size
    }
    const response = await axiosInstance.get('/api/security/security-events', { params })
    if (response.data.success) {
      securityEvents.value = response.data.data.list || []
      eventsPagination.total = response.data.data.total || 0
    }
  } catch (error) {
    console.error('获取安全事件失败:', error)
  }
}

// 获取用户权限和角色
const loadUserPermissions = async () => {
  try {
    const [permResponse, roleResponse] = await Promise.all([
      axiosInstance.get('/api/security/permissions'),
      axiosInstance.get('/api/security/roles')
    ])

    if (permResponse.data.success) {
      userPermissions.value = permResponse.data.data
    }

    if (roleResponse.data.success) {
      userRoles.value = roleResponse.data.data
    }
  } catch (error) {
    console.error('获取用户权限和角色失败:', error)
  }
}

// 登出会话
const logoutSession = async (sessionId) => {
  try {
    const response = await axiosInstance.post(`/api/security/sessions/${sessionId}/logout`)
    if (response.data.success) {
      ElMessage.success('会话已登出')
      loadSessions()
    } else {
      ElMessage.error('登出失败')
    }
  } catch (error) {
    ElMessage.error('登出失败')
  }
}

// 登出所有会话
const logoutAllSessions = async () => {
  ElMessageBox.confirm(
    '确定要登出所有设备吗？此操作将登出您在所有设备上的登录。',
    '登出确认',
    {
      confirmButtonText: '确定登出',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const response = await axiosInstance.post('/api/security/sessions/logout-all')
      if (response.data.success) {
        ElMessage.success('所有会话已登出')
        loadSessions()
      } else {
        ElMessage.error('登出失败')
      }
    } catch (error) {
      ElMessage.error('登出失败')
    }
  })
}

// 启用两因素认证
const enableTwoFactor = async () => {
  try {
    const response = await axiosInstance.post('/api/security/enable-2fa')
    if (response.data.success) {
      const data = response.data.data
      qrCodeUrl.value = data.qrCodeUrl
      backupCodes.value = data.backupCodes
      showEnableDialog.value = true
    } else {
      ElMessage.error('启用失败')
    }
  } catch (error) {
    ElMessage.error('启用失败')
  }
}

// 验证并启用2FA
const verifyAndEnable2FA = async () => {
  try {
    await verifyFormRef.value.validate()

    const response = await axiosInstance.post('/api/security/verify-2fa', {
      verificationCode: verifyForm.value.verificationCode
    })

    if (response.data.success) {
      ElMessage.success('两因素认证已启用')
      twoFactorEnabled.value = true
      showEnableDialog.value = false
      loadSecurityScore() // 刷新安全评分
    } else {
      ElMessage.error('验证失败')
    }
  } catch (error) {
    ElMessage.error('验证失败')
  }
}

// 禁用两因素认证
const confirmDisable2FA = async () => {
  try {
    await disableFormRef.value.validate()

    const response = await axiosInstance.post('/api/security/disable-2fa', {
      verificationCode: disableForm.value.verificationCode
    })

    if (response.data.success) {
      ElMessage.success('两因素认证已禁用')
      twoFactorEnabled.value = false
      showDisableDialog.value = false
      loadSecurityScore() // 刷新安全评分
    } else {
      ElMessage.error('禁用失败')
    }
  } catch (error) {
    ElMessage.error('禁用失败')
  }
}

// 修改密码
const changePassword = async () => {
  try {
    await passwordFormRef.value.validate()

    if (passwordStrength.value < 30) {
      ElMessage.warning('密码强度太弱，请选择更安全的密码')
      return
    }

    const response = await axiosInstance.post('/api/security/change-password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })

    if (response.data.success) {
      ElMessage.success('密码修改成功')
      // 清空表单
      Object.assign(passwordForm, {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
    } else {
      ElMessage.error('密码修改失败')
    }
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('密码修改失败')
    }
  }
}

// 密码强度验证
const validatePasswordStrength = () => {
  const password = passwordForm.newPassword
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

// 获取事件类型标签颜色
const getEventType = (eventType) => {
  const types = {
    '密码修改': 'success',
    '登录失败': 'danger',
    '登录成功': 'success',
    '权限变更': 'warning'
  }
  return types[eventType] || 'info'
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 刷新安全评分
const refreshSecurityScore = () => {
  loadSecurityScore()
  ElMessage.success('安全评分已刷新')
}

// 初始化数据
onMounted(() => {
  loadSecurityScore()
  loadSessions()
  loadLoginHistory()
  loadSecurityEvents()
  loadUserPermissions()
})
</script>

<style scoped>
.security-center {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
  text-align: center;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-header h1 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.security-content {
  max-width: 1200px;
  margin: 0 auto;
}

/* 安全评分样式 */
.security-score-section {
  margin-bottom: 24px;
}

.score-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.score-header h3 {
  margin: 0;
  color: white;
  font-size: 18px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 40px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.score-circle::before {
  content: '';
  position: absolute;
  top: 5px;
  left: 5px;
  right: 5px;
  bottom: 5px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.score-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
}

.score-suggestions {
  flex: 1;
}

.score-suggestions h4 {
  margin: 0 0 12px 0;
  color: white;
  font-size: 16px;
}

.score-suggestions ul {
  margin: 0;
  padding-left: 20px;
  color: rgba(255, 255, 255, 0.9);
}

.score-suggestions li {
  margin-bottom: 6px;
  font-size: 14px;
}

/* 标签样式 */
.security-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.current-device {
  color: #909399;
  font-size: 12px;
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

/* 两因素认证样式 */
.two-factor-section {
  max-width: 600px;
}

.two-factor-status {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.two-factor-status h3 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
}

.status-text {
  font-size: 14px;
  color: #606266;
}

.enable-tip, .disable-tip {
  color: #606266;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.enable-2fa, .disable-2fa {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

/* 权限和角色样式 */
.permissions-section {
  max-width: 800px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
}

.permissions-list, .roles-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-tag, .role-tag {
  margin-bottom: 8px;
}

/* 2FA对话框样式 */
.enable-2fa-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

.step-title {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 500;
}

.qr-code-placeholder {
  width: 200px;
  height: 200px;
  margin: 0 auto 16px;
  border: 2px dashed #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.qr-code-placeholder img {
  max-width: 100%;
  max-height: 100%;
}

.qr-loading {
  color: #909399;
  font-size: 14px;
}

.qr-tip {
  text-align: center;
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.backup-codes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.backup-code {
  padding: 8px 12px;
  background: #f0f9ff;
  border: 1px solid #e0f2fe;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  text-align: center;
  color: #0369a1;
}

.backup-tip {
  color: #606266;
  font-size: 12px;
  margin: 0;
  line-height: 1.4;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .security-center {
    padding: 12px;
  }

  .score-display {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .score-circle {
    width: 100px;
    height: 100px;
  }

  .score-number {
    font-size: 28px;
  }
}
</style>
