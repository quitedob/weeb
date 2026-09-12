import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import ElementPlus, { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import instance from '@/api/axiosInstance'
import Groups from './Groups.vue'
import GroupPage from './GroupPage.vue'
import GroupDetail from './GroupDetail.vue'
import groupApi from '@/api/modules/group'

const { auth, push, setActiveChat } = vi.hoisted(() => ({
  auth: { currentUser: { id: '1' }, accessToken: 'group-test-token' },
  push: vi.fn(), setActiveChat: vi.fn()
}))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => auth }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => ({ setActiveChat }) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { groupId: '44' } }), useRouter: () => ({ push, go: vi.fn() }) }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const owned = { id: 44, groupName: 'Owned group', ownerId: 1, memberCount: 2, role: 1,
  currentUserRole: 'OWNER', sharedChatId: 144, groupDescription: 'About this group', createdAt: '2026-09-12T10:00:00' }
const joined = { id: 45, groupName: 'Joined group', ownerId: 2, memberCount: 3, role: 2,
  currentUserRole: 'ADMIN', sharedChatId: 145, createdAt: '2026-09-12T10:00:00' }
const originalAdapter = instance.defaults.adapter
let wrapper, requests, myGroups, ownedGroups, searchResults, details, members

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
  vi.spyOn(ElLoading, 'service').mockReturnValue({ close: vi.fn() })
  requests = []
  myGroups = [{ ...owned }, { ...joined }]
  ownedGroups = [{ ...owned }]
  details = { ...owned }
  members = [{ userId: 1, username: 'Owner', role: 1 }, { userId: 4, username: 'Guest', role: 3 }]
  searchResults = [{ id: 77, groupName: 'Discovered group', ownerId: 9, memberCount: 2, currentUserRole: 'NON_MEMBER' }]
  instance.defaults.adapter = async config => {
    const body = config.data ? JSON.parse(config.data) : undefined
    requests.push({ method: config.method, url: config.url, params: config.params, body })
    let data
    if (config.method === 'delete') {
      if (config.url.endsWith('/members/me')) {
        const id = config.url.split('/')[3]
        myGroups = myGroups.filter(group => String(group.id) !== id)
      } else if (config.url.includes('/members/')) {
        const id = config.url.split('/').at(-1)
        members = members.filter(member => String(member.userId) !== id)
      }
      return { status: 204, headers: {}, config, data: '' }
    }
    if (config.url === '/api/groups/my-groups' || config.url === '/api/groups/my-created') {
      const rows = (config.url.endsWith('my-created') ? ownedGroups : myGroups)
        .filter(group => !config.params?.excludeOwned || String(group.ownerId) !== String(auth.currentUser.id))
      const { page, size } = config.params || {}
      data = page == null ? rows : { list: rows.slice(page * size, (page + 1) * size), total: rows.length, page, size }
    }
    else if (config.url === '/api/groups/search') data = searchResults
    else if (config.url === '/api/search/group') {
      const { page, size } = config.params
      data = { list: searchResults.slice(page * size, (page + 1) * size), total: searchResults.length }
    }
    else if (config.url === '/api/groups/44') data = details
    else if (config.url === '/api/groups/44/members') data = members
    else if (config.url.startsWith('/api/users/')) data = { id: 1, username: 'Owner' }
    else if (config.url === '/api/groups' && config.method === 'post') {
      const created = { ...owned, ...body, id: 88, sharedChatId: 188 }
      ownedGroups.push(created)
      myGroups.push(created)
      data = created
    } else if (config.url.endsWith('/applications')) data = 'Application submitted'
    else throw new Error(`Unexpected request: ${config.method} ${config.url}`)
    return { status: config.method === 'post' ? 201 : 200, headers: {}, config, data: { code: 0, message: 'OK', data } }
  }
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  document.body.innerHTML = ''
  instance.defaults.adapter = originalAdapter
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
async function render(component) {
  wrapper = mount(component, { attachTo: document.body, global: { plugins: [ElementPlus] } })
  await flushPromises()
}
async function selectTab(label) {
  await wrapper.findAll('[role="tab"]').find(tab => tab.text().startsWith(label)).trigger('click')
  await flushPromises()
}
const buttonNamed = (container, label) => container.findAll('button').find(button => button.text() === label)
const listPages = [{ name: 'Groups', component: Groups }, { name: 'GroupPage', component: GroupPage }]

