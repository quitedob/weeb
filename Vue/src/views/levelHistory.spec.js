import { afterEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserLevelHistory from './UserLevelHistory.vue'
import { useAuthStore } from '@/stores/authStore'
import { getUserLevelHistory } from '@/api/modules/userLevelHistory'
vi.mock('@/api', () => ({ default: {} }))
vi.mock('@/api/modules/userLevelHistory', () => ({
  getUserLevelHistory: vi.fn(async () => ({ code: 0, data: { list: [{ id: 1, newLevel: 2 }], total: 12 } })),
  getCurrentLevel: vi.fn(async () => ({ code: 0, data: 2 })),
  getUserLevelHistoryCount: vi.fn(async () => ({ code: 0, data: 12 }))
}))
vi.mock('@/api/modules/userLevel', () => ({ getUpgradeProgress: vi.fn(async () => ({ code: 0, data: {
  overallProgress: 40, requirements: { minArticles: 50, minMessages: 500, description: 'requirements' },
  progressDetails: { articles: 20, messages: 60 }
} })) }))
let wrapper
afterEach(() => wrapper?.unmount())
describe('real level history and progress', () => {
  it('uses returned list/current/count and deterministic server progress', async () => {
    localStorage.clear(); setActivePinia(createPinia()); useAuthStore().currentUser = { id: 7 }
    wrapper = shallowMount(UserLevelHistory, { global: { directives: { loading: () => {} }, stubs: { 'el-table-column': true } } }); await flushPromises()
    const state = wrapper.vm.$.setupState
    expect(getUserLevelHistory).toHaveBeenCalledWith(7, { page: 1, pageSize: 20 })
    expect(state.levelHistory).toEqual([{ id: 1, newLevel: 2 }])
    expect(state.currentLevelName).toBe('高级用户')
    expect(state.totalChanges).toBe(12)
    expect(state.getRequirementProgress('minArticles', 50)).toBe(20)
    expect(state.getRequirementProgress('minArticles', 50)).toBe(20)
    expect(state.upgradeRequirements.description).toBeUndefined()
  })
})
