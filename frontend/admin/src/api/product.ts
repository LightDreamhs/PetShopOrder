import request from './index'
import type { Paginated, ProductListItem, ProductDetail, ProductForm } from '@/types'

export function getProducts(params: {
  page?: number
  size?: number
  keyword?: string
  type?: string
  status?: string
}) {
  return request.get<any, { code: number; data: Paginated<ProductListItem> }>('/api/admin/products', { params })
}

export function getProduct(id: number) {
  return request.get<any, { code: number; data: ProductDetail }>(`/api/admin/products/${id}`)
}

export function createProduct(data: ProductForm) {
  return request.post<any, { code: number; data: ProductDetail }>('/api/admin/products', data)
}

export function updateProduct(id: number, data: Partial<ProductForm>) {
  return request.put<any, { code: number; data: ProductDetail }>(`/api/admin/products/${id}`, data)
}

export function updateProductStatus(id: number, status: 'ON_SALE' | 'OFF_SALE') {
  return request.put(`/api/admin/products/${id}/status`, { status })
}

export function deleteProduct(id: number) {
  return request.delete(`/api/admin/products/${id}`)
}
