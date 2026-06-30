<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    :style="{ maxHeight: '70vh' }"
  >
    <div class="sku-popup">
      <div class="sku-header">
        <div class="sku-product-info">
          <div class="sku-img">
            <div class="img-placeholder">{{ product?.type === 'SERVICE' ? '✂️' : '🦴' }}</div>
          </div>
          <div class="sku-price-info">
            <div class="sku-name">{{ product?.name }}</div>
            <div class="sku-price">{{ formatPrice(selectedSku?.dealPrice ?? product?.skus[0]?.dealPrice ?? '0') }}</div>
            <div v-if="product?.description" class="sku-desc">{{ product.description }}</div>
          </div>
        </div>
        <van-icon name="cross" size="20" class="sku-close" @click="visible = false" />
      </div>

      <div class="sku-section">
        <div class="sku-label">规格</div>
        <div class="sku-options">
          <button
            v-for="sku in product?.skus"
            :key="sku.id"
            class="sku-option"
            :class="{ active: selectedSkuId === sku.id }"
            @click="selectSku(sku)"
          >
            <div class="sku-spec-name">{{ sku.specName }}</div>
            <div class="sku-spec-price">{{ formatPrice(sku.dealPrice) }}</div>
          </button>
        </div>
      </div>

      <div class="sku-section">
        <div class="sku-label">数量</div>
        <van-stepper v-model="quantity" min="1" max="99" />
      </div>

      <div class="sku-footer safe-area-bottom">
        <van-button type="primary" block round @click="handleAddToCart">
          加入购物车
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showToast } from 'vant'
import type { ProductDetail, SkuPrice } from '@/types'
import { usePriceDisplay } from '@/composables/usePriceDisplay'
import { useCartStore } from '@/stores/cart'

const props = defineProps<{
  product: ProductDetail | null
  show: boolean
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
}>()

const { formatPrice } = usePriceDisplay()
const cartStore = useCartStore()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
})

const selectedSkuId = ref<number | null>(null)
const quantity = ref(1)

const selectedSku = computed(() => {
  if (!props.product) return null
  return props.product.skus.find((s) => s.id === selectedSkuId.value) ?? null
})

watch(
  () => props.product,
  (p) => {
    if (p && p.skus.length > 0) {
      selectedSkuId.value = p.skus[0].id
    }
    quantity.value = 1
  },
)

function selectSku(sku: SkuPrice) {
  selectedSkuId.value = sku.id
}

function handleAddToCart() {
  if (!props.product || !selectedSku.value) return
  const sku = selectedSku.value

  cartStore.addItem({
    productId: props.product.id,
    skuId: sku.id,
    quantity: quantity.value,
    productName: props.product.name,
    productCoverImg: props.product.coverImg,
    skuName: sku.specName,
    type: props.product.type,
    originalPrice: sku.price,
    dealPrice: sku.dealPrice,
  })

  showToast('已加入购物车')
  visible.value = false
}
</script>

<style scoped lang="scss">
.sku-popup {
  padding: 20px 16px 12px;
}

.sku-header {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.sku-product-info {
  display: flex;
  gap: 12px;
  flex: 1;
}

.sku-img {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fafafa, #f0f0f0);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.sku-name {
  font-size: 15px;
  font-weight: 600;
  color: $text;
  line-height: 1.4;
}

.sku-price-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
}

.sku-price {
  font-size: 22px;
  font-weight: 800;
  color: $primary;
  letter-spacing: -0.5px;
}

.sku-desc {
  font-size: 12px;
  color: $text-muted;
  line-height: 1.4;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.sku-close {
  color: $text-muted;
  padding: 4px;
  cursor: pointer;
}

.sku-section {
  margin-bottom: 18px;
}

.sku-label {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
  color: $text;
}

.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sku-option {
  padding: 8px 14px;
  background: #f7f7f7;
  border: 2px solid transparent;
  border-radius: $radius-sm;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s ease;
  min-width: 44px;
  min-height: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: inherit;

  &.active {
    border-color: $primary;
    background: $primary-light;
  }
}

.sku-spec-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 2px;
}

.sku-spec-price {
  font-size: 12px;
  color: $text-secondary;

  .sku-option.active & {
    color: $primary;
    font-weight: 600;
  }
}

.sku-footer {
  margin-top: 12px;
  padding-top: 8px;

  :deep(.van-button--primary) {
    height: 44px;
    font-weight: 700;
    font-size: 15px;
  }
}
</style>
