<template>
  <CampusFrame>
    <template #nav><RouterLink v-if="post" :to="`/campus/schools/${post.schoolId}`">{{ post.schoolName }}</RouterLink></template>
    <p v-if="busy.post" role="status">正在读取动态…</p>
    <div v-if="errors.post" role="alert" class="error">{{ errors.post }} <button @click="loadPost">重试动态</button></div>
    <article v-if="post" class="campus-card">
      <p class="muted">{{ categories[post.category] }} · {{ statuses[post.status] }}<span v-if="post.pinned"> · 已置顶</span></p>
      <h2>{{ post.title }}</h2><p class="muted">{{ post.author.nickname || post.author.username }} · {{ dateText(post.createdAt) }}</p>
      <p class="plain-text">{{ post.content }}</p><CampusImage v-for="(media, index) in post.images" :key="media.id" :media="media" :alt="`${post.title}，第 ${index + 1} 张图片`" @access-denied="denyAccess" />
      <p v-if="post.reviewReason">审核说明：{{ post.reviewReason }}</p>
      <div class="campus-actions">
        <template v-if="published"><button :disabled="locked" :aria-pressed="post.likedByMe" @click="like">{{ post.likedByMe ? '取消点赞' : '点赞' }} · {{ post.likeCount }}</button><button :disabled="locked" :aria-pressed="post.bookmarkedByMe" @click="bookmark">{{ post.bookmarkedByMe ? '取消收藏' : '收藏' }}</button><button :disabled="locked" @click="reportOpen = true">举报</button></template>
        <RouterLink v-if="post.canEdit" :to="`/campus/posts/${post.id}/edit`">编辑动态</RouterLink><button v-if="post.canDelete" :disabled="locked" @click="deleting = { type: 'post' }">删除动态</button><RouterLink v-if="post.canModerate" :to="`/campus/schools/${post.schoolId}/admin`">校园管理</RouterLink>
      </div>
    </article>
    <p v-if="notice" role="status">{{ notice }}</p><p v-if="errors.action" role="alert">{{ errors.action }}</p>
    <section v-if="published" class="campus-card" aria-labelledby="comments-title">
      <h3 id="comments-title">评论 · {{ post.commentCount }}</h3>
      <form @submit.prevent="sendComment"><p v-if="reply">回复 {{ reply.author.nickname || reply.author.username }}（#{{ reply.id }}）<button type="button" @click="reply = null">取消回复</button></p><label>评论内容<textarea v-model="commentText" required maxlength="1000" /></label><button class="primary" :disabled="locked || !commentText.trim()">发表评论</button></form>
      <p v-if="busy.comments" role="status">正在读取评论…</p><p v-if="errors.comments" role="alert">{{ errors.comments }} <button @click="loadComments(comments.page)">重试评论</button></p>
      <p v-if="!busy.comments && !comments.list.length">暂无评论，来分享你的看法。</p>
      <article v-for="comment in comments.list" :key="comment.id" class="campus-card">
        <p class="muted">#{{ comment.id }} · {{ comment.author.nickname || comment.author.username }} · {{ dateText(comment.createdAt) }}<span v-if="comment.replyToCommentId"> · 回复 #{{ comment.replyToCommentId }}</span></p>
        <p class="plain-text">{{ comment.deleted ? '此评论已删除' : comment.content }}</p>
        <div v-if="!comment.deleted" class="campus-actions"><button :disabled="locked" @click="reply = comment">回复</button><button v-if="comment.canDelete" :disabled="locked" @click="deleting = { type: 'comment', id: comment.id }">删除评论</button></div>
      </article><CampusPager :page="comments" :busy="busy.comments" @change="loadComments" />
    </section>
    <AppleModal v-model="reportOpen" title="举报动态" :show-footer="false"><form class="campus" @submit.prevent="sendReport"><label>举报原因<textarea v-model="reportReason" required maxlength="500" /></label><p>管理员将核对内容并决定移除或驳回。请勿提交无关的个人资料。</p><p v-if="errors.action" role="alert">{{ errors.action }}</p><button class="primary" :disabled="locked || !reportReason.trim()">提交举报</button></form></AppleModal>
    <AppleModal :model-value="!!deleting" @update:model-value="value => { if (!value) deleting = null }" title="确认删除" :show-footer="false"><div class="campus"><p>确认删除此{{ deleting?.type === 'post' ? '动态及其图片访问权限' : '评论内容' }}？</p><p v-if="errors.action" role="alert">{{ errors.action }}</p><button :disabled="locked" @click="confirmDelete">确认删除</button></div></AppleModal>
  </CampusFrame>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/campus'
import CampusFrame from '@/components/campus/CampusFrame.vue'
import CampusImage from '@/components/campus/CampusImage.vue'
import CampusPager from '@/components/campus/CampusPager.vue'
import AppleModal from '@/components/common/AppleModal.vue'
import { useCampusTasks, emptyPage, categories, statuses, dateText } from './useCampusTasks'
const route = useRoute(), router = useRouter(), postId = route.params.postId, { busy, errors, run, cancelAll } = useCampusTasks({ onDenied: denyAccess })
const post = ref(null), comments = ref(emptyPage()), commentText = ref(''), reply = ref(null), reportOpen = ref(false), reportReason = ref(''), deleting = ref(null), notice = ref('')
function denyAccess() { cancelAll(); post.value = null; comments.value = emptyPage(); reply.value = null; deleting.value = null; reportOpen.value = false; notice.value = '访问权限已变化，已清除当前动态内容。' }
const published = computed(() => post.value?.status === 'PUBLISHED'), locked = computed(() => !!(busy.action || busy.post))
function loadPost() { run('post', signal => api.post(postId, signal), data => { post.value = data; if (data.status === 'PUBLISHED') loadComments(comments.value.page); else comments.value = emptyPage() }) }
function loadComments(page = 0) { run('comments', signal => api.comments(postId, { page, size: 20 }, signal), data => { comments.value = data }) }
function like() { run('action', signal => api.like(postId, !post.value.likedByMe, signal), data => Object.assign(post.value, data), true) }
function bookmark() { run('action', signal => api.bookmark(postId, !post.value.bookmarkedByMe, signal), data => Object.assign(post.value, data), true) }
function sendComment() { if (!commentText.value.trim()) return; run('action', signal => api.comment(postId, { content: commentText.value, replyToCommentId: reply.value?.id || null }, signal), () => { commentText.value = ''; reply.value = null; notice.value = '评论已发表'; loadPost() }, true) }
function sendReport() { if (!reportReason.value.trim()) return; run('action', signal => api.report(postId, reportReason.value, signal), () => { reportOpen.value = false; reportReason.value = ''; notice.value = '举报已提交，等待管理员处理' }, true) }
function confirmDelete() {
  const target = deleting.value
  if (!target) return
  run('action', signal => target.type === 'post' ? api.deletePost(postId, post.value.version, signal) : api.deleteComment(postId, target.id, signal), () => {
    deleting.value = null
    if (target.type === 'post') router.push(`/campus/schools/${post.value.schoolId}`)
    else { if (String(reply.value?.id) === String(target.id)) reply.value = null; notice.value = '评论已删除'; loadPost() }
  }, true)
}
onMounted(loadPost)
</script>
