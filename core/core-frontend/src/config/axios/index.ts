import { AxiosRequestConfig } from 'axios'
import { service } from './service'

import { config } from './config'

const { default_headers } = config

export interface RequestOptions {
  url: string
  method?: string
  params?: Record<string, unknown>
  data?: unknown
  headersType?: string
  responseType?: AxiosRequestConfig['responseType']
  loading?: boolean
  silentError?: boolean
}

const request = <T = any>(option: RequestOptions & { method: string }): Promise<T> => {
  const { url, method, params, data, headersType, responseType, loading, silentError } = option
  return service({
    url: url,
    method,
    loading,
    silentError,
    params,
    data,
    responseType: responseType,
    headers: {
      'Content-Type': headersType || default_headers
    }
  }) as Promise<T>
}

export default {
  get: <T = any>(option: RequestOptions): Promise<T> => {
    return request<T>({ method: 'get', ...option })
  },
  post: <T = any>(option: RequestOptions): Promise<T> => {
    return request<T>({ method: 'post', ...option })
  },
  delete: <T = any>(option: RequestOptions): Promise<T> => {
    return request<T>({ method: 'delete', ...option })
  },
  put: <T = any>(option: RequestOptions): Promise<T> => {
    return request<T>({ method: 'put', ...option })
  }
}
