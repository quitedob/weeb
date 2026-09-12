<template>
  <section class="campus-card" aria-labelledby="membership-heading">
    <h3 id="membership-heading">{{ historyOnly ? '我的校园申请' : '校园成员认证' }}</h3>
    <p v-if="historyOnly">校园当前不可访问，仍可查看本人的申请记录或撤回待审申请。</p>
    <template v-if="!historyOnly">
    <p>认证采用站内人工审核，并未连接学校官方学籍系统。请勿提交身份证或证件照片。</p>
    <p>成员状态：{{ statuses[school.membership?.status] || '尚未认证' }}<span v-if="school.membership?.role === 'ADMIN'"> · 校园管理员</span></p>
    <p v-if="school.latestApplication">最近申请：{{ statuses[school.latestApplication.status] }} {{ school.latestApplication.reason }}</p>
    <p v-if="school.membership?.status === 'SUSPENDED'">成员权限已暂停，请联系校园管理员恢复，不能重新申请。</p>
    <form v-if="canApply" @submit.prevent="apply">
      <fieldset :disabled="busy.apply">
        <div class="campus-grid">
          <label>姓名<input v-model="form.realName" required maxlength="80" autocomplete="name" /></label>
          <label>学号<input v-model="form.studentNumber" required maxlength="40" /></label>
          <label>院系<input v-model="form.department" required maxlength="100" /></label>
          <label>入学年份<input v-model.number="form.enrollmentYear" type="number" min="1900" :max="new Date().getFullYear() + 1" required /></label>
        </div>
        <label>申请说明<textarea v-model="form.statement" maxlength="1000" /></label>
        <button class="primary" type="submit">{{ busy.apply ? '正在提交…' : '提交认证申请' }}</button>
      </fieldset>
    </form>
    <button v-if="school.membership?.status === 'VERIFIED'" :disabled="busy.leave" @click="leaveOpen = true">退出本校</button>
    </template>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-for="(error, key) in errors" v-show="error" :key="key" role="alert">{{ error }}</p>
    <details :open="historyOnly" @toggle="openHistory">
      <summary>我的申请历史（仅本人和管理员可见）</summary>
      <p v-if="busy.history" role="status">正在读取申请…</p>
      <button v-if="errors.history" @click="loadHistory(history.page)">重试申请历史</button>
      <p v-if="!busy.history && !history.list.length">暂无申请记录。</p>
      <article v-for="item in history.list" :key="item.id" class="campus-card">
        <p>{{ statuses[item.status] }} · {{ dateText(item.createdAt) }}</p>
        <p>{{ item.realName }} · {{ item.studentNumber }} · {{ item.department }} · {{ item.enrollmentYear }}</p>
        <p class="plain-text">{{ item.statement }}</p><p v-if="item.reason">处理说明：{{ item.reason }}</p>
        <button v-if="item.status === 'PENDING'" :disabled="busy.cancel" @click="cancel(item)">撤回申请</button>
      </article>
      <CampusPager :page="history" :busy="busy.history" @change="loadHistory" />
    </details>
    <AppleModal v-model="leaveOpen" title="退出本校" :show-footer="false">
      <div class="campus"><p>退出后无法读取本校动态和图片。最后一名有效校园管理员必须先安排另一名管理员。</p><p v-if="errors.leave" role="alert">{{ errors.leave }}</p><button :disabled="busy.leave" @click="leave">确认退出</button></div>
    </AppleModal>
  </section>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '@/api/campus'
import AppleModal from '@/components/common/AppleModal.vue'
import CampusPager from './CampusPager.vue'
import { useCampusTasks, emptyPage, statuses, dateText } from '@/views/campus/useCampusTasks'
const props = defineProps({ school: { type: Object, required: true }, historyOnly: Boolean }), emit = defineEmits(['changed'])
const { busy, errors, run } = useCampusTasks({ onDenied: () => { history.value = emptyPage(); leaveOpen.value = false; emit('changed') } }), history = ref(emptyPage()), notice = ref(''), leaveOpen = ref(false)
const form = reactive({ realName: '', studentNumber: '', department: '', enrollmentYear: new Date().getFullYear(), statement: '' })
const canApply = computed(() => props.school.active && !['VERIFIED', 'SUSPENDED'].includes(props.school.membership?.status) && props.school.latestApplication?.status !== 'PENDING')
function loadHistory(page = 0) { run('history', signal => api.applications(props.school.id, { page, size: 20 }, signal, true), data => { history.value = data }) }
function openHistory(event) { if (event.target.open) loadHistory() }
function apply() { run('apply', signal => api.apply(props.school.id, { ...form }, signal), () => { notice.value = '申请已提交，等待管理员审核'; emit('changed'); loadHistory() }, true) }
function cancel(item) { run('cancel', signal => api.cancelApplication(props.school.id, item.id, signal), () => { notice.value = '申请已撤回'; emit('changed'); loadHistory() }, true) }
function leave() { run('leave', signal => api.leave(props.school.id, signal), () => { leaveOpen.value = false; notice.value = '已退出本校'; emit('changed') }, true) }
onMounted(() => { if (props.historyOnly) loadHistory() })
</script>
