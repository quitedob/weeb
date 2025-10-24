<template>
  <div class="apple-table" :class="tableClasses">
    <div class="table-container" ref="tableContainer">
      <table class="table">
        <thead class="table-header">
          <tr>
            <th
              v-for="column in columns"
              :key="column.prop || column.type"
              :class="getColumnClasses(column)"
              :style="getColumnStyle(column)"
              @click="handleHeaderClick(column)"
            >
              <div class="cell">
                <div class="cell-content">
                  <input
                    v-if="column.type === 'selection'"
                    type="checkbox"
                    :checked="isAllSelected"
                    :indeterminate="isIndeterminate"
                    @change="handleSelectAll"
                    class="selection-checkbox"
                  />
                  <span v-else class="column-label">{{ column.label }}</span>
                </div>
                <div v-if="column.sortable" class="sort-icon">
                  <i
                    class="icon-caret-up"
                    :class="{ 'is-active': sortProp === column.prop && sortOrder === 'ascending' }"
                  ></i>
                  <i
                    class="icon-caret-down"
                    :class="{ 'is-active': sortProp === column.prop && sortOrder === 'descending' }"
                  ></i>
                </div>
              </div>
            </th>
          </tr>
        </thead>
        <tbody class="table-body">
          <tr
            v-for="(row, index) in data"
            :key="getRowKey(row, index)"
            :class="getRowClasses(row, index)"
            @click="handleRowClick(row, index)"
          >
            <td
              v-for="column in columns"
              :key="column.prop || column.type"
              :class="getCellClasses(column)"
              :style="getColumnStyle(column)"
            >
              <div class="cell">
                <!-- 选择列 -->
                <input
                  v-if="column.type === 'selection'"
                  type="checkbox"
                  :checked="isRowSelected(row)"
                  @change="handleRowSelect(row, $event)"
                  class="selection-checkbox"
                />

                <!-- 展开列 -->
                <button
                  v-else-if="column.type === 'expand'"
                  class="expand-button"
                  @click="toggleRowExpand(row, index)"
                >
                  <i class="icon-chevron-right" :class="{ 'is-expanded': isRowExpanded(row) }"></i>
                </button>

                <!-- 操作列 -->
                <div v-else-if="column.type === 'action'" class="action-cell">
                  <slot name="action" :row="row" :index="index">
                    <AppleButton
                      v-for="(action, actionIndex) in column.actions"
                      :key="actionIndex"
                      :variant="action.variant || 'default'"
                      :size="action.size || 'small'"
                      :disabled="action.disabled"
                      :loading="action.loading"
                      @click="action.handler(row, index)"
                    >
                      {{ action.label }}
                    </AppleButton>
                  </slot>
                </div>

                <!-- 自定义插槽 -->
                <slot
                  v-else-if="$slots[column.prop]"
                  :name="column.prop"
                  :row="row"
                  :index="index"
                  :column="column"
                />

                <!-- 普通数据列 -->
                <span v-else class="cell-text">
                  {{ getCellValue(row, column) }}
                </span>
              </div>
            </td>
          </tr>

          <!-- 空数据提示 -->
          <tr v-if="data.length === 0" class="empty-row">
            <td :colspan="columns.length" class="empty-cell">
              <div class="empty-content">
                <i class="icon-inbox"></i>
                <span class="empty-text">{{ emptyText }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 展开行 -->
      <div v-if="expandedRows.length > 0" class="expanded-rows">
        <div
          v-for="row in expandedRows"
          :key="getExpandedRowKey(row)"
          class="expanded-row"
        >
          <slot name="expand" :row="row"></slot>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="table-loading">
      <div class="loading-spinner"></div>
      <span class="loading-text">{{ loadingText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import AppleButton from './AppleButton.vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  stripe: {
    type: Boolean,
    default: false
  },
  border: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  loading: {
    type: Boolean,
    default: false
  },
  loadingText: {
    type: String,
    default: '加载中...'
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  rowKey: {
    type: [String, Function],
    default: 'id'
  },
  defaultSort: {
    type: Object,
    default: () => ({})
  },
  highlightCurrentRow: {
    type: Boolean,
    default: false
  },
  showOverflowTooltip: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'selection-change',
  'sort-change',
  'row-click',
  'row-expand',
  'header-click'
])

// 响应式数据
const selectedRows = ref([])
const expandedRows = ref([])
const currentRow = ref(null)
const sortProp = ref(props.defaultSort.prop || '')
const sortOrder = ref(props.defaultSort.order || '')

// 计算属性
const tableClasses = computed(() => ({
  'is-bordered': props.border,
  'is-striped': props.stripe,
  [`is-${props.size}`]: props.size,
  'is-loading': props.loading
}))

const isAllSelected = computed(() => {
  return props.data.length > 0 && selectedRows.value.length === props.data.length
})

const isIndeterminate = computed(() => {
  return selectedRows.value.length > 0 && selectedRows.value.length < props.data.length
})

// 方法
const getColumnClasses = (column) => {
  return {
    'is-sortable': column.sortable,
    'is-selection': column.type === 'selection',
    'is-expand': column.type === 'expand',
    'is-action': column.type === 'action',
    [`text-${column.align}`]: column.align
  }
}

const getColumnStyle = (column) => {
  const style = {}
  if (column.width) style.width = column.width
  if (column.minWidth) style.minWidth = column.minWidth
  if (column.maxWidth) style.maxWidth = column.maxWidth
  return style
}

const getRowClasses = (row, index) => {
  return {
    'is-striped': props.stripe && index % 2 === 1,
    'is-current': props.highlightCurrentRow && currentRow.value === row
  }
}

const getCellClasses = (column) => {
  return {
    [`text-${column.align}`]: column.align
  }
}

const getRowKey = (row, index) => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey(row)
  }
  return row[props.rowKey] || index
}

