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
          <div class="sku-img" :class="{ clickable: !!displayImg }" @click="previewImage">
            <img v-if="displayImg" :src="displayImg" alt="" class="img-real" />
            <div v-else class="img-placeholder">
              {{ product?.type === 'SERVICE' ? '✂️' : '🦴' }}
            </div>
            <van-icon v-if="displayImg" name="expand-o" size="12" class="img-zoom" />
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
            <div class="sku-thumb">
              <img
                v-if="sku.imgUrl || product?.coverImg"
                :src="sku.imgUrl || product?.coverImg || ''"
                alt=""
                class="thumb-img"
              />
              <span v-else class="thumb-emoji">{{ product?.type === 'SERVICE' ? '✂️' : '🦴' }}</span>
            </div>
            <div class="sku-spec-name">{{ sku.specName }}</div>
            <div class="sku-spec-price">{{ formatPrice(sku.dealPrice) }}</div>
          </button>
        </div>
      </div>

      <div class="sku-section">
        <div class="sku-label">数量</div>
        <van-stepper v-model="quantity" min="1" max="99" />
      </div>

      <div class="sku-footer">
        <van-button type="primary" block round @click="handleAddToCart">
          加入购物车
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { showToast, showImagePreview } from 'vant'
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

// 展示图：当前 SKU 图优先，未配图回退商品主图
const displayImg = computed(() => selectedSku.value?.imgUrl || props.product?.coverImg || '')

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

// 小图点击看大图（有图才可点）
function previewImage() {
  if (!displayImg.value) return
  showImagePreview({ images: [displayImg.value], closeable: true })
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
    skuImg: sku.imgUrl || props.product.coverImg,
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
  position: relative;
  display: flex;
  flex-direction: column;
  max-height: 70vh;
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
  min-width: 0;
}

.sku-img {
  position: relative;
  width: 76px;
  height: 76px;
  flex-shrink: 0;

  &.clickable {
    cursor: pointer;
  }
}

.img-real {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: $radius-md;
}

.img-zoom {
  position: absolute;
  right: 4px;
  bottom: 4px;
  padding: 3px;
  color: #fff;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 50%;
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

.sku-price-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 4px;
  min-width: 0;
}

.sku-name {
  font-size: 15px;
  font-weight: 600;
  color: $text;
  line-height: 1.4;
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
  white-space: pre-wrap;
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
  padding: 8px;
  background: #f7f7f7;
  border: 2px solid transparent;
  border-radius: $radius-sm;
  min-width: 88px;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;

  &.active {
    border-color: $primary;
    background: $primary-light;
  }
}

.sku-thumb {
  width: 56px;
  height: 56px;
  border-radius: $radius-sm;
  overflow: hidden;
  background: linear-gradient(135deg, #fafafa, #f0f0f0);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-emoji {
  font-size: 22px;
}

.sku-spec-name {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 500;
  color: $text;
}

.sku-spec-price {
  margin-top: 2px;
  font-size: 11px;
  color: $text-secondary;

  .sku-option.active & {
    color: $primary;
    font-weight: 600;
  }
}

.sku-footer {
  flex-shrink: 0;
  margin-top: 12px;
  padding: 8px 16px;
  background: #fff;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
  padding-bottom: calc(8px + constant(safe-area-inset-bottom));
  padding-bottom: calc(8px + env(safe-area-inset-bottom));

  :deep(.van-button--primary) {
    height: 44px;
    font-weight: 700;
    font-size: 15px;
  }
}
</style>
