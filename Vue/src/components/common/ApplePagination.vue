<template>
  <div class="apple-pagination" :class="paginationClasses">
    <div class="pagination-info">
      <span class="total-info">
        共 <span class="total">{{ total }}</span> 条
      </span>
      <span v-if="showSizeChanger" class="size-changer">
        <span class="size-label">每页显示</span>
        <AppleSelect
          v-model="currentPageSize"
          :options="pageSizes"
          size="small"
          @change="handleSizeChange"
        />
        <span class="size-label">条</span>
      </span>
    </div>

    <div class="pagination-controls">
      <button
        class="pagination-btn prev-btn"
        :disabled="currentPage <= 1"
        @click="handlePrev"
      >
        <i class="icon-chevron-left"></i>
      </button>

      <div class="pagination-pages">
        <!-- 第一页 -->
        <button
          v-if="showFirstLast"
          class="pagination-btn page-btn"
          :class="{ 'is-active': currentPage === 1 }"
          @click="handlePage(1)"
        >
          1
        </button>

        <!-- 前置省略号 -->
        <span v-if="showPrevEllipsis" class="pagination-ellipsis">...</span>

        <!-- 页码列表 -->
        <button
          v-for="page in visiblePages"
          :key="page"
          class="pagination-btn page-btn"
          :class="{ 'is-active': currentPage === page }"
          @click="handlePage(page)"
        >
          {{ page }}
        </button>

        <!-- 后置省略号 -->
        <span v-if="showNextEllipsis" class="pagination-ellipsis">...</span>

        <!-- 最后一页 -->
        <button
          v-if="showFirstLast && totalPages > 1"
          class="pagination-btn page-btn"
          :class="{ 'is-active': currentPage === totalPages }"
          @click="handlePage(totalPages)"
        >
          {{ totalPages }}
        </button>
      </div>

      <button
        class="pagination-btn next-btn"
        :disabled="currentPage >= totalPages"
        @click="handleNext"
      >
        <i class="icon-chevron-right"></i>
      </button>
    </div>

    <div v-if="showQuickJumper" class="pagination-jumper">
      <span class="jumper-label">前往</span>
      <AppleInput
        v-model="jumpPage"
        :placeholder="String(currentPage)"
        size="small"
        style="width: 60px;"
        @keyup.enter="handleJump"
      />
      <span class="jumper-label">页</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import AppleSelect from './AppleSelect.vue'
import AppleInput from './AppleInput.vue'

const props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  },
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  showSizeChanger: {
    type: Boolean,
    default: true
  },
  showQuickJumper: {
    type: Boolean,
    default: true
  },
  showFirstLast: {
    type: Boolean,
    default: false
  },
  pagerCount: {
    type: Number,
    default: 7
  }
})

const emit = defineEmits(['update:currentPage', 'update:pageSize', 'size-change', 'current-change'])

// 响应式数据
const currentPageSize = ref(props.pageSize)
const jumpPage = ref('')

// 计算属性
const paginationClasses = computed(() => ({
  'has-size-changer': props.showSizeChanger,
  'has-quick-jumper': props.showQuickJumper
}))

