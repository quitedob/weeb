import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ResetPassword from './ResetPassword.vue'
import axiosInstance from '@/api/axiosInstance'
const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { token: 'reset-nonce' } }), useRouter: () => ({ push }) }))
vi.mock('@/api/axiosInstance', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
let wrapper
beforeEach(() => { vi.useFakeTimers(); vi.clearAllMocks(); axiosInstance.get.mockResolvedValue({ code: 0, data: true }) })
afterEach(() => { wrapper?.unmount(); vi.useRealTimers() })
const render = () => shallowMount(ResetPassword, { global: { stubs: { 'el-form': { template: '<form><slot /></form>', methods: { validate: async () => true } } } } })
describe('password reset contract', () => {
  it('verifies the nonce and posts the dedicated reset payload before redirecting', async () => {
    axiosInstance.post.mockResolvedValue({ code: 0 })
    wrapper = render(); await flushPromises()
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/auth/verify-reset-token', { params: { token: 'reset-nonce' } })
    const state = wrapper.vm.$.setupState
    state.form.password = 'Password123!'; state.form.confirmPassword = 'Password123!'
    state.validatePasswordStrength()
    await state.handleSubmit()
    expect(axiosInstance.post).toHaveBeenCalledWith('/api/auth/reset-password', { resetToken: 'reset-nonce', newPassword: 'Password123!', confirmPassword: 'Password123!' })
    expect(state.successMessage).toContain('密码重置成功')
    await vi.advanceTimersByTimeAsync(2000)
    expect(push).toHaveBeenCalledWith('/login')
  })
  it('shows a rejected reset without reporting success or redirecting', async () => {
    axiosInstance.post.mockRejectedValue({ response: { data: { message: 'nonce expired' } } })
    wrapper = render(); await flushPromises()
    const state = wrapper.vm.$.setupState
    state.form.password = 'Password123!'; state.form.confirmPassword = 'Password123!'; state.validatePasswordStrength()
    await state.handleSubmit()
    expect(state.errorMessage).toBe('nonce expired')
    expect(state.successMessage).toBe('')
    expect(push).not.toHaveBeenCalled()
  })
})
