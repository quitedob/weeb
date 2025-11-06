/**
 * 前端性能优化工具函数
 */

import { DEBOUNCE_CONFIG, LAZY_LOAD_CONFIG } from '@/config/performance';

/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {number} wait 等待时间（毫秒）
 * @param {boolean} immediate 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, wait = DEBOUNCE_CONFIG.input, immediate = false) {
  let timeout;
  
  return function executedFunction(...args) {
    const context = this;
    
    const later = function() {
      timeout = null;
      if (!immediate) func.apply(context, args);
    };
    
    const callNow = immediate && !timeout;
    
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
    
    if (callNow) func.apply(context, args);
  };
}

/**
 * 节流函数
 * @param {Function} func 要节流的函数
 * @param {number} limit 时间限制（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, limit = DEBOUNCE_CONFIG.scroll) {
  let inThrottle;
  
  return function(...args) {
    const context = this;
    
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

/**
 * 图片懒加载
 * @param {HTMLImageElement} img 图片元素
 * @param {string} src 图片源地址
 * @param {Object} options 配置选项
 */
export function lazyLoadImage(img, src, options = {}) {
  const config = { ...LAZY_LOAD_CONFIG, ...options };
  
  // 设置占位图
  img.src = config.placeholder;
  
  // 创建Intersection Observer
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const image = entry.target;
        
        // 加载真实图片
        const tempImg = new Image();
        tempImg.onload = () => {
          image.src = src;
          image.classList.add('loaded');
        };
        tempImg.onerror = () => {
          image.src = config.error;
          image.classList.add('error');
        };
        tempImg.src = src;
        
        // 停止观察
        observer.unobserve(image);
      }
    });
  }, config.observerOptions);
  
  observer.observe(img);
  
  return observer;
}

/**
 * 批量图片懒加载
 * @param {string} selector 图片选择器
 * @param {Object} options 配置选项
 */
export function lazyLoadImages(selector = 'img[data-lazy]', options = {}) {
  const images = document.querySelectorAll(selector);
  
  images.forEach(img => {
    const src = img.dataset.src || img.dataset.lazy;
    if (src) {
      lazyLoadImage(img, src, options);
    }
  });
}

/**
 * 请求动画帧节流
 * @param {Function} func 要执行的函数
 * @returns {Function} 节流后的函数
 */
export function rafThrottle(func) {
  let rafId = null;
  
  return function(...args) {
    const context = this;
    
    if (rafId === null) {
      rafId = requestAnimationFrame(() => {
        func.apply(context, args);
        rafId = null;
      });
    }
  };
}

/**
 * 空闲时执行
 * @param {Function} func 要执行的函数
 * @param {Object} options 配置选项
 */
export function runWhenIdle(func, options = {}) {
  if ('requestIdleCallback' in window) {
    requestIdleCallback(func, options);
  } else {
    // 降级方案
    setTimeout(func, 1);
  }
}

/**
 * 预加载资源
 * @param {string} url 资源URL
 * @param {string} type 资源类型 ('image' | 'script' | 'style')
 * @returns {Promise} 加载Promise
 */
export function preloadResource(url, type = 'image') {
  return new Promise((resolve, reject) => {
    let element;
    
    switch (type) {
      case 'image':
        element = new Image();
        element.onload = resolve;
        element.onerror = reject;
        element.src = url;
        break;
        
      case 'script':
        element = document.createElement('script');
        element.onload = resolve;
        element.onerror = reject;
        element.src = url;
        document.head.appendChild(element);
        break;
        
      case 'style':
        element = document.createElement('link');
        element.rel = 'stylesheet';
        element.onload = resolve;
        element.onerror = reject;
        element.href = url;
        document.head.appendChild(element);
        break;
        
      default:
        reject(new Error(`Unsupported resource type: ${type}`));
    }
  });
}

/**
 * 批量预加载资源
 * @param {Array} resources 资源列表 [{url, type}, ...]
 * @returns {Promise} 加载Promise
 */
export function preloadResources(resources) {
  const promises = resources.map(({ url, type }) => preloadResource(url, type));
  return Promise.all(promises);
}

/**
 * 性能标记
 * @param {string} name 标记名称
 */
export function performanceMark(name) {
  if ('performance' in window && 'mark' in performance) {
    performance.mark(name);
  }
}

