/**
 * Pinia监控插件
 * 监控Store的状态变化和性能
 */

/**
 * 创建监控插件
 * @param {Object} options 配置选项
 * @returns {Function} Pinia插件函数
 */
export function createMonitorPlugin(options = {}) {
  const {
    enabled = true,
    logActions = true,
    logMutations = true,
    logGetters = false,
    performanceTracking = true,
    errorTracking = true,
  } = options;

  if (!enabled) {
    return () => {};
  }

  // 性能统计
  const performanceStats = new Map();

  return (context) => {
    const { store, options: storeOptions } = context;

    // 监控actions
    if (logActions) {
      store.$onAction(({ name, store, args, after, onError }) => {
        const startTime = performance.now();
        
        console.log(`🎬 Action开始: ${store.$id}.${name}`, args);

        after((result) => {
          const duration = performance.now() - startTime;
          console.log(`✅ Action完成: ${store.$id}.${name} (${duration.toFixed(2)}ms)`, result);

          // 记录性能统计
          if (performanceTracking) {
            recordPerformance(store.$id, name, duration);
          }
        });

        if (errorTracking) {
          onError((error) => {
            const duration = performance.now() - startTime;
            console.error(`❌ Action失败: ${store.$id}.${name} (${duration.toFixed(2)}ms)`, error);
          });
        }
      });
    }

    // 监控状态变化
    if (logMutations) {
      store.$subscribe((mutation, state) => {
        console.log(`🔄 State变化: ${store.$id}`, {
          type: mutation.type,
          storeId: mutation.storeId,
          payload: mutation.payload,
        });
      });
    }

    // 监控getters（可选，可能会很吵）
    if (logGetters) {
      const originalGetters = store.$state;
      Object.keys(storeOptions.getters || {}).forEach(getterName => {
        const getter = storeOptions.getters[getterName];
        Object.defineProperty(store, getterName, {
          get() {
            const value = getter.call(store, store.$state);
            console.log(`📊 Getter访问: ${store.$id}.${getterName}`, value);
            return value;
          }
        });
      });
    }

    // 添加性能统计方法
    store.$getPerformanceStats = () => {
      const stats = performanceStats.get(store.$id);
      if (!stats) return null;

      const result = {};
      stats.forEach((data, actionName) => {
        result[actionName] = {
          count: data.count,
          totalTime: data.totalTime,
          avgTime: data.totalTime / data.count,
          minTime: data.minTime,
          maxTime: data.maxTime,
        };
      });

      return result;
    };

    // 添加清除统计方法
    store.$clearPerformanceStats = () => {
      performanceStats.delete(store.$id);
      console.log(`🗑️ 清除性能统计: ${store.$id}`);
    };
  };

  /**
   * 记录性能数据
   */
  function recordPerformance(storeId, actionName, duration) {
    if (!performanceStats.has(storeId)) {
      performanceStats.set(storeId, new Map());
    }

    const storeStats = performanceStats.get(storeId);
    
    if (!storeStats.has(actionName)) {
      storeStats.set(actionName, {
        count: 0,
        totalTime: 0,
        minTime: Infinity,
        maxTime: 0,
      });
    }

    const actionStats = storeStats.get(actionName);
    actionStats.count++;
    actionStats.totalTime += duration;
    actionStats.minTime = Math.min(actionStats.minTime, duration);
    actionStats.maxTime = Math.max(actionStats.maxTime, duration);
  }
}

/**
 * 创建开发工具插件
 * @returns {Function} Pinia插件函数
 */
export function createDevToolsPlugin() {
  return (context) => {
    const { store } = context;

    // 添加到window以便在控制台访问
    if (typeof window !== 'undefined') {
      if (!window.__PINIA_STORES__) {
        window.__PINIA_STORES__ = {};
      }
      window.__PINIA_STORES__[store.$id] = store;
    }

    // 添加调试方法
    store.$debug = () => {
      console.group(`🔍 Store调试: ${store.$id}`);
      console.log('State:', store.$state);
      console.log('Actions:', Object.keys(store).filter(key => typeof store[key] === 'function'));
      
      if (store.$getPerformanceStats) {
        console.log('Performance:', store.$getPerformanceStats());
      }
      
      console.groupEnd();
    };

    // 添加状态快照方法
    store.$snapshot = () => {
      return JSON.parse(JSON.stringify(store.$state));
    };

    // 添加状态对比方法
    store.$diff = (snapshot) => {
      const current = store.$snapshot();
      const diff = {};

      Object.keys(current).forEach(key => {
        if (JSON.stringify(current[key]) !== JSON.stringify(snapshot[key])) {
          diff[key] = {
            old: snapshot[key],
            new: current[key],
          };
        }
      });

      return diff;
    };
  };
}

/**
 * 创建错误追踪插件
 * @param {Function} errorHandler 错误处理函数
 * @returns {Function} Pinia插件函数
 */
export function createErrorTrackingPlugin(errorHandler) {
  return (context) => {
    const { store } = context;

    store.$onAction(({ name, store, args, onError }) => {
      onError((error) => {
        const errorInfo = {
          storeId: store.$id,
          actionName: name,
          args,
          error,
          timestamp: new Date().toISOString(),
          state: store.$snapshot ? store.$snapshot() : store.$state,
        };

        if (errorHandler) {
          errorHandler(errorInfo);
        } else {
          console.error('Store Action Error:', errorInfo);
        }
      });
    });
  };
}

/**
 * 创建状态历史插件
 * @param {Object} options 配置选项
 * @returns {Function} Pinia插件函数
 */
export function createHistoryPlugin(options = {}) {
  const {
    maxHistory = 50,
    enabled = true,
  } = options;

  if (!enabled) {
    return () => {};
  }

  const historyMap = new Map();

  return (context) => {
    const { store } = context;

    // 初始化历史记录
    historyMap.set(store.$id, []);

    // 监听状态变化
    store.$subscribe((mutation, state) => {
      const history = historyMap.get(store.$id);
      
      // 添加新记录
      history.push({
        timestamp: Date.now(),
        mutation,
        state: JSON.parse(JSON.stringify(state)),
      });

      // 限制历史记录数量
      if (history.length > maxHistory) {
        history.shift();
      }
    });

    // 添加历史记录方法
    store.$getHistory = () => {
      return historyMap.get(store.$id) || [];
    };

    // 添加撤销方法
    store.$undo = () => {
      const history = historyMap.get(store.$id);
      if (history.length < 2) {
        console.warn('没有可撤销的历史记录');
        return false;
      }

      // 移除当前状态
      history.pop();
      
      // 恢复到上一个状态
      const previousState = history[history.length - 1];
      store.$patch(previousState.state);

      return true;
    };

    // 添加清除历史方法
    store.$clearHistory = () => {
      historyMap.set(store.$id, []);
      console.log(`🗑️ 清除历史记录: ${store.$id}`);
    };
  };
}

export default createMonitorPlugin;
