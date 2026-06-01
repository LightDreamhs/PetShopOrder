<template>
  <div class="success-page">
    <div class="success-card">
      <van-icon name="checked" size="60" color="#07c160" />
      <h2 class="success-title">下单成功</h2>
      <div v-if="order" class="success-info">
        <div class="info-row">
          <span class="info-label">订单号</span>
          <span class="info-value">{{ order.orderNo }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">待付金额</span>
          <span class="info-amount">¥{{ order.totalAmount }}</span>
        </div>
        <div v-if="paymentQrUrl" class="pay-qr-section">
          <p class="pay-qr-title">请扫码完成付款</p>
          <img :src="paymentQrUrl" class="pay-qr-img" alt="收款码" />
        </div>
        <div v-else class="pay-hint">请与店主通过其他方式完成付款</div>
        <div v-if="order.deliveryDistanceText" class="info-row">
          <span class="info-label">配送距离</span>
          <span class="info-value">{{ order.deliveryDistanceText }}</span>
        </div>
      </div>

      <div v-if="isOverRange" class="over-range-tip">
        <van-icon name="warning-o" />
        <span>超出配送范围，商家将电话联系您确认运费</span>
      </div>

      <div class="success-actions">
        <van-button round block @click="router.push('/')">继续选购</van-button>
        <van-button type="primary" round block @click="router.push('/orders')">查看订单</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { getPublicConfig } from '@/api/config'

const router = useRouter()
const orderStore = useOrderStore()
const order = computed(() => orderStore.lastOrder)
const isOverRange = computed(() => {
  if (!order.value) return false
  return order.value.deliveryDistanceMeter !== null && order.value.deliveryDistanceMeter > 3000
})

const paymentQrUrl = ref('')

onMounted(async () => {
  try {
    const res = await getPublicConfig()
    paymentQrUrl.value = res.data.paymentQrUrl || ''
  } catch {
    // 获取失败时保持兜底文字
  }
})
</script>

<style scoped lang="scss">
.success-page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.success-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 20px;
  text-align: center;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.success-title {
  margin: 12px 0 20px;
  font-size: 20px;
  color: $text;
}

.success-info {
  text-align: left;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  color: $text-secondary;
  font-size: 14px;
}

.info-value {
  font-size: 14px;
  color: $text;
}

.info-amount {
  font-size: 18px;
  font-weight: 700;
  color: $primary;
}

.pay-qr-section {
  text-align: center;
  margin-top: 12px;
  padding: 16px 0;
}

.pay-qr-title {
  font-size: 14px;
  color: $text;
  font-weight: 500;
  margin: 0 0 12px;
}

.pay-qr-img {
  width: 200px;
  height: 200px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.pay-hint {
  font-size: 12px;
  color: $text-muted;
  text-align: center;
  margin-top: 4px;
}

.over-range-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #fff7f2;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: $primary;
  margin-bottom: 20px;
}

.success-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;

  .van-button {
    flex: 1;
  }
}
</style>
