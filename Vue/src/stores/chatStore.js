// File path: /Vue/src/stores/chatStore.js
import { defineStore } from 'pinia';

export const useChatStore = defineStore('chat', {
  state: () => ({
    activeChatSession: null, // Stores the currently active chat session object
                             // e.g., { id: 'group101', name: 'Tech Talk', type: 'GROUP', ... }
    chatMessages: {},        // Object to store messages per chatId: { chatId1: [msg1, msg2], chatId2: [...] }
    // Potential future state:
    // recentSessions: [],   // List of recent chat sessions for a chat list panel
    // unreadCounts: {},     // Unread message counts per chatId: { chatId1: 2, chatId2: 0 }
    // connectionStatus: 'disconnected', // WebSocket connection status
  }),
  getters: {
    currentChatId: (state) => state.activeChatSession ? state.activeChatSession.id : null,
    currentChatType: (state) => state.activeChatSession ? state.activeChatSession.type : null,
    messagesForCurrentChat: (state) => {
      if (state.activeChatSession && state.chatMessages[state.activeChatSession.id]) {
        return state.chatMessages[state.activeChatSession.id];
      }
      return [];
    }
  },
  actions: {
    setActiveChat(session) {
      // session should be an object like:
      // { id: 'someId', name: 'Chat Name', type: 'USER'/'GROUP', avatar: 'url', ...otherInfo }
      this.activeChatSession = session;
      // Optionally, mark messages as read or fetch messages when a chat is activated
      // if (!this.chatMessages[session.id]) {
      //   this.fetchMessagesForChat(session.id);
      // }
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
      // If this message is for the active chat, and user is viewing, mark as read implicitly.
      // Otherwise, update unread count for this chatId.
    },
    setMessages(chatId, messages) {
      this.chatMessages[chatId] = messages;
    }
    // async fetchMessagesForChat(chatId) {
    //   // Placeholder for fetching messages for a chat session
    //   // try {
    //   //   const response = await api.chat.getMessages({ chatId }); // Example
    //   //   if (response.code === 200 && response.data) {
    //   //     this.chatMessages[chatId] = response.data;
    //   //   }
    //   // } catch (error) {
    //   //   console.error(`Failed to fetch messages for chat ${chatId}:`, error);
    //   // }
    // },
    // clearChatMessages(chatId) {
    //   if (this.chatMessages[chatId]) {
    //     delete this.chatMessages[chatId];
    //   }
    // }
  },
});
