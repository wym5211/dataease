/**
 * 全局错误处理器
 * 捕获 Vue 应用中未处理的错误
 */

import type { ComponentPublicInstance } from 'vue'
import type { App } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { logger } from '@/utils/logger'

/**
 * 错误类型枚举
 */
export enum ErrorType {
  VUE_ERROR = 'VUE_ERROR',
  RESOURCE_ERROR = 'RESOURCE_ERROR',
  PROMISE_ERROR = 'PROMISE_ERROR',
  NETWORK_ERROR = 'NETWORK_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

/**
 * 错误信息接口
 */
export interface ErrorInfo {
  type: ErrorType
  message: string
  stack?: string
  componentName?: string
  propsData?: Record<string, unknown>
  url?: string
  timestamp: number
}

/**
 * 错误处理配置
 */
interface ErrorHandlerConfig {
  showMessage?: boolean // 是否显示错误提示
  reportError?: boolean // 是否上报错误
  logError?: boolean // 是否记录日志
}

const defaultConfig: ErrorHandlerConfig = {
  showMessage: true,
  reportError: false, // 生产环境可开启
  logError: true
}

/**
 * 附加错误信息接口
 */
interface AdditionalErrorInfo {
  componentName?: string
  propsData?: Record<string, unknown>
  info?: string
  url?: string
  line?: number
  column?: number
}

/**
 * 格式化错误信息
 */
function formatErrorInfo(
  error: Error,
  type: ErrorType,
  additionalInfo?: AdditionalErrorInfo
): ErrorInfo {
  return {
    type,
    message: error.message || '未知错误',
    stack: error.stack,
    timestamp: Date.now(),
    ...additionalInfo
  }
}

/**
 * 显示错误提示
 */
function showErrorMessage(errorInfo: ErrorInfo) {
  const message = errorInfo.message || '系统错误，请稍后重试'
  ElMessage.error({
    message,
    duration: 3000,
    showClose: true
  })
}

/**
 * 上报错误到服务器
 * TODO: 实现错误上报逻辑
 */
function reportError(errorInfo: ErrorInfo) {
  // 可以集成 Sentry、阿里云日志等服务
  logger.info('错误上报:', errorInfo)
}

/**
 * Vue 错误处理器
 */
export function vueErrorHandler(
  err: Error,
  instance: ComponentPublicInstance | null,
  info: string,
  config: ErrorHandlerConfig = defaultConfig
) {
  const errorInfo = formatErrorInfo(err, ErrorType.VUE_ERROR, {
    componentName: instance?.$options?.name || instance?.$options?.__name,
    propsData: instance?.$props,
    info
  })

  if (config.logError) {
    logger.error('Vue Error:', errorInfo)
  }

  if (config.showMessage) {
    showErrorMessage(errorInfo)
  }

  if (config.reportError) {
    reportError(errorInfo)
  }
}

/**
 * Promise 未捕获错误处理器
 */
export function promiseErrorHandler(event: PromiseRejectionEvent) {
  const error = event.reason instanceof Error ? event.reason : new Error(String(event.reason))
  const errorInfo = formatErrorInfo(error, ErrorType.PROMISE_ERROR)

  logger.error('Promise Rejection:', errorInfo)

  // 阻止默认的控制台错误输出
  event.preventDefault()
}

/**
 * 资源加载错误处理器
 */
export function resourceErrorHandler(event: ErrorEvent) {
  const target = event.target as HTMLElement
  const errorInfo: ErrorInfo = {
    type: ErrorType.RESOURCE_ERROR,
    message: `资源加载失败: ${target.tagName}`,
    url:
      (target as HTMLImageElement | HTMLScriptElement | HTMLLinkElement).src ||
      (target as HTMLLinkElement).href,
    timestamp: Date.now()
  }

  logger.error('Resource Error:', errorInfo)
}

/**
 * 全局错误处理器
 */
export function globalErrorHandler(
  event: ErrorEvent | string,
  source?: string,
  lineno?: number,
  colno?: number,
  error?: Error
) {
  const err = error || new Error(typeof event === 'string' ? event : event.message)
  const errorInfo = formatErrorInfo(err, ErrorType.UNKNOWN_ERROR, {
    url: source,
    line: lineno,
    column: colno
  })

  logger.error('Global Error:', errorInfo)

  // 阻止默认的控制台错误输出
  return true
}

/**
 * 初始化全局错误处理
 */
export function setupErrorHandler(app: App, config: ErrorHandlerConfig = defaultConfig) {
  // Vue 错误处理
  app.config.errorHandler = (
    err: Error,
    instance: ComponentPublicInstance | null,
    info: string
  ) => {
    vueErrorHandler(err, instance, info, config)
  }

  // Promise 未捕获错误
  window.addEventListener('unhandledrejection', promiseErrorHandler)

  // 资源加载错误
  window.addEventListener(
    'error',
    (event: ErrorEvent) => {
      if (event.target !== window) {
        resourceErrorHandler(event)
      }
    },
    true
  )

  // 全局错误
  window.onerror = globalErrorHandler

  logger.info('全局错误处理器已初始化')
}

/**
 * 清理错误处理器
 */
export function cleanupErrorHandler() {
  window.removeEventListener('unhandledrejection', promiseErrorHandler)
  window.onerror = null
}
