import request from './index'

export function getPublicConfig() {
  return request.get<any, { code: number; data: { paymentQrUrl: string | null } }>('/api/app/system-config/public')
}
