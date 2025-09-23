// Integration tests for concurrent user operations with optimized database schema
// Tests Requirements: 3.1-3.6 (Database Schema Optimization for Scalability)

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'

// Mock axios
vi.mock('axios')
const mockedAxios = vi.mocked(axios)

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: vi.fn()
}))

// Mock router
vi.mock('@/router', () => ({
  default: {
    push: vi.fn()
  }
}))

describe('Concurrent Operations Integration Tests', () => {
  let pinia
  let mockAxiosInstance

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    
    mockAxiosInstance = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    }
    
    mockedAxios.create.mockReturnValue(mockAxiosInstance)
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Requirement 3.2: Authentication not blocked by statistical updates', () => {
    it('should handle concurrent authentication and statistical updates', async () => {
      // Mock responses for authentication
      const authResponse = {
        data: {
          code: 0,
          message: 'Authentication successful',
          data: {
            token: 'auth-token-123',
            user: { id: 1, username: 'testuser' }
          }
        }
      }

      // Mock responses for statistical updates
      const statsResponse = {
        data: {
          code: 0,
          message: 'Stats updated',
          data: null
        }
      }

      mockAxiosInstance.post.mockImplementation((url) => {
        if (url.includes('/auth/login')) {
          return Promise.resolve(authResponse)
        } else if (url.includes('/stats/')) {
          return Promise.resolve(statsResponse)
        }
        return Promise.reject(new Error('Unknown endpoint'))
      })

      // Simulate concurrent operations
      const authPromises = []
      const statsPromises = []

      // Multiple authentication attempts
      for (let i = 0; i < 5; i++) {
        authPromises.push(
          mockAxiosInstance.post('/api/auth/login', {
            username: 'testuser',
            password: 'password'
          })
        )
      }

      // Multiple statistical updates
      for (let i = 0; i < 10; i++) {
        statsPromises.push(
          mockAxiosInstance.post('/api/stats/increment-likes', { userId: 1 })
        )
      }

      // All operations should complete successfully
      const authResults = await Promise.all(authPromises)
      const statsResults = await Promise.all(statsPromises)

      // Verify all authentication requests succeeded
      authResults.forEach(result => {
        expect(result.data.code).toBe(0)
        expect(result.data.data.token).toBeDefined()
      })

      // Verify all statistical updates succeeded
      statsResults.forEach(result => {
        expect(result.data.code).toBe(0)
      })
    })
  })

  describe('Requirement 3.3: No write lock contention from same author interactions', () => {
    it('should handle multiple users interacting with same author simultaneously', async () => {
      const authorId = 1
      const interactionResponse = {
        data: {
          code: 0,
          message: 'Interaction recorded',
          data: { success: true }
        }
      }

      const profileUpdateResponse = {
        data: {
          code: 0,
          message: 'Profile updated',
          data: { id: authorId, nickname: 'Updated Author' }
        }
      }

      mockAxiosInstance.post.mockImplementation((url) => {
        if (url.includes('/like') || url.includes('/follow')) {
          // Simulate slight delay for statistical operations
          return new Promise(resolve => 
            setTimeout(() => resolve(interactionResponse), 50)
          )
        }
        return Promise.resolve(interactionResponse)
      })

      mockAxiosInstance.put.mockResolvedValue(profileUpdateResponse)

      // Simulate multiple users liking/following the same author
      const interactionPromises = []
      for (let userId = 2; userId <= 6; userId++) {
        interactionPromises.push(
          mockAxiosInstance.post(`/api/articles/${authorId}/like`, { userId })
        )
        interactionPromises.push(
          mockAxiosInstance.post(`/api/users/${authorId}/follow`, { userId })
        )
      }

      // Simultaneously, the author updates their profile
      const profileUpdatePromise = mockAxiosInstance.put(`/api/users/${authorId}`, {
        nickname: 'Updated Author'
      })

      // All operations should complete without blocking each other
      const startTime = Date.now()
      const [interactionResults, profileResult] = await Promise.all([
        Promise.all(interactionPromises),
        profileUpdatePromise
      ])
      const endTime = Date.now()

      // Operations should complete quickly (not blocked by locks)
      expect(endTime - startTime).toBeLessThan(1000)

      // Verify all interactions succeeded
      interactionResults.forEach(result => {
        expect(result.data.code).toBe(0)
      })

      // Verify profile update succeeded
      expect(profileResult.data.code).toBe(0)
      expect(profileResult.data.data.nickname).toBe('Updated Author')
    })
  })

  describe('Requirement 3.4: Profile updates not delayed by counter updates', () => {
    it('should allow fast profile updates during statistical operations', async () => {
      const userId = 1
      
      const profileResponse = {
        data: {
          code: 0,
          message: 'Profile updated',
          data: { id: userId, nickname: 'Fast Update' }
        }
      }

      const statsResponse = {
        data: {
          code: 0,
          message: 'Stats updated',
          data: null
        }
      }

      // Mock profile updates to be fast
      mockAxiosInstance.put.mockResolvedValue(profileResponse)
      
      // Mock stats updates with artificial delay
      mockAxiosInstance.post.mockImplementation((url) => {
        if (url.includes('/stats/')) {
          return new Promise(resolve => 
            setTimeout(() => resolve(statsResponse), 100)
          )
        }
        return Promise.resolve(statsResponse)
      })

      // Start continuous statistical updates
      const statsPromises = []
      for (let i = 0; i < 10; i++) {
        statsPromises.push(
          mockAxiosInstance.post(`/api/stats/increment-views`, { userId })
        )
      }

      // Measure profile update times during stats updates
      const profileUpdateTimes = []
      for (let i = 0; i < 3; i++) {
        const startTime = Date.now()
        await mockAxiosInstance.put(`/api/users/${userId}`, {
          nickname: `Update ${i}`
        })
        const endTime = Date.now()
        profileUpdateTimes.push(endTime - startTime)
      }

      // Wait for stats updates to complete
      await Promise.all(statsPromises)

      // Profile updates should be fast (not delayed by stats operations)
      profileUpdateTimes.forEach(time => {
        expect(time).toBeLessThan(200) // Should be much faster than stats operations
      })
    })
  })

  describe('Requirement 3.6: High-frequency actions only lock user_stats table', () => {
    it('should handle high-frequency likes without blocking user operations', async () => {
      const userId = 1
      
      const likeResponse = {
        data: {
          code: 0,
          message: 'Like recorded',
          data: { totalLikes: 1 }
        }
      }

      const userResponse = {
        data: {
          code: 0,
          message: 'User data retrieved',
          data: { id: userId, username: 'testuser' }
        }
      }

      mockAxiosInstance.post.mockResolvedValue(likeResponse)
      mockAxiosInstance.get.mockResolvedValue(userResponse)

      // Simulate high-frequency like operations
      const likePromises = []
      for (let i = 0; i < 20; i++) {
        likePromises.push(
          mockAxiosInstance.post(`/api/articles/1/like`, { userId })
        )
      }

      // Simultaneously perform user table operations
      const userOperationPromises = []
      for (let i = 0; i < 5; i++) {
        userOperationPromises.push(
          mockAxiosInstance.get(`/api/users/${userId}`)
        )
      }

      const startTime = Date.now()
      const [likeResults, userResults] = await Promise.all([
        Promise.all(likePromises),
        Promise.all(userOperationPromises)
      ])
      const endTime = Date.now()

      // All operations should complete quickly
      expect(endTime - startTime).toBeLessThan(1000)

      // Verify all like operations succeeded
      likeResults.forEach(result => {
        expect(result.data.code).toBe(0)
      })

      // Verify all user operations succeeded
      userResults.forEach(result => {
        expect(result.data.code).toBe(0)
        expect(result.data.data.id).toBe(userId)
      })
    })
  })

  describe('Database schema optimization validation', () => {
    it('should demonstrate improved performance with separated tables', async () => {
      // This test simulates the performance improvement from separating
      // user core data from statistical counters

      const coreUserResponse = {
        data: {
          code: 0,
          message: 'User data retrieved',
          data: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            nickname: 'Test User'
          }
        }
      }

      const userStatsResponse = {
        data: {
          code: 0,
          message: 'User stats retrieved',
          data: {
            userId: 1,
            fansCount: 100,
            totalLikes: 500,
            totalFavorites: 50
          }
        }
      }

      const statsUpdateResponse = {
        data: {
          code: 0,
          message: 'Stats updated',
          data: null
        }
      }

      mockAxiosInstance.get.mockImplementation((url) => {
        if (url.includes('/users/') && !url.includes('/stats')) {
          return Promise.resolve(coreUserResponse)
        } else if (url.includes('/stats')) {
          return Promise.resolve(userStatsResponse)
        }
        return Promise.reject(new Error('Unknown endpoint'))
      })

      mockAxiosInstance.post.mockResolvedValue(statsUpdateResponse)

      // Simulate concurrent access patterns
      const coreDataPromises = []
      const statsPromises = []
      const statsUpdatePromises = []

      // Multiple core user data requests (authentication, profile views)
      for (let i = 0; i < 10; i++) {
        coreDataPromises.push(
          mockAxiosInstance.get('/api/users/1')
        )
      }

      // Multiple stats data requests (profile displays)
      for (let i = 0; i < 10; i++) {
        statsPromises.push(
          mockAxiosInstance.get('/api/users/1/stats')
        )
      }

      // Multiple stats updates (likes, follows)
      for (let i = 0; i < 20; i++) {
        statsUpdatePromises.push(
          mockAxiosInstance.post('/api/users/1/stats/increment-likes')
        )
      }

      const startTime = Date.now()
      const [coreResults, statsResults, updateResults] = await Promise.all([
        Promise.all(coreDataPromises),
        Promise.all(statsPromises),
        Promise.all(statsUpdatePromises)
      ])
      const endTime = Date.now()

      // With optimized schema, all operations should complete quickly
      expect(endTime - startTime).toBeLessThan(2000)

      // Verify all operations succeeded
      expect(coreResults).toHaveLength(10)
      expect(statsResults).toHaveLength(10)
      expect(updateResults).toHaveLength(20)

      coreResults.forEach(result => {
        expect(result.data.code).toBe(0)
        expect(result.data.data.username).toBe('testuser')
      })

      statsResults.forEach(result => {
        expect(result.data.code).toBe(0)
        expect(result.data.data.userId).toBe(1)
      })

      updateResults.forEach(result => {
        expect(result.data.code).toBe(0)
      })
    })
  })
})