import request from './index'
import type { SystemConfig, SystemConfigChangeLog, UpdateSystemConfigRequest } from '@/types'

export interface SystemConfigResponse {
  config: SystemConfig
  changeLogs: SystemConfigChangeLog[]
}

export function getSystemConfig() {
  return request.get<any, { code: number; data: SystemConfigResponse }>('/api/admin/system-config')
}

export function updateSystemConfig(data: UpdateSystemConfigRequest) {
  return request.put<any, { code: number; data: SystemConfig }>('/api/admin/system-config', data)
}

export function testQywxWebhook(webhookUrl: string) {
  return request.post('/api/admin/system-config/test-webhook', { webhookUrl })
}
