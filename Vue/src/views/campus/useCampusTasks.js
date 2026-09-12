import { reactive, onBeforeUnmount } from 'vue'
import { createRequestScope } from '@/utils/requestScope'
import { useRoute } from 'vue-router'

// Each resource has its own generation; mutations are single-flight. Campus routes
// are keyed by route and account epoch so private forms and blobs are disposed too.
export function useCampusTasks({ onDenied } = {}) {
  const route = useRoute()
  const busy = reactive({})
  const errors = reactive({})
  const scopes = new Map()
  let disposed = false
  async function run(key, request, commit, mutation = false) {
    if (disposed || (mutation && busy[key])) return false
    if (!scopes.has(key)) scopes.set(key, createRequestScope())
    const task = scopes.get(key).begin()
    const path = route?.fullPath
    const current = () => !disposed && task.isCurrent() && path === route?.fullPath
    busy[key] = true
    errors[key] = ''
    try {
      const response = await request(task.signal)
      if (!current()) return false
      commit?.(response.data)
      return true
    } catch (error) {
      if (current() && error.code !== 'ERR_CANCELED') {
        errors[key] = error.response?.status === 409
          ? `内容或状态已经更新，请先核对最新版本。当前输入已保留。${error.response?.data?.message || ''}`
          : error.message || '请求失败，请重试'
        if ([401, 403, 404].includes(error.response?.status) && onDenied) {
          cancelAll()
          onDenied(error, key)
        }
      }
      return false
    } finally {
      if (current()) busy[key] = false
    }
  }
  function cancelAll() { scopes.forEach(scope => scope.cancel()); Object.keys(busy).forEach(key => { busy[key] = false }) }
  onBeforeUnmount(() => { disposed = true; cancelAll() })
  return { busy, errors, run, cancelAll }
}

export const categories = { GENERAL: '校园日常', STUDY: '学习交流', LIFE: '校园生活', LOST_FOUND: '失物招领', ANNOUNCEMENT: '校园公告' }
export const statuses = { DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已拒绝', REMOVED: '已移除', APPROVED: '已通过', CANCELLED: '已撤回', VERIFIED: '已认证', LEFT: '已退出', SUSPENDED: '已暂停', DISMISSED: '已驳回' }
export const dateText = value => value ? new Date(value).toLocaleString('zh-CN') : '—'
export const emptyPage = () => ({ list: [], total: 0, page: 0, size: 20 })
