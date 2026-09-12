import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { mount, shallowMount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import ArticleRead from '@/views/article/ArticleRead.vue'
import ArticleWrite from '@/views/article/ArticleWrite.vue'
import ArticleEdit from '@/views/article/ArticleEdit.vue'
import { useAuthStore } from '@/stores/authStore'
import * as articleApi from '@/api/modules/article'
import { ElMessage } from 'element-plus'
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { articleId: '12' } }), useRouter: () => ({ push: vi.fn(), go: vi.fn(), back: vi.fn() }) }))
vi.mock('@/api/modules/article', () => ({
  default: {},
  getArticle: vi.fn(async () => ({ code: 0, data: { id: 12, articleTitle: 'Title', articleContent: '**safe**' } })),
  getArticleById: vi.fn(async () => ({ code: 0, data: { id: 12, articleTitle: 'Title', articleContent: '**safe**', likesCount: 2 } })),
  increaseReadCount: vi.fn(async () => ({ code: 0 })), likeArticle: vi.fn(async () => ({ code: 0 })),
  getArticleComments: vi.fn(async () => ({ code: 0, data: [{ id: 7, content: 'comment' }] })),
  addArticleComment: vi.fn(async () => ({ code: 0 })), deleteArticleComment: vi.fn(async () => ({ code: 0 })),
  checkFavoriteStatus: vi.fn(async () => ({ code: 0, data: false })), favoriteArticle: vi.fn(async () => ({ code: 0 })),
  unfavoriteArticle: vi.fn(async () => ({ code: 0 })), createArticle: vi.fn(), updateArticle: vi.fn()
}))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() }, ElMessageBox: { confirm: vi.fn(async () => true) } }))
const wrappers = []
beforeEach(() => { localStorage.clear(); setActivePinia(createPinia()); useAuthStore().currentUser = { id: 1 }; vi.clearAllMocks() })
afterEach(() => { wrappers.forEach(w => w.unmount()); wrappers.length = 0 })
const render = Component => {
  const wrapper = shallowMount(Component, { global: { stubs: { 'el-dialog': { template: '<div><slot /></div>' } } } })
  wrappers.push(wrapper); return wrapper
}
describe('article regressions', () => {
  it.each([ArticleWrite, ArticleEdit])('sanitizes the live preview before inserting it into the DOM', async Component => {
    const wrapper = render(Component); await flushPromises()
    const state = wrapper.vm.$.setupState
    state.form.articleContent = '**safe** <img src=x onerror="alert(1)"><script>alert(1)</script>'
    state.previewVisible = true
    await nextTick()
    const preview = wrapper.find('.article-body')
    expect(preview.html()).toContain('<strong>safe</strong>')
    expect(preview.find('[onerror], script').exists()).toBe(false)
  })
  it('updates likes, comments, deletion and favorites on code zero', async () => {
    const wrapper = render(ArticleRead); await flushPromises()
    const state = wrapper.vm.$.setupState
    await state.like()
    expect(state.article.likesCount).toBe(3)
    state.newComment = 'hello'
    await state.submitComment()
    expect(articleApi.addArticleComment).toHaveBeenCalledWith('12', { content: 'hello' })
    expect(state.newComment).toBe('')
    await state.confirmDeleteComment({ id: 7 }); await flushPromises()
    expect(articleApi.deleteArticleComment).toHaveBeenCalledWith('12', 7)
    expect(articleApi.getArticleComments.mock.calls.length).toBeGreaterThanOrEqual(3)
    await state.toggleFavorite(); expect(state.isFavorited).toBe(true)
    await state.toggleFavorite(); expect(state.isFavorited).toBe(false)
    expect(ElMessage.error).not.toHaveBeenCalled()
  })
  it('keeps the unsent comment when the request fails', async () => {
    const wrapper = render(ArticleRead); await flushPromises()
    articleApi.addArticleComment.mockRejectedValueOnce(new Error('offline'))
    const state = wrapper.vm.$.setupState; state.newComment = 'keep me'
    await state.submitComment()
    expect(state.newComment).toBe('keep me')
    expect(state.isSubmittingComment).toBe(false)
    expect(ElMessage.error).toHaveBeenCalled()
  })
})
