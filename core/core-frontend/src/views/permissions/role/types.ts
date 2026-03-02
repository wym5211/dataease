// 角色实体
export interface Role {
  id: string
  name: string
  code: string
  description?: string
  status: number
  createTime: string
}

// 角色表单
export interface RoleForm {
  name: string
  code: string
  description?: string
  status: number
}

// 角色列表请求
export interface RoleListRequest {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}

// 角色列表响应
export interface RoleListResponse {
  records: Role[]
  total: number
}

// 菜单权限
export interface MenuPermission {
  menuId: string
  menuName: string
  checked: boolean
  children?: MenuPermission[]
}

// 资源权限
export interface ResourcePermission {
  resourceId: string
  resourceName: string
  actions: string[]
}

// 角色权限
export interface RolePermission {
  roleId: string
  menuIds: string[]
  resourceIds: string[]
}
