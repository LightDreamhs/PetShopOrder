<template>
  <van-popup v-model:show="visible" position="bottom" round :style="{ maxHeight: '75vh' }">
    <div class="cart-drawer">
      <div class="cart-drawer-header">
        <span class="cart-drawer-title">购物车</span>
        <van-icon name="cross" size="20" class="cart-drawer-close" @click="visible = false" />
      </div>

      <template v-if="cartStore.items.length > 0">
        <div v-if="memberStore.isMember" class="member-tip">
          <van-icon name="coupon-o" />
          <span>{{ memberStore.memberLevelName }} · 服务享{{ memberStore.serviceDiscountText }} · 商品享会员价</span>
        </div>

        <div class="cart-drawer-list">
          <div class="cart-list">
            <div
              v-for="item in cartStore.items"
              :key="cartStore.cartKey(item.productId, item.skuId)"
              class="cart-item"
            >
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
        </div>

        <div class="cart-drawer-footer safe-area-bottom">
          <div class="cart-total">
            <span class="total-label">合计</span>
            <span class="total-amount">¥{{ cartStore.totalAmount }}</span>
          </div>
          <van-button type="primary" round class="checkout-btn" @click="goCheckout">
            去结算（{{ cartStore.totalCount }}件）
          </van-button>
        </div>
      </template>

      <van-empty v-else description="购物车空空的" image="https://fastly.jsdelivr.net/npm/@vant/assets/custom-empty-image.png">
        <van-button type="primary" round class="empty-btn" @click="visible = false">去选购</van-button>
      </van-empty>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import type { CartItem } from '@/types'

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
})

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

function goCheckout() {
  visible.value = false
  router.push('/checkout')
}
</script>

<style scoped lang="scss">
.cart-drawer {
  display: flex;
  flex-direction: column;
  max-height: 75vh;
}

.cart-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
}

.cart-drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: $text;
}

.cart-drawer-close {
  color: $text-muted;
  padding: 4px;
  cursor: pointer;
}

.cart-drawer-list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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

.cart-drawer-footer {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
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
