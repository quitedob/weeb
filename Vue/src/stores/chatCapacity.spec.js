import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import api from '@/api'
import { useAuthStore } from './authStore'
import { useChatStore } from './chatStore'
import { MESSAGE_STATUS as STATUS } from '@/utils/messageStatus'

vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))
vi.mock('@/utils/bugReporter', () => ({ default: {} }))
vi.mock('sockjs-client', () => ({ default: class {} }))
vi.mock('@stomp/stompjs', () => ({ Client: class {} }))
const ok = data => ({ code: 0, data })
const message = (id, chatId = 90, extra = {}) => ({ id, chatId, senderId: 2, content: `Message ${id}`, status: STATUS.SENT, ...extra })
let chat, history
beforeEach(() => {
  localStorage.clear(); setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.setCurrentUser({ id: 1, username: 'Alice' })
  auth.applyToken({ token: 'account-a', expiresIn: 3600 })
  chat = useChatStore()
  chat.setActiveChat({ sharedChatId: 90, type: 'PRIVATE', targetId: 2 })
  history = vi.spyOn(api.chat, 'getChatMessages').mockImplementation(async (id, { page, size }) => ok(
    Array.from({ length: 500 }, (_, i) => message(500 - i, id)).slice((page - 1) * size, page * size)
  ))
})
afterEach(() => vi.restoreAllMocks())

