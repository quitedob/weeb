<template>
  <div class="system-log-viewer">
    <!-- 顶部统计卡片 -->
    <div class="stats-section">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="icon-documents"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.totalLogs }}</div>
            <div class="stat-label">总日志数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon today">
            <i class="icon-calendar"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.todayLogs }}</div>
            <div class="stat-label">今日日志</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon error">
            <i class="icon-warning"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.errorLogs }}</div>
            <div class="stat-label">错误日志</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon warning">
            <i class="icon-info"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ statistics.warningLogs }}</div>
            <div class="stat-label">警告日志</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-card">
        <div class="filter-header">
          <h3>筛选条件</h3>
          <AppleButton variant="ghost" size="small" @click="resetFilters">
            <i class="icon-refresh"></i> 重置
          </AppleButton>
        </div>
        <div class="filter-grid">
          <div class="filter-item">
            <label>关键词搜索</label>
            <AppleInput
              v-model="filters.keyword"
              placeholder="搜索操作详情..."
              clearable
            />
          </div>
          <div class="filter-item">
            <label>操作类型</label>
            <AppleSelect v-model="filters.action" placeholder="选择操作类型" clearable>
              <AppleOption
                v-for="action in availableActions"
                :key="action"
                :value="action"
                :label="action"
              />
            </AppleSelect>
          </div>
          <div class="filter-item">
            <label>操作员</label>
            <AppleSelect v-model="filters.operatorId" placeholder="选择操作员" clearable>
              <AppleOption
                v-for="operator in availableOperators"
                :key="operator.value"
                :value="operator.value"
                :label="operator.label"
              />
            </AppleSelect>
          </div>
          <div class="filter-item">
            <label>IP地址</label>
            <AppleInput
              v-model="filters.ipAddress"
              placeholder="输入IP地址..."
              clearable
            />
          </div>
          <div class="filter-item">
            <label>开始时间</label>
            <AppleInput
              v-model="filters.startDate"
              type="date"
              clearable
            />
          </div>
          <div class="filter-item">
            <label>结束时间</label>
            <AppleInput
              v-model="filters.endDate"
              type="date"
              clearable
            />
          </div>
        </div>
        <div class="filter-actions">
          <AppleButton variant="primary" @click="searchLogs" :loading="loading">
            <i class="icon-search"></i> 查询
          </AppleButton>
          <AppleButton variant="secondary" @click="exportLogs" :loading="exporting">
            <i class="icon-download"></i> 导出
          </AppleButton>
        </div>
      </div>
    </div>

    <!-- 日志表格 -->
    <div class="table-section">
      <div class="table-card">
        <div class="table-header">
          <h3>系统日志</h3>
          <div class="table-actions">
            <AppleSelect v-model="exportFormat" size="small">
              <AppleOption value="csv" label="CSV格式" />
              <AppleOption value="xlsx" label="Excel格式" />
              <AppleOption value="json" label="JSON格式" />
            </AppleSelect>
            <AppleButton variant="danger" size="small" @click="showCleanupModal = true">
              <i class="icon-trash"></i> 清理
            </AppleButton>
          </div>
        </div>

        <AppleTable
          :data="logs"
          :columns="tableColumns"
          :loading="loading"
          :pagination="pagination"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
          @row-click="handleRowClick"
          striped
          hover
        >
          <template #id="{ row }">
            <span class="log-id">#{{ row.id }}</span>
          </template>
          <template #action="{ row }">
            <AppleTag
              :variant="getActionVariant(row.action)"
              size="small"
            >
              {{ row.action }}
            </AppleTag>
          </template>
          <template #details="{ row }">
            <div class="log-details" :title="row.details">
              {{ truncateText(row.details, 50) }}
            </div>
          </template>
          <template #createdAt="{ row }">
            <div class="log-time">
              <div>{{ formatDate(row.createdAt) }}</div>
              <div class="time-detail">{{ formatTime(row.createdAt) }}</div>
            </div>
          </template>
          <template #actions="{ row }">
            <AppleButton
              variant="ghost"
              size="small"
              @click="viewLogDetails(row)"
            >
              <i class="icon-eye"></i> 详情
            </AppleButton>
          </template>
        </AppleTable>
      </div>
    </div>

    <!-- 图表分析区域 -->
    <div class="charts-section">
      <div class="charts-grid">
        <div class="chart-card">
          <h3>操作类型分布</h3>
          <div class="chart-content">
            <div class="action-distribution">
              <div
                v-for="(count, action) in actionDistribution"
                :key="action"
                class="action-item"
              >
                <div class="action-label">{{ action }}</div>
                <div class="action-bar">
                  <div
                    class="action-fill"
                    :style="{ width: (count / maxActionCount * 100) + '%' }"
                  ></div>
                </div>
                <div class="action-count">{{ count }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <h3>每小时活动统计</h3>
          <div class="chart-content">
            <div class="hourly-stats">
              <div
                v-for="(count, hour) in hourlyActivity"
                :key="hour"
                class="hour-item"
              >
                <div class="hour-label">{{ hour }}</div>
                <div class="hour-bar">
                  <div
                    class="hour-fill"
                    :style="{ height: (count / maxHourlyCount * 100) + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日志详情弹窗 -->
    <AppleModal
      v-model="showDetailsModal"
      title="日志详情"
      width="600px"
    >
      <div v-if="selectedLog" class="log-details-content">
        <div class="detail-item">
          <label>日志ID:</label>
          <span>{{ selectedLog.id }}</span>
        </div>
        <div class="detail-item">
          <label>操作者ID:</label>
          <span>{{ selectedLog.operatorId }}</span>
        </div>
        <div class="detail-item">
          <label>操作类型:</label>
          <AppleTag :variant="getActionVariant(selectedLog.action)">
            {{ selectedLog.action }}
          </AppleTag>
        </div>
        <div class="detail-item">
          <label>IP地址:</label>
          <span>{{ selectedLog.ipAddress }}</span>
        </div>
        <div class="detail-item">
          <label>创建时间:</label>
          <span>{{ formatDateTime(selectedLog.createdAt) }}</span>
        </div>
        <div class="detail-item full-width">
          <label>操作详情:</label>
          <div class="details-text">{{ selectedLog.details }}</div>
        </div>
      </div>
    </AppleModal>

    <!-- 清理日志弹窗 -->
    <AppleModal
      v-model="showCleanupModal"
      title="清理过期日志"
      width="500px"
    >
      <div class="cleanup-content">
        <p class="cleanup-warning">
          <i class="icon-warning"></i>
          此操作将永久删除超过指定天数的日志，请谨慎操作！
        </p>
        <div class="cleanup-form">
          <div class="form-item">
            <label>保留天数:</label>
            <AppleInput
              v-model="cleanupDays"
              type="number"
              :min="7"
              placeholder="最少保留7天"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <AppleButton variant="ghost" @click="showCleanupModal = false">
          取消
        </AppleButton>
        <AppleButton
          variant="danger"
          @click="cleanupLogs"
          :loading="cleaning"
        >
          确认清理
        </AppleButton>
      </template>
    </AppleModal>

    <!-- 导出选项弹窗 -->
    <AppleModal
      v-model="showExportModal"
      title="导出日志"
      width="500px"
    >
      <div class="export-content">
        <div class="export-form">
          <div class="form-item">
            <label>导出格式:</label>
            <AppleSelect v-model="exportFormat">
              <AppleOption value="csv" label="CSV格式" />
              <AppleOption value="xlsx" label="Excel格式" />
              <AppleOption value="json" label="JSON格式" />
            </AppleSelect>
          </div>
          <div class="form-item">
            <label>时间范围:</label>
            <div class="date-range">
              <AppleInput v-model="exportStartDate" type="date" />
              <span>至</span>
              <AppleInput v-model="exportEndDate" type="date" />
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <AppleButton variant="ghost" @click="showExportModal = false">
          取消
        </AppleButton>
        <AppleButton
          variant="primary"
          @click="confirmExport"
          :loading="exporting"
        >
          确认导出
        </AppleButton>
      </template>
    </AppleModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useMessageStore } from '@/stores/messageStore'
