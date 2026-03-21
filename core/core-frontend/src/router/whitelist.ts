/**
 * 路由白名单配置
 * 这些路径不需要登录即可访问
 */

/**
 * 基础白名单 - 不需要重定向的路径
 */
export const whiteList = ['/login', '/de-link', '/chart-view', '/admin-login', '/401']

/**
 * 嵌入式窗口白名单 - 允许在 iframe 中访问的路径
 */
export const embeddedWindowWhiteList = [
  '/dvCanvas',
  '/dashboard',
  '/preview',
  '/dataset-embedded-form'
]

/**
 * 嵌入式路由白名单 - 使用嵌入式 token 访问的路径
 */
export const embeddedRouteWhiteList = [
  '/dataset-embedded',
  '/dataset-form',
  '/dataset-embedded-form'
]

/**
 * 检查路径是否在白名单中
 */
export const isInWhiteList = (path: string): boolean => {
  return whiteList.includes(path) || path.startsWith('/de-link/')
}

/**
 * 检查路径是否在嵌入式窗口白名单中
 */
export const isInEmbeddedWindowWhiteList = (path: string): boolean => {
  return embeddedWindowWhiteList.includes(path)
}

/**
 * 检查路径是否在嵌入式路由白名单中
 */
export const isInEmbeddedRouteWhiteList = (path: string): boolean => {
  return embeddedRouteWhiteList.includes(path)
}
