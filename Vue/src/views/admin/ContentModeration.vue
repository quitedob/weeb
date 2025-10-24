<template>
  <div class="content-moderation">
    <div class="page-header">
      <div class="header-content">
        <h1>内容审核</h1>
        <p class="subtitle">管理文章内容和用户生成内容</p>
      </div>
      <div class="header-actions">
        <AppleButton variant="primary" @click="refreshStatistics" :loading="loading">
          <i class="icon-refresh"></i>
          刷新统计
        </AppleButton>
      </div>
    </div>

    <!-- 审核统计卡片 -->
    <div class="stats-section">
      <AppleGrid gutter="20">
        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card pending">
            <div class="stat-icon">
              <i class="icon-clock"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.pending || 0 }}</div>
              <div class="stat-label">待审核</div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card approved">
            <div class="stat-icon">
              <i class="icon-check"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.approved || 0 }}</div>
              <div class="stat-label">已通过</div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card rejected">
            <div class="stat-icon">
              <i class="icon-close"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.rejected || 0 }}</div>
              <div class="stat-label">已拒绝</div>
            </div>
          </AppleCard>
        </AppleCol>

        <AppleCol :xs="24" :sm="12" :md="6">
          <AppleCard class="stat-card total">
            <div class="stat-icon">
              <i class="icon-document"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ statistics.total || 0 }}</div>
              <div class="stat-label">总文章数</div>
            </div>
          </AppleCard>
        </AppleCol>
      </AppleGrid>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <AppleCard>
        <div class="search-form">
          <div class="form-row">
            <div class="form-group">
              <label>状态</label>
              <AppleSelect v-model="searchForm.status" placeholder="请选择状态" clearable>
                <AppleOption label="待审核" :value="0" />
                <AppleOption label="已通过" :value="1" />
                <AppleOption label="已拒绝" :value="2" />
              </AppleSelect>
            </div>
            <div class="form-group">
              <label>关键词</label>
              <AppleInput
                v-model="searchForm.keyword"
                placeholder="请输入关键词"
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="form-actions">
              <AppleButton variant="primary" @click="handleSearch" :loading="loading">
                <i class="icon-search"></i>
                搜索
              </AppleButton>
              <AppleButton variant="default" @click="handleReset">
                <i class="icon-refresh-left"></i>
                重置
              </AppleButton>
            </div>
          </div>
        </div>
      </AppleCard>
    </div>

    <!-- 文章列表 -->
    <div class="articles-section">
      <AppleCard>
        <template #header>
          <div class="card-header">
            <span>待审核文章列表</span>
            <div class="header-actions">
              <AppleButton
                variant="success"
                size="small"
                :disabled="!selectedArticles.length"
                @click="batchApprove"
              >
                批量通过
              </AppleButton>
              <AppleButton
                variant="danger"
                size="small"
                :disabled="!selectedArticles.length"
                @click="batchReject"
              >
                批量拒绝
              </AppleButton>
            </div>
          </div>
        </template>

        <AppleTable
          :data="articles"
          :columns="tableColumns"
          :loading="loading"
          stripe
          @selection-change="handleSelectionChange"
        >
          <template #title="{ row }">
            <AppleButton
              variant="text"
              @click="viewArticle(row)"
            >
              {{ row.title || '无标题' }}
            </AppleButton>
          </template>

          <template #author="{ row }">
            <span>{{ row.authorName || '未知' }}</span>
          </template>

          <template #category="{ row }">
            <AppleTag size="small" type="default">
              {{ row.categoryName || '未分类' }}
            </AppleTag>
          </template>

          <template #status="{ row }">
            <AppleTag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </AppleTag>
          </template>

          <template #createdAt="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>

          <template #action="{ row }">
            <div class="action-buttons">
              <AppleButton
                variant="success"
                size="small"
                @click="approveArticle(row)"
                :disabled="row.status === 1"
              >
                通过
              </AppleButton>
              <AppleButton
                variant="danger"
                size="small"
                @click="rejectArticle(row)"
                :disabled="row.status === 2"
              >
                拒绝
              </AppleButton>
              <AppleDropdown @command="(command) => handleCommand(command, row)">
                <AppleButton variant="primary" size="small">
                  更多 <i class="icon-chevron-down"></i>
                </AppleButton>
                <template #dropdown>
                  <AppleDropdownItem command="view">查看详情</AppleDropdownItem>
                  <AppleDropdownItem command="edit">编辑</AppleDropdownItem>
                  <AppleDropdownItem command="delete" divided>删除</AppleDropdownItem>
                </template>
              </AppleDropdown>
            </div>
          </template>
        </AppleTable>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <ApplePagination
            v-model:currentPage="pagination.page"
            v-model:pageSize="pagination.pageSize"
            :pageSizes="[10, 20, 50, 100]"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </AppleCard>
    </div>

    <!-- 文章详情对话框 -->
    <AppleModal
      v-model="articleDialog.visible"
      :title="articleDialog.title"
      width="80%"
      destroy-on-close
    >
      <div v-if="articleDialog.article" class="article-preview">
        <div class="article-header">
          <h2>{{ articleDialog.article.title }}</h2>
          <div class="article-meta">
            <span>作者：{{ articleDialog.article.authorName }}</span>
            <span>分类：{{ articleDialog.article.categoryName }}</span>
            <span>创建时间：{{ formatDateTime(articleDialog.article.createdAt) }}</span>
          </div>
        </div>
        <div class="article-content" v-html="articleDialog.article.content"></div>
      </div>

      <template #footer>
        <AppleButton @click="articleDialog.visible = false">关闭</AppleButton>
        <AppleButton
          variant="success"
          @click="approveArticle(articleDialog.article)"
          :disabled="articleDialog.article.status === 1"
        >
          通过
        </AppleButton>
        <AppleButton
          variant="danger"
          @click="rejectArticle(articleDialog.article)"
          :disabled="articleDialog.article.status === 2"
        >
          拒绝
        </AppleButton>
      </template>
    </AppleModal>

    <!-- 拒绝原因对话框 -->
    <AppleModal
      v-model="rejectDialog.visible"
      title="拒绝原因"
      width="500px"
    >
      <div class="reject-form">
        <div class="form-group">
          <label>拒绝原因</label>
          <AppleInput
            v-model="rejectDialog.form.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因..."
            maxlength="500"
            show-word-limit
          />
          <div v-if="rejectError" class="error-message">{{ rejectError }}</div>
        </div>
      </div>

      <template #footer>
        <AppleButton @click="rejectDialog.visible = false">取消</AppleButton>
        <AppleButton variant="danger" @click="confirmReject" :loading="rejectDialog.loading">
          确认拒绝
        </AppleButton>
      </template>
    </AppleModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { showMessage, showConfirm } from '@/utils/message'
