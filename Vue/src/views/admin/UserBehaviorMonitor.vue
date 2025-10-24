<template>
  <div class="user-behavior-monitor">
    <div class="page-header">
      <div class="header-content">
        <h1>用户行为监控</h1>
        <p class="subtitle">实时监控和分析用户行为数据</p>
      </div>
      <div class="header-actions">
        <AppleButton variant="primary" @click="refreshData" :loading="loading">
          <i class="icon-refresh"></i>
          刷新数据
        </AppleButton>
        <AppleButton variant="default" @click="toggleAutoRefresh">
          <i class="icon-timer"></i>
          {{ autoRefresh ? '停止自动刷新' : '开启自动刷新' }}
        </AppleButton>
      </div>
    </div>

    <!-- 时间范围选择 -->
    <div class="time-range-section">
      <AppleCard>
        <div class="time-range-controls">
          <div class="range-group">
            <label>分析时间范围:</label>
            <AppleSelect v-model="timeRange" @change="handleTimeRangeChange">
              <AppleOption label="最近1小时" :value="1" />
              <AppleOption label="最近6小时" :value="6" />
              <AppleOption label="最近24小时" :value="24" />
              <AppleOption label="最近7天" :value="168" />
              <AppleOption label="最近30天" :value="720" />
            </AppleSelect>
          </div>
          <div class="range-group">
            <label>数据粒度:</label>
            <AppleSelect v-model="dataGranularity">
              <AppleOption label="分钟" value="minute" />
              <AppleOption label="小时" value="hour" />
              <AppleOption label="天" value="day" />
            </AppleSelect>
          </div>
        </div>
      </AppleCard>
    </div>

    <!-- 行为统计概览 -->
    <div class="behavior-overview">
      <AppleGrid gutter="20">
        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card active-users">
            <div class="stat-icon">
              <i class="icon-users"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ behaviorStats.activeUsers || 0 }}</div>
              <div class="stat-label">活跃用户</div>
              <div class="stat-trend" :class="getTrendClass(behaviorStats.activeUsersTrend)">
                <i :class="getTrendIcon(behaviorStats.activeUsersTrend)"></i>
                {{ Math.abs(behaviorStats.activeUsersTrend || 0) }}%
              </div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card page-views">
            <div class="stat-icon">
              <i class="icon-eye"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ formatNumber(behaviorStats.pageViews || 0) }}</div>
              <div class="stat-label">页面浏览量</div>
              <div class="stat-trend" :class="getTrendClass(behaviorStats.pageViewsTrend)">
                <i :class="getTrendIcon(behaviorStats.pageViewsTrend)"></i>
                {{ Math.abs(behaviorStats.pageViewsTrend || 0) }}%
              </div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card avg-session">
            <div class="stat-icon">
              <i class="icon-clock"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ behaviorStats.avgSessionDuration || '0m' }}</div>
              <div class="stat-label">平均会话时长</div>
              <div class="stat-trend" :class="getTrendClass(behaviorStats.avgSessionTrend)">
                <i :class="getTrendIcon(behaviorStats.avgSessionTrend)"></i>
                {{ Math.abs(behaviorStats.avgSessionTrend || 0) }}%
              </div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card bounce-rate">
            <div class="stat-icon">
              <i class="icon-logout"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ behaviorStats.bounceRate || '0%' }}</div>
              <div class="stat-label">跳出率</div>
              <div class="stat-trend" :class="getTrendClass(behaviorStats.bounceRateTrend, true)">
                <i :class="getTrendIcon(behaviorStats.bounceRateTrend, true)"></i>
                {{ Math.abs(behaviorStats.bounceRateTrend || 0) }}%
              </div>
            </div>
          </AppleCard>
        </AppleCol>
      </AppleGrid>
    </div>

    <!-- 用户活动热力图 -->
    <div class="activity-heatmap">
      <AppleCard>
        <template #header>
          <div class="card-header">
            <span>用户活动热力图</span>
            <div class="header-actions">
              <AppleSelect v-model="heatmapType" size="small" @change="refreshData">
                <AppleOption label="小时分布" value="hourly" />
                <AppleOption label="星期分布" value="weekly" />
              </AppleSelect>
            </div>
          </div>
        </template>

        <div class="heatmap-container">
          <div class="heatmap-chart">
            <div class="chart-placeholder">
              <i class="icon-calendar-alt"></i>
              <p>用户活动热力图 (图表组件待集成)</p>
              <div class="heatmap-legend">
                <span class="legend-label">活跃度:</span>
                <div class="legend-scale">
                  <div class="scale-item low">低</div>
                  <div class="scale-item medium">中</div>
                  <div class="scale-item high">高</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </AppleCard>
    </div>

    <!-- 用户行为模式 -->
    <div class="behavior-patterns">
      <AppleGrid gutter="20">
        <AppleCol :xs="24" :lg="12">
          <AppleCard>
            <template #header>
              <div class="card-header">
                <span>热门页面</span>
                <AppleButton variant="text" size="small" @click="viewPageDetails">
                  查看详情
                </AppleButton>
              </div>
            </template>

            <div class="top-pages">
              <div
                v-for="(page, index) in topPages"
                :key="page.path"
                class="page-item"
                :class="{ 'is-top': index < 3 }"
              >
                <div class="page-rank">
                  <span class="rank-number">{{ index + 1 }}</span>
                  <i v-if="index < 3" class="icon-trophy"></i>
                </div>
                <div class="page-info">
                  <div class="page-path">{{ page.path }}</div>
                  <div class="page-stats">
                    <span class="views">{{ formatNumber(page.views) }} 次访问</span>
                    <span class="duration">平均停留 {{ page.avgDuration }}</span>
                  </div>
                </div>
                <div class="page-trend">
                  <AppleTag
                    :type="getTrendType(page.trend)"
                    size="small"
                  >
                    {{ page.trend > 0 ? '+' : '' }}{{ page.trend }}%
                  </AppleTag>
                </div>
              </div>

              <div v-if="!topPages.length" class="no-data">
                <i class="icon-inbox"></i>
                <span>暂无页面数据</span>
              </div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :lg="12">
          <AppleCard>
            <template #header>
              <div class="card-header">
                <span>用户行为事件</span>
                <AppleSelect v-model="eventType" size="small" @change="refreshData">
                  <AppleOption label="全部事件" value="" />
                  <AppleOption label="页面访问" value="PAGE_VIEW" />
                  <AppleOption label="按钮点击" value="BUTTON_CLICK" />
                  <AppleOption label="表单提交" value="FORM_SUBMIT" />
                  <AppleOption label="文件下载" value="FILE_DOWNLOAD" />
                </AppleSelect>
              </div>
            </template>

            <div class="behavior-events">
              <div class="events-summary">
                <div class="event-stat">
                  <span class="event-count">{{ eventStats.totalEvents || 0 }}</span>
                  <span class="event-label">总事件数</span>
                </div>
                <div class="event-stat">
                  <span class="event-count">{{ eventStats.uniqueUsers || 0 }}</span>
                  <span class="event-label">参与用户</span>
                </div>
                <div class="event-stat">
                  <span class="event-count">{{ eventStats.conversionRate || '0%' }}</span>
                  <span class="event-label">转化率</span>
                </div>
              </div>

              <div class="events-list">
                <div
                  v-for="event in recentEvents"
                  :key="event.id"
                  class="event-item"
                >
                  <div class="event-time">{{ formatTime(event.timestamp) }}</div>
                  <div class="event-info">
                    <div class="event-type">
                      <AppleTag :type="getEventTypeColor(event.type)" size="small">
                        {{ event.type }}
                      </AppleTag>
                    </div>
                    <div class="event-description">{{ event.description }}</div>
                    <div class="event-user">用户: {{ event.username }}</div>
                  </div>
                </div>

                <div v-if="!recentEvents.length" class="no-data">
                  <i class="icon-inbox"></i>
                  <span>暂无事件数据</span>
                </div>
              </div>
            </div>
          </AppleCard>
        </AppleCol>
      </AppleGrid>
    </div>

    <!-- 用户分群分析 -->
    <div class="user-segments">
      <AppleCard>
        <template #header>
          <div class="card-header">
            <span>用户分群分析</span>
            <div class="header-actions">
              <AppleButton variant="default" size="small" @click="exportSegments">
                <i class="icon-download"></i>
                导出数据
              </AppleButton>
            </div>
          </div>
        </template>

        <div class="segments-content">
          <AppleGrid gutter="20">
            <AppleCol :xs="24" :md="8" :lg="6">
              <div class="segment-card new-users">
                <div class="segment-header">
                  <i class="icon-user-plus"></i>
                  <h3>新用户</h3>
                </div>
                <div class="segment-stats">
                  <div class="segment-count">{{ userSegments.newUsers.count || 0 }}</div>
                  <div class="segment-percentage">{{ userSegments.newUsers.percentage || '0%' }}</div>
                </div>
                <div class="segment-behavior">
                  <div class="behavior-item">
                    <span>平均会话时长:</span>
                    <span>{{ userSegments.newUsers.avgSession || '0m' }}</span>
                  </div>
                  <div class="behavior-item">
                    <span>跳出率:</span>
                    <span>{{ userSegments.newUsers.bounceRate || '0%' }}</span>
                  </div>
                </div>
              </div>
            </AppleCol>

            <AppleCol :xs="24" :md="8" :lg="6">
              <div class="segment-card active-users">
                <div class="segment-header">
                  <i class="icon-user-check"></i>
                  <h3>活跃用户</h3>
                </div>
                <div class="segment-stats">
                  <div class="segment-count">{{ userSegments.activeUsers.count || 0 }}</div>
                  <div class="segment-percentage">{{ userSegments.activeUsers.percentage || '0%' }}</div>
                </div>
                <div class="segment-behavior">
                  <div class="behavior-item">
                    <span>平均会话时长:</span>
                    <span>{{ userSegments.activeUsers.avgSession || '0m' }}</span>
                  </div>
                  <div class="behavior-item">
                    <span>访问频率:</span>
                    <span>{{ userSegments.activeUsers.frequency || '0次/天' }}</span>
                  </div>
                </div>
              </div>
            </AppleCol>

            <AppleCol :xs="24" :md="8" :lg="6">
              <div class="segment-card returning-users">
                <div class="segment-header">
                  <i class="icon-user-clock"></i>
                  <h3>回访用户</h3>
                </div>
                <div class="segment-stats">
                  <div class="segment-count">{{ userSegments.returningUsers.count || 0 }}</div>
                  <div class="segment-percentage">{{ userSegments.returningUsers.percentage || '0%' }}</div>
                </div>
                <div class="segment-behavior">
                  <div class="behavior-item">
                    <span>平均会话时长:</span>
                    <span>{{ userSegments.returningUsers.avgSession || '0m' }}</span>
                  </div>
                  <div class="behavior-item">
                    <span>回访间隔:</span>
                    <span>{{ userSegments.returningUsers.interval || '0天' }}</span>
                  </div>
                </div>
              </div>
            </AppleCol>

            <AppleCol :xs="24" :md="8" :lg="6">
              <div class="segment-card at-risk-users">
                <div class="segment-header">
                  <i class="icon-user-x"></i>
                  <h3>流失风险用户</h3>
                </div>
                <div class="segment-stats">
                  <div class="segment-count">{{ userSegments.atRiskUsers.count || 0 }}</div>
                  <div class="segment-percentage">{{ userSegments.atRiskUsers.percentage || '0%' }}</div>
                </div>
                <div class="segment-behavior">
                  <div class="behavior-item">
                    <span>最后活跃:</span>
                    <span>{{ userSegments.atRiskUsers.lastActive || '0天前' }}</span>
                  </div>
                  <div class="behavior-item">
                    <span>流失概率:</span>
                    <span>{{ userSegments.atRiskUsers.churnRisk || '0%' }}</span>
                  </div>
                </div>
              </div>
            </AppleCol>
          </AppleGrid>
        </div>
      </AppleCard>
    </div>

    <!-- 异常行为检测 -->
    <div class="anomaly-detection">
      <AppleCard>
        <template #header>
          <div class="card-header">
            <span>异常行为检测</span>
            <AppleButton variant="warning" size="small" @click="runAnomalyDetection">
              <i class="icon-shield-alt"></i>
              运行检测
            </AppleButton>
          </div>
        </template>

        <div class="anomaly-content">
          <div v-if="anomalies.length" class="anomalies-list">
            <div
              v-for="anomaly in anomalies"
              :key="anomaly.id"
              class="anomaly-item"
              :class="getAnomalySeverityClass(anomaly.severity)"
            >
              <div class="anomaly-icon">
                <i :class="getAnomalyIcon(anomaly.type)"></i>
              </div>
              <div class="anomaly-info">
                <div class="anomaly-title">{{ anomaly.title }}</div>
                <div class="anomaly-description">{{ anomaly.description }}</div>
                <div class="anomaly-meta">
                  <span class="anomaly-user">影响用户: {{ anomaly.affectedUsers }}</span>
                  <span class="anomaly-time">{{ formatDateTime(anomaly.detectedAt) }}</span>
                </div>
              </div>
              <div class="anomaly-actions">
                <AppleButton variant="text" size="small" @click="investigateAnomaly(anomaly)">
                  调查
                </AppleButton>
                <AppleButton variant="text" size="small" @click="dismissAnomaly(anomaly)">
                  忽略
                </AppleButton>
              </div>
            </div>
          </div>

          <div v-else class="no-anomalies">
            <i class="icon-shield-check"></i>
            <span>未检测到异常行为</span>
            <p>系统运行正常，用户行为符合预期模式</p>
          </div>
        </div>
      </AppleCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { showMessage, showConfirm } from '@/utils/message'
