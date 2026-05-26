import SockJS from 'sockjs-client/dist/sockjs.min.js'
import Stomp from 'stompjs'
import { useCache } from '@/hooks/web/useCache'
import { useEmitt } from '@/hooks/web/useEmitt'
import { logger } from '@/utils/logger'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { getRoleRouters } from '@/api/common'
import { ElNotification } from 'element-plus-secondary'
import router from '@/router'
import type { RouteRecordRaw } from 'vue-router'
const { wsCache } = useCache()
let stompClient: Stomp.Client
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
import dev from '../../config/dev'
const env = import.meta.env
const basePath = env.VITE_API_BASEPATH

// 权限变更防抖定时器
let permissionChangeTimer: ReturnType<typeof setTimeout> | null = null

function handlePermissionChange(data: { type: string; changeType: string }) {
  if (permissionChangeTimer) {
    clearTimeout(permissionChangeTimer)
  }
  permissionChangeTimer = setTimeout(async () => {
    permissionChangeTimer = null
    try {
      const permissionStore = usePermissionStoreWithOut()
      const isDesktop = !!wsCache.get('app.desktop')
      // 重新获取角色路由
      let roleRouters = (await getRoleRouters()) || []
      if (isDesktop) {
        roleRouters = roleRouters.filter(item => item.name !== 'system')
      }
      const routers = roleRouters as AppCustomRouteRecordRaw[]
      routers.forEach(item => (item['top'] = true))
      // 重新生成路由
      await permissionStore.generateRoutes(routers)
      permissionStore.getAddRouters.forEach(route => {
        const r = route as unknown as RouteRecordRaw
        if (r.name && router.hasRoute(r.name)) {
          router.removeRoute(r.name)
        }
        router.addRoute(r)
      })
      // 提示用户
      ElNotification({
        title: '提示',
        message: '您的权限配置已更新，菜单已自动刷新',
        type: 'info',
        duration: 3000
      })
    } catch (e) {
      logger.error('刷新权限失败: ' + e)
    }
  }, 1000)
}

export const wsDestroy = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  if (permissionChangeTimer) {
    clearTimeout(permissionChangeTimer)
    permissionChangeTimer = null
  }
  if (stompClient && stompClient.connected) {
    stompClient.disconnect(
      function () {
        logger.debug('断开连接')
      },
      function (error) {
        logger.debug('断开连接失败: ' + error)
      }
    )
  }
  stompClient = null
}

export default {
  install() {
    const channels = [
      {
        topic: '/task-export-topic',
        event: 'task-export-topic-call'
      },
      {
        topic: '/report-notice',
        event: 'report-notice-call'
      }
    ]
    // 权限变更话题（单独处理，带防抖逻辑）
    const permissionChangeTopic = '/permission-change-topic'
    function isLoginStatus() {
      if (wsCache.get('app.desktop')) {
        return true
      }
      return wsCache.get('user.token') && wsCache.get('user.uid')
    }

    function connection() {
      if (!isLoginStatus()) {
        return
      }
      if (stompClient && stompClient.connected) {
        return
      }
      let prefix = '/'
      const dataEaseBi = window.DataEaseBi as { baseUrl?: string } | undefined
      if (dataEaseBi?.baseUrl) {
        prefix = dataEaseBi.baseUrl
      } else {
        // const href = window.location.href
        prefix = location.origin + location.pathname
        if (env.MODE === 'dev') {
          prefix = dev.server.proxy[basePath].target + '/'
        }
      }
      if (!prefix.endsWith('/')) {
        prefix += '/'
      }
      const userId = wsCache.get('app.desktop') ? 1 : wsCache.get('user.uid')
      const socket = new SockJS(prefix + 'websocket?userId=' + userId)
      stompClient = Stomp.over(socket)
      const heads = {
        userId: userId
      }
      stompClient.connect(
        heads,
        () => {
          channels.forEach(channel => {
            stompClient.subscribe('/user/' + userId + channel.topic, res => {
              res && res.body && useEmitt().emitter.emit(channel.event, res.body)
            })
          })
          // 订阅权限变更通知
          stompClient.subscribe('/user/' + userId + permissionChangeTopic, res => {
            if (res && res.body) {
              try {
                const data = JSON.parse(res.body)
                if (data && data.type === 'PERMISSION_CHANGE') {
                  handlePermissionChange(data)
                }
              } catch (e) {
                logger.error('解析权限变更消息失败: ' + e)
              }
            }
          })
        },
        error => {
          disconnect()
          logger.error('连接失败: ' + error)
        }
      )
    }

    function disconnect() {
      if (stompClient && stompClient.connected) {
        stompClient.disconnect(
          function () {
            logger.debug('断开连接')
          },
          function (error) {
            logger.debug('断开连接失败: ' + error)
          }
        )
      }
      stompClient = null
    }

    function initialize() {
      connection()
      heartbeatTimer = setInterval(() => {
        if (!isLoginStatus()) {
          disconnect()
          return
        }
        if (!stompClient || !stompClient.connected) {
          connection()
        }
      }, 5000)
    }

    initialize()
  }
}
