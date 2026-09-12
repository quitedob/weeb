import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { toRaw } from 'vue'
import { flushPromises } from '@vue/test-utils'
import axios from 'axios'
import instance from '@/api/axiosInstance'
import appleMessage from '@/utils/appleMessage'
import { useAuthStore } from './authStore'
import { useChatStore } from './chatStore'
import { useNotificationStore } from './notificationStore'

const { clients } = vi.hoisted(() => ({ clients: [] }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))
vi.mock('@/utils/bugReporter', () => ({ default: { reportBug: vi.fn() } }))
vi.mock('sockjs-client', () => ({ default: class {} }))
vi.mock('@stomp/stompjs', () => ({ Client: class {
  constructor(options) {
    Object.assign(this, options)
    this.connected = false
    this.active = false
    this.subscribe = vi.fn()
    this.publish = vi.fn()
    clients.push(this)
  }
  activate() { this.active = true }
  deactivate() { this.active = false; this.connected = false }
} }))

const originalAdapter = instance.defaults.adapter
const ok = data => ({ code: 0, message: 'OK', data })
let auth, chat, notifications, requests

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('navigator', { locks: { request: (_name, callback) => callback() } })
  localStorage.clear()
  setActivePinia(createPinia())
  clients.length = 0
  auth = useAuthStore()
  chat = useChatStore()
  notifications = useNotificationStore()
  requests = []
  instance.defaults.adapter = config => new Promise((resolve, reject) => {
    requests.push({ config,
      reply: data => resolve({ status: 200, headers: {}, config, data }),
      fail: status => reject(new axios.AxiosError('Failed', 'ERR_BAD_RESPONSE', config, {}, {
        status, config, data: { message: 'Rejected' }
      }))
    })
  })
  switchAccount('a')
})

