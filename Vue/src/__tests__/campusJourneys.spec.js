import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { reactive } from 'vue'
import { mount, flushPromises, DOMWrapper } from '@vue/test-utils'
import { AxiosError } from 'axios'
import http from '@/api/axiosInstance'
import CampusPage from '@/views/campus/CampusPage.vue'
import CampusPostEditor from '@/views/campus/CampusPostEditor.vue'
import CampusPostPage from '@/views/campus/CampusPostPage.vue'
import CampusAdminPage from '@/views/campus/CampusAdminPage.vue'
import CampusImage from '@/components/campus/CampusImage.vue'
import { getNotificationRoute, getNotificationTypeText } from '@/utils/notificationPresentation'

const { state, push } = vi.hoisted(() => ({ state: {}, push: vi.fn() }))
vi.mock('@/stores/authStore', () => ({ useAuthStore: () => state.auth }))
vi.mock('vue-router', () => ({ useRoute: () => state.route, useRouter: () => ({ push }) }))
vi.mock('@/utils/appleMessage', () => ({ default: { error: vi.fn() } }))

const originalAdapter = http.defaults.adapter
let wrapper, requests, handle, school, post, comments
const schoolDto = () => ({ id: 1, name: '校园一', description: '校园介绍', active: true, preModeration: true, version: 3, memberCount: 4,
  membership: { status: 'VERIFIED', role: 'MEMBER', version: 1 }, latestApplication: null,
  capabilities: { canRead: true, canPost: true, canModerate: false, canManageMembers: false, canManageSchool: false, isSiteAdmin: false } })
const postDto = () => ({ id: 8, schoolId: 1, schoolName: '校园一', author: { id: 1, username: 'Author', nickname: '同学' }, title: '学习交流', content: '<script>unsafe()</script>保持纯文本', category: 'STUDY', status: 'PUBLISHED', version: 5, images: [], pinned: false, likeCount: 2, commentCount: 1, likedByMe: false, bookmarkedByMe: false, canEdit: true, canDelete: true, canModerate: false, createdAt: '2026-09-12T10:00:00' })
const page = (list, number = 0, total = list.length) => ({ list, total, page: number, size: 20 })
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }
function fail(config, status, message) { throw new AxiosError(message, 'ERR_BAD_RESPONSE', config, null, { status, data: { code: status, message }, headers: {}, config }) }
function ok(config, data) { return { status: 200, config, headers: {}, data: data instanceof Blob ? data : { code: 0, message: 'OK', data } } }
beforeEach(() => {
  vi.clearAllMocks(); localStorage.clear()
  state.auth = reactive({ currentUser: { id: 1, type: 'USER' }, accessToken: 'campus-test', sessionEpoch: 1 })
  state.route = reactive({ params: { schoolId: '1' }, fullPath: '/campus/schools/1' })
  school = schoolDto(); post = postDto(); comments = [{ id: 4, postId: 8, author: { id: 2, username: 'Other' }, content: '原评论', replyToCommentId: null, deleted: false, canDelete: false, createdAt: '2026-09-12T10:00:00' }]
  requests = []; handle = null
  vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} unobserve() {} })
  vi.stubGlobal('requestAnimationFrame', callback => setTimeout(callback, 0))
  URL.createObjectURL = vi.fn(() => 'blob:campus-test'); URL.revokeObjectURL = vi.fn()
  http.defaults.adapter = async config => {
    const body = config.data instanceof FormData ? config.data : config.data ? JSON.parse(config.data) : undefined
    requests.push({ method: config.method, url: config.url, params: config.params, body, config })
    const custom = handle?.(config, body)
    if (custom !== undefined) return await custom
    if (config.method === 'get' && config.url === '/api/campus/schools/1') return ok(config, school)
    if (config.method === 'get' && config.url === '/api/campus/schools') return ok(config, page([school], config.params.page, 41))
    if (config.method === 'get' && config.url === '/api/campus/schools/1/posts') return ok(config, page([post], config.params.page, 41))
    if (config.method === 'get' && config.url === '/api/campus/posts/8') return ok(config, post)
    if (config.method === 'get' && config.url === '/api/campus/posts/8/comments') return ok(config, page(comments, config.params.page))
    if (config.method === 'get' && config.url.includes('/applications')) return ok(config, page([]))
    if (config.method === 'get' && config.url.includes('/media/')) return ok(config, new Blob(['pixels'], { type: 'image/png' }))
    throw new Error(`Unimplemented test endpoint ${config.method} ${config.url}`)
  }
})
afterEach(() => {
  wrapper?.unmount(); wrapper = null
  document.body.innerHTML = ''; http.defaults.adapter = originalAdapter
  vi.unstubAllGlobals(); vi.restoreAllMocks()
})
async function render(component) {
  wrapper = mount(component, { attachTo: document.body, global: { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } } })
  await flushPromises()
}
const button = (label, container = wrapper) => container.findAll('button').find(item => item.text() === label)
const bodyView = () => new DOMWrapper(document.body)