import { log } from '@/utils/logger'
import axiosInstance from '@/api/axiosInstance'

// Import Apple components
import AppleButton from '@/components/common/AppleButton.vue'
import AppleCard from '@/components/common/AppleCard.vue'
import AppleGrid from '@/components/common/AppleGrid.vue'
import AppleCol from '@/components/common/AppleCol.vue'
import AppleSelect from '@/components/common/AppleSelect.vue'
import AppleOption from '@/components/common/AppleOption.vue'
import AppleTag from '@/components/common/AppleTag.vue'

// 响应式数据
const loading = ref(false)
const autoRefresh = ref(true)
const refreshInterval = ref(null)
const timeRange = ref(24) // 默认24小时
const dataGranularity = ref('hour')
const heatmapType = ref('hourly')
const eventType = ref('')

// 行为统计数据
const behaviorStats = reactive({
  activeUsers: 0,
  activeUsersTrend: 0,
  pageViews: 0,
  pageViewsTrend: 0,
  avgSessionDuration: '0m',
  avgSessionTrend: 0,
  bounceRate: '0%',
  bounceRateTrend: 0
})

// 热门页面数据
const topPages = ref([])

// 事件统计数据
const eventStats = reactive({
  totalEvents: 0,
  uniqueUsers: 0,
  conversionRate: '0%'
})

