import request from './index'
import type { LoginParams, AdminProfile } from '@/types'

export function login(data: LoginParams) {
  return request.post<any, { code: number; data: AdminProfile }>('/api/admin/auth/login', data)
}

export function logout() {
  return request.post('/api/admin/auth/logout')
}

export function getProfile() {
  return request.get<any, { code: number; data: AdminProfile }>('/api/admin/auth/profile', {
    headers: { 'X-Silent': 'true' },
  })
}
