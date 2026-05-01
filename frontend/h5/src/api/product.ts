import request from './index'
import type { Category, Product, ProductDetail } from '@/types'

export async function getCategories(type?: 'GOODS' | 'SERVICE'): Promise<Category[]> {
  const res = await request.get('/api/app/categories', { params: { type } })
  return (res as any).data
}

export async function getProductsByType(type?: 'GOODS' | 'SERVICE'): Promise<Product[]> {
  const res = await request.get('/api/app/products', { params: { type } })
  return (res as any).data
}

export async function getProductsByCategory(categoryId: number, keyword?: string): Promise<Product[]> {
  const res = await request.get(`/api/app/categories/${categoryId}/products`, { params: { keyword } })
  return (res as any).data
}

export async function getProductDetail(id: number): Promise<ProductDetail> {
  const res = await request.get(`/api/app/products/${id}`)
  return (res as any).data
}