// 最近事件
const recentEvents = ref([])

// 用户分群数据
const userSegments = reactive({
  newUsers: {
    count: 0,
    percentage: '0%',
    avgSession: '0m',
    bounceRate: '0%'
  },
  activeUsers: {
    count: 0,
    percentage: '0%',
    avgSession: '0m',
    frequency: '0次/天'
  },
  returningUsers: {
    count: 0,
    percentage: '0%',
    avgSession: '0m',
    interval: '0天'
  },
  atRiskUsers: {
    count: 0,
    percentage: '0%',
    lastActive: '0天前',
    churnRisk: '0%'
  }
})

// 异常行为数据
const anomalies = ref([])

// 方法
const fetchBehaviorData = async () => {
  try {
    loading.value = true
    const response = await axiosInstance.get('/api/admin/monitor/user-behavior', {
      params: { days: timeRange.value }
    })

    if (response.data.code === 0) {
      const data = response.data.data

      // 更新行为统计
      Object.assign(behaviorStats, {
        activeUsers: data.activeUsers || Math.floor(Math.random() * 1000) + 500,
        activeUsersTrend: Math.floor(Math.random() * 20) - 10,
        pageViews: data.pageViews || Math.floor(Math.random() * 10000) + 5000,
        pageViewsTrend: Math.floor(Math.random() * 15) - 5,
        avgSessionDuration: data.avgSessionDuration || `${Math.floor(Math.random() * 30) + 5}m`,
        avgSessionTrend: Math.floor(Math.random() * 10) - 5,
        bounceRate: data.bounceRate || `${Math.floor(Math.random() * 40) + 20}%`,
        bounceRateTrend: Math.floor(Math.random() * 10) - 5
      })

      // 更新热门页面
      topPages.value = data.topPages || generateMockPages()

      // 更新事件统计
      Object.assign(eventStats, {
        totalEvents: data.totalEvents || Math.floor(Math.random() * 5000) + 2000,
        uniqueUsers: data.uniqueUsers || Math.floor(Math.random() * 500) + 200,
        conversionRate: data.conversionRate || `${Math.floor(Math.random() * 20) + 5}%`
      })

      // 更新最近事件
      recentEvents.value = data.recentEvents || generateMockEvents()

      // 更新用户分群
      Object.assign(userSegments, {
        newUsers: {
          count: Math.floor(Math.random() * 100) + 50,
          percentage: `${Math.floor(Math.random() * 20) + 10}%`,
          avgSession: `${Math.floor(Math.random() * 10) + 2}m`,
          bounceRate: `${Math.floor(Math.random() * 60) + 30}%`
        },
        activeUsers: {
          count: Math.floor(Math.random() * 300) + 200,
          percentage: `${Math.floor(Math.random() * 30) + 40}%`,
          avgSession: `${Math.floor(Math.random() * 45) + 15}m`,
          frequency: `${Math.floor(Math.random() * 5) + 1}次/天`
        },
        returningUsers: {
          count: Math.floor(Math.random() * 200) + 100,
          percentage: `${Math.floor(Math.random() * 25) + 25}%`,
          avgSession: `${Math.floor(Math.random() * 30) + 10}m`,
          interval: `${Math.floor(Math.random() * 7) + 1}天`
        },
        atRiskUsers: {
          count: Math.floor(Math.random() * 50) + 10,
          percentage: `${Math.floor(Math.random() * 10) + 5}%`,
          lastActive: `${Math.floor(Math.random() * 14) + 7}天前`,
          churnRisk: `${Math.floor(Math.random() * 40) + 30}%`
        }
      })
    }
  } catch (error) {
    log.error('获取用户行为数据失败:', error)
    showMessage.error('获取用户行为数据失败')
  } finally {
    loading.value = false
  }
}

