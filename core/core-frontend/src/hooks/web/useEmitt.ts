import mitt from 'mitt'
import { onBeforeUnmount } from 'vue'

interface Option {
  name: string // 事件名称
  callback: Fn // 回调
}

const emitter = mitt<any>()

export const useEmitt = (option?: Option) => {
  if (option) {
    emitter.on(option.name as any, option.callback as any)

    onBeforeUnmount(() => {
      emitter.off(option.name as any, option.callback as any)
    })
  }

  return {
    emitter
  }
}