describe('campus directory, membership and feed with real Axios contracts', () => {
  it('sends bounded server paging, literal search and membership filter', async () => {
    state.route.params = {}; state.route.fullPath = '/campus'
    await render(CampusPage)
    await wrapper.get('input[type=search]').setValue('100%_校园')
    await wrapper.get('input[type=checkbox]').setValue(true)
    await flushPromises(); await button('下一页').trigger('click'); await flushPromises()
    expect(requests.at(-1)).toMatchObject({ url: '/api/campus/schools', params: { q: '100%_校园', mine: true, page: 1, size: 20 } })
    expect(wrapper.text()).toContain('共 41 条')
  })
  it('does not fetch protected feed before membership; submits application without actor or role', async () => {
    school.membership = null; Object.keys(school.capabilities).forEach(key => { school.capabilities[key] = false })
    handle = (config, body) => {
      if (config.method === 'post') { school.latestApplication = { id: 9, status: 'PENDING', version: 0 }; return ok(config, { id: 9, ...body, status: 'PENDING' }) }
    }
    await render(CampusPage)
    const inputs = wrapper.findAll('form input')
    await inputs[0].setValue('测试同学'); await inputs[1].setValue('S001'); await inputs[2].setValue('计算机学院'); await inputs[3].setValue('2025')
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'post')).toMatchObject({ url: '/api/campus/schools/1/applications', body: { realName: '测试同学', studentNumber: 'S001', department: '计算机学院', enrollmentYear: 2025, statement: '' } })
    expect(requests.some(item => item.url.endsWith('/posts'))).toBe(false)
    expect(wrapper.text()).toContain('待审核')
  })
  it('loads private application history and withdraws only its pending application', async () => {
    school.membership = null; school.latestApplication = { id: 9, status: 'PENDING', version: 0 }
    let withdrawn = false
    handle = config => {
      if (config.url.endsWith('/applications/mine')) return ok(config, page([{ id: 9, realName: '本人', studentNumber: 'S001', status: withdrawn ? 'CANCELLED' : 'PENDING' }]))
      if (config.method === 'delete') { withdrawn = true; school.latestApplication.status = 'CANCELLED'; return ok(config, { success: true }) }
    }
    await render(CampusPage); const details = wrapper.get('details'); details.element.open = true; await details.trigger('toggle'); await flushPromises()
    await button('撤回申请').trigger('click'); await flushPromises()
    expect(requests.find(item => item.method === 'delete').url).toBe('/api/campus/schools/1/applications/9')
    expect(wrapper.text()).toContain('已撤回')
  })
  it('uses canonical scope/category/sort and never links removed content', async () => {
    post.status = 'REMOVED'; post.content = ''; post.title = ''; post.reviewReason = '处理原因'
    await render(CampusPage)
    const selects = wrapper.findAll('select')
    await selects[0].setValue('STUDY'); await selects[1].setValue('popular'); await selects[2].setValue('mine'); await flushPromises()
    expect(requests.at(-1).params).toMatchObject({ category: 'STUDY', sort: 'popular', scope: 'mine', page: 0, size: 20 })
    expect(wrapper.find('a[href="/campus/posts/8"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('处理原因')
  })
})

