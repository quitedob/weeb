import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { sendMessageApi, getChatHistoryApi, recallMessageApi } from '@/api/modules/message'

export const useNewChatStore = defineStore('newChat', () => {
  // 状态
  const currentChatId = ref(null)
  const currentChatType = ref('user') // 'user' or 'group'
  const messages = ref(new Map()) // 使用Map存储不同聊天的消息
  const typingUsers = ref(new Set()) // 正在输入的用户
  const onlineUsers = ref(new Set()) // 在线用户
  const unreadCounts = ref(new Map()) // 未读消息计数
  const wsConnection = ref(null)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5

  // 计算属性
  const currentMessages = computed(() => {
    if (!currentChatId.value) return []
    return messages.value.get(`${currentChatType.value}-${currentChatId.value}`) || []
  })

  const unreadTotal = computed(() => {
    let total = 0
    unreadCounts.value.forEach(count => {
      total += count
    })
    return total
  })

  const isTyping = computed(() => {
    return typingUsers.value.size > 0
  })

  // WebSocket连接
  const connectWebSocket = () => {
    if (wsConnection.value?.readyState === WebSocket.OPEN) {
      return
    }

    try {
      const wsUrl = `ws://localhost:8081/ws?token=${localStorage.getItem('token')}`
      wsConnection.value = new WebSocket(wsUrl)

      wsConnection.value.onopen = () => {
        console.log('WebSocket连接已建立')
        reconnectAttempts.value = 0

        // 发送连接消息
        sendWebSocketMessage({
          type: 'connect',
          data: {
            userId: getCurrentUserId(),
            timestamp: new Date().toISOString()
          }
        })
      }

      wsConnection.value.onmessage = (event) => {
        handleWebSocketMessage(JSON.parse(event.data))
      }

      wsConnection.value.onclose = () => {
        console.log('WebSocket连接已关闭')
        attemptReconnect()
      }

      wsConnection.value.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }

    } catch (error) {
      console.error('WebSocket连接失败:', error)
      attemptReconnect()
    }
  }

  // 断线重连
  const attemptReconnect = () => {
    if (reconnectAttempts.value >= maxReconnectAttempts) {
      console.error('WebSocket重连失败，已达到最大重试次数')
      return
    }

    reconnectAttempts.value++
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000)

    console.log(`${delay}ms后尝试WebSocket重连 (${reconnectAttempts.value}/${maxReconnectAttempts})`)

    setTimeout(() => {
      connectWebSocket()
    }, delay)
  }

  // 发送WebSocket消息
  const sendWebSocketMessage = (message) => {
    if (wsConnection.value?.readyState === WebSocket.OPEN) {
      wsConnection.value.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket未连接，消息发送失败:', message)
    }
  }

  // 处理WebSocket消息
  const handleWebSocketMessage = (message) => {
    const { type, data } = message

    switch (type) {
      case 'new_message':
        handleNewMessage(data)
        break
      case 'message_status':
        updateMessageStatus(data.messageId, data.status)
        break
      case 'typing':
        handleTypingIndicator(data)
        break
      case 'online_status':
        updateOnlineStatus(data.userId, data.isOnline)
        break
      case 'message_recall':
        handleMessageRecall(data)
        break
      default:
        console.log('未知消息类型:', type)
    }
  }

  // 处理新消息
  const handleNewMessage = (messageData) => {
    const chatKey = `${messageData.recipientType}-${messageData.recipientId}`

    if (!messages.value.has(chatKey)) {
      messages.value.set(chatKey, [])
    }

    const chatMessages = messages.value.get(chatKey)
    chatMessages.push({
      ...messageData,
      timestamp: new Date(messageData.timestamp).toISOString()
    })

    // 如果不是当前聊天，增加未读计数
    if (currentChatId.value !== messageData.recipientId ||
        currentChatType.value !== messageData.recipientType) {
      const currentCount = unreadCounts.value.get(chatKey) || 0
      unreadCounts.value.set(chatKey, currentCount + 1)
    }
  }

  // 更新消息状态
  const updateMessageStatus = (messageId, status) => {
    messages.value.forEach(chatMessages => {
      const message = chatMessages.find(msg => msg.id === messageId)
      if (message) {
        message.status = status
      }
    })
  }

  // 处理打字指示器
  const handleTypingIndicator = (data) => {
    if (data.isTyping) {
      typingUsers.value.add(data.userId)
    } else {
      typingUsers.value.delete(data.userId)
    }

    // 3秒后自动移除打字状态
    setTimeout(() => {
      typingUsers.value.delete(data.userId)
    }, 3000)
  }

  // 更新在线状态
  const updateOnlineStatus = (userId, isOnline) => {
    if (isOnline) {
      onlineUsers.value.add(userId)
    } else {
      onlineUsers.value.delete(userId)
    }
  }

  // 处理消息撤回
  const handleMessageRecall = (data) => {
    messages.value.forEach(chatMessages => {
      const message = chatMessages.find(msg => msg.id === data.messageId)
      if (message) {
        message.isRecalled = true
        message.content = '消息已撤回'
        message.type = 'system'
      }
    })
  }

  // 切换聊天
  const setCurrentChat = (chatId, chatType = 'user') => {
    currentChatId.value = chatId
    currentChatType.value = chatType

    // 清除当前聊天的未读计数
    const chatKey = `${chatType}-${chatId}`
    unreadCounts.value.delete(chatKey)
  }

  // 发送消息
  const sendMessage = async (messageData) => {
    try {
      // 先添加到本地消息列表（乐观更新）
      const chatKey = `${messageData.recipientType}-${messageData.recipientId}`
      if (!messages.value.has(chatKey)) {
        messages.value.set(chatKey, [])
      }

      const tempMessage = {
        id: `temp-${Date.now()}`,
        senderId: getCurrentUserId(),
        content: messageData.content,
        type: messageData.type,
        timestamp: new Date().toISOString(),
        status: 'sending'
      }

      messages.value.get(chatKey).push(tempMessage)

      // 通过WebSocket发送
      sendWebSocketMessage({
        type: 'send_message',
        data: {
          ...messageData,
          tempId: tempMessage.id
        }
      })

      // 同时通过HTTP API发送（备用）
      const response = await sendMessageApi(messageData)

      if (response.success) {
        // 更新消息状态
        tempMessage.id = response.data.id
        tempMessage.status = 'sent'
      } else {
        throw new Error(response.message || '发送失败')
      }

      return response.data

    } catch (error) {
      console.error('发送消息失败:', error)
      throw error
    }
  }

  // 撤回消息
  const recallMessage = async (messageId) => {
    try {
      await recallMessageApi(messageId)

      sendWebSocketMessage({
        type: 'recall_message',
        data: { messageId }
      })

      return true
    } catch (error) {
      console.error('撤回消息失败:', error)
      throw error
    }
  }

  // 加载聊天记录
  const loadChatHistory = async (chatId, chatType, page = 1, pageSize = 50) => {
    try {
      const response = await getChatHistoryApi({
        recipientId: chatId,
        recipientType: chatType,
        page,
        pageSize
      })

      if (response.success) {
        const chatKey = `${chatType}-${chatId}`
        const historyMessages = response.data.list.map(msg => ({
          ...msg,
          timestamp: new Date(msg.timestamp).toISOString()
        }))

        if (page === 1) {
          // 第一页，直接替换
          messages.value.set(chatKey, historyMessages.reverse())
        } else {
          // 加载更多，添加到开头
          const existingMessages = messages.value.get(chatKey) || []
          messages.value.set(chatKey, [...historyMessages.reverse(), ...existingMessages])
        }

        return response.data
      }

    } catch (error) {
      console.error('加载聊天记录失败:', error)
      throw error
    }
  }

  // 发送打字状态
  const sendTypingIndicator = (isTyping) => {
    sendWebSocketMessage({
      type: 'typing',
      data: {
        chatId: currentChatId.value,
        chatType: currentChatType.value,
        isTyping,
        timestamp: new Date().toISOString()
      }
    })
  }

  // 清除聊天记录
  const clearChatHistory = (chatId, chatType) => {
    const chatKey = `${chatType}-${chatId}`
    messages.value.delete(chatKey)
    unreadCounts.value.delete(chatKey)
  }

  // 获取当前用户ID
  const getCurrentUserId = () => {
    // 从authStore或其他地方获取当前用户ID
    return localStorage.getItem('userId') || null
  }

  // 断开连接
  const disconnect = () => {
    if (wsConnection.value) {
      wsConnection.value.close()
      wsConnection.value = null
    }
  }

  // 重置store
  const reset = () => {
    disconnect()
    messages.value.clear()
    typingUsers.value.clear()
    onlineUsers.value.clear()
    unreadCounts.value.clear()
    currentChatId.value = null
    currentChatType.value = 'user'
    reconnectAttempts.value = 0
  }

  return {
    // 状态
    currentChatId,
    currentChatType,
    currentMessages,
    typingUsers,
    onlineUsers,
    unreadCounts,
    unreadTotal,
    isTyping,
    reconnectAttempts,

    // 方法
    connectWebSocket,
    disconnect,
    sendWebSocketMessage,
    setCurrentChat,
    sendMessage,
    recallMessage,
    loadChatHistory,
    sendTypingIndicator,
    clearChatHistory,
    reset,
    updateMessageStatus,
    updateOnlineStatus
  }
})