import { instance as axiosInstance } from '@/api/axiosInstance'
import AppleButton from '@/components/common/AppleButton.vue'
import AppleInput from '@/components/common/AppleInput.vue'
import AppleSelect from '@/components/common/AppleSelect.vue'
import AppleOption from '@/components/common/AppleOption.vue'
import AppleTable from '@/components/common/AppleTable.vue'
import AppleTag from '@/components/common/AppleTag.vue'
import AppleModal from '@/components/common/AppleModal.vue'

const messageStore = useMessageStore()

// 响应式数据
const loading = ref(false)
const exporting = ref(false)
const cleaning = ref(false)
const logs = ref([])
const availableActions = ref([])
const availableOperators = ref([])
const statistics = ref({
  totalLogs: 0,
  todayLogs: 0,
  errorLogs: 0,
  warningLogs: 0
})
const actionDistribution = ref({})
const hourlyActivity = ref({})

// 分页数据
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: [10, 20, 50, 100]
})

// 筛选条件
const filters = reactive({
  keyword: '',
  action: '',
  operatorId: '',
  ipAddress: '',
  startDate: '',
  endDate: ''
})

// 弹窗控制
const showDetailsModal = ref(false)
const showCleanupModal = ref(false)
const showExportModal = ref(false)
const selectedLog = ref(null)

