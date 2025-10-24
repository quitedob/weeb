<template>
  <div class="apple-textarea" :class="{ 'has-error': error }">
    <label v-if="label" class="textarea-label">{{ label }}</label>
    <textarea
      :value="modelValue"
      @input="handleInput"
      @blur="handleBlur"
      @focus="handleFocus"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :rows="rows"
      :maxlength="maxlength"
      :class="textareaClass"
      class="apple-textarea-input"
      ref="textareaRef"
    ></textarea>
    <div v-if="showWordCount && maxlength" class="word-count">
      {{ currentLength }}/{{ maxlength }}
    </div>
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  rows: {
    type: Number,
    default: 4
  },
  maxlength: {
    type: Number,
    default: null
  },
  showWordCount: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  resize: {
    type: String,
    default: 'vertical',
    validator: (value) => ['none', 'both', 'horizontal', 'vertical'].includes(value)
  },
  autoResize: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'change'])

const textareaRef = ref(null)
const isFocused = ref(false)

const currentLength = computed(() => {
  return String(props.modelValue || '').length
})

const textareaClass = computed(() => {
  return [
    `size-${props.size}`,
    {
      'focused': isFocused.value,
      'disabled': props.disabled,
      'readonly': props.readonly,
      'has-error': props.error
    }
  ]
})

const handleInput = (event) => {
  const value = event.target.value
  emit('update:modelValue', value)
  emit('change', value)

  if (props.autoResize) {
    nextTick(() => {
      autoResize()
    })
  }
}

const handleBlur = (event) => {
  isFocused.value = false
  emit('blur', event)
}

const handleFocus = (event) => {
  isFocused.value = true
  emit('focus', event)
}

const autoResize = () => {
  if (textareaRef.value && props.autoResize) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = textareaRef.value.scrollHeight + 'px'
  }
}

const focus = () => {
  if (textareaRef.value) {
    textareaRef.value.focus()
  }
}

const blur = () => {
  if (textareaRef.value) {
    textareaRef.value.blur()
  }
}

defineExpose({
  focus,
  blur,
  textareaRef
})
</script>

<style scoped>
.apple-textarea {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.textarea-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-primary);
  margin-bottom: 4px;
}

.apple-textarea-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--apple-text-primary);
  background-color: var(--apple-bg-primary);
  transition: all 0.2s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', sans-serif;
  resize: v-bind(resize);
  outline: none;
}

.apple-textarea-input:hover {
  border-color: var(--apple-bg-tertiary);
}

.apple-textarea-input.focused {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.apple-textarea-input.disabled {
  background-color: var(--apple-bg-quaternary);
  color: var(--apple-text-tertiary);
  cursor: not-allowed;
  opacity: 0.6;
}

.apple-textarea-input.readonly {
  background-color: var(--apple-bg-secondary);
  cursor: default;
}

.apple-textarea-input.has-error {
  border-color: var(--apple-red);
  box-shadow: 0 0 0 3px rgba(255, 59, 48, 0.1);
}

.apple-textarea-input::placeholder {
  color: var(--apple-text-tertiary);
}

/* Size variants */
.apple-textarea-input.size-small {
  padding: 8px 12px;
  font-size: 12px;
}

.apple-textarea-input.size-medium {
  padding: 12px 16px;
  font-size: 14px;
}

.apple-textarea-input.size-large {
  padding: 16px 20px;
  font-size: 16px;
}

/* Word count */
.word-count {
  font-size: 12px;
  color: var(--apple-text-tertiary);
  text-align: right;
  margin-top: 4px;
}

/* Error message */
.error-message {
  font-size: 12px;
  color: var(--apple-red);
  margin-top: 4px;
}

/* Error state */
.apple-textarea.has-error .apple-textarea-input {
  border-color: var(--apple-red);
}

.apple-textarea.has-error .textarea-label {
  color: var(--apple-red);
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .apple-textarea-input {
    background-color: var(--apple-bg-primary);
    border-color: var(--apple-bg-quaternary);
    color: var(--apple-text-primary);
  }

  .apple-textarea-input:hover {
    border-color: var(--apple-bg-tertiary);
  }

  .apple-textarea-input::placeholder {
    color: var(--apple-text-tertiary);
  }

  .word-count {
    color: var(--apple-text-tertiary);
  }
}

/* 滚动条样式 */
.apple-textarea-input::-webkit-scrollbar {
  width: 6px;
}

.apple-textarea-input::-webkit-scrollbar-track {
  background: transparent;
}

.apple-textarea-input::-webkit-scrollbar-thumb {
  background: var(--apple-bg-quaternary);
  border-radius: 3px;
}

.apple-textarea-input::-webkit-scrollbar-thumb:hover {
  background: var(--apple-bg-tertiary);
}

/* 禁用状态下的滚动条 */
.apple-textarea-input.disabled::-webkit-scrollbar {
  display: none;
}

/* 自动调整高度时禁用手动调整 */
.apple-textarea-input[style*="height"] {
  resize: none;
}
</style>