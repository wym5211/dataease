/**
 * 日志工具类
 * 根据环境变量控制日志输出
 */

/* eslint-disable no-console */

class Logger {
  private isDevelopment: boolean

  constructor() {
    this.isDevelopment = import.meta.env.MODE === 'dev' || import.meta.env.DEV
  }

  debug(message: string | Error, ...args: unknown[]): void {
    if (this.isDevelopment) {
      if (message instanceof Error) {
        console.log('[DEBUG]', message, ...args)
      } else {
        console.log(`[DEBUG] ${message}`, ...args)
      }
    }
  }

  info(message: string | Error, ...args: unknown[]): void {
    if (this.isDevelopment) {
      if (message instanceof Error) {
        console.info('[INFO]', message, ...args)
      } else {
        console.info(`[INFO] ${message}`, ...args)
      }
    }
  }

  warn(message: string | Error, ...args: unknown[]): void {
    if (message instanceof Error) {
      console.warn('[WARN]', message, ...args)
    } else {
      console.warn(`[WARN] ${message}`, ...args)
    }
  }

  error(message: string | Error, ...args: unknown[]): void {
    if (message instanceof Error) {
      console.error('[ERROR]', message, ...args)
    } else {
      console.error(`[ERROR] ${message}`, ...args)
    }
  }
}

export const logger = new Logger()
export default logger
