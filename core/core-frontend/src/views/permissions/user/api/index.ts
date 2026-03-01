import request from '@/config/axios'
import type {
  User,
  UserForm,
  UserListRequest,
  UserListResponse,
  UserOptionsResponse
} from '../types'

const BASE_URL = '/api/permissions/users'

export const getUserList = (params: UserListRequest): Promise<UserListResponse> => {
  return request.get({ url: `${BASE_URL}/list`, params })
}

export const getUserOptions = (): Promise<UserOptionsResponse> => {
  return request.get({ url: `${BASE_URL}/options` })
}

export const createUser = (data: UserForm): Promise<User> => {
  return request.post({ url: `${BASE_URL}/create`, data })
}

export const updateUser = (userId: string, data: UserForm): Promise<User> => {
  return request.put({ url: `${BASE_URL}/update/${userId}`, data })
}

export const deleteUser = (userId: string): Promise<void> => {
  return request.post({ url: `${BASE_URL}/delete/${userId}` })
}

export const resetPassword = (userId: string, newPassword: string): Promise<void> => {
  return request.post({ url: `${BASE_URL}/reset-password/${userId}`, data: { newPassword } })
}
