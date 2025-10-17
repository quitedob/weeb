<template>
  <div class="permission-management">
    <div class="page-header">
      <div class="header-content">
        <h1>权限管理</h1>
        <p class="subtitle">管理系统权限和授权规则</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新增权限
      </el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <el-card>
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="权限名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入权限名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="资源类型">
            <el-select v-model="searchForm.resource" placeholder="请选择资源类型" clearable>
              <el-option label="用户管理" value="user" />
              <el-option label="文章管理" value="article" />
              <el-option label="评论管理" value="comment" />
              <el-option label="群组管理" value="group" />
              <el-option label="系统管理" value="system" />
              <el-option label="权限管理" value="permission" />
            </el-select>
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

    <!-- 权限列表 -->
    <div class="permission-list">
      <el-card>
        <el-table
          v-loading="loading"
          :data="permissionList"
          stripe
          border
          style="width: 100%"
        >
          <el-table-column prop="name" label="权限名称" width="200" />
          <el-table-column prop="description" label="权限描述" />
          <el-table-column prop="resource" label="资源类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getResourceTagType(row.resource)">
                {{ getResourceLabel(row.resource) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="操作" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                @change="handleStatusChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 创建/编辑权限对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="isEdit ? '编辑权限' : '新增权限'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称，如：USER_CREATE" />
        </el-form-item>
        <el-form-item label="权限描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入权限描述" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resource">
          <el-select v-model="form.resource" placeholder="请选择资源类型">
            <el-option label="用户管理" value="user" />
            <el-option label="文章管理" value="article" />
            <el-option label="评论管理" value="comment" />
            <el-option label="群组管理" value="group" />
            <el-option label="系统管理" value="system" />
            <el-option label="权限管理" value="permission" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作" prop="action">
          <el-select v-model="form.action" placeholder="请选择操作类型">
            <el-option label="创建" value="create" />
            <el-option label="读取" value="read" />
            <el-option label="更新" value="update" />
            <el-option label="删除" value="delete" />
            <el-option label="管理" value="manage" />
            <el-option label="审核" value="moderate" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限条件" prop="condition">
          <el-input v-model="form.condition" placeholder="可选，如：own, any" />
        </el-form-item>
        <el-form-item label="权限分组" prop="group">
          <el-input v-model="form.group" placeholder="如：用户管理，内容管理" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axiosInstance from '@/api/axiosInstance'

// 响应式数据
const loading = ref(false)
const showCreateDialog = ref(false)
const isEdit = ref(false)
const permissionList = ref([])

const searchForm = reactive({
  name: '',
  resource: '',
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
  resource: '',
  action: '',
  condition: '',
  group: '',
  sortOrder: 0
})

const rules = {
  name: [
    { required: true, message: '请输入权限名称', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '权限名称必须为大写字母和下划线', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入权限描述', trigger: 'blur' }
  ],
  resource: [
    { required: true, message: '请选择资源类型', trigger: 'change' }
  ],
  action: [
    { required: true, message: '请选择操作类型', trigger: 'change' }
  ]
}

// 获取权限列表
const fetchPermissions = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      pageSize: pagination.size,
      keyword: searchForm.name || undefined,
      resource: searchForm.resource || undefined,
      status: searchForm.status
    }

    const response = await axiosInstance.get('/api/admin/permissions', { params })
    if (response.data.success) {
      permissionList.value = response.data.data.list || []
      pagination.total = response.data.data.total || 0
    }
  } catch (error) {
    console.error('获取权限列表失败:', error)
    ElMessage.error('获取权限列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchPermissions()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    resource: '',
    status: null
  })
  handleSearch()
}

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchPermissions()
}

// 当前页改变
const handleCurrentChange = (current) => {
  pagination.current = current
  fetchPermissions()
}

// 状态改变
const handleStatusChange = async (row) => {
  try {
    const response = await axiosInstance.put(`/api/admin/permissions/${row.id}`, {
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

// 编辑权限
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  showCreateDialog.value = true
}

// 删除权限
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除权限"${row.name}"吗？此操作不可撤销。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const response = await axiosInstance.delete(`/api/admin/permissions/${row.id}`)
      if (response.data.success) {
        ElMessage.success('删除成功')
        fetchPermissions()
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
      response = await axiosInstance.put(`/api/admin/permissions/${form.id}`, submitData)
    } else {
      response = await axiosInstance.post('/api/admin/permissions', submitData)
    }

    if (response.data.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      showCreateDialog.value = false
      resetForm()
      fetchPermissions()
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

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    name: '',
    description: '',
    resource: '',
    action: '',
    condition: '',
    group: '',
    sortOrder: 0
  })
  isEdit.value = false
}

// 获取资源标签类型
const getResourceTagType = (resource) => {
  const types = {
    user: 'primary',
    article: 'success',
    comment: 'info',
    group: 'warning',
    system: 'danger',
    permission: 'primary'
  }
  return types[resource] || 'info'
}

// 获取资源标签文本
const getResourceLabel = (resource) => {
  const labels = {
    user: '用户管理',
    article: '文章管理',
    comment: '评论管理',
    group: '群组管理',
    system: '系统管理',
    permission: '权限管理'
  }
  return labels[resource] || resource
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchPermissions()
})
</script>

<style scoped>
.permission-management {
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

.permission-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .permission-management {
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
