<template>
  <div class="shop-list-page">
    <van-nav-bar title="选择门店" left-arrow @click-left="onBack" />

    <div class="shop-list">
      <div
        v-for="shop in shopStore.shops"
        :key="shop.id"
        class="shop-card"
        :class="{ 'is-current': shop.code === shopStore.currentCode }"
        @click="onSelect(shop)"
      >
        <div class="shop-info">
          <div class="shop-name-row">
            <span class="shop-name">{{ shop.name }}</span>
            <span class="shop-status" :class="shop.status === 'OPEN' ? 'open' : 'closed'">
              {{ shop.status === 'OPEN' ? '营业中' : '歇业' }}
            </span>
          </div>
          <div v-if="shop.address" class="shop-address">
            <van-icon name="location-o" size="12" />
            {{ shop.address }}
          </div>
          <div v-if="shop.phone" class="shop-phone">
            <van-icon name="phone-o" size="12" />
            {{ shop.phone }}
          </div>
        </div>
        <van-icon v-if="shop.code === shopStore.currentCode" name="success" color="#1989fa" size="20" />
      </div>
      <van-empty v-if="shopStore.loaded && shopStore.shops.length === 0" description="暂无门店" />
    </div>

    <div class="shop-tip">切换门店后购物车将清空，商品价格与配送范围以门店为准</div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { useShopStore } from '@/stores/shop'
import type { ShopInfo } from '@/types'

const router = useRouter()
const shopStore = useShopStore()

function onBack() {
  router.back()
}

function onSelect(shop: ShopInfo) {
  if (shop.status !== 'OPEN') {
    showSuccessToast('该门店歇业中')
    return
  }
  shopStore.switchShop(shop.code)
  showSuccessToast(`已切换到「${shop.name}」`)
  router.replace('/')
}
</script>

<style scoped lang="scss">
.shop-list-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.shop-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shop-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid transparent;

  &.is-current {
    border-color: #1989fa;
  }

  .shop-name-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .shop-name {
    font-size: 16px;
    font-weight: 600;
  }

  .shop-status {
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 4px;

    &.open {
      color: #07c160;
      background: rgba(7, 193, 96, 0.1);
    }

    &.closed {
      color: #969799;
      background: #f2f3f5;
    }
  }

  .shop-address,
  .shop-phone {
    margin-top: 6px;
    font-size: 12px;
    color: #969799;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.shop-tip {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #c8c9cc;
}
</style>
