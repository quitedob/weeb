import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import axios from 'axios'
import instance from '@/api/axiosInstance'
import chatApi from '@/api/modules/chat'
import { useAuthStore } from './authStore'
import { useChatStore } from './chatStore'
import { MESSAGE_STATUS as STATUS } from '@/utils/messageStatus'

vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))
vi.mock('@/utils/bugReporter', () => ({ default: {} }))
vi.mock('sockjs-client', () => ({ default: class {} }))
vi.mock('@stomp/stompjs', () => ({ Client: class {} }))

const originalAdapter = instance.defaults.adapter
let auth, chat, requests
const envelope = data => ({ code: 0, message: 'OK', data })
const message = (id, extra = {}) => ({ id, chatId: 90, senderId: 1, content: { content: `Message ${id}`, contentType: 1 },
  createdAt: '2026-09-12T11:00:00', status: STATUS.SENT, reactionVersion: 0, reactions: [], ...extra })
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  auth = useAuthStore()
  auth.setCurrentUser({ id: 1, username: 'Alice' })
  auth.applyToken({ token: 'account-a', expiresIn: 3600 })
  chat = useChatStore()
  chat.setActiveChat({ id: 'owner-row', sharedChatId: 90, type: 'PRIVATE', targetId: 2, name: 'Bob' })
  requests = []
  instance.defaults.adapter = config => new Promise((resolve, reject) => {
    requests.push({ config, body: config.data ? JSON.parse(config.data) : null,
      reply: data => resolve({ status: 200, config, headers: {}, data: envelope(data) }),
      fail: (status = 503) => reject(new axios.AxiosError('Unavailable', 'ERR_BAD_RESPONSE', config, {},
        { status, config, data: { message: 'Unavailable' } })) })
  })
})
afterEach(() => { instance.defaults.adapter = originalAdapter; vi.restoreAllMocks() })

