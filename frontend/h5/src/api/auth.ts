import request from './index'
import type { LoginResult, AuthCheck } from '@/types'

export async function sendSmsCode(phone: string) {
  await request.post('/api/app/auth/sms-code', { phone })
}

export async function login(phone: string, code: string): Promise<LoginResult> {
  const res = await request.post('/api/app/auth/login', { phone, code })
  return (res as any).data
}

export async function logout() {
  await request.post('/api/app/auth/logout')
}

export async function checkAuth(): Promise<AuthCheck> {
  const res = await request.get('/api/app/auth/check')
  return (res as any).data
}
