/**
 * 日志管理工具
 * 在开发环境提供详细日志，生产环境自动禁用
 */

const isDevelopment = import.meta.env.MODE === 'development'

class Logger {
  constructor() {
    this.enabled = isDevelopment
  }

  debug(...args) {
    if (this.enabled) {
      console.debug('[DEBUG]', ...args)
    }
  }

  info(...args) {
    if (this.enabled) {
      console.info('[INFO]', ...args)
    }
  }

  warn(...args) {
    if (this.enabled) {
      console.warn('[WARN]', ...args)
    }
  }

  error(...args) {
    // 错误日志在所有环境都保留
    console.error('[ERROR]', ...args)
  }

  group(label) {
    if (this.enabled) {
      console.group(label)
    }
  }

  groupEnd() {
    if (this.enabled) {
      console.groupEnd()
    }
  }

  time(label) {
    if (this.enabled) {
      console.time(label)
    }
  }

  timeEnd(label) {
    if (this.enabled) {
      console.timeEnd(label)
    }
  }

  table(data) {
    if (this.enabled) {
      console.table(data)
    }
  }
}

// 创建全局logger实例
export const logger = new Logger()

// 提供一些便捷方法
export const log = {
  debug: logger.debug.bind(logger),
  info: logger.info.bind(logger),
  warn: logger.warn.bind(logger),
  error: logger.error.bind(logger),
  group: logger.group.bind(logger),
  groupEnd: logger.groupEnd.bind(logger),
  time: logger.time.bind(logger),
  timeEnd: logger.timeEnd.bind(logger),
  table: logger.table.bind(logger)
}

export default logger