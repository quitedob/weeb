import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
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
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

describe('Authentication Integration', () => {
  let pinia
  let authStore

  beforeEach(() => {
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue(null)
    
    pinia = createPinia()
    setActivePinia(pinia)
    authStore = useAuthStore()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should successfully authenticate and make API calls', async () => {
    // Setup: Set token in auth store
    authStore.setToken('test-auth-token')
    
    // Create a mock request config
    const config = { 
      url: '/api/test',
      method: 'GET',
      headers: {} 
    }
    
    // Get the request interceptor and apply it
    const requestInterceptor = instance.interceptors.request.handlers[0]
    const processedConfig = await requestInterceptor.fulfilled(config)
    
    // Verify that the Authorization header was added
    expect(processedConfig.headers['Authorization']).toBe('Bearer test-auth-token')
    expect(processedConfig.url).toBe('/api/test')
    expect(processedConfig.method).toBe('GET')
  })

  it('should handle authentication flow with localStorage fallback', async () => {
    // Setup: Clear store but set token in localStorage
    authStore.accessToken = null
    localStorageMock.getItem.mockImplementation((key) => {
      if (key === 'jwt_token') return 'localStorage-token'
      return null
    })
    
    // Simulate Pinia not being available by temporarily breaking store access
    const originalAccessToken = authStore.accessToken
    Object.defineProperty(authStore, 'accessToken', {
      get: () => {
        throw new Error('Pinia not initialized')
      },
      configurable: true
    })
    
    const config = { headers: {} }
    
    const requestInterceptor = instance.interceptors.request.handlers[0]
    const processedConfig = await requestInterceptor.fulfilled(config)
    
    // Should fallback to localStorage
    expect(localStorageMock.getItem).toHaveBeenCalledWith('jwt_token')
    expect(processedConfig.headers['Authorization']).toBe('Bearer localStorage-token')
    
    // Restore
    Object.defineProperty(authStore, 'accessToken', {
      value: originalAccessToken,
      writable: true,
      configurable: true
    })
  })

  it('should handle successful API responses correctly', async () => {
    const successResponse = {
      data: {
        code: 0,
        message: 'Success',
        data: { id: 1, name: 'Test User' }
      }
    }
    
    const responseInterceptor = instance.interceptors.response.handlers[0]
    const result = await responseInterceptor.fulfilled(successResponse)
    
    expect(result).toEqual(successResponse.data)
    expect(result.code).toBe(0)
    expect(result.data.name).toBe('Test User')
  })

  it('should validate that interceptors are properly configured', () => {
    // Verify that interceptors are registered
    expect(instance.interceptors.request.handlers).toHaveLength(1)
    expect(instance.interceptors.response.handlers).toHaveLength(1)
    
    // Verify interceptor functions exist
    const requestInterceptor = instance.interceptors.request.handlers[0]
    const responseInterceptor = instance.interceptors.response.handlers[0]
    
    expect(typeof requestInterceptor.fulfilled).toBe('function')
    expect(typeof requestInterceptor.rejected).toBe('function')
    expect(typeof responseInterceptor.fulfilled).toBe('function')
    expect(typeof responseInterceptor.rejected).toBe('function')
  })
})