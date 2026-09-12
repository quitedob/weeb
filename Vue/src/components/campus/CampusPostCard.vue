<template>
  <article class="campus-card">
    <p class="muted"><span v-if="post.pinned" class="badge">置顶</span>{{ categories[post.category] }} · {{ statuses[post.status] }}</p>
    <h3><RouterLink :to="`/campus/posts/${post.id}`">{{ post.title }}</RouterLink></h3>
    <p class="plain-text">{{ post.content.slice(0, 240) }}{{ post.content.length > 240 ? '…' : '' }}</p>
    <p class="muted">{{ post.author.nickname || post.author.username }} · {{ dateText(post.createdAt) }} · {{ post.likeCount }} 赞 · {{ post.commentCount }} 评论<span v-if="post.images?.length"> · {{ post.images.length }} 张图片</span></p>
    <p v-if="post.reviewReason">审核说明：{{ post.reviewReason }}</p>
  </article>
</template>
<script setup>
import { categories, statuses, dateText } from '@/views/campus/useCampusTasks'
defineProps({ post: { type: Object, required: true } })
</script>
