<template>
  <form class="campus-card" @submit.prevent="save">
    <h3>{{ school ? '学校设置' : '创建学校' }}</h3>
    <fieldset :disabled="busy.save">
      <label>学校名称<input v-model="form.name" :readonly="!!school && !siteAdmin" required maxlength="120" /></label>
      <label>学校简介<textarea v-model="form.description" maxlength="2000" /></label>
      <label class="check"><input v-model="form.preModeration" type="checkbox" />发布动态前需要管理员审核</label>
      <label v-if="school" class="check"><input v-model="form.active" type="checkbox" :disabled="!siteAdmin" />启用学校</label>
      <p v-if="school" class="muted">停用后成员无法读取校园动态或图片。学校管理员只能修改简介和预审设置。</p>
      <p v-else class="muted">创建者成为该校首位管理员，可在成员管理中安排其他管理员。</p>
      <button class="primary">{{ busy.save ? '正在保存…' : school ? '保存学校设置' : '创建学校' }}</button>
    </fieldset>
    <p v-if="errors.save" role="alert">{{ errors.save }}<button v-if="school" type="button" :disabled="busy.latest" @click="checkLatest">核对最新设置</button></p><p v-if="errors.latest" role="alert">{{ errors.latest }}</p><p v-if="notice" role="status">{{ notice }}</p>
    <AppleModal v-model="latestOpen" title="核对学校设置" :show-footer="false"><div v-if="latest" class="campus"><h3>{{ latest.name }}</h3><p class="plain-text">{{ latest.description }}</p><p>版本 {{ latest.version }} · {{ latest.active ? '启用中' : '已停用' }} · {{ latest.preModeration ? '发布前审核' : '直接发布' }}</p><div class="campus-actions"><button type="button" @click="latestOpen = false">保留当前输入</button><button type="button" @click="replaceLatest">放弃当前输入并载入最新设置</button></div></div></AppleModal>
  </form>
</template>
<script setup>
import { reactive, ref, watch } from 'vue'
import api from '@/api/campus'
import AppleModal from '@/components/common/AppleModal.vue'
import { useCampusTasks } from '@/views/campus/useCampusTasks'
const props = defineProps({ school: Object, siteAdmin: Boolean }), emit = defineEmits(['saved'])
const { busy, errors, run } = useCampusTasks(), notice = ref(''), latest = ref(null), latestOpen = ref(false)
const form = reactive({ name: '', description: '', preModeration: true, active: true, version: undefined })
function fill(value) { Object.assign(form, { name: value.name, description: value.description, preModeration: value.preModeration, active: value.active, version: value.version }) }
watch(() => props.school, value => { if (value) fill(value) }, { immediate: true })
function checkLatest() { run('latest', signal => api.school(props.school.id, signal), value => { latest.value = value; latestOpen.value = true }) }
function replaceLatest() { fill(latest.value); latestOpen.value = false; errors.save = '' }
function save() {
  if (!form.name.trim() || (!props.school && !props.siteAdmin)) return
  const data = props.school ? { ...form } : { name: form.name, description: form.description, preModeration: form.preModeration }
  run('save', signal => props.school ? api.updateSchool(props.school.id, data, signal) : api.createSchool(data, signal), school => { notice.value = '学校设置已保存'; emit('saved', school) }, true)
}
</script>