// 导出设置
const exportFormat = ref('csv')
const exportStartDate = ref('')
const exportEndDate = ref('')

// 清理设置
const cleanupDays = ref(30)

// 表格列配置
const tableColumns = [
  {
    key: 'id',
    title: 'ID',
    width: 80
  },
  {
    key: 'operatorId',
    title: '操作者',
    width: 100
  },
  {
    key: 'action',
    title: '操作类型',
    width: 120
  },
  {
    key: 'details',
    title: '操作详情',
    minWidth: 200
  },
  {
    key: 'ipAddress',
    title: 'IP地址',
    width: 140
  },
  {
    key: 'createdAt',
    title: '时间',
    width: 140
  },
  {
    key: 'actions',
    title: '操作',
    width: 100,
    fixed: 'right'
  }
]

// 计算属性
const maxActionCount = computed(() => {
  const counts = Object.values(actionDistribution.value)
  return counts.length > 0 ? Math.max(...counts) : 1
})

const maxHourlyCount = computed(() => {
  const counts = Object.values(hourlyActivity.value)
  return counts.length > 0 ? Math.max(...counts) : 1
})

// 方法
const getStatistics = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/logs/statistics')
    statistics.value = response.data
  } catch (error) {
    console.error('获取统计数据失败:', error)
    messageStore.addMessage('获取统计数据失败', 'error')
  }
}

const getLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      ...filters
    }

    // 清理空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })

    const response = await axiosInstance.get('/api/admin/logs', { params })
    logs.value = response.data.list
    pagination.total = response.data.total
  } catch (error) {
    console.error('获取日志列表失败:', error)
    messageStore.addMessage('获取日志列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const getAvailableActions = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/logs/actions')
    availableActions.value = response.data
  } catch (error) {
    console.error('获取操作类型失败:', error)
  }
}

const getAvailableOperators = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/logs/operators', {
      params: { days: 30 }
    })
    availableOperators.value = response.data
  } catch (error) {
    console.error('获取操作员列表失败:', error)
  }
}

const getLogDistribution = async () => {
  try {
    const params = {
      startDate: filters.startDate || new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
      endDate: filters.endDate || new Date().toISOString().split('T')[0]
    }

    const response = await axiosInstance.get('/api/admin/logs/distribution', { params })
    actionDistribution.value = response.data.actionDistribution || {}
  } catch (error) {
    console.error('获取日志分布失败:', error)
  }
}

const getHourlyStatistics = async () => {
  try {
    const date = filters.startDate || new Date().toISOString().split('T')[0]
    const response = await axiosInstance.get('/api/admin/logs/hourly', {
      params: { date }
    })
    hourlyActivity.value = response.data.hourlyActivity || {}
  } catch (error) {
    console.error('获取每小时统计失败:', error)
  }
}

const searchLogs = () => {
  pagination.current = 1
  getLogs()
  getLogDistribution()
  getHourlyStatistics()
}

const resetFilters = () => {
  Object.keys(filters).forEach(key => {
    filters[key] = ''
  })
  searchLogs()
}

const handlePageChange = (page) => {
  pagination.current = page
  getLogs()
}

const handlePageSizeChange = (pageSize) => {
  pagination.pageSize = pageSize
  pagination.current = 1
  getLogs()
}

const handleRowClick = (row) => {
  viewLogDetails(row)
}

const viewLogDetails = (log) => {
  selectedLog.value = log
  showDetailsModal.value = true
}

const exportLogs = () => {
  showExportModal.value = true
}

const confirmExport = async () => {
  exporting.value = true
  try {
    const params = {
      format: exportFormat.value,
      startDate: exportStartDate.value || filters.startDate,
      endDate: exportEndDate.value || filters.endDate,
      ...filters
    }

    const response = await axiosInstance.get('/api/admin/logs/export', {
      params,
      responseType: 'blob'
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `system_logs.${exportFormat.value}`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    showExportModal.value = false
    messageStore.addMessage('日志导出成功', 'success')
  } catch (error) {
    console.error('导出日志失败:', error)
    messageStore.addMessage('导出日志失败', 'error')
  } finally {
    exporting.value = false
  }
}

const cleanupLogs = async () => {
  if (cleanupDays.value < 7) {
    messageStore.addMessage('保留天数不能少于7天', 'warning')
    return
  }

  cleaning.value = true
  try {
    const response = await axiosInstance.post('/api/admin/logs/cleanup', null, {
      params: { days: cleanupDays.value }
    })

    messageStore.addMessage(response.data.message, 'success')
    showCleanupModal.value = false
    getLogs()
    getStatistics()
  } catch (error) {
    console.error('清理日志失败:', error)
    messageStore.addMessage('清理日志失败', 'error')
  } finally {
    cleaning.value = false
  }
}

const getActionVariant = (action) => {
  const variants = {
    'LOGIN': 'success',
    'LOGOUT': 'info',
    'CREATE': 'primary',
    'UPDATE': 'warning',
    'DELETE': 'danger',
    'ERROR': 'danger',
    'WARNING': 'warning'
  }
  return variants[action] || 'default'
}

const truncateText = (text, maxLength) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString()
}

const formatTime = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleTimeString()
}

