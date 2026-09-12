import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import SearchPage from './SearchPage.vue'
import instance from '@/api/axiosInstance'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => ({ currentUser: null, accessToken: 'test-token' }) }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => ({}) }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), warning: vi.fn() }, ElMessageBox: {}, ElLoading: {} }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const originalAdapter = instance.defaults.adapter
let wrapper
let requests

beforeEach(() => {
  requests = []
  instance.defaults.adapter = async config => {
    requests.push(config)
    return { status: 200, statusText: 'OK', headers: {}, config,
      data: { code: 0, message: 'OK', data: {
        list: config.url === '/api/search/articles' ? [{ id: 1, articleTitle: 'Matching article' }] : [],
        total: config.url === '/api/search/articles' ? 31 : 0,
      } },
    }
  }
  wrapper = shallowMount(SearchPage, { global: {
    renderStubDefaultSlot: true,
    stubs: {
      ElIcon: true, ElDatePicker: true, ElCollapseItem: true, ElCollapse: true,
      ElSkeleton: true, ElEmpty: true, ElPagination: true, ElTabPane: true, ElTabs: true,
      ElInput: {
        props: ['modelValue'], emits: ['update:modelValue', 'keyup'],
        template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup="$emit(\'keyup\', $event)" />',
      },
      ElSelect: {
        props: ['modelValue', 'placeholder', 'remoteMethod'], emits: ['update:modelValue'],
        template: '<div><input v-if="remoteMethod" :aria-label="placeholder" @input="remoteMethod($event.target.value)"/><select :aria-label="placeholder" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot/></select></div>',
      },
      ElOption: { props: ['value', 'label'], template: '<option :value="value">{{ label }}</option>' },
      ElCard: { template: '<article><slot name="header"/><slot/><slot name="footer"/></article>' },
      ElButton: { template: '<button><slot/></button>' },
    },
  } })
})

afterEach(() => {
  wrapper?.unmount()
  instance.defaults.adapter = originalAdapter
})

async function searchWithSort(sort) {
  await wrapper.find('input').setValue('matching')
  await wrapper.find('select[aria-label="选择排序方式"]').setValue(sort)
  await wrapper.find('input').trigger('keyup', { key: 'Enter' })
  await flushPromises()
}

describe('search page sort request contracts', () => {
  it('finishes the active search when an empty Enter is submitted while its requests are pending', async () => {
    const pending = []
    instance.defaults.adapter = config => new Promise(resolve => pending.push(() => resolve({
      status: 200, headers: {}, config, data: { code: 0, message: 'OK', data: { list: [], total: 0 } }
    })))
    const input = wrapper.find('input')
    await input.setValue('active'); await input.trigger('keyup', { key: 'Enter' })
    expect(pending).toHaveLength(4)
    expect(wrapper.vm.$.setupState.searching).toBe(true)
    await input.setValue(''); await input.trigger('keyup', { key: 'Enter' })
    pending.forEach(reply => reply()); await flushPromises()
    expect(wrapper.vm.$.setupState.searching).toBe(false)
    await input.setValue('next'); await input.trigger('keyup', { key: 'Enter' })
    expect(pending).toHaveLength(8)
    pending.slice(4).forEach(reply => reply()); await flushPromises()
    expect(wrapper.vm.$.setupState.searching).toBe(false)
  })

  it('keeps all four resources on the newest query when older responses arrive later', async () => {
    const pending = []
    instance.defaults.adapter = config => new Promise(resolve => pending.push({ config, reply: label => resolve({
      status: 200, headers: {}, config, data: { code: 0, message: 'OK', data: { list: [{ id: 1,
        username: label, groupName: label, articleTitle: label, content: label, currentUserRole: 'MEMBER' }], total: 1 } }
    }) }))
    const input = wrapper.find('input')
    await input.setValue('older'); await input.trigger('keyup', { key: 'Enter' })
    await input.setValue('newer'); await input.trigger('keyup', { key: 'Enter' })
    expect(pending).toHaveLength(8)
    expect(pending.slice(0, 4).every(request => request.config.signal.aborted)).toBe(true)
    pending.slice(4).forEach(request => request.reply('newest result'))
    await flushPromises()
    pending.slice(0, 4).forEach(request => request.reply('stale result'))
    await flushPromises()
    for (const selector of ['.username', '.group-name', '.article-title', '.message-content']) {
      expect(wrapper.get(selector).text()).toBe('newest result')
    }
    expect(wrapper.text()).not.toContain('stale result')
    expect(wrapper.text()).toContain('已加入')
  })

  it.each(['搜索用户', '搜索群组'])('does not restore cleared %s autocomplete options from an old request', async label => {
    const pending = []
    instance.defaults.adapter = config => new Promise(resolve => pending.push({ config, reply: () => resolve({
      status: 200, headers: {}, config, data: { code: 0, message: 'OK', data: { list: [{ id: 1, username: 'Old option', groupName: 'Old option' }], total: 1 } }
    }) }))
    const input = wrapper.get(`input[aria-label="${label}"]`)
    await input.setValue('old')
    await input.setValue('')
    expect(pending[0].config.signal.aborted).toBe(true)
    pending[0].reply()
    await flushPromises()
    expect(wrapper.text()).not.toContain('Old option')
  })

  it.each([
    ['relevance', 'relevance', 'relevance', 'desc'],
    ['time_desc', 'time_desc', 'created_at', 'desc'],
    ['time_asc', 'time_asc', 'created_at', 'asc'],
    ['username_asc', 'name_asc', 'title', 'asc'],
    ['username_desc', 'name_desc', 'title', 'desc'],
  ])('maps the selectable %s sort through the real search adapters', async (selection, nameSort, articleSort, order) => {
    await searchWithSort(selection)
    expect(requests).toHaveLength(4)
    const paramsByUrl = Object.fromEntries(requests.map(({ url, params }) => [url, params]))
    expect(paramsByUrl).toEqual({
      '/api/search/users': { keyword: 'matching', page: 0, size: 10, sortBy: nameSort },
      '/api/search/group': { keyword: 'matching', page: 0, size: 10, sortBy: nameSort },
      '/api/search/articles': { query: 'matching', page: 1, pageSize: 10, sortBy: articleSort, sortOrder: order },
      '/api/search/messages': { q: 'matching', page: 0, size: 10, sortBy: selection },
    })
    expect(wrapper.find('.article-title').text()).toBe('Matching article')
  })

  it('keeps the chosen article order when requesting a later page', async () => {
    await searchWithSort('time_asc')
    requests.length = 0
    wrapper.findComponent({ name: 'ElPagination' }).vm.$emit('current-change', 2)
    await flushPromises()
    expect(requests).toHaveLength(1)
    expect(requests[0]).toMatchObject({ url: '/api/search/articles', params: {
      query: 'matching', page: 2, pageSize: 10, sortBy: 'created_at', sortOrder: 'asc',
    } })
  })
})
