<template>
  <div class="apple-option" :class="optionClasses" @click="handleClick">
    <span class="option-label">{{ label }}</span>
    <span v-if="isSelected" class="option-check">
      <i class="icon-check"></i>
    </span>
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number, Boolean, Object],
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

// 注入父组件的状态
const selectState = inject('selectState', null)

// 计算属性
const optionClasses = computed(() => ({
  'is-selected': isSelected.value,
  'is-disabled': props.disabled
}))

const isSelected = computed(() => {
  if (!selectState) return false

  if (selectState.multiple && Array.isArray(selectState.modelValue)) {
    return selectState.modelValue.includes(props.value)
  }

  return selectState.modelValue === props.value
})

// 方法
const handleClick = () => {
  if (props.disabled) return

  emit('click', props.value, props)

  if (selectState && selectState.selectOption) {
    selectState.selectOption({
      label: props.label,
      value: props.value,
      disabled: props.disabled
    })
  }
}
</script>

<style scoped>
.apple-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--apple-spacing-sm) var(--apple-spacing-md);
  cursor: pointer;
  transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: var(--apple-border-radius-md);
  margin: 0 var(--apple-spacing-xs);
}

.apple-option:hover {
  background-color: var(--apple-bg-hover);
}

.apple-option.is-selected {
  background-color: var(--apple-blue);
  color: white;
}

.apple-option.is-disabled {
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
  font-size: 14px;
}

.apple-option.is-disabled .option-check {
  color: var(--apple-text-disabled);
}
</style>