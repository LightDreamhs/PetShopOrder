<template>
  <div v-if="cartStore.totalCount > 0" class="cart-bar">
    <div class="cart-bar-inner safe-area-bottom">
      <div class="cart-bar-left" @click="drawerVisible = true">
        <div class="cart-icon-wrap">
          <van-icon name="cart-o" size="22" color="#fff" />
          <van-badge v-if="cartStore.totalCount > 0" :content="cartStore.totalCount" class="cart-badge" />
        </div>
        <span class="cart-amount">¥{{ cartStore.totalAmount }}</span>
      </div>
      <button class="cart-btn" @click="goCheckout">去结算</button>
    </div>
  </div>
  <CartDrawer v-model:show="drawerVisible" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import CartDrawer from './CartDrawer.vue'

const router = useRouter()
const cartStore = useCartStore()
const drawerVisible = ref(false)

function goCheckout() {
  router.push('/checkout')
}
</script>

<style scoped lang="scss">
.cart-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  padding: 0 12px 8px;
  pointer-events: none;
}

.cart-bar-inner {
  pointer-events: all;
  background: #1a1a1a;
  border-radius: 25px;
  padding: 5px 5px 5px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.cart-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex: 1;
  min-height: 44px;
}

.cart-icon-wrap {
  position: relative;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff6b1a, $primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -18px;
  box-shadow: 0 3px 10px rgba(255, 90, 0, 0.45);
  flex-shrink: 0;
}

.cart-badge {
  position: absolute;
  top: -2px;
  right: -2px;

  :deep(.van-badge) {
    font-size: 10px;
    min-width: 16px;
    padding: 0 4px;
    background: $danger;
    border: 2px solid #1a1a1a;
  }
}

.cart-amount {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.3px;
}

.cart-btn {
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 18px;
  padding: 0 22px;
  height: 36px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  min-width: 88px;
  letter-spacing: 0.5px;
}
</style>
