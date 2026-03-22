import request from '@/config/axios'

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
  datasources?: any[]
  datasets?: any[]
  dashboards?: any[]
  dataviews?: any[]
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
    const blob = response?.data
    if (blob) {
      // 根据文件扩展名确定MIME类型
      const mimeType = fileName?.endsWith('.zip') ? 'application/zip' : 'application/json'
      // 创建带有正确MIME类型的Blob
      const downloadBlob = new Blob([blob], { type: mimeType })
      const url = window.URL.createObjectURL(downloadBlob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName || `backup_${id}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }
  } catch (error) {
    console.error('Download failed:', error)
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
