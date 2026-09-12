import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus'
import instance from '@/api/axiosInstance'
import { useNotificationStore } from '@/stores/notificationStore'
import NotificationListPage from './NotificationListPage.vue'
import NotificationBell from '@/layout/components/NotificationBell.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ accessToken: null }) }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const originalAdapter = instance.defaults.adapter
let wrapper, pinia, rows, requests
const makeRows = count => Array.from({ length: count }, (_, index) => ({
  id: index + 1, type: 'follow', actorId: index + 100, entityType: 'user', entityId: index + 100,
  isRead: false, createdAt: '2026-09-12T09:00:00'
}))

beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
  pinia = createPinia()
  requests = []
  rows = makeRows(23)
  instance.defaults.adapter = async config => {
    requests.push({ method: config.method, url: config.url, params: config.params })
    let data
    if (config.url === '/api/notifications') {
      const { page, size } = config.params
      data = {
        notifications: rows.slice((page - 1) * size, page * size).map(row => ({ ...row })),
        totalCount: rows.length, currentPage: page, pageSize: size, totalPages: Math.ceil(rows.length / size)
      }
    } else if (config.url === '/api/notifications/unread-count') {
      data = { unreadCount: rows.filter(row => !row.isRead).length }
    } else if (config.method === 'delete' && config.url === '/api/notifications/read') {
      data = { deletedCount: rows.filter(row => row.isRead).length }
      rows = rows.filter(row => !row.isRead)
    } else if (config.method === 'post' && /\/\d+\/read$/.test(config.url)) {
      const id = config.url.split('/').at(-2)
      rows.find(row => String(row.id) === id).isRead = true
      data = 'Notification read'
    } else throw new Error(`Unexpected request: ${config.method} ${config.url}`)
    return { status: 200, headers: {}, config, data: { code: 0, message: 'OK', data } }
  }
})

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ''
  instance.defaults.adapter = originalAdapter
  vi.restoreAllMocks()
})

async function render(component = NotificationListPage) {
  wrapper = mount(component, { attachTo: document.body, global: { plugins: [pinia, ElementPlus], stubs: { 'router-link': true } } })
  if (component === NotificationBell) await wrapper.get('.bell-button').trigger('click')
  await flushPromises()
}

describe('notification journeys using the backend response contract', () => {
  it('opens an independent latest-ten bell preview without replacing the viewed older notification window', async () => {
    rows = makeRows(150)
    await render(NotificationBell)
    await wrapper.get('.bell-button').trigger('click')
    const store = useNotificationStore(pinia)
    for (let page = 1; page <= 13; page++) await store.fetchNotifications(page)
    expect(store.firstPage).toBe(4)
    expect(store.notifications).toHaveLength(100)
    await wrapper.get('.bell-button').trigger('click')
    await flushPromises()
    expect([store.firstPage, store.currentPage]).toEqual([4, 13])
    expect(store.notifications.map(row => row.id)).toEqual(Array.from({ length: 100 }, (_, i) => i + 31))
    expect(store.previewNotifications.map(row => row.id)).toEqual(Array.from({ length: 10 }, (_, i) => i + 1))
    expect(wrapper.findAll('.notification-item')).toHaveLength(10)
  })

  it('provides visible newer and latest navigation after moving past the notification payload limit', async () => {
    rows = makeRows(150)
    await render()
    for (let page = 2; page <= 13; page++) {
      await wrapper.findAll('button').find(button => button.text() === '加载更多').trigger('click')
      await flushPromises()
    }
    expect(wrapper.findAll('.notification-item')).toHaveLength(100)
    await wrapper.findAll('button').find(button => button.text() === '较新通知').trigger('click')
    await flushPromises()
    expect(useNotificationStore(pinia).notifications[0].id).toBe(21)
    await wrapper.findAll('button').find(button => button.text() === '返回最新').trigger('click')
    await flushPromises()
    expect(useNotificationStore(pinia).notifications[0].id).toBe(1)
    expect(wrapper.findAll('.notification-item')).toHaveLength(10)
  })

  it('loads all pages using totalCount/totalPages metadata without losing later notifications', async () => {
    await render()
    expect(wrapper.findAll('.notification-item')).toHaveLength(10)
    await wrapper.get('.load-more button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.notification-item')).toHaveLength(20)
    await wrapper.get('.load-more button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.notification-item')).toHaveLength(23)
    expect(wrapper.find('.load-more').exists()).toBe(false)
    expect(useNotificationStore(pinia).notifications.map(row => row.id)).toEqual(rows.map(row => row.id))
    const historyRequests = requests.filter(request => request.url === '/api/notifications').map(request => request.params)
    expect(historyRequests).toContainEqual({ page: 3, size: 10 })
    expect(historyRequests.every(request => request.size === 10)).toBe(true)
  })

  it('reloads server pagination after clearing read items, including unread items outside the cached page', async () => {
    rows.slice(0, 11).forEach(row => { row.isRead = true })
    await render()
    await wrapper.findAll('.header-actions button').find(button => button.text() === '清空已读').trigger('click')
    await flushPromises()
    expect(requests.some(request => request.method === 'delete' && request.url === '/api/notifications/read')).toBe(true)
    expect(useNotificationStore(pinia).currentPage).toBe(1)
    expect(wrapper.findAll('.notification-item')).toHaveLength(10)
    expect(useNotificationStore(pinia).unreadCount).toBe(12)
    await wrapper.get('.load-more button').trigger('click')
    await flushPromises()
    expect(useNotificationStore(pinia).notifications.map(row => row.id)).toEqual(rows.map(row => row.id))
    expect(wrapper.findAll('.notification-item')).toHaveLength(12)
    expect(wrapper.find('.load-more').exists()).toBe(false)
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it.each([
    { label: 'list', component: NotificationListPage },
    { label: 'bell', component: NotificationBell }
  ])('shows and opens an emitted follow notification from the $label after marking it read', async ({ component }) => {
    rows = [{ ...makeRows(1)[0], actorId: 73 }]
    await render(component)
    expect(wrapper.get('.notification-item').text()).toContain('有人关注了你')
    await wrapper.get('.notification-item').trigger('click')
    await flushPromises()
    expect(requests.some(request => request.method === 'post' && request.url === '/api/notifications/1/read')).toBe(true)
    expect(push).toHaveBeenCalledWith('/user/73')
    const store = useNotificationStore(pinia)
    expect((component === NotificationBell ? store.previewNotifications : store.notifications)[0].isRead).toBe(true)
    expect(useNotificationStore(pinia).unreadCount).toBe(0)
  })

  it.each(['ARTICLE_LIKE', 'ARTICLE_COMMENT', 'COMMENT'])('keeps the %s alias and opens the routed article page', async type => {
    rows = [{ ...makeRows(1)[0], type, entityId: 48, entityType: 'article' }]
    await render()
    await wrapper.get('.notification-item').trigger('click')
    await flushPromises()
    expect(push).toHaveBeenCalledWith('/article/read/48')
  })
})
