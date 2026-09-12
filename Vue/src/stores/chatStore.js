// File path: /Vue/src/stores/chatStore.js
import { defineStore } from 'pinia';
import { useAuthStore } from './authStore';
import api from '@/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { log } from '@/utils/logger';
import { 
  MESSAGE_STATUS, 
  normalizeMessage, 
  normalizeMessages,
  updateMessageStatus,
  isMessageFailed
} from '@/utils/messageStatus';
import bugReporter from '@/utils/bugReporter';
import { captureSession, isCurrentSession } from '@/utils/session';
import { socketUrl } from '@/utils/serviceUrls';
import { canonicalMessage, mergeMessage, sameId, isPersistedId, compareIds, highestMessageId } from '@/utils/chatProtocol';

export const useChatStore = defineStore('chat', {
  persist: {
    key: 'chat-store',
    paths: ['recentSessions', 'unreadCounts'],
    storage: localStorage,
  },
  state: () => ({
    activeChatSession: null, // Stores currently active chat session object
                             // e.g., { id: 'group101', name: 'Tech Talk', type: 'GROUP', ... }
    chatMessages: {},        // Object to store messages per chatId: { chatId1: [msg1, msg2], chatId2: [...] }
    cacheEntries: {},
    cacheClock: 0,
    historyRequests: {},
    recentMessageIds: {},
    newerIncomingIds: {},
    maxCachedChats: 50,
    maxCachedMessages: 200,
    syncCursors: {},
    syncRequests: {},
    readCursors: {},
    readRequests: {},
    reactionSnapshots: {},
    chatPagination: {},      // Pagination info per chat: { chatId1: { hasMore: true, page: 1 }, ... }
    recentSessions: [],      // List of recent chat sessions for a chat list panel
    unreadCounts: {},        // Unread message counts per chatId: { chatId1: 2, chatId2: 0 }
    unreadCountMap: {},      // ✅ 新增：未读计数映射 { chatId: unreadCount }
    connectionStatus: 'disconnected', // STOMP connection status: 'disconnected', 'connecting', 'connected', 'error'
    notificationSubscriptionReady: false,
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
      const session = captureSession();

      // authStore使用accessToken，不是token
      const token = authStore.accessToken;
      if (this.stompClient?.active && ['connected', 'connecting'].includes(this.connectionStatus)) return;

      console.log('🔌 尝试连接WebSocket...');
      console.log('Token存在:', !!token);
      console.log('Token长度:', token ? token.length : 0);

      if (!token) {
        console.error('❌ 无法连接WebSocket: 缺少认证token');
        console.error('请先登录！');
        log.warn('No auth token available for STOMP connection');
        this.connectionStatus = 'error';
        return;
      }

      // Clean up existing connection
      this.stopHeartbeat();
      if (this.stompClient) {
        try {
          this.stompClient.deactivate();
        } catch (error) {
          console.warn('清理现有连接时出错:', error);
        }
        this.stompClient = null;
      }

      this.connectionStatus = 'connecting';
      this.notificationSubscriptionReady = false;
      console.log('⏳ WebSocket连接状态: connecting');

      try {
        // 获取WebSocket URL（根据环境配置）
        const wsUrl = socketUrl();
        console.log('🌐 WebSocket URL:', wsUrl);

        // ✅ 修复2：创建STOMP客户端（增强认证）
        this.stompClient = new Client({
          webSocketFactory: () => {
            console.log('🏭 创建SockJS连接...');
            console.log('🔗 连接URL:', wsUrl);
            console.log('🔑 Token长度:', token ? token.length : 0);

            const sockJS = new SockJS(wsUrl);

            // ✅ 修复2：增强错误处理
            sockJS.onclose = (event) => {
              if (this.stompClient !== client || !isCurrentSession(session)) return;
              console.log('🔌 SockJS连接关闭:', event.code, event.reason);
              console.log('🔌 关闭详情:', {
                code: event.code,
                reason: event.reason,
                wasClean: event.wasClean,
                timestamp: new Date().toISOString()
              });
              
              // ✅ 修复2：根据关闭码判断是否需要重连
              if (event.code === 1006) {
                console.warn('⚠️ 连接异常关闭，可能是认证失败或网络问题');
              } else if (event.code === 1008) {
                console.error('❌ 连接被服务器拒绝，可能是认证失败');
              }
              
              if (this.connectionStatus === 'connecting') {
                this.connectionStatus = 'error';
              }
            };

            sockJS.onerror = (error) => {
              if (this.stompClient !== client || !isCurrentSession(session)) return;
              console.error('❌ SockJS连接错误:', error);
              console.error('❌ 错误详情:', {
                type: error.type,
                target: error.target,
                timestamp: new Date().toISOString()
              });
              this.connectionStatus = 'error';
            };

            return sockJS;
          },
          connectHeaders: {
            'Authorization': `Bearer ${token}`,
            'X-Client-Type': 'web',
            'X-Client-Version': '1.0.0'
          },
          // STOMP frames may contain Authorization headers and private messages.
          debug: () => {},
          reconnectDelay: 3000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          connectionTimeout: 15000,
        });
        const client = this.stompClient;

        this.stompClient.beforeConnect = async () => {
          if (this.stompClient !== client || !isCurrentSession(session)) {
            await client.deactivate();
            return;
          }
          if (!authStore.accessToken) {
            this.disconnectWebSocket();
            return;
          }
          client.connectHeaders.Authorization = `Bearer ${authStore.accessToken}`;
        };

        // Connection successful
        this.stompClient.onConnect = (frame) => {
          if (this.stompClient !== client || !isCurrentSession(session)) return;
          console.log('✅ WebSocket连接成功!');
          console.log('Frame:', frame);
          log.info('STOMP connected:', frame);
          this.connectionStatus = 'connected';
          this.reconnectAttempts = 0;

          try {
            // Subscribe to user-specific queues
            this.subscribeToQueues();

            // Start heartbeat
            this.startHeartbeat();

            // ✅ 拉取离线消息
            this.fetchOfflineMessages().catch(error => {
              console.error('拉取离线消息失败:', error);
            });
          } catch (error) {
            console.error('连接后处理失败:', error);
            // 不中断连接，只记录错误
          }
        };

        // Connection error
        this.stompClient.onStompError = (frame) => {
          if (this.stompClient !== client || !isCurrentSession(session)) return;
          this.stopHeartbeat();
          console.error('❌ WebSocket STOMP错误:', frame);
          console.error('错误详情:', frame.headers);
          console.error('错误消息:', frame.body);
          this.connectionStatus = 'error';

          // ✅ 指数退避重连策略
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            // 计算延迟时间：1s, 2s, 4s, 8s, 16s, 最大30s
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

            setTimeout(() => {
              if (this.stompClient !== client || !isCurrentSession(session)) return;
              if (this.connectionStatus === 'error') { // 只有在错误状态时才重连
                this.reconnectAttempts++;
                console.log(`🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts}), 延迟: ${delay}ms`);
                this.connectWebSocket();
              }
            }, delay);
          } else {
            console.error('❌ 达到最大重连次数，停止重连');
          }
        };

        // Connection lost
        this.stompClient.onDisconnect = () => {
          if (this.stompClient !== client || !isCurrentSession(session)) return;
          console.log('⚠️ WebSocket断开连接');
          this.connectionStatus = 'disconnected';
          this.stopHeartbeat();
        };

        // Web Socket error
        this.stompClient.onWebSocketError = (error) => {
          if (this.stompClient !== client || !isCurrentSession(session)) return;
          this.stopHeartbeat();
          console.error('❌ WebSocket底层错误:', error);
          this.connectionStatus = 'error';
        };

        this.stompClient.onWebSocketClose = () => {
          if (this.stompClient !== client || !isCurrentSession(session)) return;
          this.stopHeartbeat();
          this.connectionStatus = 'disconnected';
        };

        // Connect to STOMP server
        console.log('🚀 激活STOMP客户端...');

        // 直接激活STOMP客户端
        try {
          this.stompClient.activate();
        } catch (error) {
          console.error('❌ 激活STOMP客户端失败:', error);
          this.connectionStatus = 'error';
        }

      } catch (error) {
        console.error('❌ 创建STOMP连接失败:', error);
        console.error('错误堆栈:', error.stack);
        this.connectionStatus = 'error';
      }
    },

    disconnectWebSocket() {
      this.stopHeartbeat();
      try {
        if (this.stompClient) {
          console.log('🔌 断开WebSocket连接...');
          this.stompClient.deactivate();
          this.stompClient = null;
        }
        this.stopHeartbeat();
        this.connectionStatus = 'disconnected';
        this.reconnectAttempts = 0; // Reset reconnect attempts
        console.log('✅ WebSocket已断开连接');
      } catch (error) {
        console.error('❌ 断开WebSocket连接失败:', error);
        // 强制重置状态
        this.stompClient = null;
        this.connectionStatus = 'disconnected';
        this.reconnectAttempts = 0;
      }
    },

    subscribeToQueues() {
      if (!this.stompClient || !this.stompClient.connected) {
        console.warn('⚠️ STOMP客户端未连接，无法订阅队列');
        return;
      }

      const client = this.stompClient;
      const session = captureSession();
      const isCurrent = () => this.stompClient === client && isCurrentSession(session);
      const subscribe = (destination, callback) => client.subscribe(destination, message => {
        if (isCurrent()) callback(message);
      });
      try {
        // ✅ 订阅私聊消息
        subscribe(`/user/queue/private`, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            console.log('📨 收到私聊消息:', parsedMessage);
            this.handleIncomingChatMessage(parsedMessage);
          } catch (error) {
            console.error('❌ 处理私聊消息失败:', error, message.body);
          }
        });

        // ✅ 订阅聊天列表更新
        subscribe(`/user/queue/chat-list-update`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('📋 聊天列表已更新:', data);
            this.handleChatListUpdate(data);
          } catch (error) {
            console.error('❌ 处理聊天列表更新失败:', error, message.body);
          }
        });

        // ✅ 订阅消息状态更新
        subscribe(`/user/queue/message-status`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('✓ 消息状态更新:', data);
            this.handleMessageStatusUpdate(data);
          } catch (error) {
            console.error('❌ 处理消息状态更新失败:', error, message.body);
          }
        });

        // ✅ 订阅已读回执
        subscribe(`/user/queue/read-receipt`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('👁️ 收到已读回执:', data);
            this.handleReadReceipt(data);
          } catch (error) {
            console.error('❌ 处理已读回执失败:', error, message.body);
          }
        });

        // ✅ 订阅群组成员变更事件
        subscribe(`/user/queue/group-member-change`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('👥 收到群组成员变更事件:', data);
            this.handleGroupMemberChange(data);
          } catch (error) {
            console.error('❌ 处理群组成员变更失败:', error, message.body);
          }
        });

        // ✅ 订阅群组信息变更事件
        subscribe(`/user/queue/group-info-change`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('ℹ️ 收到群组信息变更事件:', data);
            this.handleGroupInfoChange(data);
          } catch (error) {
            console.error('❌ 处理群组信息变更失败:', error, message.body);
          }
        });

        // ✅ 订阅消息反应变更事件
        subscribe(`/user/queue/reaction-change`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('😊 收到消息反应变更事件:', data);
            this.handleReactionChange(data);
          } catch (error) {
            console.error('❌ 处理消息反应变更失败:', error, message.body);
          }
        });

        // Subscribe to error messages
        subscribe(`/user/queue/errors`, (message) => {
          try {
            const errorMessage = JSON.parse(message.body);
            console.error('❌ STOMP错误消息:', errorMessage);

            // 如果有clientMessageId，更新对应消息状态为失败
            if (errorMessage.clientMessageId) {
              this.updateMessageStatus(null, MESSAGE_STATUS.FAILED, errorMessage.clientMessageId);
            }
          } catch (error) {
            console.error('❌ 处理错误消息失败:', error, message.body);
          }
        });

        subscribe('/user/queue/notifications', message => {
          try {
            const notification = JSON.parse(message.body);
            import('./notificationStore').then(({ useNotificationStore }) => {
              if (!isCurrent()) return;
              const store = useNotificationStore();
              store.addNotification(notification);
            });
          } catch (error) { console.error('处理通知失败:', error); }
        });
        this.notificationSubscriptionReady = true;
        subscribe('/user/queue/contacts', message => {
          try {
            window.dispatchEvent(new CustomEvent('contact-notification', { detail: JSON.parse(message.body) }));
          } catch (error) { console.error('处理联系人通知失败:', error); }
        });

        console.log('✅ 已订阅所有WebSocket队列');
      } catch (error) {
        console.error('❌ 订阅WebSocket队列失败:', error);
      }
    },

    sendWebSocketMessage(message) {
      if (this.stompClient && this.stompClient.connected) {
        // Map message types to STOMP destinations
        let destination;
        let payload = { ...message.data };

        switch (message.type) {
          case 'chat':
            if (message.data.chatType === 'PRIVATE') {
              destination = '/app/chat/private';
              // 后端期望targetUser（用户名），但我们通常只有targetId
              // 保持targetId用于后端查找用户，后端会处理转换
              if (!payload.targetUser && payload.targetId) {
                // 后端会根据targetId查找用户
                payload.targetId = String(payload.targetId);
              }
            } else {
              // 群聊消息 - 使用正确的STOMP端点
              destination = '/app/chat.sendMessage';
              payload.roomId = `group_${message.data.targetId}`;
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
          body: JSON.stringify(payload)
        });
      } else {
        console.warn('STOMP not connected, message not sent:', message);
      }
    },

    handleIncomingChatMessage(message) {
      const normalized = canonicalMessage(message, null, useAuthStore().currentUser?.id);
      if (!normalized || !isPersistedId(normalized.id)) return;
      const chatId = String(normalized.sharedChatId);
      const wasNew = this.mergeConfirmedMessage(chatId, normalized);
      if (!wasNew || compareIds(normalized.id, this.syncCursors[chatId] ?? 0) <= 0) return;
      if (!normalized.isFromMe && this.chatPagination[chatId]?.hasNewer) {
        const ids = (this.newerIncomingIds[chatId] ||= []);
        ids.push(String(normalized.id));
        if (ids.length > this.maxCachedMessages) ids.shift();
      }
      this.updateUnreadOnNewMessage(chatId, normalized.isFromMe);
      this.updateRecentSession(chatId, { content: normalized.msgContent, timestamp: normalized.timestamp });
      // Reading is acknowledged only after ChatPage renders the confirmed message.
    },

    /**
     * ✅ 新增：处理聊天列表更新
     */
    handleChatListUpdate(chatList) {
      const id = chatList.sharedChatId ?? chatList.shared_chat_id ?? chatList.id;
      const existingIndex = this.recentSessions.findIndex(session => sameId(session.sharedChatId ?? session.id, id));
      const previous = existingIndex < 0 ? {} : this.recentSessions.splice(existingIndex, 1)[0];
      this.recentSessions.unshift({ ...previous, ...chatList, id, sharedChatId: id,
        lastMessageTime: chatList.updateTime ?? chatList.lastMessageTime,
        unreadCount: chatList.unreadCount ?? previous.unreadCount ?? 0 });
    },

    /**
     * ✅ 新增：处理消息状态更新
     */
    handleMessageStatusUpdate(data) {
      console.log('✓ 处理消息状态更新:', data);
      
      if (data.messageId) {
        this.updateMessageStatus(data.messageId, data.status, data.clientMessageId);
      }
    },

    /**
     * ✅ 新增：处理已读回执
     */
    handleReadReceipt(data) {
      const chatId = data.sharedChatId ?? data.chatId;
      const boundary = data.lastReadMessageId ?? data.messageId;
      if (chatId == null || !isPersistedId(boundary) || data.readerId == null) return;
      this.touchChat(chatId);
      if (!this.cacheEntries[chatId]) return;
      const readerKey = `${chatId}:${data.readerId}`;
      if (compareIds(boundary, this.readCursors[readerKey] ?? 0) <= 0) return;
      this.readCursors[readerKey] = String(boundary);
      if (sameId(data.readerId, useAuthStore().currentUser?.id)) {
        if (Number.isFinite(data.unreadCount)) {
          const knownNewer = new Set([
            ...(this.chatMessages[chatId] || []).filter(message => !message.isFromMe && !message.isRecalled
              && isPersistedId(message.id) && compareIds(message.id, boundary) > 0).map(message => String(message.id)),
            ...(this.newerIncomingIds[chatId] || []).filter(id => compareIds(id, boundary) > 0)
          ]).size;
          const unreadCount = Math.max(data.unreadCount, knownNewer);
          this.unreadCounts[chatId] = unreadCount;
          this.unreadCountMap[chatId] = unreadCount;
          this.newerIncomingIds[chatId] = (this.newerIncomingIds[chatId] || []).filter(id => compareIds(id, boundary) > 0);
        }
        return;
      }
      const session = sameId(this.currentChatId, chatId) ? this.activeChatSession
        : this.recentSessions.find(item => sameId(item.sharedChatId ?? item.id, chatId));
      for (const message of this.chatMessages[chatId] || []) {
        if (!message.isFromMe || !isPersistedId(message.id) || compareIds(message.id, boundary) > 0) continue;
        if (session?.type === 'GROUP' || session?.chatType === 'GROUP') {
          // One group reader is not a receipt from every member.
          message.readBy = [...new Set([...(message.readBy || []), String(data.readerId)])];
        } else if (session?.type === 'PRIVATE' || session?.chatType === 'PRIVATE') {
          message.status = MESSAGE_STATUS.READ;
        }
      }
    },

    /**
     * ✅ 新增：查找临时消息
     */
    findMessageByTempId(chatId, tempId) {
      if (!this.chatMessages[chatId] || !tempId) return null;
      
      return this.chatMessages[chatId].find(msg => 
        msg.tempId === tempId || msg.clientMessageId === tempId
      );
    },

    /**
     * ✅ 消息去重检查
     */
    isDuplicateMessage(chatId, messageId, clientMessageId) {
      if (!this.chatMessages[chatId]) return false;
      
      const messages = this.chatMessages[chatId];
      
      // 检查消息ID是否已存在
      if (messageId && messages.some(msg => msg.id === messageId)) {
        console.log('⚠️ 检测到重复消息ID:', messageId);
        return true;
      }
      
      // 检查客户端消息ID是否已存在
      if (clientMessageId && messages.some(msg => 
        msg.clientMessageId === clientMessageId || msg.tempId === clientMessageId
      )) {
        console.log('⚠️ 检测到重复客户端消息ID:', clientMessageId);
        return true;
      }
      
      return false;
    },

    /**
     * ✅ 发送已读回执
     */
    async sendReadReceipt(chatId, messageId) {
      return this.markChatAsRead(chatId, messageId);
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
      const client = this.stompClient;
      const session = captureSession();
      if (!client?.connected || !useAuthStore().accessToken) return;
      // The Redis session lease is refreshed by the authenticated application endpoint.
      // STOMP transport heartbeats do not invoke that endpoint.
      const interval = setInterval(() => {
        if (this.stompClient !== client || !client.connected || !isCurrentSession(session) || !useAuthStore().accessToken) {
          clearInterval(interval);
          if (this.heartbeatInterval === interval) this.heartbeatInterval = null;
          return;
        }
        try {
          client.publish({ destination: '/app/chat/heartbeat', body: '{}' });
        } catch (error) {
          log.warn('Unable to send the application heartbeat');
        }
      }, 30000);
      this.heartbeatInterval = interval;
    },

    stopHeartbeat() {
      if (this.heartbeatInterval !== null) {
        clearInterval(this.heartbeatInterval);
        this.heartbeatInterval = null;
      }
    },

    // Chat Methods
    setActiveChat(session) {
      console.log('🎯 ChatStore设置活跃聊天:', session);

      if (!session) {
        console.log('❌ 会话对象为空，清空活跃聊天');
        this.activeChatSession = null;
        return;
      }

      // ✅ 修复：优先使用sharedChatId作为标识，并确保类型正确
      const sharedChatId = session.sharedChatId || session.shared_chat_id;
      
      if (!sharedChatId) {
        console.error('🐛 BUG REPORT: Session missing sharedChatId', {
          session: session,
          availableFields: Object.keys(session),
          timestamp: new Date().toISOString()
        });
      }

      const normalizedSession = {
        ...session,
        id: Number(sharedChatId || session.id || session.chatId || session.chat_id), // ✅ 使用Number类型
        sharedChatId: Number(sharedChatId || session.id), // ✅ 确保sharedChatId是Number
        type: session.type || 'PRIVATE',
        name: session.name || session.groupName || session.targetInfo || '未知聊天',
        targetId: session.targetId || session.target_user_id
      };

      this.activeChatSession = normalizedSession;
      this.touchChat(normalizedSession.id);

      console.log('✅ ChatStore: 活跃聊天已设置:', {
        id: normalizedSession.id,
        sharedChatId: normalizedSession.sharedChatId,
        type: normalizedSession.type,
        name: normalizedSession.name
      });

      // ✅ 修复：使用sharedChatId标记已读

    },

    clearActiveChat() {
      this.activeChatSession = null;
    },

    addMessage(chatId, message) {
      // ✅ 修复3：统一使用sharedChatId作为key
      const normalizedChatId = String(message.sharedChatId || chatId);
      
      if (!this.chatMessages[normalizedChatId]) {
        this.chatMessages[normalizedChatId] = [];
      }
      // 确保消息包含status字段
      const normalizedMsg = normalizeMessage(message);
      this.chatMessages[normalizedChatId].push(normalizedMsg);
      this.touchChat(normalizedChatId);
      this.trimChatWindow(normalizedChatId);
    },

    setMessages(chatId, messages) {
      this.chatMessages[chatId] = messages;
      this.touchChat(chatId);
      this.trimChatWindow(chatId);
    },

    async sendMessage(content, targetId, chatType = 'PRIVATE', messageType = 1, clientMessageId = null) {
      const sharedChatId = this.activeChatSession?.sharedChatId ?? this.activeChatSession?.id;
      if (!content || !sharedChatId) throw new Error('Content and sharedChatId are required');
      const id = clientMessageId || `temp_${globalThis.crypto?.randomUUID?.() || `${Date.now()}_${Math.random().toString(36).slice(2)}`}`;
      const payload = { content, targetId: this.activeChatSession?.targetId ?? targetId,
        chatType, messageType, sharedChatId, clientMessageId: id };
      const user = useAuthStore().currentUser;
      if (!this.findMessageByTempId(sharedChatId, id)) this.addMessage(sharedChatId, {
        id, tempId: id, clientMessageId: id, fromId: user?.id, fromName: user?.username,
        msgContent: typeof content === 'object' ? content.content : content, content,
        chatId: sharedChatId, sharedChatId, targetId: payload.targetId, chatType, messageType,
        timestamp: new Date().toISOString(), isFromMe: true, status: MESSAGE_STATUS.SENDING,
        reactions: [], sendPayload: payload
      });
      return this.deliverMessage(payload);
    },

    async retryMessage(message) {
      if (message.status !== MESSAGE_STATUS.FAILED || !message.sendPayload || !message.isFromMe) return;
      return this.deliverMessage(message.sendPayload);
    },

    async deliverMessage(payload) {
      const session = captureSession();
      this.updateMessageStatus(null, MESSAGE_STATUS.SENDING, payload.clientMessageId);
      try {
        if (this.stompClient?.connected) {
          try {
            this.sendWebSocketMessage({ type: 'chat', data: payload });
            return;
          } catch { /* Retry the same client ID over HTTP if publishing fails. */ }
        }
        const content = typeof payload.content === 'object' ? payload.content
          : { content: payload.content, contentType: 1, url: null, atUidList: [] };
        const response = await api.chat.sendMessage(payload.sharedChatId, {
          content, messageType: payload.messageType, clientMessageId: payload.clientMessageId
        });
        if (!isCurrentSession(session)) return;
        if (response.code !== 0 || !response.data) throw new Error(response.message || 'Message send failed');
        this.handleIncomingChatMessage({ ...response.data, sharedChatId: payload.sharedChatId,
          clientMessageId: payload.clientMessageId, senderId: useAuthStore().currentUser?.id });
      } catch (error) {
        if (!isCurrentSession(session)) return;
        this.updateMessageStatus(null, MESSAGE_STATUS.FAILED, payload.clientMessageId);
        throw error;
      }
    },

    touchChat(chatId) {
      const key = String(chatId);
      this.cacheEntries[key] ||= { token: ++this.cacheClock, used: this.cacheClock };
      this.cacheEntries[key].used = ++this.cacheClock;
      this.chatMessages[key] ||= [];
      this.enforceChatCacheLimit();
      return this.cacheEntries[key]?.token;
    },

    enforceChatCacheLimit() {
      const candidates = Object.keys(this.cacheEntries).sort((a, b) => this.cacheEntries[a].used - this.cacheEntries[b].used);
      for (const key of candidates) {
        if (Object.keys(this.cacheEntries).length <= this.maxCachedChats) break;
        if (sameId(key, this.currentChatId) || (this.chatMessages[key] || []).some(message => message.sendPayload
          || (!isPersistedId(message.id) && [MESSAGE_STATUS.SENDING, MESSAGE_STATUS.FAILED].includes(message.status)))) continue;
        this.clearChatMessages(key);
      }
    },

    trimChatWindow(chatId, direction = 'newer') {
      const messages = this.chatMessages[chatId] || [];
      const persisted = messages.filter(message => isPersistedId(message.id));
      const kept = direction === 'older' ? persisted.slice(0, this.maxCachedMessages) : persisted.slice(-this.maxCachedMessages);
      const ids = new Set(kept.map(message => String(message.id)));
      this.chatMessages[chatId] = messages.filter(message => !isPersistedId(message.id) || ids.has(String(message.id)));
      const snapshots = Object.keys(this.reactionSnapshots).filter(key => key.startsWith(`${chatId}:`));
      for (const key of snapshots.slice(0, Math.max(0, snapshots.length - this.maxCachedMessages))) delete this.reactionSnapshots[key];
    },

    async fetchMessagesForChat(chatId, page = 1, limit = null, direction = page === 1 ? 'latest' : 'older') {
      const session = captureSession();
      const token = this.touchChat(chatId);
      const request = ++this.cacheClock;
      this.historyRequests[chatId] = request;
      const current = () => isCurrentSession(session) && this.cacheEntries[chatId]?.token === token
        && this.historyRequests[chatId] === request;
      const batchSize = Math.min(limit || this.messageBatchSize, this.maxCachedMessages);
      let response;
      try { response = await api.chat.getChatMessages(String(chatId), { page, size: batchSize }); }
      catch (error) { if (!current()) return null; delete this.historyRequests[chatId]; throw error; }
      if (!current()) return null;
      if (response.code !== 0 || !response.data) return;
      const messages = Array.isArray(response.data) ? response.data : response.data.list || response.data.data || [];
      const previous = this.chatPagination[chatId];
      const pages = Math.max(1, Math.floor(this.maxCachedMessages / batchSize));
      if (direction === 'latest' && previous?.hasNewer) {
        this.chatMessages[chatId] = this.chatMessages[chatId].filter(message => !isPersistedId(message.id));
      }
      for (const message of messages) this.mergeConfirmedMessage(chatId, message, true);
      this.trimChatWindow(chatId, direction);
      if (direction === 'older') {
        this.chatPagination[chatId] = { page, firstPage: Math.max(previous?.firstPage || 1, page - pages + 1),
          hasMore: messages.length === batchSize, hasNewer: true };
      } else if (direction === 'newer') {
        const lastPage = Math.min(previous?.page || page, page + pages - 1);
        this.chatPagination[chatId] = { page: lastPage, firstPage: page,
          hasMore: lastPage < (previous?.page || 0) || previous?.hasMore || false, hasNewer: page > 1 };
      } else {
        this.chatPagination[chatId] = { page: 1, firstPage: 1, hasMore: messages.length === batchSize, hasNewer: false };
      }
      if (page === 1 && this.syncCursors[chatId] === undefined) this.syncCursors[chatId] = highestMessageId(messages);
      if (this.historyRequests[chatId] === request) delete this.historyRequests[chatId];
    },

    mergeConfirmedMessage(chatId, source, forceWindow = false) {
      const incoming = canonicalMessage(source, chatId, useAuthStore().currentUser?.id);
      if (!incoming || !isPersistedId(incoming.id)) return false;
      this.touchChat(chatId);
      if (!this.cacheEntries[chatId]) return false;
      const key = `${chatId}:${incoming.id}`;
      const snapshot = this.reactionSnapshots[key];
      if (snapshot && compareIds(snapshot.reactionVersion, incoming.reactionVersion) >= 0) {
        incoming.reactions = snapshot.reactions;
        incoming.reactionVersion = snapshot.reactionVersion;
      }
      const rows = this.chatMessages[chatId];
      const existing = rows.find(message => sameId(message.id, incoming.id)
        || (incoming.clientMessageId && message.clientMessageId === incoming.clientMessageId && incoming.isFromMe && message.isFromMe));
      const seen = (this.recentMessageIds[chatId] ||= []);
      const duplicate = seen.includes(String(incoming.id));
      if (!duplicate) { seen.push(String(incoming.id)); if (seen.length > this.maxCachedMessages) seen.shift(); }
      const olderWindow = this.chatPagination[chatId]?.hasNewer;
      if (!forceWindow && olderWindow && !existing) return !duplicate;
      const added = mergeMessage(rows, incoming);
      if (!forceWindow) this.trimChatWindow(chatId, olderWindow ? 'older' : 'newer');
      this.enforceChatCacheLimit();
      return added && !duplicate;
    },

    async syncChatMessages(chatId) {
      if (this.syncRequests[chatId]) return this.syncRequests[chatId];
      const session = captureSession();
      const token = this.touchChat(chatId);
      const current = () => isCurrentSession(session) && this.cacheEntries[chatId]?.token === token;
      const request = (async () => {
        let cursor = this.syncCursors[chatId] ?? '0';
        while (true) {
          const response = await api.chat.syncMessages(chatId, cursor, 100);
          if (!current()) return null;
          if (response.code !== 0 || !Array.isArray(response.data?.list)) throw new Error('Invalid message sync response');
          for (const message of response.data.list) this.mergeConfirmedMessage(chatId, message);
          const next = String(response.data.nextAfterMessageId ?? cursor);
          if (compareIds(next, cursor) < 0 || (response.data.hasMore && compareIds(next, cursor) <= 0)) {
            throw new Error('Message sync cursor did not advance');
          }
          this.syncCursors[chatId] = next;
          if (!response.data.hasMore) return;
          cursor = next;
        }
      })();
      this.syncRequests[chatId] = request;
      try { return await request; }
      finally {
        if (current() && this.syncRequests[chatId] === request) delete this.syncRequests[chatId];
      }
    },

    async refreshLoadedMessageState(chatId) {
      const session = captureSession();
      const token = this.touchChat(chatId);
      const ids = (this.chatMessages[chatId] || []).map(message => message.id).filter(isPersistedId);
      for (let offset = 0; offset < ids.length; offset += 100) {
        const batch = ids.slice(offset, offset + 100);
        const response = await api.chat.getMessageState(chatId, batch);
        if (!isCurrentSession(session) || this.cacheEntries[chatId]?.token !== token) return null;
        if (response.code !== 0 || !Array.isArray(response.data)) throw new Error('Invalid message state response');
        const returnedIds = new Set(response.data.map(message => String(message.id)));
        const requestedIds = new Set(batch.map(String));
        this.chatMessages[chatId] = (this.chatMessages[chatId] || []).filter(message =>
          !requestedIds.has(String(message.id)) || returnedIds.has(String(message.id)));
        for (const message of response.data) {
          if (this.chatMessages[chatId].some(row => sameId(row.id, message.id))) this.mergeConfirmedMessage(chatId, message, true);
        }
      }
    },

    async loadMoreMessages() {
      if (!this.activeChatSession) return;
      const chatId = this.activeChatSession.id;
      const pagination = this.chatPagination[chatId] || { page: 0 };
      await this.fetchMessagesForChat(chatId, pagination.page + 1, null, 'older');
    },

    async loadNewerMessages() {
      if (!this.activeChatSession) return;
      const chatId = this.activeChatSession.id;
      const pagination = this.chatPagination[chatId];
      if (pagination?.firstPage > 1) await this.fetchMessagesForChat(chatId, pagination.firstPage - 1, null, 'newer');
      else await this.loadLatestMessages();
    },

    async loadLatestMessages() {
      if (!this.activeChatSession) return;
      const chatId = this.activeChatSession.id;
      await this.fetchMessagesForChat(chatId, 1, null, 'latest');
    },

    clearChatMessages(chatId) {
      for (const map of [this.chatMessages, this.cacheEntries, this.chatPagination, this.historyRequests,
        this.syncCursors, this.syncRequests, this.recentMessageIds, this.newerIncomingIds, this.isTyping]) delete map[chatId];
      for (const map of [this.readCursors, this.readRequests, this.reactionSnapshots]) {
        for (const key of Object.keys(map)) if (key.startsWith(`${chatId}:`)) delete map[key];
      }
    },

    async fetchRecentChats() {
      const requestSession = captureSession();
      try {
        // 使用新的chat API获取聊天列表
        const response = await api.chat.getChatList();
        if (!isCurrentSession(requestSession)) return null;
        if (response.code === 0 && response.data) {
          // ✅ 修复：处理不同的响应结构
          const chatList = Array.isArray(response.data) 
            ? response.data 
            : (response.data.data || response.data.list || []);
          this.recentSessions = chatList.map(item => ({ ...item, id: item.sharedChatId ?? item.shared_chat_id ?? item.id, sharedChatId: item.sharedChatId ?? item.shared_chat_id ?? item.id }));
        }
      } catch (error) {
        if (!isCurrentSession(requestSession)) return null;
        console.error('Failed to fetch recent chats:', error);
      }
    },

    updateRecentSession(chatId, lastMessage) {
      const existingIndex = this.recentSessions.findIndex(session => sameId(session.sharedChatId ?? session.id, chatId));
      const sessionData = {
        ...(existingIndex >= 0 ? this.recentSessions[existingIndex] : {}),
        id: chatId, sharedChatId: chatId,
        lastMessage: lastMessage.content,
        lastMessageTime: lastMessage.timestamp ?? new Date(),
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
      const session = captureSession();
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
          if (!isCurrentSession(session)) return;
          if (this.isTyping[chatId] && this.isTyping[chatId][userId]) {
            this.isTyping[chatId][userId] = false;
          }
        }, 3000);
      } else {
        delete this.isTyping[chatId][userId];
      }
    },

    // Update message status
    updateMessageStatus(messageId, status, clientMessageId = null) {
      for (const messages of Object.values(this.chatMessages)) {
        const message = messages.find(item => (clientMessageId && (item.clientMessageId === clientMessageId || item.tempId === clientMessageId))
          || sameId(item.id, messageId));
        if (!message) continue;
        if (isPersistedId(message.id) && (status === MESSAGE_STATUS.FAILED || status === MESSAGE_STATUS.SENDING)) continue;
        if (messageId != null && isPersistedId(messageId)) {
          message.id = messageId;
          message.messageId = messageId;
          delete message.tempId;
          delete message.sendPayload;
        }
        const wasConfirmed = message.status >= MESSAGE_STATUS.SENT && message.status <= MESSAGE_STATUS.READ;
        message.status = wasConfirmed && status >= MESSAGE_STATUS.SENT && status <= MESSAGE_STATUS.READ
          ? Math.max(message.status, status) : status ?? MESSAGE_STATUS.SENT;
      }
    },

    /**
     * ✅ 拉取离线消息
     */
    async fetchOfflineMessages() {
      const session = captureSession();
      await this.fetchRecentChats();
      if (!isCurrentSession(session)) return null;
      const chatIds = new Set(Object.keys(this.cacheEntries));
      if (this.currentChatId != null) chatIds.add(String(this.currentChatId));
      for (const chatId of chatIds) {
        if (!isCurrentSession(session)) return null;
        try {
          if (this.syncCursors[chatId] === undefined) {
            if (!sameId(chatId, this.currentChatId)) continue;
            await this.fetchMessagesForChat(chatId);
          }
          else await this.refreshLoadedMessageState(chatId);
          if (!isCurrentSession(session)) return null;
          await this.syncChatMessages(chatId);
        } catch (error) {
          if (!isCurrentSession(session)) return null;
          log.warn('Unable to recover messages for chat', chatId);
        }
      }
      if (isCurrentSession(session)) await this.fetchUnreadStats();
    },

    // ==================== 未读计数相关方法 ====================

    /**
     * ✅ 获取未读统计
     */
    async fetchUnreadStats() {
      const requestSession = captureSession();
      try {
        const response = await api.chat.getUnreadStats();
        if (!isCurrentSession(requestSession)) return null;
        if (response.code === 0 && response.data) {
          // 更新未读计数映射
          this.unreadCountMap = {};
          this.unreadCounts = {}; // 清空旧的未读计数
          
          // ✅ 修复：处理不同的响应结构
          const unreadData = response.data.data || response.data;
          const unreadList = unreadData?.unreadList || unreadData?.list || [];
          
          if (unreadList && Array.isArray(unreadList)) {
            unreadList.forEach(item => {
              this.unreadCountMap[item.chat_id] = item.unread_count;
              this.unreadCounts[item.chat_id] = item.unread_count;
            });
          }
          
          console.log('✅ 未读统计已更新:', this.totalUnreadCount);
        }
      } catch (error) {
        if (!isCurrentSession(requestSession)) return null;
        console.error('❌ 获取未读统计失败:', error);
      }
    },

    /**
     * ✅ 获取单个聊天的未读数
     */
    getUnreadCount(chatId) {
      return this.unreadCountMap[chatId] || 0;
    },

    /**
     * ✅ 标记聊天已读（增强版）
     */
    async markChatAsRead(chatId, lastReadMessageId = highestMessageId(this.chatMessages[chatId])) {
      const session = captureSession();
      if (!isPersistedId(lastReadMessageId)) return;
      const token = this.touchChat(chatId);
      const current = () => isCurrentSession(session) && this.cacheEntries[chatId]?.token === token;
      const key = `${chatId}:${useAuthStore().currentUser?.id}`;
      if (compareIds(lastReadMessageId, this.readCursors[key] ?? 0) <= 0) return;
      const pending = this.readRequests[key];
      if (pending && compareIds(lastReadMessageId, pending.boundary) <= 0) return pending.promise;
      const request = (async () => {
        const response = await api.chat.markAsRead(chatId, String(lastReadMessageId));
        if (!current()) return;
        if (response.code === 0 && response.data && typeof response.data === 'object') this.handleReadReceipt(response.data);
      })();
      this.readRequests[key] = { boundary: String(lastReadMessageId), promise: request };
      try { return await request; }
      catch (error) { if (isCurrentSession(session)) log.warn('Unable to acknowledge displayed messages'); }
      finally {
        if (current() && this.readRequests[key]?.promise === request) delete this.readRequests[key];
      }
    },

    /**
     * ✅ 批量标记已读
     */
    async batchMarkAsRead(chatIds) {
      const requestSession = captureSession();
      try {
        await api.chat.batchMarkAsRead(chatIds);
        if (!isCurrentSession(requestSession)) return null;
        
        // 更新本地状态
        chatIds.forEach(chatId => {
          this.unreadCountMap[chatId] = 0;
          this.unreadCounts[chatId] = 0; // 更新 unreadCounts 而不是直接修改 totalUnreadCount
        });
        
        console.log('✅ 批量标记已读成功');
      } catch (error) {
        if (!isCurrentSession(requestSession)) return null;
        console.error('❌ 批量标记已读失败:', error);
      }
    },

    /**
     * ✅ 收到新消息时更新未读计数
     */
    updateUnreadOnNewMessage(chatId, isFromMe) {
      if (!isFromMe && (!sameId(chatId, this.currentChatId) || this.chatPagination[chatId]?.hasNewer)) {
        this.unreadCountMap[chatId] = (this.unreadCountMap[chatId] || 0) + 1;
        this.unreadCounts[chatId] = (this.unreadCounts[chatId] || 0) + 1; // 更新 unreadCounts 而不是直接修改 totalUnreadCount
        console.log('📬 未读计数已更新: chatId=', chatId, 'total=', this.totalUnreadCount);
      }
    },

    /**
     * ✅ 处理群组成员变更事件
     * @param {Object} data - 群组成员变更数据
     * @param {String} data.type - 事件类型: GROUP_MEMBER_CHANGE
     * @param {Number} data.groupId - 群组ID
     * @param {String} data.changeType - 变更类型: MEMBER_ADDED, MEMBER_REMOVED, MEMBER_LEFT, ROLE_CHANGED
     * @param {Number} data.affectedUserId - 受影响的用户ID
     * @param {Number} data.operatorId - 操作者ID
     * @param {String} data.affectedUsername - 受影响用户的用户名
     * @param {String} data.affectedNickname - 受影响用户的昵称
     * @param {String} data.operatorUsername - 操作者用户名
     * @param {String} data.operatorNickname - 操作者昵称
     */
    handleGroupMemberChange(data) {
      console.log('👥 处理群组成员变更:', data);

      const { groupId, changeType, affectedUserId, operatorId, affectedUsername, affectedNickname } = data;

      // ✅ 乐观更新：立即更新UI
      // 1. 更新recentSessions中的群组信息
      const sessionIndex = this.recentSessions.findIndex(s => s.id === String(groupId) && s.type === 'GROUP');
      if (sessionIndex !== -1) {
        const session = this.recentSessions[sessionIndex];
        
        // 根据变更类型更新成员数
        if (changeType === 'MEMBER_ADDED') {
          if (session.memberCount) {
            session.memberCount++;
          }
          console.log(`✅ 群组成员增加: ${affectedNickname || affectedUsername} 加入了群组`);
        } else if (changeType === 'MEMBER_REMOVED' || changeType === 'MEMBER_LEFT') {
          if (session.memberCount && session.memberCount > 0) {
            session.memberCount--;
          }
          const action = changeType === 'MEMBER_LEFT' ? '退出了' : '被移出';
          console.log(`✅ 群组成员减少: ${affectedNickname || affectedUsername} ${action}群组`);
        } else if (changeType === 'ROLE_CHANGED') {
          console.log(`✅ 群组成员角色变更: ${affectedNickname || affectedUsername} 的角色已更新`);
        }

        // 更新最后消息时间
        session.lastMessageTime = new Date();
      }

      // 2. 如果当前正在查看该群组，触发成员列表刷新
      if (this.activeChatSession && this.activeChatSession.id === String(groupId)) {
        console.log('🔄 当前群组成员变更，触发刷新');
        // 触发自定义事件，让ChatPage组件刷新成员列表
        window.dispatchEvent(new CustomEvent('group-member-changed', { 
          detail: { groupId, changeType, affectedUserId, operatorId } 
        }));
      }

      // 3. 后台同步：获取最新的群组信息
      this.refreshGroupInfo(groupId);
    },

    /**
     * ✅ 处理群组信息变更事件
     * @param {Object} data - 群组信息变更数据
     * @param {String} data.type - 事件类型: GROUP_INFO_CHANGE
     * @param {Number} data.groupId - 群组ID
     * @param {String} data.changeType - 变更类型: INFO_UPDATED, OWNER_TRANSFERRED, GROUP_DISSOLVED
     * @param {Number} data.operatorId - 操作者ID
     */
    handleGroupInfoChange(data) {
      console.log('ℹ️ 处理群组信息变更:', data);

      const { groupId, changeType, operatorId } = data;

      if (changeType === 'GROUP_DISSOLVED') {
        // 群组已解散
        console.log('⚠️ 群组已解散:', groupId);
        
        // 从recentSessions中移除该群组
        const sessionIndex = this.recentSessions.findIndex(s => s.id === String(groupId) && s.type === 'GROUP');
        if (sessionIndex !== -1) {
          this.recentSessions.splice(sessionIndex, 1);
        }

        // 如果当前正在查看该群组，关闭会话
        if (this.activeChatSession && this.activeChatSession.id === String(groupId)) {
          this.activeChatSession = null;
          // 触发自定义事件，让ChatPage组件显示提示
          window.dispatchEvent(new CustomEvent('group-dissolved', { 
            detail: { groupId } 
          }));
        }

        // 清除该群组的消息
        delete this.chatMessages[groupId];
        delete this.unreadCountMap[groupId];

      } else if (changeType === 'OWNER_TRANSFERRED') {
        // 群主转让
        console.log('👑 群主已转让:', data);
        
        // 更新群组信息
        this.refreshGroupInfo(groupId);
        
        // 触发自定义事件
        window.dispatchEvent(new CustomEvent('group-owner-transferred', { 
          detail: data 
        }));

      } else if (changeType === 'INFO_UPDATED') {
        // 群组信息更新（名称、头像等）
        console.log('📝 群组信息已更新:', data);
        
        // 乐观更新：立即更新本地缓存
        const sessionIndex = this.recentSessions.findIndex(s => s.id === String(groupId) && s.type === 'GROUP');
        if (sessionIndex !== -1) {
          const session = this.recentSessions[sessionIndex];
          
          // 更新群组名称
          if (data.newGroupName) {
            session.name = data.newGroupName;
          }
          
          // 更新群组头像
          if (data.newGroupAvatarUrl) {
            session.avatar = data.newGroupAvatarUrl;
          }
        }

        // 后台同步：获取最新的群组信息
        this.refreshGroupInfo(groupId);
        
        // 触发自定义事件
        window.dispatchEvent(new CustomEvent('group-info-updated', { 
          detail: data 
        }));
      }
    },

    /**
     * ✅ 刷新群组信息（后台同步）
     * @param {Number} groupId - 群组ID
     */
    async refreshGroupInfo(groupId) {
      const requestSession = captureSession();
      try {
        console.log('🔄 刷新群组信息: groupId=', groupId);
        
        // 调用API获取最新的群组信息
        const response = await api.group.getGroupDetails(groupId);
        if (!isCurrentSession(requestSession)) return null;
        
        if (response.code === 0 && response.data) {
          // ✅ 修复：处理不同的响应结构
          const groupInfo = response.data.data || response.data;
          
          if (!groupInfo) {
            console.warn('⚠️ 群组信息为空');
            return;
          }
          
          // 更新recentSessions中的群组信息
          const sessionIndex = this.recentSessions.findIndex(s => s.id === String(groupId) && s.type === 'GROUP');
          if (sessionIndex !== -1) {
            this.recentSessions[sessionIndex] = {
              ...this.recentSessions[sessionIndex],
              name: groupInfo.groupName,
              avatar: groupInfo.groupAvatarUrl,
              memberCount: groupInfo.memberCount,
              // 其他字段...
            };
          }

          // 如果当前正在查看该群组，更新activeChatSession
          if (this.activeChatSession && this.activeChatSession.id === String(groupId)) {
            this.activeChatSession = {
              ...this.activeChatSession,
              name: groupInfo.groupName,
              avatar: groupInfo.groupAvatarUrl,
              memberCount: groupInfo.memberCount,
            };
          }

          console.log('✅ 群组信息刷新成功');
        }
      } catch (error) {
        if (!isCurrentSession(requestSession)) return null;
        console.error('❌ 刷新群组信息失败:', error);
      }
    },

    /**
     * ✅ 处理消息反应变更事件
     * @param {Object} data - 反应变更数据
     */
    handleReactionChange(data) {
      const chatId = data?.sharedChatId ?? data?.chatId;
      const messageId = data?.messageId;
      if (chatId == null || messageId == null || !Array.isArray(data.reactions)) return;
      this.touchChat(chatId);
      if (!this.cacheEntries[chatId]) return;
      const key = `${chatId}:${messageId}`;
      const message = this.chatMessages[chatId]?.find(item => sameId(item.id ?? item.messageId, messageId));
      const previous = this.reactionSnapshots[key] ?? message;
      const version = data.reactionVersion ?? 0;
      if (previous && data.reactionVersion != null && compareIds(version, previous.reactionVersion ?? 0) <= 0) return;
      if (data.reactionVersion == null && (previous?.reactionVersion ?? 0) > 0) return;
      const snapshot = { reactions: data.reactions, reactionVersion: version };
      this.reactionSnapshots[key] = snapshot;
      if (message) Object.assign(message, snapshot);
      this.trimChatWindow(chatId, this.chatPagination[chatId]?.hasNewer ? 'older' : 'newer');
      window.dispatchEvent(new CustomEvent('message-reaction-updated', { detail: { messageId, chatId, ...snapshot } }));
    }
  }
});
