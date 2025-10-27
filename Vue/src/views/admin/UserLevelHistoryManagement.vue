<template>
  <div class="level-history-management">
    <el-card class="header-card">
      <h2>用户等级历史管理</h2>
      <p class="subtitle">查看和管理用户等级变更记录</p>
    </el-card>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :model="queryForm" :inline="true">
        <el-form-item label="用户ID">
          <el-input
            v-model="queryForm.userId"
            placeholder="输入用户ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="queryForm.changeType" placeholder="选择类型" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="系统自动" :value="1" />
            <el-option label="管理员操作" :value="2" />
            <el-option label="用户行为触发" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史记录列表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>等级变更记录</span>
          <div>
            <el-button @click="exportData">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="historyList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="用户名" width="120">
          <template #default="scope">
            {{ scope.row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="等级变化" width="200">
          <template #default="scope">
            <el-tag :type="getLevelChangeType(scope.row)">
              {{ scope.row.oldLevel }} → {{ scope.row.newLevel }}
            </el-tag>
            <el-icon v-if="scope.row.newLevel > scope.row.oldLevel" color="#67C23A">
              <Top />
            </el-icon>
            <el-icon v-else-if="scope.row.newLevel < scope.row.oldLevel" color="#F56C6C">
              <Bottom />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column label="变更类型" width="120">
          <template #default="scope">
            <el-tag :type="getChangeTypeTag(scope.row.changeType)">
              {{ getChangeTypeName(scope.row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更原因" min-width="200" />
        <el-table-column label="操作者" width="120">
          <template #default="scope">
            {{ scope.row.operatorName || '系统' }}
          </template>
        </el-table-column>
        <el-table-column label="变更时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.changeTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewDetail(scope.row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="等级变更详情" width="50%">
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRecord.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentRecord.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原等级">{{ currentRecord.oldLevel }}</el-descriptions-item>
        <el-descriptions-item label="新等级">{{ currentRecord.newLevel }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">
          {{ getChangeTypeName(currentRecord.changeType) }}
        </el-descriptions-item>
        <el-descriptions-item label="变更原因" :span="2">
          {{ currentRecord.changeReason }}
        </el-descriptions-item>
        <el-descriptions-item label="操作者">
          {{ currentRecord.operatorName || '系统' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作者ID">
          {{ currentRecord.operatorId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          {{ currentRecord.ipAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">
          {{ currentRecord.userAgent || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="变更时间" :span="2">
          {{ formatDate(currentRecord.changeTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentRecord.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, Top, Bottom } from '@element-plus/icons-vue'
import axios from '@/api/axiosInstance'

// 查询表单
const queryForm = ref({
  userId: null,
  changeType: null,
  startTime: null,
  endTime: null,
  page: 1,
  pageSize: 10
})

// 数据
const historyList = ref([])
const total = ref(0)
const dateRange = ref([])
const currentRecord = ref(null)

// 状态
const loading = ref(false)
const detailDialogVisible = ref(false)

// 搜索
const handleSearch = async () => {
  loading.value = true
  try {
    // 处理时间范围
    if (dateRange.value && dateRange.value.length === 2) {
      queryForm.value.startTime = dateRange.value[0]
      queryForm.value.endTime = dateRange.value[1]
    } else {
      queryForm.value.startTime = null
      queryForm.value.endTime = null
    }

    const response = await axios.post('/api/user-level-history/query', queryForm.value)
    if (response.data.success) {
      historyList.value = response.data.data.list || []
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('查询失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  queryForm.value = {
    userId: null,
    changeType: null,
    startTime: null,
    endTime: null,
    page: 1,
    pageSize: 10
  }
  dateRange.value = []
  handleSearch()
}

// 查看详情
const viewDetail = (record) => {
  currentRecord.value = record
  detailDialogVisible.value = true
}

// 导出数据
const exportData = () => {
  ElMessage.info('导出功能开发中...')
}

// 获取变更类型名称
const getChangeTypeName = (type) => {
  const types = {
    1: '系统自动',
    2: '管理员操作',
    3: '用户行为触发'
  }
  return types[type] || '未知'
}

// 获取变更类型标签
const getChangeTypeTag = (type) => {
  const tags = {
    1: 'info',
    2: 'warning',
    3: 'success'
  }
  return tags[type] || ''
}

// 获取等级变化类型
const getLevelChangeType = (record) => {
  if (record.newLevel > record.oldLevel) return 'success'
  if (record.newLevel < record.oldLevel) return 'danger'
  return 'info'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.level-history-management {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 20px;
}

.content-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
