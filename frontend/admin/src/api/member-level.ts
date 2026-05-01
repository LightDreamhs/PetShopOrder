import request from './index'
import type { MemberLevel, MemberLevelForm } from '@/types'

export function getMemberLevels() {
  return request.get<any, { code: number; data: MemberLevel[] }>('/api/admin/member-levels')
}

export function createMemberLevel(data: MemberLevelForm) {
  return request.post<any, { code: number; data: MemberLevel }>('/api/admin/member-levels', data)
}

export function updateMemberLevel(id: number, data: Partial<MemberLevelForm>) {
  return request.put<any, { code: number; data: MemberLevel }>(`/api/admin/member-levels/${id}`, data)
}

export function updateMemberLevelStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return request.put(`/api/admin/member-levels/${id}/status`, { status })
}

export function deleteMemberLevel(id: number) {
  return request.delete(`/api/admin/member-levels/${id}`)
}
