import { defineStore } from 'pinia'
import { store } from '../index'

interface RequestState {
  loadingMap: {
    [key: string]: number
  }
  cachedRequestList: Array<(token: string | null) => void>
}

export const useRequestStore = defineStore('request', {
  state: (): RequestState => {
    return {
      loadingMap: {},
      cachedRequestList: []
    }
  },
  getters: {
    getRequestList(): Array<(token: string | null) => void> {
      return this.cachedRequestList
    }
  },
  actions: {
    setLoadingMap(value: Record<string, number>) {
      this.loadingMap = value
    },
    resetLoadingMap() {
      for (const key in this.loadingMap) {
        this.loadingMap[key] = 0
      }
    },
    addLoading(key: string) {
      if (Object.prototype.hasOwnProperty.call(this.loadingMap, key)) {
        this.loadingMap[key] += 1
      } else {
        this.loadingMap[key] = 1
      }
    },
    reduceLoading(key: string) {
      if (this.loadingMap) {
        this.loadingMap[key] -= 1
      }
    },
    addCacheRequest(fun: (token: string | null) => void) {
      this.cachedRequestList.push(fun)
    },
    cleanCacheRequest() {
      this.cachedRequestList = []
    }
  }
})

export const useRequestStoreWithOut = () => {
  return useRequestStore(store)
}
