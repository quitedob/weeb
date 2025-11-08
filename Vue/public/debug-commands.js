/**
 * Debug Commands for Chat System
 * Copy and paste these commands into browser console for debugging
 */

// ============================================
// CHAT SYSTEM DEBUG COMMANDS
// ============================================

console.log(`
╔════════════════════════════════════════════════════════════════╗
║           CHAT SYSTEM DEBUG COMMANDS                           ║
╚════════════════════════════════════════════════════════════════╝

📋 BASIC CHECKS:
  checkChatSystem()          - Run all basic checks
  checkWebSocket()           - Check WebSocket connection status
  checkActiveChat()          - Check current active chat
  checkChatList()            - Check chat list data
  checkMessages()            - Check messages for current chat

🐛 BUG REPORTER:
  showBugSummary()           - Show bug report summary
  exportBugs()               - Export bug reports to JSON
  clearBugs()                - Clear all bug reports
  showChatIdIssues()         - Show chat ID mismatch issues
  showWebSocketIssues()      - Show WebSocket errors
  showMissingSharedChatId()  - Show missing sharedChatId issues

🔍 DETAILED INSPECTION:
  inspectChat(chatId)        - Inspect specific chat by sharedChatId
  inspectMessage(messageId)  - Inspect specific message
  compareChats()             - Compare frontend vs backend chat IDs
  validateChatIds()          - Validate all chat IDs are correct type

🧪 TESTING:
  testChatSelection()        - Test chat selection
  testMessageSend()          - Test message sending
  testMessageReceive()       - Test message receiving
  simulateError()            - Simulate an error for testing

📊 STATISTICS:
  getChatStats()             - Get chat system statistics
  getMessageStats()          - Get message statistics
  getConnectionStats()       - Get connection statistics

Type any command above to run it.
Example: checkChatSystem()
`);

// ============================================
// IMPLEMENTATION
// ============================================

window.checkChatSystem = function() {
  console.group('🔍 Chat System Check');
  
  try {
    const chatStore = window.$nuxt?.$pinia?.state?.value?.chat || 
                     window.__PINIA__?.chat ||
                     JSON.parse(localStorage.getItem('chat-store') || '{}');
    
    const authStore = window.$nuxt?.$pinia?.state?.value?.auth ||
                     window.__PINIA__?.auth ||
                     JSON.parse(localStorage.getItem('auth-store') || '{}');
    
    console.log('✅ Auth Status:', {
      isAuthenticated: !!authStore.accessToken,
      username: authStore.currentUser?.username,
      userId: authStore.currentUser?.id
    });
    
    console.log('✅ WebSocket Status:', {
      status: chatStore.connectionStatus,
      isConnected: chatStore.connectionStatus === 'connected',
      reconnectAttempts: chatStore.reconnectAttempts
    });
    
    console.log('✅ Active Chat:', {
      id: chatStore.activeChatSession?.id,
      sharedChatId: chatStore.activeChatSession?.sharedChatId,
      type: chatStore.activeChatSession?.type,
      name: chatStore.activeChatSession?.name
    });
    
    console.log('✅ Chat List:', {
      count: chatStore.recentSessions?.length || 0,
      hasSharedChatId: chatStore.recentSessions?.every(c => c.sharedChatId) || false
    });
    
    console.log('✅ Messages:', {
      currentChatMessages: Object.keys(chatStore.chatMessages || {}).length,
      totalMessages: Object.values(chatStore.chatMessages || {}).reduce((sum, msgs) => sum + msgs.length, 0)
    });
    
    console.log('✅ Unread Counts:', {
      total: Object.values(chatStore.unreadCounts || {}).reduce((sum, count) => sum + count, 0),
      byChat: chatStore.unreadCounts
    });
    
  } catch (error) {
    console.error('❌ Error checking chat system:', error);
  }
  
  console.groupEnd();
};

window.checkWebSocket = function() {
  console.group('🔌 WebSocket Check');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    
    console.log('Connection Status:', chatStore.connectionStatus);
    console.log('Reconnect Attempts:', chatStore.reconnectAttempts);
    console.log('Max Reconnect Attempts:', chatStore.maxReconnectAttempts);
    
    if (chatStore.connectionStatus === 'connected') {
      console.log('✅ WebSocket is connected');
    } else {
      console.warn('⚠️ WebSocket is not connected:', chatStore.connectionStatus);
    }
  } catch (error) {
    console.error('❌ Error checking WebSocket:', error);
  }
  
  console.groupEnd();
};

window.checkActiveChat = function() {
  console.group('💬 Active Chat Check');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    const activeChat = chatStore.activeChatSession;
    
    if (!activeChat) {
      console.warn('⚠️ No active chat');
      console.groupEnd();
      return;
    }
    
    console.log('Chat ID:', activeChat.id);
    console.log('Shared Chat ID:', activeChat.sharedChatId);
    console.log('Type:', activeChat.type);
    console.log('Name:', activeChat.name);
    console.log('Target ID:', activeChat.targetId);
    
    // Validate ID types
    console.log('\n🔍 ID Type Validation:');
    console.log('  id type:', typeof activeChat.id, activeChat.id);
    console.log('  sharedChatId type:', typeof activeChat.sharedChatId, activeChat.sharedChatId);
    
    if (typeof activeChat.sharedChatId === 'number') {
      console.log('✅ sharedChatId is Number (correct)');
    } else {
      console.error('❌ sharedChatId is not Number (should be Number)');
    }
    
  } catch (error) {
    console.error('❌ Error checking active chat:', error);
  }
  
  console.groupEnd();
};

