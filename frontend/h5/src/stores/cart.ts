import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

  const totalAmount = computed(() => {
    return items.value
      .reduce((sum, item) => sum + parseFloat(item.dealPrice) * item.quantity, 0)
      .toFixed(2)
  })

  const hasGoods = computed(() => items.value.some((item) => item.type === 'GOODS'))

  const goodsOriginalTotal = computed(() => {
    return items.value
      .filter((item) => item.type === 'GOODS')
      .reduce((sum, item) => sum + parseFloat(item.originalPrice) * item.quantity, 0)
      .toFixed(2)
  })

  function addItem(entry: CartItem) {
    const key = cartKey(entry.productId, entry.skuId)
    const existing = items.value.find((item) => cartKey(item.productId, item.skuId) === key)
    if (existing) {
      existing.quantity += entry.quantity
    } else {
      items.value.push({ ...entry })
    }
  }

  function removeItem(productId: number, skuId: number | null) {
    const key = cartKey(productId, skuId)
    items.value = items.value.filter((item) => cartKey(item.productId, item.skuId) !== key)
  }

  function updateQuantity(productId: number, skuId: number | null, qty: number) {
    if (qty <= 0) {
      removeItem(productId, skuId)
      return
    }
    const key = cartKey(productId, skuId)
    const item = items.value.find((item) => cartKey(item.productId, item.skuId) === key)
    if (item) {
      item.quantity = qty
    }
  }

  function clearCart() {
    items.value = []
  }

  function getItemQty(productId: number, skuId: number | null): number {
    const key = cartKey(productId, skuId)
    const item = items.value.find((item) => cartKey(item.productId, item.skuId) === key)
    return item?.quantity ?? 0
  }

  function cartKey(productId: number, skuId: number | null): string {
    return `${productId}_${skuId ?? 'default'}`
  }

  return {
    items,
    totalCount,
    totalAmount,
    hasGoods,
    goodsOriginalTotal,
    addItem,
    removeItem,
    updateQuantity,
    clearCart,
    getItemQty,
    cartKey,
  }
}, {
  persist: {
    key: 'petshop_cart',
    pick: ['items'],
  },
})
