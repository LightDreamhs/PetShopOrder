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
        </van-cell-group>

        <!-- 地址选择 -->
        <div v-if="needDelivery" class="address-section">
          <div class="address-toolbar">
            <span class="address-toolbar-title">收货地址</span>
            <span class="address-toolbar-action" @click="openAddressManage">管理</span>
          </div>

          <!-- 已保存地址列表 -->
          <div v-if="addressStore.list.length > 0" class="address-list">
            <div
              v-for="addr in addressStore.list"
              :key="addr.id"
              class="address-item"
              :class="{ active: selectedAddressId === addr.id }"
              @click="selectSavedAddress(addr.id)"
            >
              <van-icon name="location-o" class="addr-icon" />
              <div class="addr-info">
                <div class="addr-title">
                  <span v-if="addr.label" class="addr-label">{{ addr.label }}</span>
                  <span>{{ addr.address }}</span>
                </div>
                <div v-if="addr.detail" class="addr-detail">{{ addr.detail }}</div>
              </div>
              <van-icon v-if="selectedAddressId === addr.id" name="success" color="#ee0a24" />
            </div>
            <div class="add-address-btn" @click="showPicker = true">
              <van-icon name="add-o" />
              <span>新增地址</span>
            </div>
          </div>

          <!-- 无地址时 -->
          <div v-else class="no-address" @click="showPicker = true">
            <van-icon name="add-o" size="20" />
            <span>新增收货地址</span>
          </div>
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

    <!-- 地图选点弹窗 -->
    <AddressPicker v-model:show="showPicker" @confirm="handleAddressConfirm" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import { useOrderStore } from '@/stores/order'
import { useAddressStore } from '@/stores/address'
import { calculateCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartCalculateResult, UserAddress } from '@/types'
import AddressPicker from '@/components/checkout/AddressPicker.vue'
import PriceBreakdown from '@/components/checkout/PriceBreakdown.vue'
import { dropCheckout, ensureCheckoutCached } from '@/stores/keepAlive'

// 组件名：配合 App.vue 的 keep-alive include="Checkout" 缓存结算页
defineOptions({ name: 'Checkout' })

const router = useRouter()
const cartStore = useCartStore()
const memberStore = useMemberStore()
const orderStore = useOrderStore()
const addressStore = useAddressStore()

const customerName = ref('')
const remark = ref('')
const needDelivery = ref(false)
const deliveryAddress = ref<{ address: string; detail: string; lat: string; lng: string } | null>(null)
const selectedAddressId = ref<number | null>(null)
const showPicker = ref(false)
const calculateResult = ref<CartCalculateResult | null>(null)
const submitting = ref(false)

const deliveryEnabled = computed(() => {
  const check = calculateResult.value?.deliveryCheck
  if (check) return check.reachedMinAmount
  return cartStore.hasGoods
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

function handleAddressConfirm(data: { address: string; detail?: string; lat: string; lng: string; saved?: boolean }) {
  deliveryAddress.value = {
    address: data.address,
    detail: data.detail || '',
    lat: data.lat,
    lng: data.lng,
  }
  // 若已保存为新地址，默认选中它
  if (data.saved && addressStore.list.length > 0) {
    const matched = addressStore.list.find(
      (a) => a.address === data.address && a.lat === data.lat && a.lng === data.lng,
    )
    if (matched) selectedAddressId.value = matched.id
  }
}

function selectSavedAddress(id: number) {
  const addr = addressStore.getById(id)
  if (!addr) return
  selectedAddressId.value = id
  deliveryAddress.value = {
    address: addr.address,
    detail: addr.detail || '',
    lat: addr.lat,
    lng: addr.lng,
  }
}

function openAddressManage() {
  router.push('/address/manage')
}

// 开启配送时加载地址列表，无地址则自动弹地图
watch(needDelivery, async (v) => {
  if (!v) return
  if (!addressStore.loaded) {
    await addressStore.fetchList()
  }
  if (addressStore.list.length === 0) {
    showPicker.value = true
    return
  }
  // 默认选中默认地址
  const def = addressStore.list.find((a) => a.isDefault) || addressStore.list[0]
  selectSavedAddress(def.id)
})

onMounted(() => {
  addressStore.fetchList().catch(() => {})
})

// 被 keep-alive 缓存，从地址管理页等子路由返回时重新拉取地址，
// 同步已选地址（可能被编辑门牌号/删除/设默认）
onActivated(async () => {
  // 下单成功后被 dropCheckout 移除过，重新进入时恢复缓存
  ensureCheckoutCached()
  if (!needDelivery.value) return
  try {
    await addressStore.fetchList()
  } catch {
    return
  }
  if (addressStore.list.length === 0) {
    // 地址被清空了
    selectedAddressId.value = null
    deliveryAddress.value = null
    return
  }
  const selected = selectedAddressId.value
    ? addressStore.getById(selectedAddressId.value)
    : null
  if (selected) {
    // 已选地址仍在，刷新其 detail（可能被编辑过）
    selectSavedAddress(selected.id)
  } else {
    // 已选地址被删除，回退到默认地址
    const def = addressStore.list.find((a) => a.isDefault) || addressStore.list[0]
    selectSavedAddress(def.id)
  }
})

/** 重置结算页草稿（下单成功后调用） */
function resetDraft() {
  customerName.value = ''
  remark.value = ''
  needDelivery.value = false
  deliveryAddress.value = null
  selectedAddressId.value = null
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
    // 拼接完整地址：POI 名称 + 楼号门牌（如「幸福小区 3栋502」）
    const fullAddress = deliveryAddress.value
      ? [deliveryAddress.value.address, deliveryAddress.value.detail].filter(Boolean).join(' ')
      : undefined
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
      deliveryAddress: fullAddress,
    })
    orderStore.setLastOrder(result)
    cartStore.clearCart()
    // 下单成功后清空结算页缓存与本地草稿，下次进入是全新页面
    resetDraft()
    dropCheckout()
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

.address-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
  margin-bottom: 6px;
}

.address-toolbar-title {
  font-size: 13px;
  color: $text-secondary;
}

.address-toolbar-action {
  font-size: 13px;
  color: $primary;
  cursor: pointer;
}

.address-list {
  background: #fff;
  margin: 0 10px;
  border-radius: 12px;
  overflow: hidden;
}

.address-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;

  &:last-of-type {
    border-bottom: none;
  }

  &.active {
    background: #fff5f5;
  }
}

.addr-icon {
  font-size: 18px;
  color: $primary;
  flex-shrink: 0;
}

.addr-info {
  flex: 1;
  min-width: 0;
}

.addr-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: $text;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.addr-label {
  flex-shrink: 0;
  font-size: 11px;
  color: #fff;
  background: $primary;
  padding: 1px 5px;
  border-radius: 3px;
}

.addr-detail {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.add-address-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  font-size: 14px;
  color: $primary;
  cursor: pointer;
}

.no-address {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px;
  margin: 0 10px;
  background: #fff;
  border-radius: 12px;
  color: $primary;
  font-size: 14px;
  cursor: pointer;
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
