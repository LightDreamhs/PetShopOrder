import request from './index'
import type { BookingBoardItem, Paginated } from '@/types'

export function getBookings(params: {
  page?: number
  size?: number
  date?: string
  status?: string
  keyword?: string
}) {
  return request.get<any, { code: number; data: Paginated<BookingBoardItem> }>(
    '/api/admin/appointments',
    { params },
  )
}

export function markBookingServiced(id: number) {
  return request.put(`/api/admin/appointments/${id}/serviced`)
}

export function cancelBooking(id: number) {
  return request.put(`/api/admin/appointments/${id}/cancel`)
}
