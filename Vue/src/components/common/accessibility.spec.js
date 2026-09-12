import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AppleSwitch from './AppleSwitch.vue'
import AppleModal from './AppleModal.vue'

const wrappers = []
const render = (component, options) => {
  const wrapper = mount(component, { attachTo: document.body, ...options })
  wrappers.push(wrapper)
  return wrapper
}
afterEach(() => { wrappers.reverse().forEach(wrapper => wrapper.unmount()); wrappers.length = 0; document.body.innerHTML = '' })

describe('keyboard controls', () => {
  it('exposes switch state and toggles with Space and Enter', async () => {
    const wrapper = render(AppleSwitch, { props: { modelValue: false }, attrs: { 'aria-label': 'Notifications' } })
    expect(wrapper.attributes('role')).toBe('switch')
    await wrapper.trigger('keydown', { key: ' ' })
    expect(wrapper.emitted('update:modelValue')).toEqual([[true]])
    await wrapper.setProps({ modelValue: true })
    await wrapper.trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('update:modelValue')[1]).toEqual([false])
  })
  it.each([{ disabled: true }, { loading: true }])('blocks click and keyboard while unavailable: %j', async props => {
    const wrapper = render(AppleSwitch, { props })
    await wrapper.trigger('click')
    await wrapper.trigger('keydown', { key: ' ' })
    expect(wrapper.attributes('tabindex')).toBe('-1')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })
  it('focuses initial dialog, wraps Tab both ways and restores trigger on close', async () => {
    const trigger = document.createElement('button'); document.body.append(trigger); trigger.focus()
    const wrapper = render(AppleModal, { props: { modelValue: true, title: 'Confirm', showHeader: false }, slots: { default: '<button id="first">First</button><button id="last">Last</button>' } })
    await nextTick(); await nextTick()
    const dialog = document.querySelector('[role="dialog"]')
    expect(document.activeElement).toBe(dialog)
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    document.querySelector('#last').focus()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }))
    expect(document.activeElement.id).toBe('first')
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true }))
    expect(document.activeElement.id).toBe('last')
    await wrapper.setProps({ modelValue: false }); await nextTick()
    expect(document.activeElement).toBe(trigger)
  })
  it('Escape closes only the top nested dialog', async () => {
    const lower = render(AppleModal, { props: { modelValue: true, title: 'Lower' } })
    await nextTick(); await nextTick()
    const upper = render(AppleModal, { props: { modelValue: true, title: 'Upper' } })
    await nextTick(); await nextTick()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }))
    expect(upper.emitted('close')).toHaveLength(1)
    expect(lower.emitted('close')).toBeUndefined()
  })
})
