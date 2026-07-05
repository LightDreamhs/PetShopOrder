<template>
  <div class="order-detail-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" />

    <div v-if="order" class="detail-content">
      <!-- 基本信息 -->
      <div class="section">
        <van-cell-group inset>
          <van-cell title="订单号" :value="order.orderNo" />
          <van-cell title="下单时间" :value="order.createTime" />
          <van-cell v-if="order.cancelled" title="订单状态" value="已取消" value-class="cancelled-text" />
          <van-cell v-if="order.memberLevelSnapshot" title="会员等级" :value="order.memberLevelSnapshot" />
          <van-cell title="联系电话" :value="order.customerPhone" />
          <van-cell v-if="order.customerName" title="联系人" :value="order.customerName" />
        </van-cell-group>
      </div>

      <!-- 预约信息（仅预约订单） -->
      <div v-if="order.appointment" class="section">
        <div class="section-title">
          预约信息
          <span class="appt-status-tag" :class="apptStatusClass(order.appointment.status)">
            {{ apptStatusText(order.appointment.status) }}
          </span>
        </div>
        <van-cell-group inset>
          <van-cell title="开始时间" :value="formatApptTime(order.appointment.startTime)" />
          <van-cell title="结束时间" :value="formatApptTime(order.appointment.endTime)" />
          <van-cell title="服务时长" :value="`${order.appointment.totalDuration} 分钟`" />
          <van-cell v-if="order.appointment.petInfo" title="宠物信息" :value="order.appointment.petInfo" />
        </van-cell-group>
      </div>

      <!-- 配送信息 -->
      <div v-if="order.needDelivery" class="section">
        <div class="section-title">配送信息</div>
        <van-cell-group inset>
          <van-cell title="配送地址" :value="order.deliveryAddress ?? ''" />
          <van-cell v-if="order.deliveryDistanceText" title="配送距离" :value="order.deliveryDistanceText" />
          <van-cell title="配送费" :value="order.deliveryFee === '0.00' ? '免运费' : `¥${order.deliveryFee}`" />
        </van-cell-group>
      </div>

      <!-- 商品明细 -->
      <div class="section">
        <div class="section-title">商品明细</div>
        <div class="item-list">
          <div v-for="(item, idx) in order.items" :key="idx" class="item-row">
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span v-if="item.skuName" class="item-spec">{{ item.skuName }}</span>
            </div>
            <div class="item-right">
              <div class="item-price-group">
                <span class="item-deal">¥{{ item.dealPrice }}</span>
                <span v-if="item.originalPrice !== item.dealPrice" class="item-original">¥{{ item.originalPrice }}</span>
              </div>
              <span class="item-qty">×{{ item.quantity }}</span>
              <span class="item-subtotal">¥{{ item.subtotal }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 费用汇总 -->
      <div class="section">
        <div class="section-title">费用汇总</div>
        <van-cell-group inset>
          <van-cell title="用品金额" :value="`¥${order.goodsAmount}`" />
          <van-cell title="服务金额" :value="`¥${order.serviceAmount}`" />
          <van-cell title="配送费" :value="`¥${order.deliveryFee}`" />
          <van-cell title="合计" :value="`¥${order.totalAmount}`" class="total-cell" />
        </van-cell-group>
      </div>

      <!-- 备注 -->
      <div v-if="order.remark" class="section">
        <div class="section-title">备注</div>
        <div class="remark-content">{{ order.remark }}</div>
      </div>

      <!-- 底部占位（取消按钮栏存在时留出空间） -->
      <div v-if="showCancelBtn" class="bottom-placeholder"></div>
    </div>

    <!-- 取消预约按钮（仅待服务状态） -->
    <div v-if="order && showCancelBtn" class="cancel-bar safe-area-bottom">
      <van-button round block type="danger" plain :loading="cancelling" @click="handleCancel">
        取消预约
      </van-button>
    </div>

    <div v-else-if="!order" class="loading-wrap">
      <van-loading size="24px" vertical>加载中...</van-loading>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showFailToast } from 'vant'
import { getOrderDetail } from '@/api/order'
import { cancelAppointment } from '@/api/appointment'
import type { AppointmentStatus, OrderDetail } from '@/types'

const router = useRouter()
const route = useRoute()
const order = ref<OrderDetail | null>(null)
const cancelling = ref(false)

const showCancelBtn = computed(
  () => !!order.value?.appointment && order.value.appointment.status === 'PENDING',
)

onMounted(async () => {
  await loadOrder()
})

async function loadOrder() {
  const id = Number(route.params.id)
  try {
    order.value = await getOrderDetail(id)
  } catch {
    // handled
  }
}

async function handleCancel() {
  const appt = order.value?.appointment
  if (!appt) return
  try {
    await showConfirmDialog({
      title: '取消预约',
      message: '取消后该时段将释放给其他顾客，确定要取消吗？',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      confirmButtonColor: '#ee0a24',
    })
  } catch {
    return // 用户点取消
  }
  cancelling.value = true
  try {
    await cancelAppointment(appt.id)
    showSuccessToast('预约已取消')
    await loadOrder()
  } catch (e: any) {
    showFailToast(e?.message || '取消失败')
  } finally {
    cancelling.value = false
  }
}

// 格式化预约时间：2026-07-08T14:00:00 → 07-08 14:00
function formatApptTime(dt: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(5, 16)
}

function apptStatusText(status: AppointmentStatus): string {
  if (status === 'PENDING') return '待服务'
  if (status === 'SERVICED') return '已完成'
  if (status === 'CANCELLED') return '已取消'
  return status
}

function apptStatusClass(status: AppointmentStatus): string {
  if (status === 'PENDING') return 'status-pending'
  if (status === 'SERVICED') return 'status-serviced'
  if (status === 'CANCELLED') return 'status-cancelled'
  return ''
}
</script>

<style scoped lang="scss">
.order-detail-page {
  min-height: 100vh;
  background: $bg;
}

.section {
  margin: 10px 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  padding: 0 16px;
  margin-bottom: 8px;
  color: $text;
  display: flex;
  align-items: center;
  gap: 8px;
}

.appt-status-tag {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 8px;
  font-weight: 500;
}

.status-pending {
  background: rgba(255, 153, 0, 0.12);
  color: #ff9900;
}

.status-serviced {
  background: rgba(7, 193, 96, 0.12);
  color: #07c160;
}

.status-cancelled {
  background: #f5f5f5;
  color: $text-muted;
}

.cancelled-text {
  color: #ee0a24 !important;
  font-weight: 600;
}

.bottom-placeholder {
  height: 70px;
}

.cancel-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
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
  gap: 10px;
  flex-shrink: 0;
}

.item-price-group {
  text-align: right;
}

.item-deal {
  font-size: 14px;
  color: $primary;
}

.item-original {
  font-size: 11px;
  color: $text-muted;
  text-decoration: line-through;
  display: block;
}

.item-qty {
  font-size: 13px;
  color: $text-secondary;
}

.item-subtotal {
  font-size: 14px;
  font-weight: 600;
  color: $text;
  min-width: 50px;
  text-align: right;
}

.total-cell {
  :deep(.van-cell__value) {
    font-size: 18px;
    font-weight: 700;
    color: $primary;
  }
}

.remark-content {
  background: #fff;
  margin: 0 10px;
  border-radius: 12px;
  padding: 14px;
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.5;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}
</style>