describe('routed group page journeys with real API responses', () => {
  it('preserves the legacy no-parameter group arrays for older callers', async () => {
    expect((await groupApi.getMyGroups()).data).toHaveLength(2)
    expect((await groupApi.getMyCreatedGroups()).data).toHaveLength(1)
    expect(requests.every(request => request.params === undefined)).toBe(true)
  })

  it.each(listPages)('uses discovery roles independently of the membership page in $name', async ({ component }) => {
    myGroups = Array.from({ length: 31 }, (_, index) => ({ ...joined, id: 100 + index, groupName: `Member ${index}` }))
    searchResults = [{ ...joined, id: 130, groupName: 'Off-page membership', currentUserRole: 'ADMIN' },
      ...Array.from({ length: 10 }, (_, index) => ({ id: 300 + index, groupName: `Discovery ${index}`, currentUserRole: 'NON_MEMBER' }))]
    await render(component)
    expect(requests.filter(request => request.url === '/api/groups/my-groups').every(request => request.params.size === 10)).toBe(true)
    await selectTab('发现群组')
    await wrapper.get('input[placeholder="搜索群组名称或ID"]').setValue('Discovery')
    await wrapper.get('.el-input-group__append button').trigger('click')
    await flushPromises()
    const pane = wrapper.get('#pane-discoverGroups')
    expect(pane.findAll('.group-card')).toHaveLength(10)
    expect(buttonNamed(pane, '已加入').element.disabled).toBe(true)
    await pane.get('.btn-next').trigger('click')
    await flushPromises()
    expect(pane.findAll('.group-card')).toHaveLength(1)
    expect(pane.text()).toContain('Discovery 9')
    expect(requests.filter(request => request.url === '/api/search/group').map(request => request.params.page)).toEqual([0, 1])
  })

  it.each(listPages)('keeps the newer discovery request loading when a cancelled older request finishes in $name', async ({ component }) => {
    await render(component)
    await selectTab('发现群组')
    const original = instance.defaults.adapter
    const pending = []
    instance.defaults.adapter = config => config.url !== '/api/search/group' ? original(config)
      : new Promise(resolve => pending.push(data => resolve({ status: 200, config, headers: {}, data: { code: 0, message: 'OK', data } })))
    const input = wrapper.get('input[placeholder="搜索群组名称或ID"]')
    await input.setValue('Old query'); await input.trigger('keyup', { key: 'Enter' })
    await input.setValue('New query'); await input.trigger('keyup', { key: 'Enter' })
    pending[0]({ list: [{ id: 400, groupName: 'Old result' }], total: 1 })
    await flushPromises()
    expect(wrapper.get('#pane-discoverGroups').find('.el-skeleton').exists()).toBe(true)
    pending[1]({ list: [{ id: 401, groupName: 'New result', currentUserRole: 'NON_MEMBER' }], total: 1 })
    await flushPromises()
    expect(wrapper.get('#pane-discoverGroups').text()).toContain('New result')
    expect(wrapper.get('#pane-discoverGroups').text()).not.toContain('Old result')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it.each(listPages)('preserves the description when creating from $name', async ({ component }) => {
    await render(component)
    await buttonNamed(wrapper, '创建群组').trigger('click')
    await flushPromises()
    const dialog = new DOMWrapper(document.body.querySelector('.el-dialog'))
    await dialog.get('input').setValue('群')
    await dialog.get('textarea').setValue('Created description')
    await buttonNamed(dialog, '确定创建').trigger('click')
    await flushPromises()
    expect(requests.find(request => request.method === 'post' && request.url === '/api/groups').body).toEqual({
      groupName: '群', groupDescription: 'Created description'
    })
    expect(wrapper.findAll('.group-card').some(card => card.text().includes('群ID: 88'))).toBe(true)
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('searches from Groups with a bounded page and displays the public owner ID fallback', async () => {
    await render(Groups)
    await selectTab('发现群组')
    await wrapper.get('input[placeholder="搜索群组名称或ID"]').setValue('Study')
    await wrapper.get('.el-input-group__append button').trigger('click')
    await flushPromises()
    expect(requests.find(request => request.url === '/api/search/group').params).toEqual({ keyword: 'Study', page: 0, size: 10 })
    expect(wrapper.get('#pane-discoverGroups').text()).toContain('群主: 9')
  })

  it('starts management discovery at page zero for Enter and button clicks and sends application message', async () => {
    await render(GroupPage)
    await selectTab('发现群组')
    const input = wrapper.get('input[placeholder="搜索群组名称或ID"]')
    await input.setValue('Study')
    await input.trigger('keyup', { key: 'Enter' })
    await flushPromises()
    await wrapper.get('.el-input-group__append button').trigger('click')
    await flushPromises()
    const searches = requests.filter(request => request.url === '/api/search/group')
    expect(searches).toHaveLength(2)
    expect(searches.every(request => request.params.page === 0 && request.params.size === 10)).toBe(true)
    await buttonNamed(wrapper.get('#pane-discoverGroups'), '申请加入').trigger('click')
    await flushPromises()
    expect(requests.find(request => request.url === '/api/groups/77/applications').body).toEqual({ groupId: 77, message: '申请加入群组' })
  })

  it.each(listPages)('opens the shared chat rather than the group ID from $name', async ({ component }) => {
    await render(component)
    const card = wrapper.findAll('.group-card').find(card => card.text().includes('Owned group'))
    await buttonNamed(card, '进入群聊').trigger('click')
    await flushPromises()
    expect(setActiveChat).toHaveBeenCalledWith(expect.objectContaining({ id: 144, sharedChatId: 144, groupId: 44 }))
    expect(push).toHaveBeenCalledWith({ path: '/chat', query: { chatId: '144', type: 'GROUP', groupId: '44' } })
    expect(requests.some(request => request.url === '/api/groups/44')).toBe(false)
  })

  it.each(listPages)('fetches missing shared chat details and refuses an incomplete destination from $name', async ({ component }) => {
    myGroups[0].sharedChatId = null
    ownedGroups[0].sharedChatId = null
    details.sharedChatId = null
    await render(component)
    const card = wrapper.findAll('.group-card').find(card => card.text().includes('Owned group'))
    await buttonNamed(card, '进入群聊').trigger('click')
    await flushPromises()
    expect(requests.some(request => request.url === '/api/groups/44')).toBe(true)
    expect(push).not.toHaveBeenCalled()
    expect(setActiveChat).not.toHaveBeenCalled()
    details.sharedChatId = 244
    await buttonNamed(card, '进入群聊').trigger('click')
    await flushPromises()
    expect(push).toHaveBeenCalledWith({ path: '/chat', query: { chatId: '244', type: 'GROUP', groupId: '44' } })
  })

  it('shows the administrator label from the group DTO', async () => {
    await render(Groups)
    const card = wrapper.findAll('.group-card').find(card => card.text().includes('Joined group'))
    expect(card.get('.el-tag').text()).toBe('管理员')
  })

  it('requests server membership pages and refreshes after leaving the final page with HTTP 204', async () => {
    ownedGroups = Array.from({ length: 11 }, (_, index) => ({ ...owned, id: 100 + index, sharedChatId: 1000 + index, groupName: `Owned ${index}` }))
    const otherGroups = Array.from({ length: 11 }, (_, index) => ({ ...joined, id: 200 + index, sharedChatId: 2000 + index, groupName: `Joined ${index}` }))
    myGroups = [...ownedGroups, ...otherGroups]
    await render(GroupPage)
    const managedPane = wrapper.get('#pane-managedGroups')
    expect(managedPane.findAll('.group-card')).toHaveLength(10)
    await managedPane.get('.btn-next').trigger('click')
    await flushPromises()
    expect(managedPane.findAll('.group-card')).toHaveLength(1)
    expect(managedPane.text()).toContain('Owned 10')
    expect(requests.filter(request => request.url === '/api/groups/my-created').map(request => request.params.page)).toEqual([0, 1])
    await selectTab('我加入的群组')
    const joinedPane = wrapper.get('#pane-joinedGroups')
    expect(joinedPane.findAll('.group-card')).toHaveLength(10)
    await joinedPane.get('.btn-next').trigger('click')
    await flushPromises()
    expect(joinedPane.findAll('.group-card')).toHaveLength(1)
    expect(requests.filter(request => request.url === '/api/groups/my-groups').map(request => request.params))
      .toEqual([{ page: 0, size: 10, excludeOwned: true }, { page: 1, size: 10, excludeOwned: true }])
    await buttonNamed(joinedPane, '退出群组').trigger('click')
    await flushPromises()
    expect(requests.some(request => request.method === 'delete' && request.url === '/api/groups/210/members/me')).toBe(true)
    expect(joinedPane.findAll('.group-card')).toHaveLength(10)
    expect(joinedPane.text()).not.toContain('Joined 10')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('refreshes visible members after a successful HTTP 204 kick', async () => {
    await render(GroupDetail)
    expect(wrapper.get('.group-info-desc').text()).not.toContain('N/A')
    await selectTab('成员')
    await buttonNamed(wrapper.get('.members-table'), '踢出').trigger('click')
    await flushPromises()
    expect(wrapper.get('.members-table').text()).not.toContain('Guest')
    expect(ElMessage.success).toHaveBeenCalledWith('成员已踢出')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('offers an application from a public non-member preview without exposing membership controls', async () => {
    details = { ...details, role: null, currentUserRole: 'NON_MEMBER' }
    await render(GroupDetail)
    expect(requests.some(request => request.url === '/api/groups/44/members')).toBe(false)
    expect(wrapper.findAll('[role="tab"]').some(tab => tab.text().startsWith('成员'))).toBe(false)
    expect(buttonNamed(wrapper, '进入群聊')).toBeUndefined()
    expect(buttonNamed(wrapper, '退出群组')).toBeUndefined()
    await buttonNamed(wrapper, '申请加入').trigger('click')
    await flushPromises()
    expect(requests.find(request => request.url === '/api/groups/44/applications').body).toEqual({ groupId: 44, message: '请允许我加入群组' })
    expect(ElMessage.success).toHaveBeenCalledWith('申请已提交，请等待群主或管理员审核')
    const submitted = buttonNamed(wrapper, '申请已提交')
    expect(submitted.element.disabled).toBe(true)
    await submitted.trigger('click')
    expect(requests.filter(request => request.url === '/api/groups/44/applications')).toHaveLength(1)
    expect(push).not.toHaveBeenCalled()
  })

  it.each(['leave', 'disband'])('returns to the list after a successful HTTP 204 %s', async action => {
    if (action === 'leave') details = { ...details, ownerId: 2, role: 3, currentUserRole: 'MEMBER' }
    await render(GroupDetail)
    await buttonNamed(wrapper, action === 'leave' ? '退出群组' : '解散群组').trigger('click')
    await flushPromises()
    expect(push).toHaveBeenCalledWith('/groups')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })
})
