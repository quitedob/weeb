<template>
  <section class="emoji-picker" aria-label="选择表情" @keydown.esc.stop="$emit('close')">
    <div class="picker-header">
      <input v-model="search" aria-label="搜索表情" placeholder="搜索表情" />
      <button type="button" aria-label="关闭表情选择器" @click="$emit('close')">×</button>
    </div>
    <div class="packages" aria-label="表情包">
      <button v-for="(pack, index) in packages" :key="pack.name" type="button"
        :aria-pressed="selectedPackage === index" @click="selectedPackage = index">{{ pack.name }}</button>
    </div>
    <div class="emoji-grid">
      <button v-for="(emoji, index) in visibleEmojis" :key="`${emoji.name}-${index}`"
        type="button" :aria-label="emoji.name" :title="emoji.name" @click="$emit('select', emoji.icon)">{{ emoji.icon }}</button>
    </div>
    <p v-if="!filtered.length">没有找到匹配的表情</p>
    <nav class="pagination" aria-label="表情分页">
      <button type="button" :disabled="page === 1" @click="page--">上一页</button>
      <span aria-live="polite">{{ page }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages" @click="page++">下一页</button>
    </nav>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import packages from '@/constant/emoji/emoji.js'

defineEmits(['select', 'close'])
const search = ref('')
const selectedPackage = ref(0)
const page = ref(1)
const pageSize = 30
const filtered = computed(() => (packages[selectedPackage.value]?.list || []).filter(emoji => {
  const query = search.value.trim().toLowerCase()
  return emoji.name.toLowerCase().includes(query) || emoji.icon.includes(query)
}))
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const visibleEmojis = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch([search, selectedPackage], () => { page.value = 1 }, { flush: 'sync' })
</script>

<style scoped>
.emoji-picker { position: absolute; bottom: 100%; left: 0; width: min(340px, 90vw); padding: 12px; border: 1px solid var(--apple-border-primary); border-radius: 12px; background: var(--apple-bg-primary); color: var(--apple-text-primary); box-shadow: var(--apple-shadow-medium); z-index: 10; }
.picker-header, .pagination, .packages { display: flex; align-items: center; gap: 8px; justify-content: space-between; margin-bottom: 8px; }
input { min-width: 0; width: 100%; padding: 8px; background: var(--apple-bg-secondary); color: inherit; border: 1px solid var(--apple-border-primary); border-radius: 6px; }
button { cursor: pointer; border: 1px solid var(--apple-border-secondary); border-radius: 6px; padding: 6px; background: var(--apple-bg-secondary); color: inherit; }
button:disabled { opacity: .5; cursor: default; }
button:focus-visible, input:focus-visible { outline: 2px solid var(--apple-blue); outline-offset: 2px; }
.emoji-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 4px; min-height: 44px; }
.emoji-grid button { font-size: 24px; }
.pagination { margin: 10px 0 0; }
</style>
