import { i18n } from '@/plugins/vue-i18n'
import { useLocaleStoreWithOut } from '@/store/modules/locale'
import { setHtmlPageLang } from '@/plugins/vue-i18n/helper'
import { PATH_URL } from '@/config/axios/service'
import type { LocaleDropdownType } from 'types/localeDropdown'
import type { WritableComputedRef } from 'vue'

const setI18nLanguage = (locale: LocaleType) => {
  const localeStore = useLocaleStoreWithOut()

  if (i18n.mode === 'legacy') {
    i18n.global.locale = locale
  } else {
    ;(i18n.global.locale as WritableComputedRef<LocaleType>).value = locale
  }
  localeStore.setCurrentLocale({
    lang: locale
  })
  setHtmlPageLang(locale)
}

interface RemoteI18nOption {
  lang: string
  name: string
}

const loadRemoteI18n = async (option: RemoteI18nOption) => {
  const name = option.lang.replace('-', '_')
  const path =
    PATH_URL.startsWith('./') && PATH_URL.length > 2
      ? window.location.pathname + PATH_URL.substring(2)
      : PATH_URL
  const url = `${path}/i18n/custom_${name}_front_${option.name}.js`
  return await import(url)
}

export const useLocale = () => {
  // Switching the language will change the locale of useI18n
  // And submit to configuration modification
  const changeLocale = async (locale: LocaleType) => {
    const globalI18n = i18n.global
    let langModule = null
    if (['zh-CN', 'en', 'tw'].includes(locale)) {
      langModule = await import(`../../locales/${locale}.ts`)
    } else {
      const localeStore = useLocaleStoreWithOut()
      const currentLocale = localeStore.getCurrentLocale
      const localeMap = await localeStore.getLocaleMap
      const cMap: LocaleDropdownType | undefined = localeMap.find(item => {
        return item.lang === currentLocale.lang
      })
      if (!cMap) {
        throw new Error(`Locale not found: ${currentLocale.lang}`)
      }
      langModule = await loadRemoteI18n(cMap as RemoteI18nOption)
    }
    // const langModule = await import(`../../locales/${locale}.ts`)

    globalI18n.setLocaleMessage(locale, langModule.default)

    setI18nLanguage(locale)
  }

  return {
    changeLocale
  }
}
