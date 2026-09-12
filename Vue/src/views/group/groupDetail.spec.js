import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import ElementPlus, { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import GroupDetail from './GroupDetail.vue'

const { api, auth } = vi.hoisted(() => ({
  api: {
    group: { getGroupDetails: vi.fn(), getMembers: vi.fn(), kickMember: vi.fn(), updateGroup: vi.fn(), getGroupApplications: vi.fn() },
    user: { getUserById: vi.fn() }
  },
  auth: { currentUser: { id: '2' } }
}))
vi.mock('@/api', () => ({ default: api }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => auth }))
vi.mock('@/stores/chatStore', () => ({ useChatStore: () => ({ setActiveChat: vi.fn() }) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { groupId: '44' } }), useRouter: () => ({ push: vi.fn(), go: vi.fn() }) }))

const members = [
  { userId: 1, username: 'Owner', role: 1 },
  { userId: 2, username: 'CurrentAdmin', role: 2 },
  { userId: 3, username: 'PeerAdmin', role: 2 },
  { userId: 4, username: 'OrdinaryMember', role: 3 },
  { userId: 5, username: 'UnknownRole', role: 99 }
]
const details = {
  id: 44, groupName: 'Study group', ownerId: '1', groupDescription: 'Original description'
}
let wrapper
beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  vi.spyOn(ElMessage, 'success').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => {})
  vi.spyOn(ElMessage, 'error').mockImplementation(() => {})
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm')
  vi.spyOn(ElLoading, 'service').mockReturnValue({ close: vi.fn() })
  api.group.getMembers.mockResolvedValue({ code: 0, data: members })
  api.user.getUserById.mockResolvedValue({ code: 0, data: { user: { id: 1, username: 'Owner' }, stats: {} } })
  api.group.getGroupApplications.mockResolvedValue({ code: 0, data: [] })
  api.group.kickMember.mockResolvedValue({ code: 0 })
  api.group.updateGroup.mockResolvedValue({ code: 0 })
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  document.body.innerHTML = ''
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

async function render(role, userId) {
  auth.currentUser = { id: userId }
  api.group.getGroupDetails.mockResolvedValue({ code: 0, data: { ...details, currentUserRole: role } })
  wrapper = mount(GroupDetail, { attachTo: document.body, global: { plugins: [ElementPlus] } })
  await flushPromises()
  return wrapper
}
async function showMembers() {
  await wrapper.findAll('[role="tab"]').find(tab => tab.text().startsWith('成员')).trigger('click')
  await flushPromises()
}
const memberRow = username => wrapper.findAll('.members-table .el-table__row').find(row => row.text().includes(username))
const removeButtons = username => memberRow(username).findAll('button').filter(button => button.text() === '踢出')

describe('group detail API contracts', () => {
  it('reads owner and applicant profiles from the backend UserWithStats envelope', async () => {
    api.user.getUserById.mockImplementation(async id => ({ code: 0, data: {
      user: { id: Number(id), username: Number(id) === 1 ? 'ProfileOwner' : 'ProfileApplicant', avatar: '/uploads/avatars/profile.png' },
      stats: { followersCount: 7 }
    } }))
    api.group.getGroupApplications.mockResolvedValue({ code: 0, data: [
      { id: 19, userId: 9, status: 'PENDING', message: 'Please accept' }
    ] })
    await render('OWNER', '1')
    expect(wrapper.get('.group-info-desc').text()).toContain('ProfileOwner')
    await wrapper.findAll('[role="tab"]').find(tab => tab.text() === '申请管理').trigger('click')
    await wrapper.findAll('button').find(button => button.text() === '刷新待审批').trigger('click')
    await flushPromises()
    expect(wrapper.get('.applications-table').text()).toContain('ProfileApplicant')
    expect(wrapper.get('.applications-table img').attributes('src')).toBe('/uploads/avatars/profile.png')
  })

  it('renders numeric roles and lets an administrator remove only ordinary members', async () => {
    await render('ADMIN', '2')
    await showMembers()
    expect(memberRow('Owner').get('.el-tag').text()).toBe('群主')
    expect(memberRow('PeerAdmin').get('.el-tag').text()).toBe('管理员')
    expect(memberRow('PeerAdmin').get('.el-tag').classes()).toContain('el-tag--warning')
    expect(memberRow('OrdinaryMember').get('.el-tag').text()).toBe('成员')
    expect(removeButtons('Owner')).toHaveLength(0)
    expect(removeButtons('CurrentAdmin')).toHaveLength(0)
    expect(removeButtons('PeerAdmin')).toHaveLength(0)
    expect(removeButtons('UnknownRole')).toHaveLength(0)
    await removeButtons('OrdinaryMember')[0].trigger('click')
    await flushPromises()
    expect(api.group.kickMember).toHaveBeenCalledExactlyOnceWith({ groupId: 44, userIdToKick: 4 })
  })

  it('allows the owner to remove administrators and members while protecting owner and unknown roles', async () => {
    await render('OWNER', '1')
    await showMembers()
    expect(removeButtons('Owner')).toHaveLength(0)
    expect(removeButtons('UnknownRole')).toHaveLength(0)
    expect(removeButtons('CurrentAdmin')).toHaveLength(1)
    await removeButtons('PeerAdmin')[0].trigger('click')
    await flushPromises()
    await removeButtons('OrdinaryMember')[0].trigger('click')
    await flushPromises()
    expect(api.group.kickMember.mock.calls.map(([payload]) => payload)).toEqual([
      { groupId: 44, userIdToKick: 3 },
      { groupId: 44, userIdToKick: 4 }
    ])
  })

  it('offers no member removal controls to an ordinary member', async () => {
    await render('MEMBER', '4')
    await showMembers()
    expect(wrapper.find('.members-table').findAll('button').filter(button => button.text() === '踢出')).toHaveLength(0)
    expect(api.group.kickMember).not.toHaveBeenCalled()
  })

  it('loads, edits and submits groupDescription without losing the API field', async () => {
    await render('OWNER', '1')
    expect(wrapper.get('.group-info-desc').text()).toContain('Original description')
    await wrapper.findAll('button').find(button => button.text() === '编辑群信息').trigger('click')
    await flushPromises()
    const dialog = new DOMWrapper(document.body.querySelector('.el-dialog'))
    expect(dialog.get('textarea').element.value).toBe('Original description')
    await dialog.get('input').setValue('Updated study group')
    await dialog.get('textarea').setValue('Updated description')
    api.group.getGroupDetails.mockResolvedValue({ code: 0, data: {
      ...details, currentUserRole: 'OWNER', groupName: 'Updated study group', groupDescription: 'Updated description'
    } })
    await dialog.findAll('button').find(button => button.text() === '保存').trigger('click')
    await flushPromises()
    expect(api.group.updateGroup).toHaveBeenCalledExactlyOnceWith({
      id: 44, groupName: 'Updated study group', groupDescription: 'Updated description'
    })
    expect(wrapper.get('.group-info-desc').text()).toContain('Updated description')
  })
})