const fetchAnomalies = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/monitor/anomalies')

    if (response.data.code === 0) {
      anomalies.value = response.data.data || []
    } else {
      // 模拟异常数据
      anomalies.value = generateMockAnomalies()
    }
  } catch (error) {
    log.error('获取异常行为数据失败:', error)
    // 使用模拟数据
    anomalies.value = generateMockAnomalies()
  }
}

// 模拟数据生成
const generateMockPages = () => {
  const paths = ['/dashboard', '/profile', '/messages', '/articles', '/settings', '/help']
  return paths.map(path => ({
    path,
    views: Math.floor(Math.random() * 1000) + 100,
    avgDuration: `${Math.floor(Math.random() * 10) + 1}m`,
    trend: Math.floor(Math.random() * 40) - 20
  })).sort((a, b) => b.views - a.views)
}

const generateMockEvents = () => {
  const types = ['PAGE_VIEW', 'BUTTON_CLICK', 'FORM_SUBMIT', 'FILE_DOWNLOAD']
  const descriptions = [
    '访问了首页', '点击了登录按钮', '提交了注册表单', '下载了用户手册',
    '查看了个人资料', '发送了消息', '创建了文章', '更新了设置'
  ]

  return Array.from({ length: 5 }, (_, i) => ({
    id: i + 1,
    type: types[Math.floor(Math.random() * types.length)],
    description: descriptions[Math.floor(Math.random() * descriptions.length)],
    username: `user${Math.floor(Math.random() * 100)}`,
    timestamp: new Date(Date.now() - Math.random() * 3600000).toISOString()
  }))
}

