import { describe, it, expect } from 'vitest'
import { renderMarkdown, sanitizeHtml } from './safeHtml'

describe('untrusted rich content', () => {
  it('keeps Markdown formatting while removing executable HTML and unsafe URLs', () => {
    const html = renderMarkdown('**safe** <img src=x onerror="alert(1)"><script>alert(2)</script> [bad](javascript:alert%281%29)')
    const node = document.createElement('div')
    node.innerHTML = html
    expect(node.querySelector('strong').textContent).toBe('safe')
    expect(node.querySelector('script, [onerror], a[href^="javascript:"]')).toBeNull()
  })
  it('removes SVG and embedded document payloads', () => {
    expect(sanitizeHtml('<svg onload="alert(1)"></svg><iframe srcdoc="<script>alert(1)</script>"></iframe><p>safe</p>')).toBe('<p>safe</p>')
  })
})
