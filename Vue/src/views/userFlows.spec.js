import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Register from './Register.vue'
import UserDetail from './UserDetail.vue'
import Settings from './Settings.vue'
import { useAuthStore } from '@/stores/authStore'
import api from '@/api'
import appleMessage from '@/utils/appleMessage'
import { getArticlesByUserId } from '@/api/modules/article'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }), useRoute: () => ({ params: { userId: '9' } }) }))
vi.mock('@/api', () => ({ default: {
  auth: { register: vi.fn() },
  user: { getCurrentUser: vi.fn(), updateCurrentUser: vi.fn(), getSettings: vi.fn(), savePrivacySettings: vi.fn(), saveNotificationSettings: vi.fn() }
} }))
vi.mock('@/api/modules/user', () => ({ default: {
  getUserById: vi.fn(async () => ({ code: 0, data: { id: 9, username: 'target' } })),
  checkFollowStatus: vi.fn(async () => ({ code: 0, data: false })),
  followUser: vi.fn(async () => ({ code: 0 })),
  unfollowUser: vi.fn(async () => ({ code: 0 }))
} }))
vi.mock('@/api/modules/article', () => ({ getArticlesByUserId: vi.fn(async () => ({ code: 0, data: [] })) }))
vi.mock('@/utils/appleMessage', () => ({ default: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }))
vi.mock('@/api/modules/chat', () => ({ default: { createChat: vi.fn(async () => ({ code: 0, data: { sharedChatId: 42 } })) } }))
import chatApi from '@/api/modules/chat'
import userApi from '@/api/modules/user'
const wrappers = []
const render = (component, options = {}) => {
  const wrapper = mount(component, { global: { stubs: { 'router-link': true } }, ...options })
  wrappers.push(wrapper); return wrapper
}
beforeEach(() => {
  vi.clearAllMocks(); localStorage.clear(); setActivePinia(createPinia())
  api.user.getSettings.mockResolvedValue({ code: 0, data: {
    privacy: { onlineVisible: false, allowMessages: true, showFollows: false },
    notifications: { newMessages: false, follows: true, likes: true, comments: true, groupInvites: true }
  } })
})
afterEach(() => { wrappers.forEach(w => w.unmount()); wrappers.length = 0 })
async function fillRegistration(wrapper) {
  for (const [field, value] of Object.entries({ username: 'tester', email: 'test@example.test', phone: '13812345678', password: 'Password123!', confirmPassword: 'Password123!' })) {
    await wrapper.find('#' + field).setValue(value)
  }
  await wrapper.find('input[type="checkbox"]').setValue(true)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}