window.checkChatList = function() {
  console.group('📋 Chat List Check');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    const chatList = chatStore.recentSessions || [];
    
    console.log('Total Chats:', chatList.length);
    
    chatList.forEach((chat, index) => {
      console.log(`\nChat ${index + 1}:`, {
        id: chat.id,
        sharedChatId: chat.sharedChatId,
        type: chat.type,
        targetId: chat.targetId,
        hasSharedChatId: !!chat.sharedChatId,
        sharedChatIdType: typeof chat.sharedChatId
      });
    });
    
    // Validation
    const missingSharedChatId = chatList.filter(c => !c.sharedChatId);
    if (missingSharedChatId.length > 0) {
      console.error('❌ Chats missing sharedChatId:', missingSharedChatId);
    } else {
      console.log('✅ All chats have sharedChatId');
    }
    
  } catch (error) {
    console.error('❌ Error checking chat list:', error);
  }
  
  console.groupEnd();
};

window.checkMessages = function() {
  console.group('💬 Messages Check');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    const activeChat = chatStore.activeChatSession;
    
    if (!activeChat) {
      console.warn('⚠️ No active chat');
      console.groupEnd();
      return;
    }
    
    const chatId = activeChat.sharedChatId || activeChat.id;
    const messages = chatStore.chatMessages?.[chatId] || [];
    
    console.log('Chat ID:', chatId);
    console.log('Total Messages:', messages.length);
    
    if (messages.length > 0) {
      console.log('\nRecent Messages:');
      messages.slice(-5).forEach((msg, index) => {
        console.log(`  ${index + 1}.`, {
          id: msg.id,
          chatId: msg.chatId,
          sharedChatId: msg.sharedChatId,
          content: msg.msgContent?.substring(0, 50),
          isFromMe: msg.isFromMe,
          status: msg.status
        });
      });
    }
    
  } catch (error) {
    console.error('❌ Error checking messages:', error);
  }
  
  console.groupEnd();
};

window.showBugSummary = function() {
  if (window.bugReporter) {
    window.bugReporter.printSummary();
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.exportBugs = function() {
  if (window.bugReporter) {
    window.bugReporter.exportReports();
    console.log('✅ Bug reports exported');
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.clearBugs = function() {
  if (window.bugReporter) {
    window.bugReporter.clearReports();
    console.log('✅ Bug reports cleared');
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.showChatIdIssues = function() {
  if (window.bugReporter) {
    const issues = window.bugReporter.getReportsByCategory('CHAT_ID_MISMATCH');
    console.log('Chat ID Mismatch Issues:', issues);
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.showWebSocketIssues = function() {
  if (window.bugReporter) {
    const issues = window.bugReporter.getReportsByCategory('WEBSOCKET_ERROR');
    console.log('WebSocket Issues:', issues);
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.showMissingSharedChatId = function() {
  if (window.bugReporter) {
    const issues = window.bugReporter.getReportsByCategory('MISSING_SHARED_CHAT_ID');
    console.log('Missing sharedChatId Issues:', issues);
  } else {
    console.warn('⚠️ Bug reporter not available');
  }
};

window.validateChatIds = function() {
  console.group('🔍 Chat ID Validation');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    const chatList = chatStore.recentSessions || [];
    
    let valid = 0;
    let invalid = 0;
    
    chatList.forEach(chat => {
      if (chat.sharedChatId && typeof chat.sharedChatId === 'number') {
        valid++;
      } else {
        invalid++;
        console.error('❌ Invalid chat:', {
          id: chat.id,
          sharedChatId: chat.sharedChatId,
          type: typeof chat.sharedChatId
        });
      }
    });
    
    console.log(`\n✅ Valid: ${valid}`);
    console.log(`❌ Invalid: ${invalid}`);
    
    if (invalid === 0) {
      console.log('\n🎉 All chat IDs are valid!');
    }
    
  } catch (error) {
    console.error('❌ Error validating chat IDs:', error);
  }
  
  console.groupEnd();
};

window.getChatStats = function() {
  console.group('📊 Chat Statistics');
  
  try {
    const chatStore = JSON.parse(localStorage.getItem('chat-store') || '{}');
    
    const stats = {
      totalChats: chatStore.recentSessions?.length || 0,
      activeChat: !!chatStore.activeChatSession,
      connectionStatus: chatStore.connectionStatus,
      totalUnread: Object.values(chatStore.unreadCounts || {}).reduce((sum, count) => sum + count, 0),
      totalMessages: Object.values(chatStore.chatMessages || {}).reduce((sum, msgs) => sum + msgs.length, 0),
      chatsWithMessages: Object.keys(chatStore.chatMessages || {}).length
    };
    
    console.table(stats);
    
  } catch (error) {
    console.error('❌ Error getting chat stats:', error);
  }
  
  console.groupEnd();
};

console.log('✅ Debug commands loaded. Type any command to run it.');
console.log('Example: checkChatSystem()');
