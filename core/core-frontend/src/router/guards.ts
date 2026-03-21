/**
 * 路由守卫辅助函数
 * 将复杂的权限检查逻辑拆分为独立的函数
 */

import { isMobile, isLarkPlatform, isPlatformClient } from '@/utils/utils'
import { useCache } from '@/hooks/web/useCache'
import type { RouteLocationNormalized } from 'vue-router'

const { wsCache } = useCache()

/**
 * 检查是否需要移动端重定向
 * @returns true 表示需要重定向（已处理），false 表示继续正常流程
 */
export const checkMobileRedirect = (to: RouteLocationNormalized, isDesktop: boolean): boolean => {
  if (!isMobile() || ['/chart-view'].includes(to.path)) {
    return false
  }

  // 处理 link 路由
  if (to.name === 'link') {
    let linkQuery = ''
    if (Object.keys(to.query).length > 0) {
      const tempQuery = Object.keys(to.query)
        .map(key => key + '=' + to.query[key])
        .join('&')
      if (tempQuery) {
        linkQuery = '?' + tempQuery
      }
    }
    let pathname = window.location.pathname
    pathname = pathname.replace('casbi/', '')
    pathname = pathname.replace('oidc/', '')
    pathname = pathname.substring(0, pathname.length - 1)
    const prefix = window.origin + pathname
    let toPath = to.fullPath
    if (toPath.includes('?')) {
      toPath = to.fullPath.substring(0, to.fullPath.lastIndexOf('?'))
    }
    window.location.href = (prefix + '/mobile.html#' + toPath + linkQuery).replace(/\+/g, '%2B')
    return true
  }

  // 处理其他移动端路由
  if (wsCache.get('user.token') || isDesktop || (!isPlatformClient() && !isLarkPlatform())) {
    let pathname = window.location.pathname
    pathname = pathname.substring(0, pathname.length - 1)
    let url = window.origin + pathname + '/mobile.html#/index'
    if (location.hash?.startsWith('#/preview')) {
      url = window.origin + pathname + '/mobile.html' + location.hash
    }
    if (window.location.search) {
      url += window.location.search
    }
    window.location.href = url
    return true
  }

  return false
}

/**
 * 构建重定向查询参数
 */
export const buildRedirectQuery = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized
): string => {
  let str = ''
  if (((from.query.redirect as string) || '?').split('?')[0] === to.path) {
    str = ((window.location.hash as string) || '?').split('?').reverse()[0]
    if (str.includes('redirect=')) {
      str = ''
    }
  }
  return str
}

/**
 * 检查是否为嵌入式路由访问
 */
export const isEmbeddedAccess = (
  to: RouteLocationNormalized,
  embeddedToken: string,
  isIframe: boolean,
  embeddedRouteWhiteList: string[]
): boolean => {
  return embeddedToken && isIframe && embeddedRouteWhiteList.includes(to.path)
}

/**
 * 检查是否为公开访问路径
 */
export const isPublicAccess = (
  to: RouteLocationNormalized,
  platform: boolean,
  embeddedWindowWhiteList: string[],
  whiteList: string[]
): boolean => {
  return (
    (!platform && embeddedWindowWhiteList.includes(to.path)) ||
    whiteList.includes(to.path) ||
    to.path.startsWith('/de-link/')
  )
}
