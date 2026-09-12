import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises } from '@vue/test-utils'
import axios from 'axios'
import instance from '@/api/axiosInstance'
import { useAuthStore } from './authStore'

vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const adapter = instance.defaults.adapter
let requests, firstTab, secondTab, locks
const ok = data => ({ code: 0, message: 'OK', data })
const activate = tab => setActivePinia(tab.pinia)
beforeEach(() => {
  localStorage.clear()
  let tail = Promise.resolve()
  locks = { request: vi.fn((_name, callback) => {
    const task = tail.then(() => callback())
    tail = task.catch(() => {})
    return task
  }) }
  vi.stubGlobal('navigator', { locks })
  const makeTab = () => {
    const pinia = createPinia(); setActivePinia(pinia)
    const auth = useAuthStore()
    return { pinia, auth }
  }
  firstTab = makeTab()
  firstTab.auth.setCurrentUser({ id: 1, username: 'Alice' })
  firstTab.auth.applyToken({ token: 'original-token', expiresIn: 3600 })
  secondTab = makeTab()
  requests = []
  instance.defaults.adapter = config => new Promise((resolve, reject) => {
    requests.push({ config,
      reply: data => resolve({ status: 200, headers: {}, config, data: ok(data) }),
      rejectCredential: () => resolve({ status: 200, headers: {}, config, data: { code: 1002, message: 'Consumed token' } }),
      fail: () => reject(new axios.AxiosError('Consumed token', 'ERR_BAD_RESPONSE', config, {}, {
        status: 401, config, data: { message: 'Consumed token' }
      })) })
  })
})
afterEach(() => { instance.defaults.adapter = adapter; vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('renewal coordination across browser tabs', () => {
  it('prevents a losing renewal 401 before the winner has returned and persisted its replacement', async () => {
    activate(firstTab)
    const winner = firstTab.auth.refreshAccessToken().catch(error => error)
    await flushPromises()
    activate(secondTab)
    const follower = secondTab.auth.refreshAccessToken().catch(error => error)
    await flushPromises()
    // This is the server's dangerous ordering without cross-tab coordination:
    // the first request consumed the JTI, but its successful response is delayed.
    if (requests.length > 1) {
      requests[1].fail()
      await follower
    }
    expect(requests).toHaveLength(1)
    expect(localStorage.getItem('jwt_token')).toBe('original-token')
    activate(firstTab)
    requests[0].reply({ token: 'renewed-token', expiresIn: 3600 })
    expect(await winner).toBe('renewed-token')
    expect(await follower).toBe('renewed-token')
    expect(secondTab.auth.accessToken).toBe('renewed-token')
    expect(localStorage.getItem('jwt_token')).toBe('renewed-token')
    expect(requests).toHaveLength(1)
  })

  it.each([401, 1002])('waits for renewal before handling an unrelated old-token %s', async outcome => {
    activate(firstTab)
    const renewal = firstTab.auth.refreshAccessToken()
    await flushPromises()
    activate(secondTab)
    let settled = false
    const oldRequest = instance.get('/api/users/me').catch(error => error).finally(() => { settled = true })
    if (outcome === 401) requests[1].fail()
    else requests[1].rejectCredential()
    await flushPromises()
    expect(settled).toBe(false)
    expect(localStorage.getItem('jwt_token')).toBe('original-token')
    activate(firstTab)
    requests[0].reply({ token: 'renewed-token', expiresIn: 3600 })
    expect(await renewal).toBe('renewed-token')
    expect(axios.isCancel(await oldRequest)).toBe(true)
    expect(localStorage.getItem('jwt_token')).toBe('renewed-token')
    expect(firstTab.auth.accessToken).toBe('renewed-token')
  })

  it('does not send queued renewal work after logout or restore its delayed winner', async () => {
    activate(firstTab)
    const winner = firstTab.auth.refreshAccessToken()
    await flushPromises()
    activate(secondTab)
    const queued = secondTab.auth.refreshAccessToken()
    secondTab.auth.logoutCleanup()
    activate(firstTab)
    requests[0].reply({ token: 'renewed-after-logout', expiresIn: 3600 })
    expect(await winner).toBeNull()
    expect(await queued).toBeNull()
    expect(requests).toHaveLength(1)
    expect(localStorage.getItem('jwt_token')).toBeNull()
    expect(firstTab.auth.accessToken).toBeNull()
  })

  it('retains the current credential without issuing unsafe rotation when Web Locks are unavailable', async () => {
    vi.stubGlobal('navigator', {})
    activate(firstTab)
    expect(await firstTab.auth.refreshAccessToken()).toBeNull()
    expect(requests).toHaveLength(0)
    expect(localStorage.getItem('jwt_token')).toBe('original-token')
    expect(firstTab.auth.isLoggedIn).toBe(true)
    expect(firstTab.auth.isRefreshing).toBe(false)
  })

  it('handles a genuinely rejected renewal without waiting on its own lock', async () => {
    activate(firstTab)
    const renewal = firstTab.auth.refreshAccessToken()
    await flushPromises()
    requests[0].fail()
    expect(await renewal).toBeNull()
    expect(firstTab.auth.accessToken).toBeNull()
    expect(locks.request).toHaveBeenCalledTimes(1)
  })
})
