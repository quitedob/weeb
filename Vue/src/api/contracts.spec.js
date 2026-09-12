import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import axios from 'axios'
import instance from './axiosInstance'
import { useAuthStore } from '@/stores/authStore'
import user from './modules/user'
import group from './modules/group'
import message from './modules/message'
import { createArticle } from './modules/article'
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))
const adapter = instance.defaults.adapter
beforeEach(() => { localStorage.clear(); setActivePinia(createPinia()) })
afterEach(() => { instance.defaults.adapter = adapter; vi.unstubAllEnvs() })

const capture = () => {
  const requests = []
  instance.defaults.adapter = async config => {
    requests.push(config)
    return { status: 200, statusText: 'OK', headers: {}, config, data: { code: 0, message: 'OK', data: { id: 1 } } }
  }
  return requests
}
describe('API contracts', () => {
  it('normalizes HTTP 204 group deletion responses without treating raw HTTP 200 data as an envelope', async () => {
    const requests = []
    instance.defaults.adapter = async config => {
      requests.push(config)
      return { status: config.method === 'delete' ? 204 : 200, headers: {}, config, data: '' }
    }
    const responses = await Promise.all([
      group.kickMember({ groupId: 44, userIdToKick: 4 }), group.leaveGroup(44), group.disbandGroup(44)
    ])
    expect(responses).toEqual(Array(3).fill({ code: 0, message: '', data: null }))
    expect(requests.map(({ method, url }) => [method, url])).toEqual([
      ['delete', '/api/groups/44/members/4'], ['delete', '/api/groups/44/members/me'], ['delete', '/api/groups/44']
    ])
    expect(await instance.get('/raw')).toBe('')
  })
  it('uses the group search query name required by the controller', async () => {
    const requests = capture()
    await group.searchGroups('Study')
    expect(requests[0]).toMatchObject({ url: '/api/groups/search', params: { q: 'Study' } })
  })
  it('preserves draft status zero in the article request body', async () => {
    const requests = capture()
    await createArticle({ articleTitle: 'Private draft', articleContent: 'Work in progress', status: 0 })
    expect(requests[0].url).toBe('/api/articles/new')
    expect(JSON.parse(requests[0].data).status).toBe(0)
  })
  it('resolves nested profile/list and uploaded avatar URLs for a separate production API', async () => {
    vi.stubEnv('DEV', false)
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.test')
    const path = '/uploads/avatars/123e4567-e89b-12d3-a456-426614174000.png'
    const payload = { user: { id: 1, avatar: path }, list: [{ avatar: path }], content: path }
    instance.defaults.adapter = async config => ({ status: 200, headers: {}, config,
      data: { code: 0, message: 'OK', data: config.url.endsWith('/avatar') ? { avatar: path } : payload }
    })
    const profile = await user.getCurrentUser()
    expect(profile.data.user.avatar).toBe('https://api.example.test' + path)
    expect(profile.data.list[0].avatar).toBe('https://api.example.test' + path)
    expect(profile.data.content).toBe(path)
    expect(payload.user.avatar).toBe(path)
    const form = new FormData(); form.append('file', new Blob(['pixels'], { type: 'image/png' }), 'avatar.png')
    expect((await user.uploadAvatar(form)).data.avatar).toBe('https://api.example.test' + path)
  })
  it('uses plural user follow resources, DELETE unfollow and real stats endpoints', async () => {
    const requests = capture()
    await user.followUser(9); await user.unfollowUser(9); await user.getFollowStats(); await user.getUserFollowStats(9)
    expect(requests.map(({ method, url }) => [method, url])).toEqual([
      ['post', '/api/users/9/follow'], ['delete', '/api/users/9/follow'], ['get', '/api/follow/stats'], ['get', '/api/follow/stats/9']
    ])
  })
  it('sends group decisions in PUT JSON bodies', async () => {
    const requests = capture()
    await group.approveApplication(3, 4, 'welcome'); await group.rejectApplication(3, 5, 'full')
    expect(requests.map(({ method, url, data }) => [method, url, JSON.parse(data)])).toEqual([
      ['put', '/api/groups/3/applications/4', { action: 'approve', reason: 'welcome' }],
      ['put', '/api/groups/3/applications/5', { action: 'reject', reason: 'full' }]
    ])
  })
  it('puts emoji reactions in query parameters accepted by Spring', async () => {
    const requests = capture()
    await message.handleReaction(2, '👍'); await message.addReaction(2, '❤️')
    expect(requests.map(r => r.params.reactionType)).toEqual(['👍', '❤️'])
    expect(requests.every(r => r.data === null)).toBe(true)
  })
  it('returns the success envelope and preserves sessions on system errors', async () => {
    const store = useAuthStore(); store.accessToken = 'test-token'
    capture()
    expect((await instance.get('/success')).code).toBe(0)
    instance.defaults.adapter = async config => ({ status: 200, headers: {}, config, data: { code: -1, message: 'failure' } })
    await expect(instance.post('/business')).rejects.toThrow('failure')
    expect(store.accessToken).toBe('test-token')
  })
  it.each([401, 403, 500])('handles HTTP %i without recursive logout or mutation retry', async status => {
    const store = useAuthStore(); store.accessToken = 'test-token'
    const logout = vi.spyOn(store, 'logout')
    const transport = vi.fn(async config => { throw new axios.AxiosError('failure', 'ERR_BAD_RESPONSE', config, {}, { status, data: { message: 'denied' }, config }) })
    instance.defaults.adapter = transport
    await expect(instance.post('/operation')).rejects.toThrow('denied')
    expect(transport).toHaveBeenCalledTimes(1)
    expect(logout).not.toHaveBeenCalled()
    expect(store.accessToken).toBe(status === 401 ? null : 'test-token')
  })
})