describe('recoverable bounded chat windows', () => {
  it('can navigate through all 500 messages using a 200-message window and return newer or latest', async () => {
    const observed = new Set()
    await chat.fetchMessagesForChat(90)
    for (let page = 1; page <= 10; page++) {
      if (page > 1) await chat.loadMoreMessages()
      chat.messagesForCurrentChat.forEach(row => observed.add(row.id))
      expect(chat.messagesForCurrentChat.length).toBeLessThanOrEqual(200)
    }
    expect(observed.size).toBe(500)
    expect(chat.messagesForCurrentChat[0].id).toBe(1)
    expect(chat.messagesForCurrentChat.at(-1).id).toBe(200)
    await chat.loadNewerMessages()
    expect(chat.messagesForCurrentChat[0].id).toBe(51)
    expect(chat.messagesForCurrentChat.at(-1).id).toBe(250)
    await chat.loadLatestMessages()
    expect(chat.messagesForCurrentChat.map(row => row.id)).toEqual(Array.from({ length: 50 }, (_, i) => i + 451))
    expect(chat.currentChatPagination.hasNewer).toBe(false)
  })

  it('keeps the viewed old window on new delivery, reconciles a pinned sender ACK and counts duplicates once', async () => {
    await chat.fetchMessagesForChat(90)
    for (let page = 2; page <= 6; page++) await chat.loadMoreMessages()
    const viewed = chat.messagesForCurrentChat.map(row => row.id)
    chat.stompClient = { connected: true, publish: vi.fn() }
    await chat.sendMessage('Pending from old history', 2)
    const sent = JSON.parse(chat.stompClient.publish.mock.calls[0][0].body)
    expect(chat.messagesForCurrentChat).toHaveLength(201)
    chat.handleIncomingChatMessage(message(501))
    chat.handleIncomingChatMessage(message('501'))
    expect(chat.messagesForCurrentChat.filter(row => typeof row.id === 'number').map(row => row.id)).toEqual(viewed)
    expect(chat.unreadCounts[90]).toBe(1)
    chat.handleReadReceipt({ chatId: 90, readerId: 1, messageId: 500, unreadCount: 0 })
    expect(chat.unreadCounts[90]).toBe(1)
    const ack = message(502, 90, { senderId: 1, clientMessageId: sent.clientMessageId })
    chat.handleIncomingChatMessage(ack); chat.handleIncomingChatMessage(ack)
    expect(chat.messagesForCurrentChat.map(row => row.id)).toEqual(viewed)
    expect(chat.messagesForCurrentChat.some(row => row.status === STATUS.SENDING)).toBe(false)
  })

  it('evicts least recently used conversations and every associated cache map, then reopens from SQL', async () => {
    chat.clearActiveChat()
    await chat.fetchMessagesForChat(1)
    chat.readCursors['1:2'] = '4'; chat.readRequests['1:2'] = {}
    chat.reactionSnapshots['1:4'] = { reactionVersion: 1, reactions: [] }
    chat.newerIncomingIds[1] = ['501']; chat.isTyping[1] = { 2: true }
    for (let id = 2; id <= 51; id++) await chat.fetchMessagesForChat(id)
    expect(Object.keys(chat.cacheEntries)).toHaveLength(50)
    for (const map of [chat.cacheEntries, chat.chatMessages, chat.chatPagination, chat.syncCursors,
      chat.syncRequests, chat.historyRequests, chat.recentMessageIds, chat.newerIncomingIds, chat.isTyping]) expect(map[1]).toBeUndefined()
    for (const map of [chat.readCursors, chat.readRequests, chat.reactionSnapshots]) expect(Object.keys(map).some(key => key.startsWith('1:'))).toBe(false)
    history.mockResolvedValueOnce(ok([message(999, 1)]))
    chat.setActiveChat({ sharedChatId: 1, type: 'PRIVATE' })
    await chat.fetchMessagesForChat(1)
    expect(history).toHaveBeenLastCalledWith('1', { page: 1, size: 50 })
    expect(chat.messagesForCurrentChat.map(row => row.id)).toEqual([999])
    expect(chat.syncCursors[1]).toBe('999')
  })

  it('retains failed and pending sends as pins when other conversations are evicted', async () => {
    chat.stompClient = { connected: true, publish: vi.fn() }
    await chat.sendMessage('Keep pending', 2)
    chat.messagesForCurrentChat[0].status = STATUS.FAILED
    chat.clearActiveChat()
    for (let id = 1; id <= 55; id++) chat.setMessages(id, [message(id, id)])
    expect(Object.keys(chat.cacheEntries)).toHaveLength(50)
    expect(chat.chatMessages[90][0]).toMatchObject({ status: STATUS.FAILED, sendPayload: { sharedChatId: 90 } })
    chat.$reset()
    for (const map of [chat.cacheEntries, chat.chatMessages, chat.chatPagination, chat.syncCursors,
      chat.syncRequests, chat.historyRequests, chat.recentMessageIds, chat.newerIncomingIds,
      chat.readCursors, chat.readRequests, chat.reactionSnapshots]) expect(map).toEqual({})
  })

  it('does not resurrect an evicted conversation or overwrite latest navigation with a delayed older page', async () => {
    let finishOld
    history.mockReturnValueOnce(new Promise(resolve => { finishOld = resolve }))
    const old = chat.fetchMessagesForChat(90, 2)
    await chat.loadLatestMessages()
    finishOld(ok([message(1)])); await old
    expect(chat.messagesForCurrentChat[0].id).toBe(451)
    let finishEvicted
    history.mockReturnValueOnce(new Promise(resolve => { finishEvicted = resolve }))
    const evicted = chat.fetchMessagesForChat(2)
    chat.clearChatMessages(2)
    finishEvicted(ok([message(1, 2)])); await evicted
    expect(chat.chatMessages[2]).toBeUndefined()
    expect(chat.cacheEntries[2]).toBeUndefined()
  })

  it('loads only the active history at connection instead of 50 messages for every sidebar conversation', async () => {
    vi.spyOn(api.chat, 'getChatList').mockResolvedValue(ok(Array.from({ length: 1000 }, (_, i) => ({ sharedChatId: i + 1 }))))
    const sync = vi.spyOn(api.chat, 'syncMessages').mockResolvedValue(ok({ list: [], nextAfterMessageId: 500, hasMore: false }))
    vi.spyOn(api.chat, 'getUnreadStats').mockResolvedValue(ok({ unreadList: [] }))
    await chat.fetchOfflineMessages()
    expect(chat.recentSessions).toHaveLength(1000)
    expect(history).toHaveBeenCalledTimes(1)
    expect(sync).toHaveBeenCalledTimes(1)
    expect(Object.values(chat.chatMessages).flat()).toHaveLength(50)
    chat.clearActiveChat(); chat.clearChatMessages(90)
    history.mockClear(); sync.mockClear()
    await chat.fetchOfflineMessages()
    expect(history).not.toHaveBeenCalled()
    expect(sync).not.toHaveBeenCalled()
  })
})
