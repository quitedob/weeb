<template>
  <div class="apple-select" :class="selectClasses">
    <div class="select-trigger" @click="toggleDropdown" :class="{ 'is-open': isOpen }">
      <span class="select-value" :class="{ 'is-placeholder': !selectedLabel }">
        {{ selectedLabel || placeholder }}
      </span>
      <span class="select-arrow" :class="{ 'is-open': isOpen }">
        <i class="icon-chevron-down"></i>
      </span>
    </div>

    <transition name="dropdown">
      <div v-show="isOpen" class="select-dropdown" @click.stop>
        <div v-if="filterable" class="select-filter">
          <AppleInput
            v-model="searchQuery"
            :placeholder="filterPlaceholder"
            @input="handleSearch"
            ref="searchInput"
            size="small"
          />
        </div>

        <div class="select-options" ref="optionsContainer">
          <div
            v-for="option in filteredOptions"
            :key="option.value"
            class="select-option"
            :class="{
              'is-selected': isSelected(option),
              'is-disabled': option.disabled
            }"
            @click="selectOption(option)"
          >
            <span class="option-label">{{ option.label }}</span>
            <span v-if="isSelected(option)" class="option-check">
              <i class="icon-check"></i>
            </span>
          </div>

          <div v-if="filteredOptions.length === 0" class="select-empty">
            <i class="icon-inbox"></i>
            <span>暂无数据</span>
          </div>
        </div>

        <div v-if="clearable && selectedValue !== null" class="select-footer">
          <AppleButton variant="text" size="small" @click="clearSelection">
            清空选择
          </AppleButton>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import AppleInput from './AppleInput.vue'
import AppleButton from './AppleButton.vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, Object, Array],
    default: null
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  clearable: {
    type: Boolean,
    default: false
  },
  filterable: {
    type: Boolean,
    default: false
  },
  filterPlaceholder: {
    type: String,
    default: '搜索选项'
  },
  multiple: {
    type: Boolean,
    default: false
  },
  options: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'clear', 'search'])

// 响应式数据
const isOpen = ref(false)
const searchQuery = ref('')
const selectedValue = ref(props.modelValue)
const searchInput = ref(null)
const optionsContainer = ref(null)

// 计算属性
const selectClasses = computed(() => ({
  'is-disabled': props.disabled,
  [`is-${props.size}`]: props.size,
  'is-open': isOpen.value
}))

const selectedLabel = computed(() => {
  if (props.multiple && Array.isArray(selectedValue.value)) {
    return selectedValue.value.map(val => {
      const option = props.options.find(opt => opt.value === val)
      return option ? option.label : val
    }).join(', ')
  }

  const option = props.options.find(opt => opt.value === selectedValue.value)
  return option ? option.label : ''
})

const filteredOptions = computed(() => {
  if (!props.filterable || !searchQuery.value) {
    return props.options
  }

  const query = searchQuery.value.toLowerCase()
  return props.options.filter(option =>
    option.label && option.label.toLowerCase().includes(query)
  )
})

// 方法
const toggleDropdown = () => {
  if (props.disabled) return

  isOpen.value = !isOpen.value

  if (isOpen.value) {
    nextTick(() => {
      if (props.filterable) {
        searchInput.value?.focus()
      }
      scrollToSelected()
    })
  }
}

const selectOption = (option) => {
  if (option.disabled) return

  if (props.multiple) {
    handleMultipleSelection(option)
  } else {
    selectedValue.value = option.value
    isOpen.value = false
    emit('update:modelValue', option.value)
    emit('change', option.value, option)
  }

  searchQuery.value = ''
}

const handleMultipleSelection = (option) => {
  if (!Array.isArray(selectedValue.value)) {
    selectedValue.value = []
  }

  const index = selectedValue.value.indexOf(option.value)
  if (index > -1) {
    selectedValue.value.splice(index, 1)
  } else {
    selectedValue.value.push(option.value)
  }

  emit('update:modelValue', [...selectedValue.value])
  emit('change', [...selectedValue.value], option)
}