import { log } from '@/utils/logger'
import axiosInstance from '@/api/axiosInstance'

// Import Apple components
import AppleButton from '@/components/common/AppleButton.vue'
import AppleCard from '@/components/common/AppleCard.vue'
import AppleGrid from '@/components/common/AppleGrid.vue'
import AppleCol from '@/components/common/AppleCol.vue'
import AppleInput from '@/components/common/AppleInput.vue'
import AppleSelect from '@/components/common/AppleSelect.vue'
import AppleOption from '@/components/common/AppleOption.vue'
import AppleTable from '@/components/common/AppleTable.vue'
import ApplePagination from '@/components/common/ApplePagination.vue'
import AppleTag from '@/components/common/AppleTag.vue'
import AppleModal from '@/components/common/AppleModal.vue'
import AppleDropdown from '@/components/common/AppleDropdown.vue'
import AppleDropdownItem from '@/components/common/AppleDropdownItem.vue'

// 响应式数据
const loading = ref(false)
const articles = ref([])
const selectedArticles = ref([])
const statistics = ref({
  pending: 0,
  approved: 0,
  rejected: 0,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  status: '',
  keyword: ''
})

// 分页数据
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 文章详情对话框
const articleDialog = reactive({
  visible: false,
  title: '',
  article: null
})

// 拒绝原因对话框
const rejectDialog = reactive({
  visible: false,
  loading: false,
  currentArticle: null,
  form: {
    reason: ''
  },
  rules: {
    reason: [
      { required: true, message: '请输入拒绝原因', trigger: 'blur' },
      { min: 5, max: 500, message: '拒绝原因长度应在5-500个字符之间', trigger: 'blur' }
    ]
  }
})

const rejectError = ref('')

// 表格列配置
const tableColumns = computed(() => [
  { type: 'selection', width: 55 },
  { prop: 'title', label: '标题', minWidth: 200 },
  { prop: 'author', label: '作者', width: 120 },
  { prop: 'category', label: '分类', width: 100 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'createdAt', label: '创建时间', width: 180 },
  { prop: 'action', label: '操作', width: 200, fixed: 'right' }
])

