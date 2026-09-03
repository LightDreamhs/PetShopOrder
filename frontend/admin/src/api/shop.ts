import request from './index'
import type { ShopInfo } from '@/types'

export function getShops() {
  return request.get<any, { code: number; data: ShopInfo[] }>('/api/admin/shops')
}

export function getCurrentShop() {
  return request.get<any, { code: number; data: ShopInfo }>('/api/admin/shops/current')
}

export function createShop(data: Partial<ShopInfo>) {
  return request.post<any, { code: number; data: ShopInfo }>('/api/admin/shops', data)
}

export function updateShop(id: number, data: Partial<ShopInfo>) {
  return request.put<any, { code: number; data: ShopInfo }>(`/api/admin/shops/${id}`, data)
}

export function updateShopStatus(id: number, status: 'OPEN' | 'CLOSED') {
  return request.put(`/api/admin/shops/${id}/status`, { status })
}

/** 进店二维码图片地址（img src 直接可用；下载用 fetchBlob） */
export function getShopQrCodeUrl(id: number) {
  return `/api/admin/shops/${id}/qrcode`
}

export async function fetchShopQrCodeBlob(id: number): Promise<Blob> {
  const res = await fetch(getShopQrCodeUrl(id), { credentials: 'include' })
  if (!res.ok) throw new Error('二维码获取失败')
  return res.blob()
}
