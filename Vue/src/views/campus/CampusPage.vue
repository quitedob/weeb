<template>
  <CampusFrame>
    <template #nav><RouterLink v-if="siteAdmin" to="/campus/admin">学校管理</RouterLink></template>
    <template v-if="!schoolId">
      <h2>找到你的校园</h2><p>完成本校成员认证，与同学分享学习、生活和校园见闻。</p>
      <form class="campus-filters" @submit.prevent="loadDirectory(0)"><label>搜索学校<input v-model="query" maxlength="100" type="search" /></label><label class="check"><input v-model="mine" type="checkbox" @change="loadDirectory(0)" />我的学校</label><button class="primary">搜索学校</button></form>
      <div class="campus-grid"><article v-for="item in directory.list" :key="item.id" class="campus-card"><h3><RouterLink :to="`/campus/schools/${item.id}`">{{ item.name }}</RouterLink></h3><p>{{ item.description }}</p><p class="muted">{{ item.memberCount }} 位成员 · {{ item.active ? '开放中' : '已停用' }} · {{ statuses[item.membership?.status] || '尚未认证' }}</p></article></div>
      <p v-if="!busy.directory && !directory.list.length">没有找到学校，请调整搜索条件。</p><CampusPager :page="directory" :busy="busy.directory" @change="loadDirectory" />
    </template>
    <CampusMembership v-else-if="schoolId && denied" :school="{ id: schoolId, active: false }" history-only />
    <template v-else-if="school">
      <h2>{{ school.name }}</h2><p class="plain-text">{{ school.description }}</p>
      <div class="campus-actions"><span>{{ school.memberCount }} 位成员</span><button :disabled="busy.school" @click="loadSchool">刷新校园状态</button><RouterLink v-if="school.capabilities.canPost" :to="`/campus/schools/${school.id}/new`">发布动态</RouterLink><RouterLink v-if="school.capabilities.canManageSchool" :to="`/campus/schools/${school.id}/admin`">管理校园</RouterLink></div>
      <p v-if="!school.active" role="status">学校已停用，校园内容暂不可读。</p>
      <CampusMembership :school="school" @changed="loadSchool" />
      <section v-if="school.capabilities.canRead" aria-label="校园动态">
        <form class="campus-filters" @submit.prevent="loadPosts(0)">
          <label>搜索动态<input v-model="filters.q" type="search" maxlength="100" /></label>
          <label>分类<select v-model="filters.category" @change="loadPosts(0)"><option value="">全部分类</option><option v-for="(label, value) in categories" :key="value" :value="value">{{ label }}</option></select></label>
          <label>排序<select v-model="filters.sort" @change="loadPosts(0)"><option value="latest">最新</option><option value="popular">热门</option></select></label>
          <label>范围<select v-model="filters.scope" @change="filters.status = ''; loadPosts(0)"><option value="feed">校园动态</option><option value="mine">我的发布</option><option value="bookmarks">我的收藏</option></select></label>
          <label v-if="filters.scope === 'mine'">状态<select v-model="filters.status" @change="loadPosts(0)"><option value="">全部状态</option><option v-for="value in ['DRAFT','PENDING','PUBLISHED','REJECTED','REMOVED']" :key="value" :value="value">{{ statuses[value] }}</option></select></label><button class="primary">搜索动态</button>
        </form>
        <template v-for="post in posts.list" :key="post.id"><CampusPostCard v-if="post.status !== 'REMOVED'" :post="post" /><p v-else class="campus-card">该动态已移除。{{ post.reviewReason }}</p></template>
        <p v-if="!busy.posts && !posts.list.length">暂无动态。可以调整筛选条件，或发布第一条校园动态。</p><CampusPager :page="posts" :busy="busy.posts" @change="loadPosts" />
      </section>
      <p v-else>校园动态仅向本校有效认证成员开放。</p>
    </template>
    <p v-if="Object.values(busy).some(Boolean)" role="status">正在读取校园信息…</p>
    <div v-for="(error, key) in errors" v-show="error" :key="key" class="error" role="alert">{{ error }} <button @click="retry(key)">重试</button></div>
  </CampusFrame>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import api from '@/api/campus'
import CampusFrame from '@/components/campus/CampusFrame.vue'
import CampusMembership from '@/components/campus/CampusMembership.vue'
import CampusPostCard from '@/components/campus/CampusPostCard.vue'
import CampusPager from '@/components/campus/CampusPager.vue'
import { useCampusTasks, emptyPage, statuses, categories } from './useCampusTasks'
const route = useRoute(), auth = useAuthStore(), schoolId = route.params.schoolId
const siteAdmin = computed(() => auth.currentUser?.type === 'ADMIN')
const { busy, errors, run, cancelAll } = useCampusTasks({ onDenied: () => { school.value = null; directory.value = emptyPage(); posts.value = emptyPage(); denied.value = true } }), school = ref(null), directory = ref(emptyPage()), posts = ref(emptyPage()), query = ref(''), mine = ref(false), denied = ref(false)
const filters = reactive({ q: '', category: '', sort: 'latest', scope: 'feed', status: '' })
function loadDirectory(page = 0) { run('directory', signal => api.schools({ q: query.value, mine: mine.value, page, size: 20 }, signal), data => { directory.value = data }) }
function loadSchool() { run('school', signal => api.school(schoolId, signal), data => { school.value = data; denied.value = false; if (data.capabilities.canRead) loadPosts(); else { cancelAll(); posts.value = emptyPage() } }) }
function loadPosts(page = 0) { run('posts', signal => api.posts(schoolId, { ...filters, category: filters.category || undefined, status: filters.status || undefined, page, size: 20 }, signal), data => { posts.value = data }) }
function retry(key) { if (key === 'school') loadSchool(); else if (key === 'posts') loadPosts(posts.value.page); else loadDirectory(directory.value.page) }
onMounted(() => schoolId ? loadSchool() : loadDirectory())
</script>
