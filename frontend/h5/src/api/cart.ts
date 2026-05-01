import request from './index'
import type { CartCalculateRequest, CartCalculateResult } from '@/types'

export async function calculateCart(data: CartCalculateRequest): Promise<CartCalculateResult> {
  const res = await request.post('/api/app/cart/calculate', data)
  return (res as any).data
}
