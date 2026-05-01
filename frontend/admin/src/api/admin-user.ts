import request from './index'
import type { AdminUser, AdminUserForm } from '@/types'

export function getAdminUsers() {
  return request.get<any, { code: number; data: AdminUser[] }>('/api/admin/users')
}

export function createAdminUser(data: AdminUserForm) {
  return request.post<any, { code: number; data: AdminUser }>('/api/admin/users', data)
}

export function updateAdminUser(id: number, data: Partial<AdminUserForm>) {
  return request.put<any, { code: number; data: AdminUser }>(`/api/admin/users/${id}`, data)
}

export function updateAdminUserStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return request.put(`/api/admin/users/${id}/status`, { status })
}

export function resetAdminUserPassword(id: number, newPassword: string) {
  return request.put(`/api/admin/users/${id}/password`, { newPassword })
}

export function deleteAdminUser(id: number) {
  return request.delete(`/api/admin/users/${id}`)
}
