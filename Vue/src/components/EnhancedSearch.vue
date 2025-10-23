<template>
  <div class="enhanced-search">
    <!-- 搜索框 -->
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        :placeholder="placeholder"
        :prefix-icon="Search"
        class="search-input"
        @input="handleSearchInput"
        @keydown.enter="performSearch"
        clearable
        @clear="handleClear"
      >
        <template #append>
          <el-button
            type="primary"
            :icon="Search"
            @click="performSearch"
            :loading="searching"
          >
            搜索
          </el-button>
        </template>
      </el-input>

      <!-- 搜索类型选择 -->
      <el-select
        v-model="searchType"
        class="search-type-select"
        @change="handleTypeChange"
      >
        <el-option
          v-for="type in searchTypes"
          :key="type.value"
          :label="type.label"
          :value="type.value"
        />
      </el-select>

      <!-- 高级搜索按钮 -->
      <el-button
        type="info"
        :icon="Setting"
        @click="showAdvancedSearch = true"
        circle
      />
    </div>

    <!-- 搜索建议 -->
    <div
      v-if="showSuggestions && suggestions.length > 0"
      class="suggestions-container"
    >
      <div class="suggestions-header">
        <span>搜索建议</span>
      </div>
      <div class="suggestions-list">
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-item"
          @click="selectSuggestion(suggestion)"
        >
          <el-icon class="suggestion-icon"><Search /></el-icon>
          <span class="suggestion-text">{{ suggestion }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div
      v-if="showHistory && searchHistory.length > 0"
      class="history-container"
    >
      <div class="history-header">
        <span>搜索历史</span>
        <el-button
          type="text"
          size="small"
          @click="clearHistory"
        >
          清除历史
        </el-button>
      </div>
      <div class="history-list">
        <div
          v-for="(item, index) in searchHistory"
          :key="index"
          class="history-item"
          @click="selectHistory(item)"
        >
          <el-icon class="history-icon"><Clock /></el-icon>
          <span class="history-text">{{ item.keyword }}</span>
          <span class="history-time">{{ formatTime(item.createdAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 热门关键词 -->
    <div
      v-if="showHotKeywords && hotKeywords.length > 0"
      class="hot-keywords-container"
    >
      <div class="hot-keywords-header">
        <span>热门搜索</span>
      </div>
      <div class="hot-keywords-list">
        <el-tag
          v-for="(keyword, index) in hotKeywords"
          :key="index"
          class="hot-keyword"
          @click="selectKeyword(keyword)"
          effect="plain"
        >
          {{ keyword }}
        </el-tag>
      </div>
    </div>

    <!-- 搜索过滤器 -->
    <div v-if="hasFilters" class="filters-container">
      <div class="filters-header">
        <span>筛选条件</span>
        <el-button
          type="text"
          size="small"
          @click="resetFilters"
        >
          重置
        </el-button>
      </div>
      <div class="filters-content">
        <!-- 根据搜索类型动态显示过滤器 -->
        <component
          :is="currentFilterComponent"
          :filters="filters"
          @update="updateFilters"
        />
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="hasResults" class="results-container">
      <div class="results-header">
        <span class="results-count">
          找到 {{ totalResults }} 条结果
        </span>
        <div class="results-actions">
          <el-button
            type="text"
            size="small"
            @click="exportResults"
          >
            导出结果
          </el-button>
          <el-button
            type="text"
            size="small"
            @click="toggleRealTimeSearch"
            :type="realTimeSearchActive ? 'danger' : 'info'"
          >
            <el-icon><VideoPlay /></el-icon>
            {{ realTimeSearchActive ? '停止实时搜索' : '开启实时搜索' }}
          </el-button>
        </div>
      </div>

      <!-- 结果列表 -->
      <div class="results-list" v-loading="searching">
        <component
          :is="currentResultComponent"
          :results="searchResults"
          :loading="searching"
          :type="searchType"
          @item-click="handleResultClick"
        />
      </div>

      <!-- 分页 -->
      <div class="pagination-container" v-if="totalResults > pageSize">
        <el-pagination
          v-model:current="currentPage"
          :page-size="pageSize"
          :total="totalResults"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 无结果提示 -->
    <div v-else-if="hasSearched && !searching" class="no-results">
      <el-empty
        :description="emptyDescription"
        :image-size="200"
      >
        <template #image>
          <el-icon :size="60"><Search /></el-icon>
        </template>
      </el-empty>
    </div>

    <!-- 高级搜索对话框 -->
    <el-dialog
      v-model="showAdvancedSearch"
      title="高级搜索"
      width="600px"
      :before-close="handleAdvancedSearchClose"
    >
      <EnhancedAdvancedSearch
        :search-type="searchType"
        :filters="filters"
        @search="performAdvancedSearch"
        @cancel="showAdvancedSearch = false"
      />
    </el-dialog>

    <!-- 实时搜索指示器 -->
    <div
      v-if="realTimeSearchActive"
      class="real-time-indicator"
    >
      <el-icon class="real-time-icon"><VideoPlay /></el-icon>
      <span>实时搜索中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Search, Setting, Clock, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  searchMessagesWithES,
  getSearchSuggestions,
  getSearchStatistics,
  getHotKeywords,
  recordSearchHistory,
  getSearchHistory,
  clearSearchHistory,
  getSearchPreferences,
  createRealTimeSearch,
  sendRealTimeSearchQuery,
  closeRealTimeSearch
} from '@/api/modules/searchEnhanced'
import EnhancedAdvancedSearch from './EnhancedAdvancedSearch.vue'
import MessageSearchResults from './search/MessageSearchResults.vue'
import UserSearchResults from './search/UserSearchResults.vue'
import GroupSearchResults from './search/GroupSearchResults.vue'
import ArticleSearchResults from './search/ArticleSearchResults.vue'

// Props
const props = defineProps({
  placeholder: {
    type: String,
    default: '请输入搜索关键词...'
  },
  defaultType: {
    type: String,
    default: 'messages'
  },
  enableRealTime: {
    type: Boolean,
    default: false
  },
  showHistory: {
    type: Boolean,
    default: true
  },
  showSuggestions: {
    type: Boolean,
    default: true
  },
  showHotKeywords: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['search', 'result-click', 'filter-change'])

// 响应式数据
const searchQuery = ref('')
const searchType = ref(props.defaultType)
const searching = ref(false)
const searchResults = ref([])
const suggestions = ref([])
const searchHistory = ref([])
const hotKeywords = ref([])
const totalResults = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filters = ref({})
const showAdvancedSearch = ref(false)
const realTimeSearchActive = ref(false)
const realTimeSearchWs = ref(null)

// 搜索类型
const searchTypes = [
  { label: '消息', value: 'messages' },
  { label: '用户', value: 'users' },
  { label: '群组', value: 'groups' },
  { label: '文章', value: 'articles' },
  { label: '综合', value: 'comprehensive' }
]

// 计算属性
const hasResults = computed(() => searchResults.value.length > 0)
const hasSearched = computed(() => searchQuery.value.trim() !== '' || searching.value)
const hasFilters = computed(() => Object.keys(filters.value).length > 0)
const currentFilterComponent = computed(() => {
  const filterComponents = {
    messages: 'MessageSearchFilters',
    users: 'UserSearchFilters',
    groups: 'GroupSearchFilters',
    articles: 'ArticleSearchFilters',
    comprehensive: 'ComprehensiveSearchFilters'
  }
  return filterComponents[searchType.value] || 'MessageSearchFilters'
})

const currentResultComponent = computed(() => {
  const resultComponents = {
    messages: MessageSearchResults,
    users: UserSearchResults,
    groups: GroupSearchResults,
    articles: ArticleSearchResults,
    comprehensive: 'ComprehensiveSearchResults'
  }
  return resultComponents[searchType.value] || MessageSearchResults
})

const emptyDescription = computed(() => {
  const descriptions = {
    messages: '没有找到相关消息',
    users: '没有找到相关用户',
    groups: '没有找到相关群组',
    articles: '没有找到相关文章',
    comprehensive: '没有找到相关内容'
  }
  return descriptions[searchType.value] || '没有找到相关内容'
})

// 防抖搜索
const debouncedSearch = ref(null)
const createDebouncedSearch = () => {
  if (debouncedSearch.value) {
    clearTimeout(debouncedSearch.value)
  }
  debouncedSearch.value = setTimeout(() => {
    if (searchQuery.value.trim()) {
      performSearch()
    }
  }, 300)
}

// 防抖获取建议
const debouncedGetSuggestions = ref(null)
const createDebouncedSuggestions = () => {
  if (debouncedGetSuggestions.value) {
    clearTimeout(debouncedGetSuggestions.value)
  }
  debouncedGetSuggestions.value = setTimeout(() => {
    if (searchQuery.value.trim().length >= 2) {
      fetchSuggestions()
    }
  }, 200)
}

// 方法
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    return
  }

  searching.value = true
  currentPage.value = 1

  try {
    // 记录搜索历史
    await recordSearchHistory(searchQuery.value.trim(), searchType.value)

    // 执行搜索
    let results
    if (searchType.value === 'messages') {
      results = await searchMessagesWithES({
        keyword: searchQuery.value.trim(),
        page: currentPage.value - 1,
        size: pageSize.value,
        ...filters.value
      })
    } else {
      // 其他类型的搜索实现
      results = await performOtherSearch()
    }

    searchResults.value = results.documents || []
    totalResults.value = results.total || 0

    // 触发搜索事件
    emit('search', {
      query: searchQuery.value,
      type: searchType.value,
      results: searchResults.value,
      total: totalResults.value
    })
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    searching.value = false
  }
}

const performOtherSearch = async () => {
  // 实现其他类型的搜索逻辑
  switch (searchType.value) {
    case 'users':
      // 用户搜索实现
      return { documents: [], total: 0 }
    case 'groups':
      // 群组搜索实现
      return { documents: [], total: 0 }
    case 'articles':
      // 文章搜索实现
      return { documents: [], total: 0 }
    case 'comprehensive':
      // 综合搜索实现
      return { documents: [], total: 0 }
    default:
      return { documents: [], total: 0 }
  }
}

const fetchSuggestions = async () => {
  try {
    const response = await getSearchSuggestions(searchQuery.value.trim(), 8)
    suggestions.value = response.data || []
  } catch (error) {
    console.error('获取搜索建议失败:', error)
  }
}

const fetchHotKeywords = async () => {
  try {
    const response = await getHotKeywords(10)
    hotKeywords.value = response.data || []
  } catch (error) {
    console.error('获取热门关键词失败:', error)
  }
}

const fetchSearchHistory = async () => {
  try {
    const response = await getSearchHistory(10)
    searchHistory.value = response.data || []
  } catch (error) {
    console.error('获取搜索历史失败:', error)
  }
}

const handleSearchInput = () => {
  // 重置页码
  currentPage.value = 1
  // 获取建议
  if (props.showSuggestions) {
    createDebouncedSuggestions()
  }
}

const handleClear = () => {
  searchResults.value = []
  totalResults.value = 0
  currentPage.value = 1
  suggestions.value = []
  emit('search', {
    query: '',
    type: searchType.value,
    results: [],
    total: 0
  })
}

const handleTypeChange = () => {
  searchResults.value = []
  totalResults.value = 0
  currentPage.value = 1
  suggestions.value = []
  emit('filter-change', { type: searchType.value })
}

const selectSuggestion = (suggestion) => {
  searchQuery.value = suggestion
  performSearch()
}

const selectKeyword = (keyword) => {
  searchQuery.value = keyword
  performSearch()
}

const selectHistory = (item) => {
  searchQuery.value = item.keyword
  performSearch()
}

const clearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistory.value = []
    ElMessage.success('搜索历史已清除')
  } catch (error) {
    console.error('清除搜索历史失败:', error)
    ElMessage.error('清除搜索历史失败')
  }
}

