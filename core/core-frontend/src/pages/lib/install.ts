import type { Plugin, Component, App } from 'vue'

interface ComponentWithName extends Component {
  name: string
}

export type SFCWithInstall<T> = T & Plugin
export const withInstall = <
  T extends ComponentWithName,
  E extends Record<string, ComponentWithName>
>(
  main: T,
  extra?: E
) => {
  ;(main as SFCWithInstall<T>).install = (app: App): void => {
    for (const comp of [main, ...Object.values(extra ?? {})]) {
      app.component(comp.name, comp)
    }
  }

  if (extra) {
    for (const [key, comp] of Object.entries(extra)) {
      ;(main as Record<string, ComponentWithName>)[key] = comp
    }
  }
  return main as SFCWithInstall<T> & E
}
