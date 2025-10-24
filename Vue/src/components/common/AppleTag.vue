<template>
  <span class="apple-tag" :class="tagClasses" @click="handleClick">
    <span v-if="icon" class="tag-icon">
      <i :class="icon"></i>
    </span>
    <span class="tag-content">
      <slot>{{ text }}</slot>
    </span>
    <button
      v-if="closable"
      type="button"
      class="tag-close"
      @click.stop="handleClose"
    >
      <i class="icon-close"></i>
    </button>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'default',
    validator: (value) => [
      'default', 'primary', 'success', 'warning', 'danger', 'info'
    ].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  effect: {
    type: String,
    default: 'light',
    validator: (value) => ['dark', 'light', 'plain'].includes(value)
  },
  closable: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  hit: {
    type: Boolean,
    default: false
  },
  color: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  round: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click', 'close'])

// 计算属性
const tagClasses = computed(() => {
  return [
    `is-${props.type}`,
    `is-${props.effect}`,
    `is-${props.size}`,
    {
      'is-closable': props.closable,
      'is-disabled': props.disabled,
      'is-hit': props.hit,
      'is-round': props.round,
      'has-color': props.color
    }
  ]
})

// 方法
const handleClick = (event) => {
  if (!props.disabled) {
    emit('click', event)
  }
}

const handleClose = (event) => {
  if (!props.disabled) {
    emit('close', event)
  }
}
</script>

<style scoped>
.apple-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: var(--tag-height, 24px);
  padding: var(--tag-padding, 0 var(--apple-spacing-sm));
  font-size: var(--apple-font-sm);
  line-height: 1;
  border-radius: var(--tag-border-radius, var(--apple-border-radius-sm));
  white-space: nowrap;
  cursor: default;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  border: 1px solid transparent;
  background-color: var(--apple-bg-primary);
  color: var(--apple-text-primary);
}

.apple-tag.is-round {
  border-radius: var(--apple-border-radius-full);
}

.apple-tag.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.apple-tag.is-closable {
  padding-right: var(--apple-spacing-xs);
}

/* 类型样式 */
.apple-tag.is-primary {
  --tag-bg-color: rgba(0, 122, 255, 0.1);
  --tag-border-color: rgba(0, 122, 255, 0.2);
  --tag-text-color: var(--apple-blue);
}

.apple-tag.is-success {
  --tag-bg-color: rgba(52, 199, 89, 0.1);
  --tag-border-color: rgba(52, 199, 89, 0.2);
  --tag-text-color: var(--apple-green);
}

.apple-tag.is-warning {
  --tag-bg-color: rgba(255, 149, 0, 0.1);
  --tag-border-color: rgba(255, 149, 0, 0.2);
  --tag-text-color: var(--apple-orange);
}

.apple-tag.is-danger {
  --tag-bg-color: rgba(255, 59, 48, 0.1);
  --tag-border-color: rgba(255, 59, 48, 0.2);
  --tag-text-color: var(--apple-red);
}

.apple-tag.is-info {
  --tag-bg-color: rgba(90, 200, 250, 0.1);
  --tag-border-color: rgba(90, 200, 250, 0.2);
  --tag-text-color: var(--apple-cyan);
}

.apple-tag.is-default {
  --tag-bg-color: var(--apple-bg-secondary);
  --tag-border-color: var(--apple-border-secondary);
  --tag-text-color: var(--apple-text-secondary);
}

/* 效果样式 */
.apple-tag.is-light {
  background-color: var(--tag-bg-color, var(--apple-bg-secondary));
  border-color: var(--tag-border-color, var(--apple-border-secondary));
  color: var(--tag-text-color, var(--apple-text-secondary));
}

.apple-tag.is-dark {
  background-color: var(--tag-text-color, var(--apple-text-primary));
  border-color: var(--tag-text-color, var(--apple-text-primary));
  color: white;
}

.apple-tag.is-plain {
  background-color: transparent;
  border-color: var(--tag-text-color, var(--apple-text-secondary));
  color: var(--tag-text-color, var(--apple-text-secondary));
}

/* 自定义颜色 */
.apple-tag.has-color {
  --tag-bg-color: v-bind('props.color + "20"');
  --tag-border-color: v-bind('props.color + "40"');
  --tag-text-color: v-bind('props.color');
}

.apple-tag.has-color.is-dark {
  background-color: v-bind('props.color');
  border-color: v-bind('props.color');
  color: white;
}

.apple-tag.has-color.is-plain {
  background-color: transparent;
  border-color: v-bind('props.color');
  color: v-bind('props.color');
}

/* 尺寸样式 */
.apple-tag.is-small {
  --tag-height: 20px;
  --tag-padding: 0 var(--apple-spacing-xs);
  font-size: var(--apple-font-xs);
}

.apple-tag.is-medium {
  --tag-height: 24px;
  --tag-padding: 0 var(--apple-spacing-sm);
  font-size: var(--apple-font-sm);
}

.apple-tag.is-large {
  --tag-height: 28px;
  --tag-padding: 0 var(--apple-spacing-md);
  font-size: var(--apple-font-md);
}

/* 击中状态 */
.apple-tag.is-hit::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--tag-text-color, var(--apple-text-primary));
  border-radius: 1px;
}

/* 交互样式 */
.apple-tag:not(.is-disabled) {
  cursor: pointer;
}

.apple-tag:not(.is-disabled):hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

/* 内部元素 */
.tag-icon {
  margin-right: var(--apple-spacing-xs);
  font-size: 0.8em;
}

.tag-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: var(--apple-spacing-xs);
  border: none;
  background: none;
  color: var(--tag-text-color, var(--apple-text-secondary));
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 12px;
}

.tag-close:hover {
  background-color: rgba(0, 0, 0, 0.1);
  color: var(--apple-text-primary);
}

.apple-tag.is-dark .tag-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 动画效果 */
.apple-tag {
  position: relative;
  overflow: hidden;
}

.apple-tag::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.apple-tag:active::before {
  width: 100px;
  height: 100px;
}

/* 可点击标签的特殊样式 */
.apple-tag:not(.is-disabled) {
  cursor: pointer;
}

.apple-tag:not(.is-disabled):focus-visible {
  outline: 2px solid var(--apple-blue);
  outline-offset: 2px;
}

/* 组合样式 */
.apple-tag + .apple-tag {
  margin-left: var(--apple-spacing-xs);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-tag {
    --tag-height: 28px;
    --tag-padding: 0 var(--apple-spacing-sm);
    font-size: var(--apple-font-sm);
  }

  .apple-tag.is-small {
    --tag-height: 24px;
    --tag-padding: 0 var(--apple-spacing-xs);
    font-size: var(--apple-font-xs);
  }

  .apple-tag.is-large {
    --tag-height: 32px;
    --tag-padding: 0 var(--apple-spacing-md);
    font-size: var(--apple-font-md);
  }
}
</style>