const isSelected = (option) => {
  if (props.multiple && Array.isArray(selectedValue.value)) {
    return selectedValue.value.includes(option.value)
  }
  return selectedValue.value === option.value
}

const clearSelection = () => {
  selectedValue.value = props.multiple ? [] : null
  emit('update:modelValue', selectedValue.value)
  emit('clear')
  searchQuery.value = ''
}

const handleSearch = () => {
  emit('search', searchQuery.value)
}

const scrollToSelected = () => {
  if (!optionsContainer.value) return

  const selectedOption = optionsContainer.value.querySelector('.select-option.is-selected')
  if (selectedOption) {
    selectedOption.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }
}

const closeDropdown = () => {
  isOpen.value = false
  searchQuery.value = ''
}

const handleClickOutside = (event) => {
  if (!event.target.closest('.apple-select')) {
    closeDropdown()
  }
}

// 监听器
watch(() => props.modelValue, (newValue) => {
  selectedValue.value = newValue
})

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.apple-select {
  position: relative;
  display: inline-block;
  width: 100%;
  vertical-align: middle;
}

.select-trigger {
  position: relative;
  display: flex;
  align-items: center;
  padding: var(--apple-input-padding);
  border: 1px solid var(--apple-border-primary);
  border-radius: var(--apple-border-radius-lg);
  background-color: var(--apple-bg-primary);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.select-trigger:hover {
  border-color: var(--apple-blue);
}

.select-trigger.is-open {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.2);
}

.apple-select.is-disabled .select-trigger {
  background-color: var(--apple-bg-disabled);
  color: var(--apple-text-disabled);
  cursor: not-allowed;
  border-color: var(--apple-border-disabled);
}

.select-value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--apple-text-primary);
}

.select-value.is-placeholder {
  color: var(--apple-text-placeholder);
}

.select-arrow {
  margin-left: var(--apple-spacing-sm);
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--apple-text-secondary);
}

.select-arrow.is-open {
  transform: rotate(180deg);
}

.select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 1000;
  margin-top: 2px;
  background-color: var(--apple-bg-elevated);
  border: 1px solid var(--apple-border-secondary);
  border-radius: var(--apple-border-radius-lg);
  box-shadow: var(--apple-shadow-large);
  backdrop-filter: blur(20px);
  max-height: 276px;
  overflow: hidden;
}

.select-filter {
  padding: var(--apple-spacing-sm);
  border-bottom: 1px solid var(--apple-border-secondary);
}

.select-options {
  max-height: 200px;
  overflow-y: auto;
  padding: var(--apple-spacing-xs) 0;
}

.select-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  cursor: pointer;
  transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.select-option:hover {
  background-color: var(--apple-bg-hover);
}

.select-option.is-selected {
  background-color: var(--apple-blue);
  color: white;
}

.select-option.is-disabled {
  color: var(--apple-text-disabled);
  cursor: not-allowed;
  pointer-events: none;
}

.option-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-check {
  margin-left: var(--apple-spacing-sm);
  color: currentColor;
}

.select-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--apple-spacing-lg) var(--apple-spacing-md);
  color: var(--apple-text-tertiary);
}

.select-empty i {
  font-size: 24px;
  margin-bottom: var(--apple-spacing-sm);
}

.select-footer {
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  border-top: 1px solid var(--apple-border-secondary);
  text-align: center;
}

/* 尺寸变体 */
.apple-select.is-small .select-trigger {
  padding: var(--apple-spacing-xs) var(--apple-spacing-sm);
  font-size: var(--apple-font-sm);
}

.apple-select.is-large .select-trigger {
  padding: var(--apple-spacing-md) var(--apple-spacing-lg);
  font-size: var(--apple-font-lg);
}

/* 动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

/* 滚动条样式 */
.select-options::-webkit-scrollbar {
  width: 6px;
}

.select-options::-webkit-scrollbar-track {
  background: transparent;
}

.select-options::-webkit-scrollbar-thumb {
  background-color: var(--apple-border-secondary);
  border-radius: 3px;
}

.select-options::-webkit-scrollbar-thumb:hover {
  background-color: var(--apple-border-primary);
}
</style>