const updateFilters = (newFilters) => {
  filters.value = newFilters
  emit('filter-change', { type: searchType.value, filters: newFilters })
}

const resetFilters = () => {
  filters.value = {}
  emit('filter-change', { type: searchType.value, filters: {} })
}

const performAdvancedSearch = (advancedParams) => {
  // 合并高级搜索参数
  const mergedParams = {
    keyword: searchQuery.value.trim(),
    type: searchType.value,
    page: 0,
    size: pageSize.value,
    filters: { ...filters.value, ...advancedParams.filters }
  }

  searching.value = true
  currentPage.value = 1

  // 执行高级搜索
  advancedSearchMessages(mergedParams).then(results => {
    searchResults.value = results.documents || []
    totalResults.value = results.total || 0
    showAdvancedSearch.value = false
  }).catch(error => {
    console.error('高级搜索失败:', error)
    ElMessage.error('高级搜索失败')
  }).finally(() => {
    searching.value = false
  })
}

const handleAdvancedSearchClose = () => {
  showAdvancedSearch.value = false
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  performSearch()
}

const handlePageChange = (newPage) => {
  currentPage.value = newPage
  performSearch()
}

const handleResultClick = (item) => {
  emit('result-click', {
    type: searchType.value,
    item: item
  })
}

const exportResults = () => {
  // 实现结果导出逻辑
  ElMessage.info('导出功能开发中...')
}

