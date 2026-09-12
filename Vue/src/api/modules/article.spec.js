import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import instance from '../axiosInstance'
import { getAllArticles } from './article'

vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const originalAdapter = instance.defaults.adapter
let requests

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  requests = []
  instance.defaults.adapter = async config => {
    requests.push(new URL(instance.getUri(config), 'https://example.invalid'))
    return { status: 200, statusText: 'OK', headers: {}, config,
      data: { code: 0, data: { list: [], total: 0 } } }
  }
})

afterEach(() => { instance.defaults.adapter = originalAdapter })

describe('article list request contract', () => {
  it.each([
    ['created_at', 'asc'], ['created_at', 'desc'], ['likes_count', 'desc'], ['exposure_count', 'desc']
  ])('forwards selected %s/%s sorting with pagination', async (sortBy, sortOrder) => {
    await expect(getAllArticles(3, 12, sortBy, sortOrder)).resolves.toMatchObject({
      code: 0, data: { list: [], total: 0 }
    })
    expect(requests[0].pathname).toBe('/api/articles/getall')
    expect(Object.fromEntries(requests[0].searchParams)).toEqual({
      page: '3', pageSize: '12', sortBy, sortOrder
    })
  })

  it('leaves backend default sorting available for existing two-argument callers', async () => {
    await getAllArticles(1, 10)
    expect(Object.fromEntries(requests[0].searchParams)).toEqual({ page: '1', pageSize: '10' })
  })
})
