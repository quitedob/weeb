import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { reactive, nextTick } from 'vue'
import ChatPage from './ChatPage.vue'

const { api, chat } = vi.hoisted(() => ({
  api: { chat: { getChatList: vi.fn(), addReaction: vi.fn() }, contact: { getContacts: vi.fn() } },
  chat: {
    connectionStatus: 'connected', onlineUsers: new Set(), messagesForCurrentChat: [],
    canLoadMoreMessages: false, isTypingInCurrentChat: false,
    setActiveChat: vi.fn(), fetchMessagesForChat: vi.fn(), clearActiveChat: vi.fn()
  }
}))
vi.mock('@/api', () => ({ default: api }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => chat }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ currentUser: { id: 1, username: 'Viewer' } }) }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }), useRoute: () => ({ params: {}, query: {} }) }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn() } }))

let wrapper
let message
beforeEach(async () => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  api.chat.getChatList.mockResolvedValue({ code: 0, data: [
    { id: 'sidebar-row', sharedChatId: 100, type: 'PRIVATE', targetId: 2, name: 'Alice' }
  ] })
  api.chat.addReaction.mockResolvedValue({ code: 0 })
  api.contact.getContacts.mockResolvedValue({ code: 0, data: [] })
  chat.fetchMessagesForChat.mockResolvedValue()
  message = reactive({ id: 11, chatId: 100, content: 'Hello', msgContent: 'Hello', isFromMe: false,
    timestamp: new Date(), reactions: [{ emoji: '👍', reactionType: '👍', count: 2, userIds: [1, 2] }] })
  chat.messagesForCurrentChat = [message]
  wrapper = mount(ChatPage, { global: { stubs: { PaginatedEmojiPicker: true } } })
  await flushPromises()
  await wrapper.get('.chat-item').trigger('click')
  await flushPromises()
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('persisted message reactions', () => {
  it('renders the server aggregate and does not increment a toggle locally', async () => {
    expect(wrapper.get('.reaction-emoji').text()).toBe('👍')
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    await wrapper.get('[title="添加反应"]').trigger('click')
    await wrapper.findAll('.reaction-emoji-item').find(item => item.text() === '👍').trigger('click')
    await flushPromises()
    expect(api.chat.addReaction).toHaveBeenCalledExactlyOnceWith(11, '👍')
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    // The server toggle removed this user's reaction; its broadcast is authoritative.
    message.reactions = [{ emoji: '👍', reactionType: '👍', count: 1, userIds: [2] }]
    await nextTick()
    expect(wrapper.get('.reaction-count').text()).toBe('1')
    await wrapper.get('.reaction-item').trigger('click')
    await flushPromises()
    expect(api.chat.addReaction).toHaveBeenCalledTimes(2)
  })

  it('keeps the persisted count when a reaction request fails', async () => {
    api.chat.addReaction.mockRejectedValueOnce(new Error('request failed'))
    await wrapper.get('[title="添加反应"]').trigger('click')
    await wrapper.findAll('.reaction-emoji-item').find(item => item.text() === '👍').trigger('click')
    await flushPromises()
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    expect(message.reactions[0].userIds).toEqual([1, 2])
  })
})