describe('campus editor and protected media', () => {
  it('clears a loaded private draft after upload403 and ignores delayed sibling media', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8/edit'
    post.status = 'DRAFT'; post.images = [{ id: 'visible' }, { id: 'delayed' }]
    const pending = deferred()
    handle = config => {
      if (config.url.endsWith('/media/delayed')) return pending.promise.then(() => ok(config, new Blob(['late'], { type: 'image/png' })))
      if (config.method === 'post') return fail(config, 403, '校园权限已撤销')
    }
    await render(CampusPostEditor)
    expect(wrapper.get('textarea').element.value).toBe(post.content)
    expect(wrapper.find('img').exists()).toBe(true)
    const input = wrapper.get('input[type=file]')
    Object.defineProperty(input.element, 'files', { value: [new File(['pixels'], 'test.png', { type: 'image/png' })], configurable: true })
    await input.trigger('change'); await flushPromises()
    pending.resolve(); await flushPromises()
    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('编辑权限已变化')
    expect(bodyView().text()).not.toContain(post.content)
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:campus-test')
  })
  it('clears post and pending comments when a protected image read returns403', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8'
    post.images = [{ id: 'forbidden' }]
    const pending = deferred()
    handle = config => {
      if (config.url.endsWith('/media/forbidden')) return fail(config, 403, '校园权限已撤销')
      if (config.url.endsWith('/comments')) return pending.promise.then(() => ok(config, page(comments)))
    }
    await render(CampusPostPage); pending.resolve(); await flushPromises()
    expect(wrapper.text()).not.toContain(post.content)
    expect(wrapper.text()).not.toContain('原评论')
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.text()).toContain('访问权限已变化')
  })
  it('uploads a protected attachment and saves exact draft fields in the chosen order', async () => {
    state.route.fullPath = '/campus/schools/1/new'
    handle = (config, data) => {
      if (config.url.endsWith('/media') && config.method === 'post') return ok(config, { id: 'image-one', url: '/api/campus/media/image-one', width: 100, height: 100 })
      if (config.url.endsWith('/posts') && config.method === 'post') return ok(config, { ...post, ...data, status: 'DRAFT' })
    }
    await render(CampusPostEditor)
    await wrapper.get('input[maxlength="120"]').setValue('草稿标题'); await wrapper.get('textarea').setValue('草稿正文')
    const file = new File(['pixels'], 'test.png', { type: 'image/png' }), input = wrapper.get('input[type=file]')
    Object.defineProperty(input.element, 'files', { value: [file], configurable: true }); await input.trigger('change'); await flushPromises()
    expect(requests.find(item => item.method === 'post').body.get('file')).toBeInstanceOf(File)
    expect(wrapper.get('img').attributes('src')).toBe('blob:campus-test')
    await wrapper.get('form').trigger('submit', { submitter: { value: 'draft' } }); await flushPromises()
    expect(requests.find(item => item.url.endsWith('/posts') && item.method === 'post').body).toEqual({ title: '草稿标题', content: '草稿正文', category: 'GENERAL', mediaIds: ['image-one'], submit: false })
    expect(push).toHaveBeenCalledWith('/campus/posts/8')
  })
  it('retains edits after a version conflict and requires explicit replacement', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8/edit'
    handle = config => { if (config.method === 'put') return fail(config, 409, '版本冲突') }
    await render(CampusPostEditor); await wrapper.get('textarea').setValue('不能丢失的编辑')
    await wrapper.get('form').trigger('submit', { submitter: { value: 'publish' } }); await flushPromises()
    expect(wrapper.get('textarea').element.value).toBe('不能丢失的编辑')
    expect(requests.find(item => item.method === 'put').body).toMatchObject({ version: 5, submit: true })
    expect(push).not.toHaveBeenCalled()
    post = { ...post, title: '最新标题', version: 6 }
    await button('核对最新版本').trigger('click'); await flushPromises()
    expect(bodyView().find('[role=dialog]').text()).toContain('最新标题')
    await button('保留当前输入', bodyView()).trigger('click')
    expect(wrapper.get('textarea').element.value).toBe('不能丢失的编辑')
  })
  it('reorders attached images and unlinks them through the versioned save', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8/edit'
    post.images = [{ id: 'first' }, { id: 'second' }, { id: 'third' }]
    handle = (config, data) => config.method === 'put' ? ok(config, { ...post, ...data }) : undefined
    await render(CampusPostEditor)
    await wrapper.get('[aria-label="后移第 1 张图片"]').trigger('click')
    await wrapper.get('[aria-label="移除第 3 张图片"]').trigger('click'); await flushPromises()
    expect(requests.some(item => item.method === 'delete')).toBe(false)
    await wrapper.get('form').trigger('submit', { submitter: { value: 'draft' } }); await flushPromises()
    expect(requests.find(item => item.method === 'put').body.mediaIds).toEqual(['second', 'first'])
  })
  it('does not navigate or clear B state after A save resolves late', async () => {
    state.route.fullPath = '/campus/schools/1/new'; const pending = deferred()
    handle = config => config.method === 'post' ? pending.promise.then(() => ok(config, post)) : undefined
    await render(CampusPostEditor); await wrapper.get('input[maxlength="120"]').setValue('A标题'); await wrapper.get('textarea').setValue('A正文')
    await wrapper.get('form').trigger('submit', { submitter: { value: 'publish' } }); await flushPromises()
    state.auth.sessionEpoch++; state.auth.accessToken = 'B'; state.auth.currentUser = { id: 2, type: 'USER' }
    pending.resolve(); await flushPromises(); expect(push).not.toHaveBeenCalled()
  })
  it('discards a late media blob and revokes a displayed URL on account change/unmount', async () => {
    const pending = deferred(); let count = 0
    handle = config => config.url.includes('/media/') && ++count > 1 ? pending.promise.then(() => ok(config, new Blob(['late'], { type: 'image/png' }))) : undefined
    wrapper = mount(CampusImage, { props: { media: { id: 'image-one' } } }); await flushPromises()
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
    state.auth.sessionEpoch++; state.auth.accessToken = 'B'; await flushPromises()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:campus-test')
    wrapper.unmount(); wrapper = null; pending.resolve(); await flushPromises()
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
  })
})

