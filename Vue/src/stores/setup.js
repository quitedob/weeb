/**
 * Pinia Store配置和初始化
 */

import { createPinia } from 'pinia';
import { createPersistPlugin } from './plugins/persistPlugin';
import { 
  createMonitorPlugin, 
  createDevToolsPlugin,
  createErrorTrackingPlugin,
  createHistoryPlugin 
} from './plugins/monitorPlugin';

/**
 * 创建并配置Pinia实例
 * @param {Object} options 配置选项
 * @returns {Pinia} Pinia实例
 */
export function setupPinia(options = {}) {
  const {
    enablePersist = true,
    enableMonitor = import.meta.env.DEV,
    enableDevTools = import.meta.env.DEV,
    enableHistory = import.meta.env.DEV,
    errorHandler = null,
  } = options;

  const pinia = createPinia();

  // 1. 持久化插件
  if (enablePersist) {
    pinia.use(createPersistPlugin({
      key: 'weeb-store',
      storage: localStorage,
    }));
  }

  // 2. 监控插件（仅开发环境）
  if (enableMonitor) {
    pinia.use(createMonitorPlugin({
      enabled: true,
      logActions: true,
      logMutations: false, // 太吵，默认关闭
      performanceTracking: true,
      errorTracking: true,
    }));
  }

  // 3. 开发工具插件（仅开发环境）
  if (enableDevTools) {
    pinia.use(createDevToolsPlugin());
  }

  // 4. 错误追踪插件
  if (errorHandler) {
    pinia.use(createErrorTrackingPlugin(errorHandler));
  }

  // 5. 历史记录插件（仅开发环境）
  if (enableHistory) {
    pinia.use(createHistoryPlugin({
      maxHistory: 50,
      enabled: true,
    }));
  }

  console.log('✅ Pinia配置完成', {
    persist: enablePersist,
    monitor: enableMonitor,
    devTools: enableDevTools,
    history: enableHistory,
  });

  return pinia;
}

/**
 * 默认错误处理器
 */
export function defaultErrorHandler(errorInfo) {
  console.error('🚨 Store错误:', errorInfo);

  // 可以在这里集成错误追踪服务（如Sentry）
  if (window.Sentry) {
    window.Sentry.captureException(errorInfo.error, {
      extra: {
        storeId: errorInfo.storeId,
        actionName: errorInfo.actionName,
        args: errorInfo.args,
        state: errorInfo.state,
      },
    });
  }
}

export default setupPinia;
