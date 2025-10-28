<template>
  <div class="group-member-manager">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>成员管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索成员"
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
            <el-button @click="loadMembers" :icon="Refresh">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 成员列表 -->
      <el-table :data="filteredMembers" v-loading="loading" stripe>
        <el-table-column label="成员" width="200">
          <template #default="scope">
            <div class="member-info">
              <el-avatar :src="scope.row.avatar" :size="40" />
              <div class="member-details">
                <div class="member-name">{{ scope.row.username }}</div>
                <div class="member-level">
                  等级 {{ scope.row.userLevel || 1 }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="角色" width="120">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)" size="small">
              {{ getRoleName(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="加入时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.joinedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="最后活跃" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.lastActive) }}
          </template>
        </el-table-column>
        
        <el-table-column label="消息数" width="100">
          <template #default="scope">
            {{ scope.row.messageCount || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-dropdown
              v-if="canManageMember(scope.row)"
              @command="(command) => handleMemberAction(command, scope.row)"
            >
              <el-button size="small" :icon="Setting">
                管理
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="scope.row.role !== 'ADMIN'"
                    command="promote"
                    :icon="Top"
                  >
                    设为管理员
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="scope.row.role === 'ADMIN'"
                    command="demote"
                    :icon="Bottom"
                  >
                    取消管理员
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="mute"
                    :icon="Mute"
                    divided
                  >
                    禁言
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="remove"
                    :icon="Delete"
                    class="danger-item"
                  >
                    移除成员
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            
            <el-button
              v-else
              size="small"
              @click="viewMemberProfile(scope.row)"
              :icon="View"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadMembers"
        @current-change="loadMembers"
        class="pagination"
      />
    </el-card>

    <!-- 禁言对话框 -->
    <el-dialog v-model="muteDialogVisible" title="禁言设置" width="400px">
      <el-form :model="muteForm" label-width="80px">
        <el-form-item label="用户">
          <el-input v-model="currentMember.username" disabled />
        </el-form-item>
        <el-form-item label="禁言时长">
          <el-select v-model="muteForm.duration" placeholder="选择时长">
            <el-option label="10分钟" :value="10" />
            <el-option label="1小时" :value="60" />
            <el-option label="1天" :value="1440" />
            <el-option label="7天" :value="10080" />
            <el-option label="永久" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="禁言原因">
          <el-input
            v-model="muteForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入禁言原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="muteDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmMute" :loading="processing">
          确认禁言
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Setting,
  Top,
  Bottom,
  Mute,
  Delete,
  View,
  ArrowDown
} from '@element-plus/icons-vue'
import groupApi from '@/api/modules/group'

const props = defineProps({
  groupId: {
    type: [Number, String],
    required: true
  },
  currentUserRole: {
    type: String,
    default: 'MEMBER'
  }
})

const emit = defineEmits(['memberUpdated'])

// 数据
const members = ref([])
const loading = ref(false)
const processing = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const muteDialogVisible = ref(false)
const currentMember = ref({})
const muteForm = ref({
  duration: 60,
  reason: ''
})

// 计算属性
const filteredMembers = computed(() => {
  if (!searchKeyword.value) return members.value
  return members.value.filter(member =>
    member.username.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 方法
const loadMembers = async () => {
  loading.value = true
  try {
    const response = await groupApi.getGroupMembers(props.groupId, {
      page: currentPage.value,
      pageSize: pageSize.value
    })

    if (response.data.success) {
      members.value = response.data.data.list || []
      total.value = response.data.data.total || 0
    }
  } catch (error) {
    console.error('加载成员列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const canManageMember = (member) => {
  // 只有群主和管理员可以管理成员，且不能管理群主
  return (
    (props.currentUserRole === 'OWNER' || props.currentUserRole === 'ADMIN') &&
    member.role !== 'OWNER'
  )
}

const handleMemberAction = async (command, member) => {
  currentMember.value = member
  
  switch (command) {
    case 'promote':
      await promoteMember(member)
      break
    case 'demote':
      await demoteMember(member)
      break
    case 'mute':
      showMuteDialog(member)
      break
    case 'remove':
      await removeMember(member)
      break
  }
}

const promoteMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.username} 设为管理员吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await groupApi.updateMemberRole(
      props.groupId,
      member.userId,
      'ADMIN'
    )

    if (response.data.success) {
      ElMessage.success('已设为管理员')
      member.role = 'ADMIN'
      emit('memberUpdated', member)
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('设置管理员失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const demoteMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消 ${member.username} 的管理员权限吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await groupApi.updateMemberRole(
      props.groupId,
      member.userId,
      'MEMBER'
    )

    if (response.data.success) {
      ElMessage.success('已取消管理员权限')
      member.role = 'MEMBER'
      emit('memberUpdated', member)
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消管理员失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const showMuteDialog = (member) => {
  currentMember.value = member
  muteForm.value = {
    duration: 60,
    reason: ''
  }
  muteDialogVisible.value = true
}

const confirmMute = async () => {
  if (!muteForm.value.reason.trim()) {
    ElMessage.warning('请输入禁言原因')
    return
  }

  processing.value = true
  try {
    const response = await groupApi.muteMember(
      props.groupId,
      currentMember.value.userId,
      {
        duration: muteForm.value.duration,
        reason: muteForm.value.reason
      }
    )

    if (response.data.success) {
      ElMessage.success('禁言设置成功')
      muteDialogVisible.value = false
      loadMembers()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('禁言失败:', error)
    ElMessage.error('操作失败')
  } finally {
    processing.value = false
  }
}

const removeMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除成员 ${member.username} 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await groupApi.removeMember(props.groupId, member.userId)

    if (response.data.success) {
      ElMessage.success('成员已移除')
      loadMembers()
      emit('memberUpdated', member)
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const viewMemberProfile = (member) => {
  // 跳转到用户资料页面
  window.open(`/user/${member.userId}`, '_blank')
}

const getRoleName = (role) => {
  const roles = {
    OWNER: '群主',
    ADMIN: '管理员',
    MEMBER: '成员'
  }
  return roles[role] || role
}

const getRoleType = (role) => {
  const types = {
    OWNER: 'danger',
    ADMIN: 'warning',
    MEMBER: 'info'
  }
  return types[role] || ''
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadMembers()
})

// 暴露方法供父组件调用
defineExpose({
  loadMembers
})
</script>

<style scoped>
.group-member-manager {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-details {
  flex: 1;
}

.member-name {
  font-weight: 500;
  color: #303133;
}

.member-level {
  font-size: 12px;
  color: #909399;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}

.danger-item {
  color: #F56C6C;
}
</style>
