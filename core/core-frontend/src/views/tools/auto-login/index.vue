<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import router from '@/router'
import { useRoute } from 'vue-router'
import { useCache } from '@/hooks/web/useCache'
import { useAppStoreWithOut } from '@/store/modules/app'
import { useUserStoreWithOut } from '@/store/modules/user'
import { loginApi, queryDekey } from '@/api/login'
import { rsaEncryp } from '@/utils/encryption'

const route = useRoute()
const { wsCache } = useCache()
const appStore = useAppStoreWithOut()
const userStore = useUserStoreWithOut()

const status = ref<'idle' | 'logging' | 'done' | 'fail'>('idle')
const logs = ref<string[]>([])

const addLog = (m: string) => {
  logs.value.push(m)
}

const sleep = (ms: number) => new Promise(res => setTimeout(res, ms))

onMounted(async () => {
  status.value = 'logging'
  const u = (route.query.u as string) || 'admin'
  const p = (route.query.p as string) || 'DataEase@123456'
  try {
    if (!wsCache.get(appStore.getDekey)) {
      const res = await queryDekey()
      wsCache.set(appStore.getDekey, res.data)
    }
    const param: { name: string; pwd: string } = { name: rsaEncryp(u), pwd: rsaEncryp(p) }
    const res = await loginApi(param)
    const { token, exp } = res.data
    userStore.setToken(token)
    userStore.setExp(exp)
    userStore.setTime(Date.now())
    addLog('登录成功')

    const tests = [
      '/permissions',
      '/permissions/menu',
      '/permissions/resource',
      '/permissions/templates',
      '/permissions/audit'
    ]
    for (const path of tests) {
      try {
        await router.push({ path })
        addLog(`页面打开成功: ${path}`)
        await sleep(400)
      } catch {
        addLog(`页面打开失败: ${path}`)
      }
    }
    status.value = 'done'
    await router.push({ path: '/permissions' })
  } catch (e) {
    status.value = 'fail'
    addLog('自动登录失败')
    await router.push({ path: '/login' })
  }
})
</script>

<template>
  <div style="padding: 16px">
    <div>自动登录状态：{{ status }}</div>
    <ul>
      <li v-for="(m, i) in logs" :key="i">{{ m }}</li>
    </ul>
  </div>
</template>
