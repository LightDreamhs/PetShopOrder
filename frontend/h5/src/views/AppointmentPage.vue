<template>
  <div class="appointment-page">
    <van-nav-bar title="服务预约" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="loading-center" />

    <template v-else-if="product">
      <!-- 主服务信息 -->
      <div class="service-card">
        <div class="service-img">
          <img v-if="product.coverImg" :src="product.coverImg" class="img-real" alt="" />
          <div v-else class="img-placeholder">✂️</div>
        </div>
        <div class="service-info">
          <div class="service-name">{{ product.name }}</div>
          <div v-if="product.description" class="service-desc">{{ product.description }}</div>
        </div>
      </div>

      <!-- 主服务规格选择 -->
      <div class="section-title">主服务规格</div>
      <div class="sku-options">
        <button
          v-for="sku in product.skus"
          :key="sku.id"
          class="sku-option"
          :class="{ active: mainSkuId === sku.id }"
          @click="mainSkuId = sku.id"
        >
          <div class="sku-spec-name">{{ sku.specName }}</div>
          <div class="sku-meta">
            <span class="sku-price">¥{{ sku.dealPrice }}</span>
            <span v-if="sku.duration" class="sku-duration">{{ sku.duration }}分钟</span>
          </div>
        </button>
      </div>

      <!-- 附加服务多选 -->
      <template v-if="addons.length > 0">
        <div class="section-title">附加服务（可选）</div>
        <div class="addon-list">
          <label
            v-for="addon in addons"
            :key="addon.productId"
            class="addon-item"
            :class="{ active: selectedAddonIds.includes(addon.productId) }"
          >
            <input
              type="checkbox"
              :checked="selectedAddonIds.includes(addon.productId)"
              class="addon-checkbox"
              @change="toggleAddon(addon.productId)"
            />
            <div class="addon-info">
              <div class="addon-name">{{ addon.name }}（{{ addon.skuName }}）</div>
              <div class="addon-meta">
                <span class="addon-price" :class="{ 'is-free': Number(addon.price) === 0 }">
                  {{ Number(addon.price) === 0 ? '免费' : `+¥${addon.price}` }}
                </span>
                <span v-if="addon.duration && addon.duration > 0" class="addon-duration">+{{ addon.duration }}分钟</span>
              </div>
            </div>
          </label>
        </div>
      </template>

      <!-- 宠物信息 + 联系人 -->
      <div class="section-title">基本信息</div>
      <van-cell-group inset class="info-group">
        <van-field
          v-model="petInfo"
          label="宠物信息"
          placeholder="如：金毛 25kg 温顺"
          type="textarea"
          rows="1"
          autosize
        />
        <van-field v-model="customerName" label="联系人" placeholder="请输入联系人姓名" />
      </van-cell-group>

      <!-- 选择预约时间：横向日期条 + 半小时网格 -->
      <div class="section-title">选择预约时间</div>
      <div class="time-picker">
        <!-- 日期条 -->
        <div class="date-bar">
          <div
            v-for="d in dateList"
            :key="d.value"
            class="date-item"
            :class="{ active: selectedDate === d.value }"
            @click="selectDate(d.value)"
          >
            <div class="date-weekday">{{ d.weekday }}</div>
            <div class="date-date">{{ d.label }}</div>
          </div>
        </div>

        <!-- 时间网格 -->
        <div v-if="slotsLoading" class="slots-loading">
          <van-loading size="20px">加载时段中...</van-loading>
        </div>
        <template v-else>
          <div v-if="timeSlots.length > 0" class="time-grid">
            <button
              v-for="slot in timeSlots"
              :key="slot.time"
              class="time-item"
              :class="{
                active: selectedTime === slot.time,
                disabled: !slot.available,
              }"
              :disabled="!slot.available"
              @click="selectTime(slot)"
            >
              {{ slot.time }}
            </button>
          </div>
          <van-empty v-else description="该日期暂无可约时段" :image-size="60" />
          <div class="time-legend">
            <span class="legend-item"><i class="dot dot-active"></i>已选</span>
            <span class="legend-item"><i class="dot dot-full"></i>已约满</span>
            <span class="legend-item muted">半小时为间隔</span>
          </div>
        </template>
      </div>

      <!-- 已选时段摘要 -->
      <div v-if="selectedDateTimeText" class="selected-summary">
        <van-icon name="clock-o" />
        <span>{{ selectedDateTimeText }}</span>
        <span class="summary-duration">（{{ totalDurationDisplay }} 分钟）</span>
      </div>

      <!-- 备注 -->
      <div class="section-title">备注</div>
      <van-cell-group inset class="info-group">
        <van-field
          v-model="remark"
          placeholder="如有特殊需求请备注（如：宠物脾气、需要陪同等）"
          type="textarea"
          rows="2"
          maxlength="200"
          show-word-limit
          autosize
        />
      </van-cell-group>

      <!-- 费用汇总 -->
      <div class="section-title">费用</div>
      <div class="fee-summary">
        <div v-for="(line, idx) in feeLines" :key="idx" class="fee-line">
          <span>{{ line.label }}</span>
          <span>¥{{ line.amount }}</span>
        </div>
        <div class="fee-line fee-total">
          <span>合计</span>
          <span class="total-amount">¥{{ totalAmount }}</span>
        </div>
      </div>

      <div class="bottom-placeholder"></div>

      <!-- 备案号 -->
      <IcpFooter />

      <!-- 底部提交栏 -->
      <div class="submit-bar safe-area-bottom">
        <div class="submit-amount">
          <span class="submit-amount-label">应付</span>
          <span class="submit-amount-value">¥{{ totalAmount }}</span>
        </div>
        <van-button
          type="primary"
          round
          :disabled="!canSubmit"
          :loading="submitting"
          class="submit-btn"
          @click="handleSubmit"
        >
          确认预约
        </van-button>
      </div>
    </template>

    <van-empty v-else description="服务不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { getProductDetail } from '@/api/product'
