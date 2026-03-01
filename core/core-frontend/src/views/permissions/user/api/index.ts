import axiosInstance from '@/config/axios'
import type {
  User,
  UserForm,
  UserListRequest,
  UserListResponse,
  UserOptionsResponse
} from '../types'

const BASE_URL = '/api/permissions/users'

export const getUserList = (params: UserListRequest): Promise<UserListResponse> => {
  return axiosInstance.get({ url: `${BASE_URL}/list`, params })
}

export const getUserOptions = (): Promise<UserOptionsResponse> => {
  return axiosInstance.get({ url: `${BASE_URL}/options` })
}

export const createUser = (data: UserForm): Promise<User> => {
  return axiosInstance.post({ url: `${BASE_URL}/create`, data })
}

export const updateUser = (userId: string, data: UserForm): Promise<User> => {
  return axiosInstance.put({ url: `${BASE_URL}/update/${userId}`, data })
}

export const deleteUser = (userId: string): Promise<void> => {
  return axiosInstance.delete({ url: `${BASE_URL}/delete/${userId}` })
}

export const resetPassword = (userId: string, newPassword: string): Promise<void> => {
  return axiosInstance.put({ url: `${BASE_URL}/reset-password/${userId}`, data: { newPassword } })
}
