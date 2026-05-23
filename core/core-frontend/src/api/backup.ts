import { logger } from '@/utils/logger'
import request from '@/config/axios'
import { ElMessage } from 'element-plus-secondary'

export interface BackupRequest {
  id?: string
  type: 'datasource' | 'dataset' | 'dashboard' | 'dataview' | 'combined'
  resourceIds?: string[]
  options?: BackupOptions
  importMode?: 'create' | 'overwrite' | 'skip' | 'rename'
  overwrite?: boolean
}

export interface BackupOptions {
  includeDatasource?: boolean
  includeDataset?: boolean
  includeChart?: boolean
  includeStyle?: boolean
  includePermission?: boolean
  compress?: boolean
}

export interface BackupResponse {
  id: string
  status: 'success' | 'failed' | 'processing'
  fileName?: string
  fileSize?: number
  downloadUrl?: string
  message?: string
  itemCount?: number
  exportTime?: number
}

export interface ExportPackage {
  version: string
  type: string
  exportTime: number
  exportBy: number
  datasources?: Record<string, unknown>[]
  datasets?: Record<string, unknown>[]
  dashboards?: Record<string, unknown>[]
  dataviews?: Record<string, unknown>[]
}

// 导出资源
export const exportData = (data: BackupRequest): Promise<IResponse> => {
  return request.post({ url: '/backupCenter/export', data })
}

// 导入资源
export const importData = (data: BackupRequest): Promise<IResponse> => {
  return request.post({ url: '/backupCenter/import', data })
}

// 上传备份文件
export const uploadBackup = (file: File): Promise<IResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post({ url: '/backupCenter/upload', data: formData })
}

// 下载备份文件
export const downloadBackup = async (id: string, fileName?: string) => {
  try {
    const response = await request.get({
      url: `/backupCenter/download/${id}`,
      responseType: 'blob'
    })
    let blob = response?.data
    // 兼容处理：如果 response.data 被包装了
    if (blob && typeof blob === 'object' && 'data' in blob) {
      blob = blob.data
    }
    if (blob instanceof Blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName || `backup_${id}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } else {
      logger.error('Invalid blob:', blob)
      ElMessage.error('下载失败：无有效的文件数据')
    }
  } catch (error) {
    logger.error('Download failed:', error)
    ElMessage.error('下载失败')
  }
}

// 预览导入内容
export const previewBackup = (file: File): Promise<IResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post({ url: '/backupCenter/preview', data: formData })
}

// 验证导入数据
export const validateBackup = (file: File): Promise<IResponse> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post({ url: '/backupCenter/validate', data: formData })
}

// 获取导出历史
export const getBackupHistory = (): Promise<IResponse> => {
  return request.get({ url: '/backupCenter/history' })
}

// 删除导出记录
export const deleteBackup = (id: string): Promise<IResponse> => {
  return request.delete({ url: `/backupCenter/${id}` })
}

// 获取支持的导出类型
export const getSupportedTypes = (): Promise<IResponse> => {
  return request.get({ url: '/backupCenter/types' })
}

// 获取导出选项
export const getBackupOptions = (type: string): Promise<IResponse> => {
  return request.get({ url: `/backupCenter/options?type=${type}` })
}

// 检查资源是否存在
export const checkResourceExist = (names: string[]): Promise<IResponse> => {
  return request.post({ url: '/backupCenter/checkExist', data: names })
}

// 获取导入模式选项
export const getImportModes = (): Promise<IResponse> => {
  return request.get({ url: '/backupCenter/importModes' })
}

// 获取导出目录
export const getBackupPath = (): Promise<IResponse> => {
  return request.get({ url: '/backupCenter/backupPath' })
}

// 依赖信息接口
export interface DependencyInfo {
  hasDependencies: boolean
  dependencies: {
    datasources: { id: string; name: string }[]
    datasets: { id: string; name: string }[]
  }
}

// 检测资源依赖
export const checkDependencies = (
  type: 'dataset' | 'dashboard' | 'dataview',
  resourceIds: string[]
): Promise<IResponse<DependencyInfo>> => {
  return request.post({
    url: '/backupCenter/checkDependencies',
    data: { type, resourceIds }
  })
}
