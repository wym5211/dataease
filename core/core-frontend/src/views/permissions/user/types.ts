export interface User {
  userId: string
  username: string
  nickName: string
  email: string
  phone?: string
  roles: Role[]
  groups: Group[]
  status: number
  createTime: string
}

export interface Role {
  roleId: string
  roleName: string
  roleCode: string
}

export interface Group {
  groupId: string
  groupName: string
}

export interface UserForm {
  username?: string
  nickName: string
  email: string
  phone?: string
  password?: string
  roleIds: string[]
  groupIds: string[]
  status: number
}

export interface UserListRequest {
  page: number
  pageSize: number
  keyword?: string
  status?: number
  roleId?: string
}

export interface UserListResponse {
  records: User[]
  total: number
}

export interface UserOptionsResponse {
  roles: Role[]
  groups: Group[]
}
