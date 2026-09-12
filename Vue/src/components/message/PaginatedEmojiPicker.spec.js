import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PaginatedEmojiPicker from './PaginatedEmojiPicker.vue'

describe('emoji pagination carryover', () => {
  it('keeps every formerly available common emoji searchable and selectable without duplicates', async () => {
    const wrapper = mount(PaginatedEmojiPicker)
    const commonEmojis = ['😊', '😂', '❤️', '👍', '👎', '🎉', '😢', '😡', '🤔', '👏', '🙏', '💪', '🔥', '✨', '🎈']

    for (const emoji of commonEmojis) {
      await wrapper.find('input').setValue(emoji)
      const matches = wrapper.findAll('.emoji-grid button').filter(button => button.text() === emoji)
      expect(matches, `Expected one selectable ${emoji}`).toHaveLength(1)
      await matches[0].trigger('click')
      expect(wrapper.emitted('select').at(-1)).toEqual([emoji])
    }

    wrapper.unmount()
  })

  it('paginates, resets the page after searching, and emits selected emoji', async () => {
    const wrapper = mount(PaginatedEmojiPicker)
    expect(wrapper.findAll('.emoji-grid button').length).toBeLessThanOrEqual(30)
    const first = wrapper.find('.emoji-grid button').text()
    await wrapper.findAll('nav button')[1].trigger('click')
    expect(wrapper.find('.emoji-grid button').text()).not.toBe(first)
    await wrapper.find('input').setValue('笑脸')
    expect(wrapper.find('nav span').text()).toMatch(/^1 \/ /)
    expect(wrapper.findAll('.emoji-grid button').length).toBeGreaterThan(0)
    const chosen = wrapper.find('.emoji-grid button').text()
    await wrapper.find('.emoji-grid button').trigger('click')
    expect(wrapper.emitted('select')[0]).toEqual([chosen])
    await wrapper.find('input').setValue('no-match-unique')
    expect(wrapper.text()).toContain('没有找到匹配的表情')
    expect(wrapper.find('nav span').text()).toBe('1 / 1')
    wrapper.unmount()
  })
})
