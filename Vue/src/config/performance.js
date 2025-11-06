/**
 * 前端性能优化配置
 * 根据todo.txt中的性能优化需求
 */

// 虚拟滚动配置
export const VIRTUAL_SCROLL_CONFIG = {
  // 消息列表虚拟滚动配置
  messageList: {
    itemHeight: 80,        // 每条消息的平均高度（像素）
    bufferSize: 5,         // 缓冲区大小（屏幕外渲染的条目数）
    threshold: 100,        // 触发虚拟滚动的最小消息数
  },
  
  // 联系人列表虚拟滚动配置
  contactList: {
    itemHeight: 60,
    bufferSize: 3,
    threshold: 50,
  },
  
  // 文章列表虚拟滚动配置
  articleList: {
    itemHeight: 200,
    bufferSize: 2,
    threshold: 20,
  }
};

// 图片懒加载配置
export const LAZY_LOAD_CONFIG = {
  // 懒加载阈值（距离视口多少像素时开始加载）
  threshold: 200,
  
  // 占位图
  placeholder: '/assets/placeholder.png',
  
  // 错误图
  error: '/assets/error.png',
  
  // 加载动画
  loading: '/assets/loading.gif',
  
  // 观察器选项
  observerOptions: {
    root: null,
    rootMargin: '200px',
    threshold: 0.01
  }
};

// 防抖和节流配置
export const DEBOUNCE_CONFIG = {
  // 搜索输入防抖延迟（毫秒）
  search: 500,
  
  // 窗口resize防抖延迟
  resize: 300,
  
  // 滚动事件节流延迟
  scroll: 100,
  
  // 输入框输入防抖延迟
  input: 300,
  
  // 自动保存防抖延迟
  autoSave: 2000
};

// 代码分割配置
export const CODE_SPLITTING_CONFIG = {
  // 路由级别代码分割
  enableRouteSplitting: true,
  
  // 组件级别代码分割
  enableComponentSplitting: true,
  
  // 预加载策略
  preloadStrategy: 'hover', // 'hover' | 'visible' | 'none'
  
  // 分块大小限制（KB）
  chunkSizeLimit: 500
};

// 缓存配置
export const CACHE_CONFIG = {
  // API响应缓存时间（毫秒）
  apiCache: {
    userInfo: 5 * 60 * 1000,      // 5分钟
    articleList: 3 * 60 * 1000,   // 3分钟
    groupInfo: 10 * 60 * 1000,    // 10分钟
    searchResults: 2 * 60 * 1000  // 2分钟
  },
  
  // 本地存储缓存策略
  localStorage: {
    maxSize: 5 * 1024 * 1024,  // 5MB
    expirationTime: 24 * 60 * 60 * 1000  // 24小时
  },
  
  // SessionStorage缓存策略
  sessionStorage: {
    maxSize: 2 * 1024 * 1024,  // 2MB
    expirationTime: 60 * 60 * 1000  // 1小时
  }
};

// 资源压缩配置
export const COMPRESSION_CONFIG = {
  // 启用gzip压缩
  enableGzip: true,
  
  // gzip压缩阈值（字节）
  gzipThreshold: 10240,  // 10KB
  
  // 图片压缩质量
  imageQuality: 0.8,
  
  // 图片最大尺寸
  maxImageSize: {
    width: 1920,
    height: 1080
  }
};

// 性能监控配置
export const PERFORMANCE_MONITORING = {
  // 启用性能监控
  enabled: true,
  
  // 监控指标
  metrics: {
    // 首次内容绘制（FCP）
    fcp: true,
    
    // 最大内容绘制（LCP）
    lcp: true,
    
    // 首次输入延迟（FID）
    fid: true,
    
    // 累积布局偏移（CLS）
    cls: true,
    
    // 首次字节时间（TTFB）
    ttfb: true
  },
  
  // 性能阈值（毫秒）
  thresholds: {
    fcp: 1800,
    lcp: 2500,
    fid: 100,
    cls: 0.1,
    ttfb: 600
  },
  
  // 采样率（0-1）
  sampleRate: 0.1
};

// 网络优化配置
export const NETWORK_CONFIG = {
  // HTTP请求并发限制
  maxConcurrentRequests: 6,
  
  // 请求超时时间（毫秒）
  timeout: 30000,
  
  // 重试次数
  retryCount: 3,
  
  // 重试延迟（毫秒）
  retryDelay: 1000,
  
  // 启用请求合并
  enableRequestBatching: true,
  
  // 请求合并延迟（毫秒）
  batchingDelay: 50
};

// 渲染优化配置
export const RENDER_CONFIG = {
  // 启用虚拟DOM优化
  enableVirtualDOM: true,
  
  // 启用组件缓存
  enableComponentCache: true,
  
  // 启用计算属性缓存
  enableComputedCache: true,
  
  // 大列表渲染策略
  largeListStrategy: 'virtual-scroll', // 'virtual-scroll' | 'pagination' | 'infinite-scroll'
  
  // 动画性能优化
  animation: {
    // 使用CSS动画而非JS动画
    preferCSS: true,
    
    // 启用硬件加速
    enableHardwareAcceleration: true,
    
    // 降低动画帧率（低性能设备）
    reducedMotion: false
  }
};

// 内存优化配置
export const MEMORY_CONFIG = {
  // 启用内存监控
  enableMonitoring: true,
  
  // 内存警告阈值（MB）
  warningThreshold: 100,
  
  // 内存清理策略
  cleanupStrategy: {
    // 自动清理未使用的缓存
    autoCleanCache: true,
    
    // 清理间隔（毫秒）
    cleanupInterval: 5 * 60 * 1000,  // 5分钟
    
    // 保留最近使用的条目数
    keepRecentItems: 100
  }
};

// 导出所有配置
export default {
  VIRTUAL_SCROLL_CONFIG,
  LAZY_LOAD_CONFIG,
  DEBOUNCE_CONFIG,
  CODE_SPLITTING_CONFIG,
  CACHE_CONFIG,
  COMPRESSION_CONFIG,
  PERFORMANCE_MONITORING,
  NETWORK_CONFIG,
  RENDER_CONFIG,
  MEMORY_CONFIG
};
