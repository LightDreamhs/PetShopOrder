import request from './index'
import type { Category, CategoryForm } from '@/types'

export function getCategories(type?: string) {
  return request.get<any, { code: number; data: Category[] }>('/api/admin/categories', { params: { type } })
}

export function createCategory(data: CategoryForm) {
  return request.post<any, { code: number; data: Category }>('/api/admin/categories', data)
}

export function updateCategory(id: number, data: Partial<CategoryForm>) {
  return request.put<any, { code: number; data: Category }>(`/api/admin/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete(`/api/admin/categories/${id}`)
}
