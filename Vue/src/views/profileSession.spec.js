import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import api from '@/api'
import { useAuthStore } from '@/stores/authStore'
import appleMessage from '@/utils/appleMessage'
import Login from './Login.vue'
import Settings from './Settings.vue'
import UserProfile from './UserProfile.vue'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }), useRoute: () => ({ query: {} }) }))
vi.mock('@/utils/appleMessage', () => ({ default: { success: vi.fn(), error: vi.fn() } }))
vi.mock('@/api', () => ({ default: {
  auth: { login: vi.fn() },
  user: { getCurrentUser: vi.fn(), getSettings: vi.fn(), updateCurrentUser: vi.fn(),
    updateProfile: vi.fn(), uploadAvatar: vi.fn(), getUserStats: vi.fn(), getUserActivities: vi.fn() }
} }))

const deferred = () => {
  let resolve, reject
  const promise = new Promise((done, fail) => { resolve = done; reject = fail })
  return { promise, resolve, reject }
}
const ok = data => ({ code: 0, message: 'OK', data })
let wrapper, auth, pinia

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  pinia = createPinia()
  setActivePinia(pinia)
  auth = useAuthStore()
  switchAccount('a')
  api.user.getUserStats.mockResolvedValue(ok({ articleCount: 1 }))
  api.user.getUserActivities.mockResolvedValue(ok([]))
  api.user.getSettings.mockResolvedValue(ok({ privacy: {}, notifications: {} }))
})
afterEach(() => { wrapper?.unmount(); vi.restoreAllMocks() })
function switchAccount(id) {
  auth.logoutCleanup()
  auth.setCurrentUser({ id, username: id, nickname: id + ' nickname', avatar: id + '.png' })
  auth.applyToken({ token: 'token-' + id, expiresIn: 3600 })
}
async function render(component) {
  wrapper = mount(component, { global: { plugins: [pinia], stubs: { 'router-link': true, 'el-icon': true } } })
  await flushPromises()
}
const button = label => wrapper.findAll('button').find(item => item.text() === label)

describe('page continuations after account switching', () => {
  it.each(['resolve', 'reject'])('does not report old login success or failure after B login (%s)', async outcome => {
    const pending = deferred()
    api.auth.login.mockReturnValue(pending.promise)
    await render(Login)
    await wrapper.get('#username').setValue('a')
    await wrapper.get('#password').setValue('password-a')
    await wrapper.get('form').trigger('submit')
    switchAccount('b')
    if (outcome === 'resolve') pending.resolve(ok({ token: 'token-a', user: { id: 'a' }, expiresIn: 3600 }))
    else pending.reject(new Error('Old login rejected'))
    await flushPromises()
    expect(auth.currentUser.id).toBe('b')
    expect(push).not.toHaveBeenCalled()
    expect(appleMessage.success).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Old login rejected')
  })

  it('still shows a failed login belonging to the active attempt', async () => {
    api.auth.login.mockRejectedValue(new Error('Wrong password'))
    await render(Login)
    await wrapper.get('#username').setValue('a')
    await wrapper.get('#password').setValue('wrong-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Wrong password')
    expect(wrapper.get('#password').attributes('disabled')).toBeUndefined()
    expect(appleMessage.success).not.toHaveBeenCalled()
  })

  it('does not commit a settings profile write resolved after B login', async () => {
    const pending = deferred()
    api.user.updateCurrentUser.mockReturnValue(pending.promise)
    await render(Settings)
    await wrapper.get('#username input').setValue('Changed A')
    await wrapper.get('.save-btn').trigger('click')
    switchAccount('b')
    pending.resolve(ok({ id: 'a', username: 'Changed A' }))
    await flushPromises()
    expect(auth.currentUser.id).toBe('b')
    expect(appleMessage.success).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('更新成功')
  })

  it('does not merge an old profile form into the then-current account', async () => {
    const pending = deferred()
    api.user.updateProfile.mockReturnValue(pending.promise)
    await render(UserProfile)
    await button('编辑资料').trigger('click')
    await wrapper.get('.edit-form textarea').setValue('Account A biography')
    await button('保存').trigger('click')
    switchAccount('b')
    pending.resolve(ok({ id: 'a', bio: 'Account A biography' }))
    await flushPromises()
    expect(auth.currentUser).toMatchObject({ id: 'b', avatar: 'b.png' })
    expect(auth.currentUser.bio).toBeUndefined()
    expect(appleMessage.success).not.toHaveBeenCalled()
    expect(api.user.getUserStats).toHaveBeenCalledTimes(1)
  })

  it('does not merge an old avatar upload into B or upload a file selected from A dialog after switching', async () => {
    let picker
    vi.spyOn(HTMLInputElement.prototype, 'click').mockImplementation(function () { if (this.type === 'file') picker = this })
    const pending = deferred()
    api.user.uploadAvatar.mockReturnValue(pending.promise)
    await render(UserProfile)
    await wrapper.get('.avatar-change-btn').trigger('click')
    const file = new File(['image'], 'avatar.png', { type: 'image/png' })
    const upload = picker.onchange({ target: { files: [file] } })
    switchAccount('b')
    pending.resolve(ok({ avatar: 'a-upload.png' }))
    await upload
    expect(auth.currentUser.avatar).toBe('b.png')
    expect(appleMessage.success).not.toHaveBeenCalled()
    await picker.onchange({ target: { files: [file] } })
    expect(api.user.uploadAvatar).toHaveBeenCalledTimes(1)
  })
})