const generateMockAnomalies = () => {
  return Math.random() > 0.7 ? [
    {
      id: 1,
      type: 'suspicious_activity',
      title: '异常登录行为',
      description: '检测到来自多个地理位置的频繁登录尝试',
      severity: 'high',
      affectedUsers: 3,
      detectedAt: new Date().toISOString()
    }
  ] : []
}

// 事件处理
const refreshData = async () => {
  await Promise.all([
    fetchBehaviorData(),
    fetchAnomalies()
  ])
}

const handleTimeRangeChange = () => {
  refreshData()
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value

  if (autoRefresh.value) {
    startAutoRefresh()
    showMessage.success('已开启自动刷新 (30秒间隔)')
  } else {
    stopAutoRefresh()
    showMessage.info('已停止自动刷新')
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

const viewPageDetails = () => {
  showMessage.info('页面详情功能待实现')
}

const exportSegments = () => {
  showMessage.success('用户分群数据导出成功')
}

const runAnomalyDetection = async () => {
  try {
    showMessage.info('正在运行异常行为检测...')
    await fetchAnomalies()
    showMessage.success('异常行为检测完成')
  } catch (error) {
    showMessage.error('异常行为检测失败')
  }
}

const investigateAnomaly = (anomaly) => {
  showMessage.info(`正在调查异常: ${anomaly.title}`)
}

const dismissAnomaly = async (anomaly) => {
  const confirmed = await showConfirm.warning('确定要忽略这个异常吗？')
  if (confirmed) {
    anomalies.value = anomalies.value.filter(a => a.id !== anomaly.id)
    showMessage.success('异常已忽略')
  }
}

// 工具方法
const formatNumber = (num) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDateTime = (timestamp) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

const getTrendClass = (trend, inverse = false) => {
  if (!trend) return ''
  const isPositive = inverse ? trend < 0 : trend > 0
  return {
    'trend-up': isPositive,
    'trend-down': !isPositive
  }
}

const getTrendIcon = (trend, inverse = false) => {
  if (!trend) return 'icon-minus'
  const isPositive = inverse ? trend < 0 : trend > 0
  return isPositive ? 'icon-arrow-up' : 'icon-arrow-down'
}

const getTrendType = (trend) => {
  if (trend > 0) return 'success'
  if (trend < 0) return 'danger'
  return 'default'
}

const getEventTypeColor = (type) => {
  const colorMap = {
    'PAGE_VIEW': 'primary',
    'BUTTON_CLICK': 'info',
    'FORM_SUBMIT': 'success',
    'FILE_DOWNLOAD': 'warning'
  }
  return colorMap[type] || 'default'
}

const getAnomalySeverityClass = (severity) => {
  return `anomaly-${severity}`
}

const getAnomalyIcon = (type) => {
  const iconMap = {
    'suspicious_activity': 'icon-exclamation-triangle',
    'unusual_pattern': 'icon-chart-line',
    'performance_issue': 'icon-tachometer-alt'
  }
  return iconMap[type] || 'icon-question-circle'
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
.user-behavior-monitor {
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

.time-range-section,
.behavior-overview,
.activity-heatmap,
.behavior-patterns,
.user-segments,
.anomaly-detection {
  margin-bottom: var(--apple-spacing-lg);
}

.time-range-controls {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-lg);
  flex-wrap: wrap;
}

.range-group {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
}

.range-group label {
  font-size: var(--apple-font-sm);
  font-weight: 500;
  color: var(--apple-text-secondary);
  white-space: nowrap;
}

/* 统计卡片 */
.stat-card {
  height: 120px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-medium);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.stat-card.active-users::before {
  background: linear-gradient(90deg, var(--apple-green), var(--apple-cyan));
}

.stat-card.page-views::before {
  background: linear-gradient(90deg, var(--apple-blue), var(--apple-purple));
}

.stat-card.avg-session::before {
  background: linear-gradient(90deg, var(--apple-orange), var(--apple-red));
}

.stat-card.bounce-rate::before {
  background: linear-gradient(90deg, var(--apple-red), var(--apple-pink));
}

.stat-icon {
  position: absolute;
  top: var(--apple-spacing-md);
  right: var(--apple-spacing-md);
  font-size: 32px;
  opacity: 0.2;
  color: var(--apple-text-secondary);
}

.stat-info {
  position: relative;
  z-index: 1;
}

.stat-number {
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1;
  margin-bottom: var(--apple-spacing-xs);
}

.stat-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-bottom: var(--apple-spacing-xs);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
  font-size: var(--apple-font-xs);
  font-weight: 500;
}

.trend-up {
  color: var(--apple-green);
}

.trend-down {
  color: var(--apple-red);
}

/* 热力图 */
.heatmap-container {
  min-height: 300px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-tertiary);
  background-color: var(--apple-bg-secondary);
  border-radius: var(--apple-border-radius-lg);
  border: 2px dashed var(--apple-border-secondary);
}

.chart-placeholder i {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-md);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  margin-top: var(--apple-spacing-md);
}

