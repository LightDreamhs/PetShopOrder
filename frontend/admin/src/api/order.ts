import request from './index'
import type { Paginated, OrderListItem, OrderDetail } from '@/types'

export function getOrders(params: {
  page?: number
  size?: number
  keyword?: string
  processed?: boolean
  needDelivery?: boolean
  startTime?: string
  endTime?: string
}) {
  return request.get<any, { code: number; data: Paginated<OrderListItem> }>('/api/admin/orders', { params })
}

export function getOrder(id: number) {
  return request.get<any, { code: number; data: OrderDetail }>(`/api/admin/orders/${id}`)
}

export function updateOrderProcessed(id: number, processed: boolean) {
  return request.put(`/api/admin/orders/${id}/processed`, { processed })
}