describe('campus post interactions and notifications', () => {
  it('renders escaped text and uses desired-state reactions with server counts', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8'
    handle = config => config.url.endsWith('/like') ? ok(config, { likedByMe: config.method === 'put', likeCount: config.method === 'put' ? 47 : 46 }) : config.url.endsWith('/bookmark') ? ok(config, { bookmarkedByMe: config.method === 'put' }) : undefined
    await render(CampusPostPage)
    expect(wrapper.find('script').exists()).toBe(false); expect(wrapper.text()).toContain('<script>unsafe()</script>')
    await button('点赞 · 2').trigger('click'); await flushPromises(); expect(button('取消点赞 · 47')).toBeTruthy()
    await button('取消点赞 · 47').trigger('click'); await flushPromises()
    await button('收藏').trigger('click'); await flushPromises(); expect(button('取消收藏')).toBeTruthy()
    expect(requests.filter(item => item.url.endsWith('/like')).map(item => item.method)).toEqual(['put','delete'])
  })
  it('submits replies to the selected comment and keeps text after a failure', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8'
    let reject = true
    handle = (config, data) => { if (config.method === 'post') { if (reject) return fail(config, 503, '服务暂不可用'); return ok(config, { id: 9, ...data }) } }
    await render(CampusPostPage); await button('回复').trigger('click'); await wrapper.get('textarea').setValue('回复正文'); await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(wrapper.get('textarea').element.value).toBe('回复正文')
    reject = false; await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'post').body).toEqual({ content: '回复正文', replyToCommentId: 4 })
    expect(wrapper.get('textarea').element.value).toBe('')
  })
  it('reports through the protected endpoint and deletes a post with its displayed version', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8'
    handle = config => ['post','delete'].includes(config.method) ? ok(config, { success: true }) : undefined
    await render(CampusPostPage); await button('举报').trigger('click'); await flushPromises()
    await bodyView().get('[role=dialog] textarea').setValue('需要管理员核查'); await bodyView().get('[role=dialog] form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'post')).toMatchObject({ url: '/api/campus/posts/8/reports', body: { reason: '需要管理员核查' } })
    await button('删除动态').trigger('click'); await flushPromises(); await button('确认删除', bodyView()).trigger('click'); await flushPromises()
    expect(requests.find(item => item.method === 'delete')).toMatchObject({ url: '/api/campus/posts/8', params: { version: 5 } })
    expect(push).toHaveBeenCalledWith('/campus/schools/1')
  })
  it('routes every campus event through protected entity routes', () => {
    for (const type of ['CAMPUS_LIKE','CAMPUS_COMMENT','CAMPUS_REPLY','CAMPUS_REVIEW']) expect(getNotificationRoute({ type, entityId: 8 })).toBe('/campus/posts/8')
    for (const type of ['CAMPUS_VERIFICATION','CAMPUS_MEMBERSHIP']) expect(getNotificationRoute({ type, entityId: 1 })).toBe('/campus/schools/1')
    expect(getNotificationTypeText('CAMPUS_VERIFICATION')).toBe('校园认证')
    expect(getNotificationRoute({ type: 'ARTICLE_COMMENT', entityId: 8 })).toBe('/article/read/8')
  })
})

