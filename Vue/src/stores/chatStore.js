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
    chatPagination: {},      // Pagination info per chat: { chatId1: { hasMore: true, page: 1 }, ... }
    recentSessions: [],      // List of recent chat sessions for a chat list panel
    unreadCounts: {},        // Unread message counts per chatId: { chatId1: 2, chatId2: 0 }
    unreadCountMap: {},      // ✅ 新增：未读计数映射 { chatId: unreadCount }
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

      // authStore使用accessToken，不是token
      const token = authStore.accessToken;

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
      if (this.stompClient) {
        try {
          if (this.stompClient.connected) {
            console.log('🔄 断开现有WebSocket连接...');
            this.stompClient.deactivate();
          }
        } catch (error) {
          console.warn('清理现有连接时出错:', error);
        }
        this.stompClient = null;
      }

      this.connectionStatus = 'connecting';
      console.log('⏳ WebSocket连接状态: connecting');

      try {
        // 获取WebSocket URL（根据环境配置）
        const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
        console.log('🌐 WebSocket URL:', wsUrl);

        // ✅ 修复2：创建STOMP客户端（增强认证）
        this.stompClient = new Client({
          webSocketFactory: () => {
            console.log('🏭 创建SockJS连接...');
            console.log('🔗 连接URL:', wsUrl);
            console.log('🔑 Token长度:', token ? token.length : 0);
            console.log('🔑 Token前缀:', token ? token.substring(0, 20) + '...' : 'N/A');

            const sockJS = new SockJS(wsUrl);

            // ✅ 修复2：增强错误处理
            sockJS.onclose = (event) => {
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
          debug: (str) => {
            // ✅ 修复2：更详细的调试日志
            if (str.includes('ERROR') || str.includes('RECEIPT')) {
              console.error('📡 STOMP错误:', str);
            } else if (str.includes('CONNECTED')) {
              console.log('📡 STOMP已连接:', str);
            } else {
              console.log('📡 STOMP:', str);
            }
            log.debug('STOMP Debug:', str);
          },
          reconnectDelay: 3000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          connectionTimeout: 15000,
        });

        // Connection successful
        this.stompClient.onConnect = (frame) => {
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
          console.error('❌ WebSocket STOMP错误:', frame);
          console.error('错误详情:', frame.headers);
          console.error('错误消息:', frame.body);
          this.connectionStatus = 'error';

          // ✅ 指数退避重连策略
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            // 计算延迟时间：1s, 2s, 4s, 8s, 16s, 最大30s
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

            setTimeout(() => {
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
          console.log('⚠️ WebSocket断开连接');
          this.connectionStatus = 'disconnected';
          this.stopHeartbeat();
        };

        // Web Socket error
        this.stompClient.onWebSocketError = (error) => {
          console.error('❌ WebSocket底层错误:', error);
          this.connectionStatus = 'error';
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

      const authStore = useAuthStore();
      const username = authStore.currentUser?.username;

      if (!username) {
        console.warn('⚠️ 用户名为空，无法订阅队列');
        return;
      }

      console.log('📡 订阅WebSocket队列: username=', username);

      try {
        // ✅ 订阅私聊消息
        this.stompClient.subscribe(`/user/${username}/queue/private`, (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            console.log('📨 收到私聊消息:', parsedMessage);
            this.handleIncomingChatMessage(parsedMessage);
          } catch (error) {
            console.error('❌ 处理私聊消息失败:', error, message.body);
          }
        });

        // ✅ 订阅聊天列表更新
        this.stompClient.subscribe(`/user/${username}/queue/chat-list-update`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('📋 聊天列表已更新:', data);
            this.handleChatListUpdate(data);
          } catch (error) {
            console.error('❌ 处理聊天列表更新失败:', error, message.body);
          }
        });

        // ✅ 订阅消息状态更新
        this.stompClient.subscribe(`/user/${username}/queue/message-status`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('✓ 消息状态更新:', data);
            this.handleMessageStatusUpdate(data);
          } catch (error) {
            console.error('❌ 处理消息状态更新失败:', error, message.body);
          }
        });

        // ✅ 订阅已读回执
        this.stompClient.subscribe(`/user/${username}/queue/read-receipt`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('👁️ 收到已读回执:', data);
            this.handleReadReceipt(data);
          } catch (error) {
            console.error('❌ 处理已读回执失败:', error, message.body);
          }
        });

        // ✅ 订阅群组成员变更事件
        this.stompClient.subscribe(`/user/${username}/queue/group-member-change`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('👥 收到群组成员变更事件:', data);
            this.handleGroupMemberChange(data);
          } catch (error) {
            console.error('❌ 处理群组成员变更失败:', error, message.body);
          }
        });

        // ✅ 订阅群组信息变更事件
        this.stompClient.subscribe(`/user/${username}/queue/group-info-change`, (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('ℹ️ 收到群组信息变更事件:', data);
            this.handleGroupInfoChange(data);
          } catch (error) {
            console.error('❌ 处理群组信息变更失败:', error, message.body);
          }
        });

        // Subscribe to error messages
        this.stompClient.subscribe(`/user/${username}/queue/errors`, (message) => {
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

        // Subscribe to general chat topics (optional)
        this.stompClient.subscribe('/topic/chat/*', (message) => {
          try {
            const parsedMessage = JSON.parse(message.body);
            if (parsedMessage.type === 'join' || parsedMessage.type === 'leave') {
              console.log('Chat room status:', parsedMessage);
            }
          } catch (error) {
            console.error('❌ 处理聊天主题消息失败:', error, message.body);
          }
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
              // 确保chatId正确设置
              if (!payload.chatId && payload.targetId) {
                payload.chatId = String(payload.targetId);
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
      console.log('📥 处理接收到的消息:', message);
      
      // ✅ 修复3：优先使用sharedChatId
      const sharedChatId = message.sharedChatId || message.chatId;
      const chatId = sharedChatId || message.roomId || message.data?.chatId || message.data?.targetId || message.targetId;
      const authStore = useAuthStore();
      const currentUserId = authStore.currentUser?.id;
      
      // ✅ 判断是否是自己发的消息
      const isFromMe = message.isFromMe !== undefined 
        ? message.isFromMe 
        : (message.fromId || message.data?.fromUserId) === currentUserId;

      console.log('📊 消息信息: chatId=', chatId, 'sharedChatId=', sharedChatId, 'isFromMe=', isFromMe, 'currentUserId=', currentUserId);

      // Parse message content for file messages
      let content, displayContent, fileData;

      if (message.content !== undefined) {
        // Spring WebSocket format
        content = message.content;
        displayContent = message.content;
      } else if (message.msgContent !== undefined) {
        // MessageResponse format
        content = message.msgContent;
        displayContent = message.msgContent;
      } else if (message.data?.content !== undefined) {
        // Old format
        content = message.data.content;
        displayContent = message.data.content;
      }

      if (message.messageType === 2 || message.type === 2 || message.data?.messageType === 2) {
        // File message - parse JSON content
        try {
          fileData = JSON.parse(content);
          displayContent = `[文件] ${fileData.fileName}`;
        } catch (error) {
          console.error('Failed to parse file message content:', error);
          displayContent = '[文件消息]';
        }
      }

      // ✅ 修复3：创建标准化消息对象，包含sharedChatId
      const standardizedMessage = {
        id: message.id || message.messageId || Date.now(),
        tempId: message.tempId,
        clientMessageId: message.clientMessageId,
        fromId: message.fromId || message.data?.fromUserId,
        fromName: message.fromName || message.data?.fromName || 'Unknown',
        msgContent: displayContent,
        content: content,
        isRecalled: message.isRecalled || 0,
        messageType: message.messageType || message.type || message.data?.messageType || 1,
        chatType: message.type === 'private' ? 'PRIVATE' : (message.data?.chatType || 'PRIVATE'),
        targetId: chatId,
        chatId: chatId,
        sharedChatId: sharedChatId, // ✅ 保存sharedChatId
        timestamp: message.timestamp || message.data?.timestamp || new Date(),
        isFromMe: isFromMe,
        msgType: message.messageType || message.type || message.data?.messageType || 1,
        fileData: fileData,
        // ✅ 使用后端返回的状态
        status: message.status !== undefined ? message.status : MESSAGE_STATUS.SENT
      };

      console.log('📦 标准化消息:', standardizedMessage);

      // 标准化消息对象，确保状态字段正确
      const normalizedMessage = normalizeMessage(standardizedMessage);

      // ✅ 消息去重检查
      if (this.isDuplicateMessage(chatId, message.id, message.clientMessageId)) {
        console.log('⚠️ 重复消息已忽略');
        return;
      }

      // ✅ 如果有clientMessageId，先查找并更新临时消息
      if (message.clientMessageId) {
        console.log('🔄 更新临时消息: clientMessageId=', message.clientMessageId);
        this.updateMessageStatus(message.id, message.status, message.clientMessageId);
        // 如果找到了临时消息，就不再添加新消息
        const found = this.findMessageByTempId(chatId, message.clientMessageId);
        if (found) {
          console.log('✅ 临时消息已更新，不重复添加');
          return;
        }
      }

      // ✅ 添加消息到聊天
      console.log('➕ 添加消息到聊天: chatId=', chatId);
      this.addMessage(chatId, normalizedMessage);

      // ✅ 更新未读计数（仅对接收的消息）
      if (!isFromMe) {
        if (chatId !== this.currentChatId) {
          console.log('📬 增加未读计数: chatId=', chatId);
          this.incrementUnreadCount(chatId);
          // ✅ 使用新的未读计数系统
          this.updateUnreadOnNewMessage(chatId, false);
        } else {
          console.log('👁️ 当前聊天，发送已读回执');
          // 如果当前在该聊天，发送已读确认
          this.sendReadReceipt(chatId, normalizedMessage.id);
        }
      }

      // ✅ 更新聊天列表
      console.log('📋 更新聊天列表');
      this.updateRecentSession(chatId, {
        content: displayContent,
        timestamp: standardizedMessage.timestamp,
        fromUserId: standardizedMessage.fromId,
        messageType: standardizedMessage.messageType
      });

      console.log('✅ 消息处理完成');
    },

    /**
     * ✅ 新增：处理聊天列表更新
     */
    handleChatListUpdate(chatList) {
      console.log('📋 处理聊天列表更新:', chatList);
      
      const existingIndex = this.recentSessions.findIndex(
        session => session.id === chatList.id
      );

      if (existingIndex >= 0) {
        // 更新现有会话
        this.recentSessions.splice(existingIndex, 1);
      }

      // 将更新的会话移到列表顶部
      this.recentSessions.unshift({
        id: chatList.id,
        targetId: chatList.targetId,
        targetInfo: chatList.targetInfo,
        lastMessage: chatList.lastMessage,
        lastMessageTime: chatList.updateTime,
        unreadCount: chatList.unreadCount || 0
      });

      console.log('✅ 聊天列表已更新');
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
      console.log('👁️ 处理已读回执:', data);
      
      const { chatId, messageId, timestamp } = data;
      
      // 更新该聊天中所有消息的状态为已读
      if (this.chatMessages[chatId]) {
        this.chatMessages[chatId].forEach(msg => {
          // 只更新已发送或已送达的消息为已读
          if (msg.isFromMe && msg.status < MESSAGE_STATUS.READ) {
            msg.status = MESSAGE_STATUS.READ;
          }
        });
      }
      
      console.log('✅ 已读回执处理完成');
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
      try {
        console.log('📨 发送已读回执: chatId=', chatId, 'messageId=', messageId);
        
        // 通过HTTP API标记消息为已读
        await api.chat.markAsRead(chatId);
        
        // 通过WebSocket通知发送者消息已读
        if (this.stompClient && this.stompClient.connected) {
          this.stompClient.publish({
            destination: '/app/chat/read-receipt',
            body: JSON.stringify({
              chatId: chatId,
              messageId: messageId,
              timestamp: new Date().toISOString()
            })
          });
        }
        
        console.log('✅ 已读回执发送成功');
      } catch (error) {
        console.error('❌ 发送已读回执失败:', error);
      }
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

      console.log('✅ ChatStore: 活跃聊天已设置:', {
        id: normalizedSession.id,
        sharedChatId: normalizedSession.sharedChatId,
        type: normalizedSession.type,
        name: normalizedSession.name
      });

      // ✅ 修复：使用sharedChatId标记已读
      const chatIdForRead = normalizedSession.sharedChatId || normalizedSession.id;
      if (chatIdForRead && this.unreadCounts[chatIdForRead]) {
        console.log('👁️ 标记聊天已读:', chatIdForRead);
        this.markAsRead(chatIdForRead);
      }
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
    },

    setMessages(chatId, messages) {
      this.chatMessages[chatId] = messages;
    },

    async sendMessage(content, targetId, chatType = 'PRIVATE', messageType = 1) {
      if (!content || !targetId) {
        throw new Error('Content and targetId are required');
      }

      // ✅ 修复：使用sharedChatId而不是targetId
      const sharedChatId = this.activeChatSession?.sharedChatId || this.activeChatSession?.id;
      
      if (!sharedChatId) {
        console.error('🐛 BUG REPORT: Missing sharedChatId in activeChatSession', {
          activeChatSession: this.activeChatSession,
          targetId: targetId,
          timestamp: new Date().toISOString()
        });
        throw new Error('无法发送消息：缺少sharedChatId');
      }

      console.log('📤 发送消息: sharedChatId=', sharedChatId, 'targetId=', targetId);

      // 生成临时消息ID用于跟踪
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      // 创建临时消息对象（立即显示在UI中）
      const authStore = useAuthStore();
      const tempMessage = {
        id: tempId,
        tempId: tempId,
        fromId: authStore.currentUser?.id,
        fromName: authStore.currentUser?.username,
        msgContent: content,
        content: content,
        messageType: messageType,
        chatType: chatType,
        targetId: targetId,
        chatId: sharedChatId, // ✅ 使用sharedChatId
        sharedChatId: sharedChatId, // ✅ 明确保存sharedChatId
        timestamp: new Date(),
        isFromMe: true,
        status: MESSAGE_STATUS.SENDING, // 设置为发送中状态
        isRecalled: 0
      };

      // ✅ 修复：使用sharedChatId添加到消息列表
      this.addMessage(sharedChatId, tempMessage);

      // 发送消息
      const message = {
        type: 'chat',
        data: {
          content,
          targetId,
          chatType,
          messageType,
          chatId: sharedChatId, // ✅ 使用sharedChatId
          sharedChatId: sharedChatId, // ✅ 明确发送sharedChatId
          clientMessageId: tempId // 传递临时ID用于后端关联
        }
      };

      try {
        console.log('📡 通过WebSocket发送消息:', message);
        this.sendWebSocketMessage(message);
        // 消息发送后，状态会通过WebSocket回调更新
      } catch (error) {
        console.error('🐛 BUG REPORT: sendWebSocketMessage failed', {
          error: error.message,
          stack: error.stack,
          message: message,
          timestamp: new Date().toISOString()
        });
        // 发送失败，更新消息状态为失败
        this.updateMessageStatus(null, MESSAGE_STATUS.FAILED, tempId);
        throw error;
      }
    },

    async fetchMessagesForChat(chatId, page = 1, limit = null) {
      try {
        const batchSize = limit || this.messageBatchSize;
        const authStore = useAuthStore();
        const currentUserId = authStore.currentUser?.id;
        
        // ✅ 修复3：确保使用正确的chatId（优先使用sharedChatId）
        const normalizedChatId = String(chatId);
        console.log('📥 获取聊天消息: chatId=', normalizedChatId);
        
        // 使用新的chat API
        const response = await api.chat.getChatMessages(normalizedChatId, {
          page,
          size: batchSize
        });

        if (response.code === 0 && response.data) {
          // ✅ 修复：处理不同的响应结构
          const messages = Array.isArray(response.data) 
            ? response.data 
            : (response.data.data || response.data.list || []);
          
          const hasMore = messages.length === batchSize;

          // 为每条消息添加isFromMe字段并标准化状态
          const messagesWithFlag = messages.map(msg => ({
            ...msg,
            isFromMe: msg.senderId === currentUserId,
            msgContent: typeof msg.content === 'object' ? msg.content.content : msg.content,
            sharedChatId: msg.chatId // 保存sharedChatId
          }));

          const normalizedMsgs = normalizeMessages(messagesWithFlag);

          // ✅ 修复3：使用sharedChatId作为key存储消息
          const storageKey = normalizedChatId;

          // 更新分页信息
          this.chatPagination[storageKey] = {
            hasMore,
            page,
            total: messages.length
          };

          if (page === 1) {
            this.setMessages(storageKey, normalizedMsgs);
          } else {
            // Append messages for pagination
            const existingMessages = this.chatMessages[storageKey] || [];
            this.setMessages(storageKey, [...normalizedMsgs, ...existingMessages]);
          }
          
          console.log('✅ 消息获取成功: count=', normalizedMsgs.length);
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
          // ✅ 修复：处理不同的响应结构
          const chatList = Array.isArray(response.data) 
            ? response.data 
            : (response.data.data || response.data.list || []);
          this.recentSessions = chatList;
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
    },

    /**
     * ✅ 拉取离线消息
     */
    async fetchOfflineMessages() {
      try {
        console.log('📥 拉取离线消息...');
        
        // 获取所有聊天列表
        await this.fetchRecentChats();
        
        // 获取未读统计
        await this.fetchUnreadStats();
        
        // 对于有未读消息的聊天，拉取最新消息
        for (const session of this.recentSessions) {
          if (session.unreadCount > 0) {
            console.log(`📬 拉取聊天 ${session.id} 的离线消息`);
            await this.fetchMessagesForChat(session.id, 1, session.unreadCount);
          }
        }
        
        console.log('✅ 离线消息拉取完成');
      } catch (error) {
        console.error('❌ 拉取离线消息失败:', error);
      }
    },

    // ==================== 未读计数相关方法 ====================

    /**
     * ✅ 获取未读统计
     */
    async fetchUnreadStats() {
      try {
        const response = await api.chat.getUnreadStats();
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
    async markChatAsRead(chatId) {
      try {
        await api.chat.markAsRead(chatId);
        
        // 更新本地状态
        const oldUnread = this.unreadCountMap[chatId] || 0;
        this.unreadCountMap[chatId] = 0;
        this.unreadCounts[chatId] = 0; // 更新 unreadCounts 而不是直接修改 totalUnreadCount
        
        console.log('✅ 标记已读成功: chatId=', chatId);
      } catch (error) {
        console.error('❌ 标记已读失败:', error);
      }
    },

    /**
     * ✅ 批量标记已读
     */
    async batchMarkAsRead(chatIds) {
      try {
        await api.chat.batchMarkAsRead(chatIds);
        
        // 更新本地状态
        chatIds.forEach(chatId => {
          this.unreadCountMap[chatId] = 0;
          this.unreadCounts[chatId] = 0; // 更新 unreadCounts 而不是直接修改 totalUnreadCount
        });
        
        console.log('✅ 批量标记已读成功');
      } catch (error) {
        console.error('❌ 批量标记已读失败:', error);
      }
    },

    /**
     * ✅ 收到新消息时更新未读计数
     */
    updateUnreadOnNewMessage(chatId, isFromMe) {
      if (!isFromMe && chatId !== this.currentChatId) {
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
      try {
        console.log('🔄 刷新群组信息: groupId=', groupId);
        
        // 调用API获取最新的群组信息
        const response = await api.group.getGroupDetails(groupId);
        
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
        console.error('❌ 刷新群组信息失败:', error);
      }
    }
  },
});
