<template>
  <div class="checkout-page">
    <van-nav-bar title="确认订单" left-arrow @click-left="router.back()" />

    <div class="checkout-content">
      <!-- 联系信息 -->
      <div class="section">
        <div class="section-title">联系信息</div>
        <van-cell-group inset>
          <van-field v-model="customerName" label="联系人" placeholder="选填，如：张先生" />
          <van-field v-model="remark" label="备注" placeholder="选填，如：周六下午送到" type="textarea" rows="2" />
        </van-cell-group>
      </div>

      <!-- 会员提示 -->
      <div v-if="memberStore.isMember" class="member-tip">
        <van-icon name="coupon-o" />
        <span>{{ memberStore.memberLevelName }} · 已享会员价/折扣</span>
      </div>

      <!-- 配送选项 -->
      <div v-if="cartStore.hasGoods" class="section">
        <div class="section-title">配送方式</div>
        <van-cell-group inset>
          <van-cell title="需要配送" center>
            <template #right-icon>
              <van-switch
                v-model="needDelivery"
                :disabled="!deliveryEnabled"
                size="22px"
              />
            </template>
          </van-cell>
          <div v-if="deliveryHint" class="delivery-hint">
            <van-icon name="info-o" />
            <span>{{ deliveryHint }}</span>
          </div>
          <template v-if="needDelivery && deliveryAddress">
            <van-cell title="配送地址" :value="deliveryAddress.address" />
          </template>
        </van-cell-group>

        <!-- 地址选择 -->
        <div v-if="needDelivery" class="address-section">
          <AddressPicker @confirm="handleAddressConfirm" />
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section">
        <div class="section-title">商品清单</div>
        <div class="item-list">
          <div v-for="item in cartStore.items" :key="cartStore.cartKey(item.productId, item.skuId)" class="item-row">
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span v-if="item.skuName" class="item-spec">{{ item.skuName }}</span>
            </div>
            <div class="item-right">
              <span class="item-price">¥{{ item.dealPrice }}</span>
              <span class="item-qty">×{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 价格明细 -->
      <div v-if="calculateResult" class="section">
        <div class="section-title">费用明细</div>
        <div class="breakdown-card">
          <PriceBreakdown :data="calculateResult" />
        </div>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div class="checkout-footer safe-area-bottom">
      <div class="footer-total">
        <span>合计</span>
        <span class="footer-amount">¥{{ calculateResult?.totalAmount ?? cartStore.totalAmount }}</span>
      </div>
      <van-button
        type="primary"
        round
        class="submit-btn"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交订单
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import { useOrderStore } from '@/stores/order'
import { calculateCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartCalculateResult } from '@/types'
import AddressPicker from '@/components/checkout/AddressPicker.vue'
import PriceBreakdown from '@/components/checkout/PriceBreakdown.vue'

const router = useRouter()
const cartStore = useCartStore()
const memberStore = useMemberStore()
const orderStore = useOrderStore()

const customerName = ref('')
const remark = ref('')
const needDelivery = ref(false)
const deliveryAddress = ref<{ address: string; lat: string; lng: string } | null>(null)
const calculateResult = ref<CartCalculateResult | null>(null)
const submitting = ref(false)

const deliveryEnabled = computed(() => {
  return calculateResult.value?.deliveryCheck.reachedMinAmount ?? false
})

const deliveryHint = computed(() => {
  if (!cartStore.hasGoods) return ''
  const check = calculateResult.value?.deliveryCheck
  if (!check) return ''
  if (!check.reachedMinAmount) {
    return `用品原价还差 ¥${check.gap} 起送`
  }
  return needDelivery.value ? '3公里内免运费，超出需电话确认' : '已满足起送条件'
})

// 计算价格
async function recalculate() {
  if (cartStore.items.length === 0) return
  try {
    calculateResult.value = await calculateCart({
      items: cartStore.items.map((i) => ({
        productId: i.productId,
        skuId: i.skuId,
        quantity: i.quantity,
      })),
      deliveryLat: needDelivery.value ? deliveryAddress.value?.lat : undefined,
      deliveryLng: needDelivery.value ? deliveryAddress.value?.lng : undefined,
    })
  } catch {
    // handled
  }
}

// 初始计算
recalculate()

// 配送变化时重算
watch([needDelivery, deliveryAddress], () => {
  recalculate()
})

function handleAddressConfirm(data: { address: string; lat: string; lng: string }) {
  deliveryAddress.value = data
}

async function handleSubmit() {
  if (cartStore.items.length === 0) {
    showToast('购物车为空')
    return
  }
  if (needDelivery.value && !deliveryAddress.value) {
    showToast('请选择配送地址')
    return
  }

  submitting.value = true
  try {
    const result = await createOrder({
      items: cartStore.items.map((i) => ({
        productId: i.productId,
        skuId: i.skuId,
        quantity: i.quantity,
      })),
      customerName: customerName.value || undefined,
      remark: remark.value || undefined,
      needDelivery: needDelivery.value,
      deliveryLat: deliveryAddress.value?.lat,
      deliveryLng: deliveryAddress.value?.lng,
      deliveryAddress: deliveryAddress.value?.address,
    })
    orderStore.setLastOrder(result)
    cartStore.clearCart()
    router.replace('/order/success')
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.checkout-page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
}

.checkout-content {
  flex: 1;
  padding-bottom: 80px;
}

.section {
  margin: 10px 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: $text;
  padding: 0 16px;
  margin-bottom: 8px;
}

.member-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 16px 10px;
  padding: 10px 14px;
  background: #fffbe8;
  border-radius: 8px;
  font-size: 12px;
  color: #ed6a0c;
}

.delivery-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  font-size: 12px;
  color: $text-muted;
}

.address-section {
  margin-top: 4px;
  padding: 0 4px;
}

.item-list {
  background: #fff;
  margin: 0 10px;
  border-radius: 12px;
  padding: 4px 14px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
}

.item-spec {
  font-size: 12px;
  color: $text-secondary;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.item-price {
  font-size: 14px;
  color: $primary;
  font-weight: 500;
}

.item-qty {
  font-size: 13px;
  color: $text-secondary;
}

.breakdown-card {
  background: #fff;
  margin: 0 10px;
  border-radius: 12px;
  padding: 4px 14px;
}

.checkout-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 50;
}

.footer-total {
  display: flex;
  flex-direction: column;
}

.footer-total span:first-child {
  font-size: 12px;
  color: $text-secondary;
}

.footer-amount {
  font-size: 20px;
  font-weight: 700;
  color: $primary;
}

.submit-btn {
  min-width: 120px;
  font-weight: 600;
}
</style>
