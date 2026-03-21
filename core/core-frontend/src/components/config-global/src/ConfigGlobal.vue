<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus-secondary'
import type { Language } from 'element-plus-secondary/es/locale'
import { useLocaleStore } from '@/store/modules/locale'

// 多语言相关
const localeStore = useLocaleStore()

const currentLocale = computed(() => localeStore.currentLocale)
const elementLocale = computed<Language | undefined>(
  () => currentLocale.value.elLocale as unknown as Language
)
</script>

<template>
  <ElConfigProvider :locale="elementLocale" namespace="ed">
    <slot></slot>
  </ElConfigProvider>
</template>
