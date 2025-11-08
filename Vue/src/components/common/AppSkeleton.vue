<template>
  <div class="app-skeleton">
    <el-skeleton
      :rows="rows"
      :animated="animated"
      :loading="loading"
      :throttle="throttle"
    >
      <template #template v-if="customTemplate">
        <slot name="template"></slot>
      </template>
      <template #default>
        <slot></slot>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
const props = defineProps({
  rows: {
    type: Number,
    default: 3
  },
  animated: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: true
  },
  throttle: {
    type: Number,
    default: 0
  },
  customTemplate: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped>
.app-skeleton {
  padding: 20px;
}

.app-skeleton :deep(.el-skeleton__item) {
  background: var(--el-fill-color-lighter, #f5f7fa);
}

.app-skeleton :deep(.el-skeleton.is-animated .el-skeleton__item) {
  background: linear-gradient(
    90deg,
    var(--el-fill-color-lighter, #f5f7fa) 25%,
    var(--el-fill-color-light, #e9ecef) 37%,
    var(--el-fill-color-lighter, #f5f7fa) 63%
  );
  background-size: 400% 100%;
  animation: el-skeleton-loading 1.4s ease infinite;
}

@keyframes el-skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
