import DOMPurify from 'dompurify'
import { marked } from 'marked'

export const sanitizeHtml = (html) => DOMPurify.sanitize(String(html ?? ''), { USE_PROFILES: { html: true } })
export const renderMarkdown = (content) => sanitizeHtml(marked.parse(String(content ?? ''), { async: false }))
