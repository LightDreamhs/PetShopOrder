import request from './index'
import type {
  AddonService,
  AppointmentCreateReq,
  AppointmentCreateResult,
  AppointmentListItem,
  ConflictCheckReq,
  ConflictCheckResult,
  Paginated,
  TimeSlot,
} from '@/types'

/** 主服务的附加服务列表 */
export async function getAddons(mainProductId: number): Promise<AddonService[]> {
  const res = await request.get('/api/app/appointments/addons', { params: { mainProductId } })
  return (res as any).data
}

/** 冲突预检（选时间时实时调用） */
export async function checkConflict(data: ConflictCheckReq): Promise<ConflictCheckResult> {
  const res = await request.post('/api/app/appointments/check', data)
  return (res as any).data
}

/** 某日时段可约状态（时间网格铺开展示用） */
export async function getSlots(
  date: string,
  mainSkuId: number,
  addonSkuIds: number[] = [],
): Promise<TimeSlot[]> {
  const res = await request.get('/api/app/appointments/slots', {
    params: { date, mainSkuId, addonSkuIds },
  })
  return (res as any).data
}

/** 创建预约（同事务生成订单+预约） */
export async function createAppointment(data: AppointmentCreateReq): Promise<AppointmentCreateResult> {
  const res = await request.post('/api/app/appointments', data)
  return (res as any).data
}

/** 取消预约 */
export async function cancelAppointment(id: number): Promise<void> {
  await request.post(`/api/app/appointments/${id}/cancel`)
}

/** 我的预约列表 */
export async function getMyAppointments(
  params: { page?: number; size?: number; status?: string } = {},
): Promise<Paginated<AppointmentListItem>> {
  const res = await request.get('/api/app/appointments/mine', { params })
  return (res as any).data
}