import { getAddons, getSlots, createAppointment } from '@/api/appointment'
import { useOrderStore } from '@/stores/order'
import type { AddonService, ProductDetail, TimeSlot } from '@/types'
import IcpFooter from '@/components/common/IcpFooter.vue'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const productId = Number(route.params.productId)
const loading = ref(true)
const submitting = ref(false)
const product = ref<ProductDetail | null>(null)
const addons = ref<AddonService[]>([])

const petInfo = ref('')
const customerName = ref('')
const remark = ref('')
const mainSkuId = ref<number | null>(null)
const selectedAddonIds = ref<number[]>([])

// 时间选择状态
const selectedDate = ref('') // yyyy-MM-dd
const selectedTime = ref('') // HH:mm
const timeSlots = ref<TimeSlot[]>([])
const slotsLoading = ref(false)

onMounted(async () => {
  try {
    const [detail, addonList] = await Promise.all([
      getProductDetail(productId),
      getAddons(productId).catch(() => [] as AddonService[]),
    ])
    product.value = detail
    addons.value = addonList
    if (detail.skus.length > 0) {
      mainSkuId.value = detail.skus[0].id
    }
    // 默认选今天
    selectedDate.value = dateList.value[0].value
  } catch {
    showFailToast('加载服务失败')
  } finally {
    loading.value = false
  }
})

