import request from '@/config/axios'
import type {
  User,
  UserForm,
  UserListRequest,
  UserListResponse,
  UserOptionsResponse
} from '../types'

/**
 * 用户管理API适配器
 * 将前端RESTful API调用适配到后端实际的API格式
 *
 * 说明：
 * - 前端baseURL：/api
 * - Vite代理自动将 /api 替换为 de2api
 * - 最终请求路径：/api/user/... -> de2api/user/...
 */

// 后端API基础路径（不含/de2api，由代理自动添加）
const API_BASE = '/user'

/**
 * 适配后端UserGridVO到前端User格式
 */
const adaptUserGridVO = (backendUser: any): User => {
  return {
    userId: String(backendUser.id || ''),
    username: backendUser.account || '',
    nickName: backendUser.name || '',
    email: backendUser.email || '',
    phone: backendUser.phone || '',
    roles: (backendUser.roleItems || []).map((role: any) => ({
      roleId: String(role.id || ''),
      roleName: role.name || '',
      roleCode: role.code || ''
    })),
    groups: [], // 后端UserGridVO没有groups字段
    status: backendUser.enable ? 1 : 0,
    createTime: backendUser.createTime
      ? new Date(backendUser.createTime).toISOString()
      : new Date().toISOString()
  }
}

// 适配请求参数格式：前端分页从1开始，后端从1开始（一致）
export const getUserList = async (params: UserListRequest): Promise<UserListResponse> => {
  const { page, pageSize, keyword, status, roleId } = params

  // 构建请求体 - 后端UserGridRequest格式
  const requestBody = {
    keyword: keyword || '',
    // statusList: 状态列表（true/false数组）
    statusList:
      status !== undefined
        ? [status === 1] // 转换为[Boolean]
        : undefined,
    // roleIdList: 角色ID列表
    roleIdList: roleId
      ? [parseInt(roleId)] // 转换为[Long]
      : undefined
  }

  try {
    // 调用后端实际API: POST /user/pager/{goPage}/{pageSize}
    // Vite代理会自动转换为：POST /de2api/user/pager/{goPage}/{pageSize}
    const response: any = await request.post({
      url: `${API_BASE}/pager/${page}/${pageSize}`,
      data: requestBody
    })

    // 适配响应格式：后端返回IPage<UserGridVO>
    const backendRecords = response.records || []
    const adaptedRecords = backendRecords.map(adaptUserGridVO)

    return {
      records: adaptedRecords,
      total: response.total || 0
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    throw error
  }
}

export const getUserOptions = async (): Promise<UserOptionsResponse> => {
  try {
    // 调用后端获取角色选项API
    // axios自动添加 /api 前缀 -> /api/user/role/option
    // Vite代理将 /api 替换为 de2api -> /de2api/user/role/option
    const roleResponse: any = await request.get({
      url: '/user/role/option'
    })

    // 后端没有分组选项，返回空数组
    return {
      roles: roleResponse || [],
      groups: []
    }
  } catch (error) {
    console.error('获取选项数据失败:', error)
    // 返回空数据而不是抛出错误，避免页面崩溃
    return {
      roles: [],
      groups: []
    }
  }
}

export const createUser = async (data: UserForm): Promise<User> => {
  try {
    // 适配请求格式：前端UserForm -> 后端UserCreator
    const requestBody = {
      account: data.username, // 前端username -> 后端account
      name: data.nickName, // 前端nickName -> 后端name
      email: data.email,
      phone: data.phone,
      roleIds: data.roleIds ? data.roleIds.map(id => parseInt(id)) : [],
      enable: data.status === 1 // 前端status(0/1) -> 后端enable(boolean)
    }

    // 调用后端实际API: POST /user/create
    // Vite代理会自动转换为：POST /de2api/user/create
    const response: any = await request.post({
      url: `${API_BASE}/create`,
      data: requestBody
    })

    // 后端返回的是用户ID（Long）
    const newUserId = response || Date.now()

    // 返回创建的用户数据
    return {
      userId: String(newUserId),
      username: data.username,
      nickName: data.nickName,
      email: data.email,
      phone: data.phone,
      roles: [], // 角色信息需要重新查询获取
      groups: [],
      status: data.status,
      createTime: new Date().toISOString()
    }
  } catch (error) {
    console.error('创建用户失败:', error)
    throw error
  }
}

export const updateUser = async (userId: string, data: UserForm): Promise<User> => {
  try {
    // 适配请求格式：前端UserForm -> 后端UserEditor
    const requestBody = {
      id: parseInt(userId), // 前端userId(string) -> 后端id(Long)
      account: data.username, // 前端username -> 后端account
      name: data.nickName, // 前端nickName -> 后端name
      email: data.email,
      phone: data.phone,
      roleIds: data.roleIds ? data.roleIds.map(id => parseInt(id)) : [],
      enable: data.status === 1 // 前端status(0/1) -> 后端enable(boolean)
    }

    // 调用后端实际API: POST /user/edit
    // Vite代理会自动转换为：POST /de2api/user/edit
    await request.post({
      url: `${API_BASE}/edit`,
      data: requestBody
    })

    // 返回更新后的数据
    return {
      userId: userId,
      username: data.username,
      nickName: data.nickName,
      email: data.email,
      phone: data.phone,
      roles: [],
      groups: [],
      status: data.status,
      createTime: new Date().toISOString()
    }
  } catch (error) {
    console.error('更新用户失败:', error)
    throw error
  }
}

export const deleteUser = async (userId: string): Promise<void> => {
  try {
    // 调用后端实际API: POST /user/delete/{id}
    // Vite代理会自动转换为：POST /de2api/user/delete/{id}
    await request.post({
      url: `${API_BASE}/delete/${userId}`
    })
  } catch (error) {
    console.error('删除用户失败:', error)
    throw error
  }
}

export const resetPassword = async (userId: string, newPassword: string): Promise<void> => {
  try {
    // 后端的重置密码API是重置为默认密码，不是设置新密码
    // Vite代理会自动转换为：POST /de2api/user/resetPwd/{id}
    await request.post({
      url: `${API_BASE}/resetPwd/${userId}`
    })
  } catch (error) {
    console.error('重置密码失败:', error)
    throw error
  }
}
