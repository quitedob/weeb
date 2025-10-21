// File path: /Vue/src/stores/chatStore.js
import { defineStore } from 'pinia';
import { useAuthStore } from './authStore';
import api from '@/api';

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
    connectionStatus: 'disconnected', // WebSocket connection status: 'disconnected', 'connecting', 'connected', 'error'
    websocket: null,         // WebSocket connection instance
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
    // WebSocket Connection Methods
    connectWebSocket() {
      const authStore = useAuthStore();
      if (!authStore.token) {
        console.warn('No auth token available for WebSocket connection');
        return;
      }

      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        console.log('WebSocket already connected');
        return;
      }

      this.connectionStatus = 'connecting';
      const wsUrl = `ws://localhost:8081/ws`; // WebSocket server on port 8081

      try {
        this.websocket = new WebSocket(wsUrl);

        this.websocket.onopen = () => {
          console.log('WebSocket connected');
          this.connectionStatus = 'connected';
          this.reconnectAttempts = 0;

          // Authenticate with WebSocket server
          this.sendWebSocketMessage({
            type: 'auth',
            data: { token: authStore.token }
          });

          // Start heartbeat
          this.startHeartbeat();
        };

        this.websocket.onmessage = (event) => {
          this.handleWebSocketMessage(event);
        };

        this.websocket.onclose = (event) => {
          console.log('WebSocket disconnected:', event);
          this.connectionStatus = 'disconnected';
          this.stopHeartbeat();

          // Attempt to reconnect
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
              this.reconnectAttempts++;
              console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
              this.connectWebSocket();
            }, 3000 * this.reconnectAttempts);
          }
        };

        this.websocket.onerror = (error) => {
          console.error('WebSocket error:', error);
          this.connectionStatus = 'error';
        };

      } catch (error) {
        console.error('Failed to create WebSocket connection:', error);
        this.connectionStatus = 'error';
      }
    },

    disconnectWebSocket() {
      if (this.websocket) {
        this.websocket.close();
        this.websocket = null;
      }
      this.stopHeartbeat();
      this.connectionStatus = 'disconnected';
    },

    sendWebSocketMessage(message) {
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        this.websocket.send(JSON.stringify(message));
      } else {
        console.warn('WebSocket not connected, message not sent:', message);
      }
    },

    handleWebSocketMessage(event) {
      try {
        const message = JSON.parse(event.data);

        switch (message.type) {
          case 'auth_success':
            console.log('WebSocket authentication successful');
            break;

          case 'chat_message':
            this.handleIncomingChatMessage(message);
            break;

          case 'heartbeat_response':
            // Heartbeat response handled automatically
            break;

          case 'error':
            console.error('WebSocket error:', message.data);
            break;

          case 'status_change':
            this.handleUserStatusChange(message);
            break;

          case 'message_sent':
            console.log('Message sent confirmation:', message);
            this.updateMessageStatus(message.data.messageId, 'sent', message.data.tempId);
            break;

          case 'message_delivered':
            console.log('Message delivered confirmation:', message);
            this.updateMessageStatus(message.data.messageId, 'delivered', message.data.tempId);
            break;

          case 'message_read':
            console.log('Message read confirmation:', message);
            this.updateMessageStatus(message.data.messageId, 'read', message.data.tempId);
            break;

          case 'typing':
            this.handleTypingIndicator(message);
            break;

          default:
            console.log('Unknown WebSocket message type:', message.type);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    },

    handleIncomingChatMessage(message) {
      const chatId = message.data.chatId || message.data.targetId;
      const isFromMe = message.data.fromUserId === useAuthStore().currentUser?.id;

      // Parse message content for file messages
      let parsedContent = message.data.content;
      let fileData = null;
      let displayContent = message.data.content;

      if (message.data.messageType === 2) {
        // File message - parse JSON content
        try {
          fileData = JSON.parse(message.data.content);
          displayContent = `[文件] ${fileData.fileName}`;
        } catch (error) {
          console.error('Failed to parse file message content:', error);
          displayContent = '[文件消息]';
        }
      }

      // Create standardized message object
      const standardizedMessage = {
        id: message.messageId || Date.now(),
        fromId: message.data.fromUserId,
        msgContent: displayContent,
        content: parsedContent,
        isRecalled: 0,
        messageType: message.data.messageType || 1,
        chatType: message.data.chatType,
        targetId: chatId,
        chatId: chatId,
        timestamp: message.data.timestamp || new Date(),
        isFromMe: isFromMe,
        msgType: message.data.messageType || 1,
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
        timestamp: message.data.timestamp || new Date(),
        fromUserId: message.data.fromUserId,
        messageType: message.data.messageType || 1
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
      this.stopHeartbeat();
      this.heartbeatInterval = setInterval(() => {
        this.sendWebSocketMessage({
          type: 'heartbeat',
          data: 'ping'
        });
      }, 30000); // Send heartbeat every 30 seconds
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
        const response = await api.message.getRecord({
          targetId: chatId,
          index: (page - 1) * batchSize,
          num: batchSize
        });

        if (response.code === 200 && response.data) {
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
        // This would need a corresponding API endpoint
        // const response = await api.message.getRecentChats();
        // if (response.code === 200 && response.data) {
        //   this.recentSessions = response.data;
        // }
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
          isTyping,
          userId: useAuthStore().currentUser?.id,
          timestamp: new Date()
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
