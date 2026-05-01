<template>
  <div class="order-list-page">
    <van-nav-bar title="我的订单" left-arrow @click-left="router.back()" />

    <div class="order-content">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          @click="router.push(`/orders/${order.id}`)"
        >
          <div class="order-header">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-time-header">{{ order.createTime }}</span>
          </div>
          <div class="order-body">
            <div class="order-summary">{{ order.summaryText }}</div>
            <div class="order-meta">
              <span v-if="order.needDelivery" class="order-delivery">
                <van-icon name="logistics" />
                配送
              </span>
            </div>
          </div>
          <div class="order-footer">
            <span class="order-total">¥{{ order.totalAmount }}</span>
            <span class="order-count">{{ order.itemCount }}件商品</span>
          </div>
        </div>
      </van-list>

      <van-empty v-if="!loading && orders.length === 0 && initialized" description="暂无订单">
        <van-button type="primary" round @click="router.push('/')">去下单</van-button>
      </van-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders } from '@/api/order'
import type { OrderListItem } from '@/types'

const router = useRouter()
const orders = ref<OrderListItem[]>([])
const loading = ref(false)
const finished = ref(false)
const initialized = ref(false)
let page = 1

async function loadMore() {
  try {
    const res = await getOrders(page)
    orders.value.push(...res.list)
    if (orders.value.length >= res.total) {
      finished.value = true
    }
    page++
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    initialized.value = true
  }
}
</script>

<style scoped lang="scss">
.order-list-page {
  min-height: 100vh;
  background: $bg;
}

.order-content {
  padding: 10px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:active {
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.order-no {
  font-size: 14px;
  font-weight: 500;
  color: $text;
}

.order-time-header {
  font-size: 12px;
  color: $text-muted;
}

.order-body {
  margin-bottom: 10px;
}

.order-summary {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: $text-muted;
}

.order-delivery {
  display: flex;
  align-items: center;
  gap: 2px;
  color: $primary;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f5f5f5;
  padding-top: 10px;
}

.order-total {
  font-size: 17px;
  font-weight: 700;
  color: $primary;
}

.order-count {
  font-size: 12px;
  color: $text-muted;
}
</style>
