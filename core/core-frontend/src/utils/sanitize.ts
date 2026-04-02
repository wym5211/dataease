import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'b', 'i', 'em', 'strong', 'a', 'p', 'br', 'span', 'div',
  'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'table', 'thead', 'tbody', 'tr', 'th', 'td', 'img', 'font'
]

const ALLOWED_ATTR = [
  'href', 'target', 'style', 'class', 'color', 'bgcolor',
  'align', 'valign', 'width', 'height', 'src', 'alt', 'face', 'size'
]

export function sanitizeHtml(dirty: string): string {
  if (!dirty) return dirty
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false
  })
}
