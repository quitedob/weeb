/**
 * 消息状态工具类
 * 统一管理消息状态的定义和转换
 * 与后端Message.java中的状态定义保持一致
 */

// 消息状态常量（与后端Message.java保持一致）
export const MESSAGE_STATUS = {
  SENDING: 0,    // 发送中
  SENT: 1,       // 已发送
  DELIVERED: 2,  // 已送达
  READ: 3,       // 已读
  FAILED: 4      // 失败
};

// 状态文本映射
export const STATUS_TEXT = {
  [MESSAGE_STATUS.SENDING]: '发送中',
  [MESSAGE_STATUS.SENT]: '已发送',
  [MESSAGE_STATUS.DELIVERED]: '已送达',
  [MESSAGE_STATUS.READ]: '已读',
  [MESSAGE_STATUS.FAILED]: '发送失败'
};

// 状态英文映射（用于前端内部）
export const STATUS_TEXT_EN = {
  [MESSAGE_STATUS.SENDING]: 'sending',
  [MESSAGE_STATUS.SENT]: 'sent',
  [MESSAGE_STATUS.DELIVERED]: 'delivered',
  [MESSAGE_STATUS.READ]: 'read',
  [MESSAGE_STATUS.FAILED]: 'failed'
};

/**
 * 从英文状态文本转换为状态码
 * @param {string} statusText - 英文状态文本
 * @returns {number} 状态码
 */
export function parseStatusText(statusText) {
  if (!statusText) return MESSAGE_STATUS.SENT;
  
  const lowerText = statusText.toLowerCase();
  switch (lowerText) {
    case 'sending':
      return MESSAGE_STATUS.SENDING;
    case 'sent':
      return MESSAGE_STATUS.SENT;
    case 'delivered':
      return MESSAGE_STATUS.DELIVERED;
    case 'read':
      return MESSAGE_STATUS.READ;
    case 'failed':
      return MESSAGE_STATUS.FAILED;
    default:
      return MESSAGE_STATUS.SENT;
  }
}

/**
 * 从状态码转换为英文状态文本
 * @param {number} status - 状态码
 * @returns {string} 英文状态文本
 */
export function getStatusTextEn(status) {
  return STATUS_TEXT_EN[status] || 'sent';
}

/**
 * 从状态码转换为中文状态文本
 * @param {number} status - 状态码
 * @returns {string} 中文状态文本
 */
export function getStatusText(status) {
  return STATUS_TEXT[status] || '已发送';
}

/**
 * 检查消息是否发送成功
 * @param {number} status - 状态码
 * @returns {boolean}
 */
export function isMessageSent(status) {
  return status >= MESSAGE_STATUS.SENT && status <= MESSAGE_STATUS.READ;
}

/**
 * 检查消息是否失败
 * @param {number} status - 状态码
 * @returns {boolean}
 */
export function isMessageFailed(status) {
  return status === MESSAGE_STATUS.FAILED;
}

/**
 * 检查消息是否正在发送
 * @param {number} status - 状态码
 * @returns {boolean}
 */
export function isMessageSending(status) {
  return status === MESSAGE_STATUS.SENDING;
}

/**
 * 检查消息是否已读
 * @param {number} status - 状态码
 * @returns {boolean}
 */
export function isMessageRead(status) {
  return status === MESSAGE_STATUS.READ;
}

/**
 * 标准化消息对象，确保包含status字段
 * @param {Object} message - 消息对象
 * @returns {Object} 标准化后的消息对象
 */
export function normalizeMessage(message) {
  if (!message) return null;
  
  // 如果已有status字段，直接返回
  if (typeof message.status === 'number') {
    return message;
  }
  
  // 兼容旧的readStatus和isRecalled字段
  if (message.isRecalled === 1) {
    // 撤回的消息保持原状态
    message.status = message.status || MESSAGE_STATUS.SENT;
  } else if (message.readStatus === 1 || message.isRead === 1) {
    message.status = MESSAGE_STATUS.READ;
  } else {
    message.status = MESSAGE_STATUS.SENT;
  }
  
  return message;
}

/**
 * 批量标准化消息列表
 * @param {Array} messages - 消息列表
 * @returns {Array} 标准化后的消息列表
 */
export function normalizeMessages(messages) {
  if (!Array.isArray(messages)) return [];
  return messages.map(normalizeMessage).filter(Boolean);
}

/**
 * 更新消息状态
 * @param {Object} message - 消息对象
 * @param {number} newStatus - 新状态
 * @returns {Object} 更新后的消息对象
 */
export function updateMessageStatus(message, newStatus) {
  if (!message) return null;
  
  message.status = newStatus;
  
  // 同步更新旧字段（向后兼容）
  if (newStatus === MESSAGE_STATUS.READ) {
    message.readStatus = 1;
    message.isRead = 1;
  } else {
    message.readStatus = 0;
    message.isRead = 0;
  }
  
  return message;
}

export default {
  MESSAGE_STATUS,
  STATUS_TEXT,
  STATUS_TEXT_EN,
  parseStatusText,
  getStatusTextEn,
  getStatusText,
  isMessageSent,
  isMessageFailed,
  isMessageSending,
  isMessageRead,
  normalizeMessage,
  normalizeMessages,
  updateMessageStatus
};