const getExpandedRowKey = (row) => {
  return getRowKey(row)
}

const getCellValue = (row, column) => {
  if (column.formatter) {
    return column.formatter(row, column, row[column.prop])
  }
  return row[column.prop]
}

const isRowSelected = (row) => {
  return selectedRows.value.some(selected => getRowKey(selected) === getRowKey(row))
}

const isRowExpanded = (row) => {
  return expandedRows.value.some(expanded => getRowKey(expanded) === getRowKey(row))
}

// 事件处理
const handleSelectAll = (event) => {
  const checked = event.target.checked
  if (checked) {
    selectedRows.value = [...props.data]
  } else {
    selectedRows.value = []
  }
  emit('selection-change', selectedRows.value)
}

const handleRowSelect = (row, event) => {
  const checked = event.target.checked
  const rowIndex = selectedRows.value.findIndex(selected => getRowKey(selected) === getRowKey(row))

  if (checked && rowIndex === -1) {
    selectedRows.value.push(row)
  } else if (!checked && rowIndex > -1) {
    selectedRows.value.splice(rowIndex, 1)
  }

  emit('selection-change', selectedRows.value)
}

const handleRowClick = (row, index) => {
  if (props.highlightCurrentRow) {
    currentRow.value = row
  }
  emit('row-click', row, index)
}

const handleHeaderClick = (column) => {
  if (column.sortable) {
    const currentProp = sortProp.value
    const currentOrder = sortOrder.value

    if (currentProp === column.prop) {
      // 切换排序顺序
      sortOrder.value = currentOrder === 'ascending' ? 'descending' : 'ascending'
    } else {
      // 切换排序列
      sortProp.value = column.prop
      sortOrder.value = 'ascending'
    }

    emit('sort-change', { prop: sortProp.value, order: sortOrder.value })
  }

  emit('header-click', column)
}

const toggleRowExpand = (row, index) => {
  const rowIndex = expandedRows.value.findIndex(expanded => getRowKey(expanded) === getRowKey(row))

  if (rowIndex > -1) {
    expandedRows.value.splice(rowIndex, 1)
  } else {
    expandedRows.value.push(row)
  }

  emit('row-expand', row, expandedRows.value.includes(row))
}

// 公开方法
const clearSelection = () => {
  selectedRows.value = []
  emit('selection-change', selectedRows.value)
}

