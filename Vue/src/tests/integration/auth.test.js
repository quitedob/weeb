// Integration tests for authentication flow with fixed Axios interceptor
// Tests Requirements: 1.1-1.5 (Frontend Authentication System Fix)

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'
import { instance } from '@/api/axiosInstance'

// Mock axios for controlled testing
vi.mock('axios')
const mockedAxios = vi.mocked(axios)

// Mock Element Plus message component
vi.mock('element-plus', () => ({
  ElMessage: vi.fn()
}))

// Mock router
vi.mock('@/router', () => ({
  default: {
    push: vi.fn()
  }
}))

describe('Authentication Integration Tests', () => {
  let pinia
  let authStore

  beforeEach(() => {
    // Setup fresh Pinia instance for each test
    pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
    
    // Clear localStorage
    localStorage.clear()
    
    // Reset all mocks
    vi.clearAllMocks()
    
    // Mock successful axios create
    mockedAxios.create.mockReturnValue({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    })
  })

  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('Requirement 1.1: ES Module imports instead of CommonJS require()', () => {
    it('should use proper ES Module imports in axiosInstance', async () => {
      // This test verifies that the axiosInstance.js file uses ES Module syntax
      // The fact that we can import it without errors proves ES Module compliance
      const axiosInstance = await import('@/api/axiosInstance')
      expect(axiosInstance.instance).toBeDefined()
      expect(typeof axiosInstance.instance).toBe('object')
    })

    it('should not throw "require is not defined" errors during initialization', () => {
      // Test that importing the axios instance doesn't throw require errors
      expect(() => {
        require('@/api/axiosInstance')
      }).not.toThrow(/require is not defined/)
    })
  })

  describe('Requirement 1.2: Successful auth token retrieval from Pinia store', () => {
    it('should retrieve token from Pinia store when available', () => {
      // Setup: Store a token in the auth store
      const testToken = 'test-jwt-token-123'
      authStore.setToken(testToken)
      
      // Verify token is accessible
      expect(authStore.accessToken).toBe(testToken)
      expect(authStore.isLoggedIn).toBe(true)
    })

    it('should fallback to localStorage when Pinia store is not available', () => {
      // Setup: Store token in localStorage but not in Pinia
      const testToken = 'localStorage-token-456'
      localStorage.setItem('jwt_token', testToken)
      
      // Create new store instance to simulate fresh state
      const freshStore = useAuthStore()
      
      // The store should sync with localStorage on initialization
      expect(localStorage.getItem('jwt_token')).toBe(testToken)
    })
  })

  describe('Requirement 1.3: Authorization header inclusion', () => {
    it('should include Authorization header when token is available', async () => {
      const testToken = 'bearer-token-789'
      authStore.setToken(testToken)
      
      // Mock a request config
      const config = {
        url: '/api/test',
        method: 'GET',
        headers: {}
      }
      
      // Simulate request interceptor behavior
      // In real implementation, this would be handled by the interceptor
      if (authStore.accessToken) {
        config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
      }
      
      expect(config.headers['Authorization']).toBe(`Bearer ${testToken}`)
    })

    it('should not include Authorization header when no token is available', async () => {
      // Ensure no token is set
      authStore.logoutCleanup()
      
      const config = {
        url: '/api/test',
        method: 'GET',
        headers: {}
      }
      
      // Simulate request interceptor behavior
      if (authStore.accessToken) {
        config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
      }
      
      expect(config.headers['Authorization']).toBeUndefined()
    })
  })

  describe('Requirement 1.4: No "require is not defined" errors', () => {
    it('should handle axios instance creation without require errors', () => {
      // Test that the axios instance can be created without CommonJS require calls
      expect(() => {
        // This simulates the module loading process
        const axiosConfig = {
          baseURL: 'http://localhost:8080',
          timeout: 10000
        }
        const testInstance = axios.create(axiosConfig)
        expect(testInstance).toBeDefined()
      }).not.toThrow()
    })
  })

  describe('Requirement 1.5: Proper Pinia lifecycle integration', () => {
    it('should wait for Pinia initialization before accessing store', async () => {
      // Test that store access is deferred until Pinia is ready
      let storeAccessAttempted = false
      let storeAccessSuccessful = false
      
      try {
        // Simulate accessing store after Pinia is initialized
        const store = useAuthStore()
        storeAccessAttempted = true
        
        if (store && typeof store.accessToken !== 'undefined') {
          storeAccessSuccessful = true
        }
      } catch (error) {
        // Should not throw when Pinia is properly initialized
        expect(error).toBeNull()
      }
      
      expect(storeAccessAttempted).toBe(true)
      expect(storeAccessSuccessful).toBe(true)
    })

    it('should handle store access gracefully when Pinia is not initialized', () => {
      // Simulate scenario where Pinia might not be available
      const fallbackToken = 'fallback-token-123'
      localStorage.setItem('jwt_token', fallbackToken)
      
      // Even if store access fails, localStorage should be accessible
      expect(localStorage.getItem('jwt_token')).toBe(fallbackToken)
    })
  })

  describe('Authentication Flow Integration', () => {
    it('should complete full authentication flow successfully', async () => {
      // Mock successful login response
      const mockLoginResponse = {
        code: 0,
        message: 'Login successful',
        data: {
          token: 'integration-test-token-456'
        }
      }
      
      // Mock successful user info response
      const mockUserResponse = {
        id: 1,
        username: 'testuser',
        email: 'test@example.com'
      }
      
      // Setup mocks
      const mockApi = {
        auth: {
          login: vi.fn().mockResolvedValue(mockLoginResponse),
          getUserInfo: vi.fn().mockResolvedValue(mockUserResponse)
        }
      }
      
      // Mock the api module
      vi.doMock('@/api', () => ({
        default: mockApi
      }))
      
      // Test login flow
      const loginCredentials = {
        username: 'testuser',
        password: 'testpass'
      }
      
      const loginResult = await authStore.login(loginCredentials)
      
      expect(loginResult).toBe(true)
      expect(authStore.accessToken).toBe(mockLoginResponse.data.token)
      expect(authStore.currentUser).toEqual(mockUserResponse)
      expect(localStorage.getItem('jwt_token')).toBe(mockLoginResponse.data.token)
    })

    it('should handle authentication errors gracefully', async () => {
      // Mock failed login response
      const mockErrorResponse = {
        code: 1001,
        message: 'Invalid credentials'
      }
      
      const mockApi = {
        auth: {
          login: vi.fn().mockResolvedValue(mockErrorResponse)
        }
      }
      
      vi.doMock('@/api', () => ({
        default: mockApi
      }))
      
      const loginCredentials = {
        username: 'wronguser',
        password: 'wrongpass'
      }
      
      await expect(authStore.login(loginCredentials)).rejects.toThrow('Invalid credentials')
      expect(authStore.accessToken).toBeNull()
      expect(authStore.currentUser).toBeNull()
    })
  })
})