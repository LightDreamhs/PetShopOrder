import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CreateOrderResult } from '@/types'

export const useOrderStore = defineStore('order', () => {
  const lastOrder = ref<CreateOrderResult | null>(null)

  function setLastOrder(data: CreateOrderResult) {
    lastOrder.value = data
  }

  function clearLastOrder() {
    lastOrder.value = null
  }

  return { lastOrder, setLastOrder, clearLastOrder }
})
