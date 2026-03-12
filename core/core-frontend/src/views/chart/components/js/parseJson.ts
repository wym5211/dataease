/**
 * 独立的 JSON 解析工具函数
 * 用于打破 chart.ts -> formatter.ts -> util.ts -> chart.ts 的循环依赖
 */

export function parseJson<T>(str: T | string): T {
  if (typeof str !== 'string') {
    return str as T
  }
  return JSON.parse(str) as T
}
