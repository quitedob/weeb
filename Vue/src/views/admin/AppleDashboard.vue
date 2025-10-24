<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>管理后台首页</h1>
      <p class="subtitle">系统概览和管理中心</p>
    </div>

    <div class="dashboard-content">
      <!-- 统计卡片 -->
      <div class="stats-section">
        <AppleGrid :gutter="20" :columns="{ xs: 1, sm: 2, md: 3, lg: 4 }">
          <AppleCol :xs="24" :sm="12" :md="6">
            <AppleCard class="stat-card stat-card--users">
              <div class="stat-content">
                <div class="stat-icon user-icon">
                  <span class="icon">👥</span>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stats.totalUsers || 0 }}</div>
                  <div class="stat-label">总用户数</div>
                  <div class="stat-trend">
                    <span :class="['trend', userStats.trend > 0 ? 'trend-up' : 'trend-down']">
                      {{ userStats.trend > 0 ? '↑' : '↓' }}
                      {{ Math.abs(userStats.trend) }}%
                    </span>
                  </div>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol :xs="24" :sm="12" :md="6">
            <AppleCard class="stat-card stat-card--articles">
              <div class="stat-content">
                <div class="stat-icon article-icon">
                  <span class="icon">📄</span>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stats.totalArticles || 0 }}</div>
                  <div class="stat-label">文章总数</div>
                  <div class="stat-trend">
                    <span :class="['trend', articleStats.trend > 0 ? 'trend-up' : 'trend-down']">
                      {{ articleStats.trend > 0 ? '↑' : '↓' }}
                      {{ Math.abs(articleStats.trend) }}%
                    </span>
                  </div>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol :xs="24" :sm="12" :md="6">
            <AppleCard class="stat-card stat-card--messages">
              <div class="stat-content">
                <div class="stat-icon message-icon">
                  <span class="icon">💬</span>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stats.totalMessages || 0 }}</div>
                  <div class="stat-label">消息总数</div>
                  <div class="stat-trend">
                    <span :class="['trend', messageStats.trend > 0 ? 'trend-up' : 'trend-down']">
                      {{ messageStats.trend > 0 ? '↑' : '↓' }}
                      {{ Math.abs(messageStats.trend) }}%
                    </span>
                  </div>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol :xs="24" :sm="12" :md="6">
            <AppleCard class="stat-card stat-card--groups">
              <div class="stat-content">
                <div class="stat-icon group-icon">
                  <span class="icon">👥</span>
                </div>
                <div class="stat-info">
                  <div class="stat-number">{{ stats.totalGroups || 0 }}</div>
                  <div class="stat-label">群组总数</div>
                  <div class="stat-trend">
                    <span :class="['trend', groupStats.trend > 0 ? 'trend-up' : 'trend-down']">
                      {{ groupStats.trend > 0 ? '↑' : '↓' }}
                      {{ Math.abs(groupStats.trend) }}%
                    </span>
                  </div>
                </div>
              </div>
            </AppleCard>
          </AppleCol>
        </AppleGrid>
      </div>

      <!-- 快速操作 -->
      <div class="quick-actions-section">
        <h2 class="section-title">快速操作</h2>
        <AppleGrid :gutter="20" :columns="{ xs: 1, sm: 2, md: 3 }">
          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/users')">
              <div class="action-content">
                <div class="action-icon">
                  <span>👥</span>
                </div>
                <div class="action-info">
                  <h3>用户管理</h3>
                  <p>管理系统用户和权限</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/content')">
              <div class="action-content">
                <div class="action-icon">
                  <span>📝</span>
                </div>
                <div class="action-info">
                  <h3>内容审核</h3>
                  <p>审核文章和评论内容</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/settings')">
              <div class="action-content">
                <div class="action-icon">
                  <span>⚙️</span>
                </div>
                <div class="action-info">
                  <h3>系统设置</h3>
                  <p>配置系统参数</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/logs')">
              <div class="action-content">
                <div class="action-icon">
                  <span>📊</span>
                </div>
                <div class="action-info">
                  <h3>系统日志</h3>
                  <p>查看操作记录</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/monitor')">
              <div class="action-content">
                <div class="action-icon">
                  <span>📈</span>
                </div>
                <div class="action-info">
                  <h3>系统监控</h3>
                  <p>查看系统状态</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="action-card" hoverable @click="navigateTo('/admin/backup')">
              <div class="action-content">
                <div class="action-icon">
                  <span>💾</span>
                </div>
                <div class="action-info">
                  <h3>数据备份</h3>
                  <p>备份系统数据</p>
                </div>
              </div>
            </AppleCard>
          </AppleCol>
        </AppleGrid>
      </div>

      <!-- 最近活动 -->
      <div class="recent-activity-section">
        <h2 class="section-title">最近活动</h2>
        <AppleCard class="activity-card">
          <div v-if="loadingActivities" class="loading-placeholder">
            <div class="loading-text">加载活动记录中...</div>
          </div>
          <div v-else class="activity-list">
            <div
              v-for="activity in recentActivities"
              :key="activity.id"
              class="activity-item"
            >
              <div class="activity-icon" :class="`activity-${activity.type}`">
                <span>{{ getActivityIcon(activity.type) }}</span>
              </div>
              <div class="activity-content">
                <div class="activity-description">{{ activity.description }}</div>
                <div class="activity-time">{{ formatTime(activity.timestamp) }}</div>
              </div>
            </div>
          </div>
        </AppleCard>
      </div>

      <!-- 系统状态 -->
      <div class="system-status-section">
        <h2 class="section-title">系统状态</h2>
        <AppleGrid :gutter="20" :columns="{ xs: 1, sm: 2, md: 3 }">
          <AppleCol>
            <AppleCard class="status-card">
              <div class="status-header">
                <div class="status-icon" :class="systemStatus.database.status">
                  <span>🗄️</span>
                </div>
                <div class="status-info">
                  <h4>数据库</h4>
                  <span class="status-text">{{ systemStatus.database.text }}</span>
                </div>
              </div>
              <div class="status-details">
                <div class="status-item">
                  <span>连接数:</span>
                  <span>{{ systemStatus.database.connections }}</span>
                </div>
                <div class="status-item">
                  <span>响应时间:</span>
                  <span>{{ systemStatus.database.responseTime }}ms</span>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="status-card">
              <div class="status-header">
                <div class="status-icon" :class="systemStatus.redis.status">
                  <span>🔴</span>
                </div>
                <div class="status-info">
                  <h4>Redis缓存</h4>
                  <span class="status-text">{{ systemStatus.redis.text }}</span>
                </div>
              </div>
              <div class="status-details">
                <div class="status-item">
                  <span>内存使用:</span>
                  <span>{{ systemStatus.redis.memory }}%</span>
                </div>
                <div class="status-item">
                  <span>命中率:</span>
                  <span>{{ systemStatus.redis.hitRate }}%</span>
                </div>
              </div>
            </AppleCard>
          </AppleCol>

          <AppleCol>
            <AppleCard class="status-card">
              <div class="status-header">
                <div class="status-icon" :class="systemStatus.websocket.status">
                  <span>🔌</span>
                </div>
                <div class="status-info">
                  <h4>WebSocket</h4>
                  <span class="status-text">{{ systemStatus.websocket.text }}</span>
                </div>
              </div>
              <div class="status-details">
                <div class="status-item">
                  <span>连接数:</span>
                  <span>{{ systemStatus.websocket.connections }}</span>
                </div>
                <div class="status-item">
                  <span>消息数:</span>
                  <span>{{ systemStatus.websocket.messages }}/分钟</span>
                </div>
              </div>
            </AppleCard>
          </AppleCol>
        </AppleGrid>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppleGrid from '@/components/common/AppleGrid.vue'
