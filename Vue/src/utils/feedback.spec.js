import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import appleMessage from './appleMessage'
import appleConfirm from './appleConfirm'
afterEach(() => { vi.useRealTimers(); document.body.innerHTML = '' })
describe('safe feedback', () => {
  it('renders toast text without interpreting HTML', () => {
    vi.useFakeTimers()
    const toast = appleMessage.error('<img src=x onerror="alert(1)">')
    expect(document.querySelector('.apple-message').textContent).toContain('<img')
    expect(document.querySelector('.apple-message img')).toBeNull()
    toast.close()
  })
  it('escapes confirmation titles/content and resolves cancellation', async () => {
    const result = appleConfirm.confirm('<img src=x>', '<script>alert(1)</script>')
    await nextTick(); await nextTick()
    const dialog = document.querySelector('[role="dialog"]')
    expect(dialog.querySelector('img,script')).toBeNull()
    expect(dialog.textContent).toContain('<script>alert(1)</script>')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await expect(result).resolves.toBe(false)
    expect(document.querySelector('[role="dialog"]')).toBeNull()
  })
})
