import request from './index'

export function getPublicConfig() {
  return request.get<
    any,
    {
      code: number
      data: {
        paymentQrUrl: string | null
        adEnabled: boolean
        adImageUrl: string | null
        adLinkType: string | null
        adLinkTarget: string | null
      }
    }
  >('/api/app/system-config/public')
}
