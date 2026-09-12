import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import api from '@/api/modules/notification'
import { useAuthStore } from './authStore'
import { useNotificationStore } from './notificationStore'

vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }
const ok = data => ({ code: 0, data })
let store, count
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.setCurrentUser({ id: 1, username: 'Alice' })
  auth.applyToken({ token: 'account-a', expiresIn: 3600 })
  store = useNotificationStore()
  vi.spyOn(document, 'hidden', 'get').mockReturnValue(false)
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true)
  count = vi.spyOn(api, 'getUnreadCount').mockResolvedValue(ok({ unreadCount: 0 }))
})
afterEach(() => { store.stopAutoRefresh(); vi.useRealTimers(); vi.restoreAllMocks() })

describe('bounded notification payloads and reconciliation', () => {
  it('merges a socket arrival into an older page-one response and preserves already-read state', async () => {
    const pending = deferred()
    vi.spyOn(api, 'getNotifications').mockReturnValue(pending.promise)
    count.mockResolvedValue(ok({ unreadCount: 1 }))
    store.notifications = [{ id: 1, isRead: false }]
    const loading = store.fetchNotifications(1)
    store.addNotification({ id: 2, isRead: false, content: 'New arrival' })
    store.addNotification({ id: 1, isRead: true })
    pending.resolve(ok({ notifications: [{ id: 1, isRead: false }], currentPage: 1, pageSize: 10, totalCount: 1, totalPages: 1 }))
    await loading
    expect(store.notifications).toEqual([{ id: 2, isRead: false, content: 'New arrival' }, { id: 1, isRead: true }])
    expect(store.totalCount).toBe(2)
    expect(store.unreadCount).toBe(1)
    expect(store.isLoading).toBe(false)
  })

  it('keeps at most 100 payloads while older, newer and latest pages remain accessible', async () => {
    vi.spyOn(api, 'getNotifications').mockImplementation(async (page, size) => ok({
      notifications: Array.from({ length: size }, (_, i) => ({ id: (page - 1) * size + i + 1, isRead: false })),
      currentPage: page, pageSize: size, totalCount: 150, totalPages: 15
    }))
    for (let page = 1; page <= 13; page++) await store.fetchNotifications(page)
    expect(store.notifications).toHaveLength(100)
    expect(store.notifications[0].id).toBe(31)
    expect([store.firstPage, store.currentPage]).toEqual([4, 13])
    store.addNotification({ id: 999, isRead: false })
    store.addNotification({ id: 999, isRead: false })
    expect(store.unreadCount).toBe(1)
    expect(store.notifications[0].id).toBe(31)
    await store.loadNewerNotifications()
    expect(store.notifications[0].id).toBe(21)
    expect(store.notifications.at(-1).id).toBe(120)
    await store.loadLatestNotifications()
    expect(store.notifications.map(row => row.id)).toEqual([1,2,3,4,5,6,7,8,9,10])
  })

  it('rejects an old count and an old list after a read mutation, without double decrementing repeated reads', async () => {
    const list = deferred(), unread = deferred(), read1 = deferred(), read2 = deferred()
    vi.spyOn(api, 'getNotifications').mockReturnValue(list.promise)
    count.mockReturnValue(unread.promise)
    vi.spyOn(api, 'markAsRead').mockReturnValueOnce(read1.promise).mockReturnValueOnce(read2.promise)
    store.notifications = [{ id: 1, isRead: false }]
    store.unreadCount = 2
    const oldList = store.fetchNotifications(2), oldCount = store.fetchUnreadCount()
    const first = store.markAsRead(1), second = store.markAsRead(1)
    read1.resolve(ok(true)); await first
    read2.resolve(ok(true)); await second
    list.resolve(ok({ notifications: [{ id: 1, isRead: false }], currentPage: 2, totalPages: 2 }))
    unread.resolve(ok({ unreadCount: 2 }))
    await Promise.all([oldList, oldCount])
    expect(store.notifications).toEqual([{ id: 1, isRead: true }])
    expect(store.unreadCount).toBe(1)
    expect(store.currentPage).toBe(1)
    expect(store.isLoading).toBe(false)
  })

  it('can still reach every older row after new notifications shift server page offsets', async () => {
    const rows = Array.from({ length: 150 }, (_, i) => ({ id: i + 1, isRead: false }))
    vi.spyOn(api, 'getNotifications').mockImplementation(async (page, size) => ok({
      notifications: rows.slice((page - 1) * size, page * size), currentPage: page,
      pageSize: size, totalCount: rows.length, totalPages: Math.ceil(rows.length / size)
    }))
    const observed = new Set()
    for (let page = 1; page <= 13; page++) {
      await store.fetchNotifications(page)
      store.notifications.forEach(row => observed.add(row.id))
    }
    const originalWindow = store.notifications.map(row => row.id)
    for (let id = 1000; id < 1015; id++) { const row = { id, isRead: false }; rows.unshift(row); store.addNotification(row) }
    expect(store.notifications.map(row => row.id)).toEqual(originalWindow)
    while (store.currentPage < store.totalPages) {
      await store.fetchNotifications(store.currentPage + 1)
      store.notifications.forEach(row => observed.add(row.id))
      expect(store.notifications.length).toBeLessThanOrEqual(100)
    }
    expect([...observed].filter(id => id <= 150)).toHaveLength(150)
    await store.loadLatestNotifications()
    expect(store.notifications.map(row => row.id)).toEqual(rows.slice(0, 10).map(row => row.id))
  })

  it('polls a healthy subscription only every five minutes and coalesces realtime bursts', async () => {
    vi.useFakeTimers()
    store.setRealtimeConnected(true)
    store.startAutoRefresh()
    await vi.advanceTimersByTimeAsync(0)
    expect(count).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(299999)
    expect(count).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(1)
    expect(count).toHaveBeenCalledTimes(2)
    for (let id = 1; id <= 30; id++) store.addNotification({ id, isRead: false })
    expect(count).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(250)
    expect(count).toHaveBeenCalledTimes(3)
  })

  it('backs off disconnected failures to five minutes, pauses hidden/offline, resumes once and stops on reset', async () => {
    vi.useFakeTimers()
    count.mockResolvedValue({ code: 503 })
    store.startAutoRefresh()
    await vi.advanceTimersByTimeAsync(0)
    expect(count).toHaveBeenCalledTimes(1)
    for (const delay of [60000, 120000, 240000, 300000]) await vi.advanceTimersByTimeAsync(delay)
    expect(count).toHaveBeenCalledTimes(5)
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(true)
    store.refreshVisibility()
    await vi.advanceTimersByTimeAsync(600000)
    expect(count).toHaveBeenCalledTimes(5)
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(false)
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    store.refreshVisibility()
    await vi.advanceTimersByTimeAsync(600000)
    expect(count).toHaveBeenCalledTimes(5)
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true)
    count.mockResolvedValue(ok({ unreadCount: 0 }))
    store.refreshVisibility(); store.refreshVisibility()
    await vi.advanceTimersByTimeAsync(0)
    expect(count).toHaveBeenCalledTimes(6)
    await vi.advanceTimersByTimeAsync(30000)
    expect(count).toHaveBeenCalledTimes(7)
    store.resetState()
    await vi.advanceTimersByTimeAsync(600000)
    expect(count).toHaveBeenCalledTimes(7)
  })

  it('has one count request in flight and never overwrites older history during background reconciliation', async () => {
    vi.useFakeTimers()
    const pending = deferred()
    count.mockReturnValueOnce(pending.promise)
    store.firstPage = 4
    store.notifications = [{ id: 40 }]
    store.startAutoRefresh()
    await vi.advanceTimersByTimeAsync(0)
    store.addNotification({ id: 1000, isRead: false })
    await vi.advanceTimersByTimeAsync(250)
    expect(count).toHaveBeenCalledTimes(1)
    pending.resolve(ok({ unreadCount: 0 })); await flushPromises()
    expect(store.unreadCount).toBe(1)
    expect(store.notifications).toEqual([{ id: 40 }])
    await vi.advanceTimersByTimeAsync(250)
    expect(count).toHaveBeenCalledTimes(2)
  })
})
