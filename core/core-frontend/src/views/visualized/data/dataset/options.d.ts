declare module './options.js' {
  export interface DatasetOptionItem {
    value: string
    label: string
  }

  export const textOptions: DatasetOptionItem[]
  export const dateOptions: DatasetOptionItem[]
  export const valueOptions: DatasetOptionItem[]
  export const textOptionsForSysParams: DatasetOptionItem[]
  export const sysParamsIlns: DatasetOptionItem[]
  export const fieldEnums: string[]
}

export {}