// ========== 日期条（未来 14 天）==========
const dateList = computed(() => {
  const cols: { value: string; label: string; weekday: string }[] = []
  const now = new Date()
  const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  for (let i = 0; i < 14; i++) {
    const d = new Date(now)
    d.setDate(now.getDate() + i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const value = `${y}-${m}-${day}`
    const weekday = i === 0 ? '今天' : i === 1 ? '明天' : i === 2 ? '后天' : weekNames[d.getDay()]
    cols.push({ value, label: `${m}/${day}`, weekday })
  }
  return cols
})

function selectDate(date: string) {
  selectedDate.value = date
  selectedTime.value = ''
  fetchSlots()
}

async function fetchSlots() {
  if (!mainSkuId.value || !selectedDate.value) {
    timeSlots.value = []
    return
  }
  slotsLoading.value = true
  try {
    timeSlots.value = await getSlots(selectedDate.value, mainSkuId.value, selectedAddonSkuIds.value)
  } catch {
    timeSlots.value = []
  } finally {
    slotsLoading.value = false
  }
}

function selectTime(slot: TimeSlot) {
  if (!slot.available) return
  selectedTime.value = slot.time
}

// SKU 或附加变化时重新拉时段
watch([mainSkuId, selectedAddonIds], fetchSlots, { deep: true })

// ========== 费用计算 ==========
const selectedAddons = computed(() =>
  addons.value.filter((a) => selectedAddonIds.value.includes(a.productId)),
)
const selectedAddonSkuIds = computed(() => selectedAddons.value.map((a) => a.skuId))

const feeLines = computed(() => {
  const lines: { label: string; amount: string }[] = []
  const mainSku = product.value?.skus.find((s) => s.id === mainSkuId.value)
  if (mainSku) {
    lines.push({ label: `主服务·${mainSku.specName}`, amount: mainSku.dealPrice })
  }
  for (const a of selectedAddons.value) {
    lines.push({ label: `附加·${a.name}`, amount: a.price })
  }
  return lines
})

const totalAmount = computed(() => {
  const sum = feeLines.value.reduce((acc, l) => acc + Number(l.amount), 0)
  return sum.toFixed(2)
})

// 总时长（主服务 + 占时附加服务）
const totalDurationDisplay = computed(() => {
  const mainSku = product.value?.skus.find((s) => s.id === mainSkuId.value)
  let total = mainSku?.duration ?? 0
  for (const a of selectedAddons.value) {
    if (a.duration && a.duration > 0) total += a.duration
  }
  return total
})

// ========== 附加服务切换 ==========
function toggleAddon(productId: number) {
  const idx = selectedAddonIds.value.indexOf(productId)
  if (idx >= 0) {
    selectedAddonIds.value.splice(idx, 1)
  } else {
    selectedAddonIds.value.push(productId)
  }
}

// ========== 已选时段摘要 ==========
const selectedDateTimeText = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return ''
  const d = selectedDate.value.replace(/-/g, '/')
  return `${d} ${selectedTime.value}`
})

// ========== 可提交判断 ==========
const canSubmit = computed(() => {
  if (!mainSkuId.value || !selectedTime.value) return false
  return true
})

