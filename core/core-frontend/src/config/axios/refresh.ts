import { useCache } from '@/hooks/web/useCache'
import { refreshApi } from '@/api/login'
import { useUserStoreWithOut } from '@/store/modules/user'
import { useRequestStoreWithOut } from '@/store/modules/request'

import { isLink } from '@/utils/utils'
const { wsCache } = useCache()
const userStore = useUserStoreWithOut()
const requestStore = useRequestStoreWithOut()
const refreshUrl = '/login/refresh'

const expConstants = 10000

const expTimeConstants = 90000

const isExpired = () => {
  const exp = wsCache.get('user.exp')
  if (!exp) {
    return false
  }
  const time = wsCache.get('user.time')
  if (!time) {
    return exp - Date.now() < expConstants
  }
  return Date.now() - time > expTimeConstants
}

const delayExecute = (token: string) => {
  const cachedRequestList = requestStore.getRequestList
  cachedRequestList.forEach(cb => {
    cb(token)
  })
  requestStore.cleanCacheRequest()
}

const getRefreshStatus = () => {
  return wsCache.get('de-global-refresh') || false
}
const setRefreshStatus = (status: boolean) => {
  wsCache.set('de-global-refresh', status, { exp: 5 })
}

const cacheRequest = cb => {
  requestStore.addCacheRequest(cb)
}

export const configHandler = config => {
  const desktop = wsCache.get('app.desktop')
  if (desktop) {
    return config
  }
  if (isLink()) {
    return config
  }
  // 直接从 localStorage 获取原始值
  const rawToken = localStorage.getItem('user.token')
  console.log('[DEBUG] Raw from localStorage:', rawToken?.substring(0, 100))
  let token = null
  if (rawToken) {
    try {
      const parsed = JSON.parse(rawToken)
      console.log('[DEBUG] Parsed type:', typeof parsed, JSON.stringify(parsed)?.substring(0, 100))
      // WebStorageCache 格式: {c: createTime, e: expireTime, v: value}
      if (parsed && parsed.v) {
        let innerToken = parsed.v
        // 如果值还被引号包裹，去除引号
        if (
          typeof innerToken === 'string' &&
          innerToken.startsWith('"') &&
          innerToken.endsWith('"')
        ) {
          innerToken = innerToken.slice(1, -1)
        }
        token = innerToken
      } else {
        token = rawToken // 如果不是包装格式，直接使用
      }
    } catch (e) {
      token = rawToken // 解析失败，直接使用原始值
    }
  }
  console.log('[DEBUG] Final token:', token ? token.substring(0, 50) + '...' : 'null')
  if (token) {
    config.headers['X-DE-TOKEN'] = token
    const expired = isExpired()
    if (expired && !config.url.includes(refreshUrl)) {
      if (!getRefreshStatus()) {
        setRefreshStatus(true)
        refreshApi(Date.now())
          .then(res => {
            if (res?.data?.token) {
              userStore.setToken(res.data.token)
              userStore.setExp(res.data.exp)
              userStore.setTime(Date.now())
              config.headers['X-DE-TOKEN'] = res.data.token
              delayExecute(res.data.token)
            } else {
              delayExecute(null)
            }
          })
          .catch(e => {
            console.error(e)
          })
          .finally(() => {
            setRefreshStatus(false)
          })
      }
      const retry = new Promise(resolve => {
        cacheRequest(token => {
          config.headers['X-DE-TOKEN'] = token
          resolve(config)
        })
      })
      return retry
    } else {
      return config
    }
  }
  return config
}
