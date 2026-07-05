import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AppointmentCreateResult, CreateOrderResult } from '@/types'

export const useOrderStore = defineStore('order', () => {
  const lastOrder = ref<CreateOrderResult | null>(null)
  // 预约下单成功后存预约信息，供下单成功页展示（普通商品下单时为 null）
  const lastAppointment = ref<AppointmentCreateResult | null>(null)

  function setLastOrder(data: CreateOrderResult) {
    lastOrder.value = data
  }

  function setLastAppointment(data: AppointmentCreateResult) {
    lastAppointment.value = data
  }

  function clearLastOrder() {
    lastOrder.value = null
    lastAppointment.value = null
  }

  return { lastOrder, lastAppointment, setLastOrder, setLastAppointment, clearLastOrder }
})