const formatDateTime = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

// 初始化
onMounted(() => {
  getStatistics()
  getLogs()
  getAvailableActions()
  getAvailableOperators()
  getLogDistribution()
  getHourlyStatistics()
})
</script>

<style scoped>
.system-log-viewer {
  padding: 20px;
  background: #f5f5f7;
  min-height: 100vh;
}

/* 统计卡片区域 */
.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: linear-gradient(135deg, #007AFF 0%, #5856D6 100%);
  color: white;
}

.stat-icon.today {
  background: linear-gradient(135deg, #34C759 0%, #30B97B 100%);
}

.stat-icon.error {
  background: linear-gradient(135deg, #FF3B30 0%, #FF6B60 100%);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #FF9500 0%, #FFB347 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1d1d1f;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #86868b;
  margin-top: 4px;
}

/* 筛选区域 */
.filter-section {
  margin-bottom: 24px;
}

.filter-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item label {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
}

.filter-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 表格区域 */
.table-section {
  margin-bottom: 24px;
}

.table-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
}

.table-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.log-id {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  color: #86868b;
}

.log-details {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-time {
  font-size: 12px;
}

.time-detail {
  font-size: 11px;
  color: #86868b;
  margin-top: 2px;
}

/* 图表区域 */
.charts-section {
  margin-bottom: 24px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 16px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.chart-card h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.chart-content {
  min-height: 200px;
}

/* 操作类型分布 */
.action-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-label {
  width: 80px;
  font-size: 12px;
  color: #1d1d1f;
  flex-shrink: 0;
}

.action-bar {
  flex: 1;
  height: 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  overflow: hidden;
}

.action-fill {
  height: 100%;
  background: linear-gradient(90deg, #007AFF 0%, #5856D6 100%);
  border-radius: 4px;
  transition: width 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.action-count {
  width: 40px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #1d1d1f;
  flex-shrink: 0;
}

/* 每小时活动统计 */
.hourly-stats {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160px;
  gap: 4px;
}

.hour-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.hour-label {
  font-size: 10px;
  color: #86868b;
  margin-bottom: 8px;
}

.hour-bar {
  flex: 1;
  width: 100%;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 2px 2px 0 0;
  position: relative;
  min-height: 20px;
}

.hour-fill {
  position: absolute;
  bottom: 0;
  width: 100%;
  background: linear-gradient(180deg, #34C759 0%, #30B97B 100%);
  border-radius: 2px 2px 0 0;
  transition: height 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* 弹窗内容 */
.log-details-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-item.full-width {
  align-items: flex-start;
}

.detail-item label {
  font-weight: 500;
  color: #1d1d1f;
  min-width: 80px;
  flex-shrink: 0;
}

.details-text {
  background: rgba(0, 0, 0, 0.02);
  padding: 12px;
  border-radius: 8px;
  line-height: 1.5;
  max-height: 200px;
  overflow-y: auto;
  flex: 1;
}

/* 清理弹窗 */
.cleanup-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cleanup-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #FF3B30;
  font-weight: 500;
}

.cleanup-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 导出弹窗 */
.export-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.export-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-range span {
  color: #86868b;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-weight: 500;
  color: #1d1d1f;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .system-log-viewer {
    padding: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }

  .table-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .table-actions {
    width: 100%;
    justify-content: space-between;
  }

  .filter-actions {
    justify-content: center;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card,
.filter-card,
.table-card,
.chart-card {
  animation: fadeIn 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .system-log-viewer {
    background: #000000;
  }

  .stat-card,
  .filter-card,
  .table-card,
  .chart-card {
    background: rgba(28, 28, 30, 0.8);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .stat-value,
  .filter-header h3,
  .table-header h3,
  .chart-card h3,
  .detail-item label,
  .form-item label {
    color: #f5f5f7;
  }

  .stat-label,
  .filter-item label,
  .time-detail,
  .hour-label {
    color: #98989f;
  }

  .details-text {
    background: rgba(255, 255, 255, 0.05);
  }
}
</style>