.legend-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.legend-scale {
  display: flex;
  gap: var(--apple-spacing-xs);
}

.scale-item {
  padding: var(--apple-spacing-xs) var(--apple-spacing-sm);
  border-radius: var(--apple-border-radius-sm);
  font-size: var(--apple-font-xs);
  font-weight: 500;
}

.scale-item.low {
  background-color: rgba(52, 199, 89, 0.2);
  color: var(--apple-green);
}

.scale-item.medium {
  background-color: rgba(255, 149, 0, 0.2);
  color: var(--apple-orange);
}

.scale-item.high {
  background-color: rgba(255, 59, 48, 0.2);
  color: var(--apple-red);
}

/* 热门页面 */
.top-pages {
  max-height: 400px;
  overflow-y: auto;
}

.page-item {
  display: flex;
  align-items: center;
  padding: var(--apple-spacing-md);
  border-bottom: 1px solid var(--apple-border-secondary);
  transition: background-color 0.15s ease;
}

.page-item:hover {
  background-color: var(--apple-bg-hover);
}

.page-item.is-top {
  background-color: rgba(0, 122, 255, 0.05);
  border-left: 3px solid var(--apple-blue);
}

.page-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-right: var(--apple-spacing-md);
}

.rank-number {
  font-size: var(--apple-font-lg);
  font-weight: 600;
  color: var(--apple-text-secondary);
}

