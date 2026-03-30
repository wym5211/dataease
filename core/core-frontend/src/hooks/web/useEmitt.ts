import mitt from 'mitt'
import { onBeforeUnmount } from 'vue'

interface Option {
  name: string // 事件名称
  callback: Fn // 回调
}

const emitter = mitt<Record<string, unknown>>()

export const useEmitt = (option?: Option) => {
  if (option) {
    emitter.on(option.name as string, option.callback as (val: unknown) => void)

    onBeforeUnmount(() => {
      emitter.off(option.name as string, option.callback as (val: unknown) => void)
    })
  }

  return {
    emitter
  }
}
