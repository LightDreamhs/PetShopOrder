import request from './index'
import type { Paginated, MemberListItem, MemberForm } from '@/types'

export function getMembers(params: { page?: number; size?: number; keyword?: string; levelId?: number }) {
  return request.get<any, { code: number; data: Paginated<MemberListItem> }>('/api/admin/members', { params })
}

export function createMember(data: MemberForm) {
  return request.post<any, { code: number; data: MemberListItem }>('/api/admin/members', data)
}

export function updateMember(id: number, data: Partial<MemberForm>) {
  return request.put<any, { code: number; data: MemberListItem }>(`/api/admin/members/${id}`, data)
}

export function deleteMember(id: number) {
  return request.delete(`/api/admin/members/${id}`)
}
