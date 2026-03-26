import request from '@/config/axios'

export interface TreeNode {
  id: string
  name: string
  leaf?: boolean
  children?: TreeNode[]
}

// 获取数据源树
export const getDatasourceList = (): Promise<IResponse> => {
  return request.post({ url: '/datasource/tree', data: {} })
}

// 获取数据集树
export const getDatasetTree = (): Promise<IResponse> => {
  return request.post({ url: '/datasetTree/tree', data: {} })
}

// 获取仪表板树
export const getDashboardTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dashboard' } })
}

// 获取数据大屏树
export const getDataviewTree = (): Promise<IResponse> => {
  return request.post({ url: '/dataVisualization/tree', data: { busiFlag: 'dataV' } })
}
