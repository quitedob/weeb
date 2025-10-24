<template>
  <div class="system-monitor">
    <div class="page-header">
      <div class="header-content">
        <h1>系统监控</h1>
        <p class="subtitle">实时监控系统状态和性能指标</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button @click="toggleAutoRefresh">
          <el-icon><Timer /></el-icon>
          {{ autoRefresh ? '停止自动刷新' : '开启自动刷新' }}
        </el-button>
      </div>
    </div>

    <!-- 系统状态概览 -->
    <div class="overview-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="status-card">
            <div class="status-header">
              <h3>数据库</h3>
              <el-tag :type="getStatusType(monitoringData.database?.status)" size="small">
                {{ monitoringData.database?.status || '未知' }}
              </el-tag>
            </div>
            <div class="status-content">
              <div class="status-icon">
                <el-icon><DataBoard /></el-icon>
              </div>
              <div class="status-info">
                <div class="response-time">
                  响应时间: {{ monitoringData.database?.responseTime || 'N/A' }}
                </div>
                <div class="status-message">
                  {{ monitoringData.database?.message || '无数据' }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="status-card">
            <div class="status-header">
              <h3>Redis</h3>
              <el-tag :type="getStatusType(monitoringData.redis?.status)" size="small">
                {{ monitoringData.redis?.status || '未知' }}
              </el-tag>
            </div>
            <div class="status-content">
              <div class="status-icon">
                <el-icon><Coin /></el-icon>
              </div>
              <div class="status-info">
                <div class="response-time">
                  响应时间: {{ monitoringData.redis?.responseTime || 'N/A' }}
                </div>
                <div class="status-message">
                  {{ monitoringData.redis?.message || '无数据' }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="status-card">
            <div class="status-header">
              <h3>Elasticsearch</h3>
              <el-tag :type="getStatusType(monitoringData.elasticsearch?.status)" size="small">
                {{ monitoringData.elasticsearch?.status || '未知' }}
              </el-tag>
            </div>
            <div class="status-content">
              <div class="status-icon">
                <el-icon><Search /></el-icon>
              </div>
              <div class="status-info">
                <div class="response-time">
                  响应时间: {{ monitoringData.elasticsearch?.responseTime || 'N/A' }}
                </div>
                <div class="status-message">
                  {{ monitoringData.elasticsearch?.message || '无数据' }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="status-card">
            <div class="status-header">
              <h3>系统负载</h3>
              <el-tag type="success" size="small">正常</el-tag>
            </div>
            <div class="status-content">
              <div class="status-icon">
                <el-icon><Monitor /></el-icon>
              </div>
              <div class="status-info">
                <div class="cpu-usage">
                  CPU: {{ performanceData.system?.cpuUsage || 'N/A' }}
                </div>
                <div class="load-average">
                  负载: {{ performanceData.system?.loadAverage || 'N/A' }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 性能指标 -->
    <div class="performance-section">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>内存使用情况</span>
                <el-button size="small" @click="refreshPerformance">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </template>

            <div class="memory-usage">
              <div class="memory-progress">
                <div class="progress-header">
                  <span>内存使用率</span>
                  <span class="percentage">{{ performanceData.memory?.usagePercent || 0 }}%</span>
                </div>
                <el-progress
                  :percentage="performanceData.memory?.usagePercent || 0"
                  :color="getMemoryColor(performanceData.memory?.usagePercent || 0)"
                  :stroke-width="8"
                />
              </div>

              <div class="memory-details">
                <div class="memory-item">
                  <span class="label">总内存:</span>
                  <span class="value">{{ performanceData.memory?.total || 'N/A' }}</span>
                </div>
                <div class="memory-item">
                  <span class="label">已使用:</span>
                  <span class="value">{{ performanceData.memory?.used || 'N/A' }}</span>
                </div>
                <div class="memory-item">
                  <span class="label">可用:</span>
                  <span class="value">{{ performanceData.memory?.free || 'N/A' }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>在线用户统计</span>
                <el-button size="small" @click="refreshData">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </template>

            <div class="online-stats">
              <div class="stat-item current">
                <div class="stat-number">{{ onlineUsers.currentOnline || 0 }}</div>
                <div class="stat-label">当前在线</div>
              </div>
              <div class="stat-item peak">
                <div class="stat-number">{{ onlineUsers.peakToday || 0 }}</div>
                <div class="stat-label">今日峰值</div>
              </div>
              <div class="stat-item average">
                <div class="stat-number">{{ onlineUsers.averageSessionDuration || 'N/A' }}</div>
                <div class="stat-label">平均会话时长</div>
              </div>
            </div>

            <div class="online-chart">
              <div class="chart-placeholder">
                <el-icon><TrendCharts /></el-icon>
                <p>用户活跃度图表 (待实现)</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 系统活动 -->
    <div class="activity-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>最近系统活动</span>
            <div class="header-actions">
              <el-select v-model="activityTimeRange" size="small" @change="refreshData">
                <el-option label="最近5分钟" :value="5" />
                <el-option label="最近15分钟" :value="15" />
                <el-option label="最近1小时" :value="60" />
                <el-option label="最近6小时" :value="360" />
              </el-select>
              <el-button size="small" @click="refreshData">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </template>

        <div class="activity-content">
          <el-table :data="recentActivity" stripe>
            <el-table-column label="时间" width="180">
              <template #default="{ row }">
                <span>{{ formatDateTime(row.timestamp) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getActivityTypeColor(row.type)" size="small">
                  {{ row.type || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="用户" width="120">
              <template #default="{ row }">
                <span>{{ row.username || '系统' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="描述" prop="description" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-icon :color="row.success ? 'var(--apple-green)' : 'var(--apple-red)'">
                  <component :is="row.success ? 'Check' : 'Close'" />
                </el-icon>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!recentActivity.length" class="no-data">
            <el-empty description="暂无最近活动数据" />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 错误统计 -->
    <div class="errors-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>错误统计</span>
            <div class="header-actions">
              <el-select v-model="errorTimeRange" size="small" @change="refreshErrorData">
                <el-option label="最近1小时" :value="1" />
                <el-option label="最近6小时" :value="6" />
                <el-option label="最近24小时" :value="24" />
                <el-option label="最近7天" :value="168" />
              </el-select>
              <el-button size="small" @click="refreshErrorData">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </template>

        <div class="error-stats">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <div class="error-stat-item">
                <div class="error-count">{{ errorStats.totalErrors || 0 }}</div>
                <div class="error-label">总错误数</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="error-stat-item">
                <div class="error-count">{{ errorStats.uniqueErrors || 0 }}</div>
                <div class="error-label">不同错误类型</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="error-stat-item">
                <div class="error-count">{{ errorStats.errorRate || '0%' }}</div>
                <div class="error-label">错误率</div>
              </div>
            </el-col>
          </el-row>

          <div class="error-trend">
            <div class="chart-placeholder">
              <el-icon><TrendCharts /></el-icon>
              <p>错误趋势图表 (待实现)</p>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import {
  Refresh, Timer, DataBoard, Coin, Search, Monitor, Check, Close, TrendCharts
} from '@element-plus/icons-vue'
import { log } from '@/utils/logger'
import axiosInstance from '@/api/axiosInstance'

// 响应式数据
const loading = ref(false)
const autoRefresh = ref(true)
const refreshInterval = ref(null)
const activityTimeRange = ref(15)
const errorTimeRange = ref(24)

const monitoringData = reactive({
  database: {},
  redis: {},
  elasticsearch: {},
  recentActivity: [],
  onlineUsers: {}
})

const performanceData = reactive({
  memory: {},
  system: {}
})

const onlineUsers = reactive({
  currentOnline: 0,
  peakToday: 0,
  averageSessionDuration: '0 minutes'
})

const recentActivity = ref([])
const errorStats = reactive({
  totalErrors: 0,
  uniqueErrors: 0,
  errorRate: '0%'
})

// 方法
const fetchMonitoringData = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/monitor/realtime')

    if (response.data.code === 0) {
      const data = response.data.data
      Object.assign(monitoringData, data)

      if (data.onlineUsers) {
        Object.assign(onlineUsers, data.onlineUsers)
      }

      if (data.recentActivity) {
        recentActivity.value = data.recentActivity.recentLogs || []
      }
    }
  } catch (error) {
    log.error('获取监控数据失败:', error)
  }
}

const fetchPerformanceData = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/monitor/performance')

    if (response.data.code === 0) {
      Object.assign(performanceData, response.data.data)
    }
  } catch (error) {
    log.error('获取性能数据失败:', error)
  }
}

const fetchErrorData = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/monitor/errors', {
      params: { hours: errorTimeRange.value }
    })

    if (response.data.code === 0) {
      Object.assign(errorStats, response.data.data)
    }
  } catch (error) {
    log.error('获取错误统计失败:', error)
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchMonitoringData(),
      fetchPerformanceData(),
      fetchErrorData()
    ])
  } finally {
    loading.value = false
  }
}

const refreshPerformance = () => {
  fetchPerformanceData()
}

const refreshErrorData = () => {
  fetchErrorData()
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value

  if (autoRefresh.value) {
    startAutoRefresh()
    ElMessage.success('已开启自动刷新 (30秒间隔)')
  } else {
    stopAutoRefresh()
    ElMessage.info('已停止自动刷新')
  }
}

const startAutoRefresh = () => {
  refreshInterval.value = setInterval(() => {
    refreshData()
  }, 30000) // 30秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
    refreshInterval.value = null
  }
}

