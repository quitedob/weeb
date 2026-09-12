<template>
  <CampusFrame>
    <template #nav><RouterLink v-if="school" :to="`/campus/schools/${school.id}`">{{ school.name }}</RouterLink></template>
    <h2>{{ school ? `${school.name} · 校园管理` : '学校管理' }}</h2>
    <p v-if="denied" role="alert">管理权限已变化，已清除当前管理资料。<button @click="loadSchool">重新检查权限</button></p>
    <template v-if="!schoolId">
      <p v-if="!siteAdmin">只有站点管理员可以创建和管理学校。</p>
      <template v-else><CampusSchoolForm site-admin @saved="created" /><form class="campus-filters" @submit.prevent="loadDirectory(0)"><label>搜索学校<input v-model="query" maxlength="100" type="search" /></label><button>搜索学校</button></form><article v-for="item in directory.list" :key="item.id" class="campus-card"><h3><RouterLink :to="`/campus/schools/${item.id}/admin`">{{ item.name }}</RouterLink></h3><p>{{ item.active ? '启用中' : '已停用' }} · {{ item.memberCount }} 位成员</p></article><p v-if="!busy.directory && !directory.list.length">暂无学校，创建后即可安排校园管理。</p><CampusPager :page="directory" :busy="busy.directory" @change="loadDirectory" /></template>
    </template>
    <template v-else-if="school">
      <p v-if="!school.capabilities.canManageSchool">当前账号没有本校管理权限。</p>
      <template v-else>
        <nav class="campus-actions" aria-label="管理分类"><button v-for="item in tabs" :key="item.key" :aria-pressed="tab === item.key" @click="selectTab(item.key)">{{ item.label }}</button></nav>
        <CampusSchoolForm v-if="tab === 'settings'" :school="school" :site-admin="school.capabilities.isSiteAdmin" @saved="school = $event" />
        <template v-else>
          <form class="campus-filters" @submit.prevent="loadRows(0)">
            <label v-if="tab === 'members'">搜索成员<input v-model="memberQuery" maxlength="100" type="search" /></label>
            <label v-if="tab !== 'audit'">状态<select v-model="filterStatus" @change="loadRows(0)"><option v-for="value in statusOptions" :key="value" :value="value">{{ statuses[value] || '全部' }}</option></select></label><button>刷新列表</button>
          </form>
          <p v-if="busy.rows" role="status">正在读取管理列表…</p><p v-if="!busy.rows && !rows.list.length">当前列表没有记录。</p>
          <article v-for="item in rows.list" :key="item.id || item.userId" class="campus-card">
            <template v-if="tab === 'applications'">
              <h3>{{ item.username }} · {{ statuses[item.status] }}</h3><p class="muted">{{ dateText(item.createdAt) }}</p><p v-if="item.reason">处理说明：{{ item.reason }}</p><button @click="openAction('application', item)">{{ item.status === 'PENDING' ? '查看并审核申请' : '查看申请详情' }}</button>
            </template>
            <template v-else-if="tab === 'members'">
              <h3>{{ item.nickname || item.username }}</h3><p>{{ item.role === 'ADMIN' ? '校园管理员' : '成员' }} · {{ statuses[item.status] }} · {{ dateText(item.joinedAt) }}</p><button v-if="canManageMember(item)" @click="openAction('member', item)">管理成员</button>
            </template>
            <template v-else-if="tab === 'moderation'">
              <h3><RouterLink v-if="item.status !== 'REMOVED'" :to="`/campus/posts/${item.id}`">{{ item.title }}</RouterLink><span v-else>已移除的动态</span></h3><p>{{ statuses[item.status] }} · {{ item.author.nickname || item.author.username }} · 版本 {{ item.version }}</p><p v-if="item.reviewReason">处理说明：{{ item.reviewReason }}</p>
              <div class="campus-actions"><button v-if="item.status === 'PENDING' && !isSelf(item.author.id)" @click="openAction('post', item)">审核当前版本</button><button v-if="item.status === 'PUBLISHED'" :disabled="busy.action" @click="pin(item)">{{ item.pinned ? '取消置顶' : '置顶动态' }}</button><button v-if="item.canDelete" @click="openAction('delete', item)">删除动态</button></div>
            </template>
            <template v-else-if="tab === 'reports'">
              <h3>举报 #{{ item.id }} · {{ statuses[item.status] }}</h3><p class="plain-text">{{ item.reason }}</p><p v-if="item.decisionReason">处理说明：{{ item.decisionReason }}</p><RouterLink v-if="item.status === 'PENDING'" :to="`/campus/posts/${item.postId}`">查看被举报动态</RouterLink><button v-if="item.status === 'PENDING' && !isSelf(item.reporterId)" @click="openAction('report', item)">处理举报</button>
            </template>
            <template v-else><h3>{{ item.action }} · {{ item.targetType }} #{{ item.targetId }}</h3><p>{{ typeof item.details === 'string' ? item.details : JSON.stringify(item.details) }}</p><p class="muted">操作人 #{{ item.actorId }} · {{ dateText(item.createdAt) }}</p></template>
          </article><CampusPager :page="rows" :busy="busy.rows" @change="loadRows" />
        </template>
      </template>
    </template>
    <p v-if="busy.school || busy.directory" role="status">正在读取学校…</p><p v-if="notice" role="status">{{ notice }}</p>
    <div v-for="(error, key) in errors" v-show="error" :key="key" class="error" role="alert">{{ error }} <button v-if="['school','directory','rows'].includes(key)" @click="retry(key)">重试</button></div>
    <AppleModal :model-value="!!selected" @update:model-value="value => { if (!value) selected = null }" :title="modalTitle" :show-footer="false" :closable="!busy.action" :mask-closable="!busy.action">
      <form v-if="selected" class="campus" @submit.prevent="submitAction">
        <template v-if="selected.kind === 'application'"><p>申请隐私信息仅用于本次人工审核。</p><dl><dt>姓名 / 学号</dt><dd>{{ selected.item.realName }} / {{ selected.item.studentNumber }}</dd><dt>院系 / 入学年份</dt><dd>{{ selected.item.department }} / {{ selected.item.enrollmentYear }}</dd><dt>申请说明</dt><dd class="plain-text">{{ selected.item.statement }}</dd></dl><p v-if="isSelf(selected.item.userId)">不能审核自己的申请。</p></template>
        <template v-if="selected.kind === 'post'"><h3>{{ selected.item.title }}</h3><p class="plain-text">{{ selected.item.content }}</p><CampusImage v-for="media in selected.item.images" :key="media.id" :media="media" /><p>正在审核版本 {{ selected.item.version }}。保存后若作者已编辑，审核将被拒绝。</p></template>
        <template v-if="selected.kind === 'member'"><p>{{ selected.item.nickname || selected.item.username }}</p><label>成员状态<select v-model="decision.status"><option value="VERIFIED">有效认证</option><option value="SUSPENDED">暂停权限</option></select></label><label>校园角色<select v-model="decision.role" :disabled="!school.capabilities.isSiteAdmin"><option value="MEMBER">成员</option><option value="ADMIN">管理员</option></select></label><p>角色仅站点管理员可更改。最后一名有效管理员不能被撤销。</p></template>
        <template v-if="selected.kind === 'report'"><p>{{ selected.item.reason }}</p><p v-if="busy.reportPost" role="status">正在核对被举报动态…</p><p v-if="reportPost && isSelf(reportPost.author.id)">不能处理针对自己动态的举报。</p><p v-if="reportUnavailable">动态已不可读，可提交驳回结论；服务器会再次核对处理权限。</p><button v-if="errors.reportPost" type="button" @click="openAction('report', selected.item)">重新核对动态</button></template>
        <p v-if="selected.kind === 'delete'">确认删除此动态？成员将无法读取正文和图片。</p>
        <template v-if="canDecide">
          <label v-if="['application','post','report'].includes(selected.kind)">处理结果<select v-model="decision.decision"><template v-if="selected.kind === 'report'"><option value="DISMISS">驳回举报</option><option value="REMOVE" :disabled="reportUnavailable">移除动态</option></template><template v-else><option value="APPROVE">通过</option><option value="REJECT">拒绝</option></template></select></label>
          <label v-if="selected.kind !== 'delete'">处理原因<textarea v-model="decision.reason" maxlength="500" :required="decision.decision === 'REJECT' || ['member','report'].includes(selected.kind)" /></label><button class="primary" :disabled="busy.action || busy.reportPost">确认处理</button>
        </template><p v-if="errors.action || errors.reportPost" role="alert">{{ errors.action || errors.reportPost }}</p>
      </form>
    </AppleModal>
  </CampusFrame>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import api from '@/api/campus'
