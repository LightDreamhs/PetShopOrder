import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShops } from '@/api/shop'
import { getStoredShopCode, setStoredShopCode } from '@/utils/shop'
import type { ShopInfo } from '@/types'

/**
 * 当前门店（H5 多店）。
 * - init()：应用启动时解析 URL ?s={code}（扫码进店）→ 覆盖本地记录 → 拉取门店列表校验；
 *   本地记录失效（门店不存在）时回退默认店（列表首个，后端同规则）。
 * - switchShop()：切换门店并清空购物车（不同门店商品/价格体系不同，禁止跨店混购）。
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
    if (fromUrl && /^[a-zA-Z0-9-]{2,32}$/.test(fromUrl)) {
      currentCode.value = fromUrl.toLowerCase()
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
  }

  function switchShop(code: string) {
    if (code === currentCode.value) return
    currentCode.value = code
    setStoredShopCode(code)
    // 跨店禁混购：清空购物车
    import('@/stores/cart').then(({ useCartStore }) => {
      useCartStore().clearCart()
    })
  }

  return { shops, currentCode, loaded, currentShop, fallbackShop, displayShop, init, switchShop }
})
