// File path: /Vue/src/stores/chatStore.js
import { defineStore } from 'pinia';
import { useAuthStore } from './authStore';
import api from '@/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { log } from '@/utils/logger';

export const useChatStore = defineStore('chat', {
  persist: {
    key: 'chat-store',
    paths: ['recentSessions', 'unreadCounts'],
    storage: localStorage,
  },
  state: () => ({
    activeChatSession: null, // Stores the currently active chat session object
                             // e.g., { id: 'group101', name: 'Tech Talk', type: 'GROUP', ... }
    chatMessages: {},        // Object to store messages per chatId: { chatId1: [msg1, msg2], chatId2: [...] }
    chatPagination: {},      // Pagination info per chat: { chatId1: { hasMore: true, page: 1 }, ... }
    recentSessions: [],      // List of recent chat sessions for a chat list panel
    unreadCounts: {},        // Unread message counts per chatId: { chatId1: 2, chatId2: 0 }
    connectionStatus: 'disconnected', // STOMP connection status: 'disconnected', 'connecting', 'connected', 'error'
    stompClient: null,       // STOMP client instance
    reconnectAttempts: 0,    // Number of reconnection attempts
    maxReconnectAttempts: 5, // Maximum reconnection attempts
    heartbeatInterval: null, // Heartbeat interval
    isTyping: {},            // Typing status per chatId: { chatId1: { userId1: true, userId2: false }, ... }
    onlineUsers: new Set(),  // Set of online user IDs
    messageBatchSize: 50,    // Number of messages to load per batch
  }),
  getters: {
    currentChatId: (state) => state.activeChatSession ? state.activeChatSession.id : null,
    currentChatType: (state) => state.activeChatSession ? state.activeChatSession.type : null,
    messagesForCurrentChat: (state) => {
      if (state.activeChatSession && state.chatMessages[state.activeChatSession.id]) {
        return state.chatMessages[state.activeChatSession.id];
      }
      return [];
    },
    // Optimized getter for virtual scrolling - returns visible messages only
    visibleMessagesForCurrentChat: (state) => (startIndex, visibleCount) => {
      if (!state.activeChatSession || !state.chatMessages[state.activeChatSession.id]) {
        return [];
      }
      const messages = state.chatMessages[state.activeChatSession.id];
      return messages.slice(startIndex, startIndex + visibleCount);
    },
    totalUnreadCount: (state) => {
      return Object.values(state.unreadCounts).reduce((total, count) => total + count, 0);
    },
    isConnected: (state) => state.connectionStatus === 'connected',
    isTypingInCurrentChat: (state) => {
      if (!state.activeChatSession) return false;
      const typingUsers = state.isTyping[state.activeChatSession.id];
      return typingUsers && Object.keys(typingUsers).some(userId => typingUsers[userId]);
    },
    // Pagination info for current chat
    currentChatPagination: (state) => {
      if (!state.activeChatSession) return null;
      return state.chatPagination[state.activeChatSession.id] || { hasMore: false, page: 0 };
    },
    // Check if more messages can be loaded for current chat
    canLoadMoreMessages: (state) => {
      if (!state.activeChatSession) return false;
      const pagination = state.chatPagination[state.activeChatSession.id];
      return pagination ? pagination.hasMore : false;
    }
  },
  actions: {
    // STOMP WebSocket Connection Methods
    connectWebSocket() {
      const authStore = useAuthStore();
      if (!authStore.token) {
        log.warn('No auth token available for STOMP connection');
        return;
      }

      if (this.stompClient && this.stompClient.connected) {
        log.debug('STOMP already connected');
        return;
      }

      this.connectionStatus = 'connecting';

      try {
        // 获取WebSocket URL（根据环境配置）
        const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
        
        // Create STOMP client with SockJS fallback
        this.stompClient = new Client({
          webSocketFactory: () => new SockJS(wsUrl),
          connectHeaders: {
            'Authorization': `Bearer ${authStore.token}`
          },
          debug: (str) => {
            log.debug('STOMP Debug:', str);
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        // Connection successful
        this.stompClient.onConnect = (frame) => {
          log.info('STOMP connected:', frame);
          this.connectionStatus = 'connected';
          this.reconnectAttempts = 0;

          // Subscribe to user-specific queues
          this.subscribeToQueues();

          // Start heartbeat
          this.startHeartbeat();
        };

        // Connection error
        this.stompClient.onStompError = (frame) => {
          console.error('STOMP error:', frame);
          this.connectionStatus = 'error';

          // Attempt to reconnect
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
              this.reconnectAttempts++;
              console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
              this.connectWebSocket();
            }, 3000 * this.reconnectAttempts);
          }
        };

        // Connection lost
        this.stompClient.onDisconnect = () => {
          console.log('STOMP disconnected');
          this.connectionStatus = 'disconnected';
          this.stopHeartbeat();
        };

        // Connect to STOMP server
        this.stompClient.activate();

      } catch (error) {
        console.error('Failed to create STOMP connection:', error);
        this.connectionStatus = 'error';
      }
    },

    disconnectWebSocket() {
      if (this.stompClient) {
        this.stompClient.deactivate();
        this.stompClient = null;
      }
      this.stopHeartbeat();
      this.connectionStatus = 'disconnected';
    },

    subscribeToQueues() {
      if (!this.stompClient || !this.stompClient.connected) return;

      const authStore = useAuthStore();
      const username = authStore.currentUser?.username;

      if (!username) return;

      // Subscribe to private messages
      this.stompClient.subscribe(`/user/${username}/queue/private`, (message) => {
        const parsedMessage = JSON.parse(message.body);
        this.handleIncomingChatMessage(parsedMessage);
      });

      // Subscribe to error messages
      this.stompClient.subscribe(`/user/${username}/queue/errors`, (message) => {
        const errorMessage = JSON.parse(message.body);
        console.error('STOMP error message:', errorMessage);
      });

      // Subscribe to general chat topics (optional)
      this.stompClient.subscribe('/topic/chat/*', (message) => {
        const parsedMessage = JSON.parse(message.body);
        if (parsedMessage.type === 'join' || parsedMessage.type === 'leave') {
          console.log('Chat room status:', parsedMessage);
        }
      });
    },

    sendWebSocketMessage(message) {
      if (this.stompClient && this.stompClient.connected) {
        // Map message types to STOMP destinations
        let destination;

        switch (message.type) {
          case 'chat':
            if (message.data.chatType === 'PRIVATE') {
              destination = '/app/chat/private';
            } else {
              destination = `/app/chat/sendMessage/${message.data.targetId}`;
            }
            break;
          case 'typing':
            destination = `/app/chat/typing/${message.data.chatId}`;
            break;
          case 'heartbeat':
            destination = '/app/chat/heartbeat';
            break;
          default:
            console.warn('Unknown message type for STOMP:', message.type);
            return;
        }

        this.stompClient.publish({
          destination: destination,
          body: JSON.stringify(message.data)
        });
      } else {
        console.warn('STOMP not connected, message not sent:', message);
      }
    },

    handleIncomingChatMessage(message) {
      // Handle both Spring WebSocket format and old format
      const chatId = message.roomId || message.data?.chatId || message.data?.targetId || message.targetId;
      const authStore = useAuthStore();
      const isFromMe = (message.fromId || message.data?.fromUserId) === authStore.currentUser?.id;

      // Parse message content for file messages
      let content, displayContent, fileData;

      if (message.content !== undefined) {
        // Spring WebSocket format
        content = message.content;
        displayContent = message.content;
      } else if (message.data?.content !== undefined) {
        // Old format
        content = message.data.content;
        displayContent = message.data.content;
      }

      if (message.type === 2 || message.data?.messageType === 2) {
        // File message - parse JSON content
        try {
          fileData = JSON.parse(content);
          displayContent = `[文件] ${fileData.fileName}`;
        } catch (error) {
          console.error('Failed to parse file message content:', error);
          displayContent = '[文件消息]';
        }
      }

      // Create standardized message object
      const standardizedMessage = {
        id: message.id || message.messageId || Date.now(),
        fromId: message.fromId || message.data?.fromUserId,
        fromName: message.fromName || message.data?.fromName,
        msgContent: displayContent,
        content: content,
        isRecalled: 0,
        messageType: message.type || message.data?.messageType || 1,
        chatType: message.type === 'private' ? 'PRIVATE' : (message.data?.chatType || 'GROUP'),
        targetId: chatId,
        chatId: chatId,
        timestamp: message.timestamp || message.data?.timestamp || new Date(),
        isFromMe: isFromMe,
        msgType: message.type || message.data?.messageType || 1,
        fileData: fileData // Store parsed file data
      };

      // Add message to chat
      this.addMessage(chatId, standardizedMessage);

      // Update unread counts if not from current user
      if (!isFromMe && chatId !== this.currentChatId) {
        this.incrementUnreadCount(chatId);
      }

      // Update recent sessions
      this.updateRecentSession(chatId, {
        content: displayContent,
        timestamp: standardizedMessage.timestamp,
        fromUserId: standardizedMessage.fromId,
        messageType: standardizedMessage.messageType
      });
    },

    handleUserStatusChange(message) {
      const { userId, status } = message.data;
      if (status === 1) {
        this.onlineUsers.add(userId);
      } else {
        this.onlineUsers.delete(userId);
      }
    },

    startHeartbeat() {
      // STOMP handles heartbeat automatically with the configured settings
      // No need for manual heartbeat with STOMP client
      console.log('STOMP heartbeat enabled automatically');
    },

    stopHeartbeat() {
      if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval);
        this.heartbeatInterval = null;
      }
    },

    // Chat Methods
    setActiveChat(session) {
      this.activeChatSession = session;
      // Mark messages as read when opening a chat
      if (session && this.unreadCounts[session.id]) {
        this.markAsRead(session.id);
      }
      console.log('ChatStore: Active chat set to', session);
    },

    clearActiveChat() {
      this.activeChatSession = null;
    },

    addMessage(chatId, message) {
      if (!this.chatMessages[chatId]) {
        this.chatMessages[chatId] = [];
      }
      this.chatMessages[chatId].push(message);
    },

    setMessages(chatId, messages) {
      this.chatMessages[chatId] = messages;
    },

    async sendMessage(content, targetId, chatType = 'PRIVATE', messageType = 1) {
      if (!content || !targetId) {
        throw new Error('Content and targetId are required');
      }

      const message = {
        type: 'chat',
        data: {
          content,
          targetId,
          chatType,
          messageType,
          chatId: targetId
        }
      };

      this.sendWebSocketMessage(message);
    },

    async fetchMessagesForChat(chatId, page = 1, limit = null) {
      try {
        const batchSize = limit || this.messageBatchSize;
        // 使用新的chat API
        const response = await api.chat.getChatMessages(chatId, {
          page,
          size: batchSize
        });

        if (response.code === 0 && response.data) {
          const hasMore = response.data.length === batchSize;

          // Update pagination info
          this.chatPagination[chatId] = {
            hasMore,
            page,
            total: response.data.length
          };

          if (page === 1) {
            this.setMessages(chatId, response.data);
          } else {
            // Append messages for pagination
            const existingMessages = this.chatMessages[chatId] || [];
            this.setMessages(chatId, [...response.data, ...existingMessages]);
          }
        }
      } catch (error) {
        console.error(`Failed to fetch messages for chat ${chatId}:`, error);
        throw error;
      }
    },

    // Load more messages for current chat
    async loadMoreMessages() {
      if (!this.activeChatSession) return;

      const chatId = this.activeChatSession.id;
      const pagination = this.chatPagination[chatId] || { page: 0 };
      const nextPage = pagination.page + 1;

      await this.fetchMessagesForChat(chatId, nextPage);
    },

    // Clear messages for a chat to free memory
    clearChatMessages(chatId) {
      if (this.chatMessages[chatId]) {
        delete this.chatMessages[chatId];
        delete this.chatPagination[chatId];
      }
    },

    async fetchRecentChats() {
      try {
        // 使用新的chat API获取聊天列表
        const response = await api.chat.getChatList();
        if (response.code === 0 && response.data) {
          this.recentSessions = response.data;
        }
      } catch (error) {
        console.error('Failed to fetch recent chats:', error);
      }
    },

    updateRecentSession(chatId, lastMessage) {
      const existingIndex = this.recentSessions.findIndex(session => session.id === chatId);
      const sessionData = {
        id: chatId,
        lastMessage: lastMessage.content,
        lastMessageTime: new Date(),
        unreadCount: this.unreadCounts[chatId] || 0
      };

      if (existingIndex >= 0) {
        // Update existing session and move to top
        this.recentSessions.splice(existingIndex, 1);
        this.recentSessions.unshift(sessionData);
      } else {
        // Add new session at top
        this.recentSessions.unshift(sessionData);
      }
    },

    incrementUnreadCount(chatId) {
      this.unreadCounts[chatId] = (this.unreadCounts[chatId] || 0) + 1;
    },

    markAsRead(chatId) {
      this.unreadCounts[chatId] = 0;
    },

    clearChatMessages(chatId) {
      if (this.chatMessages[chatId]) {
        delete this.chatMessages[chatId];
      }
    },

    // Typing indicators
    setTyping(chatId, userId, isTyping) {
      if (!this.isTyping[chatId]) {
        this.isTyping[chatId] = {};
      }
      this.isTyping[chatId][userId] = isTyping;
    },

    sendTypingIndicator(chatId, isTyping) {
      this.sendWebSocketMessage({
        type: 'typing',
        data: {
          chatId,
          action: isTyping ? 'start' : 'stop'
        }
      });
    },

    // Handle typing indicator from WebSocket
    handleTypingIndicator(message) {
      const { chatId, isTyping, userId } = message.data;

      // Update typing status for the specific chat
      if (!this.isTyping[chatId]) {
        this.isTyping[chatId] = {};
      }

      // Set or remove typing status for the user
      if (isTyping) {
        this.isTyping[chatId][userId] = true;

        // Auto-remove typing indicator after 3 seconds
        setTimeout(() => {
          if (this.isTyping[chatId] && this.isTyping[chatId][userId]) {
            this.isTyping[chatId][userId] = false;
          }
        }, 3000);
      } else {
        delete this.isTyping[chatId][userId];
      }
    },

    // Update message status
    updateMessageStatus(messageId, status, tempId = null) {
      if (!messageId && !tempId) return;

      // Search for the message in all chat messages
      Object.keys(this.chatMessages).forEach(chatId => {
        const messages = this.chatMessages[chatId];
        let messageIndex = -1;

        // First try to find by temporary ID (for new messages)
        if (tempId) {
          messageIndex = messages.findIndex(msg => msg.tempId === tempId);
        }

        // If not found by tempId, try by real ID
        if (messageIndex === -1 && messageId) {
          messageIndex = messages.findIndex(msg => msg.id === messageId);
        }

        if (messageIndex !== -1) {
          messages[messageIndex].status = status;
          // Update real ID if available
          if (messageId && !messages[messageIndex].id) {
            messages[messageIndex].id = messageId;
          }
          // Remove temporary ID after successful association
          if (tempId && messages[messageIndex].tempId === tempId) {
            delete messages[messageIndex].tempId;
          }
        }
      });
    }
  },
});
