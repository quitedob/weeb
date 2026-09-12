import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { reactive, nextTick } from 'vue'
import ChatPage from './ChatPage.vue'

const { api, chat } = vi.hoisted(() => ({
  api: { chat: { getChatList: vi.fn(), setReaction: vi.fn() }, contact: { getContacts: vi.fn() } },
  chat: {
    connectionStatus: 'connected', onlineUsers: new Set(), messagesForCurrentChat: [],
    canLoadMoreMessages: false, isTypingInCurrentChat: false,
    setActiveChat: vi.fn(), fetchMessagesForChat: vi.fn(), clearActiveChat: vi.fn(),
    sendMessage: vi.fn(), retryMessage: vi.fn(), markChatAsRead: vi.fn(), sendTypingIndicator: vi.fn(),
    loadMoreMessages: vi.fn(), loadNewerMessages: vi.fn(), loadLatestMessages: vi.fn()
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
  api.chat.setReaction.mockResolvedValue({ code: 0 })
  api.contact.getContacts.mockResolvedValue({ code: 0, data: [] })
  chat.fetchMessagesForChat.mockResolvedValue()
  chat.currentChatPagination = reactive({ hasNewer: false })
  chat.canLoadMoreMessages = true
  message = reactive({ id: 11, chatId: 100, content: 'Hello', msgContent: 'Hello', isFromMe: false,
    timestamp: new Date(), reactions: [{ emoji: '👍', reactionType: '👍', count: 2, userIds: [1, 2] }] })
  chat.messagesForCurrentChat = reactive([message])
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
  it('offers older/newer/latest navigation and does not acknowledge or scroll to latest while viewing old history', async () => {
    const container = wrapper.get('.message-container').element
    Object.defineProperty(container, 'scrollHeight', { configurable: true, value: 5000 })
    container.scrollTop = 150
    chat.markChatAsRead.mockClear()
    chat.loadMoreMessages.mockImplementation(async () => {
      chat.currentChatPagination.hasNewer = true
      chat.messagesForCurrentChat.splice(0, 1, { ...message, id: 3, msgContent: 'Older history' })
    })
    chat.loadNewerMessages.mockResolvedValue()
    chat.loadLatestMessages.mockImplementation(async () => {
      chat.currentChatPagination.hasNewer = false
      chat.messagesForCurrentChat.splice(0, 1, { ...message, id: 500, msgContent: 'Latest history' })
    })
    await wrapper.get('[title="加载更多"]').trigger('click')
    await flushPromises()
    expect(chat.loadMoreMessages).toHaveBeenCalledTimes(1)
    expect(chat.markChatAsRead).not.toHaveBeenCalled()
    expect(container.scrollTop).toBe(150)
    await wrapper.get('[title="较新消息"]').trigger('click')
    await flushPromises()
    expect(chat.loadNewerMessages).toHaveBeenCalledTimes(1)
    await wrapper.get('[title="返回最新"]').trigger('click')
    await flushPromises()
    expect(chat.loadLatestMessages).toHaveBeenCalledTimes(1)
    expect(chat.markChatAsRead).toHaveBeenCalledWith(100, '500')
    expect(container.scrollTop).toBe(5000)
    expect(wrapper.find('[title="返回最新"]').exists()).toBe(false)
  })

  it('renders the server aggregate and does not increment a toggle locally', async () => {
    expect(wrapper.get('.reaction-emoji').text()).toBe('👍')
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    await wrapper.get('[title="添加反应"]').trigger('click')
    await wrapper.findAll('.reaction-emoji-item').find(item => item.text() === '👍').trigger('click')
    await flushPromises()
    expect(api.chat.setReaction).toHaveBeenCalledExactlyOnceWith(11, '👍', true)
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    // A newer server event supplies the authoritative aggregate.
    message.reactions = [{ emoji: '👍', reactionType: '👍', count: 1, userIds: [2] }]
    await nextTick()
    expect(wrapper.get('.reaction-count').text()).toBe('1')
    await wrapper.get('.reaction-item').trigger('click')
    await flushPromises()
    expect(api.chat.setReaction).toHaveBeenCalledTimes(2)
    expect(api.chat.setReaction).toHaveBeenLastCalledWith(11, '👍', true)
  })

  it('keeps the persisted count when a reaction request fails', async () => {
    api.chat.setReaction.mockRejectedValueOnce(new Error('request failed'))
    await wrapper.get('[title="添加反应"]').trigger('click')
    await wrapper.findAll('.reaction-emoji-item').find(item => item.text() === '👍').trigger('click')
    await flushPromises()
    expect(wrapper.get('.reaction-count').text()).toBe('2')
    expect(message.reactions[0].userIds).toEqual([1, 2])
  })

  it('renders numeric sending states, retains failed messages for retry and acknowledges rendered persisted IDs', async () => {
    expect(chat.markChatAsRead).toHaveBeenCalledWith(100, '11')
    await wrapper.get('textarea').setValue('Hello from the page')
    await wrapper.get('.send-btn').trigger('click')
    await flushPromises()
    expect(chat.sendMessage).toHaveBeenCalledWith({ content: 'Hello from the page', contentType: 1,
      url: null, atUidList: [] }, 100, 'PRIVATE', 1)
    expect(wrapper.get('textarea').element.value).toBe('')
    const pending = reactive({ id: 'temp-1', clientMessageId: 'temp-1', content: 'Pending', isFromMe: true,
      status: 0, timestamp: new Date(), reactions: [] })
    chat.messagesForCurrentChat.push(pending)
    await nextTick()
    expect(wrapper.findAll('.message-status').at(-1).text()).toBe('⏳')
    pending.status = 4
    await nextTick()
    const retry = wrapper.findAll('.message-status .action-btn').at(-1)
    expect(retry.text()).toBe('重试')
    await retry.trigger('click')
    expect(chat.retryMessage).toHaveBeenCalledWith(pending)
    Object.assign(pending, { id: 12, status: 1 })
    await flushPromises()
    expect(wrapper.findAll('.message-status').at(-1).text()).toBe('✓')
    expect(chat.markChatAsRead).toHaveBeenLastCalledWith(100, '12')
  })
})
