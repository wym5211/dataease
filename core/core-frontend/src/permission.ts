import router from './router'
import { isDatasetFormRedirect } from '@/utils/datasetRedirect'
import { useUserStoreWithOut } from '@/store/modules/user'
import { useAppStoreWithOut } from '@/store/modules/app'
import type { RouteRecordRaw } from 'vue-router'
import { getDefaultSettings } from '@/api/common'
import { useNProgress } from '@/hooks/web/useNProgress'
import { usePermissionStoreWithOut, pathValid, getFirstAuthMenu } from '@/store/modules/permission'
import { usePageLoading } from '@/hooks/web/usePageLoading'
import { getRoleRouters } from '@/api/common'
import { useCache } from '@/hooks/web/useCache'
import { checkPlatform } from '@/utils/utils'
import { interactiveStoreWithOut } from '@/store/modules/interactive'
import { useAppearanceStoreWithOut } from '@/store/modules/appearance'
import { useEmbedded } from '@/store/modules/embedded'
import { useLoading } from '@/hooks/web/useLoading'
import { whiteList, embeddedWindowWhiteList, embeddedRouteWhiteList } from '@/router/whitelist'
import {
  checkMobileRedirect,
  buildRedirectQuery,
  isEmbeddedAccess,
  isPublicAccess
} from '@/router/guards'

const appearanceStore = useAppearanceStoreWithOut()
const { wsCache } = useCache()
const permissionStore = usePermissionStoreWithOut()
const interactiveStore = interactiveStoreWithOut()
const userStore = useUserStoreWithOut()
const appStore = useAppStoreWithOut()

const { start, done } = useNProgress()
const { open } = useLoading()
const { loadStart, loadDone } = usePageLoading()

router.beforeEach(async (to, from, next) => {
  // 特殊路径加载动画
  if (['/chart-view'].includes(to.path) || to.path.startsWith('/de-link/')) {
    open()
  }
  start()
  loadStart()

  const platform = checkPlatform()
  // 始终重新获取后端模式，避免缓存与后端实际模式不一致导致的问题
  await appStore.setAppModel()
  const isDesktop = !!appStore.getDesktop

  // 移动端重定向检查
  if (checkMobileRedirect(to, isDesktop)) {
    done()
    loadDone()
    return
  }
  // 设置外观和默认配置（并行执行）
  const [, , defaultSort] = await Promise.all([
    appearanceStore.setAppearance(),
    appearanceStore.setFontList(),
    getDefaultSettings()
  ])
  wsCache.set('TreeSort-backend', defaultSort['basic.defaultSort'] ?? '1')
  wsCache.set('open-backend', defaultSort['basic.defaultOpen'] ?? '0')

  // 已登录用户的路由处理
  if ((wsCache.get('user.token') || isDesktop) && !to.path.startsWith('/de-link/')) {
    await handleAuthenticatedRoute(to, from, next, isDesktop)
    return
  }

  // 未登录用户的路由处理
  await handleUnauthenticatedRoute(to, next, platform)
})

/**
 * 处理已认证用户的路由
 */
async function handleAuthenticatedRoute(to, from, next, isDesktop: boolean) {
  // 确保用户信息已加载
  if (!userStore.getUid) {
    await userStore.setUser()
  }

  // 登录页重定向到工作台
  if (to.path === '/login') {
    await initializeRouters(isDesktop)
    next({ path: '/workbranch' })
    return
  }

  permissionStore.setCurrentPath(to.path)

  // 数据集编辑表单在新窗口打开，重定向到无 Layout 包装的路由
  if (isDatasetFormRedirect(to, next)) return

  // 路由已初始化
  if (permissionStore.getIsAddRouters) {
    const queryStr = buildRedirectQuery(to, from)
    if (queryStr) {
      to.fullPath += '?' + queryStr
      to.query = queryStr.split('&').reduce((pre, itx) => {
        const [key, val] = itx.split('=')
        pre[key] = val
        return pre
      }, {})
    }

    // 检查路径权限
    if (!pathValid(to.path) && to.path !== '/404' && !to.path.startsWith('/de-link')) {
      const firstPath = getFirstAuthMenu()
      next({ path: firstPath || '/404' })
      return
    }
    next()
    return
  }

  // 初始化路由
  await initializeRouters(isDesktop)

  // 检查路径权限
  if (!pathValid(to.path) && to.path !== '/404' && !to.path.startsWith('/de-link')) {
    const firstPath = getFirstAuthMenu()
    next({ path: firstPath || '/404' })
    return
  }

  // 从登录页跳转时，跳转到第一个有权限的页面
  if (from.path === '/login') {
    const firstPath = getFirstAuthMenu()
    next({ path: firstPath || '/workbranch' })
    return
  }

  next({ ...to, replace: true })
}

/**
 * 处理未认证用户的路由
 */
async function handleUnauthenticatedRoute(to, next, platform) {
  const embeddedStore = useEmbedded()

  // 嵌入式访问检查
  if (isEmbeddedAccess(to, embeddedStore.getToken, appStore.getIsIframe, embeddedRouteWhiteList)) {
    if (isDatasetFormRedirect(to, next)) return
    permissionStore.setCurrentPath(to.path)
    next()
    return
  }

  // 公开访问路径检查
  if (isPublicAccess(to, platform, embeddedWindowWhiteList, whiteList)) {
    await appearanceStore.setFontList()
    permissionStore.setCurrentPath(to.path)
    next()
    return
  }

  // 重定向到登录页
  next(`/login?redirect=${to.fullPath || to.path}`)
}

/**
 * 初始化动态路由
 */
async function initializeRouters(isDesktop: boolean) {
  if (permissionStore.getIsAddRouters) {
    return
  }

  let roleRouters = (await getRoleRouters()) || []
  if (isDesktop) {
    roleRouters = roleRouters.filter(item => item.name !== 'system')
  }

  const routers: AppCustomRouteRecordRaw[] = roleRouters as AppCustomRouteRecordRaw[]
  routers.forEach(item => (item['top'] = true))
  await permissionStore.generateRoutes(routers as AppCustomRouteRecordRaw[])

  permissionStore.getAddRouters.forEach(route => {
    router.addRoute(route as unknown as RouteRecordRaw)
  })

  permissionStore.setIsAddRouters(true)
  await interactiveStore.initInteractive(true)
}

router.afterEach(() => {
  done()
  loadDone()
})
