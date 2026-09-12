<template>
  <section aria-label="动态图片">
    <p class="muted">最多 6 张 PNG、JPEG 或 GIF，每张不超过 5 MB，尺寸不超过 4096 × 4096 且不超过 1600 万像素。图片仅向有权访问该动态的成员展示。</p>
    <label>添加图片<input type="file" accept="image/png,image/jpeg,image/gif" :disabled="disabled || pending || modelValue.length >= 6" @change="selectFile" /></label>
    <p v-if="pending" role="status">正在处理图片…</p>
    <p v-if="validation || errors.upload || errors.remove" role="alert">{{ validation || errors.upload || errors.remove }}</p>
    <button v-if="retryFile" type="button" :disabled="disabled || pending" @click="upload(retryFile)">重试上传 {{ retryFile.name }}</button>
    <ol class="campus-grid">
      <li v-for="(media, index) in modelValue" :key="media.id">
        <CampusImage :media="media" :alt="`第 ${index + 1} 张动态图片`" @access-denied="denyAccess" />
        <div class="campus-actions">
          <button type="button" :disabled="disabled || pending || index === 0" :aria-label="`前移第 ${index + 1} 张图片`" @click="move(index, -1)">前移</button>
          <button type="button" :disabled="disabled || pending || index === modelValue.length - 1" :aria-label="`后移第 ${index + 1} 张图片`" @click="move(index, 1)">后移</button>
          <button type="button" :disabled="disabled || pending" :aria-label="`移除第 ${index + 1} 张图片`" @click="remove(media)">移除</button>
        </div>
      </li>
    </ol>
  </section>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import api from '@/api/campus'
import CampusImage from './CampusImage.vue'
import { useCampusTasks } from '@/views/campus/useCampusTasks'
const props = defineProps({ modelValue: { type: Array, required: true }, schoolId: [String, Number], attachedIds: { type: Array, default: () => [] }, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'pending', 'access-denied'])
const { busy, errors, run, cancelAll } = useCampusTasks({ onDenied: denyAccess })
const validation = ref(''), retryFile = ref(null), pending = computed(() => !!(busy.upload || busy.remove))
function denyAccess() { cancelAll(); retryFile.value = null; emit('access-denied') }
watch(pending, value => emit('pending', value))
function selectFile(event) { const file = event.target.files?.[0]; event.target.value = ''; if (file) upload(file) }
function upload(file) {
  validation.value = ''
  if (props.disabled || pending.value || props.modelValue.length >= 6) return
  if (!['image/png', 'image/jpeg', 'image/gif'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    validation.value = '请选择 5 MB 以内的 PNG、JPEG 或 GIF 图片'; return
  }
  retryFile.value = file
  run('upload', signal => api.upload(props.schoolId, file, signal), media => {
    emit('update:modelValue', [...props.modelValue, media]); retryFile.value = null
  }, true)
}
function move(index, step) { const images = [...props.modelValue]; [images[index], images[index + step]] = [images[index + step], images[index]]; emit('update:modelValue', images) }
function remove(media) {
  const unlink = () => emit('update:modelValue', props.modelValue.filter(image => image.id !== media.id))
  // Attached images are unlinked atomically with the versioned post save.
  if (props.attachedIds.includes(media.id)) unlink()
  else run('remove', signal => api.deleteMedia(media.id, signal), unlink, true)
}
</script>