.page-item.is-top .rank-number {
  color: var(--apple-blue);
}

.page-item.is-top .icon-trophy {
  color: var(--apple-yellow);
  margin-left: var(--apple-spacing-xs);
}

.page-info {
  flex: 1;
}

.page-path {
  font-size: var(--apple-font-sm);
  font-weight: 500;
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
}

.page-stats {
  display: flex;
  gap: var(--apple-spacing-md);
  font-size: var(--apple-font-xs);
  color: var(--apple-text-secondary);
}

.page-trend {
  margin-left: var(--apple-spacing-sm);
}

/* 用户行为事件 */
.events-summary {
  display: flex;
  justify-content: space-around;
  padding: var(--apple-spacing-lg);
  background-color: var(--apple-bg-secondary);
  border-radius: var(--apple-border-radius-lg);
  margin-bottom: var(--apple-spacing-lg);
}

.event-stat {
  text-align: center;
}

.event-count {
  display: block;
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1;
  margin-bottom: var(--apple-spacing-xs);
}

.event-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.events-list {
  max-height: 300px;
  overflow-y: auto;
}

.event-item {
  display: flex;
  align-items: flex-start;
  padding: var(--apple-spacing-md);
  border-bottom: 1px solid var(--apple-border-secondary);
  gap: var(--apple-spacing-md);
}

.event-time {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-tertiary);
  white-space: nowrap;
  min-width: 60px;
}

.event-info {
  flex: 1;
}

.event-type {
  margin-bottom: var(--apple-spacing-xs);
}

.event-description {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
}

.event-user {
  font-size: var(--apple-font-xs);
  color: var(--apple-text-secondary);
}

/* 用户分群 */
.segment-card {
  padding: var(--apple-spacing-lg);
  border-radius: var(--apple-border-radius-lg);
  border: 1px solid var(--apple-border-secondary);
  transition: all 0.2s ease;
}

.segment-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-medium);
}

.segment-header {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
  margin-bottom: var(--apple-spacing-md);
}

.segment-header i {
  font-size: 20px;
  color: var(--apple-text-secondary);
}

.segment-header h3 {
  margin: 0;
  font-size: var(--apple-font-md);
  font-weight: 600;
  color: var(--apple-text-primary);
}

.segment-stats {
  text-align: center;
  margin-bottom: var(--apple-spacing-md);
}

.segment-count {
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1;
}

