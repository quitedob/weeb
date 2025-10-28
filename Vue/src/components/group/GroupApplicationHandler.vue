<template>
  <div class="group-application-handler">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>群组申请</span>
          <el-badge :value="pendingCount" v-if="pendingCount > 0" />
        </div>
      </template>

      <!-- 申请列表 -->
      <div v-if="applications.length === 0" class="empty-state">
        <el-empty description="暂无申请" />
      </div>

      <div v-else class="application-list">
        <div
          v-for="application in applications"
          :key="application.id"
          class="application-item"
        >
          <div class="application-content">
            <div class="user-info">
              <el-avatar :src="application.userAvatar" :size="50" />
              <div class="user-details">
                <div class="username">{{ application.username }}</div>
                <div class="apply-time">{{ formatDate(application.createdAt) }}</div>
              </div>
            </div>

            <div class="application-message">
              <p class="message-label">申请理由：</p>
              <p class="message-content">{{ application.message || '无' }}</p>
            </div>

            <div class="application-actions">
              <el-button
                v-if="application.status === 'PENDING'"
                type="success"
                size="small"
                @click="handleApprove(application)"
                :loading="application.processing"
                :icon="Check"
              >
                通过
              </el-button>
              <el-button
                v-if="application.status === 'PENDING'"
                type="danger"
                size="small"
                @click="handleReject(application)"
                :loading="application.processing"
                :icon="Close"
              >
                拒绝
              </el-button>
              <el-tag
                v-else
                :type="getStatusType(application.status)"
                size="small"
              >
                {{ getStatusName(application.status) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadApplications"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import groupApi from '@/api/modules/group'

const props = defineProps({
  groupId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['applicationProcessed'])

// 数据
const applications = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 计算属性
const pendingCount = computed(() => {
  return applications.value.filter(app => app.status === 'PENDING').length
})

// 方法
const loadApplications = async () => {
  loading.value = true
  try {
    const response = await groupApi.getGroupApplications(props.groupId, {
      page: currentPage.value,
      pageSize: pageSize.value
    })

    if (response.data.success) {
      applications.value = (response.data.data.list || []).map(app => ({
        ...app,
        processing: false
      }))
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载申请列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (application) => {
  application.processing = true
  try {
    const response = await groupApi.approveApplication(
      props.groupId,
      application.id
    )

    if (response.data.success) {
      ElMessage.success(`已通过 ${application.username} 的申请`)
      application.status = 'APPROVED'
      emit('applicationProcessed', {
        type: 'approve',
        application
      })
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('通过申请失败:', error)
    ElMessage.error('操作失败')
  } finally {
    application.processing = false
  }
}

const handleReject = async (application) => {
  application.processing = true
  try {
    const response = await groupApi.rejectApplication(
      props.groupId,
      application.id
    )

    if (response.data.success) {
      ElMessage.success(`已拒绝 ${application.username} 的申请`)
      application.status = 'REJECTED'
      emit('applicationProcessed', {
        type: 'reject',
        application
      })
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('拒绝申请失败:', error)
    ElMessage.error('操作失败')
  } finally {
    application.processing = false
  }
}

const getStatusName = (status) => {
  const statuses = {
    PENDING: '待处理',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return statuses[status] || status
}

const getStatusType = (status) => {
  const types = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return types[status] || ''
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadApplications()
})

// 暴露方法供父组件调用
defineExpose({
  loadApplications
})
</script>

<style scoped>
.group-application-handler {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  padding: 40px 0;
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.application-item {
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 15px;
  transition: all 0.3s;
}

.application-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.application-content {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 200px;
}

.user-details {
  flex: 1;
}

.username {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.apply-time {
  font-size: 12px;
  color: #909399;
}

.application-message {
  flex: 1;
  margin-right: 15px;
}

.message-label {
  font-size: 14px;
  color: #606266;
  margin: 0 0 5px 0;
}

.message-content {
  font-size: 14px;
  color: #303133;
  margin: 0;
  line-height: 1.5;
}

.application-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 100px;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