describe('reported user journeys', () => {
  it('loads persisted privacy and saves only after the API confirms it', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1 }; auth.accessToken = 'token'
    const wrapper = render(Settings); await flushPromises()
    expect(wrapper.find('[aria-label="在线状态可见性"]').attributes('aria-checked')).toBe('false')
    await wrapper.find('[aria-label="接收私信"]').trigger('click')
    const save = wrapper.findAll('button').find(button => button.text() === '保存隐私设置')
    api.user.savePrivacySettings.mockResolvedValueOnce({ code: 1003, message: 'denied' })
    await save.trigger('click'); await flushPromises()
    expect(appleMessage.success).not.toHaveBeenCalled()
    expect(auth.preferences.privacy.allowMessages).toBe(true)
    const privacy = { onlineVisible: false, allowMessages: false, showFollows: false }
    api.user.savePrivacySettings.mockResolvedValueOnce({ code: 0, data: privacy })
    await save.trigger('click'); await flushPromises()
    expect(api.user.savePrivacySettings).toHaveBeenLastCalledWith(privacy)
    expect(auth.preferences.privacy.allowMessages).toBe(false)
    expect(appleMessage.success).toHaveBeenCalled()
  })
  it('disables preference saves when persisted settings cannot be loaded', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1 }; auth.accessToken = 'token'
    api.user.getSettings.mockRejectedValueOnce(new Error('offline'))
    const wrapper = render(Settings); await flushPromises()
    expect(wrapper.findAll('button').find(button => button.text() === '保存隐私设置').attributes()).toHaveProperty('disabled')
    expect(appleMessage.error).toHaveBeenCalled()
  })
  it('does not show save success or replace preferences after logout during a settings save', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1 }; auth.accessToken = 'old'
    const wrapper = render(Settings); await flushPromises()
    let resolve
    api.user.saveNotificationSettings.mockReturnValueOnce(new Promise(done => { resolve = done }))
    await wrapper.findAll('button').find(button => button.text() === '保存通知设置').trigger('click')
    auth.logoutCleanup(); auth.currentUser = { id: 2 }; auth.accessToken = 'new'
    resolve({ code: 0, data: { newMessages: false } }); await flushPromises()
    expect(auth.preferences).toBeNull()
    expect(appleMessage.success).not.toHaveBeenCalled()
  })
  it('clears account preferences on logout and ignores a stale response from the previous account', async () => {
    const auth = useAuthStore(); auth.accessToken = 'old'
    let resolve
    api.user.getSettings.mockReturnValueOnce(new Promise(done => { resolve = done }))
    const pending = auth.loadPreferences()
    auth.logoutCleanup(); auth.accessToken = 'new'
    resolve({ code: 0, data: { privacy: { allowMessages: false } } })
    await pending
    expect(auth.preferences).toBeNull()
  })
  it('redirects registration after code zero success', async () => {
    api.auth.register.mockResolvedValue({ code: 0, message: 'registered', data: { id: 1 } })
    await fillRegistration(render(Register))
    expect(push).toHaveBeenCalledWith('/login')
    expect(appleMessage.success).toHaveBeenCalled()
  })
  it('shows registration failure and retains the form', async () => {
    api.auth.register.mockRejectedValue(new Error('offline'))
    const wrapper = render(Register)
    await fillRegistration(wrapper)
    expect(wrapper.text()).toContain('注册失败')
    expect(push).not.toHaveBeenCalled()
    expect(wrapper.find('#username').element.value).toBe('tester')
  })
  it('loads a profile, follows it and then unfollows through the real API module methods', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1, username: 'viewer' }; auth.accessToken = 'test-token'
    const wrapper = render(UserDetail)
    await flushPromises()
    expect(wrapper.text()).toContain('target')
    expect(getArticlesByUserId).toHaveBeenCalledWith('9')
    const follow = () => wrapper.findAll('button').find(button => ['关注', '取消关注'].includes(button.text()))
    await follow().trigger('click'); await flushPromises()
    expect(userApi.followUser).toHaveBeenCalledWith('9')
    expect(follow().text()).toBe('取消关注')
    await follow().trigger('click'); await flushPromises()
    expect(userApi.unfollowUser).toHaveBeenCalledWith('9')
    expect(follow().text()).toBe('关注')
  })
  it('opens the created conversation when messaging from a profile', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1 }; auth.accessToken = 'test-token'
    const wrapper = render(UserDetail); await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '发消息').trigger('click'); await flushPromises()
    expect(chatApi.createChat).toHaveBeenCalledWith({ targetId: '9' })
    expect(push).toHaveBeenCalledWith({ name: 'SpecificChat', params: { type: 'private', id: '42' } })
  })
  it('retains follow state when the server rejects the operation', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1 }; auth.accessToken = 'test-token'
    userApi.followUser.mockRejectedValueOnce(new Error('denied'))
    const wrapper = render(UserDetail); await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '关注').trigger('click'); await flushPromises()
    expect(wrapper.findAll('button').some(button => button.text() === '关注')).toBe(true)
    expect(appleMessage.error).toHaveBeenCalled()
  })
  it('saves the profile and updates the auth store using the canonical envelope', async () => {
    const auth = useAuthStore(); auth.currentUser = { id: 1, username: 'old', nickname: 'old' }
    api.user.updateCurrentUser.mockResolvedValue({ code: 0, data: { id: 1, username: 'new', nickname: 'new' } })
    const wrapper = render(Settings); await flushPromises()
    await wrapper.find('#username input').setValue('new')
    await wrapper.find('.save-btn').trigger('click'); await flushPromises()
    expect(api.user.updateCurrentUser).toHaveBeenCalledWith(expect.objectContaining({ username: 'new' }))
    expect(auth.currentUser.username).toBe('new')
    expect(wrapper.text()).toContain('更新成功')
  })
})
