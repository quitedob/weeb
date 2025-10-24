/**
 * Message Thread API
 * Handles threaded message operations
 */

import api from '../axiosInstance'

export const messageThreadApi = {
  /**
   * Get messages for a specific thread
   * @param {number} threadId - The thread ID
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 20)
   * @returns {Promise} API response
   */
  getThreadMessages(threadId, page = 1, pageSize = 20) {
    return api.get(`/api/messages/thread/${threadId}`, {
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
    return api.get(`/api/messages/thread-summary/${parentMessageId}`)
  },

  /**
   * Create a new thread from a message
   * @param {number} parentMessageId - The parent message ID
   * @param {string} content - Thread message content
   * @returns {Promise} API response
   */
  createThread(parentMessageId, content) {
    return api.post('/api/messages/create-thread', {
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
    return api.post('/api/messages/reply-thread', {
      threadId,
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
    return api.delete(`/api/messages/thread/${threadId}`)
  },

  /**
   * Get user's participated threads
   * @param {number} page - Page number (default: 1)
   * @param {number} pageSize - Page size (default: 20)
   * @returns {Promise} API response
   */
  getUserThreads(page = 1, pageSize = 20) {
    return api.get('/api/messages/user-threads', {
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
    return api.post(`/api/messages/thread/${threadId}/read`)
  },

  /**
   * Get unread thread count
   * @returns {Promise} API response
   */
  getUnreadThreadCount() {
    return api.get('/api/messages/unread-threads-count')
  }
}

export default messageThreadApi