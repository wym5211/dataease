import axios, {
  AxiosInstance,
  AxiosRequestHeaders,
  InternalAxiosRequestConfig,
  AxiosResponse,
  AxiosError,
  AxiosRequestConfig,
  AxiosHeaders
} from 'axios'
import { tryShowLoading, tryHideLoading } from '@/utils/loading'
import qs from 'qs'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useEmbedded } from '@/store/modules/embedded'
import { useLinkStoreWithOut } from '@/store/modules/link'
import { config } from './config'
import { configHandler } from './refresh'
import { isMobile, getLocale } from '@/utils/utils'
import { useRequestStoreWithOut } from '@/store/modules/request'
import { clearCache } from '@/utils/cacheUtil'
import { logger } from '@/utils/logger'

function redirectToLogin() {
  clearCache()
  try {
    localStorage.removeItem('user.refreshToken')
  } catch (e) {
    logger.error('clear refresh token failed', e)
  }
  const redirect = router.currentRoute.value.fullPath || '/workbranch'
  router.push(`/login?redirect=${redirect}`)
}

type AxiosErrorWidthLoading<T> = T & {
  config: {
    loading?: boolean
    silentError?: boolean
    _retry?: boolean
  }
}

type InternalAxiosRequestConfigWidthLoading<T> = T & {
  loading?: boolean
  silentError?: boolean
  _retry?: boolean
}

import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import router from '@/router'

const { result_code } = config
import { useCache } from '@/hooks/web/useCache'
const { wsCache } = useCache()
const requestStore = useRequestStoreWithOut()
const embeddedStore = useEmbedded()
const basePath = import.meta.env.VITE_API_BASEPATH

const embeddedBasePath =
  basePath.startsWith('./') && basePath.length > 2 ? basePath.substring(2) : basePath
export const PATH_URL = embeddedStore.baseUrl ? embeddedStore?.baseUrl + embeddedBasePath : basePath

export interface AxiosInstanceWithLoading extends AxiosInstance {
  <T = unknown, R = AxiosResponse<T>, D = unknown>(
    config: AxiosRequestConfig<D> & { loading?: boolean; silentError?: boolean }
  ): Promise<R>
}

const DEFAULT_TIMEOUT = 100 // 默认超时时间（秒）

/**
 * 异步获取请求超时配置
 * 使用 axios 替代同步 XMLHttpRequest
 */
const getTimeOut = async (): Promise<number> => {
  try {
    const url = PATH_URL + '/sysParameter/requestTimeOut'
    const response = await axios.get(url, { timeout: 5000 })

    if (response.data && response.data.code === 0) {
      return response.data.data || DEFAULT_TIMEOUT
    } else {
      logger.error('获取超时配置失败，使用默认值')
      return DEFAULT_TIMEOUT
    }
  } catch (e) {
    logger.error('获取超时配置异常，使用默认值:', e)
    return DEFAULT_TIMEOUT
  }
}

// 创建axios实例（使用默认超时，稍后更新）
let requestTimeout = DEFAULT_TIMEOUT * 1000
const service: AxiosInstanceWithLoading = axios.create({
  baseURL: PATH_URL,
  timeout: requestTimeout
})

// 异步初始化超时配置
getTimeOut()
  .then(time => {
    requestTimeout = time * 1000
    window._de_get_time_out = time
    // 更新已创建的 axios 实例的超时配置
    service.defaults.timeout = requestTimeout
    logger.info('请求超时配置已更新:', time + 's')
  })
  .catch(err => {
    logger.error('初始化超时配置失败:', err)
  })
const mapping: Record<string, string> = {
  'zh-CN': 'zh-CN',
  en: 'en-US',
  tw: 'zh-TW'
}
const permissionStore = usePermissionStoreWithOut()
const linkStore = useLinkStoreWithOut()
const CancelToken = axios.CancelToken
const cancelMap: Record<string, (message?: string) => void> = {}

/**
 * 使用 refresh token 换取新 access token，并重试原请求
 * 使用原生 axios 避免拦截器递归
 */
let refreshingPromise: Promise<string | null> | null = null
const refreshTokenAndRetry = async (
  originalConfig: InternalAxiosRequestConfig,
  refreshToken: string
): Promise<unknown> => {
  try {
    if (!refreshingPromise) {
      refreshingPromise = axios
        .post(
          PATH_URL + '/login/refreshAccess',
          qs.stringify({ refreshToken }),
          {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            timeout: 10000
          }
        )
        .then(resp => {
          const data = resp.data as Record<string, unknown>
          const inner = data?.data as Record<string, unknown> | undefined
          const tokenCandidate = inner?.token ?? data?.token
          const newToken = tokenCandidate ? String(tokenCandidate) : null
          const rtCandidate = (inner?.refreshToken as string) ?? (data?.refreshToken as string) ?? (resp.headers['x-refresh-token'] as string)
          if (newToken) {
            wsCache.set('user.token', newToken)
            if (rtCandidate) {
              localStorage.setItem('user.refreshToken', rtCandidate)
            }
            return newToken
          }
          return null
        })
        .catch(err => {
          logger.error('refresh access token failed', err)
          return null
        })
        .finally(() => {
          refreshingPromise = null
        })
    }
    const newToken = await refreshingPromise
    if (!newToken) {
      redirectToLogin()
      return Promise.reject(new Error('refresh token failed'))
    }
    if (!originalConfig.headers) {
      originalConfig.headers = new AxiosHeaders()
    }
    ;(originalConfig.headers as AxiosRequestHeaders)['X-DE-TOKEN'] = newToken
    return service(originalConfig)
  } catch (e) {
    logger.error('refreshTokenAndRetry exception', e)
    return Promise.reject(e)
  }
}

