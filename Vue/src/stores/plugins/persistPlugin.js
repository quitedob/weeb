/**
 * Pinia持久化插件
 * 自动保存和恢复Store状态
 */

/**
 * 创建持久化插件
 * @param {Object} options 配置选项
 * @returns {Function} Pinia插件函数
 */
export function createPersistPlugin(options = {}) {
  const {
    storage = localStorage,
    key = 'pinia',
    paths = [],
    beforeRestore = null,
    afterRestore = null,
  } = options;

  return (context) => {
    const { store, options: storeOptions } = context;

    // 如果Store没有配置persist，跳过
    if (!storeOptions.persist) {
      return;
    }

    const persistConfig = typeof storeOptions.persist === 'object' 
      ? storeOptions.persist 
      : {};

    const storageKey = persistConfig.key || `${key}-${store.$id}`;
    const storagePaths = persistConfig.paths || paths;
    const storageInstance = persistConfig.storage || storage;

    // 恢复状态
    const restoreState = () => {
      try {
        const savedState = storageInstance.getItem(storageKey);
        
        if (savedState) {
          const parsedState = JSON.parse(savedState);
          
          if (beforeRestore) {
            beforeRestore(parsedState, store);
          }

          // 如果指定了paths，只恢复指定的字段
          if (storagePaths.length > 0) {
            storagePaths.forEach(path => {
              if (path in parsedState) {
                store.$state[path] = parsedState[path];
              }
            });
          } else {
            // 恢复所有状态
            store.$patch(parsedState);
          }

          if (afterRestore) {
            afterRestore(store);
          }

          console.log(`✅ 恢复Store状态: ${store.$id}`);
        }
      } catch (error) {
        console.error(`❌ 恢复Store状态失败: ${store.$id}`, error);
      }
    };

    // 保存状态
    const saveState = () => {
      try {
        let stateToSave;

        // 如果指定了paths，只保存指定的字段
        if (storagePaths.length > 0) {
          stateToSave = {};
          storagePaths.forEach(path => {
            if (path in store.$state) {
              stateToSave[path] = store.$state[path];
            }
          });
        } else {
          // 保存所有状态
          stateToSave = store.$state;
        }

        storageInstance.setItem(storageKey, JSON.stringify(stateToSave));
      } catch (error) {
        console.error(`❌ 保存Store状态失败: ${store.$id}`, error);
      }
    };

    // 初始化时恢复状态
    restoreState();

    // 监听状态变化并保存
    store.$subscribe((mutation, state) => {
      saveState();
    }, { detached: true });

    // 添加清除方法
    store.$clearPersist = () => {
      try {
        storageInstance.removeItem(storageKey);
        console.log(`🗑️ 清除Store持久化数据: ${store.$id}`);
      } catch (error) {
        console.error(`❌ 清除Store持久化数据失败: ${store.$id}`, error);
      }
    };
  };
}

/**
 * 清除所有持久化数据
 * @param {Storage} storage 存储实例
 * @param {string} prefix 键前缀
 */
export function clearAllPersist(storage = localStorage, prefix = 'pinia') {
  try {
    const keys = Object.keys(storage);
    const piniaKeys = keys.filter(key => key.startsWith(prefix));

    piniaKeys.forEach(key => {
      storage.removeItem(key);
    });

    console.log(`🗑️ 清除所有持久化数据: ${piniaKeys.length} 个`);
  } catch (error) {
    console.error('❌ 清除持久化数据失败', error);
  }
}

/**
 * 获取持久化数据大小
 * @param {Storage} storage 存储实例
 * @param {string} prefix 键前缀
 * @returns {number} 数据大小（字节）
 */
export function getPersistSize(storage = localStorage, prefix = 'pinia') {
  try {
    const keys = Object.keys(storage);
    const piniaKeys = keys.filter(key => key.startsWith(prefix));

    let totalSize = 0;
    piniaKeys.forEach(key => {
      const value = storage.getItem(key);
      if (value) {
        totalSize += new Blob([value]).size;
      }
    });

    return totalSize;
  } catch (error) {
    console.error('❌ 获取持久化数据大小失败', error);
    return 0;
  }
}

/**
 * 检查存储空间是否充足
 * @param {Storage} storage 存储实例
 * @param {number} requiredSize 需要的空间（字节）
 * @returns {boolean} 是否有足够空间
 */
export function hasEnoughSpace(storage = localStorage, requiredSize = 0) {
  try {
    // 尝试写入测试数据
    const testKey = '__storage_test__';
    const testData = 'x'.repeat(requiredSize);
    
    storage.setItem(testKey, testData);
    storage.removeItem(testKey);
    
    return true;
  } catch (error) {
    console.warn('⚠️ 存储空间不足', error);
    return false;
  }
}

/**
 * 压缩存储数据
 * @param {Object} data 要压缩的数据
 * @returns {string} 压缩后的字符串
 */
export function compressData(data) {
  try {
    const jsonString = JSON.stringify(data);
    
    // 简单的压缩：移除空格和换行
    const compressed = jsonString.replace(/\s+/g, '');
    
    return compressed;
  } catch (error) {
    console.error('❌ 压缩数据失败', error);
    return JSON.stringify(data);
  }
}

/**
 * 解压存储数据
 * @param {string} compressed 压缩的字符串
 * @returns {Object} 解压后的数据
 */
export function decompressData(compressed) {
  try {
    return JSON.parse(compressed);
  } catch (error) {
    console.error('❌ 解压数据失败', error);
    return null;
  }
}

export default createPersistPlugin;
