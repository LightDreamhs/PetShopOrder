import request from './index'
import type { Paginated, CreateOrderRequest, CreateOrderResult, OrderListItem, OrderDetail } from '@/types'

export async function createOrder(data: CreateOrderRequest): Promise<CreateOrderResult> {
  const res = await request.post('/api/app/orders', data)
  return (res as any).data
}

export async function getOrders(page = 1, size = 10): Promise<Paginated<OrderListItem>> {
  const res = await request.get('/api/app/orders', { params: { page, size } })
  return (res as any).data
}

export async function getOrderDetail(id: number): Promise<OrderDetail> {
  const res = await request.get(`/api/app/orders/${id}`)
  return (res as any).data
}
