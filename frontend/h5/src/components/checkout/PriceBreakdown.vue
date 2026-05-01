<template>
  <div class="price-breakdown">
    <div class="price-row">
      <span>用品金额</span>
      <span>¥{{ data.goodsAmount }}</span>
    </div>
    <div class="price-row">
      <span>服务金额</span>
      <span>¥{{ data.serviceAmount }}</span>
    </div>
    <div class="price-row">
      <span>配送费</span>
      <span v-if="data.deliveryFee !== null">¥{{ data.deliveryFee }}</span>
      <span v-else class="text-muted">待确认</span>
    </div>
    <div class="price-row total">
      <span>合计</span>
      <span class="total-amount">¥{{ data.totalAmount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CartCalculateResult } from '@/types'

defineProps<{
  data: CartCalculateResult
}>()
</script>

<style scoped lang="scss">
.price-breakdown {
  padding: 4px 0;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: $text-secondary;

  &.total {
    border-top: 1px solid $border;
    margin-top: 4px;
    padding-top: 12px;
    font-size: 15px;
    color: $text;
    font-weight: 500;
  }
}

.total-amount {
  font-size: 18px;
  font-weight: 700;
  color: $primary;
}
</style>