/**
 * 性能测量
 * @param {string} name 测量名称
 * @param {string} startMark 开始标记
 * @param {string} endMark 结束标记
 * @returns {number} 持续时间（毫秒）
 */
export function performanceMeasure(name, startMark, endMark) {
  if ('performance' in window && 'measure' in performance) {
    try {
      performance.measure(name, startMark, endMark);
      const measure = performance.getEntriesByName(name)[0];
      return measure ? measure.duration : 0;
    } catch (e) {
      console.warn('Performance measure failed:', e);
      return 0;
    }
  }
  return 0;
}

/**
 * 获取性能指标
 * @returns {Object} 性能指标对象
 */
export function getPerformanceMetrics() {
  if (!('performance' in window)) {
    return null;
  }
  
  const navigation = performance.getEntriesByType('navigation')[0];
  const paint = performance.getEntriesByType('paint');
  
  return {
    // 首次内容绘制
    fcp: paint.find(entry => entry.name === 'first-contentful-paint')?.startTime || 0,
    
    // DNS查询时间
    dns: navigation ? navigation.domainLookupEnd - navigation.domainLookupStart : 0,
    
    // TCP连接时间
    tcp: navigation ? navigation.connectEnd - navigation.connectStart : 0,
    
    // 请求时间
    request: navigation ? navigation.responseStart - navigation.requestStart : 0,
    
    // 响应时间
    response: navigation ? navigation.responseEnd - navigation.responseStart : 0,
    
    // DOM解析时间
    domParse: navigation ? navigation.domInteractive - navigation.responseEnd : 0,
    
    // 资源加载时间
    resourceLoad: navigation ? navigation.loadEventStart - navigation.domContentLoadedEventEnd : 0,
    
    // 总加载时间
    totalLoad: navigation ? navigation.loadEventEnd - navigation.fetchStart : 0
  };
}

/**
 * 内存使用情况
 * @returns {Object} 内存使用对象
 */
export function getMemoryUsage() {
  if ('memory' in performance) {
    return {
      // 已使用内存（MB）
      usedJSHeapSize: (performance.memory.usedJSHeapSize / 1024 / 1024).toFixed(2),
      
      // 总内存（MB）
      totalJSHeapSize: (performance.memory.totalJSHeapSize / 1024 / 1024).toFixed(2),
      
      // 内存限制（MB）
      jsHeapSizeLimit: (performance.memory.jsHeapSizeLimit / 1024 / 1024).toFixed(2),
      
      // 使用率
      usageRate: ((performance.memory.usedJSHeapSize / performance.memory.jsHeapSizeLimit) * 100).toFixed(2) + '%'
    };
  }
  return null;
}

/**
 * 清理未使用的缓存
 * @param {Object} cache 缓存对象
 * @param {number} maxAge 最大缓存时间（毫秒）
 */
export function cleanupCache(cache, maxAge = 5 * 60 * 1000) {
  const now = Date.now();
  
  Object.keys(cache).forEach(key => {
    const item = cache[key];
    if (item && item.timestamp && (now - item.timestamp > maxAge)) {
      delete cache[key];
    }
  });
}

/**
 * 压缩图片
 * @param {File} file 图片文件
 * @param {number} quality 压缩质量 (0-1)
 * @param {number} maxWidth 最大宽度
 * @param {number} maxHeight 最大高度
 * @returns {Promise<Blob>} 压缩后的图片Blob
 */
export function compressImage(file, quality = 0.8, maxWidth = 1920, maxHeight = 1080) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onload = (e) => {
      const img = new Image();
      
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;
        
        // 计算缩放比例
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width *= ratio;
          height *= ratio;
        }
        
        canvas.width = width;
        canvas.height = height;
        
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        
        canvas.toBlob(resolve, file.type, quality);
      };
      
      img.onerror = reject;
      img.src = e.target.result;
    };
    
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export default {
  debounce,
  throttle,
  lazyLoadImage,
  lazyLoadImages,
  rafThrottle,
  runWhenIdle,
  preloadResource,
  preloadResources,
  performanceMark,
  performanceMeasure,
  getPerformanceMetrics,
  getMemoryUsage,
  cleanupCache,
  compressImage
};
