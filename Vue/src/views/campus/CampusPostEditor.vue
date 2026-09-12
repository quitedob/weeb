<template>
  <CampusFrame>
    <template #nav><RouterLink v-if="school" :to="`/campus/schools/${school.id}`">{{ school.name }}</RouterLink></template>
    <h2>{{ postId ? '编辑校园动态' : '发布校园动态' }}</h2>
    <p v-if="busy.load" role="status">正在读取编辑信息…</p>
    <p v-if="denied" role="alert">编辑权限已变化，已停止显示原动态。请重新检查校园权限。</p>
    <div v-if="errors.load" role="alert" class="error">{{ errors.load }} <button @click="load">重试</button></div>
    <p v-if="school && !allowed">当前账号没有编辑此动态的权限。</p>
    <form v-if="school && allowed" @submit.prevent="save($event.submitter?.value === 'publish')">
      <p>{{ school.preModeration ? '提交后等待另一位管理员审核。' : '提交后立即发布。' }}编辑已发布的动态会撤下旧版本，保存草稿或重新提交。</p>
      <fieldset :disabled="busy.save">
        <label>标题<input v-model="form.title" maxlength="120" required /></label>
        <label>分类<select v-model="form.category"><option v-for="(label, value) in allowedCategories" :key="value" :value="value">{{ label }}</option></select></label>
        <label>正文<textarea v-model="form.content" maxlength="10000" rows="12" required /></label><p class="muted">{{ form.content.length }} / 10000 · 支持纯文本，图片在下方单独添加。</p>
        <CampusImagePicker v-model="images" :school-id="school.id" :attached-ids="attachedIds" :disabled="busy.save" @pending="uploading = $event" @access-denied="denyAccess" />
        <div class="campus-actions"><button type="submit" value="draft" :disabled="uploading || busy.save">保存草稿</button><button class="primary" type="submit" value="publish" :disabled="uploading || busy.save">{{ busy.save ? '正在保存…' : '提交动态' }}</button></div>
      </fieldset>
      <p v-if="validation" role="alert">{{ validation }}</p>
      <div v-if="errors.save" role="alert" class="error">{{ errors.save }}<button v-if="postId" type="button" :disabled="busy.latest" @click="checkLatest">核对最新版本</button></div>
      <p v-if="errors.latest" role="alert">{{ errors.latest }}</p>
    </form>
    <AppleModal v-model="latestOpen" title="核对最新版本" :show-footer="false">
      <div class="campus" v-if="latest"><p>当前输入仍保留在编辑器中。以下为服务器最新版本 {{ latest.version }}（{{ statuses[latest.status] }}）。</p><h3>{{ latest.title }}</h3><p class="plain-text">{{ latest.content }}</p><div class="campus-actions"><button @click="latestOpen = false">保留当前输入</button><button @click="replaceWithLatest">放弃当前输入并载入最新版本</button></div></div>
    </AppleModal>
  </CampusFrame>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/campus'
import CampusFrame from '@/components/campus/CampusFrame.vue'
import CampusImagePicker from '@/components/campus/CampusImagePicker.vue'
import AppleModal from '@/components/common/AppleModal.vue'
import { useCampusTasks, categories, statuses } from './useCampusTasks'
const route = useRoute(), router = useRouter(), postId = route.params.postId
const { busy, errors, run, cancelAll } = useCampusTasks({ onDenied: denyAccess }), school = ref(null), original = ref(null), images = ref([]), attachedIds = ref([]), uploading = ref(false), validation = ref(''), latest = ref(null), latestOpen = ref(false), denied = ref(false)
const form = reactive({ title: '', content: '', category: 'GENERAL', version: undefined })
function denyAccess() {
  cancelAll(); school.value = null; original.value = null; images.value = []; attachedIds.value = []
  latest.value = null; latestOpen.value = false; uploading.value = false; denied.value = true
  Object.assign(form, { title: '', content: '', category: 'GENERAL', version: undefined })
}
const allowed = computed(() => school.value?.capabilities.canPost && (!postId || original.value?.canEdit))
const allowedCategories = computed(() => Object.fromEntries(Object.entries(categories).filter(([key]) => key !== 'ANNOUNCEMENT' || school.value?.capabilities.canModerate)))
function fill(post) { original.value = post; Object.assign(form, { title: post.title, content: post.content, category: post.category, version: post.version }); images.value = [...post.images]; attachedIds.value = post.images.map(image => image.id) }
function load() {
  run('load', async signal => {
    const post = postId ? (await api.post(postId, signal)).data : null
    const response = await api.school(post?.schoolId || route.params.schoolId, signal)
    return { data: { school: response.data, post } }
  }, data => { school.value = data.school; denied.value = false; if (data.post) fill(data.post) })
}
function save(submit) {
  if (!allowed.value || uploading.value || busy.save) return
  validation.value = ''
  if (!form.title.trim() || !form.content.trim()) { validation.value = '标题和正文不能为空'; return }
  const body = { ...form, mediaIds: images.value.map(image => image.id), submit }
  run('save', signal => postId ? api.updatePost(postId, body, signal) : api.createPost(school.value.id, body, signal), post => router.push(`/campus/posts/${post.id}`), true)
}
function checkLatest() { run('latest', signal => api.post(postId, signal), post => { latest.value = post; latestOpen.value = true }) }
function replaceWithLatest() { fill(latest.value); latestOpen.value = false; errors.save = '' }
onMounted(load)
</script>