const toggleRealTimeSearch = () => {
  if (realTimeSearchActive.value) {
    // 停止实时搜索
    closeRealTimeSearch(realTimeSearchWs.value)
    realTimeSearchWs.value = null
    realTimeSearchActive.value = false
  } else {
    // 开始实时搜索
    if (props.enableRealTime) {
      realTimeSearchWs.value = createRealTimeSearch(
        (data) => {
          // 处理实时搜索结果
          if (data.type === 'result') {
            searchResults.value = data.results || []
            totalResults.value = data.total || 0
          }
        },
        (error) => {
          console.error('实时搜索连接错误:', error)
          ElMessage.error('实时搜索连接失败')
          realTimeSearchActive.value = false
        }
      )
      realTimeSearchActive.value = true
    }
  }
}

const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString()
}

// 监听搜索查询变化
watch(searchQuery, (newValue) => {
  if (newValue.trim()) {
    createDebouncedSearch()
  } else {
    suggestions.value = []
  }
})

// 生命周期
onMounted(() => {
  createDebouncedSearch()
  createDebouncedSuggestions()

  // 初始化数据
  if (props.showHistory) {
    fetchSearchHistory()
  }
  if (props.showHotKeywords) {
    fetchHotKeywords()
  }
})

onUnmounted(() => {
  // 清理防抖定时器
  if (debouncedSearch.value) {
    clearTimeout(debouncedSearch.value)
  }
  if (debouncedGetSuggestions.value) {
    clearTimeout(debouncedGetSuggestions.value)
  }

  // 关闭实时搜索连接
  if (realTimeSearchWs.value) {
    closeRealTimeSearch(realTimeSearchWs.value)
  }
})
</script>

