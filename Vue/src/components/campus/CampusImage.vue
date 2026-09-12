<template>
  <figure class="campus-image">
    <img v-if="url" :src="url" :alt="alt" />
    <div v-else role="status">{{ error || '正在读取图片…' }} <button v-if="error" type="button" @click="load">重试图片</button></div>
  </figure>
</template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import api from '@/api/campus'
import { useAuthStore } from '@/stores/authStore'
import { createRequestScope } from '@/utils/requestScope'
const props = defineProps({ media: { type: Object, required: true }, alt: { type: String, default: '校园动态图片' } })
const emit = defineEmits(['access-denied'])
const auth = useAuthStore(), url = ref(''), error = ref(''), scope = createRequestScope()
function clear() { if (url.value) URL.revokeObjectURL(url.value); url.value = '' }
async function load() {
  clear(); error.value = ''
  const task = scope.begin()
  if (!auth.accessToken) return
  try {
    const blob = await api.media(props.media.id, task.signal)
    if (!task.isCurrent()) return
    if (!(blob instanceof Blob) || blob.type !== 'image/png') throw new Error('图片响应无效')
    url.value = URL.createObjectURL(blob)
  } catch (failure) {
    if (task.isCurrent() && failure.code !== 'ERR_CANCELED') {
      error.value = '图片暂不可读，权限可能已变化'
      if ([401, 403].includes(failure.response?.status)) emit('access-denied')
    }
  }
}
watch(() => [props.media.id, auth.sessionEpoch], load, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { scope.cancel(); clear() })
</script>
<style scoped>
.campus-image { margin: 12px 0; min-height: 64px; }
img { max-width: 100%; max-height: 560px; object-fit: contain; border-radius: 10px; }
</style>