import CampusFrame from '@/components/campus/CampusFrame.vue'
import CampusPager from '@/components/campus/CampusPager.vue'
import CampusSchoolForm from '@/components/campus/CampusSchoolForm.vue'
import CampusImage from '@/components/campus/CampusImage.vue'
import AppleModal from '@/components/common/AppleModal.vue'
import { useCampusTasks, emptyPage, statuses, dateText } from './useCampusTasks'
const route = useRoute(), router = useRouter(), auth = useAuthStore(), schoolId = route.params.schoolId
const { busy, errors, run, cancelAll } = useCampusTasks({ onDenied: () => { school.value = null; directory.value = emptyPage(); rows.value = emptyPage(); selected.value = null; reportPost.value = null; denied.value = true } }), school = ref(null), directory = ref(emptyPage()), rows = ref(emptyPage()), tab = ref('settings'), query = ref(''), memberQuery = ref(''), filterStatus = ref('PENDING'), selected = ref(null), reportPost = ref(null), reportUnavailable = ref(false), notice = ref(''), denied = ref(false)
const decision = reactive({ decision: 'APPROVE', reason: '', status: 'VERIFIED', role: 'MEMBER' })
const siteAdmin = computed(() => auth.currentUser?.type === 'ADMIN')
const isSelf = id => String(id) === String(auth.currentUser?.id)
const tabs = computed(() => [{ key: 'settings', label: '学校设置' }, ...(school.value?.capabilities.canModerate ? [{ key: 'applications', label: '认证审核' }, { key: 'moderation', label: '内容审核' }, { key: 'reports', label: '举报处理' }, { key: 'audit', label: '操作审计' }] : []), ...(school.value?.capabilities.canManageMembers ? [{ key: 'members', label: '成员管理' }] : [])])
const statusOptions = computed(() => ({ applications: ['PENDING','APPROVED','REJECTED','CANCELLED'], moderation: ['PENDING','PUBLISHED','DRAFT','REJECTED','REMOVED'], reports: ['PENDING','REMOVED','DISMISSED'], members: ['VERIFIED','SUSPENDED','LEFT'] }[tab.value] || []))
const modalTitle = computed(() => ({ application: '认证申请详情', member: '管理校园成员', post: '审核校园动态', report: '处理举报', delete: '删除动态' }[selected.value?.kind] || '管理操作'))
const canDecide = computed(() => {
  const target = selected.value
  if (!target) return false
  if (target.kind === 'application') return target.item.status === 'PENDING' && !isSelf(target.item.userId)
  if (target.kind === 'report') return !isSelf(target.item.reporterId) && (reportPost.value ? !isSelf(reportPost.value.author.id) : reportUnavailable.value && decision.decision === 'DISMISS')
  return true
})
function canManageMember(member) { return school.value.capabilities.isSiteAdmin || (!isSelf(member.userId) && member.role !== 'ADMIN') }
function loadDirectory(page = 0) { run('directory', signal => api.schools({ q: query.value, page, size: 20 }, signal), data => { directory.value = data }) }
function loadSchool() { run('school', signal => api.school(schoolId, signal), data => { school.value = data; denied.value = false; if (!data.capabilities.canModerate) { cancelAll(); rows.value = emptyPage(); selected.value = null; reportPost.value = null } }) }
function created(value) { router.push(`/campus/schools/${value.id}/admin`) }
function selectTab(value) { tab.value = value; rows.value = emptyPage(); filterStatus.value = statusOptions.value[0] || ''; if (value !== 'settings') loadRows() }
function loadRows(page = 0) {
  const methods = { applications: api.applications, members: api.members, moderation: api.moderation, reports: api.reports, audit: api.audit }
  const method = methods[tab.value]
  if (!method) return
  run('rows', signal => method(schoolId, { page, size: 20, status: filterStatus.value || undefined, q: tab.value === 'members' ? memberQuery.value : undefined }, signal), data => { rows.value = data })
}
function openAction(kind, item) {
  selected.value = { kind, item }; errors.action = ''; errors.reportPost = ''; reportPost.value = null; reportUnavailable.value = false
  Object.assign(decision, { decision: kind === 'report' ? 'DISMISS' : 'APPROVE', reason: '', status: item.status === 'SUSPENDED' ? 'SUSPENDED' : 'VERIFIED', role: item.role || 'MEMBER' })
  if (kind === 'report') run('reportPost', async signal => {
    try { return { data: { post: (await api.post(item.postId, signal)).data, unavailable: false } } }
    catch (error) { if (error.response?.status === 404) return { data: { post: null, unavailable: true } }; throw error }
  }, data => {
    if (selected.value?.item.id === item.id && selected.value?.kind === kind) { reportPost.value = data.post; reportUnavailable.value = data.unavailable }
  })
}
function submitAction() {
  if (!canDecide.value) return
  const { kind, item } = selected.value, body = { decision: decision.decision, reason: decision.reason, version: item.version }
  const actions = {
    application: signal => api.reviewApplication(schoolId, item.id, body, signal),
    post: signal => api.reviewPost(item.id, body, signal),
    member: signal => api.updateMember(schoolId, item.userId, { status: decision.status, role: decision.role, reason: decision.reason, version: item.version }, signal),
    report: signal => api.resolveReport(schoolId, item.id, body, signal),
    delete: signal => api.deletePost(item.id, item.version, signal),
  }
  run('action', actions[kind], () => { selected.value = null; notice.value = '管理操作已完成'; loadRows(rows.value.page); loadSchool() }, true)
}
function pin(item) { run('action', signal => api.pin(item.id, { pinned: !item.pinned, version: item.version }, signal), () => { notice.value = '置顶状态已更新'; loadRows(rows.value.page) }, true) }
function retry(key) { if (key === 'school') loadSchool(); else if (key === 'directory') loadDirectory(directory.value.page); else loadRows(rows.value.page) }
onMounted(() => schoolId ? loadSchool() : siteAdmin.value && loadDirectory())
</script>
