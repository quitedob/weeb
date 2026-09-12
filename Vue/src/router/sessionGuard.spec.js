import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import { useAuthStore } from '@/stores/authStore'

const { hooks } = vi.hoisted(() => ({ hooks: [] }))
vi.mock('vue-router', () => ({
  createWebHistory: vi.fn(),
  createRouter: () => ({ beforeEach: hook => hooks.push(hook) })
}))
vi.mock('@/layout/Layout.vue', () => ({ default: {} }))
vi.mock('@/api', () => ({ default: {} }))
import './index'

const deferred = () => {
  let resolve, reject
  const promise = new Promise((done, fail) => { resolve = done; reject = fail })
  return { promise, resolve, reject }
}
let auth, next
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  auth = useAuthStore()
  auth.applyToken({ token: 'token-a', expiresIn: 3600 })
  next = vi.fn()
})
const navigate = () => hooks[0]({ meta: { requiresAuth: true }, fullPath: '/profile' }, {}, next)

describe('authentication navigation lifetime', () => {
  it.each(['resolve', 'reject'])('cancels an old account navigation when its profile request %s', async outcome => {
    const pending = deferred()
    vi.spyOn(auth, 'fetchUserInfo').mockReturnValue(pending.promise)
    const navigation = navigate()
    auth.logoutCleanup()
    auth.applyToken({ token: 'token-b', expiresIn: 3600 })
    auth.setCurrentUser({ id: 'b' })
    const cleanup = vi.spyOn(auth, 'logoutCleanup')
    if (outcome === 'resolve') pending.resolve({ id: 'a' })
    else pending.reject(new Error('Old request failed'))
    await navigation
    expect(next).toHaveBeenCalledWith(false)
    expect(auth.accessToken).toBe('token-b')
    expect(cleanup).not.toHaveBeenCalled()
  })

  it('retries a profile read with a renewed credential in the same account', async () => {
    const old = deferred(), current = deferred()
    const fetch = vi.spyOn(auth, 'fetchUserInfo').mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    const navigation = navigate()
    auth.applyToken({ token: 'renewed-a', expiresIn: 3600 })
    old.reject(new Error('Old token revoked'))
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(2)
    auth.setCurrentUser({ id: 'a' })
    current.resolve(auth.currentUser)
    await navigation
    expect(next).toHaveBeenCalledWith()
    expect(auth.accessToken).toBe('renewed-a')
  })

  it('preserves a current session during a temporary profile failure', async () => {
    vi.spyOn(auth, 'fetchUserInfo').mockRejectedValue(new Error('Unavailable'))
    await navigate()
    expect(next).toHaveBeenCalledWith(false)
    expect(auth.accessToken).toBe('token-a')
  })

  it('redirects to login after current authentication is rejected without a nested cleanup', async () => {
    const pending = deferred()
    vi.spyOn(auth, 'fetchUserInfo').mockReturnValue(pending.promise)
    const navigation = navigate()
    auth.logoutCleanup()
    const cleanup = vi.spyOn(auth, 'logoutCleanup')
    pending.resolve(null)
    await navigation
    expect(next).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/profile' } })
    expect(cleanup).not.toHaveBeenCalled()
  })
})
