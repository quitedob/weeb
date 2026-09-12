import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './authStore'
import api from '@/api'

vi.mock('@/api', () => ({ default: { user: {
  getSettings: vi.fn(), savePrivacySettings: vi.fn(), saveNotificationSettings: vi.fn()
} } }))

const oldPreferences = () => ({
  privacy: { onlineVisible: true, allowMessages: true, showFollows: true },
  notifications: { newMessages: true, follows: true, likes: true, comments: true, groupInvites: true }
})
const changedPrivacy = { onlineVisible: false, allowMessages: false, showFollows: false }
const changedNotifications = { newMessages: false, follows: false, likes: false, comments: false, groupInvites: false }
const ok = data => ({ code: 0, data })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((done, fail) => { resolve = done; reject = fail })
  return { promise, resolve, reject }
}

beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); setActivePinia(createPinia())
  const auth = useAuthStore(); auth.accessToken = 'account-a'; auth.currentUser = { id: 1 }
  auth.preferences = oldPreferences()
})

describe('account preference request lifetime', () => {
  it.each(['before', 'during'])('ignores an old load started %s a successful save', async timing => {
    const auth = useAuthStore()
    const load = deferred(), save = deferred()
    api.user.getSettings.mockReturnValue(load.promise)
    api.user.savePrivacySettings.mockReturnValue(save.promise)
    let loading
    if (timing === 'before') loading = auth.loadPreferences()
    const saving = auth.savePrivacyPreferences(changedPrivacy)
    if (timing === 'during') loading = auth.loadPreferences()
    save.resolve(ok(changedPrivacy)); await saving
    load.resolve(ok(oldPreferences()))
    expect(await loading).toBeNull()
    expect(auth.preferences.privacy).toEqual(changedPrivacy)
  })

  it.each([
    ['savePrivacyPreferences', 'savePrivacySettings', changedPrivacy],
    ['saveNotificationPreferences', 'saveNotificationSettings', changedNotifications]
  ])('ignores a delayed %s response after logout and another account login', async (action, endpoint, section) => {
    const auth = useAuthStore()
    const save = deferred()
    api.user[endpoint].mockReturnValue(save.promise)
    const saving = auth[action](section)
    auth.logoutCleanup(); auth.accessToken = 'account-b'; auth.currentUser = { id: 2 }
    const accountB = oldPreferences(); auth.preferences = accountB
    save.resolve(ok(section))
    expect(await saving).toBeNull()
    expect(auth.preferences).toEqual(accountB)
  })

  it('does not restore a pending save even if the next session happens to reuse the token', async () => {
    const auth = useAuthStore(), save = deferred()
    api.user.savePrivacySettings.mockReturnValue(save.promise)
    const saving = auth.savePrivacyPreferences(changedPrivacy)
    auth.logoutCleanup(); auth.accessToken = 'account-a'
    save.resolve(ok(changedPrivacy))
    expect(await saving).toBeNull()
    expect(auth.preferences).toBeNull()
  })

  it('merges successful saves to different sections regardless of completion order', async () => {
    const auth = useAuthStore(), privacy = deferred(), notifications = deferred()
    api.user.savePrivacySettings.mockReturnValue(privacy.promise)
    api.user.saveNotificationSettings.mockReturnValue(notifications.promise)
    const first = auth.savePrivacyPreferences(changedPrivacy)
    const second = auth.saveNotificationPreferences(changedNotifications)
    notifications.resolve(ok(changedNotifications)); await second
    privacy.resolve(ok(changedPrivacy)); await first
    expect(auth.preferences).toEqual({ privacy: changedPrivacy, notifications: changedNotifications })
  })

  it('preserves visible settings and rejects an active-session save failure', async () => {
    const auth = useAuthStore()
    api.user.savePrivacySettings.mockRejectedValue(new Error('offline'))
    await expect(auth.savePrivacyPreferences(changedPrivacy)).rejects.toThrow('offline')
    expect(auth.preferences).toEqual(oldPreferences())
  })
})
