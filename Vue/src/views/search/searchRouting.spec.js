import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import SearchPage from './SearchPage.vue'
import searchApi from '@/api/modules/search'

const { push, error } = vi.hoisted(() => ({ push: vi.fn(), error: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ currentUser: null }) }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => ({}) }))
vi.mock('element-plus', () => ({ ElMessage: { error, warning: vi.fn() }, ElMessageBox: {}, ElLoading: {} }))
vi.mock('@/api/modules/search', () => ({ default: {
  searchUsers: vi.fn(), searchGroups: vi.fn(), searchArticles: vi.fn(), searchMessages: vi.fn()
} }))

let wrapper
beforeEach(() => {
  vi.clearAllMocks()
  for (const name of ['searchUsers', 'searchGroups', 'searchArticles']) {
    searchApi[name].mockResolvedValue({ code: 0, data: { list: [], total: 0 } })
  }
})
afterEach(() => wrapper?.unmount())

async function openMessage(result) {
  searchApi.searchMessages.mockResolvedValue({ code: 0, data: { list: [result], total: 1 } })
  wrapper = shallowMount(SearchPage, { global: {
    renderStubDefaultSlot: true,
    stubs: {
      ElIcon: true, ElDatePicker: true, ElOption: true, ElSelect: true,
      ElCollapseItem: true, ElCollapse: true, ElSkeleton: true, ElEmpty: true,
      ElPagination: true, ElTabPane: true, ElTabs: true,
      ElInput: {
        props: ['modelValue'], emits: ['update:modelValue', 'keyup'],
        template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup="$emit(\'keyup\', $event)" />'
      },
      ElCard: { template: '<article><slot name="header"/><slot/><slot name="footer"/></article>' },
      ElButton: { template: '<button><slot/></button>' }
    }
  } })
  await wrapper.find('input').setValue('matching message')
  await wrapper.find('input').trigger('keyup', { key: 'Enter' })
  await flushPromises()
  expect(wrapper.find('.message-content').text()).toBe('matching message')
  const button = wrapper.findAll('button').find(item => item.text() === '查看消息')
  await button.trigger('click')
}

describe('message search navigation', () => {
  it.each([
    [{ type: 'GROUP', targetId: 9, groupId: 9, sharedChatId: 42 }, 'group', { groupId: '9' }],
    [{ type: 'PRIVATE', targetId: 42, chatId: 42 }, 'private', {}]
  ])('opens the canonical conversation from the result', async (result, type, query) => {
    await openMessage({ id: 101, content: 'matching message', ...result })
    expect(push).toHaveBeenCalledWith({ name: 'SpecificChat', params: { type, id: '42' }, query })
  })

  it('shows an error when the result has no conversation ID', async () => {
    await openMessage({ id: 101, content: 'matching message', type: 'GROUP', targetId: 9 })
    expect(push).not.toHaveBeenCalled()
    expect(error).toHaveBeenCalled()
  })
})