<style scoped>
.enhanced-search {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

/* 搜索容器 */
.search-container {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.search-type-select {
  width: 120px;
}

/* 建议容器 */
.suggestions-container {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.suggestions-header {
  padding: 8px 16px;
  font-weight: 500;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
}

.suggestions-list {
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.suggestion-item:hover {
  background-color: #f5f7fa;
}

.suggestion-icon {
  color: #909399;
  margin-right: 8px;
}

.suggestion-text {
  color: #303133;
  font-size: 14px;
}

/* 历史容器 */
.history-container {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  font-weight: 500;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
}

.history-list {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-icon {
  color: #909399;
  margin-right: 8px;
}

.history-text {
  flex: 1;
  color: #303133;
  font-size: 14px;
}

.history-time {
  color: #909399;
  font-size: 12px;
}

/* 热门关键词容器 */
.hot-keywords-container {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 16px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.hot-keywords-header {
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
  font-size: 14px;
}

.hot-keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-keyword {
  cursor: pointer;
  transition: all 0.2s;
}

.hot-keyword:hover {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

/* 过滤器容器 */
.filters-container {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 16px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.filters-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  color:  #606266;
  margin-bottom: 12px;
  font-size: 14px;
}

/* 结果容器 */
.results-container {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.results-count {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.results-actions {
  display: flex;
  gap: 8px;
}

.results-list {
  min-height: 200px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 无结果 */
.no-results {
  padding: 40px 0;
  text-align: center;
}

/* 实时搜索指示器 */
.real-time-indicator {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #67c23a;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: pulse 2s infinite;
}

.real-time-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-container {
    flex-direction: column;
    align-items: stretch;
  }

  .search-type-select {
    width: 100%;
  }

  .suggestions-container,
  .history-container,
  .hot-keywords-container,
  .filters-container,
  .results-container {
    margin-left: 0;
    margin-right: 0;
  }

  .results-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .results-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>