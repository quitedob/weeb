<template>
  <div class="app-empty">
    <el-empty
      :description="description"
      :image="image"
      :image-size="imageSize"
    >
      <template #image v-if="customImage">
        <slot name="image"></slot>
      </template>
      <template #description v-if="customDescription">
        <slot name="description"></slot>
      </template>
      <template #default v-if="$slots.default || $slots.extra">
        <slot name="extra">
          <slot></slot>
        </slot>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  description: {
    type: String,
    default: '暂无数据'
  },
  image: {
    type: String,
    default: ''
  },
  imageSize: {
    type: Number,
    default: 200
  },
  customImage: {
    type: Boolean,
    default: false
  },
  customDescription: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped>
.app-empty {
  padding: 40px 20px;
  text-align: center;
}

.app-empty :deep(.el-empty__description) {
  color: var(--el-text-color-secondary, #909399);
  font-size: 14px;
  margin-top: 16px;
}

.app-empty :deep(.el-empty__image) {
  margin: 0 auto;
}

.app-empty :deep(.el-empty__bottom) {
  margin-top: 20px;
}
</style>
