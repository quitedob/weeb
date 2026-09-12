import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import instance from '@/api/axiosInstance'
import Forget from './Forget.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ accessToken: null }) }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const originalAdapter = instance.defaults.adapter
let wrapper, requests, rejectReset
beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
  requests = []
  rejectReset = false
  instance.defaults.adapter = async config => {
    requests.push({ url: config.url, method: config.method, body: JSON.parse(config.data) })
    const rejected = rejectReset && config.url === '/api/auth/reset-password'
    return { status: 200, headers: {}, config, data: {
      code: rejected ? 1 : 0, message: rejected ? '重置令牌已过期' : 'OK', data: null
    } }
  }
  wrapper = mount(Forget, { global: { stubs: { 'router-link': true } } })
})
afterEach(() => {
  wrapper?.unmount()
  instance.defaults.adapter = originalAdapter
  vi.restoreAllMocks()
})

async function requestResetEmail() {
  await wrapper.get('#email').setValue('person@example.test')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}
async function submitNewPassword() {
  await wrapper.get('#resetToken').setValue(' reset-nonce ')
  await wrapper.get('#newPassword').setValue('Password123!')
  await wrapper.get('#confirmPassword').setValue('Password123!')
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('forgot password API journey', () => {
  it('sends a scalar email then resets with the token and matching passwords through the real API module', async () => {
    await requestResetEmail()
    expect(requests[0]).toEqual({ method: 'post', url: '/api/auth/forgot-password', body: { email: 'person@example.test' } })
    expect(wrapper.text()).toContain('请打开邮件中的链接设置新密码')
    await submitNewPassword()
    expect(requests[1]).toEqual({ method: 'post', url: '/api/auth/reset-password', body: {
      resetToken: 'reset-nonce', newPassword: 'Password123!', confirmPassword: 'Password123!'
    } })
    expect(wrapper.get('.success-content').text()).toContain('密码重置成功')
    await wrapper.get('.success-content button').trigger('click')
    expect(push).toHaveBeenCalledWith('/login')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('retains the reset form and shows a rejected token without reporting reset success', async () => {
    await requestResetEmail()
    ElMessage.success.mockClear()
    rejectReset = true
    await submitNewPassword()
    expect(wrapper.get('.error-message').text()).toContain('重置令牌已过期')
    expect(wrapper.get('#resetToken').element.value.trim()).toBe('reset-nonce')
    expect(wrapper.find('.success-content').exists()).toBe(false)
    expect(ElMessage.success).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })
})