describe('campus administration', () => {
  it('clears private rows and the open review dialog after current authorization is denied', async () => {
    management()
    handle = config => {
      if (config.url.endsWith('/applications')) return ok(config, page([{ id: 9, userId: 2, username: 'Applicant', realName: 'private revoked name', studentNumber: 'private-number', department: 'Faculty', enrollmentYear: 2026, statement: 'Private statement', status: 'PENDING', version: 2 }]))
      if (config.method === 'put') return fail(config, 403, '管理权限已撤销')
    }
    await render(CampusAdminPage)
    await button('认证审核').trigger('click'); await flushPromises()
    await button('查看并审核申请').trigger('click'); await flushPromises()
    expect(bodyView().text()).toContain('private revoked name')
    await bodyView().get('[role=dialog] form').trigger('submit'); await flushPromises()
    expect(bodyView().text()).not.toContain('private revoked name')
    expect(bodyView().text()).not.toContain('private-number')
    expect(button('查看并审核申请')).toBeUndefined()
    expect(wrapper.text()).toContain('已清除当前管理资料')
  })

  it('allows private application history and withdrawal when school metadata is denied', async () => {
    let withdrawn = false
    handle = config => {
      if (config.url === '/api/campus/schools/1') return fail(config, 403, '学校已停用')
      if (config.url.endsWith('/applications/mine')) return ok(config, page([{ id: 9, userId: 1, realName: 'Own historical applicant', studentNumber: 'S1', department: 'Faculty', enrollmentYear: 2026, statement: '', status: withdrawn ? 'CANCELLED' : 'PENDING' }]))
      if (config.method === 'delete') { withdrawn = true; return ok(config, { success: true }) }
    }
    await render(CampusPage); await flushPromises()
    expect(wrapper.text()).toContain('Own historical applicant')
    expect(button('提交认证申请')).toBeUndefined()
    await button('撤回申请').trigger('click'); await flushPromises()
    expect(withdrawn).toBe(true)
    expect(wrapper.text()).toContain('已撤回')
    expect(requests.some(item => item.url.endsWith('/posts'))).toBe(false)
  })

  it('invalidates a delayed private feed when refreshing school permission returns403', async () => {
    const pending = deferred(); let deny = false
    handle = config => {
      if (config.url === '/api/campus/schools/1' && deny) return fail(config, 403, '校园权限已撤销')
      if (config.url.endsWith('/posts')) return pending.promise.then(() => ok(config, page([post])))
    }
    await render(CampusPage)
    deny = true; await button('刷新校园状态').trigger('click'); await flushPromises()
    pending.resolve(); await flushPromises()
    expect(wrapper.find('a[href="/campus/posts/8"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain(post.content)
    expect(wrapper.find('a[href="/campus/schools/1/new"]').exists()).toBe(false)
  })

  it('clears an already loaded post and its images on a current forbidden interaction', async () => {
    state.route.params = { postId: '8' }; state.route.fullPath = '/campus/posts/8'
    handle = config => config.method === 'put' ? fail(config, 403, '成员权限已撤销') : undefined
    await render(CampusPostPage)
    expect(wrapper.text()).toContain(post.content)
    await button('点赞 · 2').trigger('click'); await flushPromises()
    expect(wrapper.text()).not.toContain(post.content)
    expect(wrapper.find('textarea').exists()).toBe(false)
  })
  function management() { school.capabilities = { canRead: true, canPost: true, canModerate: true, canManageMembers: true, canManageSchool: true, isSiteAdmin: false }; school.membership.role = 'ADMIN'; state.route.fullPath = '/campus/schools/1/admin' }
  it('creates a school using current ADMIN identity without accepting a supplied owner', async () => {
    state.auth.currentUser.type = 'ADMIN'; state.route.params = {}; state.route.fullPath = '/campus/admin'
    handle = (config, data) => config.method === 'post' ? ok(config, { ...school, ...data }) : undefined
    await render(CampusAdminPage); await wrapper.get('input[maxlength="120"]').setValue('新学校'); await wrapper.get('textarea').setValue('简介'); await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'post').body).toEqual({ name: '新学校', description: '简介', preModeration: true })
    expect(push).toHaveBeenCalledWith('/campus/schools/1/admin')
  })
  it('reviews another applicant with a version and keeps own applications read-only', async () => {
    management(); let applicationUser = 1
    handle = config => config.url.endsWith('/applications') ? ok(config, page([{ id: 9, userId: applicationUser, username: '申请人', realName: '隐私姓名', studentNumber: 'S001', department: '学院', enrollmentYear: 2025, statement: '说明', status: 'PENDING', version: 2 }])) : config.method === 'put' ? ok(config, { id: 9, status: 'APPROVED' }) : undefined
    await render(CampusAdminPage); await button('认证审核').trigger('click'); await flushPromises(); await button('查看并审核申请').trigger('click'); await flushPromises()
    expect(bodyView().find('[role=dialog]').text()).toContain('不能审核自己的申请')
    expect(button('确认处理', bodyView())).toBeUndefined()
    await bodyView().find('[role=dialog]').trigger('keydown', { key: 'Escape' }); await flushPromises()
    applicationUser = 2; await button('刷新列表').trigger('click'); await flushPromises(); await button('查看并审核申请').trigger('click'); await flushPromises()
    await bodyView().find('[role=dialog] form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'put')).toMatchObject({ url: '/api/campus/schools/1/applications/9/review', body: { decision: 'APPROVE', reason: '', version: 2 } })
  })
  it('prevents a campus admin from managing self/peer admins and sends member suspension', async () => {
    management()
    handle = config => config.url.endsWith('/members') ? ok(config, page([{ userId: 1, username: '本人', role: 'ADMIN', status: 'VERIFIED', version: 1 }, { userId: 2, username: '另一个管理员', role: 'ADMIN', status: 'VERIFIED', version: 1 }, { userId: 3, username: '普通成员', role: 'MEMBER', status: 'VERIFIED', version: 4 }])) : config.method === 'put' ? ok(config, { userId: 3, status: 'SUSPENDED' }) : undefined
    await render(CampusAdminPage); await button('成员管理').trigger('click'); await flushPromises()
    expect(wrapper.findAll('button').filter(item => item.text() === '管理成员')).toHaveLength(1)
    await button('管理成员').trigger('click'); await flushPromises(); const modal = bodyView().find('[role=dialog]')
    await modal.findAll('select')[0].setValue('SUSPENDED'); expect(modal.findAll('select')[1].attributes('disabled')).toBeDefined()
    await modal.get('textarea').setValue('违规处理'); await modal.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'put')).toMatchObject({ url: '/api/campus/schools/1/members/3', body: { status: 'SUSPENDED', role: 'MEMBER', reason: '违规处理', version: 4 } })
  })
  it('reviews another author version, pins published posts and loads audit records', async () => {
    management(); post.author.id = 2; post.status = 'PENDING'; post.canModerate = true
    handle = (config, data) => {
      if (config.url.endsWith('/moderation/posts')) return ok(config, page([post]))
      if (config.url.endsWith('/review')) { post.status = 'PUBLISHED'; post.version++; return ok(config, post) }
      if (config.url.endsWith('/pin')) { post.pinned = data.pinned; post.version++; return ok(config, post) }
      if (config.url.endsWith('/audit')) return ok(config, page([{ id: 1, actorId: 1, action: 'POST_APPROVE', targetType: 'POST', targetId: 8, details: '审核通过' }]))
    }
    await render(CampusAdminPage); await button('内容审核').trigger('click'); await flushPromises(); await button('审核当前版本').trigger('click'); await flushPromises()
    await bodyView().get('[role=dialog] form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.url.endsWith('/review')).body).toEqual({ decision: 'APPROVE', reason: '', version: 5 })
    await button('置顶动态').trigger('click'); await flushPromises()
    expect(requests.find(item => item.url.endsWith('/pin')).body).toEqual({ pinned: true, version: 6 })
    await button('操作审计').trigger('click'); await flushPromises(); expect(wrapper.text()).toContain('POST_APPROVE')
  })
  it('allows a site admin to assign a campus role using persisted member version', async () => {
    management(); school.capabilities.isSiteAdmin = true; state.auth.currentUser.type = 'ADMIN'
    handle = config => config.url.endsWith('/members') ? ok(config, page([{ userId: 3, username: '普通成员', role: 'MEMBER', status: 'VERIFIED', version: 4 }])) : config.method === 'put' ? ok(config, { userId: 3, role: 'ADMIN' }) : undefined
    await render(CampusAdminPage); await button('成员管理').trigger('click'); await flushPromises(); await button('管理成员').trigger('click'); await flushPromises()
    const modal = bodyView().get('[role=dialog]'); await modal.findAll('select')[1].setValue('ADMIN'); await modal.get('textarea').setValue('安排校园管理员'); await modal.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'put').body).toEqual({ role: 'ADMIN', status: 'VERIFIED', reason: '安排校园管理员', version: 4 })
  })
  it('can dismiss a pending report after its post has already been removed', async () => {
    management()
    handle = config => {
      if (config.url.endsWith('/reports')) return ok(config, page([{ id: 6, postId: 8, reporterId: 2, reason: '举报原因', status: 'PENDING', version: 3 }]))
      if (config.url === '/api/campus/posts/8') return fail(config, 404, '动态不存在')
      if (config.method === 'put') return ok(config, { id: 6, status: 'DISMISSED' })
    }
    await render(CampusAdminPage); await button('举报处理').trigger('click'); await flushPromises(); await button('处理举报').trigger('click'); await flushPromises()
    const modal = bodyView().get('[role=dialog]'); expect(modal.text()).toContain('动态已不可读')
    expect(modal.get('option[value="REMOVE"]').attributes('disabled')).toBeDefined()
    await modal.get('textarea').setValue('已由其他管理员处理'); await modal.get('form').trigger('submit'); await flushPromises()
    expect(requests.find(item => item.method === 'put')).toMatchObject({ url: '/api/campus/schools/1/reports/6', body: { decision: 'DISMISS', reason: '已由其他管理员处理', version: 3 } })
  })
  it('ignores late school A list data after navigating to school B', async () => {
    const pending = deferred(); handle = config => config.url.endsWith('/posts') ? pending.promise.then(() => ok(config, page([post]))) : undefined
    await render(CampusPage); state.route.fullPath = '/campus/schools/2'; state.route.params.schoolId = '2'
    pending.resolve(); await flushPromises(); expect(wrapper.find('a[href="/campus/posts/8"]').exists()).toBe(false)
  })
})
