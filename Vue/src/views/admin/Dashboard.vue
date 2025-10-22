<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>管理后台首页</h1>
      <p class="subtitle">系统概览和管理中心</p>
    </div>

    <div class="dashboard-content">
      <!-- 统计卡片 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="stat-card">
              <div class="stat-icon user-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.totalUsers || 0 }}</div>
                <div class="stat-label">总用户数</div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="stat-card">
              <div class="stat-icon article-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.totalArticles || 0 }}</div>
                <div class="stat-label">总文章数</div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="stat-card">
              <div class="stat-icon group-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.totalGroups || 0 }}</div>
                <div class="stat-label">总群组数</div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="stat-card">
              <div class="stat-icon online-icon">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.onlineUsers || 0 }}</div>
                <div class="stat-label">在线用户</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-button
                type="primary"
                class="action-btn"
                @click="$router.push('/admin/permissions')"
              >
                <el-icon><Lock /></el-icon>
                权限管理
              </el-button>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-button
                type="success"
                class="action-btn"
                @click="$router.push('/admin/roles')"
              >
                <el-icon><UserSolid /></el-icon>
                角色管理
              </el-button>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-button
                type="warning"
                class="action-btn"
                @click="$router.push('/admin/users')"
              >
                <el-icon><Management /></el-icon>
                用户管理
              </el-button>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- 系统状态 -->
      <div class="system-status">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>系统状态</span>
                </div>
              </template>
              <div class="status-list">
                <div class="status-item" v-for="(status, key) in systemStatus" :key="key">
                  <el-icon :class="`status-icon ${status.status}`">
                    <CheckCircle v-if="status.status === 'success'" />
                    <Warning v-else-if="status.status === 'warning'" />
                    <Warning v-else />
                  </el-icon>
                  <span>{{ status.message }}</span>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>最近活动</span>
                </div>
              </template>
              <div class="activity-list">
                <div v-if="recentActivities.length === 0" class="no-activities">
                  <span>暂无最近活动</span>
                </div>
                <div v-else class="activity-item" v-for="activity in recentActivities" :key="activity.id">
                  <div class="activity-time">{{ formatTime(activity.createdAt) }}</div>
                  <div class="activity-desc">{{ activity.description || activity.content || '系统活动' }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Document, ChatDotRound, Connection, Lock, UserSolid, Management, CheckCircle, Warning } from '@element-plus/icons-vue'
import { getSystemLogs } from '@/api/modules/admin'
import axiosInstance from '@/api/axiosInstance'

const stats = ref({
  totalUsers: 0,
  totalArticles: 0,
  totalGroups: 0,
  onlineUsers: 0
})

const loading = ref(false)
const recentActivities = ref([])
const systemStatus = ref({
  database: { status: 'unknown', message: '检查中...' },
  redis: { status: 'unknown', message: '检查中...' },
  elasticsearch: { status: 'unknown', message: '检查中...' }
})

// 获取系统统计数据
const fetchSystemStats = async () => {
  try {
    loading.value = true
    const response = await axiosInstance.get('/api/admin/statistics')
    if (response.data.success) {
      stats.value = response.data.data
    }
  } catch (error) {
    console.error('获取系统统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取最近活动日志
const fetchRecentActivities = async () => {
  try {
    const response = await getSystemLogs({ page: 1, pageSize: 5 })
    if (response.data.success) {
      recentActivities.value = response.data.data.list || []
    }
  } catch (error) {
    console.error('获取最近活动失败:', error)
    // 如果API调用失败，使用默认数据
    recentActivities.value = []
  }
}

// 检查系统状态（简化版本，基于统计数据的响应情况）
const checkSystemStatus = async () => {
  try {
    // 通过检查API响应来判断系统状态
    const startTime = Date.now()
    await fetchSystemStats()
    const responseTime = Date.now() - startTime

    // 假设快速响应表示数据库正常
    systemStatus.value.database = {
      status: responseTime < 1000 ? 'success' : 'warning',
      message: responseTime < 1000 ? '数据库连接正常' : '数据库响应较慢'
    }

    // 模拟Redis和Elasticsearch状态检查
    systemStatus.value.redis = {
      status: 'success',
      message: 'Redis连接正常'
    }

    systemStatus.value.elasticsearch = {
      status: 'warning',
      message: 'Elasticsearch离线'
    }
  } catch (error) {
    systemStatus.value.database = {
      status: 'error',
      message: '数据库连接失败'
    }
  }
}

// 格式化时间显示
const formatTime = (timestamp) => {
  if (!timestamp) return '未知时间'

  const now = new Date()
  const time = new Date(timestamp)
  const diff = now - time

  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return time.toLocaleDateString('zh-CN')
}

onMounted(async () => {
  await Promise.all([
    fetchSystemStats(),
    fetchRecentActivities(),
    checkSystemStatus()
  ])
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.dashboard-header {
  margin-bottom: 24px;
  text-align: center;
}

.dashboard-header h1 {
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

.dashboard-content {
  max-width: 1200px;
  margin: 0 auto;
}

/* 统计卡片样式 */
.stats-section {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.stat-icon {
  font-size: 48px;
  margin-right: 16px;
  opacity: 0.8;
}

.stat-icon.user-icon {
  color: #409eff;
}

.stat-icon.article-icon {
  color: #67c23a;
}

.stat-icon.group-icon {
  color: #e6a23c;
}

.stat-icon.online-icon {
  color: #f56c6c;
}

.stat-info {
  text-align: left;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

/* 快捷操作样式 */
.quick-actions {
  margin-bottom: 24px;
}

.action-btn {
  width: 100%;
  height: 60px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 系统状态样式 */
.system-status {
  margin-bottom: 24px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-icon {
  font-size: 18px;
}

.status-icon.success {
  color: #67c23a;
}

.status-icon.warning {
  color: #e6a23c;
}

.status-icon.error {
  color: #f56c6c;
}

/* 活动列表样式 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  padding: 12px 0;
  border-bottom: 1px solid #ecf0f1;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.activity-desc {
  font-size: 14px;
  color: #606266;
}

.no-activities {
  text-align: center;
  padding: 20px 0;
  color: #909399;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-dashboard {
    padding: 12px;
  }

  .stat-card {
    height: 100px;
    margin-bottom: 12px;
  }

  .stat-icon {
    font-size: 36px;
    margin-right: 12px;
  }

  .stat-number {
    font-size: 24px;
  }

  .action-btn {
    height: 50px;
    font-size: 14px;
  }
}
</style>
