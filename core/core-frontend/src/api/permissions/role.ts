import request from '@/config/axios'

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  createTime: string
}

export interface RoleListRequest {
  page: number
  pageSize: number
  keyword?: string
}

export interface RoleListResponse {
  records: Role[]
  total: number
}

const BASE_URL = '/api/permissions/roles'

export const roleList = (params: RoleListRequest): Promise<RoleListResponse> => {
  return request.get({ url: `${BASE_URL}/list`, params })
}

export const getRoleOptions = (): Promise<Role[]> => {
  return request.get({ url: `${BASE_URL}/options` })
}

export const createRole = (data: Partial<Role>): Promise<Role> => {
  return request.post({ url: `${BASE_URL}/create`, data })
}

export const updateRole = (id: string, data: Partial<Role>): Promise<Role> => {
  return request.put({ url: `${BASE_URL}/update/${id}`, data })
}

export const deleteRole = (id: string): Promise<void> => {
  return request.delete({ url: `${BASE_URL}/delete/${id}` })
}

export default {
  roleList,
  getRoleOptions,
  createRole,
  updateRole,
  deleteRole
}
