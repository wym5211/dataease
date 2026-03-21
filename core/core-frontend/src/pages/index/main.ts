import { createApp } from 'vue'
import '@/style/index.less'
import 'normalize.css/normalize.css'
import '@antv/s2/dist/style.min.css'
import 'vxe-table/lib/style.css'
import App from './App.vue'
import { setupI18n } from '@/plugins/vue-i18n'
import { setupStore } from '@/store'
import { setupRouter } from '@/router'
import { setupElementPlus, setupElementPlusIcons } from '@/plugins/element-plus'
// 注册数据大屏组件
import { setupCustomComponent } from '@/custom-component'
import { installDirective } from '@/directive'
import '@/utils/DateUtil'
import '@/permission'
import WebSocketPlugin from '../../websocket'
import { setupErrorHandler } from '@/utils/errorHandler'

const setupAll = async () => {
  const app = createApp(App)

  // 初始化全局错误处理（优先初始化）
  setupErrorHandler(app, {
    showMessage: true,
    reportError: false, // 生产环境可开启
    logError: true
  })

  installDirective(app)
  setupStore(app)
  await setupI18n(app)
  setupRouter(app)
  setupElementPlus(app)
  setupCustomComponent(app)
  setupElementPlusIcons(app)
  app.use(WebSocketPlugin)
  app.mount('#app')
}

setupAll()
