/**
 * 独立的 JSON 解析工具函数
 * 用于打破 chart.ts -> formatter.ts -> util.ts -> chart.ts 的循环依赖
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function parseJson(str: any): any {
  if (typeof str !== 'string') {
    return str
  }
  return JSON.parse(str)
}
