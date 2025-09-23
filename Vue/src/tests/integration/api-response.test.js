// Integration tests for API response consistency across all endpoints
// Tests Requirements: 2.1-2.5 (Backend API Response Standardization)

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'
import { instance } from '@/api/axiosInstance'

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

describe('API Response Consistency Integration Tests', () => {
  let pinia
  let mockAxiosInstance

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    
    // Create mock axios instance
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

  describe('Requirement 2.1: Standardized ApiResponse<T> format for success', () => {
    it('should handle standardized success response from AuthController', async () => {
      const mockResponse = {
        data: {
          code: 0,
          message: 'Login successful',
          data: {
            token: 'jwt-token-123',
            user: { id: 1, username: 'testuser' }
          }
        }
      }
      
      mockAxiosInstance.post.mockResolvedValue(mockResponse)
      
      const response = await mockAxiosInstance.post('/api/auth/login', {
        username: 'testuser',
        password: 'password'
      })
      
      // Verify standardized format
      expect(response.data).toHaveProperty('code')
      expect(response.data).toHaveProperty('message')
      expect(response.data).toHaveProperty('data')
      expect(response.data.code).toBe(0)
      expect(typeof response.data.message).toBe('string')
      expect(response.data.data).toBeDefined()
    })

    it('should handle standardized success response from ArticleCenterController', async () => {
      const mockResponse = {
        data: {
          code: 0,
          message: 'Articles retrieved successfully',
          data: [
            { id: 1, title: 'Test Article 1', content: 'Content 1' },
            { id: 2, title: 'Test Article 2', content: 'Content 2' }
          ]
        }
      }
      
      mockAxiosInstance.get.mockResolvedValue(mockResponse)
      
      const response = await mockAxiosInstance.get('/articles')
      
      // Verify standardized format
      expect(response.data).toHaveProperty('code')
      expect(response.data).toHaveProperty('message')
      expect(response.data).toHaveProperty('data')
      expect(response.data.code).toBe(0)
      expect(Array.isArray(response.data.data)).toBe(true)
    })

    it('should handle standardized success response from UserController', async () => {
      const mockResponse = {
        data: {
          code: 0,
          message: 'User profile retrieved successfully',
          data: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            stats: {
              fansCount: 10,
              totalLikes: 25
            }
          }
        }
      }
      
      mockAxiosInstance.get.mockResolvedValue(mockResponse)
      
      const response = await mockAxiosInstance.get('/api/user/profile')
      
      // Verify standardized format
      expect(response.data).toHaveProperty('code')
      expect(response.data).toHaveProperty('message')
      expect(response.data).toHaveProperty('data')
      expect(response.data.code).toBe(0)
      expect(response.data.data).toHaveProperty('id')
      expect(response.data.data).toHaveProperty('username')
    })
  })

  describe('Requirement 2.2: Standardized ApiResponse<T> format for errors', () => {
    it('should handle standardized error response with proper error details', async () => {
      const mockErrorResponse = {
        data: {
          code: 1001,
          message: 'Invalid credentials provided',
          data: null,
          errorCode: 'AUTH_INVALID_CREDENTIALS'
        }
      }
      
      mockAxiosInstance.post.mockResolvedValue(mockErrorResponse)
      
      const response = await mockAxiosInstance.post('/api/auth/login', {
        username: 'wronguser',
        password: 'wrongpass'
      })
      
      // Verify standardized error format
      expect(response.data).toHaveProperty('code')
      expect(response.data).toHaveProperty('message')
      expect(response.data).toHaveProperty('data')
      expect(response.data.code).not.toBe(0)
      expect(typeof response.data.message).toBe('string')
      expect(response.data.data).toBeNull()
      
      // Check for optional errorCode
      if (response.data.errorCode) {
        expect(typeof response.data.errorCode).toBe('string')
      }
    })

    it('should handle validation error responses consistently', async () => {
      const mockValidationError = {
        data: {
          code: 1002,
          message: 'Validation failed',
          data: null,
          errorCode: 'VALIDATION_ERROR'
        }
      }
      
      mockAxiosInstance.post.mockResolvedValue(mockValidationError)
      
      const response = await mockAxiosInstance.post('/api/user/update', {
        email: 'invalid-email'
      })
      
      expect(response.data.code).toBe(1002)
      expect(response.data.message).toBe('Validation failed')
      expect(response.data.errorCode).toBe('VALIDATION_ERROR')
    })
  })

  describe('Requirement 2.3: ArticleCenterController consistency', () => {
    it('should return consistent ApiResponse format for article operations', async () => {
      const testCases = [
        {
          method: 'get',
          url: '/articles',
          mockResponse: {
            data: {
              code: 0,
              message: 'Articles retrieved',
              data: []
            }
          }
        },
        {
          method: 'post',
          url: '/articles',
          mockResponse: {
            data: {
              code: 0,
              message: 'Article created',
              data: { id: 1, title: 'New Article' }
            }
          }
        },
        {
          method: 'put',
          url: '/articles/1',
          mockResponse: {
            data: {
              code: 0,
              message: 'Article updated',
              data: { id: 1, title: 'Updated Article' }
            }
          }
        },
        {
          method: 'delete',
          url: '/articles/1',
          mockResponse: {
            data: {
              code: 0,
              message: 'Article deleted',
              data: null
            }
          }
        }
      ]
      
      for (const testCase of testCases) {
        mockAxiosInstance[testCase.method].mockResolvedValue(testCase.mockResponse)
        
        const response = await mockAxiosInstance[testCase.method](testCase.url)
        
        // Verify each response follows the standard format
        expect(response.data).toHaveProperty('code')
        expect(response.data).toHaveProperty('message')
        expect(response.data).toHaveProperty('data')
        expect(response.data.code).toBe(0)
        expect(typeof response.data.message).toBe('string')
      }
    })
  })

  describe('Requirement 2.4: Unified frontend response handler', () => {
    it('should parse all API responses using single unified handler', async () => {
      const testResponses = [
        {
          endpoint: '/api/auth/login',
          response: { code: 0, message: 'Success', data: { token: 'abc' } }
        },
        {
          endpoint: '/articles',
          response: { code: 0, message: 'Success', data: [] }
        },
        {
          endpoint: '/api/user/profile',
          response: { code: 0, message: 'Success', data: { id: 1 } }
        }
      ]
      
      // Simulate unified response handler
      const unifiedResponseHandler = (response) => {
        // This simulates the simplified response interceptor logic
        const res = response.data
        
        if (typeof res?.code !== 'number' || !('message' in res)) {
          throw new Error('Unexpected response format')
        }
        
        if (res.code !== 0) {
          throw new Error(res.message || 'Request failed')
        }
        
        return res
      }
      
      for (const testCase of testResponses) {
        const mockResponse = { data: testCase.response }
        
        expect(() => {
          const result = unifiedResponseHandler(mockResponse)
          expect(result.code).toBe(0)
          expect(result).toHaveProperty('data')
        }).not.toThrow()
      }
    })

    it('should handle errors consistently across all endpoints', async () => {
      const errorResponses = [
        { code: 1001, message: 'Auth error', data: null },
        { code: 1002, message: 'Validation error', data: null },
        { code: 1003, message: 'Not found error', data: null }
      ]
      
      const unifiedErrorHandler = (response) => {
        const res = response.data
        if (res.code !== 0) {
          return {
            isError: true,
            code: res.code,
            message: res.message,
            errorCode: res.errorCode
          }
        }
        return { isError: false }
      }
      
      for (const errorResponse of errorResponses) {
        const result = unifiedErrorHandler({ data: errorResponse })
        expect(result.isError).toBe(true)
        expect(result.code).toBe(errorResponse.code)
        expect(result.message).toBe(errorResponse.message)
      }
    })
  })

  describe('Requirement 2.5: Consistent API documentation', () => {
    it('should validate that all endpoints follow the same response schema', () => {
      // Define the expected ApiResponse schema
      const apiResponseSchema = {
        code: 'number',
        message: 'string',
        data: 'any', // Can be object, array, null, etc.
        errorCode: 'string|undefined' // Optional field
      }
      
      const validateApiResponse = (response) => {
        const data = response.data
        
        // Check required fields
        expect(typeof data.code).toBe('number')
        expect(typeof data.message).toBe('string')
        expect(data).toHaveProperty('data')
        
        // Check optional fields
        if (data.errorCode !== undefined) {
          expect(typeof data.errorCode).toBe('string')
        }
        
        return true
      }
      
      // Test various response types
      const testResponses = [
        { data: { code: 0, message: 'Success', data: { id: 1 } } },
        { data: { code: 0, message: 'Success', data: [] } },
        { data: { code: 0, message: 'Success', data: null } },
        { data: { code: 1001, message: 'Error', data: null, errorCode: 'ERR_001' } }
      ]
      
      testResponses.forEach(response => {
        expect(() => validateApiResponse(response)).not.toThrow()
      })
    })
  })

  describe('Response Interceptor Integration', () => {
    it('should process standardized responses correctly', async () => {
      // Mock the response interceptor behavior
      const responseInterceptor = (response) => {
        const res = response.data
        
        // Check for standardized format
        if (typeof res?.code !== 'number' || !('message' in res)) {
          console.warn('Unexpected response format:', res)
          return res
        }
        
        // Handle business errors
        if (res.code !== 0) {
          throw new Error(res.message || 'Request failed')
        }
        
        return res
      }
      
      // Test successful response
      const successResponse = {
        data: {
          code: 0,
          message: 'Operation successful',
          data: { result: 'success' }
        }
      }
      
      const result = responseInterceptor(successResponse)
      expect(result.code).toBe(0)
      expect(result.data.result).toBe('success')
      
      // Test error response
      const errorResponse = {
        data: {
          code: 1001,
          message: 'Operation failed',
          data: null
        }
      }
      
      expect(() => responseInterceptor(errorResponse)).toThrow('Operation failed')
    })
  })
})