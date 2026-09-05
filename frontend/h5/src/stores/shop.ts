import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShops } from '@/api/shop'
import { getStoredShopCode, setStoredShopCode } from '@/utils/shop'
import type { ShopInfo } from '@/types'

/**
 * 当前门店（H5 多店）。
 * 顾客进店只靠扫码：?s={code} 定店并落 localStorage；无参数时沿用上次门店，
 * 无记录时回退默认店（列表首个，后端同规则）。不向顾客提供切店入口。
 * - init()：启动时解析 URL 并校验本地记录有效性；
 *   扫码进入了与上次不同的门店时清空购物车，防止跨店混购。
 */
export const useShopStore = defineStore('shop', () => {
  const shops = ref<ShopInfo[]>([])
  const currentCode = ref(getStoredShopCode())
  const loaded = ref(false)

  const currentShop = computed(() => shops.value.find((s) => s.code === currentCode.value) || null)

  /** 门店不存在/未记录时展示的兜底店（列表首个 = 默认店） */
  const fallbackShop = computed(() => shops.value.find((s) => s.status === 'OPEN') || shops.value[0] || null)

  const displayShop = computed(() => currentShop.value || fallbackShop.value)

  async function init() {
    // 1. 扫码参数优先：?s={code}
    const params = new URLSearchParams(window.location.search)
    const fromUrl = params.get('s')
    let switchedByQr = false
    if (fromUrl && /^[a-zA-Z0-9-]{2,32}$/.test(fromUrl)) {
      const code = fromUrl.toLowerCase()
      switchedByQr = code !== currentCode.value
      currentCode.value = code
      setStoredShopCode(currentCode.value)
      // 清掉地址栏进店参数，避免后续分享/刷新重复处理
      params.delete('s')
      const rest = params.toString()
      const url = window.location.pathname + (rest ? `?${rest}` : '') + window.location.hash
      window.history.replaceState(null, '', url)
    }

    // 2. 拉取门店列表，校验本地记录有效性
    try {
      const res = await getShops()
      shops.value = res.data || []
      if (currentCode.value && !shops.value.some((s) => s.code === currentCode.value)) {
        currentCode.value = ''
        setStoredShopCode('')
      }
      loaded.value = true
    } catch {
      // 列表拉取失败不阻断启动（后端会按默认店兜底）
    }

    // 3. 扫码换店：清空购物车（不同门店商品/价格体系不同）
    if (switchedByQr) {
      import('@/stores/cart').then(({ useCartStore }) => {
        useCartStore().clearCart()
      })
    }
  }

  return { shops, currentCode, loaded, currentShop, fallbackShop, displayShop, init }
})
