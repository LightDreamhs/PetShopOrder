<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" left-arrow @click-left="router.back()" fixed placeholder />

    <div class="cart-content">
      <template v-if="cartStore.items.length > 0">
        <div class="cart-list">
          <div v-for="item in cartStore.items" :key="cartStore.cartKey(item.productId, item.skuId)" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.productName }}</div>
              <div v-if="item.skuName" class="cart-item-spec">{{ item.skuName }}</div>
              <div class="cart-item-price">
                <span class="price-symbol">¥</span>
                <span class="price-deal">{{ item.dealPrice }}</span>
                <span v-if="item.originalPrice !== item.dealPrice" class="price-original">¥{{ item.originalPrice }}</span>
              </div>
            </div>
            <div class="cart-item-qty">
              <van-stepper
                :model-value="item.quantity"
                min="0"
                max="99"
                @change="(val: number) => handleQtyChange(item, val)"
              />
            </div>
          </div>
        </div>

        <div v-if="memberStore.isMember" class="member-tip">
          <van-icon name="coupon-o" />
          <span>{{ memberStore.memberLevelName }} · 服务享{{ memberStore.serviceDiscountText }} · 商品享会员价</span>
        </div>

        <div class="cart-footer safe-area-bottom">
          <div class="cart-total">
            <span class="total-label">合计</span>
            <span class="total-amount">¥{{ cartStore.totalAmount }}</span>
          </div>
          <van-button type="primary" round class="checkout-btn" @click="router.push('/checkout')">
            去结算（{{ cartStore.totalCount }}件）
          </van-button>
        </div>
      </template>

      <van-empty v-else description="购物车空空的" image="https://fastly.jsdelivr.net/npm/@vant/assets/custom-empty-image.png">
        <van-button type="primary" round class="empty-btn" @click="router.push('/')">去选购</van-button>
      </van-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import type { CartItem } from '@/types'

const router = useRouter()
const cartStore = useCartStore()
const memberStore = useMemberStore()

function handleQtyChange(item: CartItem, val: number) {
  if (val <= 0) {
    cartStore.removeItem(item.productId, item.skuId)
  } else {
    cartStore.updateQuantity(item.productId, item.skuId, val)
  }
}
</script>

<style scoped lang="scss">
.cart-page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
}

.cart-content {
  flex: 1;
  padding-bottom: 80px;
}

.cart-list {
  background: #fff;
  margin: 10px;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.cart-item-info {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.cart-item-name {
  font-size: 14px;
  font-weight: 600;
  color: $text;
}

.cart-item-spec {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 3px;
}

.cart-item-price {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price-symbol {
  color: $primary;
  font-size: 12px;
  font-weight: 700;
}

.price-deal {
  color: $primary;
  font-size: 16px;
  font-weight: 800;
}

.price-original {
  color: $text-muted;
  font-size: 12px;
  text-decoration: line-through;
  margin-left: 4px;
}

.member-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 10px 0;
  padding: 12px 14px;
  background: #fff;
  border-radius: $radius-md;
  font-size: 12px;
  color: #b37400;
  box-shadow: $shadow-sm;
}

.cart-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
  z-index: 50;
}

.cart-total {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.total-label {
  font-size: 14px;
  color: $text-secondary;
}

.total-amount {
  font-size: 22px;
  font-weight: 800;
  color: $primary;
  letter-spacing: -0.5px;
}

.checkout-btn {
  min-width: 120px;
  font-weight: 700;
  height: 40px;
}

.empty-btn {
  width: 160px;
}
</style>