// 方法
const fetchStatistics = async () => {
  try {
    const response = await axiosInstance.get('/api/admin/content/statistics')
    if (response.data.code === 0) {
      statistics.value = response.data.data
    }
  } catch (error) {
    log.error('获取审核统计失败:', error)
  }
}

const fetchArticles = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    if (searchForm.status !== '') {
      params.status = searchForm.status
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }

    const response = await axiosInstance.get('/api/admin/content/articles/pending', { params })

    if (response.data.code === 0) {
      articles.value = response.data.data.list || []
      pagination.total = response.data.data.total || 0
    }
  } catch (error) {
    log.error('获取文章列表失败:', error)
    showMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchArticles()
}

const handleReset = () => {
  searchForm.status = ''
  searchForm.keyword = ''
  pagination.page = 1
  fetchArticles()
}

const handleSelectionChange = (selection) => {
  selectedArticles.value = selection
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchArticles()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  fetchArticles()
}

const refreshStatistics = () => {
  fetchStatistics()
  fetchArticles()
}

const viewArticle = (article) => {
  articleDialog.article = article
  articleDialog.title = `文章详情 - ${article.title || '无标题'}`
  articleDialog.visible = true
}

const approveArticle = async (article) => {
  try {
    const response = await axiosInstance.post(`/api/admin/content/articles/${article.id}/approve`)

    if (response.data.code === 0) {
      showMessage.success('文章已通过审核')
      fetchArticles()
      fetchStatistics()
    } else {
      showMessage.error(response.data.message || '审核失败')
    }
  } catch (error) {
    log.error('审核通过失败:', error)
    showMessage.error('审核通过失败')
  }
}

const rejectArticle = (article) => {
  rejectDialog.currentArticle = article
  rejectDialog.form.reason = ''
  rejectDialog.visible = true
}

const confirmReject = async () => {
  try {
    // 验证拒绝原因
    if (!rejectDialog.form.reason || rejectDialog.form.reason.trim().length < 5) {
      rejectError.value = '拒绝原因长度应在5-500个字符之间'
      return
    }

    rejectError.value = ''
    rejectDialog.loading = true
    const response = await axiosInstance.post(
      `/api/admin/content/articles/${rejectDialog.currentArticle.id}/reject`,
      { reason: rejectDialog.form.reason }
    )

    if (response.data.code === 0) {
      showMessage.success('文章已拒绝')
      rejectDialog.visible = false
      fetchArticles()
      fetchStatistics()
    } else {
      showMessage.error(response.data.message || '拒绝失败')
    }
  } catch (error) {
    log.error('拒绝文章失败:', error)
    showMessage.error('拒绝文章失败')
  } finally {
    rejectDialog.loading = false
  }
}

const batchApprove = async () => {
  try {
    const confirmed = await showConfirm.warning(
      `确定要通过选中的 ${selectedArticles.value.length} 篇文章吗？`,
      '批量通过'
    )

    if (confirmed) {
      const promises = selectedArticles.value.map(article =>
        axiosInstance.post(`/api/admin/content/articles/${article.id}/approve`)
      )

      await Promise.all(promises)

      showMessage.success('批量审核通过成功')
      fetchArticles()
      fetchStatistics()
    }
  } catch (error) {
    log.error('批量审核通过失败:', error)
    showMessage.error('批量审核通过失败')
  }
}

const batchReject = async () => {
  try {
    const reason = await showConfirm.prompt(
      '请输入拒绝原因',
      '批量拒绝',
      {
        placeholder: '拒绝原因至少需要5个字符',
        required: true,
        minLength: 5
      }
    )

    if (reason) {
      const promises = selectedArticles.value.map(article =>
        axiosInstance.post(`/api/admin/content/articles/${article.id}/reject`, { reason })
      )

      await Promise.all(promises)

      showMessage.success('批量拒绝成功')
      fetchArticles()
      fetchStatistics()
    }
  } catch (error) {
    log.error('批量拒绝失败:', error)
    showMessage.error('批量拒绝失败')
  }
}

const handleCommand = (command, article) => {
  switch (command) {
    case 'view':
      viewArticle(article)
      break
    case 'edit':
      // TODO: 实现编辑功能
      showMessage.info('编辑功能待实现')
      break
    case 'delete':
      deleteArticle(article)
      break
  }
}

