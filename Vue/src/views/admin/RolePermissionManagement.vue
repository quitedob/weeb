<template>
  <div class="role-permission-management">
    <el-card class="header-card">
      <h2>角色权限管理</h2>
      <p class="subtitle">管理系统角色和权限分配</p>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalRoles || 0 }}</div>
              <div class="stat-label">系统角色</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A"><Key /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalPermissions || 0 }}</div>
              <div class="stat-label">系统权限</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C"><Link /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalRoleAssignments || 0 }}</div>
              <div class="stat-label">角色分配</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C"><Connection /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalMappings || 0 }}</div>
              <div class="stat-label">权限映射</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色列表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <el-button type="primary" @click="refreshStatistics">
            <el-icon><Refresh /></el-icon>
            刷新统计
          </el-button>
        </div>
      </template>

      <el-table :data="roleList" style="width: 100%" v-loading="loading">
        <el-table-column prop="roleId" label="角色ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleDescription" label="描述" />
        <el-table-column prop="userCount" label="用户数" width="100" align="center" />
        <el-table-column prop="permissionCount" label="权限数" width="100" align="center" />
        <el-table-column label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewRolePermissions(scope.row)">
              查看权限
            </el-button>
            <el-button size="small" type="primary" @click="editRolePermissions(scope.row)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 权限编辑对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      :title="`编辑角色权限 - ${currentRole?.roleName}`"
      width="60%"
    >
      <el-transfer
        v-model="selectedPermissions"
        :data="allPermissions"
        :titles="['可用权限', '已分配权限']"
        filterable
        filter-placeholder="搜索权限"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRolePermissions" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 权限查看对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      :title="`角色权限详情 - ${currentRole?.roleName}`"
      width="50%"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="角色ID">{{ currentRole?.roleId }}</el-descriptions-item>
        <el-descriptions-item label="角色名称">{{ currentRole?.roleName }}</el-descriptions-item>
        <el-descriptions-item label="用户数量">{{ currentRole?.userCount }}</el-descriptions-item>
        <el-descriptions-item label="权限数量">{{ currentRole?.permissionCount }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>权限列表</h4>
      <el-tag
        v-for="permission in rolePermissions"
        :key="permission.id"
        style="margin: 5px"
        type="success"
      >
        {{ permission.name }}
      </el-tag>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Key, Link, Connection, Refresh } from '@element-plus/icons-vue'
import axios from '@/api/axiosInstance'

// 数据
const statistics = ref({})
const roleList = ref([])
const allPermissions = ref([])
const rolePermissions = ref([])
const selectedPermissions = ref([])
const currentRole = ref(null)

// 状态
const loading = ref(false)
const saving = ref(false)
const permissionDialogVisible = ref(false)
const viewDialogVisible = ref(false)

// 加载统计信息
const loadStatistics = async () => {
  try {
    const response = await axios.get('/api/role-permissions/statistics')
    if (response.data.success) {
      statistics.value = response.data.data
      roleList.value = response.data.data.roleStatistics || []
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
    ElMessage.error('加载统计信息失败')
  }
}

// 刷新统计
const refreshStatistics = async () => {
  loading.value = true
  await loadStatistics()
  loading.value = false
  ElMessage.success('统计信息已刷新')
}

// 查看角色权限
const viewRolePermissions = async (role) => {
  currentRole.value = role
  try {
    const response = await axios.get(`/api/role-permissions/role/${role.roleId}/permissions`)
    if (response.data.success) {
      rolePermissions.value = response.data.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载角色权限失败:', error)
    ElMessage.error('加载角色权限失败')
  }
}

// 编辑角色权限
const editRolePermissions = async (role) => {
  currentRole.value = role
  try {
    // 加载所有权限
    // TODO: 需要实现获取所有权限的API
    allPermissions.value = []

    // 加载角色当前权限
    const response = await axios.get(`/api/role-permissions/role/${role.roleId}/permissions`)
    if (response.data.success) {
      selectedPermissions.value = response.data.data.map(p => p.id)
      permissionDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载权限数据失败:', error)
    ElMessage.error('加载权限数据失败')
  }
}

// 保存角色权限
const saveRolePermissions = async () => {
  saving.value = true
  try {
    const response = await axios.post('/api/role-permissions/update-role-permissions', {
      roleId: currentRole.value.roleId,
      permissionIds: selectedPermissions.value
    })

    if (response.data.success) {
      ElMessage.success('角色权限更新成功')
      permissionDialogVisible.value = false
      await loadStatistics()
    } else {
      ElMessage.error(response.data.message || '更新失败')
    }
  } catch (error) {
    console.error('保存角色权限失败:', error)
    ElMessage.error('保存角色权限失败')
  } finally {
    saving.value = false
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.role-permission-management {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
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
