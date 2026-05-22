import request from '@/config/axios'

export const loginApi = (data: Record<string, unknown>) =>
  request.post({ url: '/login/localLogin', data })

export const queryDekey = () => request.get({ url: 'dekey' })

export const querySymmetricKey = () => request.get({ url: 'symmetricKey' })

export const modelApi = () => request.get({ url: 'model' })

export const platformLoginApi = (origin: string) =>
  request.post({ url: '/login/platformLogin/' + origin })

export const logoutApi = () => request.get({ url: '/logout' })

export const refreshApi = (time?: number | string) =>
  request.get({ url: '/login/refresh', params: { time } })

export const refreshAccessApi = (refreshToken: string) =>
  request.post({
    url: '/login/refreshAccess',
    data: `refreshToken=${encodeURIComponent(refreshToken)}`,
    headersType: 'application/x-www-form-urlencoded'
  })

export const uiLoadApi = () => request.get({ url: '/sysParameter/ui' })

export const loginCategoryApi = () => request.get({ url: '/sysParameter/defaultLogin' })
