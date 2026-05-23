<script lang="ts" setup>
import { logger } from '@/utils/logger'
import noLic from './nolic.vue'
import { ref, useAttrs, onMounted, computed } from 'vue'
import { execute, randomKey, formatArray } from './convert'
import { load, loadDistributed, xpackModelApi } from '@/api/plugin'
import configGlobal from '@/components/config-global/src/ConfigGlobal.vue'
import { useCache } from '@/hooks/web/useCache'
import { i18n } from '@/plugins/vue-i18n'
import * as Vue from 'vue'
import axios from 'axios'
import * as Pinia from 'pinia'
import * as echarts from 'echarts'
import router from '@/router'
import tinymce from 'tinymce/tinymce'
import { useEmitt } from '@/hooks/web/useEmitt'
import { isNull } from '@/utils/utils'

declare global {
  interface Window {
    DEXPack?: {
      mapping: Record<string, { default: Component }>
    }
    _de_xpack_not_loaded?: boolean
    VueDe?: unknown
    AxiosDe?: unknown
    PiniaDe?: unknown
    vueRouterDe?: unknown
    MittAllDe?: unknown
    I18nDe?: unknown
    EchartsDE?: unknown
    tinymce?: unknown
  }
}

const { wsCache } = useCache()

const plugin = ref()

const loading = ref(false)

const attrs = useAttrs()
const jsname = computed(() => String(attrs.jsname || ''))

const showNolic = () => {
  plugin.value = noLic
  loading.value = false
}
const generateRamStr = (len: number) => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let randomStr = ''
  for (var i = 0; i < len; i++) {
    randomStr += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return randomStr
}

import type { Component } from 'vue'

interface VueComponent {
  default: Component
  __esModule?: boolean
}

const importProxy = (bytesArray: number[][]) => {
  const promise = import(
    `../../../../../../${formatArray(bytesArray[6])}/${formatArray(bytesArray[7])}/${formatArray(
      bytesArray[8]
    )}/${formatArray(bytesArray[9])}/${formatArray(bytesArray[10])}.vue`
  )
  promise
    .then((res: VueComponent) => {
      plugin.value = res.default
    })
    .catch(e => {
      logger.error(e)
      showNolic()
    })
}

const loadXpack = async () => {
  if (window['DEXPack']) {
    const xpack = await window['DEXPack'].mapping[jsname.value]
    plugin.value = xpack.default
  }
}

useEmitt({
  name: 'load-xpack',
  callback: loadXpack
})

const loadComponent = () => {
  loading.value = true
  const byteArray = wsCache.get(`de-plugin-proxy`)
  if (byteArray) {
    importProxy(JSON.parse(byteArray))
    loading.value = false
    return
  }
  const key = generateRamStr(randomKey())
  load(key)
    .then(response => {
      const code = response.data
      const byteArray = execute(code, key)
      storeCacheProxy(byteArray)
      importProxy(byteArray)
    })
    .catch(() => {
      emits('loadFail')
      showNolic()
    })
    .finally(() => {
      loading.value = false
    })
}
const storeCacheProxy = (byteArray: number[][]) => {
  const result: number[][] = []
  byteArray.forEach(item => {
    result.push([...item])
  })
  wsCache.set(`de-plugin-proxy`, JSON.stringify(result))
}
const pluginProxy = ref(null)
const invokeMethod = param => {
  if (pluginProxy.value && pluginProxy.value['invokeMethod']) {
    pluginProxy.value['invokeMethod'](param)
  } else if (param.methodName && pluginProxy.value[param.methodName]) {
    pluginProxy.value[param.methodName](param.args)
  }
}
const emits = defineEmits(['loadFail'])
defineExpose({
  invokeMethod
})
onMounted(async () => {
  const key = 'xpack-model-distributed'
  let distributed = false
  if (wsCache.get(key) === null) {
    const res = await xpackModelApi()
    const resData = isNull(res.data) ? 'null' : res.data
    wsCache.set('xpack-model-distributed', resData)
    distributed = res.data
  } else {
    distributed = wsCache.get(key)
  }
  if (isNull(distributed)) {
    setTimeout(() => {
      emits('loadFail')
      loading.value = false
    }, 1000)
    return
  }
  if (distributed) {
    if (window['DEXPack']) {
      const xpack = await window['DEXPack'].mapping[jsname.value]
      plugin.value = xpack.default
    } else if (!window._de_xpack_not_loaded) {
      window._de_xpack_not_loaded = true
      window['VueDe'] = Vue
      window['AxiosDe'] = axios
      window['PiniaDe'] = Pinia
      window['vueRouterDe'] = router
      window['MittAllDe'] = useEmitt().emitter.all
      window['I18nDe'] = i18n
      window['EchartsDE'] = echarts
      if (!window.tinymce) {
        window.tinymce = tinymce
      }
      loadDistributed().then(async res => {
        new Function(res.data)()
        useEmitt().emitter.emit('load-xpack')
      })
    }
  } else {
    loadComponent()
  }
})
</script>

<template>
  <configGlobal>
    <component
      :key="attrs.jsname"
      ref="pluginProxy"
      :is="plugin"
      v-loading="loading"
      v-bind="attrs"
    ></component>
  </configGlobal>
</template>
