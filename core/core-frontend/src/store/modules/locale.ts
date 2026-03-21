import { defineStore } from 'pinia'
import { store } from '../index'
import type { LocaleDropdownType } from 'types/localeDropdown'
import zhCnOriginal from 'element-plus-secondary/es/locale/lang/zh-cn'
import enOriginal from 'element-plus-secondary/es/locale/lang/en'
import twOriginal from 'element-plus-secondary/es/locale/lang/zh-tw'
import { getLocale, deepCopy } from '@/utils/utils'
import request from '@/config/axios'
import { setElementPlusLocale } from '@/plugins/element-plus'

// Element Plus locale 类型
type ElementPlusLocale = Record<string, unknown>

// 合并DataEase的国际化配置到Element Plus的国际化结构中
const mergeLocaleData = (baseLocale: ElementPlusLocale, customData: ElementPlusLocale) => {
  const merged = deepCopy(baseLocale || {})

  const mergeRecursive = (obj: ElementPlusLocale, source: ElementPlusLocale) => {
    for (const key in source) {
      if (source.hasOwnProperty(key)) {
        if (
          typeof source[key] === 'object' &&
          source[key] !== null &&
          !Array.isArray(source[key])
        ) {
          if (!obj[key]) obj[key] = {}
          mergeRecursive(obj[key] as ElementPlusLocale, source[key] as ElementPlusLocale)
        } else {
          obj[key] = source[key]
        }
      }
    }
    return obj
  }

  return mergeRecursive(merged, customData)
}

// 加载DataEase的国际化配置
const loadCustomLocaleData = async (lang: string) => {
  try {
    const localeModule = await import(`../../locales/${lang}.ts`)
    const localeData = localeModule.default || localeModule
    return localeData.element_plus || {}
  } catch (error) {
    console.warn(`Failed to load custom locale data for ${lang}:`, error)
    return {}
  }
}

const elLocaleMap = {
  'zh-CN': zhCnOriginal,
  en: enOriginal,
  tw: twOriginal
}
type LocaleLang = keyof typeof elLocaleMap
type LocaleItem = LocaleDropdownType
const normalizeLang = (lang?: string): LocaleLang => {
  if (lang === 'en' || lang === 'tw' || lang === 'zh-CN') return lang
  if (lang?.startsWith('zh')) return 'zh-CN'
  return 'en'
}
interface LocaleState {
  customLoaded: boolean
  currentLocale: LocaleDropdownType
  localeMap: LocaleItem[]
}

export const useLocaleStore = defineStore('locales', {
  state: (): LocaleState => {
    return {
      customLoaded: false,
      currentLocale: {
        lang: normalizeLang(getLocale()),
        elLocale: elLocaleMap[normalizeLang(getLocale())]
      },
      // 多语言
      localeMap: [
        {
          lang: 'zh-CN',
          name: '简体中文'
        },
        {
          lang: 'en',
          name: 'English'
        },
        {
          lang: 'tw',
          name: '繁體中文'
        }
      ]
    }
  },
  getters: {
    getCurrentLocale(): LocaleDropdownType {
      return this.currentLocale
    },
    async getLocaleMap(): Promise<LocaleItem[]> {
      if (this.customLoaded) {
        return this.localeMap
      }
      try {
        const res = await request.get({ url: '/sysParameter/i18nOptions' })
        this.customLoaded = true
        const customMap = res.data
        let match = false
        for (const key in customMap) {
          const item: LocaleItem = {
            lang: normalizeLang(key),
            name: String(customMap[key])
          }
          this.localeMap.push(item)
          if (this.currentLocale?.lang === normalizeLang(key)) {
            match = true
          }
        }
        if (this.currentLocale?.lang && !match) {
          const matchItem = this.localeMap.find(item =>
            item.lang.startsWith(this.currentLocale.lang)
          )
          if (matchItem) {
            this.currentLocale['lang'] = matchItem.lang
          }
        }
        return this.localeMap
      } catch (error) {
        this.customLoaded = true
        return this.localeMap
      }
    }
  },
  actions: {
    async setCurrentLocale(localeMap: LocaleDropdownType) {
      // this.locale = Object.assign(this.locale, localeMap)
      this.currentLocale.lang = normalizeLang(localeMap?.lang as string)
      const baseLocale = elLocaleMap[normalizeLang(localeMap?.lang as string)]
      const customData = await loadCustomLocaleData(localeMap?.lang)

      // 合并基础国际化配置和自定义配置
      this.currentLocale.elLocale = {
        ...(baseLocale as any),
        el: mergeLocaleData((baseLocale as any).el || {}, customData)
      } as any

      // 同时更新Element Plus的国际化配置
      if (this.currentLocale.elLocale) {
        setElementPlusLocale(this.currentLocale.elLocale)
      }
      // wsCache.set('lang', localeMap?.lang)
    },
    async setLang(language: string) {
      this.currentLocale.lang = normalizeLang(language)
      const baseLocale = elLocaleMap[normalizeLang(language)]
      const customData = await loadCustomLocaleData(language)

      // 合并基础国际化配置和自定义配置
      this.currentLocale.elLocale = {
        ...(baseLocale as any),
        el: mergeLocaleData((baseLocale as any).el || {}, customData)
      } as any

      // 同时更新Element Plus的国际化配置
      if (this.currentLocale.elLocale) {
        setElementPlusLocale(this.currentLocale.elLocale)
      }
    }
  }
})

export const useLocaleStoreWithOut = () => {
  return useLocaleStore(store)
}
