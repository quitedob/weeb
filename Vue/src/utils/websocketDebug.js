/**
 * WebSocket连接调试工具
 * 用于诊断WebSocket连接问题
 */

export function debugWebSocketConnection() {
  console.group('🔍 WebSocket连接诊断');
  
  // 1. 检查localStorage中的token
  const token = localStorage.getItem('token');
  console.log('1. Token检查:', token ? '✅ 存在' : '❌ 不存在');
  if (token) {
    console.log('   Token长度:', token.length);
    console.log('   Token前20字符:', token.substring(0, 20) + '...');
  }
  
  // 2. 检查authStore
  try {
    const authStoreData = localStorage.getItem('auth-store');
    if (authStoreData) {
      const parsed = JSON.parse(authStoreData);
      console.log('2. AuthStore检查:', parsed.token ? '✅ 有token' : '❌ 无token');
      console.log('   用户信息:', parsed.currentUser ? '✅ 存在' : '❌ 不存在');
      if (parsed.currentUser) {
        console.log('   用户ID:', parsed.currentUser.id);
        console.log('   用户名:', parsed.currentUser.username);
      }
    } else {
      console.log('2. AuthStore检查: ❌ 不存在');
    }
  } catch (e) {
    console.error('2. AuthStore解析失败:', e);
  }
  
  // 3. 检查WebSocket URL
  const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
  console.log('3. WebSocket URL:', wsUrl);
  
  // 4. 检查后端是否可访问
  console.log('4. 后端连接测试:');
  fetch('http://localhost:8080/api/users/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
    .then(response => {
      console.log('   API响应状态:', response.status);
      if (response.ok) {
        console.log('   ✅ 后端API可访问');
      } else {
        console.log('   ❌ 后端API返回错误:', response.status);
      }
    })
    .catch(error => {
      console.error('   ❌ 后端API无法访问:', error.message);
    });
  
  // 5. 测试WebSocket端点
  console.log('5. WebSocket端点测试:');
  try {
    const testWs = new WebSocket('ws://localhost:8080/ws');
    testWs.onopen = () => {
      console.log('   ✅ WebSocket端点可访问');
      testWs.close();
    };
    testWs.onerror = (error) => {
      console.error('   ❌ WebSocket端点无法访问:', error);
    };
  } catch (e) {
    console.error('   ❌ WebSocket创建失败:', e);
  }
  
  console.groupEnd();
}

// 自动在开发环境运行诊断
if (import.meta.env.DEV) {
  // 延迟执行，等待应用初始化
  setTimeout(() => {
    debugWebSocketConnection();
  }, 2000);
}

export default {
  debugWebSocketConnection
};
