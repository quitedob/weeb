import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiBaseUrl, socketUrl, proxyOrigin } from './serviceUrls'
import { resolveAssetUrl } from './assetUrl'

afterEach(() => vi.unstubAllEnvs())
describe('portable deployment service URLs', () => {
  const origin = 'https://community.example.org'
  it('keeps the default production HTTP API and SockJS endpoint on the browser domain', () => {
    expect(new URL('/api/users/me', new URL(apiBaseUrl({ DEV: false }), origin)).href)
      .toBe(`${origin}/api/users/me`)
    expect(socketUrl({}, origin)).toBe(`${origin}/ws`)
  })
  it('honors explicit production API and SockJS overrides', () => {
    const env = { DEV: false, VITE_API_BASE_URL: 'https://api.example.org', VITE_WS_URL: 'https://socket.example.org/ws' }
    expect(apiBaseUrl(env)).toBe(env.VITE_API_BASE_URL)
    expect(socketUrl(env, origin)).toBe(env.VITE_WS_URL)
    expect(socketUrl({ VITE_WS_URL: '/custom/ws' }, origin)).toBe(`${origin}/custom/ws`)
  })
  it('supplies a proxy origin without duplicating /ws or /api paths', () => {
    expect(apiBaseUrl({ DEV: true, VITE_API_BASE_URL: 'http://localhost:18080' })).toBe('/')
    expect(proxyOrigin()).toBe('http://localhost:8080')
    expect(new URL('/ws/info', proxyOrigin('http://localhost:18080/ws')).href).toBe('http://localhost:18080/ws/info')
    expect(proxyOrigin('https://api.example.org/api')).toBe('https://api.example.org')
  })
  it('resolves uploaded avatars against the browser origin when no production API override exists', () => {
    vi.stubEnv('DEV', false); vi.stubEnv('VITE_API_BASE_URL', '')
    const path = '/uploads/avatars/123e4567-e89b-12d3-a456-426614174000.png'
    expect(resolveAssetUrl(path)).toBe(new URL(path, window.location.origin).href)
    expect(resolveAssetUrl('message text /uploads/avatars/example.png')).toBe('message text /uploads/avatars/example.png')
  })
})