// 工具方法
const getStatusType = (status) => {
  switch (status) {
    case 'healthy': return 'success'
    case 'warning': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
}

const getMemoryColor = (percentage) => {
  if (percentage < 60) return 'var(--apple-green)'
  if (percentage < 80) return 'var(--apple-orange)'
  return 'var(--apple-red)'
}

const getActivityTypeColor = (type) => {
  switch (type) {
    case 'LOGIN': return 'success'
    case 'LOGOUT': return 'info'
    case 'ERROR': return 'danger'
    case 'CREATE': return 'primary'
    case 'UPDATE': return 'warning'
    default: return 'info'
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  refreshData()
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.system-monitor {
  padding: var(--apple-spacing-lg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--apple-spacing-lg);
}

.header-content h1 {
  margin: 0 0 var(--apple-spacing-sm) 0;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-xl);
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: var(--apple-text-secondary);
  font-size: var(--apple-font-md);
}

.header-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.overview-section,
.performance-section,
.activity-section,
.errors-section {
  margin-bottom: var(--apple-spacing-lg);
}

.status-card {
  height: 120px;
  transition: all 0.2s ease;
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-medium);
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--apple-spacing-sm);
}

.status-header h3 {
  margin: 0;
  font-size: var(--apple-font-lg);
  color: var(--apple-text-primary);
}

