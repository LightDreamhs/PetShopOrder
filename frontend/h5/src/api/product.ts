import request from './index'
import type { Product, ProductDetail } from '@/types'

export async function getProductsByType(type?: 'GOODS' | 'SERVICE'): Promise<Product[]> {
  const res = await request.get('/api/app/products', { params: { type } })
  return (res as any).data
}

export async function getProductDetail(id: number): Promise<ProductDetail> {
  const res = await request.get(`/api/app/products/${id}`)
  return (res as any).data
}
