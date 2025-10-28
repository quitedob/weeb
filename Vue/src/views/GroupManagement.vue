<template>
  <div class="group-management">
    <el-card class="header-card">
      <h2>群组管理</h2>
      <p class="subtitle">管理群组成员、权限和申请</p>
    </el-card>

    <!-- 群组信息 -->
    <el-card class="info-card" v-if="groupInfo">
      <template #header>
        <div class="card-header">
          <span>群组信息</span>
          <el-button size="small" @click="editGroupInfo" :icon="Edit">编辑</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="群组名称">{{ groupInfo.groupName }}</el-descriptions-item>
        <el-descriptions-item label="群组ID">{{ groupInfo.id }}</el-descriptions-item>
        <el-descriptions-item label="群主">{{ groupInfo.ownerUsername }}</el-descriptions-item>
        <el-descriptions-item label="成员数">{{ groupInfo.memberCount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ formatDate(groupInfo.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="群组描述" :span="2">
          {{ groupInfo.description || '暂无描述' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 标签页 -->
    <el-card class="tabs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="成员管理" name="members">
          <div class="members-section">
            <!-- 搜索 -->
            <el-input
              v-model="memberSearch"
              placeholder="搜索成员"
              clearable
              style="width: 300px; margin-bottom: 20px"
              :prefix-icon="Search"
            />

            <!-- 成员列表 -->
            <el-table :data="filteredMembers" v-loading="loadingMembers" stripe>
              <el-table-column label="头像" width="80">
                <template #default="scope">
                  <el-avatar :src="scope.row.avatar" />
                </template>
              </el-table-column>
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column label="角色" width="120">
                <template #default="scope">
                  <el-tag :type="getRoleType(scope.row.role)">
                    {{ getRoleName(scope.row.role) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="加入时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.joinedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="scope">
                  <el-button
                    v-if="canChangeRole(scope.row)"
                    size="small"
                    @click="changeRole(scope.row)"
                    :icon="User"
                  >
                    设置角色
                  </el-button>
                  <el-button
                    v-if="canRemoveMember(scope.row)"
                    size="small"
                    type="danger"
                    @click="removeMember(scope.row)"
                    :icon="Delete"
                  >
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane name="applications">
          <template #label>
            <span>
              加入申请
              <el-badge :value="pendingCount" v-if="pendingCount > 0" class="badge" />
            </span>
          </template>
          <div class="applications-section">
            <el-table :data="applications" v-loading="loadingApplications" stripe>
              <el-table-column label="申请人" width="150">
                <template #default="scope">
                  <div class="user-info">
                    <el-avatar :src="scope.row.avatar" size="small" />
                    <span>{{ scope.row.username }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="申请理由" min-width="200" />
              <el-table-column label="申请时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusName(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.status === 'PENDING'"
                    size="small"
                    type="success"
                    @click="approveApplication(scope.row)"
                    :icon="Check"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="scope.row.status === 'PENDING'"
                    size="small"
                    type="danger"
                    @click="rejectApplication(scope.row)"
                    :icon="Close"
                  >
                    拒绝
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="权限设置" name="permissions">
          <div class="permissions-section">
            <el-form :model="permissionForm" label-width="150px">
              <el-form-item label="允许成员邀请">
                <el-switch v-model="permissionForm.allowMemberInvite" />
              </el-form-item>
              <el-form-item label="允许成员发言">
                <el-switch v-model="permissionForm.allowMemberSpeak" />
              </el-form-item>
              <el-form-item label="需要审批加入">
                <el-switch v-model="permissionForm.requireApproval" />
              </el-form-item>
              <el-form-item label="群组公开">
                <el-switch v-model="permissionForm.isPublic" />
              </el-form-item>
              <el-form-item label="最大成员数">
                <el-input-number
                  v-model="permissionForm.maxMembers"
                  :min="10"
                  :max="500"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="savePermissions" :loading="saving">
                  保存设置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="统计信息" name="statistics">
          <div class="statistics-section">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-statistic title="总成员数" :value="stats.totalMembers" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="今日活跃" :value="stats.todayActive" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="本周新增" :value="stats.weeklyNew" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="待处理申请" :value="stats.pendingApplications" />
              </el-col>
            </el-row>

            <el-divider />

            <h3>成员活跃度</h3>
            <el-table :data="stats.memberActivity" stripe>
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="messageCount" label="消息数" />
              <el-table-column prop="lastActive" label="最后活跃">
                <template #default="scope">
                  {{ formatDate(scope.row.lastActive) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 角色设置对话框 -->
    <el-dialog v-model="roleDialogVisible" title="设置成员角色" width="400px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="用户">
          <el-input v-model="currentMember.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.role" placeholder="选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通成员" value="MEMBER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmChangeRole" :loading="saving">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Edit,
  User,
  Delete,
  Check,
  Close
} from '@element-plus/icons-vue'
import groupApi from '@/api/modules/group'

const route = useRoute()
const groupId = ref(route.params.groupId)

// 数据
const activeTab = ref('members')
const groupInfo = ref(null)
const members = ref([])
const applications = ref([])
const memberSearch = ref('')
const loadingMembers = ref(false)
const loadingApplications = ref(false)
const saving = ref(false)

const roleDialogVisible = ref(false)
const currentMember = ref({})
const roleForm = ref({ role: '' })

const permissionForm = ref({
  allowMemberInvite: true,
  allowMemberSpeak: true,
  requireApproval: true,
  isPublic: false,
  maxMembers: 200
})

const stats = ref({
  totalMembers: 0,
  todayActive: 0,
  weeklyNew: 0,
  pendingApplications: 0,
  memberActivity: []
})

// 计算属性
const filteredMembers = computed(() => {
  if (!memberSearch.value) return members.value
  return members.value.filter(m =>
    m.username.toLowerCase().includes(memberSearch.value.toLowerCase())
  )
})

const pendingCount = computed(() => {
  return applications.value.filter(a => a.status === 'PENDING').length
})

// 方法
const loadGroupInfo = async () => {
  try {
    const response = await groupApi.getGroupById(groupId.value)
    if (response.data.success) {
      groupInfo.value = response.data.data
    }
  } catch (error) {
    console.error('加载群组信息失败:', error)
  }
}

const loadMembers = async () => {
  loadingMembers.value = true
  try {
    const response = await groupApi.getGroupMembers(groupId.value)
    if (response.data.success) {
      members.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载成员列表失败:', error)
  } finally {
    loadingMembers.value = false
  }
}

const loadApplications = async () => {
  loadingApplications.value = true
  try {
    const response = await groupApi.getGroupApplications(groupId.value)
    if (response.data.success) {
      applications.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载申请列表失败:', error)
  } finally {
    loadingApplications.value = false
  }
}

const loadStatistics = async () => {
  try {
    const response = await groupApi.getGroupStatistics(groupId.value)
    if (response.data.success) {
      stats.value = response.data.data
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const canChangeRole = (member) => {
  return member.role !== 'OWNER'
}

const canRemoveMember = (member) => {
  return member.role !== 'OWNER'
}

const changeRole = (member) => {
  currentMember.value = member
  roleForm.value.role = member.role
  roleDialogVisible.value = true
}

const confirmChangeRole = async () => {
  saving.value = true
  try {
    const response = await groupApi.updateMemberRole(
      groupId.value,
      currentMember.value.userId,
      roleForm.value.role
    )

    if (response.data.success) {
      ElMessage.success('角色设置成功')
      roleDialogVisible.value = false
      loadMembers()
    } else {
      ElMessage.error(response.data.message || '设置失败')
    }
  } catch (error) {
    console.error('设置角色失败:', error)
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
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

    const response = await groupApi.removeMember(groupId.value, member.userId)

    if (response.data.success) {
      ElMessage.success('成员已移除')
      loadMembers()
    } else {
      ElMessage.error(response.data.message || '移除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const approveApplication = async (application) => {
  try {
    const response = await groupApi.approveApplication(
      groupId.value,
      application.id
    )

    if (response.data.success) {
      ElMessage.success('已通过申请')
      loadApplications()
      loadMembers()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('通过申请失败:', error)
    ElMessage.error('操作失败')
  }
}

const rejectApplication = async (application) => {
  try {
    const response = await groupApi.rejectApplication(
      groupId.value,
      application.id
    )

    if (response.data.success) {
      ElMessage.success('已拒绝申请')
      loadApplications()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('拒绝申请失败:', error)
    ElMessage.error('操作失败')
  }
}

const savePermissions = async () => {
  saving.value = true
  try {
    const response = await groupApi.updateGroupPermissions(
      groupId.value,
      permissionForm.value
    )

    if (response.data.success) {
      ElMessage.success('权限设置已保存')
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const editGroupInfo = () => {
  ElMessage.info('编辑功能开发中...')
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
  loadGroupInfo()
  loadMembers()
  loadApplications()
  loadStatistics()
})
</script>

<style scoped>
.group-management {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0 0 10px 0;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.info-card {
  margin-bottom: 20px;
}

.tabs-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge {
  margin-left: 5px;
}

.statistics-section {
  padding: 20px;
}
</style>
