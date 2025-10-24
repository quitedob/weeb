<template>
  <teleport to="body">
    <transition name="apple-modal">
      <div v-if="modelValue" class="apple-modal-overlay" @click="handleOverlayClick">
        <div
          ref="modalRef"
          :class="modalClass"
          :style="modalStyle"
          @click.stop
        >
          <!-- 模态框头部 -->
          <div v-if="showHeader" class="apple-modal-header">
            <slot name="header">
              <h3 class="apple-modal-title">{{ title }}</h3>
              <button
                v-if="showCloseButton"
                class="apple-modal-close"
                @click="handleClose"
                aria-label="关闭"
              >
                ✕
              </button>
            </slot>
          </div>

          <!-- 模态框内容 -->
          <div class="apple-modal-body">
            <slot></slot>
          </div>

          <!-- 模态框底部 -->
          <div v-if="showFooter" class="apple-modal-footer">
            <slot name="footer">
              <AppleButton
                v-if="showCancelButton"
                type="secondary"
                @click="handleCancel"
              >
                {{ cancelText }}
              </AppleButton>
              <AppleButton
                v-if="showConfirmButton"
                :type="confirmButtonType"
                :loading="confirmLoading"
                @click="handleConfirm"
              >
                {{ confirmText }}
              </AppleButton>
            </slot>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import AppleButton from './AppleButton.vue'

export default {
  name: 'AppleModal',
  components: {
    AppleButton
  },
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large', 'full'].includes(value)
    },
    closable: {
      type: Boolean,
      default: true
    },
    maskClosable: {
      type: Boolean,
      default: true
    },
    showHeader: {
      type: Boolean,
      default: true
    },
    showFooter: {
      type: Boolean,
      default: false
    },
    showCloseButton: {
      type: Boolean,
      default: true
    },
    showCancelButton: {
      type: Boolean,
      default: true
    },
    showConfirmButton: {
      type: Boolean,
      default: true
    },
    cancelText: {
      type: String,
      default: '取消'
    },
    confirmText: {
      type: String,
      default: '确定'
    },
    confirmButtonType: {
      type: String,
      default: 'primary'
    },
    confirmLoading: {
      type: Boolean,
      default: false
    },
    destroyOnClose: {
      type: Boolean,
      default: false
    },
    centered: {
      type: Boolean,
      default: false
    },
    width: {
      type: [String, Number],
      default: null
    },
    maxWidth: {
      type: [String, Number],
      default: null
    },
    zIndex: {
      type: Number,
      default: 1000
    }
  },
  emits: ['update:modelValue', 'close', 'cancel', 'confirm', 'opened', 'closed'],
  setup(props, { emit }) {
    const modalRef = ref(null)
    const previousActiveElement = ref(null)

    const modalClass = computed(() => {
      const classes = ['apple-modal']

      if (props.size) {
        classes.push(`apple-modal--${props.size}`)
      }

      if (props.centered) {
        classes.push('apple-modal--centered')
      }

      return classes.join(' ')
    })

    const modalStyle = computed(() => {
      const style = {
        zIndex: props.zIndex
      }

      if (props.width) {
        style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
      }

      if (props.maxWidth) {
        style.maxWidth = typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
      }

      return style
    })

    const handleOverlayClick = () => {
      if (props.maskClosable && props.closable) {
        handleClose()
      }
    }

    const handleClose = () => {
      emit('update:modelValue', false)
      emit('close')
    }

    const handleCancel = () => {
      emit('cancel')
      if (props.closable) {
        handleClose()
      }
    }

    const handleConfirm = () => {
      emit('confirm')
    }

    const handleKeydown = (event) => {
      if (event.key === 'Escape' && props.modelValue && props.closable) {
        handleClose()
      }
    }

    const focusModal = () => {
      nextTick(() => {
        if (modalRef.value) {
          modalRef.value.focus()
        }
      })
    }

    const savePreviousActiveElement = () => {
      previousActiveElement.value = document.activeElement
    }

    const restorePreviousActiveElement = () => {
      if (previousActiveElement.value && typeof previousActiveElement.value.focus === 'function') {
        previousActiveElement.value.focus()
      }
    }

    // 监听visible变化
    watch(() => props.modelValue, (newVal) => {
      if (newVal) {
        savePreviousActiveElement()
        nextTick(() => {
          emit('opened')
          focusModal()
        })
      } else {
        emit('closed')
        nextTick(() => {
          restorePreviousActiveElement()
        })
      }
    })

    onMounted(() => {
      document.addEventListener('keydown', handleKeydown)
    })

    onUnmounted(() => {
      document.removeEventListener('keydown', handleKeydown)
      restorePreviousActiveElement()
    })

    return {
      modalRef,
      modalClass,
      modalStyle,
      handleOverlayClick,
      handleClose,
      handleCancel,
      handleConfirm
    }
  }
}
</script>

<style scoped>
.apple-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.apple-modal {
  background: var(--apple-bg-primary);
  border-radius: 12px;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  outline: none;
}

/* 模态框尺寸 */
.apple-modal--small {
  width: 400px;
}

.apple-modal--medium {
  width: 520px;
}

.apple-modal--large {
  width: 800px;
}

.apple-modal--full {
  width: 90vw;
  height: 90vh;
}

.apple-modal--centered {
  margin: auto;
}

/* 模态框头部 */
.apple-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--apple-bg-quaternary);
}

.apple-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

.apple-modal-close {
  background: none;
  border: none;
  font-size: 18px;
  color: var(--apple-text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.apple-modal-close:hover {
  background: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
}

/* 模态框内容 */
.apple-modal-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  color: var(--apple-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
}

/* 模态框底部 */
.apple-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--apple-bg-quaternary);
  background: var(--apple-bg-secondary);
}

/* 动画效果 */
.apple-modal-enter-active,
.apple-modal-leave-active {
  transition: all 0.3s ease;
}

.apple-modal-enter-from,
.apple-modal-leave-to {
  opacity: 0;
}

.apple-modal-enter-active .apple-modal,
.apple-modal-leave-active .apple-modal {
  transition: all 0.3s ease;
}

.apple-modal-enter-from .apple-modal {
  transform: scale(0.9) translateY(-20px);
  opacity: 0;
}

.apple-modal-leave-to .apple-modal {
  transform: scale(0.9) translateY(-20px);
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-modal-overlay {
    padding: 16px;
  }

  .apple-modal--small,
  .apple-modal--medium,
  .apple-modal--large {
    width: 100%;
    max-width: none;
  }

  .apple-modal--full {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
  }

  .apple-modal-header {
    padding: 16px 20px;
  }

  .apple-modal-body {
    padding: 20px;
  }

  .apple-modal-footer {
    padding: 12px 20px;
  }

  .apple-modal-title {
    font-size: 16px;
  }

  .apple-modal-footer :deep(.apple-button) {
    flex: 1;
  }
}

/* 滚动条样式 */
.apple-modal-body::-webkit-scrollbar {
  width: 6px;
}

.apple-modal-body::-webkit-scrollbar-track {
  background: var(--apple-bg-secondary);
}

.apple-modal-body::-webkit-scrollbar-thumb {
  background: var(--apple-bg-quaternary);
  border-radius: 3px;
}

.apple-modal-body::-webkit-scrollbar-thumb:hover {
  background: var(--apple-text-tertiary);
}
</style>