import AppleCol from '@/components/common/AppleCol.vue'
import AppleCard from '@/components/common/AppleCard.vue'
import { useAdminStore } from '@/stores/adminStore'

export default {
  name: 'AppleDashboard',
  components: {
    AppleGrid,
    AppleCol,
    AppleCard
  },
  setup() {
    const router = useRouter()
    const adminStore = useAdminStore()

    const stats = ref({
      totalUsers: 0,
      totalArticles: 0,
      totalMessages: 0,
      totalGroups: 0
    })

    const userStats = ref({ trend: 0 })
    const articleStats = ref({ trend: 0 })
    const messageStats = ref({ trend: 0 })
    const groupStats = ref({ trend: 0 })

    const recentActivities = ref([])
    const loadingActivities = ref(true)

    const systemStatus = ref({
      database: {
        status: 'normal',
        text: '正常',
        connections: 15,
        responseTime: 12
      },
      redis: {
        status: 'normal',
        text: '正常',
        memory: 45,
        hitRate: 89
      },
      websocket: {
        status: 'normal',
        text: '正常',
        connections: 8,
        messages: 156
      }
    })

    const navigateTo = (path) => {
      router.push(path)
    }

    const getActivityIcon = (type) => {
      const iconMap = {
        'user_create': '👤',
        'user_login': '🔐',
        'user_logout': '🚪',
        'article_create': '📝',
        'article_update': '✏️',
        'article_delete': '🗑️',
        'message_send': '💬',
        'group_create': '👥',
        'system_error': '⚠️',
        'system_warning': '⚡',
        'system_info': 'ℹ️'
      }
      return iconMap[type] || '📋'
    }

    const formatTime = (timestamp) => {
      const date = new Date(timestamp)
      const now = new Date()
      const diff = now - date

      if (diff < 60000) { // 1分钟内
        return '刚刚'
      } else if (diff < 3600000) { // 1小时内
        return `${Math.floor(diff / 60000)}分钟前`
      } else if (diff < 86400000) { // 1天内
        return `${Math.floor(diff / 3600000)}小时前`
      } else {
        return date.toLocaleDateString('zh-CN')
      }
    }

    const loadDashboardData = async () => {
      try {
        // 加载统计数据
        const statsResponse = await adminStore.getDashboardStats()
        if (statsResponse.success) {
          stats.value = statsResponse.data
        }

        // 加载最近活动
        const activitiesResponse = await adminStore.getRecentActivities()
        if (activitiesResponse.success) {
          recentActivities.value = activitiesResponse.data
        }

        // 加载系统状态
        const statusResponse = await adminStore.getSystemStatus()
        if (statusResponse.success) {
          systemStatus.value = statusResponse.data
        }

        // 模拟趋势数据
        userStats.value.trend = Math.floor(Math.random() * 20) - 10
        articleStats.value.trend = Math.floor(Math.random() * 15) - 5
        messageStats.value.trend = Math.floor(Math.random() * 25) - 10
        groupStats.value.trend = Math.floor(Math.random() * 12) - 5

      } catch (error) {
        console.error('加载仪表板数据失败:', error)
      } finally {
        loadingActivities.value = false
      }
    }

    onMounted(() => {
      loadDashboardData()
      // 每30秒刷新一次数据
      setInterval(loadDashboardData, 30000)
    })

    return {
      stats,
      userStats,
      articleStats,
      messageStats,
      groupStats,
      recentActivities,
      loadingActivities,
      systemStatus,
      navigateTo,
      getActivityIcon,
      formatTime
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  background: var(--apple-bg-secondary);
  min-height: 100vh;
}

.dashboard-header {
  margin-bottom: 30px;
}

.dashboard-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.subtitle {
  margin: 0;
  color: var(--apple-text-tertiary);
  font-size: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.stats-section,
.quick-actions-section,
.recent-activity-section,
.system-status-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 统计卡片 */
.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-card--users .stat-icon {
  background: linear-gradient(135deg, #007AFF, #0056CC);
}

.stat-card--articles .stat-icon {
  background: linear-gradient(135deg, #34C759, #28a745);
}

.stat-card--messages .stat-icon {
  background: linear-gradient(135deg, #FF9500, #e0a800);
}

.stat-card--groups .stat-icon {
  background: linear-gradient(135deg, #AF52DE, #8b3fd6);
}

.stat-icon .icon {
  filter: brightness(0) invert(1);
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: var(--apple-text-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 500;
}

.trend-up {
  color: var(--apple-green);
}

.trend-down {
  color: var(--apple-red);
}

/* 快速操作卡片 */
.action-card {
  cursor: pointer;
  height: 100px;
  transition: all 0.2s ease;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-content {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 12px;
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--apple-bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.action-info h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.action-info p {
  margin: 0;
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

/* 活动卡片 */
.activity-card {
  min-height: 300px;
}

.loading-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.loading-text {
  color: var(--apple-text-tertiary);
  font-size: 14px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--apple-bg-quaternary);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
}

.activity-description {
  font-size: 14px;
  color: var(--apple-text-primary);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--apple-text-tertiary);
}

/* 状态卡片 */
.status-card {
  height: 140px;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.status-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.status-icon.normal {
  background: var(--apple-green);
}

.status-icon.warning {
  background: var(--apple-orange);
}

.status-icon.error {
  background: var(--apple-red);
}

.status-info h4 {
  margin: 0 0 2px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.status-text {
  font-size: 12px;
  color: var(--apple-text-secondary);
}

.status-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  color: var(--apple-text-tertiary);
}

.status-item span:first-child {
  font-weight: 500;
}

.status-item span:last-child {
  font-weight: 600;
  color: var(--apple-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-dashboard {
    padding: 16px;
  }

  .dashboard-header h1 {
    font-size: 24px;
  }

  .subtitle {
    font-size: 14px;
  }

  .section-title {
    font-size: 18px;
  }

  .stat-card {
    height: 100px;
  }

  .stat-content {
    gap: 12px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }

  .stat-number {
    font-size: 20px;
  }

  .action-card {
    height: 80px;
  }

  .action-icon {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .action-info h3 {
    font-size: 14px;
  }

  .action-info p {
    font-size: 11px;
  }

  .status-card {
    height: 120px;
  }

  .status-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
}
</style>