const deleteArticle = async (article) => {
  try {
    const confirmDelete = await showConfirm.warning(
      `确定要删除文章 "${article.title}" 吗？此操作不可恢复。`,
      '删除文章'
    )

    if (confirmDelete) {
      const reason = await showConfirm.prompt(
        '请输入删除原因',
        '删除确认',
        {
          placeholder: '删除原因至少需要5个字符',
          required: true,
          minLength: 5
        }
      )

      if (reason) {
        const response = await axiosInstance.delete(
          `/api/admin/content/articles/${article.id}`,
          { data: { reason: reason.trim() } }
        )

        if (response.data.code === 0) {
          showMessage.success('文章已删除')
          fetchArticles()
          fetchStatistics()
        } else {
          showMessage.error(response.data.message || '删除失败')
        }
      }
    }
  } catch (error) {
    log.error('删除文章失败:', error)
    showMessage.error('删除文章失败')
  }
}

// 工具方法
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'warning' // 待审核
    case 1: return 'success' // 已通过
    case 2: return 'danger'  // 已拒绝
    default: return 'default'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '待审核'
    case 1: return '已通过'
    case 2: return '已拒绝'
    default: return '未知'
  }
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchStatistics()
  fetchArticles()
})
</script>

<style scoped>
.content-moderation {
  padding: var(--apple-spacing-lg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--apple-spacing-lg);
}

.header-content h1 {
  margin: 0 0 var(--apple-spacing-sm) 0;
  color: var(--apple-text-primary);
  font-size: var(--apple-font-xl);
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: var(--apple-text-secondary);
  font-size: var(--apple-font-md);
}

.header-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.stats-section {
  margin-bottom: var(--apple-spacing-lg);
}

.stat-card {
  height: 100px;
  display: flex;
  align-items: center;
  padding: var(--apple-spacing-md);
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-medium);
}

.stat-card.pending {
  border-left: 4px solid var(--apple-orange);
}

.stat-card.approved {
  border-left: 4px solid var(--apple-green);
}

.stat-card.rejected {
  border-left: 4px solid var(--apple-red);
}

.stat-card.total {
  border-left: 4px solid var(--apple-blue);
}

.stat-icon {
  font-size: 32px;
  margin-right: var(--apple-spacing-md);
  color: var(--apple-text-secondary);
}

.stat-card.pending .stat-icon {
  color: var(--apple-orange);
}

.stat-card.approved .stat-icon {
  color: var(--apple-green);
}

.stat-card.rejected .stat-icon {
  color: var(--apple-red);
}

.stat-card.total .stat-icon {
  color: var(--apple-blue);
}

.stat-number {
  font-size: var(--apple-font-xl);
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
  margin-top: var(--apple-spacing-xs);
}

.search-section {
  margin-bottom: var(--apple-spacing-lg);
}

.search-form {
  margin: 0;
}

.form-row {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-md);
  flex-wrap: wrap;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--apple-spacing-xs);
  min-width: 200px;
}

.form-group label {
  font-size: var(--apple-font-sm);
  font-weight: 500;
  color: var(--apple-text-secondary);
}

.form-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
  align-items: flex-end;
}

.action-buttons {
  display: flex;
  gap: var(--apple-spacing-xs);
  align-items: center;
  flex-wrap: wrap;
}

.reject-form {
  min-height: 200px;
}

.reject-form .form-group {
  margin-bottom: var(--apple-spacing-lg);
}

.error-message {
  color: var(--apple-red);
  font-size: var(--apple-font-sm);
  margin-top: var(--apple-spacing-xs);
}

.articles-section {
  background-color: var(--apple-bg-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: var(--apple-spacing-sm);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--apple-spacing-lg);
}

.article-preview {
  max-height: 600px;
  overflow-y: auto;
}

.article-header {
  margin-bottom: var(--apple-spacing-lg);
  border-bottom: 1px solid var(--apple-border-secondary);
  padding-bottom: var(--apple-spacing-md);
}

.article-header h2 {
  margin: 0 0 var(--apple-spacing-md) 0;
  color: var(--apple-text-primary);
}

.article-meta {
  display: flex;
  gap: var(--apple-spacing-lg);
  color: var(--apple-text-secondary);
  font-size: var(--apple-font-sm);
}

.article-content {
  line-height: 1.6;
  color: var(--apple-text-primary);
}

@media (max-width: 768px) {
  .content-moderation {
    padding: var(--apple-spacing-md);
  }

  .page-header {
    flex-direction: column;
    gap: var(--apple-spacing-md);
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .stats-section .el-col {
    margin-bottom: var(--apple-spacing-sm);
  }

  .article-meta {
    flex-direction: column;
    gap: var(--apple-spacing-sm);
  }
}
</style>