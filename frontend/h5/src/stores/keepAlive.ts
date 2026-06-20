import { ref } from 'vue'

/**
 * keep-alive 缓存控制。
 *
 * 仅缓存结算页（Checkout）：从结算页跳地址管理再返回时，保留开关/备注/联系人等草稿状态。
 * 下单成功或登出后调用 dropCheckout() 动态移除，避免回到填了一半的旧结算页。
 */
const cachedViews = ref<string[]>(['Checkout'])

/** 移除结算页缓存，下次进入会是全新页面 */
export function dropCheckout() {
  cachedViews.value = cachedViews.value.filter((name) => name !== 'Checkout')
}

/** 恢复结算页缓存（再次进入结算页时调用） */
export function ensureCheckoutCached() {
  if (!cachedViews.value.includes('Checkout')) {
    cachedViews.value = [...cachedViews.value, 'Checkout']
  }
}

export function useKeepAlive() {
  return { cachedViews }
}
