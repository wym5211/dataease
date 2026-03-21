/**
 * 日志工具类
 * 根据环境变量控制日志输出
 */

/* eslint-disable no-console */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type LogLevel = 'debug' | 'info' | 'warn' | 'error'

class Logger {
  private isDevelopment: boolean

  constructor() {
    this.isDevelopment = import.meta.env.MODE === 'dev' || import.meta.env.DEV
  }

  /**
   * 调试日志 - 仅开发环境输出
   */
  debug(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.log(`[DEBUG] ${message}`, ...args)
    }
  }

  /**
   * 信息日志 - 仅开发环境输出
   */
  info(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.info(`[INFO] ${message}`, ...args)
    }
  }

  /**
   * 警告日志 - 所有环境输出
   */
  warn(message: string, ...args: unknown[]): void {
    console.warn(`[WARN] ${message}`, ...args)
  }

  /**
   * 错误日志 - 所有环境输出
   */
  error(message: string, ...args: unknown[]): void {
    console.error(`[ERROR] ${message}`, ...args)
  }

  /**
   * 条件日志 - 仅在条件为真时输出
   */
  debugIf(condition: boolean, message: string, ...args: unknown[]): void {
    if (condition && this.isDevelopment) {
      console.log(`[DEBUG] ${message}`, ...args)
    }
  }
}

export const logger = new Logger()
export default logger
