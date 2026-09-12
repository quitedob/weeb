import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './authStore'
import { useChatStore } from './chatStore'
import api from '@/api'

const { clients } = vi.hoisted(() => ({ clients: [] }))
vi.mock('@stomp/stompjs', () => ({ Client: class {
  constructor(options) {
    Object.assign(this, options)
    this.active = false
    this.connected = false
    this.subscribe = vi.fn()
    this.publish = vi.fn()
    this.deactivate = vi.fn(() => { this.active = false; this.connected = false })
    clients.push(this)
  }
  activate() { this.active = true }
} }))
vi.mock('sockjs-client', () => ({ default: class {} }))
vi.mock('@/utils/bugReporter', () => ({ default: { reportBug: vi.fn() } }))
vi.mock('./notificationStore', () => ({ useNotificationStore: () => ({ resetState: vi.fn() }) }))
vi.mock('@/api', () => ({ default: {
  auth: { logout: vi.fn(async () => ({ code: 0 })) },
  chat: {
    getChatMessages: vi.fn(),
    getOfflineMessages: vi.fn(async () => ({ code: 0, data: [] })),
  },
} }))

let chat
beforeEach(() => {
  vi.useFakeTimers()
  localStorage.clear()
  setActivePinia(createPinia())
  clients.length = 0
  const auth = useAuthStore()
  auth.accessToken = 'local-test-access'
  auth.currentUser = { id: 1, username: 'viewer' }
  chat = useChatStore()
})
afterEach(() => {
  chat.disconnectWebSocket()
  vi.clearAllTimers()
  vi.useRealTimers()
  vi.clearAllMocks()
})

function connect() {
  chat.connectWebSocket()
  const client = clients.at(-1)
  client.connected = true
  client.onConnect({})
  return client
}

describe('application heartbeat lease', () => {
  it('publishes every30 seconds with one timer and resumes once after reconnect', () => {
    const client = connect()
    vi.advanceTimersByTime(29999)
    expect(client.publish).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(client.publish).toHaveBeenCalledTimes(1)
    expect(client.publish).toHaveBeenCalledWith({ destination: '/app/chat/heartbeat', body: '{}' })
    client.onConnect({})
    client.onConnect({})
    vi.advanceTimersByTime(30000)
    expect(client.publish).toHaveBeenCalledTimes(2)
    client.connected = false
    client.onWebSocketClose({})
    expect(chat.heartbeatInterval).toBeNull()
    vi.advanceTimersByTime(90000)
    expect(client.publish).toHaveBeenCalledTimes(2)
    client.connected = true
    client.onConnect({})
    vi.advanceTimersByTime(30000)
    expect(client.publish).toHaveBeenCalledTimes(3)
  })

  it('stops on logout and cannot publish again from an old timer', async () => {
    const client = connect()
    await useAuthStore().logout()
    expect(client.deactivate).toHaveBeenCalled()
    expect(chat.heartbeatInterval).toBeNull()
    vi.advanceTimersByTime(120000)
    expect(client.publish).not.toHaveBeenCalled()
  })

  it('clears the timer even when transport deactivation throws', () => {
    const client = connect()
    client.deactivate.mockImplementation(() => { throw new Error('transport closed') })
    chat.disconnectWebSocket()
    expect(chat.heartbeatInterval).toBeNull()
    vi.advanceTimersByTime(60000)
    expect(client.publish).not.toHaveBeenCalled()
  })
})

describe('server reaction aggregates', () => {
  const aggregate = [{ emoji: '👍', reactionType: '👍', count: 2, userIds: [1, 2] }]

  it('preserves history and realtime aggregates through message normalization', async () => {
    api.chat.getChatMessages.mockResolvedValue({ code: 0, data: [
      { id: 101, chatId: 90, senderId: 1, content: { content: 'history' }, reactions: aggregate },
    ] })
    await chat.fetchMessagesForChat(90)
    expect(chat.chatMessages['90'][0].reactions).toEqual(aggregate)
    chat.handleIncomingChatMessage({ id: 102, sharedChatId: 90, fromId: 1,
      content: 'realtime', isFromMe: true, reactions: aggregate })
    expect(chat.chatMessages['90'].find(message => message.id === 102).reactions).toEqual(aggregate)
  })

  it('matches numeric/string IDs and replaces counts without optimistic increments', () => {
    chat.chatMessages['90'] = [{ id: 101, reactions: [] }, { messageId: '102', reactions: [] }]
    chat.handleReactionChange({ sharedChatId: '90', messageId: '101', reactions: aggregate })
    chat.handleReactionChange({ chatId: 90, messageId: 101, reactions: aggregate })
    expect(chat.chatMessages['90'][0].reactions).toEqual(aggregate)
    expect(chat.chatMessages['90'][0].reactions[0].count).toBe(2)
    chat.handleReactionChange({ chatId: '90', messageId: 102, reactions: aggregate })
    expect(chat.chatMessages['90'][1].reactions).toEqual(aggregate)
    chat.handleReactionChange({ chatId: 90, messageId: '101', reactions: [] })
    expect(chat.chatMessages['90'][0].reactions).toEqual([])
  })
})
