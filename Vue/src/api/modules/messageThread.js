/**
 * Message Thread API
 * Handles threaded message operations
 */

import api from '../axiosInstance'

export const messageThreadApi = {
  /**
   * Get messages for a specific thread
   * 统一使用 /api/threads/* 路径
   * @param {number} threadId - The thread ID
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 20)
   * @returns {Promise} API response
   */
  getThreadMessages(threadId, page = 1, pageSize = 20) {
    return api.get(`/api/threads/${threadId}/messages`, {
      params: {
        page,
        pageSize
      }
    })
  },

  /**
   * Get thread summary information
   * @param {number} parentMessageId - The parent message ID
   * @returns {Promise} API response
   */
  getThreadSummary(parentMessageId) {
    return api.get(`/api/threads/summary/${parentMessageId}`)
  },

  /**
   * Create a new thread from a message
   * @param {number} parentMessageId - The parent message ID
   * @param {string} content - Thread message content
   * @returns {Promise} API response
   */
  createThread(parentMessageId, content) {
    return api.post('/api/threads', {
      parentMessageId,
      content
    })
  },

  /**
   * Reply to a message in a thread
   * @param {number} threadId - The thread ID
   * @param {string} content - Reply content
   * @param {number} replyToMessageId - The message being replied to (optional)
   * @returns {Promise} API response
   */
  replyToThread(threadId, content, replyToMessageId = null) {
    return api.post(`/api/threads/${threadId}/reply`, {
      content,
      replyToMessageId
    })
  },

  /**
   * Delete a thread
   * @param {number} threadId - The thread ID
   * @returns {Promise} API response
   */
  deleteThread(threadId) {
    return api.delete(`/api/threads/${threadId}`)
  },

  /**
   * Get user's participated threads
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 20)
   * @returns {Promise} API response
   */
  getUserThreads(page = 1, pageSize = 20) {
    return api.get('/api/threads/user', {
      params: {
        page,
        pageSize
      }
    })
  },

  /**
   * Mark thread messages as read
   * @param {number} threadId - The thread ID
   * @returns {Promise} API response
   */
  markThreadAsRead(threadId) {
    return api.post(`/api/threads/${threadId}/read`)
  },

  /**
   * Get unread thread count
   * @returns {Promise} API response
   */
  getUnreadThreadCount() {
    return api.get('/api/threads/unread-count')
  }
}

export default messageThreadApi