.status-content {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
}

.status-icon {
  font-size: 32px;
  color: var(--apple-text-secondary);
}

.status-info {
  flex: 1;
}

.response-time,
.status-message,
.cpu-usage,
.load-average {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-bottom: var(--apple-spacing-xs);
}

.status-message {
  color: var(--apple-text-tertiary);
}

.memory-usage {
  margin-bottom: var(--apple-spacing-md);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--apple-spacing-sm);
}

.percentage {
  font-weight: 600;
  color: var(--apple-text-primary);
}

.memory-details {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.memory-item {
  display: flex;
  justify-content: space-between;
  font-size: var(--apple-font-sm);
}

.memory-item .label {
  color: var(--apple-text-secondary);
}

.memory-item .value {
  color: var(--apple-text-primary);
  font-weight: 500;
}

.online-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: var(--apple-spacing-lg);
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-top: var(--apple-spacing-xs);
}

.stat-item.current .stat-number {
  color: var(--apple-green);
}

.stat-item.peak .stat-number {
  color: var(--apple-orange);
}

.stat-item.average .stat-number {
  color: var(--apple-blue);
}

.chart-placeholder {
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-tertiary);
  font-size: var(--apple-font-sm);
}

.chart-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
  align-items: center;
}

.activity-content {
  max-height: 400px;
  overflow-y: auto;
}

.no-data {
  padding: var(--apple-spacing-xl) 0;
}

.error-stats {
  margin-bottom: var(--apple-spacing-lg);
}

.error-stat-item {
  text-align: center;
  padding: var(--apple-spacing-md);
  background-color: var(--apple-bg-secondary);
  border-radius: var(--apple-radius-medium);
}

.error-count {
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-red);
  line-height: 1;
}

.error-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-top: var(--apple-spacing-xs);
}

.error-trend {
  height: 200px;
  margin-top: var(--apple-spacing-lg);
}

@media (max-width: 768px) {
  .system-monitor {
    padding: var(--apple-spacing-md);
  }

  .page-header {
    flex-direction: column;
    gap: var(--apple-spacing-md);
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .overview-section .el-col {
    margin-bottom: var(--apple-spacing-sm);
  }

  .status-content {
    flex-direction: column;
    text-align: center;
  }

  .online-stats {
    flex-direction: column;
    gap: var(--apple-spacing-md);
  }

  .memory-details {
    margin-top: var(--apple-spacing-md);
  }
}
</style>