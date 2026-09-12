import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ChatPage from './ChatPage.vue'

const { api, chat, push } = vi.hoisted(() => ({
  api: {
    chat: { getChatList: vi.fn(), createChat: vi.fn() },
    contact: { getContacts: vi.fn() }
  },
  chat: {
    connectionStatus: 'connected',
    onlineUsers: new Set(),
    messagesForCurrentChat: [],
    canLoadMoreMessages: false,
    isTypingInCurrentChat: false,
    setActiveChat: vi.fn(),
    fetchMessagesForChat: vi.fn(),
    clearActiveChat: vi.fn()
  },
  push: vi.fn()
}))
vi.mock('@/api', () => ({ default: api }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => chat }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ currentUser: { id: 1, username: 'Viewer' } }) }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }), useRoute: () => ({ params: {}, query: {} }) }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn() } }))

let wrapper
beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  api.chat.getChatList.mockResolvedValue({ code: 0, data: [] })
  api.contact.getContacts.mockResolvedValue({ code: 0, data: [
    { id: 7, username: 'Alice', bio: 'Available' },
    { id: 8, username: 'Bob' }
  ] })
  chat.fetchMessagesForChat.mockResolvedValue()
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  document.body.innerHTML = ''
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('New Chat dialog', () => {
  it('manages page focus and Escape while preserving contact creation and Add Friend navigation', async () => {
    wrapper = mount(ChatPage, { attachTo: document.body, global: { stubs: { PaginatedEmojiPicker: true } } })
    await flushPromises()
    const trigger = wrapper.get('.new-chat-btn')
    const openDialog = async () => {
      trigger.element.focus()
      await trigger.trigger('click')
      await nextTick()
      await nextTick()
      return document.querySelector('[role="dialog"]')
    }

    let dialog = await openDialog()
    expect(dialog.getAttribute('aria-label')).toBe('新建聊天')
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    expect(document.activeElement).toBe(dialog)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }))
    await nextTick()
    await nextTick()
    expect(document.querySelector('[role="dialog"]')).toBeNull()
    expect(document.activeElement).toBe(trigger.element)

    dialog = await openDialog()
    const search = dialog.querySelector('input[aria-label="搜索联系人"]')
    search.value = 'alice'
    search.dispatchEvent(new Event('input', { bubbles: true }))
    await nextTick()
    expect(dialog.querySelectorAll('.contact-item')).toHaveLength(1)
    const contact = dialog.querySelector('.contact-item')
    expect(contact.tagName).toBe('BUTTON')
    expect(contact.textContent).toContain('Alice')
    const createdChat = { id: 'private-7', sharedChatId: 37, targetId: 7, type: 'PRIVATE', name: 'Alice' }
    api.chat.createChat.mockResolvedValue({ code: 0, data: createdChat })
    api.chat.getChatList.mockResolvedValue({ code: 0, data: [createdChat] })
    contact.focus()
    contact.click()
    await flushPromises()
    expect(api.chat.createChat).toHaveBeenCalledWith({ targetId: '7' })
    expect(chat.setActiveChat).toHaveBeenCalledWith(expect.objectContaining({ sharedChatId: 37, targetId: 7 }))
    expect(chat.fetchMessagesForChat).toHaveBeenCalledWith(37)
    expect(document.querySelector('[role="dialog"]')).toBeNull()

    dialog = await openDialog()
    const emptySearch = dialog.querySelector('input')
    emptySearch.value = 'Nobody'
    emptySearch.dispatchEvent(new Event('input', { bubbles: true }))
    await nextTick()
    dialog.querySelector('.empty-contacts button').click()
    await nextTick()
    expect(push).toHaveBeenCalledWith('/search?type=user')
    expect(document.querySelector('[role="dialog"]')).toBeNull()
  })
})
