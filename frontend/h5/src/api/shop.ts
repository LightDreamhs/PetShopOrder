import request from './index'
import type { ShopInfo } from '@/types'

export function getShops() {
  return request.get<unknown, { code: number; data: ShopInfo[] }>('/api/app/shops')
}
