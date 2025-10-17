<template>
  <div class="role-management">
    <div class="page-header">
      <div class="header-content">
        <h1>角色管理</h1>
        <p class="subtitle">管理系统角色和权限分配</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <el-card>
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="角色名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入角色名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 角色列表 -->
    <div class="role-list">
      <el-card>
        <el-table
          v-loading="loading"
          :data="roleList"
          stripe
          border
          style="width: 100%"
        >
          <el-table-column prop="name" label="角色名称" width="150" />
          <el-table-column prop="description" label="角色描述" />
          <el-table-column prop="type" label="角色类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 0 ? 'danger' : 'primary'">
                {{ row.type === 0 ? '系统角色' : '自定义角色' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="层级" width="80">
            <template #default="{ row }">
              <el-tag size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :disabled="row.type === 0"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" @click="handlePermission(row)">权限分配</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.type === 0">
                删除
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

    <!-- 创建/编辑角色对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称，如：管理员" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="角色层级" prop="level">
          <el-input-number v-model="form.level" :min="1" :max="999" />
          <div class="form-tip">数字越小权限越大，建议：管理员=10，版主=50，用户=100</div>
        </el-form-item>
        <el-form-item label="是否默认角色" prop="isDefault">
          <el-switch v-model="form.isDefault" />
          <div class="form-tip">新用户注册时自动分配此角色</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog
      v-model="showPermissionDialog"
      title="权限分配"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="permission-assignment">
        <div class="role-info">
          <h3>{{ currentRole?.name }}</h3>
          <p>{{ currentRole?.description }}</p>
        </div>

        <!-- 权限分组显示 -->
        <div class="permission-groups">
          <el-tabs v-model="activeTab">
            <el-tab-pane
              v-for="group in permissionGroups"
              :key="group"
              :label="group"
              :name="group"
            >
              <el-checkbox-group v-model="selectedPermissions">
                <el-row :gutter="20">
                  <el-col
                    v-for="permission in groupedPermissions[group]"
                    :key="permission.id"
                    :span="12"
                  >
                    <el-checkbox :label="permission.id" size="large">
                      <div class="permission-item">
                        <div class="permission-name">{{ permission.name }}</div>
                        <div class="permission-desc">{{ permission.description }}</div>
                      </div>
                    </el-checkbox>
                  </el-col>
                </el-row>
              </el-checkbox-group>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showPermissionDialog = false">取消</el-button>
          <el-button type="primary" @click="handlePermissionSubmit">保存权限</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axiosInstance from '@/api/axiosInstance'

// 响应式数据
const loading = ref(false)
const showCreateDialog = ref(false)
const showPermissionDialog = ref(false)
const isEdit = ref(false)
const roleList = ref([])
const permissionList = ref([])
const selectedPermissions = ref([])

const searchForm = reactive({
  name: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const form = reactive({
  name: '',
  description: '',
  level: 100,
  isDefault: false
})

const currentRole = ref(null)
const activeTab = ref('')

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入角色描述', trigger: 'blur' }
  ]
}

// 计算属性：权限分组
const permissionGroups = computed(() => {
  if (!permissionList.value.length) return []
  const groups = new Set(permissionList.value.map(p => p.group).filter(Boolean))
  return Array.from(groups)
})

// 计算属性：分组后的权限
const groupedPermissions = computed(() => {
  const groups = {}
  permissionList.value.forEach(permission => {
    const group = permission.group || '其他'
    if (!groups[group]) groups[group] = []
    groups[group].push(permission)
  })
  return groups
})

// 获取角色列表
const fetchRoles = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      pageSize: pagination.size,
      keyword: searchForm.name || undefined,
      status: searchForm.status
    }

    const response = await axiosInstance.get('/api/admin/roles', { params })
    if (response.data.success) {
      roleList.value = response.data.data.list || []
      pagination.total = response.data.data.total || 0
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 获取权限列表（用于权限分配）
const fetchPermissions = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/permissions')
    if (response.data.success) {
      permissionList.value = response.data.data.list || []
    }
  } catch (error) {
    console.error('获取权限列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchRoles()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    status: null
  })
  handleSearch()
}

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchRoles()
}

// 当前页改变
const handleCurrentChange = (current) => {
  pagination.current = current
  fetchRoles()
}

// 状态改变
const handleStatusChange = async (row) => {
  try {
    const response = await axiosInstance.put(`/api/admin/roles/${row.id}`, {
      status: row.status
    })
    if (response.data.success) {
      ElMessage.success('状态更新成功')
    } else {
      row.status = row.status === 1 ? 0 : 1 // 回滚状态
      ElMessage.error('状态更新失败')
    }
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1 // 回滚状态
    ElMessage.error('状态更新失败')
  }
}

// 编辑角色
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  showCreateDialog.value = true
}

// 权限分配
const handlePermission = async (row) => {
  currentRole.value = row
  await fetchPermissions()

  // 获取当前角色的权限
  try {
    const response = await axiosInstance.get(`/api/admin/roles/${row.id}/permissions`)
    if (response.data.success) {
      selectedPermissions.value = response.data.data.map(p => p.id || p)
    }
  } catch (error) {
    selectedPermissions.value = []
  }

  // 设置默认选中的标签页
  if (permissionGroups.value.length > 0) {
    activeTab.value = permissionGroups.value[0]
  }

  showPermissionDialog.value = true
}

// 删除角色
const handleDelete = (row) => {
  if (row.type === 0) {
    ElMessage.warning('系统角色不能删除')
    return
  }

  ElMessageBox.confirm(
    `确定要删除角色"${row.name}"吗？此操作不可撤销。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const response = await axiosInstance.delete(`/api/admin/roles/${row.id}`)
      if (response.data.success) {
        ElMessage.success('删除成功')
        fetchRoles()
      } else {
        ElMessage.error('删除失败')
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    const submitData = { ...form }
    delete submitData.id // 删除id字段，避免更新时传递

    let response
    if (isEdit.value) {
      response = await axiosInstance.put(`/api/admin/roles/${form.id}`, submitData)
    } else {
      response = await axiosInstance.post('/api/admin/roles', submitData)
    }

    if (response.data.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      showCreateDialog.value = false
      resetForm()
      fetchRoles()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

// 提交权限分配
const handlePermissionSubmit = async () => {
  try {
    const response = await axiosInstance.post(`/api/admin/roles/${currentRole.value.id}/permissions`, selectedPermissions.value)
    if (response.data.success) {
      ElMessage.success('权限分配成功')
      showPermissionDialog.value = false
    } else {
      ElMessage.error('权限分配失败')
    }
  } catch (error) {
    ElMessage.error('权限分配失败')
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    name: '',
    description: '',
    level: 100,
    isDefault: false
  })
  isEdit.value = false
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped>
.role-management {
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

.role-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 权限分配对话框样式 */
.permission-assignment {
  max-height: 60vh;
  overflow-y: auto;
}

.role-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.role-info h3 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
}

.role-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.permission-groups {
  margin-top: 16px;
}

.permission-item {
  margin-left: 8px;
}

.permission-name {
  font-weight: 500;
  color: #2c3e50;
}

.permission-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .role-management {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
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
