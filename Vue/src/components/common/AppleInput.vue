<template>
  <div :class="inputWrapperClass">
    <label v-if="label" :for="inputId" class="apple-input-label">
      {{ label }}
      <span v-if="required" class="required-mark">*</span>
    </label>

    <div class="apple-input-container">
      <span v-if="$slots.prefix" class="input-prefix">
        <slot name="prefix"></slot>
      </span>

      <input
        :id="inputId"
        ref="inputRef"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        :class="inputClass"
        @input="handleInput"
        @change="handleChange"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />

      <span v-if="showPasswordToggle" class="password-toggle" @click="togglePassword">
        {{ showPassword ? '👁️' : '🙈' }}
      </span>

      <span v-if="$slots.suffix" class="input-suffix">
        <slot name="suffix"></slot>
      </span>

      <span v-if="showClearButton && modelValue" class="clear-button" @click="clearInput">
        ✕
      </span>
    </div>

    <div v-if="errorMessage" class="apple-input-error">
      {{ errorMessage }}
    </div>

    <div v-if="helpText" class="apple-input-help">
      {{ helpText }}
    </div>
  </div>
</template>

<script>
import { ref, computed, nextTick } from 'vue'

export default {
  name: 'AppleInput',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    type: {
      type: String,
      default: 'text'
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
    required: {
      type: Boolean,
      default: false
    },
    error: {
      type: String,
      default: ''
    },
    helpText: {
      type: String,
      default: ''
    },
    maxlength: {
      type: [String, Number],
      default: null
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
    showPasswordToggle: {
      type: Boolean,
      default: false
    },
    autofocus: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'change', 'focus', 'blur', 'keydown', 'clear'],
  setup(props, { emit }) {
    const inputRef = ref(null)
    const isFocused = ref(false)
    const showPassword = ref(false)

    const inputId = computed(() => `apple-input-${Math.random().toString(36).substr(2, 9)}`)

    const inputType = computed(() => {
      if (props.type === 'password' && props.showPasswordToggle) {
        return showPassword.value ? 'text' : 'password'
      }
      return props.type
    })

    const inputWrapperClass = computed(() => {
      const classes = ['apple-input-wrapper']

      if (props.size) {
        classes.push(`apple-input-wrapper--${props.size}`)
      }

      if (props.disabled) {
        classes.push('apple-input-wrapper--disabled')
      }

      if (props.error || errorMessage.value) {
        classes.push('apple-input-wrapper--error')
      }

      if (isFocused.value) {
        classes.push('apple-input-wrapper--focused')
      }

      return classes.join(' ')
    })

    const inputClass = computed(() => {
      const classes = ['apple-input']

      if (props.size) {
        classes.push(`apple-input--${props.size}`)
      }

      if (props.disabled) {
        classes.push('apple-input--disabled')
      }

      if (props.readonly) {
        classes.push('apple-input--readonly')
      }

      return classes.join(' ')
    })

    const errorMessage = computed(() => {
      return props.error || ''
    })

    const showClearButton = computed(() => {
      return props.clearable && !props.disabled && !props.readonly
    })

    const handleInput = (event) => {
      emit('update:modelValue', event.target.value)
    }

    const handleChange = (event) => {
      emit('change', event.target.value)
    }

    const handleFocus = (event) => {
      isFocused.value = true
      emit('focus', event)
    }

    const handleBlur = (event) => {
      isFocused.value = false
      emit('blur', event)
    }

    const handleKeydown = (event) => {
      emit('keydown', event)
    }

    const togglePassword = () => {
      showPassword.value = !showPassword.value
    }

    const clearInput = () => {
      emit('update:modelValue', '')
      emit('clear')
      nextTick(() => {
        inputRef.value?.focus()
      })
    }

    const focus = () => {
      inputRef.value?.focus()
    }

    const blur = () => {
      inputRef.value?.blur()
    }

    // 自动聚焦
    if (props.autofocus) {
      nextTick(() => {
        focus()
      })
    }

    return {
      inputRef,
      inputId,
      inputType,
      inputWrapperClass,
      inputClass,
      errorMessage,
      showClearButton,
      showPassword,
      handleInput,
      handleChange,
      handleFocus,
      handleBlur,
      handleKeydown,
      togglePassword,
      clearInput,
      focus,
      blur
    }
  }
}
</script>

<style scoped>
.apple-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.apple-input-label {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-primary);
  margin-bottom: 2px;
}

.required-mark {
  color: var(--apple-red);
  margin-left: 2px;
}

.apple-input-container {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-bg-quaternary);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.apple-input-wrapper--focused .apple-input-container {
  border-color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.apple-input-wrapper--error .apple-input-container {
  border-color: var(--apple-red);
}

.apple-input-wrapper--disabled .apple-input-container {
  background: var(--apple-bg-secondary);
  opacity: 0.6;
}

.input-prefix,
.input-suffix {
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: var(--apple-text-tertiary);
  font-size: 14px;
}

.apple-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
  color: var(--apple-text-primary);
  transition: all 0.2s ease;
}

.apple-input::placeholder {
  color: var(--apple-text-quaternary);
}

.apple-input--disabled {
  cursor: not-allowed;
  color: var(--apple-text-tertiary);
}

.apple-input--readonly {
  cursor: default;
}

/* 输入框尺寸 */
.apple-input-wrapper--small .apple-input {
  padding: 6px 8px;
  font-size: 12px;
  height: 28px;
}

.apple-input-wrapper--medium .apple-input {
  padding: 8px 12px;
  font-size: 14px;
  height: 34px;
}

.apple-input-wrapper--large .apple-input {
  padding: 12px 16px;
  font-size: 16px;
  height: 44px;
}

/* 密码切换按钮 */
.password-toggle {
  padding: 0 12px;
  cursor: pointer;
  color: var(--apple-text-tertiary);
  font-size: 16px;
  user-select: none;
  transition: color 0.2s ease;
}

.password-toggle:hover {
  color: var(--apple-text-secondary);
}

/* 清除按钮 */
.clear-button {
  padding: 0 12px;
  cursor: pointer;
  color: var(--apple-text-tertiary);
  font-size: 14px;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.clear-button:hover {
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-secondary);
}

/* 错误信息 */
.apple-input-error {
  font-size: 12px;
  color: var(--apple-red);
  margin-top: 2px;
}

/* 帮助文本 */
.apple-input-help {
  font-size: 12px;
  color: var(--apple-text-tertiary);
  margin-top: 2px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-input-wrapper--small .apple-input {
    height: 32px;
    font-size: 13px;
  }

  .apple-input-wrapper--medium .apple-input {
    height: 38px;
    font-size: 15px;
  }

  .apple-input-wrapper--large .apple-input {
    height: 48px;
    font-size: 16px;
  }
}
</style>