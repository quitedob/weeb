import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/authStore'
import { instance } from '../axiosInstance'

// Mock dependencies
vi.mock('element-plus', () => ({
  ElMessage: vi.fn()
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn()
  }
}))

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(() => null), // Default to null
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

describe('axiosInstance', () => {
  let pinia
  let authStore

  beforeEach(() => {
    // Clear all mocks first
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue(null) // Default return value
    localStorageMock.setItem.mockClear()
    localStorageMock.removeItem.mockClear()
    
    // Create fresh Pinia instance for each test
    pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Request Interceptor', () => {
    it('should add Authorization header when token is available in Pinia store', async () => {
      // Setup: Set token in auth store
      authStore.accessToken = 'test-token-from-store'
      
      const config = { headers: {} }
      
      // Get the request interceptor
      const requestInterceptor = instance.interceptors.request.handlers[0]
      const result = await requestInterceptor.fulfilled(config)
      
      expect(result.headers['Authorization']).toBe('Bearer test-token-from-store')
    })

    it('should fallback to localStorage when Pinia store is not available', async () => {
      // Setup: Mock useAuthStore to throw error (simulating Pinia not initialized)
      const originalConsoleWarn = console.warn
      console.warn = vi.fn()
      
      // Mock localStorage to return a token
      localStorageMock.getItem.mockReturnValue('test-token-from-localStorage')
      
      // Temporarily break the store access
      const originalAccessToken = authStore.accessToken
      Object.defineProperty(authStore, 'accessToken', {
        get: () => {
          throw new Error('Pinia not initialized')
        },
        configurable: true
      })
      
      const config = { headers: {} }
      
      const requestInterceptor = instance.interceptors.request.handlers[0]
      const result = await requestInterceptor.fulfilled(config)
      
      expect(localStorageMock.getItem).toHaveBeenCalledWith('jwt_token')
      expect(result.headers['Authorization']).toBe('Bearer test-token-from-localStorage')
      expect(console.warn).toHaveBeenCalledWith(
        'Pinia store not available, falling back to localStorage:',
        expect.any(Error)
      )
      
      // Restore
      Object.defineProperty(authStore, 'accessToken', {
        value: originalAccessToken,
        writable: true,
        configurable: true
      })
      console.warn = originalConsoleWarn
    })

    it('should not add Authorization header when no token is available', async () => {
      // Setup: No token in store or localStorage
      authStore.accessToken = null
      localStorageMock.getItem.mockReturnValue(null)
      
      const config = { headers: {} }
      
      const requestInterceptor = instance.interceptors.request.handlers[0]
      const result = await requestInterceptor.fulfilled(config)
      
      expect(result.headers['Authorization']).toBeUndefined()
    })

    it('should handle request errors properly', async () => {
      const originalConsoleError = console.error
      console.error = vi.fn()
      
      const error = new Error('Request failed')
      const requestInterceptor = instance.interceptors.request.handlers[0]
      
      await expect(requestInterceptor.rejected(error)).rejects.toThrow('Request failed')
      expect(console.error).toHaveBeenCalledWith('Request Error Interceptor:', error)
      
      console.error = originalConsoleError
    })
  })

  describe('Response Interceptor', () => {
    it('should handle successful ApiResponse format', async () => {
      const response = {
        data: {
          code: 0,
          message: 'Success',
          data: { id: 1, name: 'test' }
        }
      }
      
      const responseInterceptor = instance.interceptors.response.handlers[0]
      const result = await responseInterceptor.fulfilled(response)
      
      expect(result).toEqual(response.data)
    })

    it('should handle token expiration (code: -1) and trigger logout', async () => {
      const { ElMessage } = await import('element-plus')
      const router = (await import('@/router')).default
      
      // Mock the logout function on the store
      authStore.logout = vi.fn()
      
      // Use ResultUtil format (without 'message' field) to trigger the second condition
      const response = {
        data: {
          code: -1,
          msg: 'Token expired'
        }
      }
      
      const responseInterceptor = instance.interceptors.response.handlers[0]
      
      await expect(responseInterceptor.fulfilled(response)).rejects.toThrow('Token expired')
      expect(ElMessage).toHaveBeenCalledWith({
        message: 'Token expired',
        type: 'error',
        duration: 5000
      })
      expect(authStore.logout).toHaveBeenCalled()
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('should handle 401 errors and trigger logout', async () => {
      const { ElMessage } = await import('element-plus')
      const router = (await import('@/router')).default
      
      authStore.logout = vi.fn()
      
      const error = {
        response: {
          status: 401,
          data: { message: 'Unauthorized' }
        },
        message: 'Request failed with status code 401'
      }
      
      const responseInterceptor = instance.interceptors.response.handlers[0]
      
      await expect(responseInterceptor.rejected(error)).rejects.toThrow()
      expect(ElMessage).toHaveBeenCalledWith({
        message: 'Unauthorized',
        type: 'error',
        duration: 5000
      })
      expect(authStore.logout).toHaveBeenCalled()
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('should fallback to localStorage cleanup when store is not available', async () => {
      const originalConsoleWarn = console.warn
      console.warn = vi.fn()
      
      // Create a new Pinia instance that will throw when useAuthStore is called
      const brokenPinia = createPinia()
      setActivePinia(brokenPinia)
      
      // Mock useAuthStore to throw an error by overriding the store definition
      vi.doMock('@/stores/authStore', () => ({
        useAuthStore: () => {
          throw new Error('Store not available')
        }
      }))
      
      const response = {
        data: {
          code: -1,
          msg: 'Token expired'
        }
      }
      
      const responseInterceptor = instance.interceptors.response.handlers[0]
      
      await expect(responseInterceptor.fulfilled(response)).rejects.toThrow('Token expired')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('jwt_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('currentUser')
      expect(console.warn).toHaveBeenCalledWith(
        'useAuthStore not available in response interceptor for logout:',
        expect.any(Error)
      )
      
      console.warn = originalConsoleWarn
      vi.doUnmock('@/stores/authStore')
      
      // Restore original Pinia
      setActivePinia(pinia)
    })
  })
})