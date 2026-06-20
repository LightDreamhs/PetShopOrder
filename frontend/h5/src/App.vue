<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="cachedViews">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import { useKeepAlive } from '@/stores/keepAlive'

// 仅缓存结算页：从结算页跳地址管理再返回时，保留开关/备注/联系人等草稿状态。
// 下单成功后由 CheckoutPage 调用 dropCheckout() 动态移除，避免回到填了一半的旧结算页。
const { cachedViews } = useKeepAlive()
</script>
