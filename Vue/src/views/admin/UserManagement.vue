<template>
  <div class="user-management">
    <div class="page-header">
      <div class="header-content">
        <h1>用户管理</h1>
        <p class="subtitle">管理系统用户和权限分配</p>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="exportUsers">
          <el-icon><Download /></el-icon>
          导出用户
        </el-button>
        <el-button type="primary" @click="showBatchAssignDialog = true">
          <el-icon><UserSolid /></el-icon>
          批量分配角色
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <el-card>
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="用户名">
            <el-input
              v-model="searchForm.username"
              placeholder="请输入用户名"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="用户类型">
            <el-select v-model="searchForm.type" placeholder="请选择用户类型" clearable>
              <el-option label="普通用户" :value="1" />
              <el-option label="管理员" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="在线状态">
            <el-select v-model="searchForm.onlineStatus" placeholder="请选择在线状态" clearable>
              <el-option label="在线" :value="1" />
              <el-option label="离线" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="注册时间">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 用户列表 -->
    <div class="user-list">
      <el-card>
        <el-table
          v-loading="loading"
          :data="userList"
          stripe
          border
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column prop="userEmail" label="邮箱" width="200" />
          <el-table-column prop="type" label="用户类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 0 ? 'danger' : 'primary'">
                {{ row.type === 0 ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="onlineStatus" label="在线状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.onlineStatus === 1 ? 'success' : 'info'">
                {{ row.onlineStatus === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registrationDate" label="注册时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.registrationDate) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button size="small" @click="handleRole(row)">角色分配</el-button>
              <el-button size="small" type="danger" @click="handleBan(row)">
                {{ row.banned ? '解封' : '封禁' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="showUserDialog"
      title="用户详情"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="currentUser" class="user-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.userEmail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phoneNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ getGenderLabel(currentUser.sex) }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag :type="currentUser.type === 0 ? 'danger' : 'primary'">
              {{ currentUser.type === 0 ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="在线状态">
            <el-tag :type="currentUser.onlineStatus === 1 ? 'success' : 'info'">
              {{ currentUser.onlineStatus === 1 ? '在线' : '离线' }}
            </el-descriptions-item>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(currentUser.registrationDate) }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ formatDate(currentUser.loginTime) }}</el-descriptions-item>
          <el-descriptions-item label="粉丝数" :span="2">{{ currentUser.fansCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="总点赞数" :span="2">{{ currentUser.totalLikes || 0 }}</el-descriptions-item>
        </el-descriptions>

        <!-- 用户角色 -->
        <div class="user-roles">
          <h4>用户角色</h4>
          <el-tag
            v-for="role in currentUser.roles"
            :key="role.id"
            class="role-tag"
            :type="role.type === 0 ? 'danger' : 'primary'"
          >
            {{ role.name }}
          </el-tag>
        </div>
      </div>
    </el-dialog>

    <!-- 批量分配角色对话框 -->
    <el-dialog
      v-model="showBatchAssignDialog"
      title="批量分配角色"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="batch-assign">
        <div class="selected-info">
          <p>已选择 {{ selectedUsers.length }} 个用户</p>
        </div>

        <el-form ref="batchFormRef" :model="batchForm" label-width="100px">
          <el-form-item label="选择角色" prop="roleId">
            <el-select v-model="batchForm.roleId" placeholder="请选择要分配的角色">
              <el-option
                v-for="role in availableRoles"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showBatchAssignDialog = false">取消</el-button>
          <el-button type="primary" @click="handleBatchAssign">确定分配</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 单个用户角色分配对话框 -->
    <el-dialog
      v-model="showRoleAssignDialog"
      title="角色分配"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="role-assign">
        <div class="user-info">
          <h4>{{ currentUser?.username }}</h4>
        </div>

        <el-transfer
          v-model="assignedRoleIds"
          :data="availableRoles"
          :titles="['可选角色', '已分配角色']"
          :props="{
            key: 'id',
            label: 'name'
          }"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showRoleAssignDialog = false">取消</el-button>
          <el-button type="primary" @click="handleRoleAssign">保存分配</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, UserSolid } from '@element-plus/icons-vue'
import adminApi from '@/api/modules/admin'

// 响应式数据
const loading = ref(false)
const showUserDialog = ref(false)
const showBatchAssignDialog = ref(false)
const showRoleAssignDialog = ref(false)
const userList = ref([])
const availableRoles = ref([])
const selectedUsers = ref([])
const currentUser = ref(null)
const assignedRoleIds = ref([])

const searchForm = reactive({
  username: '',
  type: null,
  onlineStatus: null,
  dateRange: []
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const batchForm = reactive({
  roleId: ''
})

// 获取用户列表
const fetchUsers = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      pageSize: pagination.size,
      keyword: searchForm.username || undefined,
      // 注意：后端的getUsersWithPaging目前只支持keyword，其他筛选条件需要后端实现
      // type: searchForm.type,
      // onlineStatus: searchForm.onlineStatus,
      // startDate: searchForm.dateRange?.[0] || undefined,
      // endDate: searchForm.dateRange?.[1] || undefined
    }

    const response = await adminApi.getUsers(params)
    userList.value = response.data.list || []
    pagination.total = response.data.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取可用角色列表
const fetchRoles = async () => {
  try {
    // 获取所有角色用于分配，可以不分页或设置一个较大的pageSize
    const response = await adminApi.getRoles({ page: 1, pageSize: 1000 })
    availableRoles.value = response.data.list || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchUsers()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    username: '',
    type: null,
    onlineStatus: null,
    dateRange: []
  })
  handleSearch()
}

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchUsers()
}

// 当前页改变
const handleCurrentChange = (current) => {
  pagination.current = current
  fetchUsers()
}

// 多选变化
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// 查看用户详情
const handleView = (row) => {
  currentUser.value = row
  showUserDialog.value = true
}

// 封禁/解封用户
const handleBan = async (row) => {
  const action = row.banned ? '解封' : '封禁'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户"${row.username}"吗？`,
      `${action}确认`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    if (row.banned) {
      await adminApi.unbanUser(row.id)
    } else {
      await adminApi.banUser(row.id)
    }
    
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 角色分配
const handleRole = (row) => {
  currentUser.value = row
  assignedRoleIds.value = row.roles?.map(r => r.id) || []
  showRoleAssignDialog.value = true
}

// 导出用户
const exportUsers = () => {
  ElMessage.info('导出功能正在开发中...')
}

// 批量分配角色
const handleBatchAssign = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请选择要分配角色的用户')
    return
  }

  if (!batchForm.roleId) {
    ElMessage.warning('请选择要分配的角色')
    return
  }

  try {
    const userIds = selectedUsers.value.map(u => u.id)
    for (const userId of userIds) {
      await adminApi.assignRoleToUser(userId, batchForm.roleId)
    }
    ElMessage.success('批量分配角色成功')
    showBatchAssignDialog.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('批量分配角色失败')
  }
}

// 单个用户角色分配
const handleRoleAssign = async () => {
  try {
    const currentRoleIds = currentUser.value.roles?.map(r => r.id) || []
    const newRoleIds = assignedRoleIds.value
    
    // 需要移除的角色
    const rolesToRemove = currentRoleIds.filter(id => !newRoleIds.includes(id))
    // 需要新增的角色
    const rolesToAdd = newRoleIds.filter(id => !currentRoleIds.includes(id))

    for (const roleId of rolesToRemove) {
      await adminApi.removeRoleFromUser(currentUser.value.id, roleId)
    }

    for (const roleId of rolesToAdd) {
      await adminApi.assignRoleToUser(currentUser.value.id, roleId)
    }

    ElMessage.success('角色分配成功')
    showRoleAssignDialog.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('角色分配失败')
  }
}

// 获取性别标签
const getGenderLabel = (sex) => {
  const labels = {
    0: '男',
    1: '女',
    2: '其他'
  }
  return labels[sex] || '未知'
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-content h1 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}

.search-section {
  margin-bottom: 24px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: end;
}

.user-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

/* 用户详情样式 */
.user-details {
  max-height: 60vh;
  overflow-y: auto;
}

.user-roles {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.user-roles h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
}

.role-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

/* 批量分配样式 */
.batch-assign {
  max-height: 50vh;
  overflow-y: auto;
}

.selected-info {
  margin-bottom: 20px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #e0f2fe;
  border-radius: 4px;
}

.selected-info p {
  margin: 0;
  color: #0369a1;
  font-weight: 500;
}

/* 角色分配样式 */
.role-assign {
  max-height: 60vh;
  overflow-y: auto;
}

.user-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.user-info h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-management {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-actions {
    justify-content: stretch;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form .el-form-item {
    margin-bottom: 12px;
  }
}
</style>