const toggleRowSelection = (row, selected) => {
  const rowIndex = selectedRows.value.findIndex(item => getRowKey(item) === getRowKey(row))

  if (selected && rowIndex === -1) {
    selectedRows.value.push(row)
  } else if (!selected && rowIndex > -1) {
    selectedRows.value.splice(rowIndex, 1)
  }

  emit('selection-change', selectedRows.value)
}

const toggleAllSelection = () => {
  if (selectedRows.value.length === props.data.length) {
    selectedRows.value = []
  } else {
    selectedRows.value = [...props.data]
  }
  emit('selection-change', selectedRows.value)
}

const clearSort = () => {
  sortProp.value = ''
  sortOrder.value = ''
}

defineExpose({
  clearSelection,
  toggleRowSelection,
  toggleAllSelection,
  clearSort
})
</script>

<style scoped>
.apple-table {
  position: relative;
  width: 100%;
  background-color: var(--apple-bg-primary);
  border-radius: var(--apple-border-radius-lg);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  border-spacing: 0;
}

.table-header {
  background-color: var(--apple-bg-secondary);
}

.table-header th {
  padding: var(--apple-spacing-md);
  text-align: left;
  font-weight: 600;
  color: var(--apple-text-primary);
  border-bottom: 1px solid var(--apple-border-secondary);
  position: relative;
  user-select: none;
}

.table-header th.is-sortable {
  cursor: pointer;
  transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.table-header th.is-sortable:hover {
  background-color: var(--apple-bg-hover);
}

.cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 20px;
}

.cell-content {
  display: flex;
  align-items: center;
  flex: 1;
}

.column-label {
  color: inherit;
}

.sort-icon {
  display: flex;
  flex-direction: column;
  margin-left: var(--apple-spacing-xs);
  color: var(--apple-text-tertiary);
}

.sort-icon i {
  font-size: 12px;
  line-height: 1;
  transition: color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.sort-icon i.is-active {
  color: var(--apple-blue);
}

.table-body td {
  padding: var(--apple-spacing-md);
  border-bottom: 1px solid var(--apple-border-secondary);
  color: var(--apple-text-primary);
}

.table-body tr {
  transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.table-body tr:hover {
  background-color: var(--apple-bg-hover);
}

.table-body tr.is-striped {
  background-color: var(--apple-bg-striped);
}

.table-body tr.is-current {
  background-color: rgba(0, 122, 255, 0.1);
}

.apple-table.is-bordered .table-body td {
  border-right: 1px solid var(--apple-border-secondary);
}

.apple-table.is-bordered .table-body td:last-child {
  border-right: none;
}

.selection-checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--apple-blue);
  cursor: pointer;
}

.expand-button {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--apple-text-secondary);
  transition: transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.expand-button .icon-chevron-right {
  transition: transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.expand-button .icon-chevron-right.is-expanded {
  transform: rotate(90deg);
}

.action-cell {
  display: flex;
  gap: var(--apple-spacing-xs);
  align-items: center;
}

.cell-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-row {
  height: 80px;
}

.empty-cell {
  text-align: center;
  padding: var(--apple-spacing-lg) !important;
  color: var(--apple-text-tertiary);
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-content i {
  font-size: 24px;
  margin-bottom: var(--apple-spacing-sm);
  color: var(--apple-text-placeholder);
}

.empty-text {
  font-size: var(--apple-font-sm);
}

.table-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--apple-border-secondary);
  border-top: 3px solid var(--apple-blue);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--apple-spacing-sm);
}

.loading-text {
  color: var(--apple-text-secondary);
  font-size: var(--apple-font-sm);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 尺寸变体 */
.apple-table.is-small .table-header th,
.apple-table.is-small .table-body td {
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
}

.apple-table.is-large .table-header th,
.apple-table.is-large .table-body td {
  padding: var(--apple-spacing-lg) var(--apple-spacing-xl);
}

/* 文本对齐 */
.text-left {
  text-align: left;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

/* 滚动条样式 */
.table-container::-webkit-scrollbar {
  height: 6px;
}

.table-container::-webkit-scrollbar-track {
  background: transparent;
}

.table-container::-webkit-scrollbar-thumb {
  background-color: var(--apple-border-secondary);
  border-radius: 3px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background-color: var(--apple-border-primary);
}
</style>