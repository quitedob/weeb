import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { effectScope, nextTick } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { useAuthStore } from './authStore'
import { useChatStore } from './chatStore'
import { useNotificationStore } from './notificationStore'
import { useThemeStore } from './themeStore'
import { initializeStores } from './index'
import ChatPage from '@/views/chat/ChatPage.vue'
const { clients } = vi.hoisted(() => ({ clients: [] }))
vi.mock('@stomp/stompjs', () => ({ Client: class {
  constructor(options) { this.options = options; this.active = false; this.connected = false; this.subscribe = vi.fn(); this.publish = vi.fn(); clients.push(this) }
  activate() { this.active = true }
  deactivate() { this.active = false; this.connected = false }
} }))
vi.mock('sockjs-client', () => ({ default: class {} }))
vi.mock('@/utils/bugReporter', () => ({ default: { reportBug: vi.fn() } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }), useRoute: () => ({ params: {}, query: {} }) }))
vi.mock('@/api', () => ({ default: {
  chat: { getChatList: vi.fn(async () => ({ code: 0, data: [] })), getOfflineMessages: vi.fn(async () => ({ code: 0, data: [] })) },
  contact: { getContacts: vi.fn(async () => ({ code: 0, data: [] })) },
  notification: {}
  ,user: { getSettings: vi.fn(async () => ({ code: 0, data: { notifications: { newMessages: true } } })) }
} }))
let wrapper
beforeEach(() => { localStorage.clear(); setActivePinia(createPinia()); vi.useFakeTimers(); clients.length = 0 })
afterEach(() => { wrapper?.unmount(); wrapper = null; vi.useRealTimers() })
describe('session-owned WebSocket', () => {
  it('opens one client, subscribes only to private destinations and does not invent a chat ID', () => {
    const auth = useAuthStore(); auth.accessToken = 'test-token'
    const chat = useChatStore()
    chat.connectWebSocket(); chat.connectWebSocket()
    expect(clients).toHaveLength(1)
    clients[0].connected = true
    chat.subscribeToQueues()
    const destinations = clients[0].subscribe.mock.calls.map(([destination]) => destination)
    expect(destinations).toContain('/user/queue/private')
    expect(destinations).toContain('/user/queue/notifications')
    expect(destinations.every(destination => destination.startsWith('/user/queue/') && !destination.includes('*'))).toBe(true)
    chat.sendWebSocketMessage({ type: 'chat', data: { chatType: 'PRIVATE', targetId: 7, content: 'hello' } })
    const payload = JSON.parse(clients[0].publish.mock.calls[0][0].body)
    expect(payload.targetId).toBe('7')
    expect(payload.chatId).toBeUndefined()
    chat.disconnectWebSocket()
    expect(clients[0].active).toBe(false)
  })
  it('starts a restored session, keeps its socket when leaving chat, and disconnects on logout', async () => {
    const auth = useAuthStore(); auth.accessToken = 'test-token'; auth.currentUser = { id: 1, username: 'viewer' }
    vi.spyOn(auth, 'syncAuthStatus').mockImplementation(() => {})
    vi.spyOn(auth, 'startTokenRefreshTimer').mockImplementation(() => {})
    const notifications = useNotificationStore()
    vi.spyOn(notifications, 'startAutoRefresh').mockImplementation(() => {})
    vi.spyOn(notifications, 'fetchUnreadCount').mockResolvedValue()
    vi.spyOn(useThemeStore(), 'initTheme').mockImplementation(() => {})
    const scope = effectScope(); scope.run(initializeStores)
    expect(clients).toHaveLength(1)
    const chat = useChatStore()
    wrapper = mount(ChatPage, { global: { stubs: { PaginatedEmojiPicker: true } } })
    await flushPromises()
    wrapper.unmount(); wrapper = null
    expect(chat.stompClient).not.toBeNull()
    expect(clients[0].active).toBe(true)
    auth.logoutCleanup(); await nextTick()
    expect(clients[0].active).toBe(false)
    expect(chat.stompClient).toBeNull()
    scope.stop()
  })
})
