import request from '@/config/axios'
import type {
  Role,
  RoleForm,
  RoleListRequest,
  RoleListResponse,
  MenuPermission,
  ResourcePermission
} from './types'

const BASE_URL = '/role'

interface RoleResponse {
  data?: { records?: Role[] } | Role[]
  records?: Role[]
}

// 获取角色列表
export const getRoleList = async (params: RoleListRequest): Promise<RoleListResponse> => {
  const response: RoleResponse = await request.post({
    url: `${BASE_URL}/byCurOrg`,
    data: { keyword: params.keyword || '' }
  })
  // 适配后端响应格式: { code, data: [...], msg }
  // 后端直接返回数组，不是分页对象
  const data = response.data || response
  const records = Array.isArray(data) ? data : data.records || []

  return {
    records,
    total: records.length
  }
}

// 获取角色选项
export const getRoleOptions = (): Promise<Role[]> => {
  return request.get({ url: `${BASE_URL}/options` })
}

// 创建角色
export const createRole = (data: RoleForm): Promise<Role> => {
  return request.post({ url: `${BASE_URL}/create`, data })
}

// 更新角色
export const updateRole = (id: string, data: RoleForm): Promise<Role> => {
  return request.post({ url: `${BASE_URL}/edit`, data: { ...data, id } })
}

// 删除角色
export const deleteRole = (id: string): Promise<void> => {
  return request.post({ url: `${BASE_URL}/delete/${id}` })
}

// 获取角色权限
export const getRolePermissions = (
  roleId: string
): Promise<{
  menus: MenuPermission[]
  resources: ResourcePermission[]
}> => {
  return request.get({ url: `${BASE_URL}/permission/${roleId}` })
}

// 保存角色权限
export const saveRolePermissions = (
  roleId: string,
  data: { menuIds: string[]; resourceIds: string[] }
): Promise<void> => {
  return request.post({ url: `${BASE_URL}/permission/${roleId}`, data })
}

interface MenuTreeItem {
  id: string | number
  name: string
  path?: string
  icon?: string
  type?: number
  meta?: {
    title?: string
    icon?: string
  }
  children?: MenuTreeItem[]
}

// 获取菜单树（用于权限管理，返回所有菜单）
export const getMenuTree = (): Promise<MenuTreeItem[]> => {
  return request.get({ url: '/menu/tree' })
}

// 获取资源列表
export const getResourceList = (): Promise<ResourcePermission[]> => {
  return request.get({ url: '/resource/list' })
}
