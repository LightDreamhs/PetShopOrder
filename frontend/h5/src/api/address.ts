import request from './index'
import type { AddressRequest, UserAddress } from '@/types'

export async function getAddressList(): Promise<UserAddress[]> {
  const res = await request.get('/api/app/addresses')
  return (res as any).data
}

export async function createAddress(data: AddressRequest): Promise<UserAddress> {
  const res = await request.post('/api/app/addresses', data)
  return (res as any).data
}

export async function updateAddress(id: number, data: AddressRequest): Promise<UserAddress> {
  const res = await request.put(`/api/app/addresses/${id}`, data)
  return (res as any).data
}

export async function setDefaultAddress(id: number): Promise<void> {
  await request.put(`/api/app/addresses/${id}/default`)
}

export async function deleteAddress(id: number): Promise<void> {
  await request.delete(`/api/app/addresses/${id}`)
}
