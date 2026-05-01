<template>
  <div class="product-card">
    <div class="product-img">
      <div class="img-placeholder">
        {{ product.type === 'SERVICE' ? '✂️' : '🦴' }}
      </div>
    </div>
    <div class="product-info">
      <div class="product-name">{{ product.name }}</div>
      <div class="product-price-row">
        <div class="price-group">
          <span class="price-symbol">¥</span>
          <span class="price-deal">{{ product.dealPrice.replace(/^(\d+)\.(\d{2})$/, '$1') }}</span>
          <span class="price-deal-cent">.{{ product.dealPrice.replace(/^(\d+)\.(\d{2})$/, '$2') }}</span>
          <span v-if="product.hasSpec" class="price-from">起</span>
          <span v-if="hasDiscount(product.price, product.dealPrice)" class="price-original">
            ¥{{ product.price }}
          </span>
        </div>
        <div class="product-action">
          <template v-if="product.hasSpec">
            <button class="spec-btn" @click="$emit('selectSpec', product)">
              选规格
            </button>
          </template>
          <template v-else>
            <div class="qty-control">
              <button
                v-if="cartQty > 0"
                class="qty-btn qty-minus"
                @click="$emit('changeQty', product, -1)"
              >
                −
              </button>
              <span v-if="cartQty > 0" class="qty-num">{{ cartQty }}</span>
              <button class="qty-btn qty-add" @click="$emit('changeQty', product, 1)">
                +
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Product } from '@/types'
import { computed } from 'vue'
import { usePriceDisplay } from '@/composables/usePriceDisplay'
import { useCartStore } from '@/stores/cart'

const props = defineProps<{
  product: Product
}>()

defineEmits<{
  selectSpec: [product: Product]
  changeQty: [product: Product, delta: number]
}>()

const { hasDiscount } = usePriceDisplay()
const cartStore = useCartStore()

const cartQty = computed(() => cartStore.getItemQty(props.product.id, null))
</script>

<style scoped lang="scss">
.product-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: $radius-md;
  margin-bottom: 8px;
  box-shadow: $shadow-sm;
}

.product-img {
  flex-shrink: 0;
  width: 76px;
  height: 76px;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fafafa, #f0f0f0);
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: $text;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}

.price-group {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  color: $primary;
  font-size: 12px;
  font-weight: 700;
}

.price-deal {
  color: $primary;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.5px;
}

.price-deal-cent {
  color: $primary;
  font-size: 12px;
  font-weight: 700;
}

.price-from {
  color: $primary;
  font-size: 11px;
  font-weight: 500;
  margin-left: 1px;
}

.price-original {
  color: $text-muted;
  font-size: 11px;
  text-decoration: line-through;
  margin-left: 4px;
}

.product-action {
  flex-shrink: 0;
}

.spec-btn {
  background: $primary;
  color: #fff;
  border: none;
  border-radius: 14px;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  min-width: 44px;
  min-height: 28px;
}

.qty-control {
  display: flex;
  align-items: center;
  gap: 4px;
}

.qty-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;

  &.qty-add {
    background: $primary;
    color: #fff;
  }

  &.qty-minus {
    background: #f0f0f0;
    color: $text-secondary;
  }
}

.qty-num {
  font-size: 14px;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
  color: $text;
}
</style>