// ========== 提交预约 ==========
async function handleSubmit() {
  if (!canSubmit.value || !mainSkuId.value) return
  submitting.value = true
  try {
    const startTime = `${selectedDate.value} ${selectedTime.value}`
    const result = await createAppointment({
      mainProductId: productId,
      mainSkuId: mainSkuId.value,
      addonSkuIds: selectedAddonSkuIds.value,
      startTime,
      petInfo: petInfo.value || undefined,
      customerName: customerName.value || undefined,
      remark: remark.value || undefined,
    })
    orderStore.setLastOrder({
      id: result.orderId,
      orderNo: result.orderNo,
      totalAmount: result.totalAmount,
      goodsAmount: '0.00',
      serviceAmount: result.totalAmount,
      deliveryFee: '0.00',
      deliveryDistanceMeter: null,
      deliveryDistanceText: null,
    })
    orderStore.setLastAppointment(result)
    router.replace('/order/success')
  } catch (e: any) {
    showFailToast(e?.message || '预约失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.appointment-page {
  min-height: 100vh;
  background: $bg;
  padding-bottom: 20px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.service-card {
  display: flex;
  gap: 12px;
  margin: 12px;
  padding: 12px;
  background: #fff;
  border-radius: $radius-md;
  box-shadow: $shadow-sm;
}

.service-img {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
}

.img-real {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: $radius-sm;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fafafa, #f0f0f0);
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.service-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.service-name {
  font-size: 16px;
  font-weight: 700;
  color: $text;
}

.service-desc {
  margin-top: 4px;
  font-size: 12px;
  color: $text-muted;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.section-title {
  padding: 16px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: $text;
}

.info-group {
  margin-top: 0;
}

/* SKU 选择 */
.sku-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 12px;
}

.sku-option {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  min-width: 90px;

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.05);
  }
}

.sku-spec-name {
  font-size: 13px;
  font-weight: 600;
  color: $text;
}

.sku-meta {
  margin-top: 2px;
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.sku-price {
  font-size: 13px;
  color: $primary;
  font-weight: 700;
}

.sku-duration {
  font-size: 11px;
  color: $text-muted;
}

/* 附加服务 */
.addon-list {
  padding: 0 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.addon-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.05);
  }
}

.addon-checkbox {
  width: 18px;
  height: 18px;
  accent-color: $primary;
  flex-shrink: 0;
}

.addon-info {
  flex: 1;
}

.addon-name {
  font-size: 13px;
  font-weight: 600;
  color: $text;
}

.addon-meta {
  margin-top: 2px;
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.addon-price {
  font-size: 13px;
  color: $primary;
  font-weight: 600;

  &.is-free {
    color: #07c160;
  }
}

.addon-duration {
  font-size: 11px;
  color: $text-muted;
}

/* 时间选择器 */
.time-picker {
  margin: 0 12px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.date-bar {
  display: flex;
  gap: 0;
  overflow-x: auto;
  border-bottom: 1px solid #f5f5f5;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

.date-item {
  flex-shrink: 0;
  padding: 10px 14px;
  text-align: center;
  cursor: pointer;
  position: relative;

  &.active {
    .date-weekday,
    .date-date {
      color: $primary;
      font-weight: 700;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 24px;
      height: 3px;
      background: $primary;
      border-radius: 2px;
    }
  }
}

.date-weekday {
  font-size: 12px;
  color: $text-secondary;
}

.date-date {
  margin-top: 2px;
  font-size: 13px;
  color: $text;
  font-weight: 500;
}

.slots-loading {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 12px;
}

.time-item {
  padding: 9px 0;
  background: #f7f8fa;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  color: $text;
  cursor: pointer;
  transition: all 0.15s;

  &.active {
    background: $primary;
    color: #fff;
    font-weight: 600;
  }

  &.disabled {
    background: #f5f5f5;
    color: #c8c9cc;
    cursor: not-allowed;
    text-decoration: line-through;
  }

  &:not(.disabled):not(.active):active {
    background: rgba($primary, 0.1);
  }
}

.time-legend {
  display: flex;
  gap: 14px;
  padding: 0 12px 12px;
  font-size: 11px;
  color: $text-muted;
  align-items: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;

  &.muted {
    margin-left: auto;
  }
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.dot-active {
  background: $primary;
}

.dot-full {
  background: #f5f5f5;
  border: 1px solid #ddd;
}

/* 已选时段摘要 */
.selected-summary {
  margin: 8px 16px 0;
  padding: 10px 12px;
  background: rgba($primary, 0.06);
  border-radius: 8px;
  font-size: 13px;
  color: $primary;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.summary-duration {
  color: $text-muted;
  font-weight: 400;
  font-size: 12px;
}

/* 费用 */
.fee-summary {
  margin: 0 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
}

.fee-line {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
  color: $text-secondary;
}

.fee-total {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
  font-size: 14px;
  font-weight: 600;
  color: $text;
}

.total-amount {
  color: $primary;
  font-weight: 800;
}

.bottom-placeholder {
  height: 70px;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.submit-amount {
  flex: 1;
}

.submit-amount-label {
  font-size: 12px;
  color: $text-muted;
}

.submit-amount-value {
  font-size: 20px;
  font-weight: 800;
  color: $primary;
}

.submit-btn {
  min-width: 130px;
}
</style>
