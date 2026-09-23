<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="cachedViews">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useKeepAlive } from '@/stores/keepAlive'
import { useShopStore } from '@/stores/shop'

// 仅缓存结算页：从结算页跳地址管理再返回时，保留开关/备注/联系人等草稿状态。
// 下单成功后由 CheckoutPage 调用 dropCheckout() 动态移除，避免回到填了一半的旧结算页。
const { cachedViews } = useKeepAlive()

// 页面标题唯一出口：路由 meta 优先（登录/确认订单等），否则用当前店对外品牌名。
// 必须响应式同步而不能在路由守卫里写一次：首航守卫执行时 init() 尚未拉到
// 门店列表，brandName 还是默认店品牌，之后不会有人再纠正。
const route = useRoute()
const shopStore = useShopStore()
watch(
  () => (route.meta.title as string | undefined) || shopStore.brandName,
  (title) => {
    document.title = title
  },
  { immediate: true },
)
</script>
