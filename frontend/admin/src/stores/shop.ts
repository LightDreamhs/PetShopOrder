import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShops } from '@/api/shop'
import { useAuthStore } from '@/stores/auth'
import type { ShopInfo } from '@/types'

const STORAGE_KEY = 'petshop_admin_shop_id'

/**
 * 管理端当前门店。
 * - BOSS：可通过切店器切换任意门店（请求头 X-Shop-Id），后端据此过滤数据；
 * - MANAGER/STAFF：后端按员工归属店强制限定，前端仅展示所属店名称。
 */
export const useShopStore = defineStore('adminShop', () => {
  const authStore = useAuthStore()
  const shops = ref<ShopInfo[]>([])
  const currentShopId = ref<number | null>(Number(localStorage.getItem(STORAGE_KEY)) || null)
  const loaded = ref(false)

  const isBoss = computed(() => authStore.role === 'BOSS')

  const currentShop = computed(
    () => shops.value.find((s) => s.id === currentShopId.value) || shops.value[0] || null,
  )

  /** 员工归属店（MANAGER/STAFF 展示用）：由 /current 接口提供 */
  const boundShop = ref<ShopInfo | null>(null)

  async function fetchShops() {
    const res = await getShops()
    shops.value = res.data || []
    // 校验本地记录的切店目标仍存在
    if (currentShopId.value && !shops.value.some((s) => s.id === currentShopId.value)) {
      currentShopId.value = null
      localStorage.removeItem(STORAGE_KEY)
    }
    loaded.value = true
  }

  /** 登录后初始化：BOSS 拉全量店铺；店长/店员拉当前归属店 */
  async function init() {
    if (!authStore.isLoggedIn) return
    if (authStore.role === 'BOSS') {
      try {
        await fetchShops()
      } catch {
        // BOSS 拉取失败不阻断（无店铺数据时各页面按后端默认店走）
      }
    } else {
      try {
        const { getCurrentShop } = await import('@/api/shop')
        const res = await getCurrentShop()
        boundShop.value = res.data
      } catch {
        // ignore
      }
    }
  }

  function switchShop(id: number) {
    currentShopId.value = id
    if (id) {
      localStorage.setItem(STORAGE_KEY, String(id))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  function reset() {
    shops.value = []
    currentShopId.value = null
    boundShop.value = null
    loaded.value = false
    localStorage.removeItem(STORAGE_KEY)
  }

  return { shops, currentShopId, currentShop, boundShop, isBoss, loaded, init, fetchShops, switchShop, reset }
})