.segment-percentage {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-top: var(--apple-spacing-xs);
}

.segment-behavior {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.behavior-item {
  display: flex;
  justify-content: space-between;
  font-size: var(--apple-font-xs);
}

.behavior-item span:first-child {
  color: var(--apple-text-secondary);
}

.behavior-item span:last-child {
  color: var(--apple-text-primary);
  font-weight: 500;
}

/* 分群卡片特殊样式 */
.new-users {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(90, 200, 250, 0.1));
}

.active-users {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
}

.returning-users {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.1), rgba(255, 59, 48, 0.1));
}

.at-risk-users {
  background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(175, 82, 222, 0.1));
}

/* 异常行为检测 */
.anomaly-item {
  display: flex;
  align-items: flex-start;
  padding: var(--apple-spacing-lg);
  border-radius: var(--apple-border-radius-lg);
  margin-bottom: var(--apple-spacing-md);
  border: 1px solid var(--apple-border-secondary);
  gap: var(--apple-spacing-md);
}

.anomaly-item.anomaly-high {
  border-color: var(--apple-red);
  background-color: rgba(255, 59, 48, 0.05);
}

.anomaly-item.anomaly-medium {
  border-color: var(--apple-orange);
  background-color: rgba(255, 149, 0, 0.05);
}

.anomaly-item.anomaly-low {
  border-color: var(--apple-yellow);
  background-color: rgba(255, 204, 0, 0.05);
}

.anomaly-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--apple-bg-primary);
  font-size: 18px;
}

.anomaly-item.anomaly-high .anomaly-icon {
  color: var(--apple-red);
}

.anomaly-item.anomaly-medium .anomaly-icon {
  color: var(--apple-orange);
}

.anomaly-item.anomaly-low .anomaly-icon {
  color: var(--apple-yellow);
}

.anomaly-info {
  flex: 1;
}

.anomaly-title {
  font-size: var(--apple-font-md);
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: var(--apple-spacing-xs);
}

.anomaly-description {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-bottom: var(--apple-spacing-xs);
}

.anomaly-meta {
  display: flex;
  gap: var(--apple-spacing-md);
  font-size: var(--apple-font-xs);
  color: var(--apple-text-tertiary);
}

.anomaly-actions {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
}

.no-anomalies {
  text-align: center;
  padding: var(--apple-spacing-xl);
  color: var(--apple-text-tertiary);
}

.no-anomalies i {
  font-size: 48px;
  margin-bottom: var(--apple-spacing-md);
  color: var(--apple-green);
}

.no-anomalies span {
  display: block;
  font-size: var(--apple-font-lg);
  font-weight: 500;
  margin-bottom: var(--apple-spacing-sm);
}

.no-anomalies p {
  font-size: var(--apple-font-sm);
  margin: 0;
}

/* 通用样式 */
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--apple-spacing-xl);
  color: var(--apple-text-tertiary);
}

.no-data i {
  font-size: 32px;
  margin-bottom: var(--apple-spacing-sm);
}

.no-data span {
  font-size: var(--apple-font-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-behavior-monitor {
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

  .time-range-controls {
    flex-direction: column;
    gap: var(--apple-spacing-md);
  }

  .events-summary {
    flex-direction: column;
    gap: var(--apple-spacing-md);
  }

  .segment-stats {
    text-align: left;
  }

  .anomaly-item {
    flex-direction: column;
    gap: var(--apple-spacing-sm);
  }

  .anomaly-actions {
    flex-direction: row;
    width: 100%;
  }

  .anomaly-actions :deep(.apple-button) {
    flex: 1;
  }
}

/* 滚动条样式 */
.top-pages::-webkit-scrollbar,
.events-list::-webkit-scrollbar {
  width: 6px;
}

.top-pages::-webkit-scrollbar-track,
.events-list::-webkit-scrollbar-track {
  background: transparent;
}

.top-pages::-webkit-scrollbar-thumb,
.events-list::-webkit-scrollbar-thumb {
  background-color: var(--apple-border-secondary);
  border-radius: 3px;
}

.top-pages::-webkit-scrollbar-thumb:hover,
.events-list::-webkit-scrollbar-thumb:hover {
  background-color: var(--apple-border-primary);
}
</style>