<template>
  <nav class="campus-actions" aria-label="分页">
    <button :disabled="busy || page.page <= 0" @click="$emit('change', page.page - 1)">上一页</button>
    <span aria-live="polite">第 {{ page.page + 1 }} 页 · 共 {{ page.total }} 条</span>
    <button :disabled="busy || (page.page + 1) * page.size >= Math.min(page.total, 10000)" @click="$emit('change', page.page + 1)">下一页</button>
  </nav>
</template>
<script setup>
defineProps({ page: { type: Object, required: true }, busy: Boolean })
defineEmits(['change'])
</script>
