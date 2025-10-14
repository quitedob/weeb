/**
 * 简单的UUID生成工具
 * 用于生成客户端临时消息ID，确保消息关联的准确性
 */

/**
 * 生成UUID v4格式的字符串
 * @returns {string} UUID字符串
 */
export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 生成短UUID（用于消息临时ID）
 * @returns {string} 短UUID字符串
 */
export function generateShortUUID() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

export default {
  generateUUID,
  generateShortUUID
}