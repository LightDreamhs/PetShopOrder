import request from './index'
import type { UploadResult } from '@/types'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { code: number; data: UploadResult }>('/api/admin/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteFile(key: string) {
  return request.delete(`/api/admin/files/${encodeURIComponent(key)}`)
}