const totalPages = computed(() => {
  return Math.ceil(props.total / currentPageSize.value) || 1
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = props.currentPage
  const count = props.pagerCount

  if (total <= count) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const half = Math.floor(count / 2)
  let start = current - half
  let end = current + half

  if (start < 1) {
    start = 1
    end = count
  } else if (end > total) {
    end = total
    start = total - count + 1
  }

  // 调整显示范围，确保不包含第一页和最后一页（如果它们被单独显示）
  if (props.showFirstLast) {
    if (start <= 2) start = 2
    if (end >= total - 1) end = total - 1
  }

  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

const showPrevEllipsis = computed(() => {
  if (!props.showFirstLast) return false
  const firstVisiblePage = visiblePages.value[0]
  return firstVisiblePage > 2
})

const showNextEllipsis = computed(() => {
  if (!props.showFirstLast) return false
  const lastVisiblePage = visiblePages.value[visiblePages.value.length - 1]
  return lastVisiblePage < totalPages.value - 1
})

// 方法
const handlePrev = () => {
  if (props.currentPage > 1) {
    emit('update:currentPage', props.currentPage - 1)
    emit('current-change', props.currentPage - 1)
  }
}

const handleNext = () => {
  if (props.currentPage < totalPages.value) {
    emit('update:currentPage', props.currentPage + 1)
    emit('current-change', props.currentPage + 1)
  }
}

const handlePage = (page) => {
  if (page !== props.currentPage) {
    emit('update:currentPage', page)
    emit('current-change', page)
  }
}

const handleSizeChange = (size) => {
  currentPageSize.value = size
  emit('update:pageSize', size)
  emit('size-change', size)

  // 当页面大小改变时，重置到第一页
  if (props.currentPage !== 1) {
    emit('update:currentPage', 1)
    emit('current-change', 1)
  }
}

const handleJump = () => {
  const page = parseInt(jumpPage.value)
  if (!isNaN(page) && page >= 1 && page <= totalPages.value) {
    handlePage(page)
  }
  jumpPage.value = ''
}

// 监听器
watch(() => props.pageSize, (newSize) => {
  currentPageSize.value = newSize
})

watch(() => props.currentPage, () => {
  jumpPage.value = ''
})
</script>

<style scoped>
.apple-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--apple-spacing-md) 0;
  font-size: var(--apple-font-sm);
  color: var(--apple-text-secondary);
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-lg);
}

.total-info {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
}

.total {
  font-weight: 600;
  color: var(--apple-text-primary);
}

.size-changer {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
}

.size-label {
  color: var(--apple-text-secondary);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-xs);
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 2px;
}

.pagination-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 var(--apple-spacing-xs);
  border: 1px solid var(--apple-border-secondary);
  border-radius: var(--apple-border-radius-md);
  background-color: var(--apple-bg-primary);
  color: var(--apple-text-primary);
  font-size: var(--apple-font-sm);
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.pagination-btn:hover:not(:disabled) {
  border-color: var(--apple-blue);
  color: var(--apple-blue);
  background-color: var(--apple-bg-hover);
}

.pagination-btn:disabled {
  background-color: var(--apple-bg-disabled);
  color: var(--apple-text-disabled);
  border-color: var(--apple-border-disabled);
  cursor: not-allowed;
}

.pagination-btn.is-active {
  background-color: var(--apple-blue);
  border-color: var(--apple-blue);
  color: white;
}

.prev-btn,
.next-btn {
  padding: 0 var(--apple-spacing-sm);
}

.pagination-ellipsis {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  color: var(--apple-text-placeholder);
  user-select: none;
}

.pagination-jumper {
  display: flex;
  align-items: center;
  gap: var(--apple-spacing-sm);
}

.jumper-label {
  color: var(--apple-text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-pagination {
    flex-direction: column;
    gap: var(--apple-spacing-md);
    align-items: center;
  }

  .pagination-info {
    order: 2;
    flex-direction: column;
    gap: var(--apple-spacing-sm);
    text-align: center;
  }

  .pagination-controls {
    order: 1;
  }

  .pagination-jumper {
    order: 3;
  }
}

@media (max-width: 480px) {
  .pagination-pages {
    gap: 1px;
  }

  .pagination-btn {
    min-width: 28px;
    height: 28px;
    font-size: var(--apple-font-xs);
  }

  .size-changer {
    flex-direction: column;
    gap: var(--apple-spacing-xs);
    align-items: center;
  }
}

/* 动画效果 */
.pagination-btn {
  position: relative;
  overflow: hidden;
}

.pagination-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              height 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.pagination-btn:active::before {
  width: 100px;
  height: 100px;
}

/* 焦点样式 */
.pagination-btn:focus-visible {
  outline: 2px solid var(--apple-blue);
  outline-offset: 2px;
}
</style>