describe('durable chat client contracts', () => {
  it('reconciles a sender acknowledgement before duplicate suppression and retains the canonical ID', async () => {
    chat.stompClient = { connected: true, publish: vi.fn() }
    await chat.sendMessage('Hello', 90)
    const sent = JSON.parse(chat.stompClient.publish.mock.calls[0][0].body)
    expect(sent).toMatchObject({ sharedChatId: 90, targetId: '2' })
    expect(chat.stompClient.publish.mock.calls[0][0].destination).toBe('/app/chat/private')
    expect(chat.messagesForCurrentChat[0].status).toBe(STATUS.SENDING)
    const ack = message(101, { clientMessageId: sent.clientMessageId, content: 'Hello' })
    chat.handleIncomingChatMessage(ack)
    chat.handleIncomingChatMessage({ ...ack, id: '101', eventId: 'outbox-1', content: { content: 'Hello' } })
    expect(chat.messagesForCurrentChat).toHaveLength(1)
    expect(chat.messagesForCurrentChat[0]).toMatchObject({ id: '101', clientMessageId: sent.clientMessageId,
      status: STATUS.SENT, msgContent: 'Hello', isFromMe: true })
    expect(chat.messagesForCurrentChat[0].tempId).toBeUndefined()
    chat.updateMessageStatus(null, STATUS.FAILED, sent.clientMessageId)
    expect(chat.messagesForCurrentChat[0].status).toBe(STATUS.SENT)
  })

  it('uses the same client ID across transport fallback, a failed request and a user retry', async () => {
    chat.stompClient = { connected: true, publish: vi.fn(() => { throw new Error('closed') }) }
    const first = chat.sendMessage({ content: 'Retry me', contentType: 1, url: null, atUidList: [] }, 90).catch(error => error)
    const published = JSON.parse(chat.stompClient.publish.mock.calls[0][0].body)
    expect(requests[0].body.clientMessageId).toBe(published.clientMessageId)
    requests[0].fail()
    await first
    const failed = chat.messagesForCurrentChat[0]
    expect(failed.status).toBe(STATUS.FAILED)
    chat.stompClient.connected = false
    const retry = chat.retryMessage(failed)
    expect(requests[1].body).toEqual(requests[0].body)
    requests[1].reply(message(110, { content: requests[1].body.content, clientMessageId: published.clientMessageId }))
    await retry
    expect(chat.messagesForCurrentChat).toHaveLength(1)
    expect(chat.messagesForCurrentChat[0]).toMatchObject({ id: 110, status: STATUS.SENT })
  })

  it('merges history with concurrent sender acknowledgements and pending messages', async () => {
    chat.stompClient = { connected: true, publish: vi.fn() }
    await chat.sendMessage('Pending', 90)
    const load = chat.fetchMessagesForChat(90)
    chat.handleIncomingChatMessage(message(102, { senderId: 2 }))
    requests[0].reply([message(102, { senderId: '2' }), message(101)])
    await load
    expect(chat.messagesForCurrentChat.map(item => item.id)).toEqual([101, 102, expect.stringMatching(/^temp_/)])
    expect(chat.messagesForCurrentChat[1].isFromMe).toBe(false)
    expect(chat.syncCursors[90]).toBe('102')
  })

  it('counts duplicate outbox events once and retains conversation metadata', () => {
    chat.clearActiveChat()
    chat.recentSessions = [{ id: 90, sharedChatId: 90, name: 'Bob', type: 'PRIVATE', targetId: 2 }]
    chat.handleIncomingChatMessage(message(101, { senderId: 2, eventId: 'event-1' }))
    chat.handleIncomingChatMessage(message('101', { senderId: '2', eventId: 'event-1' }))
    expect(chat.unreadCounts[90]).toBe(1)
    expect(chat.unreadCountMap[90]).toBe(1)
    expect(chat.recentSessions[0]).toMatchObject({ name: 'Bob', type: 'PRIVATE', targetId: 2 })
  })

  it('keeps newer reactions across reordered broadcasts, HTTP replies, acknowledgements and history', async () => {
    const latest = [{ emoji: '👍', count: 2, userIds: [1, 2] }]
    chat.handleReactionChange({ chatId: 90, messageId: 101, reactionVersion: 3, reactions: latest })
    chat.handleIncomingChatMessage(message(101, { reactionVersion: 1 }))
    chat.handleReactionChange({ chatId: 90, messageId: 101, reactionVersion: 2, reactions: [] })
    chat.handleReactionChange({ chatId: 90, messageId: 101, reactionVersion: 3, reactions: [] })
    const history = chat.fetchMessagesForChat(90)
    requests[0].reply([message(101, { reactionVersion: 2 })])
    await history
    expect(chat.messagesForCurrentChat[0]).toMatchObject({ reactionVersion: 3, reactions: latest })
    const add = chatApi.setReaction(101, '👍', true)
    expect(requests[1].config).toMatchObject({ method: 'put', url: '/api/chats/messages/101/react', params: { reactionType: '👍' } })
    requests[1].reply({ messageId: 101, chatId: 90, reactionVersion: 4, reactions: latest })
    chat.handleReactionChange((await add).data)
    const remove = chatApi.setReaction(101, '👍', false)
    expect(requests[2].config).toMatchObject({ method: 'delete', params: { reactionType: '👍' } })
    requests[2].reply({ messageId: 101, chatId: 90, reactionVersion: 5, reactions: [] })
    chat.handleReactionChange((await remove).data)
    expect(chat.messagesForCurrentChat[0]).toMatchObject({ reactionVersion: 5, reactions: [] })
  })

  it('acknowledges only a displayed cursor over one transport and ignores late older read responses', async () => {
    chat.stompClient = { connected: true, publish: vi.fn() }
    await chat.markChatAsRead(90)
    expect(requests).toHaveLength(0)
    chat.mergeConfirmedMessage(90, message(101, { senderId: 2 }))
    const first = chat.sendReadReceipt(90, 101)
    expect(requests[0].body).toEqual({ lastReadMessageId: '101' })
    chat.mergeConfirmedMessage(90, message(102, { senderId: 2 }))
    const second = chat.markChatAsRead(90, '102')
    requests[1].reply({ chatId: 90, readerId: 1, lastReadMessageId: 102, unreadCount: 0 })
    await second
    requests[0].reply({ chatId: 90, readerId: 1, lastReadMessageId: 101, unreadCount: 1 })
    await first
    expect(chat.unreadCounts[90]).toBe(0)
    expect(chat.stompClient.publish).not.toHaveBeenCalled()
    await chat.markChatAsRead(90, 101)
    expect(requests).toHaveLength(2)
  })

  it('bounds private read receipts and records one group reader without claiming everyone read', () => {
    chat.chatMessages[90] = [message(100, { isFromMe: true }), message(101, { isFromMe: true }),
      message('temp-1', { isFromMe: true, status: STATUS.SENDING })]
    chat.handleReadReceipt({ chatId: 90, readerId: 2, messageId: 100 })
    expect(chat.chatMessages[90].map(item => item.status)).toEqual([STATUS.READ, STATUS.SENT, STATUS.SENDING])
    chat.activeChatSession.type = 'GROUP'
    chat.handleReadReceipt({ chatId: 90, readerId: 3, lastReadMessageId: 101 })
    expect(chat.chatMessages[90][1]).toMatchObject({ status: STATUS.SENT, readBy: ['3'] })
  })

  it('does not erase a newer incoming message with a delayed read snapshot', () => {
    chat.clearActiveChat()
    chat.handleIncomingChatMessage(message(102, { senderId: 2 }))
    chat.handleReadReceipt({ chatId: 90, readerId: 1, lastReadMessageId: 101, unreadCount: 0 })
    expect(chat.unreadCounts[90]).toBe(1)
    chat.handleReadReceipt({ chatId: 90, readerId: 1, lastReadMessageId: 102, unreadCount: 0 })
    expect(chat.unreadCounts[90]).toBe(0)
  })

  it('pages SQL recovery from its durable cursor without skipping a gap after a newer live event', async () => {
    chat.syncCursors[90] = '100'
    chat.handleIncomingChatMessage(message(104))
    const recovery = chat.syncChatMessages(90)
    const duplicate = chat.syncChatMessages(90)
    expect(requests).toHaveLength(1)
    expect(requests[0].config).toMatchObject({ url: '/api/chats/90/sync', params: { afterMessageId: '100', size: 100 } })
    requests[0].reply({ list: [message(101), message(102)], nextAfterMessageId: 102, hasMore: true })
    await flushPromises()
    expect(requests[1].config.params.afterMessageId).toBe('102')
    requests[1].reply({ list: [message(103), message(104)], nextAfterMessageId: 104, hasMore: false })
    await Promise.all([recovery, duplicate])
    expect(chat.messagesForCurrentChat.map(item => item.id)).toEqual([101, 102, 103, 104])
    expect(chat.syncCursors[90]).toBe('104')
  })

  it('refreshes old loaded message state before reconnect catch-up without moving its SQL cursor', async () => {
    chat.syncCursors[90] = '102'
    chat.mergeConfirmedMessage(90, message(100))
    chat.mergeConfirmedMessage(90, message(101))
    chat.mergeConfirmedMessage(90, message(102))
    const recovery = chat.fetchOfflineMessages()
    requests[0].reply([{ id: 'row', sharedChatId: 90, type: 'PRIVATE' }])
    await flushPromises()
    expect(requests[1].config).toMatchObject({ url: '/api/chats/90/messages/state', params: { ids: '100,101,102' } })
    requests[1].reply([
      message(100, { status: STATUS.READ, reactionVersion: 3, reactions: [{ emoji: '👍', count: 1, userIds: [2] }] }),
      message(101, { isRecalled: 1 })
    ])
    await flushPromises()
    expect(chat.chatMessages[90].map(item => item.id)).toEqual([100, 101])
    expect(chat.chatMessages[90][0]).toMatchObject({ status: STATUS.READ, reactionVersion: 3 })
    expect(chat.chatMessages[90][1].isRecalled).toBe(1)
    expect(requests[2].config.params.afterMessageId).toBe('102')
    requests[2].reply({ list: [], nextAfterMessageId: 102, hasMore: false })
    await flushPromises()
    requests[3].reply({ unreadList: [] })
    await recovery
  })

  it('bounds loaded-state requests to 100 IDs per batch', async () => {
    for (let id = 1; id <= 101; id++) chat.mergeConfirmedMessage(90, message(id))
    const refresh = chat.refreshLoadedMessageState(90)
    expect(requests[0].config.params.ids.split(',')).toHaveLength(100)
    requests[0].reply(Array.from({ length: 100 }, (_, index) => message(index + 1)))
    await flushPromises()
    expect(requests[1].config.params.ids).toBe('101')
    requests[1].reply([message(101)])
    await refresh
  })

  it('does not apply an old account recovery page or finish the new account recovery request', async () => {
    const old = chat.syncChatMessages(90).catch(error => error)
    auth.logoutCleanup(); chat.$reset()
    auth.setCurrentUser({ id: 2, username: 'Bob' }); auth.applyToken({ token: 'account-b', expiresIn: 3600 })
    const current = chat.syncChatMessages(90)
    const currentHandle = chat.syncRequests[90]
    requests[0].reply({ list: [message(101)], nextAfterMessageId: 101, hasMore: true })
    await old
    expect(chat.chatMessages[90]).toEqual([])
    expect(chat.syncRequests[90]).toBe(currentHandle)
    expect(requests).toHaveLength(2)
    requests[1].reply({ list: [message(102, { senderId: 2 })], nextAfterMessageId: 102, hasMore: false })
    await current
    expect(chat.chatMessages[90][0].isFromMe).toBe(true)
  })
})
