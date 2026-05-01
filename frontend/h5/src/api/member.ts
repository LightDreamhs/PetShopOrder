import request from './index'
import type { MemberProfile } from '@/types'

export async function getMemberProfile(): Promise<MemberProfile> {
  const res = await request.get('/api/app/member/profile')
  return (res as any).data
}