// request拦截器
service.interceptors.request.use(
  async (c: InternalAxiosRequestConfigWidthLoading<InternalAxiosRequestConfig>) => {
    logger.debug('Request URL:', c.url)
    let config = configHandler(c)
    if (config instanceof Promise) {
      config = await config
    }
    logger.debug(
      'After configHandler, X-DE-TOKEN:',
      (config.headers as Record<string, unknown>)['X-DE-TOKEN'] ? 'exists' : 'missing'
    )
    if (
      config.method === 'post' &&
      (config.headers as AxiosRequestHeaders)['Content-Type'] ===
        'application/x-www-form-urlencoded'
    ) {
      config.data = qs.stringify(config.data)
    }
    if (embeddedStore.baseUrl) {
      config.baseURL = PATH_URL
    }

    if (isMobile()) {
      ;(config.headers as AxiosRequestHeaders)['X-DE-MOBILE'] = true
    }
    if (linkStore.getLinkToken) {
      ;(config.headers as AxiosRequestHeaders)['X-DE-LINK-TOKEN'] = linkStore.getLinkToken
    } else if (embeddedStore.token) {
      ;(config.headers as AxiosRequestHeaders)['X-EMBEDDED-TOKEN'] = embeddedStore.token
    }
    const locale = getLocale()
    if (locale) {
      const val = mapping[locale] || locale
      ;(config.headers as AxiosRequestHeaders)['Accept-Language'] = val
    }

    if (config.method === 'get' && config.params) {
      let url = config.url as string
      url += '?'
      const keys = Object.keys(config.params)
      for (const key of keys) {
        if (config.params[key] !== void 0 && config.params[key] !== null) {
          url += `${key}=${encodeURIComponent(config.params[key])}&`
        }
      }
      url = url.substring(0, url.length - 1)
      config.params = {}
      config.url = url
    }

    if (config.url.endsWith('chartData/getData')) {
      const chartKey = `chartData/getData/${(config.data as { id: string | number }).id}`
      config.cancelToken = new CancelToken(function executor(c) {
        cancelMap[chartKey] = c
      })
    } else {
      config.cancelToken = new CancelToken(function executor(c) {
        cancelMap[config.url] = c
      })
    }

    config.loading && tryShowLoading(permissionStore.getCurrentPath)
    return config
  },
  (error: AxiosErrorWidthLoading<AxiosError>) => {
    error.config.loading && tryHideLoading(permissionStore.getCurrentPath)
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  (
    response: AxiosResponse<unknown> & {
      config: InternalAxiosRequestConfig & { loading?: boolean }
    }
  ) => {
    executeVersionHandler(response)
    // 捕获后端下发的 Refresh Token，写入 localStorage
    const refreshTokenHeader =
      response.headers['x-refresh-token'] || response.headers['X-Refresh-Token']
    if (refreshTokenHeader) {
      try {
        localStorage.setItem('user.refreshToken', String(refreshTokenHeader))
      } catch (e) {
        logger.error('store refresh token failed', e)
      }
    }
    if (response.headers['x-de-link-token']) {
      linkStore.setLinkToken(response.headers['x-de-link-token'])
    }
    response.config.loading && tryHideLoading(permissionStore.getCurrentPath)

    const responseData = response.data as Record<string, unknown>
    if (response.config.responseType === 'blob') {
      // 如果是文件流，直接过
      return response
    } else if (responseData.code === result_code || responseData.code === 50002) {
      return responseData
    } else if (
      responseData &&
      !responseData.code &&
      (responseData.status === 'success' ||
        responseData.status === 'failed' ||
        responseData.status === 'processing' ||
        responseData.version)
    ) {
      // 兼容后端直接返回的数据对象（如 BackupResponse、ExportPackage）
      // 如果没有 code 字段但有特定的 status 或 version 字段，认为请求成功
      return responseData
    } else if (response.config.url.match(/^\/map|geo\/\d{3}\/\d+\.json$/)) {
      //   TODO 处理静态文件
      return response
    } else if (
      response.config.url.includes('DEXPack.umd.js') ||
      response.config.url.includes('/i18n/custom_')
    ) {
      return response
    } else if (response.config.url.startsWith('/xpackComponent/pluginStaticInfo/extensions-')) {
      return response
    } else {
      if (
        !response?.config?.url.startsWith('/xpackComponent/content') &&
        responseData?.code !== 60003
      ) {
        ElMessage({
          type: 'error',
          message: responseData.msg as string,
          showClose: true
        })
        if (responseData.code === 80001) {
          redirectToLogin()
        }
      } else if (response?.config?.url.startsWith('/xpackComponent/content')) {
        console.error(
          "never mind this error about '/xpackComponent/content', just a reminder to support the official license"
        )
      }

      return Promise.reject(responseData.msg as string)
    }
  },
  (error: AxiosErrorWidthLoading<AxiosError>) => {
    if (error.message?.includes('timeout of')) {
      requestStore.resetLoadingMap()
      ElMessage({
        type: 'error',
        message: '请求超时，请稍后再试',
        showClose: true
      })
    }

    if (!error?.response) {
      return Promise.reject(error)
    }

    if (error?.response.status === 413) {
      ElMessage({
        type: 'error',
        message: '文件大小超出限制, 请修改相关配置文件',
        showClose: true
      })
      return
    }
    const header = error.response?.headers as AxiosHeaders
    if (
      !error.config.url.startsWith('/xpackComponent/content') &&
      !header.has('DE-FORBIDDEN-FLAG') &&
      !header.has('DE-GATEWAY-FLAG') &&
      !error.config.silentError
    ) {
      const errorResponseData = error.response?.data as Record<string, unknown>
      ElMessage({
        type: 'error',
        message: errorResponseData?.msg ? (errorResponseData.msg as string) : error.message,
        showClose: true
      })
    } else if (error?.config?.url.startsWith('/xpackComponent/content')) {
      console.error(
        "never mind this error about '/xpackComponent/content', just a reminder to support the official license"
      )
    }

    error.config.loading && tryHideLoading(permissionStore.getCurrentPath)
    // 401 处理：尝试使用 refresh token 换取新 access token 并重试原请求
    if (error?.response?.status === 401) {
      const originalConfig = error.config as InternalAxiosRequestConfig & {
        _retry?: boolean
        loading?: boolean
      }
      const reqUrl = originalConfig?.url || ''
      const isRefreshCall =
        reqUrl.includes('/login/refreshAccess') || reqUrl.includes('/login/localLogin')
      if (!originalConfig?._retry && !isRefreshCall) {
        const refreshToken = localStorage.getItem('user.refreshToken')
        if (refreshToken) {
          originalConfig._retry = true
          return refreshTokenAndRetry(originalConfig, refreshToken)
        }
      }
      if (isRefreshCall || !localStorage.getItem('user.refreshToken')) {
        redirectToLogin()
        return Promise.reject(error)
      }
    }
    if (header.has('DE-GATEWAY-FLAG')) {
      const userToken = wsCache.get('user.token')
      const inPlatformClient = !!wsCache.get('de-platform-client')
      clearCache()
      if (!(userToken && inPlatformClient)) {
        const flag = header.get('DE-GATEWAY-FLAG')
        localStorage.setItem('DE-GATEWAY-FLAG', String(flag || ''))
      }
      const redirect = router.currentRoute.value.fullPath || '/workbranch'
      router.push(`/login?redirect=${redirect}`)
    }
    if (header.has('DE-FORBIDDEN-FLAG')) {
      showMsg('当前用户权限配置已变更，请刷新页面', '-changed-')
    }
    if (error?.response.status === 400) {
      return Promise.reject(error)
    }

    return Promise.resolve()
  }
)

const showMsg = (msg: string, id: string) => {
  if (window['cross-permission-' + id]) {
    return
  }
  window['cross-permission-' + id] = ElMessageBox.confirm(msg, {
    confirmButtonType: 'primary',
    type: 'warning',
    confirmButtonText: '刷新',
    cancelButtonText: '取消',
    autofocus: false,
    showClose: false
  })
    .then(() => {
      window.location.reload()
    })
    .catch(() => {
      window['cross-permission-' + id] = null
    })
}

const executeVersionHandler = (response: AxiosResponse) => {
  const key = 'x-de-execute-version'
  const executeVersion = response.headers[key]
  const cacheVal = wsCache.get(key)
  if (!cacheVal) {
    wsCache.set(key, executeVersion)
    return
  }
  if (executeVersion && executeVersion !== cacheVal) {
    wsCache.set(key, executeVersion)
    showMsg('系统有升级，请点击刷新页面', '-sys-upgrade-')
  }
}

const cancelRequestBatch = (cancelKey: string) => {
  if (cancelKey) {
    if (cancelKey.indexOf('/**') > -1) {
      const cancelKeyPre = cancelKey.split('/**')[0]
      Object.keys(cancelMap).forEach(key => {
        if (key.indexOf(cancelKeyPre) > -1) {
          cancelMap[key]?.('Operation canceled by the user, url:' + key)
        }
      })
    } else {
      cancelMap[cancelKey]?.('Operation canceled by the user, url:' + cancelKey)
    }
  }
}
export { service, cancelMap, cancelRequestBatch }
