import { afterEach, describe, expect, it, vi } from 'vitest'
import { resolveAssetUrl, resolveAvatarUrls } from './assetUrl'

afterEach(() => vi.unstubAllEnvs())
const path = '/uploads/avatars/123e4567-e89b-12d3-a456-426614174000.png'

describe('uploaded avatar URLs', () => {
  it('keeps development avatars on the same origin for the Vite upload proxy', () => {
    vi.stubEnv('DEV', true)
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.test')
    expect(resolveAssetUrl(path)).toBe(path)
  })

  it('supports the production default and relative API bases', () => {
    vi.stubEnv('DEV', false)
    vi.stubEnv('VITE_API_BASE_URL', '')
    expect(resolveAssetUrl(path)).toBe('http://localhost:8080' + path)
    vi.stubEnv('VITE_API_BASE_URL', '/')
    expect(resolveAssetUrl(path)).toBe(window.location.origin + path)
  })

  it('preserves external images, ordinary text and unrelated URLs', () => {
    vi.stubEnv('DEV', false)
    const payload = { avatar: 'https://images.example.test/picture.png',
      content: path, articleContent: 'Image path: ' + path, image: path,
      metadata: { avatar: '/article/avatar-help' }, empty: null }
    expect(resolveAvatarUrls(payload)).toEqual(payload)
  })
})
