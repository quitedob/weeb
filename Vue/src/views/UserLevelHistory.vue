<template>
  <div class="user-level-history">
    <div class="page-header">
      <h1>用户等级历史</h1>
      <p>查看您的等级变更记录和成长轨迹</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon current-level">
            <el-icon><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ currentLevelName }}</h3>
            <p>当前等级</p>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon total-changes">
            <el-icon><Refresh /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ totalChanges }}</h3>
            <p>等级变更次数</p>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon upgrade-progress">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ upgradeProgress }}%</h3>
            <p>下一等级进度</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 升级进度条 -->
    <el-card class="progress-card" v-if="upgradeProgress > 0 && upgradeProgress < 100">
      <h3>升级进度</h3>
      <div class="progress-info">
        <span>{{ currentLevelName }} → {{ nextLevelName }}</span>
        <span>{{ upgradeProgress }}%</span>
      </div>
      <el-progress
        :percentage="upgradeProgress"
        :color="progressColor"
        :stroke-width="12"
      />
      <div class="progress-requirements" v-if="upgradeRequirements">
        <p>升级要求：</p>
        <div class="requirement-item" v-for="(value, key) in upgradeRequirements" :key="key">
          <span>{{ formatRequirementName(key) }}: {{ value }}</span>
          <el-progress
            :percentage="getRequirementProgress(key, value)"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </div>
    </el-card>

    <!-- 等级历史记录 -->
    <el-card class="history-card">
      <div class="card-header">
        <h3>等级变更历史</h3>
        <div class="header-actions">
          <el-button type="primary" @click="refreshHistory">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <el-table
        :data="levelHistory"
        v-loading="loading"
        empty-text="暂无等级变更记录"
        class="history-table"
      >
        <el-table-column label="变更时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.changeTime) }}
          </template>
        </el-table-column>

        <el-table-column label="变更类型" width="120">
          <template #default="{ row }">
            <el-tag
              :type="getChangeTypeTagType(row.changeType)"
              size="small"
            >
              {{ getChangeTypeName(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="等级变化" width="180">
          <template #default="{ row }">
            <div class="level-change">
              <span class="old-level" :style="{ color: getLevelColor(row.oldLevel) }">
                {{ getLevelName(row.oldLevel) }}
              </span>
              <el-icon class="change-icon">
                <Right />
              </el-icon>
              <span class="new-level" :style="{ color: getLevelColor(row.newLevel) }">
                {{ getLevelName(row.newLevel) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="变更原因" prop="changeReason" />

        <el-table-column label="操作者" width="120">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <el-tag v-else type="info" size="small">系统</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              type="text"
              size="small"
              @click="viewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="等级变更详情"
      width="600px"
    >
      <div v-if="selectedRecord" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="变更时间">
            {{ formatDateTime(selectedRecord.changeTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="变更类型">
            <el-tag :type="getChangeTypeTagType(selectedRecord.changeType)">
              {{ getChangeTypeName(selectedRecord.changeType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="原等级">
            <span :style="{ color: getLevelColor(selectedRecord.oldLevel) }">
              {{ getLevelName(selectedRecord.oldLevel) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="新等级">
            <span :style="{ color: getLevelColor(selectedRecord.newLevel) }">
              {{ getLevelName(selectedRecord.newLevel) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="操作者" span="2">
            {{ selectedRecord.operatorName || '系统自动' }}
          </el-descriptions-item>
          <el-descriptions-item label="变更原因" span="2">
            {{ selectedRecord.changeReason || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="IP地址" span="2" v-if="selectedRecord.ipAddress">
            {{ selectedRecord.ipAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" span="2" v-if="selectedRecord.remark">
            {{ selectedRecord.remark }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Trophy, Refresh, TrendCharts, Right
} from '@element-plus/icons-vue'
import { userLevelApi } from '@/api/modules/user'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()

// 响应式数据
const loading = ref(false)
const levelHistory = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 用户等级信息
const currentLevel = ref(1)
const currentLevelName = ref('新用户')
const totalChanges = ref(0)
const upgradeProgress = ref(0)
const upgradeRequirements = ref(null)

// 详情对话框
const detailDialogVisible = ref(false)
const selectedRecord = ref(null)

// 等级颜色映射
const levelColors = {
  1: '#909399', // 新用户 - 灰色
  2: '#409EFF', // 普通用户 - 蓝色
  3: '#67C23A', // 活跃用户 - 绿色
  4: '#E6A23C', // 资深用户 - 橙色
  5: '#F56C6C', // 版主 - 红色
  6: '#B37FEB'  // 管理员 - 紫色
}

// 计算属性
const nextLevelName = computed(() => {
  const nextLevel = currentLevel.value + 1
  return getLevelName(nextLevel)
})

const progressColor = computed(() => {
  if (upgradeProgress.value >= 80) return '#67C23A'
  if (upgradeProgress.value >= 50) return '#E6A23C'
  return '#409EFF'
})

// 等级名称映射
const getLevelName = (level) => {
  const levelNames = {
    1: '新用户',
    2: '普通用户',
    3: '活跃用户',
    4: '资深用户',
    5: '版主',
    6: '管理员'
  }
  return levelNames[level] || '未知等级'
}

// 获取等级颜色
const getLevelColor = (level) => {
  return levelColors[level] || '#909399'
}

// 变更类型名称
const getChangeTypeName = (type) => {
  const typeNames = {
    1: '系统自动',
    2: '管理员操作',
    3: '用户行为触发',
    4: '系统调整'
  }
  return typeNames[type] || '未知'
}

// 变更类型标签颜色
const getChangeTypeTagType = (type) => {
  const typeColors = {
    1: 'success', // 系统自动 - 绿色
    2: 'warning', // 管理员操作 - 橙色
    3: 'primary', // 用户行为触发 - 蓝色
    4: 'info'     // 系统调整 - 灰色
  }
  return typeColors[type] || 'info'
}

// 格式化时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 格式化要求名称
const formatRequirementName = (key) => {
  const names = {
    minArticles: '文章数',
    minMessages: '消息数',
    minLoginDays: '登录天数',
    minLikes: '获赞数',
    minFollowers: '关注者'
  }
  return names[key] || key
}

// 获取要求进度
const getRequirementProgress = (key, required) => {
  // 这里应该从用户统计数据中获取实际值
  // 暂时返回模拟数据
  return Math.random() * 100
}

// 加载等级历史
const loadLevelHistory = async () => {
  try {
    loading.value = true
    const response = await userLevelApi.getLevelHistory({
      page: currentPage.value,
      pageSize: pageSize.value
    })

    if (response.success) {
      levelHistory.value = response.data.records || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error(response.message || '加载等级历史失败')
    }
  } catch (error) {
    console.error('加载等级历史失败:', error)
    ElMessage.error('加载等级历史失败')
  } finally {
    loading.value = false
  }
}

// 加载用户等级信息
const loadUserLevelInfo = async () => {
  try {
    const response = await userLevelApi.getUserLevelInfo()

    if (response.success) {
      currentLevel.value = response.data.currentLevel
      currentLevelName.value = getLevelName(currentLevel.value)
      totalChanges.value = response.data.totalChanges || 0

      // 加载升级进度
      const progressResponse = await userLevelApi.getUpgradeProgress()
      if (progressResponse.success) {
        upgradeProgress.value = Math.round(progressResponse.data.overallProgress || 0)
        upgradeRequirements.value = progressResponse.data.requirements || null
      }
    }
  } catch (error) {
    console.error('加载用户等级信息失败:', error)
  }
}

// 刷新历史记录
const refreshHistory = () => {
  loadLevelHistory()
  loadUserLevelInfo()
}

// 查看详情
const viewDetail = (record) => {
  selectedRecord.value = record
  detailDialogVisible.value = true
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  loadLevelHistory()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadLevelHistory()
}

// 生命周期
onMounted(() => {
  loadUserLevelInfo()
  loadLevelHistory()
})
</script>

<style scoped>
.user-level-history {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 2.5em;
  color: #303133;
  margin-bottom: 10px;
}

.page-header p {
  color: #606266;
  font-size: 1.1em;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.stat-icon.current-level {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.total-changes {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.upgrade-progress {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-info h3 {
  font-size: 1.8em;
  color: #303133;
  margin: 0 0 5px 0;
}

.stat-info p {
  color: #909399;
  margin: 0;
}

.progress-card {
  margin-bottom: 30px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.progress-card h3 {
  margin-bottom: 15px;
  color: #303133;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
}

.progress-requirements {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.progress-requirements p {
  color: #303133;
  margin-bottom: 15px;
  font-weight: 500;
}

.requirement-item {
  margin-bottom: 10px;
}

.requirement-item span {
  display: block;
  margin-bottom: 5px;
  color: #606266;
  font-size: 0.9em;
}

.history-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  color: #303133;
  margin: 0;
}

.history-table {
  margin-bottom: 20px;
}

.level-change {
  display: flex;
  align-items: center;
  gap: 8px;
}

.change-icon {
  color: #909399;
  font-size: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  padding: 20px 0;
}

@media (max-width: 768px) {
  .user-level-history {
    padding: 10px;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .page-header h1 {
    font-size: 2em;
  }
}
</style>