afterEach(() => {
  chat.disconnectWebSocket()
  notifications.stopAutoRefresh()
  if (auth.refreshTimer) clearInterval(auth.refreshTimer)
  instance.defaults.adapter = originalAdapter
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

function switchAccount(account) {
  auth.logoutCleanup()
  // These resets are the synchronous application logout watcher.
  chat.disconnectWebSocket()
  chat.$reset()
  notifications.resetState()
  auth.setCurrentUser({ id: account, username: account })
  auth.applyToken({ token: `token-${account}`, expiresIn: 3600 })
}

describe('account-bound asynchronous work through the real Axios adapter', () => {
  it('rejects an old profile success when another tab has already persisted a different account', async () => {
    const pending = auth.fetchUserInfo()
    localStorage.setItem('currentUser', JSON.stringify({ id: 'b', username: 'b' }))
    localStorage.setItem('jwt_token', 'token-b')
    localStorage.setItem('token_expiry', String(Date.now() + 3600000))
    requests[0].reply(ok({ user: { id: 'a', username: 'a' } }))
    expect(await pending).toBeNull()
    expect(auth.currentUser.id).toBe('b')
    expect(JSON.parse(localStorage.getItem('currentUser')).id).toBe('b')
  })
  it('adopts a winning cross-tab renewal before an old-token 401 can clear shared storage', async () => {
    const pending = auth.refreshAccessToken()
    localStorage.setItem('jwt_token', 'other-tab-renewed-a')
    localStorage.setItem('token_expiry', String(Date.now() + 3600000))
    requests[0].fail(401)
    expect(await pending).toBeNull()
    expect(auth.accessToken).toBe('other-tab-renewed-a')
    expect(localStorage.getItem('jwt_token')).toBe('other-tab-renewed-a')
    expect(appleMessage.error).not.toHaveBeenCalled()
  })

  it('retains a still-valid credential when renewal has a temporary service failure', async () => {
    const pending = auth.refreshAccessToken().catch(error => error)
    requests[0].fail(503)
    await pending
    expect(auth.accessToken).toBe('token-a')
    expect(localStorage.getItem('jwt_token')).toBe('token-a')
    expect(auth.isRefreshing).toBe(false)
  })
  it.each(['success', '401', '1002'])('rejects a late account A %s without clearing or notifying account B', async outcome => {
    const pending = instance.get('/api/users/me').catch(error => error)
    expect(requests[0].config.headers.Authorization).toBe('Bearer token-a')
    switchAccount('b')
    if (outcome === '401') requests[0].fail(401)
    else requests[0].reply(outcome === '1002' ? { code: 1002, message: 'Expired' } : ok({ id: 'a' }))
    expect(axios.isCancel(await pending)).toBe(true)
    expect(auth.currentUser.id).toBe('b')
    expect(localStorage.getItem('jwt_token')).toBe('token-b')
    expect(appleMessage.error).not.toHaveBeenCalled()
  })

  it.each(['401', '1002'])('ignores an old credential %s after a successful renewal in the same session', async outcome => {
    const oldRequest = instance.get('/api/users/me').catch(error => error)
    const renewal = auth.refreshAccessToken()
    const expiresAt = Date.now() + 3600000
    requests[1].reply(ok({ token: 'renewed-a', expiresAt, expiresIn: 3600 }))
    expect(await renewal).toBe('renewed-a')
    if (outcome === '401') requests[0].fail(401)
    else requests[0].reply({ code: 1002, message: 'Old token revoked' })
    expect(axios.isCancel(await oldRequest)).toBe(true)
    expect(auth.accessToken).toBe('renewed-a')
    expect(auth.tokenExpiry).toBe(String(expiresAt))
    expect(appleMessage.error).not.toHaveBeenCalled()
  })

  it('does not retry an account A request using account B credentials', async () => {
    vi.useFakeTimers()
    const pending = instance.get('/api/chats', { retryDelay: 100 }).catch(error => error)
    requests[0].fail(503)
    await vi.advanceTimersByTimeAsync(0)
    switchAccount('b')
    await vi.advanceTimersByTimeAsync(100)
    expect(axios.isCancel(await pending)).toBe(true)
    expect(requests).toHaveLength(1)
    expect(auth.accessToken).toBe('token-b')
    expect(appleMessage.error).not.toHaveBeenCalled()
  })

  it('keeps the new profile request pending when an earlier session finishes', async () => {
    const old = auth.fetchUserInfo()
    switchAccount('b')
    const current = auth.fetchUserInfo()
    const currentRequest = auth._fetchingUserInfo
    requests[0].reply(ok({ user: { id: 'a', username: 'Old' } }))
    expect(await old).toBeNull()
    expect(auth._fetchingUserInfo).toBe(currentRequest)
    expect(auth.currentUser.username).toBe('b')
    requests[1].reply(ok({ user: { id: 'b', username: 'Current' }, userStats: {} }))
    expect(await current).toEqual({ id: 'b', username: 'Current' })
    expect(auth._fetchingUserInfo).toBeNull()
    expect(JSON.parse(localStorage.getItem('currentUser')).id).toBe('b')
  })

  it.each(['success', 'failure'])('keeps B renewal flags and tokens when A renewal returns %s', async result => {
    const old = auth.refreshAccessToken()
    switchAccount('b')
    const current = auth.refreshAccessToken()
    const currentRequest = auth.refreshPromise
    if (result === 'failure') requests[0].fail(401)
    else requests[0].reply(ok({ token: 'renewed-a', expiresIn: 3600 }))
    expect(await old).toBeNull()
    expect(auth.accessToken).toBe('token-b')
    expect(auth.isRefreshing).toBe(true)
    expect(auth.refreshPromise).toBe(currentRequest)
    requests[1].reply(ok({ token: 'renewed-b', expiresIn: 3600 }))
    expect(await current).toBe('renewed-b')
    expect(auth.isRefreshing).toBe(false)
    expect(auth.refreshPromise).toBeNull()
  })

  it('does not restore an older login after a newer login succeeds', async () => {
    const old = auth.login({ username: 'a', password: 'password-a' })
    const current = auth.login({ username: 'b', password: 'password-b' })
    requests[1].reply(ok({ token: 'token-b', user: { id: 'b' }, expiresIn: 3600 }))
    expect(await current).toBe(true)
    requests[0].reply(ok({ token: 'token-a', user: { id: 'a' }, expiresIn: 3600 }))
    expect(await old).toBe(false)
    expect(auth.accessToken).toBe('token-b')
    expect(auth.currentUser.id).toBe('b')
  })

  it('does not let a late logout clear the next account or its stores', async () => {
    const old = auth.logout()
    switchAccount('b')
    chat.recentSessions = [{ id: 'b-chat' }]
    notifications.notifications = [{ id: 'b-notification' }]
    requests[0].reply(ok(null))
    await old
    expect(auth.accessToken).toBe('token-b')
    expect(chat.recentSessions).toEqual([{ id: 'b-chat' }])
    expect(notifications.notifications).toEqual([{ id: 'b-notification' }])
  })

  it('ignores old chat history, conversation lists and unread counts after account switching', async () => {
    const history = chat.fetchMessagesForChat('a-chat')
    const recent = chat.fetchRecentChats()
    const counts = chat.fetchUnreadStats()
    switchAccount('b')
    chat.recentSessions = [{ id: 'b-chat' }]
    chat.unreadCounts = { 'b-chat': 2 }
    requests[0].reply(ok([{ id: 1, chatId: 'a-chat', content: 'Private A', senderId: 'a' }]))
    requests[1].reply(ok([{ id: 'a-chat' }]))
    requests[2].reply(ok({ unreadList: [{ chat_id: 'a-chat', unread_count: 9 }] }))
    await Promise.all([history, recent, counts])
    expect(chat.chatMessages).toEqual({})
    expect(chat.recentSessions).toEqual([{ id: 'b-chat' }])
    expect(chat.unreadCounts).toEqual({ 'b-chat': 2 })
  })

  it('does not let an old notification load finish B loading or start a follow-up request', async () => {
    const old = notifications.fetchNotifications()
    switchAccount('b')
    const current = notifications.fetchNotifications()
    requests[0].reply(ok({ notifications: [{ id: 'a' }], totalPages: 1 }))
    expect(await old).toBeNull()
    expect(notifications.isLoading).toBe(true)
    expect(notifications.notifications).toEqual([])
    expect(requests).toHaveLength(2)
    requests[1].reply(ok({ notifications: [{ id: 'b' }], totalPages: 1 }))
    await flushPromises()
    requests[2].reply(ok({ unreadCount: 1 }))
    await current
    expect(notifications.notifications).toEqual([{ id: 'b' }])
    expect(notifications.isLoading).toBe(false)
    expect(notifications.unreadCount).toBe(1)
  })

  it.each(['markAsRead', 'markAllAsRead', 'deleteReadNotifications'])('ignores a late notification %s mutation without touching B or refetching', async action => {
    notifications.notifications = [{ id: 1, isRead: false }]
    const old = notifications[action](1)
    switchAccount('b')
    notifications.notifications = [{ id: 1, isRead: false }]
    notifications.unreadCount = 4
    requests[0].reply(ok({ deletedCount: 3 }))
    expect(await old).toBeNull()
    expect(notifications.notifications).toEqual([{ id: 1, isRead: false }])
    expect(notifications.unreadCount).toBe(4)
    expect(requests).toHaveLength(1)
  })

  it('ignores old socket callbacks and deferred notification imports while the current client remains usable', async () => {
    chat.connectWebSocket()
    const oldClient = clients[0]
    oldClient.connected = true
    chat.subscribeToQueues()
    const oldCallbacks = Object.fromEntries(oldClient.subscribe.mock.calls)
    oldCallbacks['/user/queue/notifications']({ body: JSON.stringify({ id: 'old-import' }) })
    switchAccount('b')
    chat.connectWebSocket()
    const currentClient = clients[1]
    currentClient.connected = true
    chat.subscribeToQueues()
    oldCallbacks['/user/queue/chat-list-update']({ body: JSON.stringify({ id: 'a-chat' }) })
    oldCallbacks['/user/queue/notifications']({ body: JSON.stringify({ id: 'a-notification' }) })
    await oldClient.beforeConnect()
    oldClient.onWebSocketClose()
    await flushPromises()
    expect(toRaw(chat.stompClient)).toBe(currentClient)
    expect(currentClient.active).toBe(true)
    expect(chat.recentSessions).toEqual([])
    expect(notifications.notifications).toEqual([])
    expect(requests).toHaveLength(0)
    const callback = Object.fromEntries(currentClient.subscribe.mock.calls)['/user/queue/chat-list-update']
    callback({ body: JSON.stringify({ id: 'b-chat' }) })
    expect(chat.recentSessions[0].id).toBe('b-chat')
  })

  it.each(['explicit', 'seconds', 'legacy'])('reads %s token expiry without extending it by centuries', async format => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-12T00:00:00Z'))
    const expiresAt = Date.now() + 3600000
    const data = format === 'explicit' ? { expiresAt, expiresIn: 3600 }
      : { expiresIn: format === 'seconds' ? 3600 : expiresAt }
    const login = auth.login({ username: 'a', password: 'password-a' })
    requests[0].reply(ok({ user: { id: 'a' }, token: 'token-a', ...data }))
    await login
    expect(Number(auth.tokenExpiry)).toBe(expiresAt)
    vi.advanceTimersByTime(56 * 60000)
    expect(auth.needsRefresh).toBe(true)
  })

  it('adopts a cross-tab session without writing a logout back into shared storage', () => {
    const oldEpoch = auth.sessionEpoch
    localStorage.setItem('jwt_token', 'token-b')
    localStorage.setItem('currentUser', JSON.stringify({ user: { id: 'b' }, userStats: {} }))
    localStorage.setItem('token_expiry', String(Date.now() + 3600000))
    const remove = vi.spyOn(Storage.prototype, 'removeItem')
    auth.syncAuthStatus()
    expect(auth.sessionEpoch).toBeGreaterThan(oldEpoch)
    expect(auth.accessToken).toBe('token-b')
    expect(auth.currentUser).toEqual({ id: 'b' })
    expect(localStorage.getItem('jwt_token')).toBe('token-b')
    expect(remove).not.toHaveBeenCalled()
  })
})
