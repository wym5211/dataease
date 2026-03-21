import type { Language as ElementPlusLanguage } from 'element-plus-secondary/es/locale'

export type Language = ElementPlusLanguage

export interface LocaleDropdownType {
  lang: LocaleType
  name?: string
  elLocale?: Language
}
